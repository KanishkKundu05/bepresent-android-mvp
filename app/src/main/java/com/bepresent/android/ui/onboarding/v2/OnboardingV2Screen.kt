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
import androidx.compose.runtime.LaunchedEffect
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
    viewModel: OnboardingViewModel = hiltViewModel(),
    onComplete: () -> Unit = {}
) {
    val currentIndex by viewModel.currentIndex.collectAsState()
    val incomingIndex by viewModel.incomingIndex.collectAsState()
    val isAnimating by viewModel.isAnimating.collectAsState()
    val isComplete by viewModel.isComplete.collectAsState()
    val currentOffset by viewModel.offset.asState()
    val incomingOffsetValue by viewModel.incomingOffset.asState()

    LaunchedEffect(isComplete) {
        if (isComplete) onComplete()
    }

    val currentScreen = viewModel.screens.getOrElse(currentIndex) { viewModel.screens.last() }
    val incomingScreen = incomingIndex?.let { viewModel.screens.getOrElse(it) { null } }
    // Use incoming screen for chrome (gradient, progress bar, button) during transitions
    val displayScreen = incomingScreen ?: currentScreen

    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    // Animated gradient background
    val targetGradient = displayScreen.gradientType
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
            if (displayScreen.showProgressBar) {
                OnboardingProgressBar(
                    currentStep = (incomingIndex ?: currentIndex).toFloat(),
                    totalSteps = viewModel.totalScreens.toFloat(),
                    onBack = { viewModel.goBack() },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            // Screen content area — both screens visible during transitions
            // Button is inside each sliding container so it slides with its screen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Current screen (slides out during transition)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset((currentOffset * screenWidthPx).roundToInt(), 0) }
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        ScreenRouter(
                            screen = currentScreen,
                            viewModel = viewModel
                        )
                    }
                    if (currentScreen.buttonConfig == ButtonConfig.Full) {
                        OnboardingContinueButton(
                            title = currentScreen.buttonTitle,
                            appearance = currentScreen.buttonAppearance,
                            onClick = { viewModel.advance() },
                            enabled = !isAnimating,
                            modifier = Modifier.padding(
                                horizontal = OnboardingTokens.ScreenHorizontalPadding,
                                vertical = 16.dp
                            )
                        )
                    }
                }

                // Incoming screen (slides in during transition)
                if (incomingScreen != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset((incomingOffsetValue * screenWidthPx).roundToInt(), 0) }
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            ScreenRouter(
                                screen = incomingScreen,
                                viewModel = viewModel
                            )
                        }
                        if (incomingScreen.buttonConfig == ButtonConfig.Full) {
                            OnboardingContinueButton(
                                title = incomingScreen.buttonTitle,
                                appearance = incomingScreen.buttonAppearance,
                                onClick = { viewModel.advance() },
                                enabled = false,
                                modifier = Modifier.padding(
                                    horizontal = OnboardingTokens.ScreenHorizontalPadding,
                                    vertical = 16.dp
                                )
                            )
                        }
                    }
                }
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
        is OnboardingScreenType.SuggestedIntention -> SuggestedIntentionScreen(
            onComplete = { viewModel.advance() }
        )
        is OnboardingScreenType.NotificationPermission -> NotificationPermissionScreen(
            onComplete = { viewModel.advance() },
            onEnableClicked = { viewModel.trackClickedEnableNotifications() },
            onMaybeLater = { viewModel.trackMaybeLaterNotifications() },
            onPermissionResult = { granted -> viewModel.trackNotificationPermissionResult(granted) }
        )
        is OnboardingScreenType.SevenDayChallenge -> SevenDayChallengeScreen(
            hoursSaved = viewModel.hoursSavedEstimate,
            onAccepted = { viewModel.advance() }
        )
        is OnboardingScreenType.Paywall -> PaywallScreen(
            onSubscribed = { viewModel.advance() }
        )
        is OnboardingScreenType.WelcomeSubscription -> WelcomeSubscriptionScreen(
            onContinue = { viewModel.advance() }
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
