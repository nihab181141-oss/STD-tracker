package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val durationSeconds: Long,
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCustom: Boolean = false,
    val streak: Int = 0,
    val lastCompletedDateString: String = "", // e.g., "2026-06-01"
    val createdDateString: String = ""
)

@Entity(tableName = "habit_history")
data class HabitHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val dateString: String // e.g., "2026-06-01"
)

@Entity(tableName = "academic_subjects")
data class AcademicSubject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val creditHours: Int = 3,
    val colorHex: String = "#FF6161"
)

@Entity(tableName = "academic_items")
data class AcademicItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "EXAM" or "ASSIGNMENT"
    val subjectName: String,
    val name: String,
    val deadlineMillis: Long,
    val isCompleted: Boolean = false,
    val gradeReceived: Double? = null
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "DAILY", "WEEKLY", "MONTHLY", "SEMESTER", "YEARLY"
    val name: String,
    val progressPercent: Int = 0,
    val deadlineMillis: Long,
    val isCompleted: Boolean = false
)

// Represents cached AI recommendations
@Entity(tableName = "ai_insights")
data class AiInsight(
    @PrimaryKey val type: String = "DAILY_SUGGESTIONS", // single row
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Helper object for level calculation
object GamificationEngine {
    data class LevelInfo(
        val level: Int,
        val title: String,
        val xpForCurrentLevel: Int,
        val xpRequiredForNextLevel: Int,
        val prevLevelMaxXp: Int
    )

    fun calculateLevel(totalXp: Int): LevelInfo {
        val levels = listOf(
            1 to "Rookie" to 1000,
            2 to "Learner" to 2000,
            3 to "Focused" to 3500,
            4 to "Achiever" to 5000,
            5 to "Elite" to 7500,
            6 to "Master" to 10000,
            7 to "Champion" to 13500,
            8 to "Legend" to 18000,
            9 to "Titan" to 24000,
            10 to "Supreme" to Int.MAX_VALUE
        )

        var cumulative = 0
        var currentLevel = 1
        var currentTitle = "Rookie"
        var nextLevelRequired = 1000
        var prevLevelMax = 0

        for (i in levels.indices) {
            val levelNum = levels[i].first.first
            val title = levels[i].first.second
            val maxVal = levels[i].second
            
            if (totalXp >= maxVal) {
                prevLevelMax = maxVal
                continue
            } else {
                currentLevel = levelNum
                currentTitle = title
                nextLevelRequired = maxVal
                break
            }
        }

        return LevelInfo(
            level = currentLevel,
            title = currentTitle,
            xpForCurrentLevel = totalXp - prevLevelMax,
            xpRequiredForNextLevel = nextLevelRequired - prevLevelMax,
            prevLevelMaxXp = prevLevelMax
        )
    }

    // Achievements calculation
    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val progress: Float, // 0f to 1f
        val isUnlocked: Boolean,
        val badgeIcon: String // Name of graphic
    )

    fun getAchievements(
        sessionsCount: Int,
        habitsCompletedCount: Int,
        itemsCompletedCount: Int,
        goalsCompletedCount: Int,
        totalStudyHours: Float
    ): List<Achievement> {
        return listOf(
            Achievement(
                "first_step",
                "First Step",
                "Complete your first focus study session",
                if (sessionsCount >= 1) 1f else 0f,
                sessionsCount >= 1,
                "ic_first_step"
            ),
            Achievement(
                "deep_diver",
                "Deep Diver",
                "Amass 10 hours of focused study time",
                (totalStudyHours / 10f).coerceIn(0f, 1f),
                totalStudyHours >= 10f,
                "ic_deep_diver"
            ),
            Achievement(
                "habit_master",
                "Habit Architect",
                "Tick off 30 habit completions",
                (habitsCompletedCount / 30f).coerceIn(0f, 1f),
                habitsCompletedCount >= 30,
                "ic_habit_master"
            ),
            Achievement(
                "exam_conqueror",
                "Academic Conqueror",
                "Finish 5 exams or assignments",
                (itemsCompletedCount / 5f).coerceIn(0f, 1f),
                itemsCompletedCount >= 5,
                "ic_exam_conqueror"
            ),
            Achievement(
                "goal_slayer",
                "Visionary",
                "Accomplish 5 personal goals",
                (goalsCompletedCount / 5f).coerceIn(0f, 1f),
                goalsCompletedCount >= 5,
                "ic_goal_slayer"
            ),
            Achievement(
                "grindset",
                "Unyielding Elite",
                "Amass 50 hours of total study time",
                (totalStudyHours / 50f).coerceIn(0f, 1f),
                totalStudyHours >= 50f,
                "ic_grindset"
            )
        )
    }
}

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val type: String, // "TIMER", "HABIT", "DEADLINE"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "notification_settings")
data class NotificationSetting(
    @PrimaryKey val id: String = "USER_PREF",
    val timerReminders: Boolean = true,
    val habitReminders: Boolean = true,
    val deadlineAlerts: Boolean = true,
    val silentMode: Boolean = false
)

@Entity(tableName = "study_groups")
data class StudyGroup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val groupCode: String,
    val createdBy: String = "You",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_members")
data class GroupMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val name: String,
    val avatarSeed: String,
    val studyHours: Float = 0.0f,
    val xp: Int = 0,
    val isCurrentUser: Boolean = false
)

@Entity(tableName = "group_messages")
data class GroupMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val groupId: Int,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSystemMessage: Boolean = false
)
