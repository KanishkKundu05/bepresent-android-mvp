package com.bepresent.android.ui.onboarding.v2

import com.bepresent.android.ui.onboarding.v2.animation.ScreenAnimation

/**
 * Represents each screen in the V2 onboarding flow.
 */
sealed class OnboardingScreenType {
    data object Welcome : OnboardingScreenType()
    data object UserWhy : OnboardingScreenType()
    data object UserHow : OnboardingScreenType()
    data object UserWhat : OnboardingScreenType()
    data class Question(
        val title: String,
        val emoji: String,
        val options: List<QuestionOption>,
        val questionType: QuestionType
    ) : OnboardingScreenType()
    data object Loading : OnboardingScreenType()
    data object ShockPage1 : OnboardingScreenType()
    data object ShockPage2 : OnboardingScreenType()
    data object Rating : OnboardingScreenType()
    data object PermissionsSetup : OnboardingScreenType()
    data object SuggestedIntention : OnboardingScreenType()
    data object NotificationPermission : OnboardingScreenType()
    data object SevenDayChallenge : OnboardingScreenType()
    data object Paywall : OnboardingScreenType()
    data object WelcomeSubscription : OnboardingScreenType()
    data class PostPaywallMessage(val message: String) : OnboardingScreenType()
    data object ChooseUsername : OnboardingScreenType()
    data object SelectApps : OnboardingScreenType()
    data object Acquisition : OnboardingScreenType()
}

data class QuestionOption(
    val title: String,
    val emoji: String? = null
)

enum class QuestionType {
    Age,
    ScreenTime
}

/** Whether to show the top progress bar on this screen. */
val OnboardingScreenType.showProgressBar: Boolean
    get() = when (this) {
        is OnboardingScreenType.Welcome,
        is OnboardingScreenType.UserWhy,
        is OnboardingScreenType.UserHow,
        is OnboardingScreenType.UserWhat,
        is OnboardingScreenType.Loading,
        is OnboardingScreenType.ShockPage1,
        is OnboardingScreenType.ShockPage2,
        is OnboardingScreenType.Rating,
        is OnboardingScreenType.PermissionsSetup,
        is OnboardingScreenType.SuggestedIntention,
        is OnboardingScreenType.NotificationPermission,
        is OnboardingScreenType.SevenDayChallenge,
        is OnboardingScreenType.WelcomeSubscription,
        is OnboardingScreenType.PostPaywallMessage,
        is OnboardingScreenType.Paywall -> false
        else -> true
    }

/** Whether the screen has a continue button at the bottom. */
enum class ButtonConfig {
    Full,          // Full-width continue button
    Hidden,        // No button (screen handles its own navigation)
    None           // No button at all
}

val OnboardingScreenType.buttonConfig: ButtonConfig
    get() = when (this) {
        is OnboardingScreenType.Welcome -> ButtonConfig.Full
        is OnboardingScreenType.UserWhy -> ButtonConfig.Full
        is OnboardingScreenType.UserHow -> ButtonConfig.Full
        is OnboardingScreenType.UserWhat -> ButtonConfig.Hidden
        is OnboardingScreenType.Question -> ButtonConfig.Hidden
        is OnboardingScreenType.Loading -> ButtonConfig.Hidden
        is OnboardingScreenType.ShockPage1 -> ButtonConfig.Hidden
        is OnboardingScreenType.ShockPage2 -> ButtonConfig.Full
        is OnboardingScreenType.Rating -> ButtonConfig.Hidden
        is OnboardingScreenType.PermissionsSetup -> ButtonConfig.Hidden
        is OnboardingScreenType.SuggestedIntention -> ButtonConfig.Hidden
        is OnboardingScreenType.NotificationPermission -> ButtonConfig.Hidden
        is OnboardingScreenType.SevenDayChallenge -> ButtonConfig.Hidden
        is OnboardingScreenType.Paywall -> ButtonConfig.Hidden
        is OnboardingScreenType.WelcomeSubscription -> ButtonConfig.Hidden
        is OnboardingScreenType.PostPaywallMessage -> ButtonConfig.Full
        is OnboardingScreenType.ChooseUsername -> ButtonConfig.Hidden
        is OnboardingScreenType.SelectApps -> ButtonConfig.Hidden
        is OnboardingScreenType.Acquisition -> ButtonConfig.Hidden
    }

/** Button title for screens that show a button. */
val OnboardingScreenType.buttonTitle: String
    get() = when (this) {
        is OnboardingScreenType.Welcome -> "Get Started"
        is OnboardingScreenType.UserWhy -> "Continue"
        is OnboardingScreenType.UserHow -> "Continue"
        is OnboardingScreenType.ShockPage2 -> "Get those years back!"
        is OnboardingScreenType.NotificationPermission -> "Enable Notifications"
        is OnboardingScreenType.PostPaywallMessage -> "Continue"
        else -> "Continue"
    }

/** Background gradient type for each screen. */
enum class GradientType {
    Blue,
    Orange,
    White
}

val OnboardingScreenType.gradientType: GradientType
    get() = when (this) {
        is OnboardingScreenType.Welcome -> GradientType.Blue
        is OnboardingScreenType.UserWhy -> GradientType.Blue
        is OnboardingScreenType.UserHow -> GradientType.Blue
        is OnboardingScreenType.UserWhat -> GradientType.Blue
        is OnboardingScreenType.Question -> GradientType.White
        is OnboardingScreenType.Loading -> GradientType.Blue
        is OnboardingScreenType.ShockPage1 -> GradientType.White
        is OnboardingScreenType.ShockPage2 -> GradientType.White
        is OnboardingScreenType.Rating -> GradientType.Blue
        is OnboardingScreenType.PermissionsSetup -> GradientType.White
        is OnboardingScreenType.SuggestedIntention -> GradientType.White
        is OnboardingScreenType.NotificationPermission -> GradientType.White
        is OnboardingScreenType.SevenDayChallenge -> GradientType.White
        is OnboardingScreenType.Paywall -> GradientType.White
        is OnboardingScreenType.WelcomeSubscription -> GradientType.Blue
        is OnboardingScreenType.PostPaywallMessage -> GradientType.Blue
        is OnboardingScreenType.ChooseUsername -> GradientType.White
        is OnboardingScreenType.SelectApps -> GradientType.White
        is OnboardingScreenType.Acquisition -> GradientType.White
    }

/** Animation style for entering/exiting each screen. */
val OnboardingScreenType.introAnimation: ScreenAnimation
    get() = when (this) {
        is OnboardingScreenType.Question,
        is OnboardingScreenType.Acquisition -> ScreenAnimation.Drawer
        else -> ScreenAnimation.Intro
    }

val OnboardingScreenType.outroAnimation: ScreenAnimation
    get() = when (this) {
        is OnboardingScreenType.Question,
        is OnboardingScreenType.Acquisition -> ScreenAnimation.Drawer
        else -> ScreenAnimation.Intro
    }

/** Build the default onboarding screen list. */
fun buildOnboardingScreens(): List<OnboardingScreenType> = listOf(
    OnboardingScreenType.Welcome,
    OnboardingScreenType.UserWhy,
    OnboardingScreenType.UserHow,
    OnboardingScreenType.UserWhat,
    OnboardingScreenType.Question(
        title = "How old are you?",
        emoji = "\uD83D\uDC64",
        options = listOf(
            QuestionOption("Under 18"),
            QuestionOption("18-24"),
            QuestionOption("25-29"),
            QuestionOption("30-34"),
            QuestionOption("35-44"),
            QuestionOption("45-54"),
            QuestionOption("55 and over")
        ),
        questionType = QuestionType.Age
    ),
    OnboardingScreenType.Question(
        title = "How much screen time do you average per day?",
        emoji = "\uD83D\uDCF1",
        options = listOf(
            QuestionOption("1-2 hours", "\u23F0"),
            QuestionOption("2-3 hours", "\uD83D\uDCCA"),
            QuestionOption("3-4 hours", "\uD83D\uDCCA"),
            QuestionOption("4-5 hours", "\uD83D\uDCCA"),
            QuestionOption("5-6 hours", "\uD83D\uDCCA"),
            QuestionOption("6-7 hours", "\uD83D\uDCCA"),
            QuestionOption("7-8 hours", "\uD83D\uDE31"),
            QuestionOption("Over 8 hours", "\uD83D\uDCA3")
        ),
        questionType = QuestionType.ScreenTime
    ),
    OnboardingScreenType.Loading,
    OnboardingScreenType.ShockPage1,
    OnboardingScreenType.ShockPage2,
    OnboardingScreenType.Rating,
    OnboardingScreenType.PermissionsSetup,
    OnboardingScreenType.SuggestedIntention,
    OnboardingScreenType.NotificationPermission,
    OnboardingScreenType.SevenDayChallenge,
    OnboardingScreenType.Paywall,
    OnboardingScreenType.WelcomeSubscription,
    OnboardingScreenType.PostPaywallMessage(
        message = "Let's get set up so BePresent can help you stop scrolling and start living."
    ),
    OnboardingScreenType.ChooseUsername,
    OnboardingScreenType.SelectApps,
    OnboardingScreenType.Acquisition
)
