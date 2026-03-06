package com.bepresent.android.ui.onboarding.v2.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.analytics.AnalyticsEvents
import com.bepresent.android.data.analytics.AnalyticsManager
import com.bepresent.android.data.subscription.SubscriptionManager
import com.stripe.android.paymentsheet.PaymentSheetResult
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    private val subscriptionManager: SubscriptionManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        analyticsManager.track(AnalyticsEvents.VIEWED_PAYWALL)
    }

    fun startSubscription() {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val result = subscriptionManager.createSubscription()
                Log.d("PaywallViewModel", "Subscription created, presenting payment sheet")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    clientSecret = result.clientSecret
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("PaywallViewModel", "Failed to create subscription", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong. Please try again."
                )
            }
        }
    }

    fun consumeClientSecret() {
        _uiState.value = _uiState.value.copy(clientSecret = null)
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

    fun skipPaywall() {
        analyticsManager.track(AnalyticsEvents.SKIPPED_PAYWALL)
        viewModelScope.launch {
            subscriptionManager.recordSuccessfulPayment()
            _uiState.value = _uiState.value.copy(subscriptionSuccess = true)
        }
    }

    fun onPaymentSheetError(message: String) {
        _uiState.value = _uiState.value.copy(
            clientSecret = null,
            error = message,
            isLoading = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
