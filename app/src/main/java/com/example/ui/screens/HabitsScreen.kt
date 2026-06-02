package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.ui.components.CustomHeatmap
import com.example.ui.components.GlassCard
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.TrackerViewModel

@Composable
fun HabitsScreen(
    viewModel: TrackerViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val habits by viewModel.habits.collectAsState()
    val histories by viewModel.habitHistories.collectAsState()
    val todayStr = viewModel.todayString

    var newHabitName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Upper Title
        item {
            Column {
                Text(
                    text = "HABIT TRACKER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Habit Streaks & Metrics",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Completion Heatmap
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                CustomHeatmap(
                    habitHistories = histories,
                    habitsCount = habits.size
                )
            }
        }

        // Add custom habit field
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newHabitName,
                    onValueChange = { newHabitName = it },
                    label = { Text("Create Custom Habit...") },
                    placeholder = { Text("e.g. Code 1 hour, Sleep early") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (newHabitName.isNotBlank()) {
                            viewModel.addCustomHabit(newHabitName)
                            newHabitName = ""
                            keyboardController?.hide()
                        }
                    })
                )

                IconButton(
                    onClick = {
                        if (newHabitName.isNotBlank()) {
                            viewModel.addCustomHabit(newHabitName)
                            newHabitName = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Habit",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Habit list heading
        item {
            Text(
                text = "YOUR DAILY HABITS LIST",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (habits.isEmpty()) {
            item {
                Text(
                    text = "No habits. Type custom habits above to start.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }

        // Habits rows
        items(habits) { habit ->
            val wasCompletedToday = histories.any { it.habitId == habit.id && it.dateString == todayStr }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (wasCompletedToday) SuccessGreen.copy(alpha = 0.08f) 
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (wasCompletedToday) SuccessGreen.copy(alpha = 0.4f) 
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tick Box circles
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = if (wasCompletedToday) SuccessGreen else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (wasCompletedToday) SuccessGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { viewModel.toggleHabit(habit.id, todayStr) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (wasCompletedToday) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        // Streak subtitle indicator
                        if (habit.streak > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = "Streak Fire",
                                    tint = WarningOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${habit.streak} day streak",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = WarningOrange
                                )
                            }
                        } else {
                            Text(
                                text = "No active streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // Delete Custom Habit Trigger
                if (habit.isCustom) {
                    IconButton(
                        onClick = { viewModel.deleteHabit(habit.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Habit",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
