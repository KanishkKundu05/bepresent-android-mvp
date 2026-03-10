package com.bepresent.android.ui.onboarding.v2.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
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
fun NotificationPermissionScreen(
    onComplete: () -> Unit,
    onEnableClicked: () -> Unit = {},
    onMaybeLater: () -> Unit = {},
    onPermissionResult: (Boolean) -> Unit = {}
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        onPermissionResult(granted)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enable Screen Time\nReminders",
                style = OnboardingTypography.h2,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(35.dp))

            Image(
                painter = painterResource(R.drawable.notifications_mask),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(25.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding)
        ) {
            OnboardingContinueButton(
                title = "Enable Notifications",
                appearance = OnboardingButtonAppearance.Secondary,
                onClick = {
                    onEnableClicked()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onPermissionResult(true)
                        onComplete()
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OnboardingContinueButton(
                title = "Maybe Later",
                appearance = OnboardingButtonAppearance.Plain,
                onClick = {
                    onMaybeLater()
                    onComplete()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
