package com.ffa.flashforalert.notification

import android.app.Notification
import android.os.Build
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

            val n = sbn.notification ?: return
            if (isCallNotification(n)) {
                handleCallNotification(sbn, n)
            }
            // App is call-only by product requirement: ignore non-call notifications.
            return
        } catch (e: Exception) {
            Log.w(TAG, "onNotificationPosted error", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        try {
            deduplicator.remove(sbn)
            val n = sbn.notification
            if (n != null && isCallNotification(n)) {
                FfaServices.flashEngine.cancel()
            }
        } catch (_: Exception) { }
    }

    private fun isCallNotification(n: Notification): Boolean {
        return n.category == Notification.CATEGORY_CALL
    }

    private fun handleCallNotification(sbn: StatusBarNotification, n: Notification) {
        val isRinging = isIncomingRingingCall(n)
        if (isRinging) {
            if (!deduplicator.isNewEvent(sbn)) return
            triggerCallFlash()
        } else {
            FfaServices.flashEngine.cancel()
        }
    }

    private fun isIncomingRingingCall(n: Notification): Boolean {
        if (n.category != Notification.CATEGORY_CALL) return false
        val isOngoing = (n.flags and Notification.FLAG_ONGOING_EVENT) != 0
        val isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0
        if (isGroupSummary || !isOngoing) return false
        // Incoming ringing call notifications commonly expose a full-screen intent.
        return n.fullScreenIntent != null
    }

    private fun triggerCallFlash() {
        val settings = FfaServices.currentSettings()
        if (!FfaServices.torchController.hasFlashUnit()) return
        val pattern = FlashPattern(
            initialOffMs = 0L,
            onMs = settings.callOnMs,
            offMs = settings.callOffMs,
            pulses = 1,
            repeatUntilCancelled = true
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