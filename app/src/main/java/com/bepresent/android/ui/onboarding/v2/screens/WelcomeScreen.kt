package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.LaurelBadge
import com.bepresent.android.ui.onboarding.v2.components.LaurelStatBadge

@Composable
fun WelcomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Stat badges
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Badge 1: Screen time reduction
            LaurelStatBadge(
                headline = "15 Hour",
                body = "Weekly Screen Time\nReduction",
                modifier = Modifier.fillMaxWidth(),
                laurelSize = 72.dp,
                spacing = 20.dp,
                expanded = true
            )

            // Badge 2: Users count
            LaurelStatBadge(
                headline = "500,000",
                body = "Satisfied Users",
                modifier = Modifier.fillMaxWidth(),
                laurelSize = 72.dp,
                spacing = 20.dp,
                expanded = true
            )

            // Badge 3: 5-star rating
            LaurelBadge(
                modifier = Modifier.fillMaxWidth(),
                laurelSize = 72.dp,
                spacing = 20.dp,
                expanded = true
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        repeat(5) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = OnboardingTokens.YellowPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"thank you for freeing me from this device\"",
                        style = OnboardingTypography.subLabel,
                        color = OnboardingTokens.NeutralBlack,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Welcome text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Welcome to\nBePresent!",
                style = OnboardingTypography.h1,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "More time to focus on what matters\nmost",
                style = OnboardingTypography.p1,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(0.75f))
        // Bottom space for the continue button
        Spacer(modifier = Modifier.height(80.dp))
    }
}
