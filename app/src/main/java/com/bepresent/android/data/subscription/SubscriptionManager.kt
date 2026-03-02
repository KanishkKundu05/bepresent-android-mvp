package com.bepresent.android.data.subscription

import com.bepresent.android.BuildConfig
import com.bepresent.android.data.datastore.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class CreateSubscriptionResult(
    val clientSecret: String,
    val customerId: String,
    val subscriptionId: String
)

@Singleton
class SubscriptionManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    private val convexUrl: String
        get() {
            // Convex HTTP actions are on the site URL (replace .cloud with .site)
            val deploymentUrl = BuildConfig.CONVEX_URL
            return deploymentUrl.replace(".cloud.convex.cloud", ".convex.site")
                .replace(".convex.cloud", ".convex.site")
        }

    suspend fun createSubscription(): CreateSubscriptionResult = withContext(Dispatchers.IO) {
        val deviceId = preferencesManager.getOrCreateDeviceUuid()
        val url = URL("$convexUrl/stripe/create-subscription")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val body = JSONObject().apply {
                put("deviceId", deviceId)
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                throw SubscriptionException("Failed to create subscription: $responseCode - $errorBody")
            }

            val responseBody = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(responseBody)

            val result = CreateSubscriptionResult(
                clientSecret = json.getString("clientSecret"),
                customerId = json.getString("customerId"),
                subscriptionId = json.getString("subscriptionId")
            )

            preferencesManager.setStripeCustomerId(result.customerId)
            result
        } finally {
            connection.disconnect()
        }
    }

    suspend fun recordSuccessfulPayment() {
        preferencesManager.setSubscriptionActive(true)
    }

    suspend fun isSubscriptionActive(): Boolean {
        return preferencesManager.getSubscriptionActiveOnce()
    }

    suspend fun getDeviceUuid(): String {
        return preferencesManager.getOrCreateDeviceUuid()
    }
}

class SubscriptionException(message: String) : Exception(message)
