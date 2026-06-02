package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    borderWidth: Dp = 1.dp,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .border(borderWidth, borderColor, shape)
            .clip(shape),
        color = backgroundColor,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}


@Composable
fun ProgressRing(
    progress: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 10.dp,
    primaryColor: Color = MaterialTheme.colorScheme.secondary,
    secondaryColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    centerLabel: String = ""
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000),
        label = "ProgressRing"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw background circle
            drawCircle(
                color = secondaryColor,
                radius = (size / 2 - strokeWidth / 2).toPx(),
                style = Stroke(width = strokeWidth.toPx())
            )
            // Draw progress circle
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(
                    width = (size - strokeWidth).toPx(),
                    height = (size - strokeWidth).toPx()
                ),
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = strokeWidth.toPx() / 2,
                    y = strokeWidth.toPx() / 2
                )
            )
        }
        if (centerLabel.isNotEmpty()) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun StreakBadge(
    streakValue: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(WarningOrange.copy(alpha = 0.2f), WarningOrange.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, WarningOrange.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Whatshot,
            contentDescription = "Streak Fire",
            tint = WarningOrange,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "$streakValue Days",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = WarningOrange
            )
        )
    }
}

/**
 * Habit Heatmap corresponding to the last 28 days (4 weeks of 7 days).
 * Grid block color shows the density of habits completed on each day.
 */
@Composable
fun CustomHeatmap(
    habitHistories: List<com.example.data.model.HabitHistory>,
    habitsCount: Int,
    modifier: Modifier = Modifier
) {
    val totalDaysToShow = 28
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    // Generate dates list for the last 28 days
    val dates = remember(habitHistories, habitsCount) {
        val list = mutableListOf<Pair<String, Int>>() // DateString to completions count
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -(totalDaysToShow - 1))
        
        for (i in 0 until totalDaysToShow) {
            val dStr = sdf.format(cal.time)
            val completions = habitHistories.count { it.dateString == dStr }
            list.add(dStr to completions)
            cal.add(Calendar.DATE, 1)
        }
        list
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HABIT COMPLETION HEATMAP (LAST 28 DAYS)",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 10.dp)
        )
        
        // Show 4 weeks representing 28 days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (week in 0 until 4) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (day in 0 until 7) {
                        val index = (week * 7) + day
                        val datePair = dates.getOrNull(index)
                        val count = datePair?.second ?: 0
                        
                        // Calculate color based on percentage
                        val percent = if (habitsCount > 0) count.toFloat() / habitsCount else 0f
                        val blockColor = when {
                            percent == 0f -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            percent <= 0.3f -> SuccessGreen.copy(alpha = 0.25f)
                            percent <= 0.6f -> SuccessGreen.copy(alpha = 0.55f)
                            percent <= 0.8f -> SuccessGreen.copy(alpha = 0.8f)
                            else -> SuccessGreen // Glowing fully emerald green
                        }

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(blockColor, RoundedCornerShape(4.dp))
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }
            }
        }
        
        // Heatmap Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Less",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(2.dp))
            Box(Modifier.size(10.dp).background(SuccessGreen.copy(alpha = 0.25f), RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(2.dp))
            Box(Modifier.size(10.dp).background(SuccessGreen.copy(alpha = 0.55f), RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(2.dp))
            Box(Modifier.size(10.dp).background(SuccessGreen.copy(alpha = 0.8f), RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(2.dp))
            Box(Modifier.size(10.dp).background(SuccessGreen, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "More",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Vertical bar chart mapping hours studied over the past 7 days.
 */
@Composable
fun WeeklyStudyChart(
    sessions: List<com.example.data.model.StudySession>,
    modifier: Modifier = Modifier
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    // Group study seconds by day of week for the current week
    val rawDailyDurations = remember(sessions) {
        val calendar = Calendar.getInstance()
        val map = mutableMapOf<String, Long>()
        daysOfWeek.forEach { map[it] = 0L }

        // Find how many seconds each day had
        sessions.forEach { session ->
            calendar.timeInMillis = session.dateMillis
            val dayNum = calendar.get(Calendar.DAY_OF_WEEK) // Sunday=1, Monday=2...
            val label = when (dayNum) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                else -> "Mon"
            }
            map[label] = (map[label] ?: 0L) + session.durationSeconds
        }
        map
    }

    val totalHours = rawDailyDurations.values.sum() / 3600f
    val maxSecondsOfDay = rawDailyDurations.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "WEEKLY STUDY TIME",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Text(
                    text = String.format(Locale.US, "%.1f Hours", totalHours),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Beautiful Bar layout with animations
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            daysOfWeek.forEach { dayName ->
                val seconds = rawDailyDurations[dayName] ?: 0L
                val hours = seconds / 3600f
                val ratio = seconds.toFloat() / maxSecondsOfDay.toFloat()
                
                val animRatio by animateFloatAsState(
                    targetValue = ratio.coerceAtLeast(0.04f), // minor stub so always visible
                    animationSpec = tween(durationMillis = 800),
                    label = "BarChartHeight"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    if (hours > 0f) {
                        Text(
                            text = String.format(Locale.US, "%.1f", hours),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(animRatio)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                    )
                                ),
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
