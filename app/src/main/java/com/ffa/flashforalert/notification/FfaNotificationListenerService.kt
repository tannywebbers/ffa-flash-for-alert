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
                return
            }

            if (!deduplicator.isNewEvent(sbn)) return
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
        if (isIncomingRingingCall(n)) {
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

        // Android 12+ Notification.CallStyle sets "android.callType":
        // 1 = CALL_TYPE_INCOMING, 2 = CALL_TYPE_ONGOING (answered or outgoing), 3 = CALL_TYPE_SCREENING
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val extras = n.extras
            if (extras != null && extras.containsKey("android.callType")) {
                val callType = extras.getInt("android.callType", -1)
                return callType == 1
            }
        }

        // Fallback for older Android versions or custom dialers:
        // Incoming ringing calls set a fullScreenIntent; outgoing/active calls do not.
        return n.fullScreenIntent != null
    }

    private fun isAlertableNotification(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification ?: return false
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
            pulses = FlashDefaults.NOTIFICATION_PULSES,
            repeatUntilCancelled = false
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