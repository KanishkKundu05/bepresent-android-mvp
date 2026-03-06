package com.bepresent.android.features.blocking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bepresent.android.debug.RuntimeLog
import com.bepresent.android.data.datastore.PreferencesManager
import com.bepresent.android.data.db.AppIntention
import com.bepresent.android.data.db.PresentSession
import com.bepresent.android.data.db.PresentSessionDao
import com.bepresent.android.features.intentions.IntentionManager
import com.bepresent.android.features.sessions.SessionManager
import com.bepresent.android.features.sessions.SessionStateMachine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ShieldScreen(
    blockedPackage: String,
    shieldType: String,
    intentionManager: IntentionManager,
    sessionManager: SessionManager,
    sessionDao: PresentSessionDao,
    preferencesManager: PreferencesManager,
    onNavigateHome: () -> Unit,
    onFinish: () -> Unit
) {
    LaunchedEffect(blockedPackage, shieldType) {
        RuntimeLog.i(TAG, "ShieldScreen: package=$blockedPackage shieldType=$shieldType")
    }

    when (shieldType) {
        BlockedAppActivity.SHIELD_SESSION -> SessionShield(
            sessionDao = sessionDao,
            onNavigateHome = onNavigateHome
        )
        BlockedAppActivity.SHIELD_GOAL_REACHED -> GoalReachedShield(
            sessionDao = sessionDao,
            sessionManager = sessionManager,
            onNavigateHome = onNavigateHome
        )
        BlockedAppActivity.SHIELD_SCHEDULE -> ScheduleShield(
            onNavigateHome = onNavigateHome
        )
        BlockedAppActivity.SHIELD_INTENTION -> IntentionShield(
            blockedPackage = blockedPackage,
            intentionManager = intentionManager,
            preferencesManager = preferencesManager,
            onNavigateHome = onNavigateHome,
            onFinish = onFinish
        )
        else -> UnknownShield(
            blockedPackage = blockedPackage,
            shieldType = shieldType,
            onNavigateHome = onNavigateHome
        )
    }
}

@Composable
private fun SessionShield(
    sessionDao: PresentSessionDao,
    onNavigateHome: () -> Unit
) {
    var session by remember { mutableStateOf<PresentSession?>(null) }

    LaunchedEffect(Unit) {
        session = sessionDao.getActiveSession()
        RuntimeLog.i(TAG, "SessionShield: activeSession=${session?.id} state=${session?.state}")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "\uD83D\uDEE1\uFE0F", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Focus Session",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            session?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    RuntimeLog.d(TAG, "SessionShield: Nevermind -> navigate home")
                    onNavigateHome()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Be Present", style = MaterialTheme.typography.titleMedium)
            }

            session?.let { s ->
                Spacer(modifier = Modifier.height(16.dp))
                if (s.beastMode) {
                    Text(
                        text = "Beast Mode is ON \u2014 this session cannot be ended early",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    TextButton(onClick = { /* Show unlock info */ }) {
                        Text("Unlock?")
                    }
                    Text(
                        text = "To end this session, open BePresent and tap \"Give Up\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalReachedShield(
    sessionDao: PresentSessionDao,
    sessionManager: SessionManager,
    onNavigateHome: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<PresentSession?>(null) }

    LaunchedEffect(Unit) {
        session = sessionDao.getActiveSession()
        RuntimeLog.i(TAG, "GoalReachedShield: activeSession=${session?.id} state=${session?.state}")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "\uD83C\uDF89", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Session Goal Reached!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            session?.let {
                val (xp, _) = SessionStateMachine.calculateRewards(it.goalDurationMinutes)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "+$xp XP",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    session?.let { s ->
                        RuntimeLog.i(TAG, "GoalReachedShield: complete session=${s.id}")
                        scope.launch {
                            sessionManager.complete(s.id)
                            onNavigateHome()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Complete", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onNavigateHome) {
                Text("Stay Present")
            }
        }
    }
}

private const val COUNTDOWN_SECONDS = 5

@Composable
private fun IntentionShield(
    blockedPackage: String,
    intentionManager: IntentionManager,
    preferencesManager: PreferencesManager,
    onNavigateHome: () -> Unit,
    onFinish: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var intention by remember { mutableStateOf<AppIntention?>(null) }
    val freezeAvailable by preferencesManager.streakFreezeAvailable.collectAsState(initial = false)
    val countdownEnabled by preferencesManager.intentionCountdownEnabled.collectAsState(initial = false)
    var countdownSeconds by remember { mutableIntStateOf(COUNTDOWN_SECONDS) }

    LaunchedEffect(blockedPackage) {
        intention = intentionManager.getByPackage(blockedPackage)
        RuntimeLog.i(
            TAG,
            "IntentionShield: lookup package=$blockedPackage found=${intention != null}"
        )
    }

    // Countdown timer (only when enabled in settings)
    LaunchedEffect(countdownEnabled) {
        if (countdownEnabled) {
            while (countdownSeconds > 0) {
                delay(1000L)
                countdownSeconds--
            }
        } else {
            countdownSeconds = 0
        }
    }

    val currentIntention = intention
    if (currentIntention == null) {
        RuntimeLog.w(TAG, "IntentionShield: no intention found for package=$blockedPackage")
        MissingIntentionShield(
            blockedPackage = blockedPackage,
            onNavigateHome = onNavigateHome
        )
        return
    }

    val isOverLimit = currentIntention.totalOpensToday >= currentIntention.allowedOpensPerDay
    val opensLeft = (currentIntention.allowedOpensPerDay - currentIntention.totalOpensToday)
        .coerceAtLeast(0)
    val isCountingDown = countdownSeconds > 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Heading
            Text(
                text = "Is this Important?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Opens remaining counter
            Text(
                text = "$opensLeft/${currentIntention.allowedOpensPerDay}",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "opens left today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Indicator circles
            OpensIndicator(
                total = currentIntention.allowedOpensPerDay,
                remaining = opensLeft
            )

            // Over-limit warning
            if (isOverLimit) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You've used all your opens today",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
                if (currentIntention.streak > 0 && !freezeAvailable) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Opening will break your \uD83D\uDD25 ${currentIntention.streak} day streak",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (freezeAvailable) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Streak Freeze Active \u2744\uFE0F",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Open button with countdown
            Button(
                onClick = {
                    scope.launch {
                        RuntimeLog.i(
                            TAG,
                            "IntentionShield: openApp intentionId=${currentIntention.id} overLimit=$isOverLimit"
                        )
                        intentionManager.openApp(currentIntention.id)
                        onFinish()
                    }
                },
                enabled = !isCountingDown,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (isCountingDown) {
                        "Open in ${countdownSeconds}s"
                    } else if (isOverLimit) {
                        "Open Anyway"
                    } else {
                        "Open for ${currentIntention.timePerOpenMinutes} min"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dismiss text
            TextButton(onClick = onNavigateHome) {
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OpensIndicator(
    total: Int,
    remaining: Int
) {
    val filledColor = MaterialTheme.colorScheme.primary
    val hollowColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total) {
            if (i > 0) Spacer(modifier = Modifier.width(8.dp))
            val isFilled = i < remaining
            Canvas(modifier = Modifier.size(12.dp)) {
                if (isFilled) {
                    drawCircle(color = filledColor)
                } else {
                    drawCircle(
                        color = hollowColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleShield(
    onNavigateHome: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "\uD83E\uDD77", fontSize = 64.sp) // ninja emoji
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Scheduled Block Active",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This app is blocked during your scheduled session",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    RuntimeLog.d(TAG, "ScheduleShield: Be Present -> navigate home")
                    onNavigateHome()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Be Present", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun UnknownShield(
    blockedPackage: String,
    shieldType: String,
    onNavigateHome: () -> Unit
) {
    LaunchedEffect(blockedPackage, shieldType) {
        RuntimeLog.w(TAG, "UnknownShield: package=$blockedPackage shieldType=$shieldType")
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Unknown shield type: $shieldType",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateHome) {
                Text("Go Home")
            }
        }
    }
}

@Composable
private fun MissingIntentionShield(
    blockedPackage: String,
    onNavigateHome: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Intention not found for $blockedPackage",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateHome) {
                Text("Go Home")
            }
        }
    }
}

private const val TAG = "BP_Shield"
