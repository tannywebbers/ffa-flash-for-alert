package com.ffa.flashforalert.domain.models

import com.ffa.flashforalert.core.constants.FlashDefaults

enum class NotificationPreset(
    val label: String,
    val onMs: Long,
    val offMs: Long
) {
    IPHONE_SHARP("iPhone Sharp", FlashDefaults.NOTIFICATION_ON_MS, FlashDefaults.NOTIFICATION_OFF_MS),
    FAST("Fast", 60L, 100L),
    BALANCED("Balanced", 120L, 180L),
    SOFT("Soft", 180L, 250L),
    CUSTOM("Custom", FlashDefaults.NOTIFICATION_ON_MS, FlashDefaults.NOTIFICATION_OFF_MS);

    companion object {
        fun fromName(name: String?): NotificationPreset =
            entries.firstOrNull { it.name == name } ?: IPHONE_SHARP
    }
}