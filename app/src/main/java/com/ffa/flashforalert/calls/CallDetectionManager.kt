package com.ffa.flashforalert.calls

/**
 * Call flash triggering is handled via NotificationListenerService CATEGORY_CALL detection
 * (FfaNotificationListenerService.isIncomingCallNotification). This keeps call detection
 * permission-conscious without requiring READ_PHONE_STATE/Telephony runtime permissions.
 *
 * Android/device dialer behavior varies: some incoming calls surface as call-category
 * notifications (reliably detectable here), while others may not. This module documents
 * the call-detection boundary and provides a single place to extend with Telephony-based
 * detection if a future, justified permission strategy is adopted.
 */
object CallDetectionManager {
    const val IMPLEMENTATION_NOTE = "Incoming call flashes are triggered from call-category notifications."
}