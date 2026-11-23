package com.ffa.flashforalert.notification

data class NotificationEventKey(
    val packageName: String,
    val id: Int,
    val tag: String?,
    val key: String?
)