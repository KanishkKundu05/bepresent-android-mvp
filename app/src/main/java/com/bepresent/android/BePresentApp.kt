package com.bepresent.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.bepresent.android.data.convex.ConvexManager
import com.bepresent.android.data.convex.SyncWorker
import com.stripe.android.PaymentConfiguration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class BePresentApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var convexManager: ConvexManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.STRIPE_PUBLISHABLE_KEY.isNotBlank()) {
            PaymentConfiguration.init(this, BuildConfig.STRIPE_PUBLISHABLE_KEY)
        }
        createNotificationChannels()
        com.bepresent.android.features.intentions.DailyResetWorker.schedule(this)
        SyncWorker.schedulePeriodic(this)

        // Attempt cached login on startup
        appScope.launch {
            try {
                convexManager.loginFromCache()
            } catch (_: Exception) {
                // No cached credentials, user will login manually
            }
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val monitoringChannel = NotificationChannel(
            CHANNEL_MONITORING,
            "Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent notification while BePresent is monitoring app usage"
            setShowBadge(false)
        }

        val sessionChannel = NotificationChannel(
            CHANNEL_SESSION,
            "Focus Sessions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Session timer and goal notifications"
        }

        val intentionChannel = NotificationChannel(
            CHANNEL_INTENTION,
            "App Intentions",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Timed open window notifications"
        }

        manager.createNotificationChannels(
            listOf(monitoringChannel, sessionChannel, intentionChannel)
        )
    }

    companion object {
        const val CHANNEL_MONITORING = "monitoring"
        const val CHANNEL_SESSION = "session"
        const val CHANNEL_INTENTION = "intention"
    }
}
