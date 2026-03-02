package com.bepresent.android.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bepresent.android.data.convex.AuthState
import com.bepresent.android.data.convex.ConvexManager
import com.bepresent.android.data.subscription.SubscriptionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val convexManager: ConvexManager,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    val authState: StateFlow<AuthState> = convexManager.authState

    val isLoggedIn: Boolean
        get() = convexManager.isAuthenticated

    fun login() {
        viewModelScope.launch {
            convexManager.login()
            // After successful login, upsert user in Convex
            if (convexManager.isAuthenticated) {
                try {
                    convexManager.client?.mutation<String>("users:store")
                } catch (_: Exception) {
                    // Non-critical: user store can be retried
                }
                linkSubscription()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            convexManager.logout()
        }
    }

    fun loginFromCache() {
        viewModelScope.launch {
            convexManager.loginFromCache()
            if (convexManager.isAuthenticated) {
                try {
                    convexManager.client?.mutation<String>("users:store")
                } catch (_: Exception) {}
                linkSubscription()
            }
        }
    }

    private suspend fun linkSubscription() {
        try {
            val deviceId = subscriptionManager.getDeviceUuid()
            convexManager.client?.mutation<Unit>(
                "stripe:linkSubscriptionToUser",
                mapOf("deviceId" to deviceId)
            )
        } catch (_: Exception) {
            // Non-critical: subscription linking can be retried
        }
    }
}
