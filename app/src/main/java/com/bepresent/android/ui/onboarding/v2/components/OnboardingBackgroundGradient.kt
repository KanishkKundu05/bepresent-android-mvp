package com.bepresent.android.ui.onboarding.v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens

/**
 * Animated background gradient that transitions between blue / orange / white.
 * @param gradientProgress 0f = blue, 1f = orange, 2f = white
 */
@Composable
fun OnboardingBackgroundGradient(
    gradientProgress: Float,
    modifier: Modifier = Modifier
) {
    val topColor: Color
    val bottomColor: Color

    when {
        gradientProgress <= 1f -> {
            // Blue → Orange
            val t = gradientProgress.coerceIn(0f, 1f)
            topColor = lerp(OnboardingTokens.BlueGradientTop, OnboardingTokens.NeutralWhite, t)
            bottomColor = lerp(OnboardingTokens.BlueGradientBottom, OnboardingTokens.OrangeGradientBottom.copy(alpha = 0.5f), t)
        }
        else -> {
            // Orange → White
            val t = (gradientProgress - 1f).coerceIn(0f, 1f)
            topColor = lerp(OnboardingTokens.NeutralWhite, OnboardingTokens.NeutralWhite, t)
            bottomColor = lerp(OnboardingTokens.OrangeGradientBottom.copy(alpha = 0.5f), OnboardingTokens.NeutralWhite, t)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor)
                )
            )
    )
}
