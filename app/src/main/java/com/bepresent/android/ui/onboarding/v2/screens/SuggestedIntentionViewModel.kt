package com.bepresent.android.ui.onboarding.v2.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.usage.AppUsageInfo
import com.bepresent.android.data.usage.UsageStatsRepository
import com.bepresent.android.features.intentions.IntentionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SuggestedIntentionViewModel @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val intentionManager: IntentionManager
) : ViewModel() {

    private val _topApp = MutableStateFlow<AppUsageInfo?>(null)
    val topApp: StateFlow<AppUsageInfo?> = _topApp.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _topApp.value = try {
                usageStatsRepository.getTopDistractingApp()
            } catch (_: Exception) {
                null
            }
            _isLoaded.value = true
        }
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
