package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography

@Composable
fun ShockPage2Screen(yearsBack: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding)
            .padding(bottom = 50.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "...but the good news is,\nwe'll help you get",
            style = OnboardingTypography.h2,
            color = OnboardingTokens.NeutralBlack,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "$yearsBack years\nback",
            style = OnboardingTypography.extraLarge,
            color = OnboardingTokens.BrandPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}
