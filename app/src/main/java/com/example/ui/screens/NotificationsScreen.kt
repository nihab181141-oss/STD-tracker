package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotification
import com.example.data.model.NotificationSetting
import com.example.ui.components.GlassCard
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    viewModel: TrackerViewModel,
    onNavigateBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    val settingsState by viewModel.notificationSettings.collectAsState()
    var isPreferencesExpanded by remember { mutableStateOf(false) }

    val settings = settingsState ?: NotificationSetting()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07070A)) // Luxury dark atmosphere
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .testTag("notification_back_button")
                            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                            .background(Color.White.copy(alpha = 0.03f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = "NOTIFICATIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Activity Inbox",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Header Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { isPreferencesExpanded = !isPreferencesExpanded },
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = if (isPreferencesExpanded) 0.5f else 0.1f), CircleShape)
                            .background(if (isPreferencesExpanded) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Custom Preferences",
                            tint = if (isPreferencesExpanded) MaterialTheme.colorScheme.secondary else Color.White
                        )
                    }

                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllNotifications() },
                            modifier = Modifier
                                .border(1.dp, Color.Red.copy(alpha = 0.15f), CircleShape)
                                .background(Color.Red.copy(alpha = 0.03f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All Inbox",
                                tint = Color.Red.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Expandable Preferences Panel
            AnimatedVisibility(
                visible = isPreferencesExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    backgroundColor = Color.White.copy(alpha = 0.04f)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Notification Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Divider(color = Color.White.copy(alpha = 0.08f))

                        // Switch row 1: Study Schedule
                        PreferenceSwitchRow(
                            title = "Study Schedule Reminders",
                            subtitle = "Prompts to study based on schedule times of day",
                            checked = settings.timerReminders,
                            onCheckedChange = {
                                viewModel.updateNotificationSettings(
                                    timer = it,
                                    habit = settings.habitReminders,
                                    deadline = settings.deadlineAlerts,
                                    silent = settings.silentMode
                                )
                            }
                        )

                        // Switch row 2: Habit completes
                        PreferenceSwitchRow(
                            title = "Habit Streaks Prompts",
                            subtitle = "Reminds you to check off unchecked habits",
                            checked = settings.habitReminders,
                            onCheckedChange = {
                                viewModel.updateNotificationSettings(
                                    timer = settings.timerReminders,
                                    habit = it,
                                    deadline = settings.deadlineAlerts,
                                    silent = settings.silentMode
                                )
                            }
                        )

                        // Switch row 3: Academic deadlines
                        PreferenceSwitchRow(
                            title = "Academic Deadline Alerts",
                            subtitle = "Upcoming exam & assignment due timers",
                            checked = settings.deadlineAlerts,
                            onCheckedChange = {
                                viewModel.updateNotificationSettings(
                                    timer = settings.timerReminders,
                                    habit = settings.habitReminders,
                                    deadline = it,
                                    silent = settings.silentMode
                                )
                            }
                        )

                        // Switch row 4: Silent Mode
                        PreferenceSwitchRow(
                            title = "Mute (Silent Mode)",
                            subtitle = "Silence all triggers temporarily",
                            checked = settings.silentMode,
                            onCheckedChange = {
                                viewModel.updateNotificationSettings(
                                    timer = settings.timerReminders,
                                    habit = settings.habitReminders,
                                    deadline = settings.deadlineAlerts,
                                    silent = it
                                )
                            }
                        )

                        // Action button inside options
                        Button(
                            onClick = {
                                viewModel.triggerContextNotificationsEvaluation()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.secondary
                            ),
                            border = borderStroke(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan Context Now",
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text("Trigger Custom Smart Scan")
                        }
                    }
                }
            }

            // Inbox list
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MailOutline,
                            contentDescription = "Inbox Empty",
                            tint = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No notification entries found",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "When scheduling study slots, pending habits, or approaching exam deadlines exist, context warnings will automatically populate here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.2f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 70.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            onDelete = { viewModel.deleteNotification(notification.id) },
                            onMarkAsRead = { viewModel.markNotificationAsRead(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun NotificationCard(
    notification: AppNotification,
    onDelete: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    val dateText = remember(notification.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(notification.timestamp))
    }

    val iconInfo = when (notification.type) {
        "TIMER" -> Pair(Icons.Default.Timer, MaterialTheme.colorScheme.secondary)
        "HABIT" -> Pair(Icons.Default.Whatshot, WarningOrange)
        else -> Pair(Icons.Default.Warning, Color(0xFFFF5252))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (notification.isRead) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = if (notification.isRead) Color.White.copy(alpha = 0.04f) else iconInfo.second.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconInfo.second.copy(alpha = 0.08f), CircleShape)
                    .border(1.dp, iconInfo.second.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconInfo.first,
                    contentDescription = "Notification Badge",
                    tint = iconInfo.second,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold
                        ),
                        color = if (notification.isRead) Color.White.copy(alpha = 0.8f) else Color.White
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (notification.isRead) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.8f)
                )

                // Optional buttons inside card
                if (!notification.isRead) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "MARK READ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = iconInfo.second,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier
                            .clickable { onMarkAsRead() }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                }
            }

            // Absolute trailing delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Notification",
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// Inline helper because compose might miss border stroke dependency wrapper sometimes
@Composable
private fun borderStroke(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)
