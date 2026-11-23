package com.ffa.flashforalert.notification

import android.service.notification.StatusBarNotification
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap

class NotificationDeduplicator {
    private data class Seen(val postTime: Long, val seenAtElapsed: Long)
    private val seen = ConcurrentHashMap<String, Seen>()
    private val ttlMs = 10_000L

    @Synchronized
    fun isNewEvent(sbn: StatusBarNotification): Boolean {
        cleanup()
        val key = sbn.key ?: "${sbn.packageName}|${sbn.id}|${sbn.tag}|${sbn.postTime}"
        val nowElapsed = SystemClock.elapsedRealtime()
        val postTime = sbn.postTime
        val prev = seen[key]
        val isNew = if (prev == null) {
            true
        } else {
            // Same sbn key but newer post time indicates a genuinely new/updated alert worth flashing
            postTime > prev.postTime && (nowElapsed - prev.seenAtElapsed) > 250L
        }
        if (isNew) {
            seen[key] = Seen(postTime = postTime, seenAtElapsed = nowElapsed)
        }
        return isNew
    }

    fun remove(sbn: StatusBarNotification) {
        val key = sbn.key ?: return
        seen.remove(key)
    }

    private fun cleanup() {
        val now = SystemClock.elapsedRealtime()
        val it = seen.entries.iterator()
        while (it.hasNext()) {
            val e = it.next()
            if (now - e.value.seenAtElapsed > ttlMs) it.remove()
        }
    }

    fun clear() { seen.clear() }
}