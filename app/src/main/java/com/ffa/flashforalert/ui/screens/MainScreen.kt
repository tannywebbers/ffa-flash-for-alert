package com.ffa.flashforalert.ui.screens

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ffa.flashforalert.domain.models.InstalledApp
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
        uiState.settings.ffaEnabled && uiState.noneSelected -> "FFA active · Incoming calls only (no apps selected)"
        uiState.settings.ffaEnabled -> "FFA active · ${uiState.selectedCount} apps & incoming calls"
        else -> "FFA inactive · Enable from Quick Settings tile"
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
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("main_refresh_button")
                    ) { Icon(Icons.Outlined.Refresh, contentDescription = "Refresh") }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("main_settings_button")
                    ) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
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
                item {
                    BatteryOptimizationCard(
                        onOpenSettings = { onOpenBatteryOptimization(context) },
                        onDismiss = onDismissBatteryPrompt
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            item {
                TurnOnAllRow(
                    allSelected = uiState.allSelected,
                    partial = !uiState.allSelected && !uiState.noneSelected,
                    selectedCount = uiState.selectedCount,
                    totalCount = uiState.apps.size,
                    enabled = uiState.apps.isNotEmpty(),
                    onToggle = onToggleAll
                )
                Spacer(Modifier.height(10.dp))
            }

            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text("Search apps") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("app_search_input")
                )
                Spacer(Modifier.height(10.dp))
            }

            if (!uiState.notificationAccessGranted) {
                item {
                    SetupHint { onOpenNotificationAccess(context) }
                    Spacer(Modifier.height(10.dp))
                }
            }

            when {
                uiState.isLoadingApps -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }
                uiState.filteredApps.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (uiState.apps.isEmpty()) "No launchable apps found." else "No apps match \"${uiState.searchQuery}\".",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    items(uiState.filteredApps, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            loadIcon = loadIcon,
                            onToggle = { onToggleApp(app.packageName, it) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatusCard(status: String, color: Color, showSetup: Boolean, onSetup: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).padding(16.dp)
    ) {
        Text("Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(status, color = color, fontWeight = FontWeight.Medium)
        if (showSetup) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onSetup,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.testTag("grant_notification_access_button")
            ) { Text("Grant notification access") }
        }
    }
}

@Composable
private fun BatteryOptimizationCard(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.BatterySaver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("Battery optimization active", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Disable battery optimization for FFA so background call and app flash alerts are not delayed or killed by Android.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("main_battery_settings_button")
            ) { Text("Open battery settings") }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("main_battery_dismiss_button")
            ) { Text("Already unrestricted") }
        }
    }
}

@Composable
private fun TurnOnAllRow(
    allSelected: Boolean,
    partial: Boolean,
    selectedCount: Int,
    totalCount: Int,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled) { onToggle(!allSelected) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Turn On All", fontWeight = FontWeight.SemiBold)
            val subtitle = when {
                totalCount == 0 -> "Loading apps..."
                partial -> "Partial ($selectedCount of $totalCount selected)"
                allSelected -> "All $totalCount apps selected"
                else -> "Incoming calls only (0 of $totalCount apps)"
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = allSelected,
            onCheckedChange = onToggle,
            enabled = enabled,
            modifier = Modifier.testTag("turn_on_all_switch")
        )
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    loadIcon: (String) -> Drawable?,
    onToggle: (Boolean) -> Unit
) {
    val iconBitmap: ImageBitmap? = remember(app.packageName) {
        runCatching { loadIcon(app.packageName)?.toBitmap()?.asImageBitmap() }.getOrNull()
    }
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onToggle(!app.enabled) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = "${app.label} icon",
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = app.enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.testTag("app_toggle_${app.packageName}")
        )
    }
}

@Composable
private fun SetupHint(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(
            "Add FFA to Quick Settings for instant activation. Grant notification access to detect selected apps and incoming calls.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}