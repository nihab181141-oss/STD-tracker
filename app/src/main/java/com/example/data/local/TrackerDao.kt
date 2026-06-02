package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {

    // --- Study Sessions ---
    @Query("SELECT * FROM study_sessions ORDER BY dateMillis DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)


    // --- Habits ---
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Int): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Int)


    // --- Habit History ---
    @Query("SELECT * FROM habit_history")
    fun getAllHabitHistories(): Flow<List<HabitHistory>>

    @Query("SELECT * FROM habit_history WHERE habitId = :habitId AND dateString = :dateString LIMIT 1")
    suspend fun getCompletion(habitId: Int, dateString: String): HabitHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(history: HabitHistory)

    @Query("DELETE FROM habit_history WHERE habitId = :habitId AND dateString = :dateString")
    suspend fun deleteCompletion(habitId: Int, dateString: String)


    // --- Academic Subjects ---
    @Query("SELECT * FROM academic_subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<AcademicSubject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: AcademicSubject)

    @Query("DELETE FROM academic_subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Int)


    // --- Academic Items (Exams/Assignments) ---
    @Query("SELECT * FROM academic_items ORDER BY deadlineMillis ASC")
    fun getAllAcademicItems(): Flow<List<AcademicItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicItem(item: AcademicItem)

    @Update
    suspend fun updateAcademicItem(item: AcademicItem)

    @Query("DELETE FROM academic_items WHERE id = :id")
    suspend fun deleteAcademicItemById(id: Int)


    // --- Goals ---
    @Query("SELECT * FROM goals ORDER BY deadlineMillis ASC")
    fun getAllGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)


    // --- AI Insights ---
    @Query("SELECT * FROM ai_insights WHERE type = :type LIMIT 1")
    fun getInsight(type: String = "DAILY_SUGGESTIONS"): Flow<AiInsight?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: AiInsight)

    // --- Notifications ---
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Int)

    @Query("DELETE FROM app_notifications")
    suspend fun clearAllNotifications()

    // --- Notification Settings ---
    @Query("SELECT * FROM notification_settings WHERE id = :id LIMIT 1")
    suspend fun getNotificationSettings(id: String = "USER_PREF"): NotificationSetting?

    @Query("SELECT * FROM notification_settings WHERE id = :id LIMIT 1")
    fun getNotificationSettingsFlow(id: String = "USER_PREF"): Flow<NotificationSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationSetting(setting: NotificationSetting)

    // --- Study Groups ---
    @Query("SELECT * FROM study_groups ORDER BY createdAt DESC")
    fun getAllStudyGroups(): Flow<List<StudyGroup>>

    @Query("SELECT * FROM study_groups WHERE id = :groupId LIMIT 1")
    suspend fun getStudyGroupById(groupId: Int): StudyGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyGroup(group: StudyGroup): Long

    @Query("DELETE FROM study_groups WHERE id = :id")
    suspend fun deleteStudyGroupById(id: Int)

    // --- Study Group Members ---
    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY xp DESC")
    fun getGroupMembers(groupId: Int): Flow<List<GroupMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMember(member: GroupMember)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun clearGroupMembers(groupId: Int)

    // --- Group Messages ---
    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getGroupMessages(groupId: Int): Flow<List<GroupMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMessage(message: GroupMessage)
}

