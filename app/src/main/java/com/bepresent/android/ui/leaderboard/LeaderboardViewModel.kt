package com.bepresent.android.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val currentTier: Tier = Tier.BRONZE,
    val daysLeft: Int = 0,
    val entries: List<TieredLeaderboardEntry> = emptyList(),
    val username: String = "",
    val userRank: Int = 0,
    val maxPromotionRank: Int = 0,
    val minDemotionRank: Int = 100,
    val showResults: Boolean = false,
    val resultsInfo: LeaderboardResultsInfo? = null,
    val showIntro: Boolean = false
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val manager: TieredLeaderboardManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val username = preferencesManager.getOnboardingV2UsernameOnce().ifEmpty { "You" }
            val totalXp = preferencesManager.totalXp.first()

            // Run daily update (may create/end leaderboard, grant fake points)
            manager.dailyUpdate(username, totalXp)

            // Compute weekly XP after dailyUpdate (startXp may have been reset)
            val weeklyXp = (totalXp - manager.leaderboardStartXp).coerceAtLeast(0)

            val entries = manager.getOrderedLeaderboardEntries(username, weeklyXp)
            val userRank = manager.getCurrentUserRank(entries, username)

            _uiState.value = LeaderboardUiState(
                currentTier = manager.currentTier,
                daysLeft = manager.daysLeft,
                entries = entries,
                username = username,
                userRank = userRank,
                maxPromotionRank = manager.maxPromotionRank(manager.currentTier),
                minDemotionRank = manager.minDemotionRank(manager.currentTier),
                showIntro = !manager.introShown,
                showResults = manager.showResults.value,
                resultsInfo = manager.resultsInfo.value
            )
        }

        // Keep results state in sync
        viewModelScope.launch {
            manager.showResults.collect { show ->
                _uiState.value = _uiState.value.copy(showResults = show)
            }
        }
        viewModelScope.launch {
            manager.resultsInfo.collect { info ->
                _uiState.value = _uiState.value.copy(resultsInfo = info)
            }
        }
    }

    fun dismissResults() {
        manager.dismissResults()
        // Refresh entries after results dismissed
        viewModelScope.launch {
            val username = _uiState.value.username
            val totalXp = preferencesManager.totalXp.first()
            val weeklyXp = (totalXp - manager.leaderboardStartXp).coerceAtLeast(0)
            val entries = manager.getOrderedLeaderboardEntries(username, weeklyXp)
            val userRank = manager.getCurrentUserRank(entries, username)
            _uiState.value = _uiState.value.copy(
                currentTier = manager.currentTier,
                daysLeft = manager.daysLeft,
                entries = entries,
                userRank = userRank,
                maxPromotionRank = manager.maxPromotionRank(manager.currentTier),
                minDemotionRank = manager.minDemotionRank(manager.currentTier)
            )
        }
    }

    fun markIntroShown() {
        manager.introShown = true
        _uiState.value = _uiState.value.copy(showIntro = false)
    }
}
