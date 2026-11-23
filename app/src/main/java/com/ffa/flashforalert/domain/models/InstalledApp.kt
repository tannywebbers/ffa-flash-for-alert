package com.ffa.flashforalert.domain.models

data class InstalledApp(
    val packageName: String,
    val label: String,
    val enabled: Boolean
)