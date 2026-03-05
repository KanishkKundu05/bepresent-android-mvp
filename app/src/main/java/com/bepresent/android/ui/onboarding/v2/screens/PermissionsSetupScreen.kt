package com.bepresent.android.ui.onboarding.v2.screens

import android.app.Activity
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bepresent.android.permissions.PermissionManager
import com.bepresent.android.ui.onboarding.OnboardingEntryPoint
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay

@Composable
fun PermissionsSetupScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            OnboardingEntryPoint::class.java
        )
    }
    val permissionManager = remember { entryPoint.permissionManager() }

    var usageGranted by remember { mutableStateOf(permissionManager.hasUsageStatsPermission()) }
    var waitingForPermission by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var showLearnMore by remember { mutableStateOf(false) }

    val doComplete: () -> Unit = {
        if (!finished) {
            finished = true
            onComplete()
        }
    }

    // Re-check permission on every resume
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            waitingForPermission = false
            val granted = permissionManager.hasUsageStatsPermission()
            usageGranted = granted
            if (granted) {
                doComplete()
            }
        }
    }

    // Poll permission while user is in Settings
    LaunchedEffect(waitingForPermission) {
        if (!waitingForPermission) return@LaunchedEffect
        while (!permissionManager.hasUsageStatsPermission()) {
            delay(500)
        }
        delay(300)
        waitingForPermission = false
        bringAppToForeground(context)
    }

    // Auto-complete if already granted
    LaunchedEffect(usageGranted) {
        if (usageGranted) doComplete()
    }

    // ── Main UI ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Allow BePresent to Monitor Screentime",
            style = OnboardingTypography.h2,
            textAlign = TextAlign.Center,
            color = OnboardingTokens.NeutralBlack
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "All your information is secure and will stay 100% on your phone.",
            style = OnboardingTypography.p3,
            color = OnboardingTokens.Neutral800,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))
        TwoStepTimeline(
            firstInstruction = "Find BePresent in the list of apps",
            secondInstruction = "Toggle \"Permit Usage Access\""
        )

        Spacer(modifier = Modifier.height(32.dp))
        OnboardingContinueButton(
            title = "Continue",
            appearance = OnboardingButtonAppearance.Primary,
            onClick = {
                if (usageGranted) {
                    doComplete()
                } else {
                    waitingForPermission = true
                    openSettings(
                        context,
                        permissionManager.getUsageAccessIntent(),
                        permissionManager.getAppSettingsIntent()
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = { showLearnMore = true }) {
            Text(
                "Learn more",
                style = OnboardingTypography.label,
                color = OnboardingTokens.BrandPrimary
            )
        }
    }

    if (showLearnMore) {
        AlertDialog(
            onDismissRequest = { showLearnMore = false },
            title = { Text("Why usage access is needed") },
            text = {
                Text("Usage access lets BePresent detect app launches and screen-time usage directly on your phone. Your usage data stays local.")
            },
            confirmButton = {
                Button(onClick = { showLearnMore = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// ── TwoStepTimeline ──

@Composable
private fun TwoStepTimeline(
    firstInstruction: String,
    secondInstruction: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            TimelineNumber(1)
            Spacer(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .size(width = 2.dp, height = 34.dp)
                    .background(OnboardingTokens.Neutral300)
            )
            TimelineNumber(2)
        }

        Spacer(modifier = Modifier.size(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(26.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = firstInstruction,
                style = OnboardingTypography.p3,
                color = OnboardingTokens.NeutralBlack
            )
            Text(
                text = secondInstruction,
                style = OnboardingTypography.p3,
                color = OnboardingTokens.NeutralBlack
            )
        }
    }
}

@Composable
private fun TimelineNumber(number: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(OnboardingTokens.BrandPrimary)
            .border(1.dp, OnboardingTokens.BrandPrimary, CircleShape)
    ) {
        Text(
            text = number.toString(),
            style = OnboardingTypography.label,
            color = OnboardingTokens.NeutralWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Helpers ──

private fun openSettings(context: Context, intent: Intent, fallback: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(fallback)
    }
}

private fun bringAppToForeground(context: Context) {
    val activity = context as? Activity
    if (activity != null) {
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.moveTaskToFront(activity.taskId, ActivityManager.MOVE_TASK_WITH_HOME)
            return
        } catch (_: Exception) { }
    }
    try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val appTasks = am?.appTasks
        if (!appTasks.isNullOrEmpty()) {
            appTasks[0].moveToFront()
            return
        }
    } catch (_: Exception) { }
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        context.startActivity(intent)
    } catch (_: Exception) { }
}
