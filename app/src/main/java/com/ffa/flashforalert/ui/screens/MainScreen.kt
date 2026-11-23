package com.ffa.flashforalert.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ffa.flashforalert.ui.FfaUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: FfaUiState,
    onOpenSettings: () -> Unit,
    onOpenNotificationAccess: (Context) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val statusText = when {
        !uiState.hasFlash -> "Flash unavailable on this device"
        !uiState.notificationAccessGranted -> "Notification access required"
        uiState.settings.ffaEnabled -> "FFA active · Incoming calls will blink continuously"
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
                    IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, contentDescription = "Refresh") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
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
                StatusCard(statusText, statusColor, !uiState.notificationAccessGranted) { onOpenNotificationAccess(context) }
                Spacer(Modifier.height(12.dp))
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(16.dp)
                ) {
                    Text("Call-only mode", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "FFA flashes only for incoming calls. It blinks continuously while ringing and stops when answered, declined, or ended. App notification flashes are disabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            if (!uiState.notificationAccessGranted) {
                item { SetupHint { onOpenNotificationAccess(context) }; Spacer(Modifier.height(8.dp)) }
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
            TextButton(onClick = onSetup, contentPadding = PaddingValues(0.dp)) { Text("Grant notification access") }
        }
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
            "Add FFA to Quick Settings for instant activation. Flash alerts trigger for incoming calls only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}