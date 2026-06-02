package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcademicItem
import com.example.ui.components.GlassCard
import com.example.ui.components.ProgressRing
import com.example.ui.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun AcademicScreen(
    viewModel: TrackerViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    val subjects by viewModel.subjects.collectAsState()
    val academicItems by viewModel.academicItems.collectAsState()

    // Creation States
    var subjectNameInput by remember { mutableStateOf("") }
    var credsInput by remember { mutableStateOf("3") }
    
    var itemNameInput by remember { mutableStateOf("") }
    var selectedItemType by remember { mutableStateOf("EXAM") } // EXAM vs ASSIGNMENT
    var selectedSubjectName by remember { mutableStateOf("General") }
    var deadlineDaysOffset by remember { mutableStateOf(3L) } // default 3 days ahead

    // Countdown Logic
    val upcomingExam = remember(academicItems) {
        val now = System.currentTimeMillis()
        academicItems
            .asSequence()
            .filter { !it.isCompleted && it.deadlineMillis > now && it.type == "EXAM" }
            .minByOrNull { it.deadlineMillis }
    }

    val countdownText = remember(upcomingExam) {
        if (upcomingExam == null) {
            "No pending exams with future deadlines."
        } else {
            val delta = upcomingExam.deadlineMillis - System.currentTimeMillis()
            val days = TimeUnit.MILLISECONDS.toDays(delta)
            val hours = TimeUnit.MILLISECONDS.toHours(delta) % 24
            
            if (days > 0) {
                "${upcomingExam.name.uppercase(Locale.US)} IN $days DAYS, $hours HRS"
            } else {
                "${upcomingExam.name.uppercase(Locale.US)} IN $hours HOURS!"
            }
        }
    }

    // Dynamic Semester Progress Based on Completed Items
    val semesterProgress = remember(academicItems) {
        if (academicItems.isEmpty()) 0f
        else {
            val completed = academicItems.count { it.isCompleted }.toFloat()
            completed / academicItems.size.toFloat()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Core Header
        item {
            Column {
                Text(
                    text = "ACADEMIC MANAGEMENT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Semester & Syllabus Control",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Countdown Ticker Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Alert",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "EXAM COUNTDOWN TIMER",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            countdownText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // GPA Goals and Semester Progress Bar
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "SEMESTER TRACKER",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            "Target Goal: 4.0 GPA",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        LinearProgressIndicator(
                            progress = { semesterProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Semester Completed: ${(semesterProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    ProgressRing(
                        progress = semesterProgress,
                        size = 54.dp,
                        strokeWidth = 5.dp,
                        primaryColor = MaterialTheme.colorScheme.secondary,
                        centerLabel = "${(semesterProgress * 100).toInt()}%"
                    )
                }
            }
        }

        // Create Course / Subject
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    "ADD COURSE / SUBJECT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = subjectNameInput,
                        onValueChange = { subjectNameInput = it },
                        label = { Text("Course Chemistry/Maths") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    OutlinedTextField(
                        value = credsInput,
                        onValueChange = { credsInput = it },
                        label = { Text("Credits") },
                        modifier = Modifier.width(76.dp),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                    Button(
                        onClick = {
                            if (subjectNameInput.isNotBlank()) {
                                viewModel.addSubject(
                                    subjectNameInput,
                                    credsInput.toIntOrNull() ?: 3,
                                    "#3B82F6"
                                )
                                subjectNameInput = ""
                                keyboardController?.hide()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save")
                    }
                }

                // Show Current Course Badges
                if (subjects.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(subjects) { sub ->
                            InputChip(
                                selected = false,
                                onClick = { viewModel.deleteSubject(sub.id) },
                                label = { Text("${sub.name} (${sub.creditHours} cr)") },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "delete", modifier = Modifier.size(12.dp)) }
                            )
                        }
                    }
                }
            }
        }

        // Add Academic Item (Exam / Assignment)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "ADD ASSESSMENT (EXAM / ASSIGNMENT)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("EXAM", "ASSIGNMENT").forEach { type ->
                        val active = selectedItemType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    color = if (active) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) 
                                            else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (active) MaterialTheme.colorScheme.secondary 
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedItemType = type }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = itemNameInput,
                    onValueChange = { itemNameInput = it },
                    label = { Text("Syllabus Title (e.g., Midterm mechanics)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.secondary)
                )

                // Quick course selection dropdown
                if (subjects.isNotEmpty()) {
                    Column {
                        Text("Course Attribution:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(subjects) { sub ->
                                val selected = selectedSubjectName == sub.name
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedSubjectName = sub.name },
                                    label = { Text(sub.name) }
                                )
                            }
                        }
                    }
                }

                // Quick-set offset deadline chips
                Column {
                    Text("Duration/Deadline Offset:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2L to "2 Days", 5L to "5 Days", 7L to "1 Week", 14L to "2 Weeks").forEach { (offset, label) ->
                            val selected = deadlineDaysOffset == offset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (selected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.secondary 
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { deadlineDaysOffset = offset }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (itemNameInput.isNotBlank()) {
                            val futureMills = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(deadlineDaysOffset)
                            viewModel.addAcademicItem(
                                type = selectedItemType,
                                subjectName = selectedSubjectName,
                                name = itemNameInput,
                                deadlineMillis = futureMills
                            )
                            itemNameInput = ""
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Assessment Task")
                }
            }
        }

        // Assessment Tasks List section
        item {
            Text(
                "PENDING ASSESSMENT TASKS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (academicItems.isEmpty()) {
            item {
                Text(
                    "No pending assessments recorded. Add assessments above.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        items(academicItems) { item ->
            val sdf = SimpleDateFormat("MMM - dd, hh:mm a", Locale.US)
            val dateFormatted = sdf.format(Date(item.deadlineMillis))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (item.isCompleted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f) 
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (item.isCompleted) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) 
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(if (item.isCompleted) MaterialTheme.colorScheme.secondary else Color.Transparent, CircleShape)
                            .border(1.5.dp, if (item.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                            .clickable { viewModel.toggleAcademicItem(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.isCompleted) {
                            Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.type,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = if (item.type == "EXAM") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .background(
                                        color = (if (item.type == "EXAM") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Course: ${item.subjectName} | Due: $dateFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.deleteAcademicItem(item.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
