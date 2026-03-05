package com.bepresent.android.ui.permissions

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.bepresent.android.permissions.PermissionManager
import androidx.compose.ui.graphics.Color
import com.bepresent.android.ui.homev2.HomeV2Tokens
import com.bepresent.android.ui.homev2.components.FullButton
import com.bepresent.android.ui.homev2.components.FullButtonAppearance
import com.bepresent.android.ui.onboarding.OnboardingEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay

private enum class GatePhase {
    OVERLAY,
    ACCESSIBILITY,
    DONE
}

/**
 * Modal bottom sheet that walks the user through granting overlay and accessibility
 * permissions. Automatically advances through phases and calls [onAllGranted] when done.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionGateSheet(
    onDismiss: () -> Unit,
    onAllGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            OnboardingEntryPoint::class.java
        )
    }
    val permissionManager = remember { entryPoint.permissionManager() }

    var phase by remember {
        val needsOverlay = !permissionManager.hasOverlayPermission()
        val needsAccessibility = !permissionManager.hasAccessibilityPermission()
        mutableStateOf(
            when {
                needsOverlay -> GatePhase.OVERLAY
                needsAccessibility -> GatePhase.ACCESSIBILITY
                else -> GatePhase.DONE
            }
        )
    }
    var waitingForPermission by remember { mutableStateOf(false) }
    var showAccessibilityWhyDialog by remember { mutableStateOf(false) }
    var showLearnMore by remember { mutableStateOf<String?>(null) }

    // Complete immediately if all permissions already granted
    LaunchedEffect(phase) {
        if (phase == GatePhase.DONE) {
            onAllGranted()
        }
    }

    // Re-check on resume
    LaunchedEffect(lifecycleOwner, phase) {
        if (phase == GatePhase.DONE) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            waitingForPermission = false
            when (phase) {
                GatePhase.OVERLAY -> {
                    if (permissionManager.hasOverlayPermission()) {
                        phase = if (permissionManager.hasAccessibilityPermission()) {
                            GatePhase.DONE
                        } else {
                            GatePhase.ACCESSIBILITY
                        }
                    }
                }
                GatePhase.ACCESSIBILITY -> {
                    if (permissionManager.hasAccessibilityPermission()) {
                        phase = GatePhase.DONE
                    }
                }
                else -> {}
            }
        }
    }

    // Poll while user is in Settings
    LaunchedEffect(waitingForPermission, phase) {
        if (!waitingForPermission) return@LaunchedEffect
        val check: () -> Boolean = when (phase) {
            GatePhase.OVERLAY -> permissionManager::hasOverlayPermission
            GatePhase.ACCESSIBILITY -> permissionManager::hasAccessibilityPermission
            else -> return@LaunchedEffect
        }
        while (!check()) {
            delay(500)
        }
        delay(300)
        waitingForPermission = false
        bringAppToForeground(context)
    }

    if (phase != GatePhase.DONE) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = HomeV2Tokens.NeutralWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (phase) {
                    GatePhase.OVERLAY -> {
                        Text(
                            text = "Enable BePresent to block distracting apps",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeV2Tokens.NeutralBlack,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Don't worry, you can take a break any time.",
                            fontSize = 15.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        GateTimeline(
                            firstInstruction = "Find BePresent in the list of apps",
                            secondInstruction = "Toggle \"Allow Display over other apps\""
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        FullButton(
                            title = "Continue",
                            appearance = FullButtonAppearance.Primary,
                            onClick = {
                                if (permissionManager.hasOverlayPermission()) {
                                    phase = if (permissionManager.hasAccessibilityPermission()) {
                                        GatePhase.DONE
                                    } else {
                                        GatePhase.ACCESSIBILITY
                                    }
                                } else {
                                    waitingForPermission = true
                                    openSettings(
                                        context,
                                        permissionManager.getOverlayPermissionIntent(),
                                        permissionManager.getAppSettingsIntent()
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showLearnMore = "overlay" }) {
                            Text(
                                "Learn more",
                                fontSize = 14.sp,
                                color = HomeV2Tokens.BrandPrimary
                            )
                        }
                    }

                    GatePhase.ACCESSIBILITY -> {
                        Text(
                            text = "Enable Accessibility permission",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeV2Tokens.NeutralBlack,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This lets BePresent detect the app currently active, so it can block distractions. We never track or read your screen content.",
                            fontSize = 15.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        GateTimeline(
                            firstInstruction = "Find BePresent in the list of services",
                            secondInstruction = "Toggle BePresent accessibility on"
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                        FullButton(
                            title = "Continue",
                            appearance = FullButtonAppearance.Primary,
                            onClick = {
                                if (permissionManager.hasAccessibilityPermission()) {
                                    phase = GatePhase.DONE
                                } else {
                                    showAccessibilityWhyDialog = true
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showLearnMore = "accessibility" }) {
                            Text(
                                "Learn more",
                                fontSize = 14.sp,
                                color = HomeV2Tokens.BrandPrimary
                            )
                        }
                    }

                    GatePhase.DONE -> {}
                }
            }
        }
    }

    // Accessibility "why" dialog
    if (showAccessibilityWhyDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityWhyDialog = false },
            title = { Text("Why Accessibility is needed") },
            text = {
                Text(
                    "BePresent uses accessibility permission to detect which app you're " +
                        "currently using so it can block distractions during a focus session. " +
                        "We do not read, record, or collect your screen content."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityWhyDialog = false
                        waitingForPermission = true
                        openSettings(
                            context,
                            permissionManager.getAccessibilitySettingsIntent(),
                            permissionManager.getAppSettingsIntent()
                        )
                    }
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityWhyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Learn more dialogs
    if (showLearnMore != null) {
        val (dialogTitle, dialogBody) = when (showLearnMore) {
            "overlay" -> "Why overlay is needed" to
                "Overlay permission lets BePresent show the blocking shield above distracting apps so you can immediately return to your focus session."
            "accessibility" -> "Why accessibility is needed" to
                "Accessibility lets BePresent detect the active app during focus sessions to enforce blocks. Screen contents are never read or stored."
            else -> "" to ""
        }
        AlertDialog(
            onDismissRequest = { showLearnMore = null },
            title = { Text(dialogTitle) },
            text = { Text(dialogBody) },
            confirmButton = {
                Button(onClick = { showLearnMore = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun GateTimeline(
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
            GateTimelineNumber(1)
            Spacer(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .size(width = 2.dp, height = 34.dp)
                    .background(HomeV2Tokens.Neutral200)
            )
            GateTimelineNumber(2)
        }

        Spacer(modifier = Modifier.size(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(26.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = firstInstruction,
                fontSize = 15.sp,
                color = HomeV2Tokens.NeutralBlack
            )
            Text(
                text = secondInstruction,
                fontSize = 15.sp,
                color = HomeV2Tokens.NeutralBlack
            )
        }
    }
}

@Composable
private fun GateTimelineNumber(number: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(HomeV2Tokens.BrandPrimary)
            .border(1.dp, HomeV2Tokens.BrandPrimary, CircleShape)
    ) {
        Text(
            text = number.toString(),
            fontSize = 14.sp,
            color = HomeV2Tokens.NeutralWhite,
            fontWeight = FontWeight.Bold
        )
    }
}

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
