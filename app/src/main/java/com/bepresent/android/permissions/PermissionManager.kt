package com.bepresent.android.permissions

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.bepresent.android.service.AccessibilityMonitorService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SETTINGS_HIGHLIGHT_KEY = ":settings:fragment_args_key"
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasNotificationPermission(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun getUsageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            putExtra(SETTINGS_HIGHLIGHT_KEY, context.packageName)
        }
    }

    fun getBatteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun getOverlayPermissionIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun hasAccessibilityPermission(): Boolean {
        val enabled =
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        if (!enabled) return false

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val expectedService = ComponentName(
            context,
            AccessibilityMonitorService::class.java
        )

        return enabledServices.split(':').any { service ->
            val enabledComponent = ComponentName.unflattenFromString(service) ?: return@any false
            enabledComponent.packageName == expectedService.packageName &&
                enabledComponent.className == expectedService.className
        }
    }

    fun getAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            val componentName = ComponentName(context, AccessibilityMonitorService::class.java)
            putExtra(SETTINGS_HIGHLIGHT_KEY, componentName.flattenToString())
        }
    }

    fun getAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun needsNotificationPermissionRequest(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    data class PermissionStatus(
        val usageStats: Boolean,
        val notifications: Boolean,
        val batteryOptimization: Boolean,
        val overlay: Boolean,
        val accessibility: Boolean
    ) {
        val allGranted: Boolean
            get() = usageStats && notifications && batteryOptimization && overlay && accessibility

        val criticalGranted: Boolean
            get() = usageStats && overlay && accessibility
    }

    fun checkAll(): PermissionStatus {
        return PermissionStatus(
            usageStats = hasUsageStatsPermission(),
            notifications = hasNotificationPermission(),
            batteryOptimization = isBatteryOptimizationDisabled(),
            overlay = hasOverlayPermission(),
            accessibility = hasAccessibilityPermission()
        )
    }
}
