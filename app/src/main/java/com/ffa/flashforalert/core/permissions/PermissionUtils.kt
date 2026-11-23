package com.ffa.flashforalert.core.permissions

import android.content.ComponentName
import android.content.Context
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
}