package com.bepresent.android.ui.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.db.ScheduledSession
import com.bepresent.android.features.schedules.ScheduledSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchedulesViewModel @Inject constructor(
    private val scheduledSessionManager: ScheduledSessionManager
) : ViewModel() {

    val sessions: StateFlow<List<ScheduledSession>> = scheduledSessionManager.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            scheduledSessionManager.seedDefaults()
        }
    }

    fun toggleSchedule(id: String, enabled: Boolean) {
        viewModelScope.launch {
            scheduledSessionManager.toggle(id, enabled)
        }
    }
}
