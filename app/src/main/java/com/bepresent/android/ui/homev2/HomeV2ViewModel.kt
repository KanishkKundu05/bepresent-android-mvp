package com.bepresent.android.ui.homev2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.analytics.AnalyticsEvents
import com.bepresent.android.data.analytics.AnalyticsManager
import com.bepresent.android.data.datastore.PreferencesManager
import com.bepresent.android.data.db.AppIntention
import com.bepresent.android.data.db.PresentSession
import com.bepresent.android.data.usage.UsageStatsRepository
import com.bepresent.android.features.intentions.IntentionManager
import com.bepresent.android.features.sessions.DefaultAllowedApps
import com.bepresent.android.features.sessions.DefaultBlockedApps
import com.bepresent.android.features.sessions.SessionManager
import com.bepresent.android.features.sessions.SessionStateMachine
import com.bepresent.android.permissions.PermissionManager
import com.bepresent.android.ui.homev2.components.ActiveSessionSubState
import com.bepresent.android.ui.homev2.components.ActiveSessionUiState
import com.bepresent.android.ui.homev2.components.BlockedTimeState
import com.bepresent.android.ui.homev2.components.DailyQuestUiState
import com.bepresent.android.ui.homev2.components.DayUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class HomeScreenState {
    Idle,
    Countdown,
    ActiveSession
}

data class HomeV2UiState(
    val screenState: HomeScreenState = HomeScreenState.Idle,
    val countdownValue: Int = 3,
    val blockedTimeState: BlockedTimeState = BlockedTimeState(),
    val activeSessionState: ActiveSessionUiState = ActiveSessionUiState(),
    val intentions: List<AppIntention> = emptyList(),
    val dailyQuestState: DailyQuestUiState = DailyQuestUiState(),
    val days: List<DayUiModel> = emptyList(),
    val streak: Int = 0,
    val isStreakFrozen: Boolean = false,
    val weeklyXp: Int = 0,
    val sessionModeIndex: Int = 0,        // 0 = All Apps, 1 = Specific Apps
    val sessionAllowedPackages: Set<String> = emptySet(),
    val sessionBlockedPackages: Set<String> = emptySet(),
    val sessionDurationMinutes: Int = 60,
    val sessionBeastMode: Boolean = false,
    val permissionsOk: Boolean = true,
    val totalBlockedTodayMs: Long = 0L
)

@HiltViewModel
class HomeV2ViewModel @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val intentionManager: IntentionManager,
    private val sessionManager: SessionManager,
    private val preferencesManager: PreferencesManager,
    private val permissionManager: PermissionManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _screenState = MutableStateFlow(HomeScreenState.Idle)
    private val _countdownValue = MutableStateFlow(3)
    private val _sessionModeIndex = MutableStateFlow(0)
    private val _sessionAllowedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _sessionBlockedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _sessionDurationMinutes = MutableStateFlow(60)
    private val _sessionBeastMode = MutableStateFlow(false)
    private val _totalBlockedTodayMs = MutableStateFlow(0L)
    private val _activeSessionUi = MutableStateFlow(ActiveSessionUiState())

    private val _showXpPopup = MutableStateFlow(false)
    val showXpPopup: StateFlow<Boolean> = _showXpPopup
    private val _xpPopupAmount = MutableStateFlow(0)
    val xpPopupAmount: StateFlow<Int> = _xpPopupAmount

    private var countdownJob: Job? = null
    private var sessionTimerJob: Job? = null

    val uiState: StateFlow<HomeV2UiState> = combine(
        combine(_screenState, _activeSessionUi) { screen, sessionUi -> screen to sessionUi },
        _countdownValue,
        intentionManager.observeAll(),
        sessionManager.observeActiveSession(),
        preferencesManager.totalXp
    ) { screenAndUi, countdown, intentions, activeSession, xp ->
        val (screenState, activeSessionUiVal) = screenAndUi

        // If there's an active session, ensure we're in ActiveSession state
        val effectiveState = if (activeSession != null && screenState == HomeScreenState.Idle) {
            HomeScreenState.ActiveSession
        } else {
            screenState
        }

        // Update active session timer state if session exists
        if (activeSession != null && effectiveState == HomeScreenState.ActiveSession) {
            startSessionTimerIfNeeded(activeSession)
        }

        val blockedMs = _totalBlockedTodayMs.value
        val h = (blockedMs / 3_600_000).toInt()
        val m = ((blockedMs % 3_600_000) / 60_000).toInt()
        val s = ((blockedMs % 60_000) / 1_000).toInt()

        HomeV2UiState(
            screenState = effectiveState,
            countdownValue = countdown,
            blockedTimeState = BlockedTimeState(
                hours = "%02d".format(h),
                minutes = "%02d".format(m),
                seconds = "%02d".format(s),
                sessionModeLabel = if (_sessionModeIndex.value == 0) "All apps" else "Specific apps",
                sessionDurationLabel = formatDurationLabel(_sessionDurationMinutes.value),
                selectedPackages = if (_sessionModeIndex.value == 0)
                    _sessionAllowedPackages.value else _sessionBlockedPackages.value
            ),
            activeSessionState = activeSessionUiVal,
            intentions = intentions,
            dailyQuestState = DailyQuestUiState(
                completedSession = activeSession?.state == PresentSession.STATE_COMPLETED ||
                        activeSession?.state == PresentSession.STATE_GOAL_REACHED
            ),
            days = generateDays(),
            streak = intentions.maxOfOrNull { it.streak } ?: 0,
            isStreakFrozen = false,
            weeklyXp = xp,
            sessionModeIndex = _sessionModeIndex.value,
            sessionAllowedPackages = _sessionAllowedPackages.value,
            sessionBlockedPackages = _sessionBlockedPackages.value,
            sessionDurationMinutes = _sessionDurationMinutes.value,
            sessionBeastMode = _sessionBeastMode.value,
            permissionsOk = permissionManager.checkAll().criticalGranted,
            totalBlockedTodayMs = blockedMs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeV2UiState(days = generateDays()))

    init {
        // Restore persisted session mode + selected packages, seeding defaults if empty
        viewModelScope.launch {
            preferencesManager.sessionModeIndex.first().let { _sessionModeIndex.value = it }
            val savedAllowed = preferencesManager.sessionAllowedPackages.first()
            // Always include default productivity/utility apps in the allow list
            val mergedAllowed = savedAllowed + DefaultAllowedApps.PACKAGES
            _sessionAllowedPackages.value = mergedAllowed
            if (mergedAllowed != savedAllowed) {
                preferencesManager.setSessionAllowedPackages(mergedAllowed)
            }
            val savedBlocked = preferencesManager.sessionBlockedPackages.first()
            // Always include default social apps in the UI selection
            val merged = savedBlocked + DefaultBlockedApps.PACKAGES
            _sessionBlockedPackages.value = merged
            if (merged != savedBlocked) {
                preferencesManager.setSessionBlockedPackages(merged)
            }
        }
        // Poll blocked time every 5 seconds
        viewModelScope.launch {
            while (isActive) {
                refreshBlockedTime()
                delay(5_000)
            }
        }
    }

    // --- Session Mode / Goal ---

    fun setSessionMode(index: Int) {
        _sessionModeIndex.value = index
        viewModelScope.launch { preferencesManager.setSessionModeIndex(index) }
    }

    fun setSessionApps(packages: Set<String>) {
        if (_sessionModeIndex.value == 0) {
            _sessionAllowedPackages.value = packages
            viewModelScope.launch { preferencesManager.setSessionAllowedPackages(packages) }
        } else {
            _sessionBlockedPackages.value = packages
            viewModelScope.launch { preferencesManager.setSessionBlockedPackages(packages) }
        }
    }

    fun setSessionGoal(durationMinutes: Int, beastMode: Boolean) {
        _sessionDurationMinutes.value = durationMinutes
        _sessionBeastMode.value = beastMode
    }

    // --- Countdown ---

    fun startCountdown() {
        _screenState.value = HomeScreenState.Countdown
        _countdownValue.value = 3
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (i in 3 downTo 1) {
                _countdownValue.value = i
                delay(1000)
            }
            // Countdown finished -> start session
            startSession()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _screenState.value = HomeScreenState.Idle
        _countdownValue.value = 3
    }

    // --- Session ---

    private fun startSession() {
        viewModelScope.launch {
            val packages = if (_sessionModeIndex.value == 0)
                _sessionAllowedPackages.value else _sessionBlockedPackages.value
            sessionManager.createAndStart(
                name = "Focus Session",
                goalDurationMinutes = _sessionDurationMinutes.value,
                blockedPackages = packages.toList(),
                beastMode = _sessionBeastMode.value
            )
            _screenState.value = HomeScreenState.ActiveSession
        }
    }

    private var sessionTimerActive = false

    private fun startSessionTimerIfNeeded(session: PresentSession) {
        if (sessionTimerActive) return
        sessionTimerActive = true
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val started = session.startedAt ?: now
                val goalMs = session.goalDurationMinutes * 60 * 1000L
                val elapsed = now - started
                val remaining = (goalMs - elapsed).coerceAtLeast(0)
                val progress = (elapsed.toFloat() / goalMs).coerceIn(0f, 1f)
                val (xp, _) = SessionStateMachine.calculateRewards(session.goalDurationMinutes)

                val subState = when (session.state) {
                    PresentSession.STATE_GOAL_REACHED -> ActiveSessionSubState.GoalReached
                    PresentSession.STATE_COMPLETED -> ActiveSessionSubState.GoalReached
                    else -> ActiveSessionSubState.Active
                }

                val mins = (remaining / 60_000).toInt()
                val secs = ((remaining % 60_000) / 1_000).toInt()

                _activeSessionUi.value = ActiveSessionUiState(
                    subState = subState,
                    sessionName = session.name,
                    modeLabel = if (_sessionModeIndex.value == 0) "Allow List" else "Block List",
                    timeRemainingString = "%02d:%02d".format(mins, secs),
                    progress = progress,
                    points = xp,
                    beastMode = session.beastMode
                )

                delay(1000)
            }
        }
    }

    fun giveUpSession() {
        viewModelScope.launch {
            val session = sessionManager.getActiveSession() ?: return@launch
            sessionManager.giveUp(session.id)
            resetToIdle()
        }
    }

    fun completeSession() {
        viewModelScope.launch {
            val session = sessionManager.getActiveSession() ?: return@launch
            sessionManager.complete(session.id)
            resetToIdle()
        }
    }

    fun finishAndUnblock() {
        viewModelScope.launch {
            val session = sessionManager.getActiveSession() ?: return@launch
            val (xp, _) = SessionStateMachine.calculateRewards(session.goalDurationMinutes)
            _xpPopupAmount.value = xp
            _showXpPopup.value = true
            sessionManager.complete(session.id)
            resetToIdle()
        }
    }

    fun dismissXpPopup() {
        _showXpPopup.value = false
        _xpPopupAmount.value = 0
    }

    fun cancelSession() {
        viewModelScope.launch {
            val session = sessionManager.getActiveSession() ?: return@launch
            sessionManager.cancel(session.id)
            resetToIdle()
        }
    }

    private fun resetToIdle() {
        sessionTimerJob?.cancel()
        sessionTimerActive = false
        _screenState.value = HomeScreenState.Idle
        _activeSessionUi.value = ActiveSessionUiState()
    }

    // --- Intentions ---

    fun createIntention(
        packageName: String,
        appName: String,
        allowedOpensPerDay: Int,
        timePerOpenMinutes: Int
    ) {
        viewModelScope.launch {
            intentionManager.create(packageName, appName, allowedOpensPerDay, timePerOpenMinutes)
        }
    }

    fun updateIntention(intention: AppIntention) {
        viewModelScope.launch { intentionManager.update(intention) }
    }

    fun deleteIntention(intention: AppIntention) {
        viewModelScope.launch { intentionManager.delete(intention) }
    }

    // --- Helpers ---

    private fun refreshBlockedTime() {
        viewModelScope.launch {
            try {
                _totalBlockedTodayMs.value = sessionManager.getTotalBlockedTodayMs()
            } catch (_: Exception) { }
        }
    }

    private fun generateDays(): List<DayUiModel> {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd", Locale.getDefault())

        return (-3..3).map { offset ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            val date = c.time
            val weekDay = dayFormat.format(date).replaceFirstChar { it.uppercase() }.take(3)
            DayUiModel(
                weekDay = weekDay,
                number = dateFormat.format(date),
                isEnabled = offset <= 0,
                isChecked = offset < 0, // past days shown as checked (placeholder)
                isFailed = false,        // TODO: wire real per-day goal data
                isCurrentDay = offset == 0
            )
        }
    }

    private fun formatDurationLabel(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }
}
