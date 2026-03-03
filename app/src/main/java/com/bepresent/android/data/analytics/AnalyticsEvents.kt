package com.bepresent.android.data.analytics

/**
 * All analytics event name constants.
 * Event names use "Capitalized With Spaces" convention matching the iOS app.
 */
object AnalyticsEvents {

    // ── Application Lifecycle ──
    const val APPLICATION_INSTALLED = "Application Installed"
    const val APPLICATION_UPDATED = "Application Updated"
    const val APPLICATION_FOREGROUNDED = "Application Foregrounded"
    const val APPLICATION_BACKGROUNDED = "Application Backgrounded"

    // ── User Auth ──
    const val ENTERED_USERNAME = "Entered Username"
    const val ERROR_ON_SIGN_IN = "Error On Sign In"
    const val LOGGED_OUT = "Logged Out"
    const val DELETED_ACCOUNT = "Deleted Account"

    // ── Goal Management ──
    const val UPDATED_GOAL = "Updated Goal"
    const val INITIALIZED_GOAL = "Initialized Goal"
    const val TRIED_GOAL_UPDATE_DURING_COMMITMENT = "Tried Goal Update During Commitment"

    // ── Present Sessions ──
    const val STARTED_PRESENT_SESSION = "Started Present Session"
    const val ENDED_PRESENT_SESSION = "Ended Present Session"
    const val MODIFIED_SCHEDULED_SESSION = "Modified Scheduled Session"
    const val REMOVED_SCHEDULED_SESSION = "Removed Scheduled Session"
    const val CANCELED_SESSION_BREAK = "Canceled Session Break"
    const val CLICKED_DAILY_QUEST_PRESENT_SESSION = "Clicked Daily Quest Present Session"
    const val CLICKED_DAILY_REVIEW = "Clicked Daily Review"
    const val CLICKED_TIP_OF_THE_DAY = "Clicked Tip of the Day"

    // ── App Intentions ──
    const val SET_APP_INTENTION = "Set App Intention"
    const val MODIFIED_APP_INTENTION = "Modified App Intention"
    const val REMOVED_APP_INTENTION = "Removed App Intention"
    const val INTENTION_TOKENS_UPDATED = "Intention Tokens Updated"
    const val HAS_APP_INTENTION = "Has App Intention"
    const val FIRST_APP_INTENTION_OPEN = "First App Intention Open"
    const val CLOSED_LIMITED_APP = "Closed Limited App"

    // ── Navigation & Tabs ──
    const val CLICKED_HOME = "Clicked Home"
    const val CLICKED_SCHEDULES = "Clicked Schedules"
    const val CLICKED_LEADERBOARD = "Clicked Leaderboard"
    const val CLICKED_SCREEN_TIME = "Clicked Screen Time"
    const val CLICKED_SOCIAL = "Clicked Social"
    const val CLICKED_PROFILE = "Clicked Profile"
    const val OPENED_HELP_CENTER = "Opened Help Center"
    const val CLICKED_INTO_GROUP = "Clicked Into Group"

    // ── Notifications ──
    const val OPENED_ACCOUNTABILITY_PARTNER_NOTIFICATION = "Opened Accountability Partner Notification"
    const val OPENED_AWARENESS_NOTIFICATION = "Opened Awareness Notification"
    const val OPENED_DAILY_REPORT_NOTIFICATION = "Opened Daily Report Notification"
    const val OPENED_MORNING_SESSION_NOTIFICATION = "Opened Morning Session Notification"

    // ── Rewards ──
    const val CLICKED_REWARD = "Clicked Reward"
    const val UNLOCKED_REWARD = "Unlocked Reward"
    const val OPENED_OFFER_LINK = "Opened Offer Link"

    // ── Review ──
    const val LIKED_PRESENT = "Liked Present"
    const val DISLIKED_PRESENT = "Disliked Present"

    // ── Leaderboard ──
    const val CREATED_GROUP = "Created Group"
    const val CLICKED_LEADERBOARD_INVITE = "Clicked Leaderboard Invite"
    const val SHARED_LEADERBOARD_INVITE = "Shared Leaderboard Invite"
    const val JOINED_LEADERBOARD = "Joined Leaderboard"

    // ── Accountability Partners ──
    const val ADDED_ACCOUNTABILITY_PARTNERS = "Added Accountability Partners"

    // ── Subscriptions & Paywall ──
    const val VIEWED_PAYWALL = "Viewed Paywall"
    const val SKIPPED_PAYWALL = "Skipped Paywall"
    const val STARTED_FREE_TRIAL = "Started Free Trial"
    const val PURCHASED_SUBSCRIPTION = "Purchased Subscription"
    const val TRANSACTION_COMPLETE = "Transaction Complete"
    const val TAPPED_CONTACT_SUPPORT = "Tapped Contact Support"
    const val STRIPE_REDEMPTION_FAILED = "Stripe Redemption Failed"
    const val STRIPE_REDEMPTION_EXPIRED_CODE = "Stripe Redemption Expired Code"
    const val STRIPE_REDEMPTION_INVALID_CODE = "Stripe Redemption Invalid Code"
    const val STRIPE_REDEMPTION_EXPIRED_SUBSCRIPTION = "Stripe Redemption Expired Subscription"

    // ── Onboarding ──
    const val COMPLETED_ONBOARDING = "Completed Onboarding"
    const val ANSWERED_ONBOARDING_QUESTION = "Answered Onboarding Question"
    const val SET_AGE = "Set Age"
    const val SET_SCREEN_TIME_ESTIMATE = "Set Screen Time Estimate"
    const val CLICKED_ENABLE_NOTIFICATIONS = "Clicked Enable Notifications"
    const val TAPPED_MAYBE_LATER_NOTIFICATIONS = "Tapped Maybe Later Notifications"
    const val NOTIFICATION_PERMISSION_RESULT = "Notification Permission Result"
    const val ANSWERED_ACQUISITION_QUESTION = "Answered Acquisition Question"
    const val ASSIGNED_ONBOARDING_EXPERIMENT_GROUP = "Assigned Onboarding Experiment Group"

    // ── Onboarding Screen Views ──
    const val VIEWED_ONBOARDING_SCREEN = "Viewed Onboarding Screen"

    // ── Streak & Lives ──
    const val DISMISSED_STREAK_FREEZE_SHEET = "Dismissed Streak Freeze Sheet"
    const val TAPPED_BREAK_STREAK = "Tapped Break Streak"
    const val TAPPED_USE_STREAK_FREEZE = "Tapped Use Streak Freeze"
    const val THRESHOLD_REACHED_FOR_DAY = "Threshold Reached For Day"

    // ── Categories ──
    const val UPDATED_EXCLUDED_CATEGORIES = "Updated Excluded Categories"
    const val GAVE_SCREEN_TIME_ACCESS = "Gave Screen Time Access"

    // ── Shield ──
    const val PRESSED_SHIELD_BUTTON = "Pressed Shield Button"

    // ── Experiments ──
    const val EXPERIMENT_STARTED = "\$experiment_started"
}
