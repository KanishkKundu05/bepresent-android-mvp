package com.bepresent.android.ui.onboarding.v2

import android.view.Choreographer
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.datastore.PreferencesManager
import com.bepresent.android.data.subscription.SubscriptionManager
import com.bepresent.android.ui.onboarding.v2.animation.OnboardingAnimSpecs
import com.bepresent.android.ui.onboarding.v2.animation.ScreenAnimation
import com.bepresent.android.ui.onboarding.v2.util.calculateYearsBack
import com.bepresent.android.ui.onboarding.v2.util.calculateYearsOnPhone
import com.bepresent.android.ui.onboarding.v2.util.screenTimeAnswerToHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellableContinuation
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
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    val screens: List<OnboardingScreenType> = buildOnboardingScreens()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    /** Offset for slide animation: -1 = exited left, 0 = center, +1 = exited right */
    val offset = Animatable(0f)

    private val _answers = MutableStateFlow<Map<String, String>>(emptyMap())
    val answers: StateFlow<Map<String, String>> = _answers.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

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

            // Phase 1: slide current screen out
            val outAnim = if (forward) currentScreen.outroAnimation else targetScreen.introAnimation
            val outSpec = when (outAnim) {
                ScreenAnimation.Intro -> OnboardingAnimSpecs.introOut<Float>()
                ScreenAnimation.Drawer -> OnboardingAnimSpecs.drawerOut<Float>()
            }
            val outTarget = if (forward) -1f else 1f
            offset.animateTo(outTarget, outSpec)

            // Swap screen
            _currentIndex.value = targetIndex
            saveProgress()

            // Phase 2: position new screen off-screen on opposite side, then slide in
            val inAnim = if (forward) targetScreen.introAnimation else currentScreen.outroAnimation
            val inSpec = when (inAnim) {
                ScreenAnimation.Intro -> OnboardingAnimSpecs.introIn<Float>()
                ScreenAnimation.Drawer -> OnboardingAnimSpecs.drawerIn<Float>()
            }
            val inStart = if (forward) 1f else -1f
            offset.snapTo(inStart)
            offset.animateTo(0f, inSpec)

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
    }

    fun getAnswer(key: String): String? = _answers.value[key]

    fun setUsername(name: String) {
        _username.value = name
    }

    // ── Computed values ──

    val yearsOnPhone: Int
        get() {
            val screenTimeAnswer = _answers.value["ScreenTime"] ?: return 8
            val ageAnswer = _answers.value["Age"] ?: "25-34"
            val dailyHours = screenTimeAnswerToHours(screenTimeAnswer)
            return calculateYearsOnPhone(dailyHours, ageAnswer)
        }

    val yearsBack: Int
        get() = calculateYearsBack(yearsOnPhone)

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
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
            preferencesManager.clearOnboardingV2Progress()
        }
    }

    fun saveUsername() {
        viewModelScope.launch {
            preferencesManager.setOnboardingV2Username(_username.value)
        }
    }
}
