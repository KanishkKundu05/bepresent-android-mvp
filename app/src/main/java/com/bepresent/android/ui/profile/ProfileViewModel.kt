package com.bepresent.android.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.convex.AuthState
import com.bepresent.android.data.convex.ConvexManager
import com.bepresent.android.data.datastore.PreferencesManager
import com.bepresent.android.data.db.SyncQueueDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfile(
    val displayName: String,
    val friendCode: String?
)

data class PartnerInfo(
    val partnershipId: String,
    val otherUserId: String,
    val otherDisplayName: String,
    val status: String,
    val isIncoming: Boolean
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val convexManager: ConvexManager,
    private val preferencesManager: PreferencesManager,
    syncQueueDao: SyncQueueDao
) : ViewModel() {

    val authState: StateFlow<AuthState> = convexManager.authState

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    private val _partners = MutableStateFlow<List<PartnerInfo>>(emptyList())
    val partners: StateFlow<List<PartnerInfo>> = _partners

    val pendingSyncCount: StateFlow<Int> = syncQueueDao.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val intentionCountdownEnabled: StateFlow<Boolean> = preferencesManager.intentionCountdownEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setIntentionCountdownEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setIntentionCountdownEnabled(enabled)
        }
    }

    init {
        loadProfile()
        loadPartners()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            if (!convexManager.isAuthenticated) return@launch
            val client = convexManager.client ?: return@launch
            try {
                client.subscribe<Map<String, Any?>>("users:getMe")
                    .collect { result ->
                        result.onSuccess { data ->
                            if (data != null) {
                                _profile.value = UserProfile(
                                    displayName = data["displayName"] as? String ?: "",
                                    friendCode = data["friendCode"] as? String
                                )
                            }
                        }
                    }
            } catch (_: Exception) {}
        }
    }

    private fun loadPartners() {
        viewModelScope.launch {
            if (!convexManager.isAuthenticated) return@launch
            val client = convexManager.client ?: return@launch
            try {
                client.subscribe<List<Map<String, Any?>>>("partners:getMyPartners")
                    .collect { result ->
                        result.onSuccess { list ->
                            _partners.value = list.map { m ->
                                PartnerInfo(
                                    partnershipId = m["partnershipId"] as? String ?: "",
                                    otherUserId = m["otherUserId"] as? String ?: "",
                                    otherDisplayName = m["otherDisplayName"] as? String ?: "Unknown",
                                    status = m["status"] as? String ?: "",
                                    isIncoming = m["isIncoming"] as? Boolean ?: false
                                )
                            }
                        }
                    }
            } catch (_: Exception) {}
        }
    }

    fun login() {
        viewModelScope.launch {
            convexManager.login()
            if (convexManager.isAuthenticated) {
                try {
                    convexManager.client?.mutation<String>("users:store")
                } catch (_: Exception) {}
                loadProfile()
                loadPartners()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            convexManager.logout()
            _profile.value = null
            _partners.value = emptyList()
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            try {
                convexManager.client?.mutation<Unit>(
                    "users:updateDisplayName",
                    args = mapOf("displayName" to name)
                )
            } catch (_: Exception) {}
        }
    }

    fun respondToRequest(partnershipId: String, accept: Boolean) {
        viewModelScope.launch {
            try {
                convexManager.client?.mutation<Unit>(
                    "partners:respondToRequest",
                    args = mapOf(
                        "partnershipId" to partnershipId,
                        "accept" to accept
                    )
                )
            } catch (_: Exception) {}
        }
    }
}
