package com.bepresent.android.ui.onboarding.v2.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.subscription.SubscriptionManager
import com.stripe.android.paymentsheet.PaymentSheetResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallUiState(
    val isLoading: Boolean = false,
    val clientSecret: String? = null,
    val error: String? = null,
    val subscriptionSuccess: Boolean = false
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    fun startSubscription() {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = subscriptionManager.createSubscription()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    clientSecret = result.clientSecret
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong. Please try again."
                )
            }
        }
    }

    fun handlePaymentResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                viewModelScope.launch {
                    subscriptionManager.recordSuccessfulPayment()
                    _uiState.value = _uiState.value.copy(subscriptionSuccess = true)
                }
            }
            is PaymentSheetResult.Canceled -> {
                // User dismissed — no error, just reset
                _uiState.value = _uiState.value.copy(clientSecret = null)
            }
            is PaymentSheetResult.Failed -> {
                _uiState.value = _uiState.value.copy(
                    clientSecret = null,
                    error = result.error.localizedMessage ?: "Payment failed. Please try again."
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
