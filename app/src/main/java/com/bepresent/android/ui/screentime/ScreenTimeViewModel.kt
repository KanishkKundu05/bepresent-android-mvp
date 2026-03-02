package com.bepresent.android.ui.screentime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.usage.AppUsageInfo
import com.bepresent.android.data.usage.UsageStatsRepository
import com.bepresent.android.permissions.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScreenTimeUiState(
    val totalScreenTimeMs: Long = 0L,
    val perAppUsage: List<AppUsageInfo> = emptyList(),
    val hasPermission: Boolean = true
)

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenTimeUiState())
    val uiState: StateFlow<ScreenTimeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(30_000)
            }
        }
    }

    fun refresh() {
        val hasPermission = permissionManager.hasUsageStatsPermission()
        if (!hasPermission) {
            _uiState.value = ScreenTimeUiState(hasPermission = false)
            return
        }
        viewModelScope.launch {
            try {
                val total = usageStatsRepository.getTotalScreenTimeToday()
                val perApp = usageStatsRepository.getPerAppScreenTime()
                _uiState.value = ScreenTimeUiState(
                    totalScreenTimeMs = total,
                    perAppUsage = perApp,
                    hasPermission = true
                )
            } catch (_: Exception) {
                // Permission might have been revoked
            }
        }
    }
}
