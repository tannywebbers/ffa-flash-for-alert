package com.ffa.flashforalert.core.permissions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils

object PermissionUtils {
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?: return false
        if (flat.isBlank()) return false
        val names = flat.split(":")
        for (name in names) {
            val cn = ComponentName.unflattenFromString(name) ?: continue
            if (TextUtils.equals(pkgName, cn.packageName)) return true
        }
        return false
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
            pm.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrDefault(false)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val batteryListIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val settingsFallbackIntent = Intent(Settings.ACTION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching {
            context.startActivity(batteryListIntent)
        }.onFailure {
            runCatching {
                context.startActivity(appDetailsIntent)
            }.onFailure {
                runCatching { context.startActivity(settingsFallbackIntent) }
            }
        }
    }
}