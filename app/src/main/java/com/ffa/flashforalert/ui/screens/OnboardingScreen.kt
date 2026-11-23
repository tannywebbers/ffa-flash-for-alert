package com.ffa.flashforalert.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ffa.flashforalert.ui.FfaUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    uiState: FfaUiState,
    onOpenNotificationAccess: (Context) -> Unit,
    onContinue: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("FFA", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> OnboardPage(
                        icon = { Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) },
                        title = "Welcome to FFA",
                        body = "Never miss an important notification. Your camera flash will alert you instantly with sharp, precise pulses."
                    )
                    1 -> OnboardPage(
                        icon = { Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) },
                        title = "Notification access",
                        body = "FFA needs notification access to detect alerts from the applications you choose. FFA only uses the source app to decide whether to flash — it does not read or collect message content.",
                        extra = {
                            Spacer(Modifier.height(20.dp))
                            if (uiState.notificationAccessGranted) {
                                AssistChip(
                                    onClick = { onRefresh() },
                                    label = { Text("Access granted") },
                                    leadingIcon = { Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
                                )
                            } else {
                                Button(onClick = {
                                    onOpenNotificationAccess(context)
                                }) {
                                    Text("Open notification access settings")
                                }
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = onRefresh) { Text("I've granted access") }
                            }
                        }
                    )
                    2 -> OnboardPage(
                        icon = { Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) },
                        title = "Add FFA to Quick Settings",
                        body = "Add FFA to your Control Center for instant activation.\n\n1. Swipe down to open Quick Settings.\n2. Tap Edit / pencil.\n3. Find \"FFA\" and drag it into your active tiles.\n4. Tap FFA once to activate or deactivate flash alerts.",
                        extra = {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "If notification access or flash is unavailable, tapping the tile will open setup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    )
                    3 -> OnboardPage(
                        icon = { Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) },
                        title = "Choose your apps",
                        body = "Select which applications can trigger flash alerts. You can change this anytime from the main screen.",
                        extra = {
                            Spacer(Modifier.height(12.dp))
                            val status = when {
                                !uiState.hasFlash -> "No camera flash detected on this device."
                                !uiState.notificationAccessGranted -> "Notification access is still required for alerts to work."
                                else -> "You're ready. Continue to pick apps and enable FFA from Quick Settings."
                            }
                            Text(
                                status,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!uiState.hasFlash) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(vertical = 12.dp)) {
                repeat(4) { i ->
                    val selected = pagerState.currentPage == i
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { onContinue() },
                    enabled = pagerState.currentPage < 3
                ) { if (pagerState.currentPage < 3) Text("Skip") else Spacer(Modifier.width(1.dp)) }

                Button(onClick = {
                    if (pagerState.currentPage < 3) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onContinue()
                    }
                }) {
                    Text(if (pagerState.currentPage < 3) "Next" else "Open FFA")
                }
            }
        }
    }
}

@Composable
private fun OnboardPage(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.height(28.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        extra()
    }
}