package com.bepresent.android.data.analytics

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.bepresent.android.data.datastore.PreferencesManager
import com.bepresent.android.debug.RuntimeLog
import com.mixpanel.android.mpmetrics.MixpanelAPI
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val mixpanel: MixpanelAPI by lazy {
        MixpanelAPI.getInstance(context, MIXPANEL_TOKEN, true)
    }

    private val prefs by lazy {
        context.getSharedPreferences(ANALYTICS_PREFS, Context.MODE_PRIVATE)
    }

    fun initialize() {
        // Force lazy init
        mixpanel.toString()
        RuntimeLog.d(TAG, "Mixpanel initialized")
    }

    /**
     * Identify user on Mixpanel with profile properties.
     */
    fun identify(
        userId: String,
        username: String = "",
        email: String = "",
        firstName: String = "",
        lastName: String = "",
        acquisitionSource: String = "",
        ageBracket: String = "",
        stripeEmail: String? = null,
        stripeCustomerId: String? = null
    ) {
        val fullName = if (lastName.isEmpty()) firstName else "$firstName $lastName"
        val appVersion = getAppVersion()

        mixpanel.identify(userId, true)

        val props = JSONObject().apply {
            put("\$username", username)
            put("\$email", email)
            put("\$first_name", firstName)
            put("\$last_name", lastName)
            put("\$name", fullName)
            put("Locale", Locale.getDefault().toString())
            put("Acquisition Source", acquisitionSource)
            put("App Version", appVersion)
            put("Age Bracket", ageBracket)
            stripeEmail?.let { put("Stripe Email", it) }
            stripeCustomerId?.let { put("Stripe Customer Id", it) }
        }

        mixpanel.people.set(props)
        RuntimeLog.d(TAG, "identify: userId=$userId")
    }

    /**
     * Track an event with optional properties.
     */
    fun track(eventName: String, properties: Map<String, Any>? = null) {
        val jsonProps = properties?.let { map ->
            JSONObject().apply {
                map.forEach { (key, value) -> put(key, value) }
            }
        }

        if (jsonProps != null) {
            mixpanel.track(eventName, jsonProps)
        } else {
            mixpanel.track(eventName)
        }

        // Auto-update profile properties for certain events
        updateProfileForEvent(eventName)

        RuntimeLog.d(TAG, "track: $eventName${properties?.let { " props=$it" } ?: ""}")
    }

    /**
     * Reset Mixpanel identity (on logout).
     */
    fun reset() {
        mixpanel.reset()
        RuntimeLog.d(TAG, "reset")
    }

    /**
     * Detect whether this is a new install or an app update, and track accordingly.
     */
    fun detectAppInstallAndUpdate() {
        val hasLaunchedKey = "hasLaunchedBefore"
        val versionKey = "appVersion"

        val currentVersion = getAppVersion()
        val previousVersion = prefs.getString(versionKey, null)

        if (!prefs.getBoolean(hasLaunchedKey, false)) {
            // First launch ever
            // Check if app was installed more than 24h ago (existing install updating to tracked version)
            val installTime = try {
                context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
            } catch (_: PackageManager.NameNotFoundException) {
                System.currentTimeMillis()
            }
            val isExistingInstallation = (System.currentTimeMillis() - installTime) > 86_400_000L

            if (isExistingInstallation) {
                track(
                    AnalyticsEvents.APPLICATION_UPDATED,
                    mapOf("previous_version" to "unknown", "version" to currentVersion)
                )
            } else {
                track(
                    AnalyticsEvents.APPLICATION_INSTALLED,
                    mapOf("version" to currentVersion)
                )
            }

            prefs.edit()
                .putBoolean(hasLaunchedKey, true)
                .putString(versionKey, currentVersion)
                .apply()
        } else if (previousVersion != null && previousVersion != currentVersion) {
            // App updated
            track(
                AnalyticsEvents.APPLICATION_UPDATED,
                mapOf("previous_version" to previousVersion, "version" to currentVersion)
            )
            prefs.edit().putString(versionKey, currentVersion).apply()
        }
    }

    /**
     * Flush pending events to Mixpanel.
     */
    fun flush() {
        mixpanel.flush()
    }

    // ── Private ──

    private fun updateProfileForEvent(eventName: String) {
        val now = Date()
        val isoDate = isoFormatter.format(now)

        when (eventName) {
            AnalyticsEvents.APPLICATION_FOREGROUNDED -> {
                mixpanel.people.set(
                    JSONObject().apply {
                        put("Last Active", isoDate)
                        put("Last App Open", isoDate)
                    }
                )
            }

            AnalyticsEvents.STARTED_PRESENT_SESSION,
            AnalyticsEvents.ENDED_PRESENT_SESSION -> {
                mixpanel.people.set(
                    JSONObject().apply {
                        put("Last Active", isoDate)
                        put("Last Session", isoDate)
                    }
                )
            }
        }
    }

    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }

    companion object {
        private const val TAG = "BP_Analytics"
        private const val MIXPANEL_TOKEN = "2a2e7b9b0d3762a0881cbc866e3dc266"
        private const val ANALYTICS_PREFS = "analytics_prefs"

        private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
