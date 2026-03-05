package com.bepresent.android.ui.onboarding.v2.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.usage.UsageStatsRepository
import com.bepresent.android.features.intentions.IntentionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Insight about the user's most distracting social media app,
 * derived from yesterday's usage data.
 */
data class DistractingAppInsight(
    val packageName: String,
    val openCountYesterday: Int,
    val screenTimeMinutesYesterday: Long,
    val suggestedOpens: Int,
    val suggestedMinutesPerOpen: Int
)

/**
 * Thresholds for qualifying a social media app as "distracting enough" to highlight.
 */
private data class AppThreshold(
    val packageName: String,
    val minOpens: Int,
    val minMinutes: Long
)

private val APP_THRESHOLDS = listOf(
    AppThreshold("com.instagram.android", 10, 60),
    AppThreshold("com.zhiliaoapp.musically", 10, 60),       // TikTok
    AppThreshold("com.google.android.youtube", 10, 60),
    AppThreshold("com.twitter.android", 10, 30),            // X/Twitter
    AppThreshold("com.snapchat.android", 10, 30),
    AppThreshold("com.facebook.katana", 10, 60),
    AppThreshold("com.reddit.frontpage", 10, 30),
    AppThreshold("com.whatsapp", 15, 60)
)

private val THRESHOLD_MAP = APP_THRESHOLDS.associateBy { it.packageName }

@HiltViewModel
class SuggestedIntentionViewModel @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val intentionManager: IntentionManager
) : ViewModel() {

    private val _insight = MutableStateFlow<DistractingAppInsight?>(null)
    val insight: StateFlow<DistractingAppInsight?> = _insight.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _insight.value = try {
                computeInsight()
            } catch (_: Exception) {
                null
            }
            _isLoaded.value = true
        }
    }

    private fun computeInsight(): DistractingAppInsight? {
        // Get last 2 days of usage so we have yesterday's data
        val dailyUsage = usageStatsRepository.getDailyAppUsage(days = 2)

        // Yesterday's date string
        val yesterday = java.time.LocalDate.now().minusDays(1)
            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)

        // Filter to yesterday + known social media apps that meet thresholds
        val qualifyingApps = dailyUsage
            .filter { it.date == yesterday && it.packageName in THRESHOLD_MAP }
            .filter { usage ->
                val threshold = THRESHOLD_MAP[usage.packageName]!!
                val minutes = usage.totalTimeMs / (1000 * 60)
                usage.openCount >= threshold.minOpens && minutes >= threshold.minMinutes
            }

        if (qualifyingApps.isEmpty()) {
            // Fallback: try today's data with same thresholds
            val today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            val todayApps = dailyUsage
                .filter { it.date == today && it.packageName in THRESHOLD_MAP }
                .filter { usage ->
                    val threshold = THRESHOLD_MAP[usage.packageName]!!
                    val minutes = usage.totalTimeMs / (1000 * 60)
                    usage.openCount >= threshold.minOpens && minutes >= threshold.minMinutes
                }

            if (todayApps.isEmpty()) {
                // Final fallback: use weekly top distracting app (old behavior)
                val weeklyTop = usageStatsRepository.getTopDistractingApp() ?: return null
                return DistractingAppInsight(
                    packageName = weeklyTop.packageName,
                    openCountYesterday = 0,
                    screenTimeMinutesYesterday = weeklyTop.totalTimeMs / (1000 * 60),
                    suggestedOpens = 10,
                    suggestedMinutesPerOpen = 5
                )
            }

            return pickBestApp(todayApps.map { Triple(it.packageName, it.openCount, it.totalTimeMs / (1000 * 60)) })
        }

        return pickBestApp(qualifyingApps.map { Triple(it.packageName, it.openCount, it.totalTimeMs / (1000 * 60)) })
    }

    /**
     * Pick the "worst" app by a combined score of opens + normalized time,
     * then compute smart intention defaults.
     */
    private fun pickBestApp(apps: List<Triple<String, Int, Long>>): DistractingAppInsight {
        // Score = opens + (minutes / 10) — weight opens more heavily
        val best = apps.maxBy { (_, opens, minutes) -> opens + (minutes / 10).toInt() }

        val (pkg, opens, minutes) = best

        // Smart defaults: suggest roughly 40-50% of their current opens
        val suggestedOpens = when {
            opens >= 40 -> 15
            opens >= 25 -> 10
            opens >= 15 -> 8
            else -> 5
        }
        // Time per open: 5 min is the sweet spot for most apps
        val suggestedMinutes = 5

        return DistractingAppInsight(
            packageName = pkg,
            openCountYesterday = opens,
            screenTimeMinutesYesterday = minutes,
            suggestedOpens = suggestedOpens,
            suggestedMinutesPerOpen = suggestedMinutes
        )
    }

    fun createIntention(
        packageName: String,
        appName: String,
        allowedOpensPerDay: Int,
        timePerOpenMinutes: Int
    ) {
        viewModelScope.launch {
            intentionManager.create(packageName, appName, allowedOpensPerDay, timePerOpenMinutes)
        }
    }
}
