package com.ffa.flashforalert.domain.models

enum class FlashBrightnessMode {
    MAX,
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun fromName(name: String?): FlashBrightnessMode =
            entries.firstOrNull { it.name == name } ?: MAX
    }
}