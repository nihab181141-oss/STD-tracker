package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.TrackerDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AppRepository(private val trackerDao: TrackerDao) {

    // --- Study Sessions ---
    val allSessions: Flow<List<StudySession>> = trackerDao.getAllSessions()

    suspend fun addStudySession(subject: String, durationSeconds: Long, notes: String) {
        trackerDao.insertSession(
            StudySession(
                subject = subject,
                durationSeconds = durationSeconds,
                notes = notes,
                dateMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSession(id: Int) {
        trackerDao.deleteSessionById(id)
    }


    // --- Habits ---
    val allHabits: Flow<List<Habit>> = trackerDao.getAllHabits()
    val allHabitHistories: Flow<List<HabitHistory>> = trackerDao.getAllHabitHistories()

    suspend fun prepopulateHabitsIfEmpty() {
        // Run check
        val current = trackerDao.getAllHabits().firstOrNull()
        if (current.isNullOrEmpty()) {
            val defaultList = listOf(
                "Wake Up Early",
                "Fajr Prayer",
                "Exercise",
                "Reading",
                "Deep Work",
                "Water Intake",
                "Sleep Early",
                "Revision",
                "No Social Media",
                "Quran Reading"
            )
            val todayStr = getTodayString()
            defaultList.forEach { name ->
                trackerDao.insertHabit(
                    Habit(
                        name = name,
                        isCustom = false,
                        streak = 0,
                        lastCompletedDateString = "",
                        createdDateString = todayStr
                    )
                )
            }
        }
    }

    suspend fun addCustomHabit(name: String) {
        val todayStr = getTodayString()
        trackerDao.insertHabit(
            Habit(
                name = name,
                isCustom = true,
                streak = 0,
                lastCompletedDateString = "",
                createdDateString = todayStr
            )
        )
    }

    suspend fun toggleHabitCompletion(habitId: Int, dateStr: String): Boolean {
        val habit = trackerDao.getHabitById(habitId) ?: return false
        val existingCompletion = trackerDao.getCompletion(habitId, dateStr)

        if (existingCompletion != null) {
            // Uncomplete
            trackerDao.deleteCompletion(habitId, dateStr)
            
            // Adjust streak (decrement/reset simple calculation)
            val prevStreak = habit.streak
            val updatedHabit = habit.copy(
                streak = (prevStreak - 1).coerceAtLeast(0),
                lastCompletedDateString = "" // Can simplify reset
            )
            trackerDao.updateHabit(updatedHabit)
            return false
        } else {
            // Complete!
            trackerDao.insertCompletion(HabitHistory(habitId = habitId, dateString = dateStr))
            
            // Calculate new streak
            val lastComp = habit.lastCompletedDateString
            var newStreak = habit.streak
            if (lastComp.isEmpty()) {
                newStreak = 1
            } else {
                val yesterdayStr = getYesterdayString(dateStr)
                if (lastComp == yesterdayStr) {
                    newStreak += 1
                } else if (lastComp != dateStr) {
                    newStreak = 1
                }
            }
            
            val updatedHabit = habit.copy(
                streak = newStreak,
                lastCompletedDateString = dateStr
            )
            trackerDao.updateHabit(updatedHabit)
            return true
        }
    }

    suspend fun deleteHabit(id: Int) {
        trackerDao.deleteHabitById(id)
    }


    // --- Academic Management ---
    val allSubjects: Flow<List<AcademicSubject>> = trackerDao.getAllSubjects()
    val allAcademicItems: Flow<List<AcademicItem>> = trackerDao.getAllAcademicItems()

    suspend fun addSubject(name: String, creditHours: Int, colorHex: String) {
        trackerDao.insertSubject(
            AcademicSubject(
                name = name,
                creditHours = creditHours,
                colorHex = colorHex
            )
        )
    }

    suspend fun deleteSubject(id: Int) {
        trackerDao.deleteSubjectById(id)
    }

    suspend fun addAcademicItem(type: String, subjectName: String, name: String, deadlineMillis: Long) {
        trackerDao.insertAcademicItem(
            AcademicItem(
                type = type,
                subjectName = subjectName,
                name = name,
                deadlineMillis = deadlineMillis,
                isCompleted = false
            )
        )
    }

    suspend fun toggleAcademicItemCompletion(item: AcademicItem) {
        trackerDao.updateAcademicItem(item.copy(isCompleted = !item.isCompleted))
    }

    suspend fun updateAcademicItemGrade(item: AcademicItem, grade: Double?) {
        trackerDao.updateAcademicItem(item.copy(gradeReceived = grade))
    }

    suspend fun deleteAcademicItem(id: Int) {
        trackerDao.deleteAcademicItemById(id)
    }


    // --- Goals Management ---
    val allGoals: Flow<List<Goal>> = trackerDao.getAllGoals()

    suspend fun addGoal(type: String, name: String, deadlineMillis: Long) {
        trackerDao.insertGoal(
            Goal(
                type = type,
                name = name,
                deadlineMillis = deadlineMillis,
                progressPercent = 0,
                isCompleted = false
            )
        )
    }

    suspend fun updateGoalProgress(goal: Goal, progress: Int) {
        trackerDao.updateGoal(
            goal.copy(
                progressPercent = progress,
                isCompleted = progress >= 100
            )
        )
    }

    suspend fun toggleGoalCompletion(goal: Goal) {
        val completed = !goal.isCompleted
        trackerDao.updateGoal(
            goal.copy(
                isCompleted = completed,
                progressPercent = if (completed) 100 else 0
            )
        )
    }

    suspend fun deleteGoal(id: Int) {
        trackerDao.deleteGoalById(id)
    }


    // --- AI Suggestions (Gemini integration via REST API) ---
    val cachedInsight: Flow<AiInsight?> = trackerDao.getInsight()

    suspend fun generateAiRecommendations(
        sessionsList: List<StudySession>,
        habitsList: List<Habit>,
        habitHistoriesList: List<HabitHistory>,
        goalsList: List<Goal>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            "MY_GEMINI_API_KEY"
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val localResult = generateOfflineSuggestions(sessionsList, habitsList, habitHistoriesList, goalsList)
            trackerDao.insertInsight(AiInsight(text = localResult))
            return@withContext localResult
        }

        // Aggregate statistics to send to AI
        val totalSessions = sessionsList.size
        val totalHours = sessionsList.sumOf { it.durationSeconds } / 3600.0
        val subjectStats = sessionsList.groupBy { it.subject }
            .mapValues { entry -> String.format(Locale.US, "%.1f hrs", entry.value.sumOf { it.durationSeconds } / 3600.0) }
        
        val totalHabits = habitsList.size
        val totalHabitComp = habitHistoriesList.size
        val habitStreaks = habitsList.associate { it.name to it.streak }

        val pendingGoals = goalsList.filter { !it.isCompleted }.map { it.name }
        val completedGoalsCount = goalsList.filter { it.isCompleted }.size

        val analyticsSummary = """
            Student Analytics Data:
            - Focus Sessions Completed: $totalSessions
            - Total Study Hours: ${String.format(Locale.US, "%.1f", totalHours)}
            - Study hours per subject: $subjectStats
            - Habits Active: $totalHabits
            - Total Habits Checked in History: $totalHabitComp
            - Habit streaks: $habitStreaks
            - Core goals completed: $completedGoalsCount
            - Unfinished goals: $pendingGoals
        """.trimIndent()

        val prompt = """
            You are a highly premium AI Academic and Habit Performance Coach called STD-Coach. 
            Analyze the following student metrics and provide an ultra-premium, actionable analysis with:
            1. An executive summary rating their performance this week (Grade A to F, color tags, high encouragement).
            2. 2-3 specific, weak habits or missing consistency issues they should fix immediately.
            3. A customized recommended pomodoro planning structure for tomorrow.
            
            Keep the content highly structured, engaging, and professional (minimalist Notion-inspired tone, Duolingo-level motivation). Be concise but high fidelity.
            
            $analyticsSummary
        """.trimIndent()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            // systemInstruction setup
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "You are STD-Coach, an expert premium academic performance counselor. You communicate with supreme clarity and exceptional encouragement.")
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)
        val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("GeminiAPI", "Failed calls: ${response.code} - ${response.message}")
                    val backupResult = generateOfflineSuggestions(sessionsList, habitsList, habitHistoriesList, goalsList) + "\n\n*(Showing local engine recommendations - Network Sync Error)*"
                    trackerDao.insertInsight(AiInsight(text = backupResult))
                    return@withContext backupResult
                }
                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val textResponse = parts.getJSONObject(0).getString("text")

                trackerDao.insertInsight(AiInsight(text = textResponse))
                return@withContext textResponse
            }
        } catch (e: Exception) {
            Log.e("GeminiAPI", "Error executing request", e)
            val backupResult = generateOfflineSuggestions(sessionsList, habitsList, habitHistoriesList, goalsList) + "\n\n*(Showing local engine recommendations - Timeout or connection issue)*"
            trackerDao.insertInsight(AiInsight(text = backupResult))
            return@withContext backupResult
        }
    }


    // --- Local Recommendation Engine as Robust On-Device Fallback ---
    private fun generateOfflineSuggestions(
        sessionsList: List<StudySession>,
        habitsList: List<Habit>,
        habitHistoriesList: List<HabitHistory>,
        goalsList: List<Goal>
    ): String {
        val totalSessions = sessionsList.size
        val totalHours = sessionsList.sumOf { it.durationSeconds } / 3600.0
        val longestStreak = habitsList.maxOfOrNull { it.streak } ?: 0

        val feedback = StringBuilder()
        feedback.append("### 🔱 **STD-Coach Local Performance Analysis**\n\n")
        feedback.append("✨ Welcome! This is your customized offline analysis. Here is what your current performance data reveals:\n\n")

        // 1. Study Hours Diagnosis
        feedback.append("🏆 **Performance Rating: Grade ")
        when {
            totalHours >= 15 -> feedback.append("A+ (Elite)**\n\nOutstanding work! You've logged ${String.format(Locale.US, "%.1f", totalHours)} hours of deep work. Your consistency is placing you in the top 5% of active students. Keep pushing!")
            totalHours >= 8 -> feedback.append("B+ (Steady Builder)**\n\nYou are building deep-focus inertia with ${String.format(Locale.US, "%.1f", totalHours)} focus study hours. Tighten your Pomodoro routines to optimize time.")
            totalHours >= 3 -> feedback.append("C (Ramp Up Stage)**\n\nYou are loging focus hours. Consistent session habits are required. Try studying at the same time each day.")
            else -> feedback.append("D- (Focus Inertia Alert)**\n\nYour study timer logged less than 3 hours. Time to break focus barriers with a simple 25-minute study block tonight.")
        }
        feedback.append("\n\n")

        // 2. Habits Analysis
        feedback.append("⚡ **Habit Streak & Rhythm Insights:**\n")
        if (longestStreak > 0) {
            feedback.append("- You have a **$longestStreak-day active streak** going on! This momentum is incredibly powerful. Protect this streak at all costs today.\n")
        } else {
            feedback.append("- No active habit streaks found yet. Toggle completions in the **Habit Tracker** to kickstart your first streak streak today.\n")
        }

        if (habitsList.any { it.name == "No Social Media" && it.streak == 0 }) {
            feedback.append("- **Friction point detected:** 'No Social Media' habit is empty today. Consider installing an app blocker to protect focus blocks.\n")
        }
        if (habitsList.any { it.name == "Deep Work" && it.streak == 0 }) {
            feedback.append("- **Opportunity identified:** Kickstart 'Deep Work' habits immediately before your focus session today to build momentum.\n")
        }
        feedback.append("\n")

        // 3. Recommended Actions
        feedback.append("📈 **My Blueprint Recommendations for Tomorrow:**\n")
        feedback.append("1. **Block Morning Hours:** Complete an early block of 50/10 Pomodoro work before 10 AM to secure 80% of daily productivity early.\n")
        feedback.append("2. **Hydration Boost:** Aim to tick off 'Water Intake' habit first thing in the morning.\n")
        feedback.append("3. **Goal Tracking:** Review your current pending goals and break down major milestones into bite-sized tasks.\n\n")
        feedback.append("_You've got this! Execute tomorrow's blueprint to build elite level rankings today._")

        return feedback.toString()
    }


    // --- Notifications Repository Hooks ---
    val allNotifications: Flow<List<AppNotification>> = trackerDao.getAllNotifications()
    val notificationSettings: Flow<NotificationSetting?> = trackerDao.getNotificationSettingsFlow()

    suspend fun getNotificationSettings(): NotificationSetting {
        return trackerDao.getNotificationSettings("USER_PREF") ?: NotificationSetting().also {
            trackerDao.insertNotificationSetting(it)
        }
    }

    suspend fun updateNotificationSettings(settings: NotificationSetting) {
        trackerDao.insertNotificationSetting(settings)
    }

    suspend fun markNotificationAsRead(id: Int) {
        trackerDao.markNotificationAsRead(id)
    }

    suspend fun deleteNotification(id: Int) {
        trackerDao.deleteNotificationById(id)
    }

    suspend fun clearAllNotifications() {
        trackerDao.clearAllNotifications()
    }

    // --- Context-Aware Notification Generator ---
    suspend fun generateContextNotifications() {
        val settings = getNotificationSettings()
        if (settings.silentMode) return

        val todayStr = getTodayString()
        val now = System.currentTimeMillis()

        // 1. Check approaching deadlines (next 3 days)
        if (settings.deadlineAlerts) {
            val items = trackerDao.getAllAcademicItems().firstOrNull() ?: emptyList()
            items.forEach { item ->
                if (!item.isCompleted) {
                    val diff = item.deadlineMillis - now
                    val daysRemaining = (diff / (1000 * 60 * 60 * 24)).toInt()
                    if (daysRemaining in 0..3) {
                        val messageText = when (daysRemaining) {
                            0 -> "DEADLINE TODAY: Your ${item.type.lowercase()} for '${item.name}' is due within 24 hours!"
                            1 -> "URGENT ALERT: Your ${item.type.lowercase()} for '${item.name}' is due tomorrow!"
                            else -> "Approaching Academic Deadline: '${item.name}' belongs to subject ${item.subjectName} and is due in $daysRemaining days."
                        }
                        
                        val notifications = trackerDao.getAllNotifications().firstOrNull() ?: emptyList()
                        if (notifications.none { it.type == "DEADLINE" && it.message.contains(item.name) && (now - it.timestamp) < 43200000 }) {
                            trackerDao.insertNotification(
                                AppNotification(
                                    title = "⚠️ Academic Deadline Alert",
                                    message = messageText,
                                    type = "DEADLINE"
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Check Habit consistency today
        if (settings.habitReminders) {
            val habits = trackerDao.getAllHabits().firstOrNull() ?: emptyList()
            habits.forEach { habit ->
                val alreadyCompleted = trackerDao.getCompletion(habit.id, todayStr) != null
                if (!alreadyCompleted) {
                    val streakText = if (habit.streak > 0) "Protect your ${habit.streak}-day streak! 🔥" else "Start an Elite streak today!"
                    val messageText = "Daily Habit Reminder: '${habit.name}' is pending. $streakText"
                    
                    val notifications = trackerDao.getAllNotifications().firstOrNull() ?: emptyList()
                    if (notifications.none { it.type == "HABIT" && it.message.contains(habit.name) && (now - it.timestamp) < 43200000 }) {
                        trackerDao.insertNotification(
                            AppNotification(
                                title = "⚡ Habit Consistency Check",
                                message = messageText,
                                type = "HABIT"
                            )
                        )
                    }
                }
            }
        }

        // 3. Recommended Study Session prompt based on time of day
        if (settings.timerReminders) {
            val notifications = trackerDao.getAllNotifications().firstOrNull() ?: emptyList()
            if (notifications.none { it.type == "TIMER" && (now - it.timestamp) < 43200000 }) {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val timeLabel = when (hour) {
                    in 5..11 -> "Morning Productivity window"
                    in 12..17 -> "Afternoon Study block"
                    else -> "Evening Focus session"
                }
                trackerDao.insertNotification(
                    AppNotification(
                        title = "⏱️ Time to Study!",
                        message = "Your scheduled $timeLabel is open. Fire up a Pomodoro sprint to climb the Apex Student ranks.",
                        type = "TIMER"
                    )
                )
            }
        }
    }

    // --- Study Groups Hooks & Methods ---
    val allStudyGroups: Flow<List<StudyGroup>> = trackerDao.getAllStudyGroups()

    fun getGroupMembers(groupId: Int): Flow<List<GroupMember>> = trackerDao.getGroupMembers(groupId)
    fun getGroupMessages(groupId: Int): Flow<List<GroupMessage>> = trackerDao.getGroupMessages(groupId)

    suspend fun createStudyGroup(name: String, description: String, groupCode: String): Long {
        val groupId = trackerDao.insertStudyGroup(
            StudyGroup(name = name, description = description, groupCode = groupCode)
        ).toInt()

        // Insert current user as founding member
        trackerDao.insertGroupMember(
            GroupMember(groupId = groupId, name = "You", avatarSeed = "user_you", studyHours = 0f, xp = 0, isCurrentUser = true)
        )

        // Insert a welcome system message
        trackerDao.insertGroupMessage(
            GroupMessage(groupId = groupId, senderName = "System", message = "Study group '$name' was successfully created! Invite code is '$groupCode'.", isSystemMessage = true)
        )
        return groupId.toLong()
    }

    suspend fun joinStudyGroup(groupCode: String): StudyGroup? {
        val groups = trackerDao.getAllStudyGroups().firstOrNull() ?: emptyList()
        val matchingGroup = groups.find { it.groupCode.trim().lowercase() == groupCode.trim().lowercase() }
        
        if (matchingGroup != null) {
            val groupId = matchingGroup.id
            val members = trackerDao.getGroupMembers(groupId).firstOrNull() ?: emptyList()
            if (members.none { it.isCurrentUser }) {
                trackerDao.insertGroupMember(
                    GroupMember(groupId = groupId, name = "You", avatarSeed = "user_you", studyHours = 0f, xp = 0, isCurrentUser = true)
                )
                trackerDao.insertGroupMessage(
                    GroupMessage(groupId = groupId, senderName = "System", message = "You joined the study group!", isSystemMessage = true)
                )
            }
            return matchingGroup
        }

        if (groupCode.isNotBlank()) {
            val cleanCode = groupCode.trim().uppercase()
            val newGroupId = trackerDao.insertStudyGroup(
                StudyGroup(
                    name = "Unified Study Guild $cleanCode",
                    description = "A custom collaborative group joined via invitation. Study targets shared automatically!",
                    groupCode = cleanCode
                )
            ).toInt()

            trackerDao.insertGroupMember(GroupMember(groupId = newGroupId, name = "You", avatarSeed = "user_you", studyHours = 0.0f, xp = 0, isCurrentUser = true))
            trackerDao.insertGroupMember(GroupMember(groupId = newGroupId, name = "Zayn (Math Wiz)", avatarSeed = "avatar_4", studyHours = 12.5f, xp = 3100, isCurrentUser = false))
            trackerDao.insertGroupMember(GroupMember(groupId = newGroupId, name = "Rania (Biochemist)", avatarSeed = "avatar_5", studyHours = 8.2f, xp = 2100, isCurrentUser = false))

            trackerDao.insertGroupMessage(GroupMessage(groupId = newGroupId, senderName = "System", message = "Group joined via invite code: $cleanCode", isSystemMessage = true))
            trackerDao.insertGroupMessage(GroupMessage(groupId = newGroupId, senderName = "Zayn (Math Wiz)", message = "What's up! Great to have you with us. Hit some study blocks! 🙌"))

            return trackerDao.getStudyGroupById(newGroupId)
        }

        return null
    }

    suspend fun postGroupMessage(groupId: Int, senderName: String, message: String, isSystemMessage: Boolean = false) {
        trackerDao.insertGroupMessage(
            GroupMessage(groupId = groupId, senderName = senderName, message = message, isSystemMessage = isSystemMessage)
        )
    }

    suspend fun shareSessionToGroup(groupId: Int, subject: String, durationSeconds: Long) {
        val minutes = durationSeconds / 60
        val xpGain = (durationSeconds / 6).toInt()
        val text = "I completed a $minutes-minute Pomodoro focus block on '$subject'! 🚀 (+$xpGain XP)"
        
        postGroupMessage(groupId, "You", text)

        val members = trackerDao.getGroupMembers(groupId).firstOrNull() ?: emptyList()
        val curUser = members.find { it.isCurrentUser }
        if (curUser != null) {
            trackerDao.insertGroupMember(
                curUser.copy(
                    studyHours = curUser.studyHours + (durationSeconds / 3600f),
                    xp = curUser.xp + xpGain
                )
            )
        }

        triggerSimulatedBotReply(groupId, subject)
    }

    private suspend fun triggerSimulatedBotReply(groupId: Int, subject: String) {
        delay(1500)
        val members = trackerDao.getGroupMembers(groupId).firstOrNull() ?: emptyList()
        val chatbot = members.filter { !it.isCurrentUser }.randomOrNull() ?: return
        
        val botsReplies = listOf(
            "Incredible focus, ${chatbot.name.substringBefore(" ")}! That's how we climb levels!",
            "Let's go! Truly premium study discipline on '$subject' inside this group.",
            "Formidable focus block! I need to catch up to you now. 👀",
            "This streak is infectious! Excellent work, team.",
            "That is awesome! Keep that Pomodoro cycle roaring!",
            "Phenomenal session. My focus score is lagging behind, gotta start my timer now!"
        )
        postGroupMessage(groupId, chatbot.name, botsReplies.random())
    }

    suspend fun simulateActiveMembers(groupId: Int) {
        val members = trackerDao.getGroupMembers(groupId).firstOrNull() ?: emptyList()
        val chatbot = members.filter { !it.isCurrentUser }.randomOrNull() ?: return
        
        val randomMinutes = listOf(25, 50, 60).random()
        val randomXp = randomMinutes * 10
        val randomSubjects = listOf("Calculus", "Thermodynamics", "Anatomy", "Revision", "Deep Work")
        val randomSub = randomSubjects.random()

        trackerDao.insertGroupMember(
            chatbot.copy(
                studyHours = chatbot.studyHours + (randomMinutes / 60f),
                xp = chatbot.xp + randomXp
            )
        )

        postGroupMessage(
            groupId = groupId,
            senderName = chatbot.name,
            message = "I completed a $randomMinutes-minute Pomodoro focus block on '$randomSub'! 🚀 (+$randomXp XP)"
        )
    }

    suspend fun prepopulateGroupsIfEmpty() {
        val current = trackerDao.getAllStudyGroups().firstOrNull() ?: emptyList()
        if (current.isEmpty()) {
            val groupId = trackerDao.insertStudyGroup(
                StudyGroup(
                    name = "Apex Study Guild",
                    description = "Join elite students from around the globe to smash study thresholds. Weekly Pomodoro targets are actively logged!",
                    groupCode = "APEX99"
                )
            ).toInt()

            // Prepopulate study group members
            trackerDao.insertGroupMember(GroupMember(groupId = groupId, name = "You", avatarSeed = "user_you", studyHours = 12.5f, xp = 3200, isCurrentUser = true))
            trackerDao.insertGroupMember(GroupMember(groupId = groupId, name = "Lina (Deep Focus)", avatarSeed = "avatar_1", studyHours = 34.2f, xp = 8500, isCurrentUser = false))
            trackerDao.insertGroupMember(GroupMember(groupId = groupId, name = "Arif (Revision Pro)", avatarSeed = "avatar_2", studyHours = 24.8f, xp = 6120, isCurrentUser = false))
            trackerDao.insertGroupMember(GroupMember(groupId = groupId, name = "Sofia (Notes Guru)", avatarSeed = "avatar_3", studyHours = 18.0f, xp = 4500, isCurrentUser = false))

            // Prepopulate some group messages
            trackerDao.insertGroupMessage(GroupMessage(groupId = groupId, senderName = "System", message = "Study group 'Apex Study Guild' was created. Invitation code is APEX99.", isSystemMessage = true))
            trackerDao.insertGroupMessage(GroupMessage(groupId = groupId, senderName = "Lina (Deep Focus)", message = "What's up everyone! Ready to crush our study metrics this week?"))
            trackerDao.insertGroupMessage(GroupMessage(groupId = groupId, senderName = "Arif (Revision Pro)", message = "Just logged a 50-minute Pomodoro block on Thermodynamics! That feels incredible."))
            trackerDao.insertGroupMessage(GroupMessage(groupId = groupId, senderName = "Sofia (Notes Guru)", message = "Nice job! I will be starting my Calculus review session in 10 minutes. Hit me up if you want to join!"))
        }

        val settings = trackerDao.getNotificationSettings()
        if (settings == null) {
            trackerDao.insertNotificationSetting(NotificationSetting())
        }
    }


    // --- String Helpers ---
    fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getYesterdayString(todayString: String): String {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(todayString) ?: return ""
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DATE, -1)
            return sdf.format(cal.time)
        } catch (e: Exception) {
            return ""
        }
    }
}
