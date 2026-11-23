package com.ffa.flashforalert.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ffa.flashforalert.core.constants.FlashDefaults
import com.ffa.flashforalert.domain.models.FlashBrightnessMode
import com.ffa.flashforalert.ui.FfaUiState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: FfaUiState,
    onBack: () -> Unit,
    onCallTiming: (Long, Long) -> Unit,
    onBrightness: (FlashBrightnessMode) -> Unit,
    onVibration: (Boolean) -> Unit,
    onPreviewCall: () -> Unit
) {
    val settings = uiState.settings
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(
                title = "Incoming call flash",
                subtitle = "FFA blinks continuously while an incoming call is ringing and stops when answered, declined, or ended."
            ) {
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
                if (!uiState.hasFlash) {
                    Text("Flash unavailable on this device.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            SectionCard(
                title = "Dim",
                subtitle = "Slide up for brighter flash, down for dimmer. Colors show intensity from red (low) to green (bright)."
            ) {
                if (uiState.supportsTorchStrength) {
                    BrightnessSlider(
                        selected = settings.brightnessMode,
                        onSelect = onBrightness
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hardware supports torch strength (max level ${uiState.maxTorchStrength}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Dimming is unavailable on this device. The flash will use standard ON/OFF torch control.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SectionCard(title = "Vibration", subtitle = "Hard-coded ${FlashDefaults.VIBRATION_MS} ms pulse on call flash trigger. No duration configuration.") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Vibrate on call", fontWeight = FontWeight.Medium)
                        Text("40 ms (fixed)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = settings.vibrationEnabled, onCheckedChange = onVibration)
                }
            }

            SectionCard(title = "Behavior", subtitle = "Call-only mode.") {
                FixedBehaviorRow("Incoming calls", "Blink continuously")
                FixedBehaviorRow("Answered/declined/ended", "Stop immediately")
                FixedBehaviorRow("App notifications", "No flash")
                FixedBehaviorRow("Silent mode", "Always enabled")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BrightnessSlider(selected: FlashBrightnessMode, onSelect: (FlashBrightnessMode) -> Unit) {
    val modes = listOf(FlashBrightnessMode.LOW, FlashBrightnessMode.MEDIUM, FlashBrightnessMode.HIGH, FlashBrightnessMode.MAX)
    val labels = listOf("Low", "Medium", "High", "Max")
    val colors = listOf(Color(0xFFD32F2F), Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFF4CAF50))
    val selectedIndex = modes.indexOf(selected).coerceAtLeast(0)
    var trackHeightPx by remember { mutableIntStateOf(1) }

    Row(modifier = Modifier.fillMaxWidth().height(260.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
            labels.reversed().forEachIndexed { i, label ->
                val idx = labels.lastIndex - i
                Text(
                    label,
                    color = if (idx == selectedIndex) colors[idx] else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (idx == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .width(72.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(36.dp))
                .background(Brush.verticalGradient(colors.reversed()))
                .onSizeChanged { trackHeightPx = it.height.coerceAtLeast(1) }
                .pointerInput(trackHeightPx) {
                    detectTapGestures { offset ->
                        val fraction = 1f - (offset.y / trackHeightPx.toFloat()).coerceIn(0f, 1f)
                        val idx = (fraction * (modes.lastIndex)).roundToInt().coerceIn(0, modes.lastIndex)
                        onSelect(modes[idx])
                    }
                }
                .pointerInput(trackHeightPx) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val fraction = 1f - (change.position.y / trackHeightPx.toFloat()).coerceIn(0f, 1f)
                        val idx = (fraction * (modes.lastIndex)).roundToInt().coerceIn(0, modes.lastIndex)
                        onSelect(modes[idx])
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            val fraction = selectedIndex.toFloat() / modes.lastIndex.toFloat()
            val thumbY = ((1f - fraction) * (trackHeightPx - with(LocalDensity.current) { 28.dp.toPx() })).roundToInt().coerceAtLeast(0)
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbY) }
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(labels[selectedIndex], fontWeight = FontWeight.Bold, color = colors[selectedIndex])
            Text("Brightness level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)
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

@Composable
private fun DurationInputs(
    onLabel: String, offLabel: String, onValue: Long, offValue: Long,
    onApply: (Long, Long) -> Unit, onRestoreDefault: () -> Unit
) {
    var onText by remember(onValue) { mutableStateOf(onValue.toString()) }
    var offText by remember(offValue) { mutableStateOf(offValue.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = onText, onValueChange = { onText = it.filter(Char::isDigit).take(4) }, label = { Text(onLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = offText, onValueChange = { offText = it.filter(Char::isDigit).take(4) }, label = { Text(offLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
        }
        if (error != null) { Spacer(Modifier.height(4.dp)); Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                val on = onText.toLongOrNull(); val off = offText.toLongOrNull()
                when {
                    on == null || off == null -> error = "Enter numeric ON and OFF values."
                    on < FlashDefaults.MIN_DURATION_MS || off < FlashDefaults.MIN_DURATION_MS -> error = "Minimum is ${FlashDefaults.MIN_DURATION_MS} ms."
                    on > FlashDefaults.MAX_DURATION_MS || off > FlashDefaults.MAX_DURATION_MS -> error = "Maximum is ${FlashDefaults.MAX_DURATION_MS} ms."
                    else -> { error = null; onApply(on, off) }
                }
            }) { Text("Apply") }
            OutlinedButton(onClick = { error = null; onRestoreDefault() }) {
                Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text("Restore default")
            }
        }
        Text("Limits: ${FlashDefaults.MIN_DURATION_MS}–${FlashDefaults.MAX_DURATION_MS} ms.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FixedBehaviorRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
    }
}