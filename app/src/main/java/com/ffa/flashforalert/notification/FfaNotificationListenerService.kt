package com.ffa.flashforalert.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ffa.flashforalert.core.constants.FlashDefaults
import com.ffa.flashforalert.core.permissions.PermissionUtils
import com.ffa.flashforalert.domain.models.FlashPattern
import com.ffa.flashforalert.service.FfaServices
import kotlinx.coroutines.launch

class FfaNotificationListenerService : NotificationListenerService() {

    private val deduplicator = NotificationDeduplicator()

    override fun onCreate() {
        super.onCreate()
        if (!FfaServices.isInitialized()) {
            FfaServices.init(applicationContext)
        }
        FfaServices.updateNotificationAccess(PermissionUtils.isNotificationListenerEnabled(this))
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        if (!FfaServices.isInitialized()) FfaServices.init(applicationContext)
        FfaServices.updateNotificationAccess(true)
        FfaServices.appScope.launch { FfaServices.preferences.setFfaEnabled(FfaServices.currentSettings().ffaEnabled) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (FfaServices.isInitialized()) {
            FfaServices.updateNotificationAccess(PermissionUtils.isNotificationListenerEnabled(this))
        }
    }

    override fun onDestroy() {
        if (FfaServices.isInitialized()) {
            FfaServices.updateNotificationAccess(PermissionUtils.isNotificationListenerEnabled(this))
        }
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        try {
            if (!FfaServices.isInitialized()) FfaServices.init(applicationContext)
            val settings = FfaServices.currentSettings()
            if (!settings.ffaEnabled) return
            if (!PermissionUtils.isNotificationListenerEnabled(this)) {
                FfaServices.updateNotificationAccess(false)
                return
            }
            FfaServices.updateNotificationAccess(true)

            val pkg = sbn.packageName ?: return
            if (pkg == packageName) return

            if (!deduplicator.isNewEvent(sbn)) return

            val isCall = isIncomingCallNotification(sbn)
            if (isCall) {
                triggerCallFlash()
                return
            }

            if (!isAlertableNotification(sbn)) return
            if (!settings.selectedPackages.contains(pkg)) return

            triggerNotificationFlash()
        } catch (e: Exception) {
            Log.w(TAG, "onNotificationPosted error", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        try {
            deduplicator.remove(sbn)
        } catch (_: Exception) { }
    }

    private fun isIncomingCallNotification(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification ?: return false
        val category = n.category
        if (category == Notification.CATEGORY_CALL) return true
        // Some dialers post call-style notifications without category; be conservative.
        return false
    }

    private fun isAlertableNotification(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification ?: return false
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            // Group summaries can be noisy; allow only if they look like fresh alert events? 
            // Safer to ignore summaries to avoid duplicate flashes for grouped updates.
            // But some apps may only post summaries - keep conservative: ignore summaries.
            // return false
        }
        // Avoid ongoing/non-alert service notifications where possible
        if (sbn.isOngoing && n.category != Notification.CATEGORY_MESSAGE && n.category != Notification.CATEGORY_EMAIL) {
            return false
        }
        return true
    }

    private fun triggerNotificationFlash() {
        val settings = FfaServices.currentSettings()
        if (!FfaServices.torchController.hasFlashUnit()) return
        val pattern = FlashPattern(
            initialOffMs = FlashDefaults.NOTIFICATION_INITIAL_OFF_MS,
            onMs = settings.notificationOnMs,
            offMs = settings.notificationOffMs,
            pulses = FlashDefaults.NOTIFICATION_PULSES
        )
        FfaServices.flashEngine.play(
            pattern = pattern,
            brightnessMode = settings.brightnessMode,
            vibrate = settings.vibrationEnabled
        )
    }

    private fun triggerCallFlash() {
        val settings = FfaServices.currentSettings()
        if (!FfaServices.torchController.hasFlashUnit()) return
        // Calls: continuous-ish alert pattern? spec defines call flash intervals (ON/OFF) but not pulse count explicitly
        // "CALL FLASH INTERVALS ... Provide ON/OFF duration..." and "separate call flash configuration".
        // Use a repeating-feel sequence with multiple pulses while keeping bounded; choose reasonable pulse count.
        val pattern = FlashPattern(
            initialOffMs = 0L,
            onMs = settings.callOnMs,
            offMs = settings.callOffMs,
            pulses = 6
        )
        FfaServices.flashEngine.play(
            pattern = pattern,
            brightnessMode = settings.brightnessMode,
            vibrate = settings.vibrationEnabled
        )
    }

    companion object {
        private const val TAG = "FfaNotifListener"
    }
}