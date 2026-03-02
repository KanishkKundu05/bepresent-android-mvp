package com.bepresent.android.ui.onboarding.v2.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance

/**
 * Notification permission screen. On API 33+ requests POST_NOTIFICATIONS
 * when the user taps "Enable Notifications". Below API 33 it advances directly.
 */
@Composable
fun NotificationPermissionScreen(onComplete: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — advance either way */
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(R.drawable.notifications_mask),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(200.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Stay on Track with\nNotifications",
            style = OnboardingTypography.h1,
            color = OnboardingTokens.NeutralBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Get reminders for your daily streak, session goals, and challenge progress.",
            style = OnboardingTypography.p2,
            color = OnboardingTokens.Neutral800,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        OnboardingContinueButton(
            title = "Enable Notifications",
            appearance = OnboardingButtonAppearance.Primary,
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onComplete()
                }
            },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextButton(onClick = onComplete) {
            Text(
                "Maybe Later",
                style = OnboardingTypography.label,
                color = OnboardingTokens.Neutral800
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
