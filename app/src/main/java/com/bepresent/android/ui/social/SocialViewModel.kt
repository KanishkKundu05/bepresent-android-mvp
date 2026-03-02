package com.bepresent.android.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.convex.ConvexManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountabilityPartner(
    val id: String,
    val contactName: String,
    val phoneNumber: String,
    val createdAt: Long
)

data class SocialUiState(
    val partners: List<AccountabilityPartner> = emptyList(),
    val isLoading: Boolean = true,
    val addError: String? = null,
    val isAdding: Boolean = false
)

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val convexManager: ConvexManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    val isAuthenticated: Boolean
        get() = convexManager.isAuthenticated

    init {
        subscribeToPartners()
    }

    private fun subscribeToPartners() {
        viewModelScope.launch {
            if (!convexManager.isAuthenticated) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }
            val client = convexManager.client ?: return@launch
            try {
                client.subscribe<List<Map<String, Any?>>>(
                    "accountabilityPartners:list"
                ).collect { result ->
                    result.onSuccess { list ->
                        _uiState.value = _uiState.value.copy(
                            partners = list.mapNotNull { it.toAccountabilityPartner() },
                            isLoading = false
                        )
                    }
                    result.onFailure {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun addPartner(contactName: String, phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true, addError = null)
            try {
                convexManager.client?.mutation<String>(
                    "accountabilityPartners:add",
                    args = mapOf(
                        "contactName" to contactName,
                        "phoneNumber" to phoneNumber
                    )
                )
                _uiState.value = _uiState.value.copy(isAdding = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAdding = false,
                    addError = e.message ?: "Failed to add partner"
                )
            }
        }
    }

    fun removePartner(partnerId: String) {
        viewModelScope.launch {
            try {
                convexManager.client?.mutation<Unit>(
                    "accountabilityPartners:remove",
                    args = mapOf("partnerId" to partnerId)
                )
            } catch (_: Exception) {}
        }
    }

    fun clearAddError() {
        _uiState.value = _uiState.value.copy(addError = null)
    }

    private fun Map<String, Any?>.toAccountabilityPartner(): AccountabilityPartner? {
        return try {
            AccountabilityPartner(
                id = this["id"] as? String ?: "",
                contactName = this["contactName"] as? String ?: "",
                phoneNumber = this["phoneNumber"] as? String ?: "",
                createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L
            )
        } catch (_: Exception) {
            null
        }
    }
}
