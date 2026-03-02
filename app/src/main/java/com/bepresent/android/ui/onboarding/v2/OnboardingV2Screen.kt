package com.bepresent.android.ui.onboarding.v2

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.bepresent.android.ui.onboarding.v2.components.OnboardingBackgroundGradient
import com.bepresent.android.ui.onboarding.v2.components.OnboardingContinueButton
import com.bepresent.android.ui.onboarding.v2.components.OnboardingProgressBar
import com.bepresent.android.ui.onboarding.v2.screens.*

@Composable
fun OnboardingV2Screen(
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val offset by viewModel.offset.asState()
    val screen = viewModel.screens.getOrElse(currentIndex) { viewModel.screens.last() }

    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    // Animated gradient background
    val targetGradient = screen.gradientType
    val gradientProgress by animateFloatAsState(
        targetValue = when (targetGradient) {
            GradientType.Blue -> 0f
            GradientType.Orange -> 1f
            GradientType.White -> 2f
        },
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 120f),
        label = "gradient"
    )

    // Handle system back
    BackHandler(enabled = currentIndex > 0 && !isAnimating) {
        viewModel.goBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background gradient layer
        OnboardingBackgroundGradient(gradientProgress = gradientProgress)

        // Content layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Progress bar (only on screens that show it)
            if (screen.showProgressBar) {
                OnboardingProgressBar(
                    currentStep = currentIndex.toFloat(),
                    totalSteps = viewModel.totalScreens.toFloat(),
                    onBack = { viewModel.goBack() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Screen content with slide animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .offset { IntOffset((offset * screenWidthPx).roundToInt(), 0) }
            ) {
                ScreenRouter(
                    screen = screen,
                    viewModel = viewModel
                )
            }

            // Bottom button (if this screen has one)
            if (screen.buttonConfig == ButtonConfig.Full) {
                OnboardingContinueButton(
                    title = screen.buttonTitle,
                    onClick = { viewModel.advance() },
                    enabled = !isAnimating,
                    modifier = Modifier.padding(
                        horizontal = OnboardingTokens.ScreenHorizontalPadding,
                        vertical = 16.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun ScreenRouter(
    screen: OnboardingScreenType,
    viewModel: OnboardingViewModel
) {
    when (screen) {
        is OnboardingScreenType.Welcome -> WelcomeScreen()
        is OnboardingScreenType.UserWhy -> UserWhyScreen()
        is OnboardingScreenType.UserHow -> UserHowScreen()
        is OnboardingScreenType.UserWhat -> UserWhatScreen(onContinue = { viewModel.advance() })
        is OnboardingScreenType.Question -> QuestionScreen(
            title = screen.title,
            emoji = screen.emoji,
            options = screen.options,
            questionType = screen.questionType,
            selectedAnswer = viewModel.getAnswer(screen.questionType.name),
            onSelect = { answer ->
                viewModel.setAnswer(screen.questionType.name, answer)
                viewModel.advance()
            }
        )
        is OnboardingScreenType.Loading -> LoadingScreen(onComplete = { viewModel.advance() })
        is OnboardingScreenType.ShockPage1 -> ShockPage1Screen(
            yearsOnPhone = viewModel.yearsOnPhone,
            onContinue = { viewModel.advance() }
        )
        is OnboardingScreenType.ShockPage2 -> ShockPage2Screen(
            yearsBack = viewModel.yearsBack
        )
        is OnboardingScreenType.Rating -> RatingScreen(onContinue = { viewModel.advance() })
        is OnboardingScreenType.PermissionsSetup -> PermissionsSetupScreen(
            onComplete = { viewModel.advance() }
        )
        is OnboardingScreenType.NotificationPermission -> NotificationPermissionScreen(
            onComplete = { viewModel.advance() }
        )
        is OnboardingScreenType.SevenDayChallenge -> SevenDayChallengeScreen(
            yearsOnPhone = viewModel.yearsOnPhone,
            onAccepted = { viewModel.advance() }
        )
        is OnboardingScreenType.PostPaywallMessage -> PostPaywallMessageScreen(
            message = screen.message
        )
        is OnboardingScreenType.ChooseUsername -> ChooseUsernameScreen(
            username = viewModel.username.collectAsState().value,
            onUsernameChanged = { viewModel.setUsername(it) },
            onConfirm = {
                viewModel.saveUsername()
                viewModel.advance()
            }
        )
        is OnboardingScreenType.SelectApps -> AppPickerOnboardingScreen(
            onComplete = { viewModel.advance() }
        )
        is OnboardingScreenType.Acquisition -> AcquisitionScreen(
            onSelect = { answer ->
                viewModel.setAnswer("acquisition", answer)
                viewModel.advance()
            }
        )
    }
}
