package com.bepresent.android.ui.onboarding.v2.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.bepresent.android.R
import com.bepresent.android.ui.onboarding.v2.OnboardingTokens
import com.bepresent.android.ui.onboarding.v2.OnboardingTypography
import com.bepresent.android.ui.onboarding.v2.components.OnboardingButtonAppearance
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import kotlinx.coroutines.delay

@Composable
fun WelcomeSubscriptionScreen(onContinue: () -> Unit) {
    var showWelcomeText by remember { mutableStateOf(false) }
    var showGratitudeText by remember { mutableStateOf(false) }
    var showImage by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.confetti_animation))

    LaunchedEffect(Unit) {
        showWelcomeText = true
        delay(200)
        showGratitudeText = true
        delay(200)
        showImage = true
        delay(600)
        showButton = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(220.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = OnboardingTokens.ScreenHorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1.1f))

            AnimatedVisibility(visible = showWelcomeText, enter = fadeIn()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Welcome to",
                        style = OnboardingTypography.h1,
                        color = OnboardingTokens.NeutralBlack,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "BePresent",
                        style = OnboardingTypography.h1,
                        color = OnboardingTokens.BrandPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = showGratitudeText, enter = fadeIn()) {
                Text(
                    text = "My brother and I are so grateful for your support \uD83D\uDC99",
                    style = OnboardingTypography.p2,
                    color = OnboardingTokens.NeutralBlack,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(visible = showImage, enter = fadeIn()) {
                Image(
                    painter = painterResource(R.drawable.jack_charles_welcome_polaroid),
                    contentDescription = "Jack and Charles polaroid",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(width = 270.dp, height = 345.dp)
                        .shadow(
                            elevation = 25.dp,
                            ambientColor = OnboardingTokens.NeutralBlack.copy(alpha = 0.5f),
                            spotColor = OnboardingTokens.NeutralBlack.copy(alpha = 0.5f)
                        )
                        .rotate(-3f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = showButton,
                enter = fadeIn(),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                OnboardingContinueButton(
                    title = "Continue",
                    appearance = OnboardingButtonAppearance.Primary,
                    onClick = onContinue
                )
            }
        }
    }
}
