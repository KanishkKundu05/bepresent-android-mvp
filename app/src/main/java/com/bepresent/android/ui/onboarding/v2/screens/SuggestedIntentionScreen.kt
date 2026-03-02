package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.bepresent.android.data.db.AppIntention
import com.bepresent.android.ui.intention.IntentionConfigSheet
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance

@Composable
fun SuggestedIntentionScreen(
    onComplete: () -> Unit,
    viewModel: SuggestedIntentionViewModel = hiltViewModel()
) {
    val topApp by viewModel.topApp.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState()
    val context = LocalContext.current

    var showConfigSheet by remember { mutableStateOf(false) }

    // Auto-skip if no distracting app found
    LaunchedEffect(isLoaded, topApp) {
        if (isLoaded && topApp == null) {
            onComplete()
        }
    }

    val app = topApp ?: return

    // Resolve app label and icon from PackageManager
    val appLabel = remember(app.packageName) {
        try {
            val ai = context.packageManager.getApplicationInfo(app.packageName, 0)
            context.packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            app.packageName.substringAfterLast('.')
                .replaceFirstChar { it.uppercase() }
        }
    }
    val appIcon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(80, 80).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    val weeklyHours = app.totalTimeMs / (1000 * 60 * 60)
    val weeklyMinutes = (app.totalTimeMs / (1000 * 60)) % 60
    val timeText = when {
        weeklyHours > 0 -> "${weeklyHours}h ${weeklyMinutes}m"
        else -> "${weeklyMinutes}m"
    }

    AnimatedVisibility(
        visible = isLoaded && topApp != null,
        enter = fadeIn()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // App icon
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = appLabel,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // "You spent Xh on Instagram this week"
            Text(
                text = "You spent $timeText on $appLabel\nthis week",
                style = OnboardingTypography.h2,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = "Set your first intention to\nuse it more mindfully",
                style = OnboardingTypography.p3,
                color = OnboardingTokens.Neutral800,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // CTA button
            OnboardingContinueButton(
                title = "Set My Intention",
                appearance = OnboardingButtonAppearance.Primary,
                onClick = { showConfigSheet = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Skip
            TextButton(onClick = onComplete) {
                Text(
                    text = "Skip for now",
                    style = OnboardingTypography.label,
                    color = OnboardingTokens.Neutral800
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Intention config sheet — pre-filled with the suggested app
    if (showConfigSheet) {
        IntentionConfigSheet(
            existingIntention = AppIntention(
                id = "",
                packageName = app.packageName,
                appName = appLabel,
                allowedOpensPerDay = 10,
                timePerOpenMinutes = 5
            ),
            onDismiss = { showConfigSheet = false },
            onSave = { packageName, appName, allowedOpens, timePerOpen ->
                viewModel.createIntention(packageName, appName, allowedOpens, timePerOpen)
                showConfigSheet = false
                onComplete()
            }
        )
    }
}
