package com.bepresent.android.ui.leaderboard

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

enum class Tier {
    BRONZE, SILVER, GOLD, PLATINUM, DIAMOND;

    val displayName: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

enum class TierMovement { PROMOTED, DEMOTED, STAYED }

data class TieredLeaderboardEntry(
    val username: String,
    val points: Int,
    val expectedEndPoints: Int
)

data class LeaderboardResultsInfo(
    val rank: Int,
    val points: Int,
    val previousTier: Tier,
    val newTier: Tier,
    val tierMovement: TierMovement
)

@Singleton
class TieredLeaderboardManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("leaderboard_prefs", Context.MODE_PRIVATE)
    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // ── Persisted properties ──

    var currentTier: Tier
        get() = try {
            Tier.valueOf(prefs.getString("current_tier", Tier.BRONZE.name)!!)
        } catch (_: Exception) { Tier.BRONZE }
        set(value) { prefs.edit().putString("current_tier", value.name).apply() }

    var leaderboardEndDate: String
        get() = prefs.getString("end_date", "") ?: ""
        set(value) { prefs.edit().putString("end_date", value).apply() }

    var lastFakePointsDate: String
        get() = prefs.getString("last_fake_points_date", "") ?: ""
        set(value) { prefs.edit().putString("last_fake_points_date", value).apply() }

    var introShown: Boolean
        get() = prefs.getBoolean("intro_shown", false)
        set(value) { prefs.edit().putBoolean("intro_shown", value).apply() }

    var leaderboardStartXp: Int
        get() = prefs.getInt("leaderboard_start_xp", 0)
        set(value) { prefs.edit().putInt("leaderboard_start_xp", value).apply() }

    // ── Observable state ──

    private val _showResults = MutableStateFlow(false)
    val showResults: StateFlow<Boolean> = _showResults.asStateFlow()

    private val _resultsInfo = MutableStateFlow<LeaderboardResultsInfo?>(null)
    val resultsInfo: StateFlow<LeaderboardResultsInfo?> = _resultsInfo.asStateFlow()

    // ── Computed properties ──

    val daysLeft: Int
        get() {
            if (leaderboardEndDate.isEmpty()) return 0
            return try {
                val today = LocalDate.now()
                val endDate = LocalDate.parse(leaderboardEndDate, dateFormat)
                ChronoUnit.DAYS.between(today, endDate).toInt().coerceAtLeast(0)
            } catch (_: Exception) { 0 }
        }

    // ── Fake entries CRUD ──

    fun getFakeLeaderboardEntries(): List<TieredLeaderboardEntry> {
        val json = prefs.getString("fake_entries", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TieredLeaderboardEntry(
                    username = obj.getString("username"),
                    points = obj.getInt("points"),
                    expectedEndPoints = obj.getInt("expectedEndPoints")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun setFakeLeaderboardEntries(entries: List<TieredLeaderboardEntry>) {
        val arr = JSONArray()
        entries.forEach { entry ->
            arr.put(JSONObject().apply {
                put("username", entry.username)
                put("points", entry.points)
                put("expectedEndPoints", entry.expectedEndPoints)
            })
        }
        prefs.edit().putString("fake_entries", arr.toString()).apply()
    }

    // ── Create new leaderboard ──

    fun createNewFakeLeaderboard(currentTotalXp: Int) {
        val users = possibleFakeUsernames.shuffled()
        val startingPoints = listOf(3, 3, 3, 5, 5, 5, 5, 8, 8, 8, 10, 10, 10, 13, 13, 15, 20, 25)
        val entries = (0..28).map { i ->
            var expectedEnd = randomEndingPointsValue(currentTier)
            if ((currentTier == Tier.BRONZE || currentTier == Tier.SILVER) && i < 3) {
                expectedEnd = (500..1000).random()
            }
            TieredLeaderboardEntry(
                username = users[i],
                points = startingPoints.random(),
                expectedEndPoints = expectedEnd
            )
        }
        setFakeLeaderboardEntries(entries)
        leaderboardStartXp = currentTotalXp
        leaderboardEndDate = LocalDate.now()
            .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            .format(dateFormat)
    }

    // ── Get ordered entries with user ──

    fun getOrderedLeaderboardEntries(username: String, userPoints: Int): List<TieredLeaderboardEntry> {
        val entries = getFakeLeaderboardEntries().toMutableList()
        entries.add(TieredLeaderboardEntry(username = username, points = userPoints, expectedEndPoints = -1))
        entries.sortByDescending { it.points }
        return entries
    }

    fun getCurrentUserRank(entries: List<TieredLeaderboardEntry>, username: String): Int {
        return 1 + (entries.indexOfFirst { it.username == username })
    }

    // ── Tier thresholds ──

    fun maxPromotionRank(tier: Tier): Int = when (tier) {
        Tier.BRONZE -> 20
        Tier.SILVER -> 15
        Tier.GOLD -> 10
        Tier.PLATINUM -> 10
        Tier.DIAMOND -> -100
    }

    fun minDemotionRank(tier: Tier): Int = when (tier) {
        Tier.BRONZE -> 100
        Tier.SILVER -> 26
        Tier.GOLD -> 26
        Tier.PLATINUM -> 21
        Tier.DIAMOND -> 21
    }

    // ── End leaderboard ──

    fun endLeaderboard(username: String, weeklyXp: Int, totalXp: Int) {
        // Grant remaining fake points up to end date
        if (lastFakePointsDate.isNotEmpty() && leaderboardEndDate.isNotEmpty()) {
            try {
                val lastDate = LocalDate.parse(lastFakePointsDate, dateFormat)
                val endDate = LocalDate.parse(leaderboardEndDate, dateFormat)
                val days = ChronoUnit.DAYS.between(lastDate, endDate).toInt()
                grantFakePointsForNDays(days)
            } catch (_: Exception) {}
        }

        val entries = getOrderedLeaderboardEntries(username, weeklyXp)
        val rank = getCurrentUserRank(entries, username)
        val points = entries.find { it.username == username }?.points ?: 0

        val tier = currentTier
        var newTier = tier
        var movement = TierMovement.STAYED

        if (rank in 1..maxPromotionRank(tier) && tier.ordinal < Tier.DIAMOND.ordinal) {
            newTier = Tier.entries[tier.ordinal + 1]
            movement = TierMovement.PROMOTED
        } else if (rank >= minDemotionRank(tier) && tier.ordinal > Tier.BRONZE.ordinal) {
            newTier = Tier.entries[tier.ordinal - 1]
            movement = TierMovement.DEMOTED
        }

        _resultsInfo.value = LeaderboardResultsInfo(rank, points, tier, newTier, movement)
        _showResults.value = true

        currentTier = newTier
        createNewFakeLeaderboard(totalXp)
    }

    fun dismissResults() {
        _showResults.value = false
    }

    // ── Grant fake user points ──

    fun grantFakeUsersPoints() {
        var numDays = 0

        if (lastFakePointsDate.isNotEmpty()) {
            try {
                val lastDate = LocalDate.parse(lastFakePointsDate, dateFormat)
                val today = LocalDate.now()
                val daysSince = ChronoUnit.DAYS.between(lastDate, today).toInt()

                // Cap to days since Monday (Mon=0 days of fake points, Tue=1, ..., Sun=6)
                val daysSinceMonday = today.dayOfWeek.value - 1
                numDays = minOf(daysSince, daysSinceMonday)
            } catch (_: Exception) {}
        }

        grantFakePointsForNDays(numDays)
        lastFakePointsDate = LocalDate.now().format(dateFormat)
    }

    private fun grantFakePointsForNDays(n: Int) {
        if (n <= 0) return
        val entries = getFakeLeaderboardEntries().toMutableList()
        repeat(n) {
            for (i in entries.indices) {
                val entry = entries[i]
                entries[i] = entry.copy(
                    points = entry.points + randomPoints(entry.expectedEndPoints)
                )
            }
        }
        setFakeLeaderboardEntries(entries)
    }

    private fun randomPoints(expectedEndPoints: Int): Int {
        val avgDaily = expectedEndPoints / 7
        if (avgDaily <= 0) return 0
        val stdDev = avgDaily * 0.3
        val value = (java.util.Random().nextGaussian() * stdDev + avgDaily).toInt()
        return max(0, value)
    }

    // ── Daily update (call on leaderboard screen open) ──

    fun dailyUpdate(username: String, totalXp: Int) {
        if (getFakeLeaderboardEntries().isEmpty()) {
            createNewFakeLeaderboard(totalXp)
        }

        val weeklyXp = (totalXp - leaderboardStartXp).coerceAtLeast(0)

        // Check if leaderboard period has ended
        if (leaderboardEndDate.isNotEmpty()) {
            try {
                val endDate = LocalDate.parse(leaderboardEndDate, dateFormat)
                if (!LocalDate.now().isBefore(endDate)) {
                    endLeaderboard(username, weeklyXp, totalXp)
                }
            } catch (_: Exception) {}
        }

        grantFakeUsersPoints()
    }

    // ── Ending points generation ──

    private fun getEndingPointsMean(tier: Tier): Int = when (tier) {
        Tier.BRONZE -> 50; Tier.SILVER -> 100; Tier.GOLD -> 200
        Tier.PLATINUM -> 300; Tier.DIAMOND -> 400
    }

    private fun getEndingPointsStdDev(tier: Tier): Int = when (tier) {
        Tier.BRONZE -> 50; Tier.SILVER -> 60; Tier.GOLD -> 100
        Tier.PLATINUM -> 125; Tier.DIAMOND -> 150
    }

    private fun getEndingPointsMin(tier: Tier): Int = when (tier) {
        Tier.BRONZE -> 3; Tier.SILVER -> 3; Tier.GOLD -> 8
        Tier.PLATINUM -> 25; Tier.DIAMOND -> 50
    }

    private fun randomEndingPointsValue(tier: Tier): Int {
        val mean = getEndingPointsMean(tier)
        val stdDev = getEndingPointsStdDev(tier)
        val value = (java.util.Random().nextGaussian() * stdDev + mean).toInt()
        return max(value, getEndingPointsMin(tier))
    }
}
