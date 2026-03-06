package com.bepresent.android.ui.onboarding.v2.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography

enum class OnboardingButtonAppearance {
    Primary,         // Brand blue bg, white text
    Secondary,       // Black bg, white text, with drop shadow
    SecondaryShadow  // Black bg + darker shadow offset
}

@Composable
fun OnboardingContinueButton(
    title: String,
    modifier: Modifier = Modifier,
    appearance: OnboardingButtonAppearance = OnboardingButtonAppearance.SecondaryShadow,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = when (appearance) {
        OnboardingButtonAppearance.Primary -> OnboardingTokens.BrandPrimary
        OnboardingButtonAppearance.Secondary -> OnboardingTokens.NeutralBlack
        OnboardingButtonAppearance.SecondaryShadow -> OnboardingTokens.NeutralBlack
    }

    val contentColor = OnboardingTokens.NeutralWhite

    val dropShadowColor = when (appearance) {
        OnboardingButtonAppearance.Primary -> OnboardingTokens.BrandDropShadow
        OnboardingButtonAppearance.Secondary -> null
        OnboardingButtonAppearance.SecondaryShadow -> OnboardingTokens.Neutral800
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Drop shadow
        if (dropShadowColor != null) {
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OnboardingTokens.ButtonHeight)
                    .offset(y = 4.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = dropShadowColor,
                    disabledContainerColor = dropShadowColor.copy(alpha = 0.3f)
                ),
                enabled = false,
                contentPadding = PaddingValues(0.dp)
            ) {}
        }

        // Main button
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(OnboardingTokens.ButtonHeight),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = contentColor,
                disabledContainerColor = backgroundColor.copy(alpha = 0.2f),
                disabledContentColor = contentColor.copy(alpha = 0.2f)
            ),
            enabled = enabled && !isLoading,
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = contentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = title,
                    style = OnboardingTypography.p1
                )
            }
        }
    }
}
