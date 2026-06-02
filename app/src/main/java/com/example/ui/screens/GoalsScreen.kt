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
import com.example.data.model.Goal
import com.example.ui.components.GlassCard
import com.example.ui.components.ProgressRing
import com.example.ui.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun GoalsScreen(
    viewModel: TrackerViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val goals by viewModel.goals.collectAsState()

    var goalNameInput by remember { mutableStateOf("") }
    var selectedGoalType by remember { mutableStateOf("DAILY") } // DAILY, WEEKLY, MONTHLY, SEMESTER, YEARLY
    var selectedDeadlineOffsetDays by remember { mutableStateOf(1L) } // default 1 day for daily

    // Category view filters
    var currentFilterTab by remember { mutableStateOf("ALL") }

    val filteredGoals = remember(goals, currentFilterTab) {
        if (currentFilterTab == "ALL") goals
        else goals.filter { it.type == currentFilterTab }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Headers
        item {
            Column {
                Text(
                    text = "GOAL TRACKING SYSTEM",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Define Your Academic Vision",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Creation Board
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "CONSTRUCT NEW MILESTONE Goal",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )

                OutlinedTextField(
                    value = goalNameInput,
                    onValueChange = { goalNameInput = it },
                    label = { Text("Goal e.g., Summarize Anatomy notes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.secondary)
                )

                // Goal Type Selector chips (Daily, Weekly, Monthly, Semester, Yearly)
                Column {
                    Text("Goal Class:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly", "SEMESTER" to "Semester", "YEARLY" to "Yearly").forEach { (type, label) ->
                            val active = selectedGoalType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (active) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) 
                                                else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (active) MaterialTheme.colorScheme.secondary 
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { 
                                        selectedGoalType = type
                                        // Auto adjust offsets sensibly
                                        selectedDeadlineOffsetDays = when(type) {
                                            "DAILY" -> 1L
                                            "WEEKLY" -> 7L
                                            "MONTHLY" -> 30L
                                            "SEMESTER" -> 120L
                                            "YEARLY" -> 365L
                                            else -> 1L
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (goalNameInput.isNotBlank()) {
                            val targetMills = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(selectedDeadlineOffsetDays)
                            viewModel.addGoal(
                                type = selectedGoalType,
                                name = goalNameInput,
                                deadlineMillis = targetMills
                            )
                            goalNameInput = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Inscribe Goal Vision")
                }
            }
        }

        // Filtering Tabs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("ALL" to "All", "DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly").forEach { (tab, label) ->
                    val active = currentFilterTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (active) MaterialTheme.colorScheme.secondary 
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { currentFilterTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (active) MaterialTheme.colorScheme.onPrimary 
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Goals Content Listing
        if (filteredGoals.isEmpty()) {
            item {
                Text(
                    "No matching milestone goals under this tab category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }

        items(filteredGoals) { goal ->
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val dateStr = sdf.format(Date(goal.deadlineMillis))

            var sliderValue by remember(goal) { mutableStateOf(goal.progressPercent.toFloat()) }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = if (goal.isCompleted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(if (goal.isCompleted) MaterialTheme.colorScheme.secondary else Color.Transparent, CircleShape)
                                .border(1.5.dp, if (goal.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                                .clickable { viewModel.toggleGoal(goal) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (goal.isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = "Tick", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = goal.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = goal.type,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "Due Deadline: $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.deleteGoal(goal.id) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Goal",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Interactive Progress Slider
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            viewModel.updateGoalProgress(goal, sliderValue.toInt())
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.secondary,
                            thumbColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    Text(
                        "${sliderValue.toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
