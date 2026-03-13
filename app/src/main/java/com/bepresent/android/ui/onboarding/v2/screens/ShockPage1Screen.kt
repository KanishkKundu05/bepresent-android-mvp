package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance
import kotlinx.coroutines.delay

private const val EXPLODING_HEAD_HAPTIC_PROGRESS = 0.1f

@Composable
fun ShockPage1Screen(
    yearsOnPhone: Int,
    onContinue: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showButton by remember { mutableStateOf(false) }
    var lastAnimationProgress by remember { mutableFloatStateOf(0f) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.exploding_head))
    val animationProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 0.7f
    )

    LaunchedEffect(Unit) {
        delay(2000)
        showButton = true
    }

    LaunchedEffect(animationProgress) {
        val crossedBlastMoment =
            lastAnimationProgress < EXPLODING_HEAD_HAPTIC_PROGRESS &&
                animationProgress >= EXPLODING_HEAD_HAPTIC_PROGRESS
        if (crossedBlastMoment) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        lastAnimationProgress = animationProgress
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Lottie animation
            LottieAnimation(
                composition = composition,
                progress = { animationProgress },
                modifier = Modifier.size(145.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Mixed color text
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = OnboardingTokens.NeutralBlack)) {
                        append("At this rate you're going to spend ")
                    }
                    withStyle(SpanStyle(color = OnboardingTokens.BrandPrimary)) {
                        append("$yearsOnPhone years")
                    }
                    withStyle(SpanStyle(color = OnboardingTokens.NeutralBlack)) {
                        append(" of your life on your phone.")
                    }
                },
                style = OnboardingTypography.h1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(50.dp))
        }

        // Button appears after delay
        AnimatedVisibility(
            visible = showButton,
            enter = fadeIn(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            OnboardingContinueButton(
                title = "Wow.",
                appearance = OnboardingButtonAppearance.SecondaryShadow,
                onClick = onContinue
            )
        }
    }
}
