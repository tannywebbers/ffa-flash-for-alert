package com.ffa.flashforalert.ui.screens

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ffa.flashforalert.ui.FfaUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: FfaUiState,
    onSearchChange: (String) -> Unit,
    onToggleApp: (String, Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotificationAccess: (Context) -> Unit,
    onOpenBatteryOptimization: (Context) -> Unit,
    onDismissBatteryPrompt: () -> Unit,
    loadIcon: (String) -> Drawable?,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val statusText = when {
        !uiState.hasFlash -> "Flash unavailable on this device"
        !uiState.notificationAccessGranted -> "Notification access required"
        uiState.settings.ffaEnabled && uiState.selectedCount == 0 -> "FFA active � No apps selected"
        uiState.settings.ffaEnabled -> "FFA active � ${uiState.selectedCount} app${if (uiState.selectedCount == 1) "" else "s"} selected"
        else -> "FFA inactive � Enable from Quick Settings tile"
    }
    val statusColor = when {
        !uiState.hasFlash || !uiState.notificationAccessGranted -> MaterialTheme.colorScheme.error
        uiState.settings.ffaEnabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("FFA", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                            Text("Flash For Alert", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                StatusCard(
                    status = statusText,
                    color = statusColor,
                    showSetup = !uiState.notificationAccessGranted,
                    onSetup = { onOpenNotificationAccess(context) }
                )
                Spacer(Modifier.height(12.dp))
            }

            if (!uiState.isBatteryOptimizationDisabled && !uiState.settings.batteryPromptDismissed) {
                item(key = "battery_optimization_card") {
                    BatteryOptimizationCard(
                        onOpenSettings = { onOpenBatteryOptimization(context) },
                        onDismiss = onDismissBatteryPrompt
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Turn On All", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Select every application in this list",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val toggleState = when {
                        uiState.allSelected -> true
                        uiState.noneSelected -> false
                        else -> false
                    }
                    if (!uiState.allSelected && !uiState.noneSelected && uiState.apps.isNotEmpty()) {
                        // partial state hint: show unchecked but textually we can't do tri-state easily with Switch;
                        // use a small indicator
                        Text("Partial", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Switch(
                        checked = uiState.allSelected,
                        onCheckedChange = { onToggleAll(it) },
                        enabled = uiState.apps.isNotEmpty()
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search installed applications") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(Modifier.height(8.dp))
            }

            if (!uiState.notificationAccessGranted) {
                item {
                    SetupHint(onClick = { onOpenNotificationAccess(context) })
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (uiState.isLoadingApps) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.filteredApps.isEmpty()) {
                item {
                    Text(
                        if (uiState.searchQuery.isNotBlank()) "No matching applications" else "No launchable applications found",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(uiState.filteredApps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        icon = loadIcon(app.packageName),
                        onToggle = { onToggleApp(app.packageName, it) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatusCard(status: String, color: Color, showSetup: Boolean, onSetup: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text("Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(status, color = color, fontWeight = FontWeight.Medium)
        if (showSetup) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onSetup, contentPadding = PaddingValues(0.dp)) {
                Text("Grant notification access")
            }
        }
    }
}

@Composable
private fun BatteryOptimizationCard(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_battery_optimization_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.BatteryAlert,
                    contentDescription = "Battery optimization active",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Disable battery optimization",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Android may pause or delay flash alerts when battery optimization is enabled. Set FFA to Unrestricted / Don't optimize.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("main_battery_settings_button")
                ) {
                    Text("Open battery settings")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("main_battery_dismiss_button")
                ) {
                    Text("Already unrestricted")
                }
            }
        }
    }
}

@Composable
private fun SetupHint(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(
            "Add FFA to Quick Settings for instant activation. Long-press your Quick Settings panel to edit tiles.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppRow(app: com.ffa.flashforalert.domain.models.InstalledApp, icon: Drawable?, onToggle: (Boolean) -> Unit) {
    val bitmap = remember(app.packageName, icon) {
        icon?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
        } else {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) { Text(app.label.take(1).uppercase(), fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = app.enabled, onCheckedChange = onToggle)
    }
}