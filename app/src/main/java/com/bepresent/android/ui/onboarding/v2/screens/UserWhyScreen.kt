package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography

@Composable
fun UserWhyScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "BePresent\ncombines app\nblocking with\ngamification",
                style = OnboardingTypography.title2,
                color = OnboardingTokens.NeutralBlack,
                modifier = Modifier
                    .padding(top = 60.dp)
                    .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(R.drawable.user_why_phone),
                contentDescription = "Phone screenshot",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clipToBounds(),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter
            )
        }

        // Bottom gradient overlay — uses exact background gradient color for seamless blend
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            OnboardingTokens.BlueGradientBottom.copy(alpha = 0f),
                            OnboardingTokens.BlueGradientBottom
                        )
                    )
                )
        )
    }
}
