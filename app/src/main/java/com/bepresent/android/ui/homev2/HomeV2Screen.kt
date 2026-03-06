package com.bepresent.android.ui.homev2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.zIndex
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bepresent.android.data.db.AppIntention
import com.bepresent.android.ui.homev2.components.ActiveSessionCard
import com.bepresent.android.ui.homev2.components.BlockedTimeCard
import com.bepresent.android.ui.homev2.components.HomeDateCarousel
import com.bepresent.android.ui.homev2.components.HomeHeaderRow
import com.bepresent.android.ui.homev2.components.IntentionsCard
import com.bepresent.android.ui.homev2.components.SessionCountdownCard
import com.bepresent.android.ui.homev2.components.SessionGoalSheet
import com.bepresent.android.ui.homev2.components.SessionModeSheet
import com.bepresent.android.ui.homev2.components.StreakSheet
import com.bepresent.android.ui.intention.IntentionConfigSheet
import com.bepresent.android.ui.profile.ProfileSheet

@Composable
@Suppress("unused")
fun HomeV2Screen(
    viewModel: HomeV2ViewModel,
    onLeaderboardClick: () -> Unit = {},
    onDevClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    // Sheet states
    var showProfileSheet by remember { mutableStateOf(false) }
    var showStreakSheet by remember { mutableStateOf(false) }
    var showModeSheet by remember { mutableStateOf(false) }
    var showGoalSheet by remember { mutableStateOf(false) }
    var showIntentionConfig by remember { mutableStateOf(false) }
    var editingIntention by remember { mutableStateOf<AppIntention?>(null) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Background gradient
        BackgroundV2()

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header (not scrollable) — sits just below system status bar
            HomeHeaderRow(
                streak = uiState.streak,
                isStreakFrozen = uiState.isStreakFrozen,
                weeklyXp = uiState.weeklyXp,
                onProfileClick = { showProfileSheet = true },
                onStreakClick = { showStreakSheet = true },
                onDevClick = onDevClick,
                modifier = Modifier.padding(top = statusBarTop, bottom = 2.dp)
            )

            // Scrollable body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Date carousel (only in idle state)
                if (uiState.screenState == HomeScreenState.Idle) {
                    HomeDateCarousel(
                        days = uiState.days,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .zIndex(1f)
                    )
                }

                // Main card — state-switched
                CardV2(modifier = Modifier.padding(horizontal = 16.dp)) {
                    when (uiState.screenState) {
                        HomeScreenState.Idle -> {
                            BlockedTimeCard(
                                state = uiState.blockedTimeState,
                                onSessionModeClick = { showModeSheet = true },
                                onSessionGoalClick = { showGoalSheet = true },
                                onBlockNowClick = { viewModel.startCountdown() }
                            )
                        }
                        HomeScreenState.Countdown -> {
                            SessionCountdownCard(
                                count = uiState.countdownValue,
                                onCancel = { viewModel.cancelCountdown() }
                            )
                        }
                        HomeScreenState.ActiveSession -> {
                            ActiveSessionCard(
                                state = uiState.activeSessionState,
                                onTakeBreak = { /* TODO: break flow */ },
                                onEndBreak = { /* TODO: end break */ },
                                onGiveUp = { viewModel.giveUpSession() },
                                onBeastModeInfo = { /* TODO: beast mode info */ }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Intentions card
                CardV2(modifier = Modifier.padding(horizontal = 16.dp)) {
                    IntentionsCard(
                        intentions = uiState.intentions,
                        onReload = { /* TODO: reload intentions */ },
                        onAdd = {
                            showIntentionConfig = true
                        },
                        onIntentionClick = { intention ->
                            editingIntention = intention
                        }
                    )
                }

                Spacer(modifier = Modifier.height(100.dp)) // Bottom padding for tab bar
            }
        }
    }

    // --- Bottom Sheets ---

    if (showStreakSheet) {
        StreakSheet(
            intentions = uiState.intentions,
            isStreakFrozen = uiState.isStreakFrozen,
            onDismiss = { showStreakSheet = false },
            onAddIntention = { showIntentionConfig = true }
        )
    }

    if (showProfileSheet) {
        ProfileSheet(
            onDismiss = { showProfileSheet = false },
            onGetHelp = { /* TODO */ },
            onGiveFeedback = { /* TODO */ },
            onScreenTimeSettings = { /* TODO */ }
        )
    }

    if (showModeSheet) {
        SessionModeSheet(
            currentModeIndex = uiState.sessionModeIndex,
            onDismiss = { showModeSheet = false },
            onSetMode = { index ->
                viewModel.setSessionMode(index)
                showModeSheet = false
            }
        )
    }

    if (showGoalSheet) {
        SessionGoalSheet(
            currentDurationMinutes = uiState.sessionDurationMinutes,
            currentBeastMode = uiState.sessionBeastMode,
            onDismiss = { showGoalSheet = false },
            onSetGoal = { duration, beast ->
                viewModel.setSessionGoal(duration, beast)
                showGoalSheet = false
            }
        )
    }

    // Intention Config (new)
    if (showIntentionConfig) {
        val existingPackages = uiState.intentions.map { it.packageName }.toSet()
        IntentionConfigSheet(
            excludePackages = existingPackages,
            onDismiss = { showIntentionConfig = false },
            onSave = { packageName, appName, opens, time ->
                viewModel.createIntention(
                    packageName = packageName,
                    appName = appName,
                    allowedOpensPerDay = opens,
                    timePerOpenMinutes = time
                )
                showIntentionConfig = false
            }
        )
    }

    // Intention Config (edit)
    if (editingIntention != null) {
        val existingPackages = uiState.intentions.map { it.packageName }.toSet() - editingIntention!!.packageName
        IntentionConfigSheet(
            existingIntention = editingIntention,
            excludePackages = existingPackages,
            onDismiss = { editingIntention = null },
            onSave = { packageName, appName, opens, time ->
                viewModel.updateIntention(
                    editingIntention!!.copy(
                        packageName = packageName,
                        appName = appName,
                        allowedOpensPerDay = opens,
                        timePerOpenMinutes = time
                    )
                )
                editingIntention = null
            },
            onDelete = {
                viewModel.deleteIntention(editingIntention!!)
                editingIntention = null
            }
        )
    }
}
