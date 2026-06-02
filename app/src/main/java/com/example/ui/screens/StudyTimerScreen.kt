package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.components.GlassCard
import com.example.ui.components.ProgressRing
import com.example.ui.viewmodel.TrackerViewModel
import com.example.ui.viewmodel.TimerState
import java.util.Locale

@Composable
fun StudyTimerScreen(
    viewModel: TrackerViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val timerState = viewModel.timerRunningState
    val secondsRemaining = viewModel.timerSecondsRemaining
    val totalSeconds = viewModel.timerInitialDurationSeconds
    val currentSubject = viewModel.timerSelectedSubject
    val activeModeName = viewModel.timerSelectedModeName

    val formattedTime = remember(secondsRemaining) {
        val minutes = secondsRemaining / 60
        val seconds = secondsRemaining % 60
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    val progressRatio = remember(secondsRemaining, totalSeconds) {
        if (totalSeconds > 0) {
            secondsRemaining.toFloat() / totalSeconds.toFloat()
        } else {
            0f
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper Mode Banner
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CURRENT TASK FOCUS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Text(
                            text = activeModeName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Text(
                        text = currentSubject.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Animated Timer Countdown circle
        item {
            Box(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                ProgressRing(
                    progress = progressRatio,
                    size = 220.dp,
                    strokeWidth = 12.dp,
                    primaryColor = if (timerState == TimerState.RUNNING) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    secondaryColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (timerState) {
                            TimerState.RUNNING -> "FOCUS TIME ACTIVE"
                            TimerState.PAUSED -> "SESSION PAUSED"
                            TimerState.STOPPED -> "TAP START"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Control Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // STOP/RESET
                if (timerState != TimerState.STOPPED) {
                    IconButton(
                        onClick = { viewModel.stopTimer() },
                        modifier = Modifier
                            .size(54.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // PLAY/PAUSE
                val controllerBgColor by animateColorAsState(
                    targetValue = if (timerState == TimerState.RUNNING) MaterialTheme.colorScheme.error 
                                  else MaterialTheme.colorScheme.secondary,
                    label = "PlayPauseColor"
                )

                IconButton(
                    onClick = {
                        when (timerState) {
                            TimerState.RUNNING -> viewModel.pauseTimer()
                            TimerState.PAUSED -> viewModel.resumeTimer()
                            TimerState.STOPPED -> viewModel.startTimer()
                        }
                    },
                    modifier = Modifier
                        .size(76.dp)
                        .background(controllerBgColor, CircleShape)
                ) {
                    Icon(
                        imageVector = if (timerState == TimerState.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Control Play Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // Focus Subjects and Modes Selector
        item {
            if (timerState == TimerState.STOPPED) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CHOOSE SESSION BLOCK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("25/5" to "25 Min", "50/10" to "50 Min", "CUSTOM_5" to "5 Min", "CUSTOM_1" to "1 Min").forEach { (type, name) ->
                            val selected = (type == "25/5" && activeModeName.contains("25/5")) ||
                                           (type == "50/10" && activeModeName.contains("50/10")) ||
                                           (type == "CUSTOM_5" && activeModeName.contains("Fast Session")) ||
                                           (type == "CUSTOM_1" && activeModeName.contains("Rapid Sprint"))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) 
                                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        color = if (selected) MaterialTheme.colorScheme.secondary 
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setTimerMode(type) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Subject selector list
        item {
            if (timerState == TimerState.STOPPED) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SELECT TOPIC / SUBJECT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Deep Work", "Revision", "Assignments", "Reading").forEach { subject ->
                            val isSelected = currentSubject == subject
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) 
                                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.2.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.secondary 
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.timerSelectedSubject = subject }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = subject,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Subject Textfield
        item {
            if (timerState == TimerState.STOPPED) {
                OutlinedTextField(
                    value = viewModel.timerSelectedSubject,
                    onValueChange = { viewModel.timerSelectedSubject = it },
                    label = { Text("Or Type Custom Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )
            }
        }

        // Focus Session Notes
        item {
            OutlinedTextField(
                value = viewModel.timerNotes,
                onValueChange = { viewModel.timerNotes = it },
                label = { Text("Log Focus Session Notes") },
                placeholder = { Text("e.g. Worked on Chapter 3 Exercises") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
            )
        }
    }
}
