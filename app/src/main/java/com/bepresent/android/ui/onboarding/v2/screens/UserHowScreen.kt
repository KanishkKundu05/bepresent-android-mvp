package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography

@Composable
fun UserHowScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "With Psychology\nand Gamification",
                style = OnboardingTypography.title2,
                color = OnboardingTokens.NeutralBlack,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 60.dp)
                    .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding)
            )

            Spacer(modifier = Modifier.weight(0.3f))

            // Overlapping bubble cards
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                val assetLift = minOf(maxHeight * 0.2f, 96.dp)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = -assetLift)
                ) {
                    // Bubble 1: Streaks (tilted right, offset right)
                    BubbleCard(
                        emoji = "\uD83D\uDD25",
                        title = "Streaks",
                        modifier = Modifier
                            .offset(x = 80.dp, y = 0.dp)
                            .rotate(1.5f)
                            .scale(0.8f)
                    )

                    // Bubble 2: Leaderboards (tilted left, offset left)
                    BubbleCard(
                        emoji = "\uD83C\uDFC6",
                        title = "Leaderboards",
                        modifier = Modifier
                            .offset(x = (-85).dp, y = 60.dp)
                            .rotate(-1.5f)
                    )

                    // Phone image overlapping bubbles
                    Image(
                        painter = painterResource(R.drawable.user_how_phone),
                        contentDescription = "Phone screenshot",
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .align(Alignment.BottomCenter)
                            .offset(y = 20.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Bubble 3: Accountability (tilted right, offset right)
                BubbleCard(
                    emoji = "\uD83E\uDD1D",
                    title = "Accountability",
                    modifier = Modifier
                        .offset(x = 90.dp, y = 120.dp)
                        .rotate(2.5f)
                        .zIndex(2f)
                )
            }
        }

        // Bottom gradient overlay — matches the blue background to soften the image edge
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to OnboardingTokens.BlueGradientBottom.copy(alpha = 0f),
                            0.45f to OnboardingTokens.BlueGradientBottom.copy(alpha = 0.2f),
                            0.75f to OnboardingTokens.BlueGradientBottom.copy(alpha = 0.7f),
                            1.0f to OnboardingTokens.BlueGradientBottom
                        )
                    )
                )
        )
    }
}

@Composable
private fun BubbleCard(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(OnboardingTokens.NeutralWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, style = OnboardingTypography.h2)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = OnboardingTypography.label,
            color = OnboardingTokens.NeutralBlack
        )
    }
}
