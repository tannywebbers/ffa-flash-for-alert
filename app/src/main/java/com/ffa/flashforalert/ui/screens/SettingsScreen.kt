package com.ffa.flashforalert.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ffa.flashforalert.core.constants.FlashDefaults
import com.ffa.flashforalert.domain.models.FlashBrightnessMode
import com.ffa.flashforalert.domain.models.NotificationPreset
import com.ffa.flashforalert.ui.FfaUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: FfaUiState,
    onBack: () -> Unit,
    onPreset: (NotificationPreset) -> Unit,
    onNotificationTiming: (Long, Long) -> Unit,
    onCallTiming: (Long, Long) -> Unit,
    onBrightness: (FlashBrightnessMode) -> Unit,
    onVibration: (Boolean) -> Unit,
    onPreviewNotification: () -> Unit,
    onPreviewCall: () -> Unit
) {
    val settings = uiState.settings
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notification flash
            SectionCard(title = "Notification flash intervals", subtitle = "Two-pulse signature: initial OFF ${FlashDefaults.NOTIFICATION_INITIAL_OFF_MS} ms, then ON/OFF pulses, final OFF.") {
                PresetRow(
                    selected = settings.notificationPreset,
                    onSelect = onPreset
                )
                Spacer(Modifier.height(12.dp))
                DurationInputs(
                    onLabel = "Flash ON (ms)",
                    offLabel = "Flash OFF (ms)",
                    onValue = settings.notificationOnMs,
                    offValue = settings.notificationOffMs,
                    onApply = { on, off -> onNotificationTiming(on, off) },
                    onRestoreDefault = {
                        onPreset(NotificationPreset.IPHONE_SHARP)
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pattern preview: OFF ${FlashDefaults.NOTIFICATION_INITIAL_OFF_MS} ? ON ${settings.notificationOnMs} ? OFF ${settings.notificationOffMs} ? ON ${settings.notificationOnMs} ? OFF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onPreviewNotification, enabled = uiState.hasFlash) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Preview notification pattern")
                }
                if (!uiState.hasFlash) {
                    Text("Flash unavailable on this device.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Call flash
            SectionCard(title = "Call flash intervals", subtitle = "Independent timing for incoming call alerts (category-call notifications).") {
                CallPresetRow(
                    onMs = settings.callOnMs,
                    offMs = settings.callOffMs,
                    onApply = onCallTiming
                )
                Spacer(Modifier.height(12.dp))
                DurationInputs(
                    onLabel = "Call ON (ms)",
                    offLabel = "Call OFF (ms)",
                    onValue = settings.callOnMs,
                    offValue = settings.callOffMs,
                    onApply = { on, off -> onCallTiming(on, off) },
                    onRestoreDefault = { onCallTiming(FlashDefaults.CALL_ON_MS, FlashDefaults.CALL_OFF_MS) }
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onPreviewCall, enabled = uiState.hasFlash) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Preview call pattern")
                }
            }

            // Brightness
            SectionCard(title = "Dim", subtitle = "Adjust flash intensity to conserve battery or reduce aggressiveness.") {
                if (uiState.supportsTorchStrength) {
                    BrightnessSelector(
                        selected = settings.brightnessMode,
                        maxLevel = uiState.maxTorchStrength,
                        onSelect = onBrightness
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Hardware supports torch strength (max level ${uiState.maxTorchStrength}). Default is maximum.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Dimming is unavailable on this device. The flash will use standard ON/OFF torch control at hardware default brightness.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Vibration
            SectionCard(title = "Vibration", subtitle = "Hard-coded ${FlashDefaults.VIBRATION_MS} ms pulse on each flash trigger. No duration configuration.") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Vibrate on alert", fontWeight = FontWeight.Medium)
                        Text("40 ms (fixed)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.vibrationEnabled, onCheckedChange = onVibration)
                }
            }

            // Fixed behaviors
            SectionCard(title = "Always-on behaviors", subtitle = "Hard-coded by design.") {
                FixedBehaviorRow("Flash while locked", "Always enabled")
                FixedBehaviorRow("Flash while unlocked", "Always enabled")
                FixedBehaviorRow("Flash in silent mode", "Always enabled")
                Spacer(Modifier.height(4.dp))
                Text(
                    "These behaviors are not configurable. Android system, camera, and manufacturer background restrictions may still affect delivery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetRow(selected: NotificationPreset, onSelect: (NotificationPreset) -> Unit) {
    val presets = listOf(
        NotificationPreset.IPHONE_SHARP,
        NotificationPreset.FAST,
        NotificationPreset.BALANCED,
        NotificationPreset.SOFT,
        NotificationPreset.CUSTOM
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { p ->
            FilterChip(
                selected = selected == p,
                onClick = { onSelect(p) },
                label = {
                    Text(if (p == NotificationPreset.IPHONE_SHARP) "iPhone Sharp (Default)" else p.label)
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CallPresetRow(onMs: Long, offMs: Long, onApply: (Long, Long) -> Unit) {
    data class CallPreset(val label: String, val on: Long, val off: Long)
    val presets = listOf(
        CallPreset("Default", FlashDefaults.CALL_ON_MS, FlashDefaults.CALL_OFF_MS),
        CallPreset("Fast", 250, 250),
        CallPreset("Slow", 600, 600),
    )
    val currentIsPreset = presets.any { it.on == onMs && it.off == offMs }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { p ->
            FilterChip(
                selected = p.on == onMs && p.off == offMs,
                onClick = { onApply(p.on, p.off) },
                label = { Text("${p.label} ${p.on}/${p.off} ms") }
            )
        }
        if (!currentIsPreset) {
            AssistChip(onClick = {}, label = { Text("Custom") })
        }
    }
}

@Composable
private fun DurationInputs(
    onLabel: String,
    offLabel: String,
    onValue: Long,
    offValue: Long,
    onApply: (Long, Long) -> Unit,
    onRestoreDefault: () -> Unit
) {
    var onText by remember(onValue) { mutableStateOf(onValue.toString()) }
    var offText by remember(offValue) { mutableStateOf(offValue.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = onText,
                onValueChange = { onText = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text(onLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = offText,
                onValueChange = { offText = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text(offLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                val on = onText.toLongOrNull()
                val off = offText.toLongOrNull()
                when {
                    on == null || off == null -> error = "Enter numeric ON and OFF values."
                    on < FlashDefaults.MIN_DURATION_MS || off < FlashDefaults.MIN_DURATION_MS ->
                        error = "Minimum is ${FlashDefaults.MIN_DURATION_MS} ms."
                    on > FlashDefaults.MAX_DURATION_MS || off > FlashDefaults.MAX_DURATION_MS ->
                        error = "Maximum is ${FlashDefaults.MAX_DURATION_MS} ms."
                    else -> {
                        error = null
                        onApply(on, off)
                    }
                }
            }) { Text("Apply") }
            OutlinedButton(onClick = {
                error = null
                onRestoreDefault()
            }) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore default")
            }
        }
        Text(
            "Limits: ${FlashDefaults.MIN_DURATION_MS}–${FlashDefaults.MAX_DURATION_MS} ms.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrightnessSelector(selected: FlashBrightnessMode, maxLevel: Int, onSelect: (FlashBrightnessMode) -> Unit) {
    val modes = listOf(
        FlashBrightnessMode.MAX to "Maximum",
        FlashBrightnessMode.HIGH to "High",
        FlashBrightnessMode.MEDIUM to "Medium",
        FlashBrightnessMode.LOW to "Low"
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { (mode, label) ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun FixedBehaviorRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
    }
}