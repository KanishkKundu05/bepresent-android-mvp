package com.bepresent.android.ui.onboarding.v2

import android.view.Choreographer
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.analytics.AnalyticsEvents
import com.bepresent.android.data.analytics.AnalyticsManager
import com.bepresent.android.data.datastore.PreferencesManager
import com.bepresent.android.data.subscription.SubscriptionManager
import com.bepresent.android.ui.onboarding.v2.animation.OnboardingAnimSpecs
import com.bepresent.android.ui.onboarding.v2.animation.ScreenAnimation
import com.bepresent.android.ui.onboarding.v2.util.calculateYearsBack
import com.bepresent.android.ui.onboarding.v2.util.calculateYearsOnPhone
import com.bepresent.android.ui.onboarding.v2.util.screenTimeAnswerToHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val subscriptionManager: SubscriptionManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    val screens: List<OnboardingScreenType> = buildOnboardingScreens()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    /** Offset for slide animation: -1 = exited left, 0 = center, +1 = exited right */
    val offset = Animatable(0f)

    /** Index of the incoming screen during a transition (null when idle). */
    private val _incomingIndex = MutableStateFlow<Int?>(null)
    val incomingIndex: StateFlow<Int?> = _incomingIndex.asStateFlow()

    /** Offset for the incoming screen during a simultaneous slide. */
    val incomingOffset = Animatable(0f)

    private val _answers = MutableStateFlow<Map<String, String>>(emptyMap())
    val answers: StateFlow<Map<String, String>> = _answers.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    /** Frame clock backed by Android Choreographer so Animatable works in viewModelScope. */
    private val frameClock = object : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            suspendCancellableCoroutine { cont ->
                val callback = Choreographer.FrameCallback { frameTimeNanos ->
                    cont.resume(onFrame(frameTimeNanos))
                }
                Choreographer.getInstance().postFrameCallback(callback)
                cont.invokeOnCancellation {
                    Choreographer.getInstance().removeFrameCallback(callback)
                }
            }
    }

    val totalScreens: Int get() = screens.size

    val currentScreen: OnboardingScreenType
        get() = screens.getOrElse(_currentIndex.value) { screens.last() }

    init {
        viewModelScope.launch {
            loadProgress()
        }
    }

    // ── Navigation ──

    fun advance() {
        if (_isAnimating.value) return
        val nextIndex = _currentIndex.value + 1
        if (nextIndex >= screens.size) {
            completeOnboarding()
            return
        }
        animateTransition(forward = true, targetIndex = nextIndex)
    }

    fun goBack() {
        if (_isAnimating.value) return
        val prevIndex = _currentIndex.value - 1
        if (prevIndex < 0) return
        animateTransition(forward = false, targetIndex = prevIndex)
    }

    private fun animateTransition(forward: Boolean, targetIndex: Int) {
        viewModelScope.launch(frameClock) {
            _isAnimating.value = true
            val currentScreen = screens[_currentIndex.value]
            val targetScreen = screens[targetIndex]

            // Determine animation specs
            val outAnim = if (forward) currentScreen.outroAnimation else targetScreen.introAnimation
            val outSpec = when (outAnim) {
                ScreenAnimation.Intro -> OnboardingAnimSpecs.introOut<Float>()
                ScreenAnimation.Drawer -> OnboardingAnimSpecs.drawerOut<Float>()
            }
            val outTarget = if (forward) -1f else 1f

            val inAnim = if (forward) targetScreen.introAnimation else currentScreen.outroAnimation
            val inSpec = when (inAnim) {
                ScreenAnimation.Intro -> OnboardingAnimSpecs.introIn<Float>()
                ScreenAnimation.Drawer -> OnboardingAnimSpecs.drawerIn<Float>()
            }
            val inStart = if (forward) 1f else -1f

            // Show incoming screen and position it off-screen
            _incomingIndex.value = targetIndex
            incomingOffset.snapTo(inStart)

            // Animate both screens simultaneously
            coroutineScope {
                launch { offset.animateTo(outTarget, outSpec) }
                launch { incomingOffset.animateTo(0f, inSpec) }
            }

            // Swap: incoming becomes current
            _currentIndex.value = targetIndex
            offset.snapTo(0f)
            _incomingIndex.value = null

            saveProgress()

            // Track screen view
            analyticsManager.track(
                AnalyticsEvents.VIEWED_ONBOARDING_SCREEN,
                mapOf("screen_name" to (screens[targetIndex]::class.simpleName ?: "Unknown"))
            )

            _isAnimating.value = false
        }
    }

    /** Jump to a specific index without animation (used for restoring progress). */
    fun jumpTo(index: Int) {
        _currentIndex.value = index.coerceIn(0, screens.size - 1)
    }

    // ── Answers ──

    fun setAnswer(key: String, value: String) {
        _answers.value = _answers.value + (key to value)
        viewModelScope.launch { saveAnswers() }

        // Track the answer
        analyticsManager.track(
            AnalyticsEvents.ANSWERED_ONBOARDING_QUESTION,
            mapOf("question" to key, "answer" to value)
        )

        // Track specific answer events
        when (key) {
            "Age" -> analyticsManager.track(
                AnalyticsEvents.SET_AGE,
                mapOf("age" to value)
            )
            "ScreenTime" -> analyticsManager.track(
                AnalyticsEvents.SET_SCREEN_TIME_ESTIMATE,
                mapOf("estimate" to value)
            )
            "Acquisition", "acquisition" -> analyticsManager.track(
                AnalyticsEvents.ANSWERED_ACQUISITION_QUESTION,
                mapOf("selection" to value, "other_text" to "")
            )
        }
    }

    fun getAnswer(key: String): String? = _answers.value[key]

    fun setUsername(name: String) {
        _username.value = name
    }

    // ── Computed values ──

    val yearsOnPhone: Int
        get() {
            val screenTimeAnswer = _answers.value["ScreenTime"] ?: return 8
            return calculateYearsOnPhone(screenTimeAnswer)
        }

    val yearsBack: String
        get() = calculateYearsBack(yearsOnPhone)

    val hoursSavedEstimate: Int
        get() {
            val screenTimeAnswer = _answers.value["ScreenTime"] ?: return 2
            return (screenTimeAnswerToHours(screenTimeAnswer) / 2).coerceAtLeast(1)
        }

    // ── Persistence ──

    private suspend fun saveProgress() {
        preferencesManager.setOnboardingV2Progress(_currentIndex.value)
    }

    private suspend fun saveAnswers() {
        val encoded = _answers.value.entries.joinToString(";") { "${it.key}=${it.value}" }
        preferencesManager.setOnboardingV2Answers(encoded)
    }

    private suspend fun loadProgress() {
        val savedIndex = preferencesManager.getOnboardingV2ProgressOnce()
        if (savedIndex > 0) {
            var index = savedIndex.coerceIn(0, screens.size - 1)
            // Paywall guard: if saved past paywall but subscription inactive, clamp to paywall
            val paywallIndex = screens.indexOfFirst { it is OnboardingScreenType.Paywall }
            if (paywallIndex >= 0 && index > paywallIndex) {
                val active = subscriptionManager.isSubscriptionActive()
                if (!active) {
                    index = paywallIndex
                }
            }
            _currentIndex.value = index
        }
        val savedAnswers = preferencesManager.getOnboardingV2AnswersOnce()
        if (savedAnswers.isNotEmpty()) {
            _answers.value = savedAnswers
                .split(";")
                .filter { it.contains("=") }
                .associate {
                    val parts = it.split("=", limit = 2)
                    parts[0] to parts[1]
                }
        }
        val savedUsername = preferencesManager.getOnboardingV2UsernameOnce()
        if (savedUsername.isNotEmpty()) {
            _username.value = savedUsername
        }
    }

    fun completeOnboarding() {
        analyticsManager.track(AnalyticsEvents.COMPLETED_ONBOARDING)
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
            preferencesManager.clearOnboardingV2Progress()
            _isComplete.value = true
        }
    }

    fun saveUsername() {
        analyticsManager.track(AnalyticsEvents.ENTERED_USERNAME)
        viewModelScope.launch {
            preferencesManager.setOnboardingV2Username(_username.value)
        }
    }

    // ── Notification Permission Tracking ──

    fun trackClickedEnableNotifications() {
        analyticsManager.track(AnalyticsEvents.CLICKED_ENABLE_NOTIFICATIONS)
    }

    fun trackMaybeLaterNotifications() {
        analyticsManager.track(AnalyticsEvents.TAPPED_MAYBE_LATER_NOTIFICATIONS)
    }

    fun trackNotificationPermissionResult(granted: Boolean) {
        analyticsManager.track(
            AnalyticsEvents.NOTIFICATION_PERMISSION_RESULT,
            mapOf("granted" to granted)
        )
    }
}
