package com.bepresent.android.data.analytics

/**
 * Data classes for analytics event properties.
 * Property keys use snake_case to match iOS CodingKeys convention.
 */

// ── Application Lifecycle ──

data class ApplicationUpdatedProperties(
    val previous_version: String,
    val version: String
)

data class ApplicationInstalledProperties(
    val version: String
)

// ── Sessions ──

data class StartedSessionProperties(
    val session_id: String,
    val goal_duration_minutes: Int,
    val beast_mode: Boolean,
    val session_name: String
)

data class EndedSessionProperties(
    val session_id: String,
    val goal_duration_minutes: Int,
    val beast_mode: Boolean,
    val session_name: String,
    val outcome: String,  // "completed", "gave_up", "canceled"
    val earned_xp: Int,
    val earned_coins: Int,
    val duration_seconds: Long
)

// ── App Intentions ──

data class SetAppIntentionProperties(
    val package_name: String,
    val app_name: String,
    val opens: Int,
    val min_per_open: Int,
    val from: String  // "onboarding" or "home"
)

data class ModifyAppIntentionProperties(
    val package_name: String,
    val app_name: String,
    val old_opens: Int,
    val new_opens: Int,
    val old_min_per_open: Int,
    val new_min_per_open: Int
)

data class RemoveAppIntentionProperties(
    val package_name: String,
    val app_name: String,
    val opens: Int,
    val min_per_open: Int
)

data class IntentionTokensUpdatedProperties(
    val intentions_count: Int
)

// ── Goals ──

data class GoalProperties(
    val hours: Int,
    val minutes: Int,
    val raised: Boolean,
    val existing_user: Boolean
)

// ── Onboarding ──

data class OnboardingQuestionProperties(
    val question: String,
    val answer: String
)

data class OnboardingScreenProperties(
    val screen_name: String
)

data class AgeProperties(
    val age: String
)

data class ScreenTimeEstimateProperties(
    val estimate: String
)

data class AcquisitionSelectionProperties(
    val selection: String,
    val other_text: String
)

data class NotificationPermissionProperties(
    val granted: Boolean
)

// ── Leaderboard ──

data class LeaderboardProperties(
    val name: String,
    val id: String? = null
)

// ── Review ──

data class ReviewPromptProperties(
    val viewed_from: String
)

// ── Rewards ──

data class RewardProperties(
    val reward_id: String,
    val unlocked: Boolean? = null
)

// ── Subscriptions ──

data class SubscriptionPurchaseProperties(
    val product_id: String,
    val viewed_from: String
)

data class RedemptionErrorProperties(
    val code: String,
    val reason: String
)

// ── Categories ──

data class ExcludedCategoriesProperties(
    val excluded: Int
)

// ── Streak ──

data class ThresholdProperties(
    val hours: Int,
    val minutes: Int
)

// ── Error Tracking ──

data class ErrorMessageProperties(
    val error_message: String
)

// ── Notifications ──

data class AwarenessNotificationProperties(
    val type: String  // "Warning", "Treshold", "Goal Warning", "Goal Reached"
)

// ── Help Center ──

data class HelpCenterProperties(
    val origin: String  // "Profile" or "Home"
)
