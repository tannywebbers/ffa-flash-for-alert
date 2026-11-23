package com.ffa.flashforalert.domain.models

data class FlashPattern(
    val initialOffMs: Long = 0L,
    val onMs: Long,
    val offMs: Long,
    val pulses: Int,
    val repeatUntilCancelled: Boolean = false
)