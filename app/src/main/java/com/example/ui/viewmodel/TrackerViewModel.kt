package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AcademicItem
import com.example.data.model.GamificationEngine
import com.example.data.model.Goal
import com.example.data.model.Habit
import com.example.data.model.StudySession
import com.example.data.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TimerState {
    RUNNING, PAUSED, STOPPED
}

class TrackerViewModel(private val repository: AppRepository) : ViewModel() {

    // --- Database Flows ---
    val sessions: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habits: StateFlow<List<Habit>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitHistories: StateFlow<List<com.example.data.model.HabitHistory>> = repository.allHabitHistories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<com.example.data.model.AcademicSubject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val academicItems: StateFlow<List<AcademicItem>> = repository.allAcademicItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<Goal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiInsight: StateFlow<com.example.data.model.AiInsight?> = repository.cachedInsight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val notifications: StateFlow<List<com.example.data.model.AppNotification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationSettings: StateFlow<com.example.data.model.NotificationSetting?> = repository.notificationSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val studyGroups: StateFlow<List<com.example.data.model.StudyGroup>> = repository.allStudyGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Local Interactive States ---
    var todayString by mutableStateOf("")
        private set

    // Timer States
    var timerRunningState by mutableStateOf(TimerState.STOPPED)
    var timerSecondsRemaining by mutableStateOf(1500L) // default 25 min
    var timerInitialDurationSeconds by mutableStateOf(1500L)
    var timerSelectedSubject by mutableStateOf("Deep Work")
    var timerNotes by mutableStateOf("")
    var timerSelectedModeName by mutableStateOf("25/5 Mode")

    private var timerJob: Job? = null

    // UI Loading states
    private val _isGeneratingAi = MutableStateFlow(false)
    val isGeneratingAi: StateFlow<Boolean> = _isGeneratingAi.asStateFlow()

    init {
        todayString = repository.getTodayString()
        viewModelScope.launch {
            repository.prepopulateHabitsIfEmpty()
            repository.prepopulateGroupsIfEmpty()
            repository.generateContextNotifications()
        }
    }

    // --- Dynamic Gamification Metrics (Reactive to database) ---
    val gamificationMetrics: StateFlow<Metrics> = combine(
        sessions, habitHistories, academicItems, goals, habits
    ) { sess, hist, acItems, gls, hbs ->
        val totalStudySec = sess.sumOf { it.durationSeconds }
        val totalStudyHours = totalStudySec / 3600f
        
        // XP calculation
        val studyXp = sess.sumOf { (it.durationSeconds / 6).toInt() } // 10 XP per minute studied
        val habitXp = hist.size * 50
        val academicXp = acItems.count { it.isCompleted } * 150
        val goalXp = gls.count { it.isCompleted } * 200
        val totalXp = studyXp + habitXp + academicXp + goalXp

        val levelInfo = GamificationEngine.calculateLevel(totalXp)
        val achievements = GamificationEngine.getAchievements(
            sessionsCount = sess.size,
            habitsCompletedCount = hist.size,
            itemsCompletedCount = acItems.count { it.isCompleted },
            goalsCompletedCount = gls.count { it.isCompleted },
            totalStudyHours = totalStudyHours
        )

        // Today's Habit completion %
        val todayLogs = hist.filter { it.dateString == todayString }
        val todayCompPercent = if (hbs.isNotEmpty()) {
            (todayLogs.size.toFloat() / hbs.size.toFloat() * 100).toInt()
        } else {
            0
        }

        // Longest Streak of habits
        val longestStreak = hbs.maxOfOrNull { it.streak } ?: 0

        Metrics(
            totalStudyHours = totalStudyHours,
            totalStudyMinutes = totalStudySec / 60,
            lifetimeXp = totalXp,
            levelInfo = levelInfo,
            achievements = achievements,
            todayHabitCompletionPercent = todayCompPercent,
            longestStreak = longestStreak
        ).also {
            todayString = repository.getTodayString()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Metrics()
    )


    // --- Timer Actions ---
    fun setTimerMode(type: String) {
        if (timerRunningState != TimerState.STOPPED) return
        when (type) {
            "25/5" -> {
                timerInitialDurationSeconds = 1500L
                timerSelectedModeName = "25/5 Mode"
            }
            "50/10" -> {
                timerInitialDurationSeconds = 3000L
                timerSelectedModeName = "50/10 Mode"
            }
            "CUSTOM_5" -> {
                timerInitialDurationSeconds = 300L // 5 mins (fast test)
                timerSelectedModeName = "Fast Session"
            }
            "CUSTOM_1" -> {
                timerInitialDurationSeconds = 60L // 1 minute (for fast verification/testing)
                timerSelectedModeName = "Rapid Sprint"
            }
            "CUSTOM_60" -> {
                timerInitialDurationSeconds = 3600L // 60 mins
                timerSelectedModeName = "Ultra Focus"
            }
        }
        timerSecondsRemaining = timerInitialDurationSeconds
    }

    fun startTimer() {
        if (timerRunningState == TimerState.RUNNING) return
        timerRunningState = TimerState.RUNNING
        timerJob = viewModelScope.launch {
            while (timerSecondsRemaining > 0 && timerRunningState == TimerState.RUNNING) {
                delay(1000)
                if (timerRunningState == TimerState.RUNNING) {
                    timerSecondsRemaining--
                }
            }
            if (timerSecondsRemaining <= 0 && timerRunningState == TimerState.RUNNING) {
                // Completed!
                saveSession(
                    subjectName = timerSelectedSubject,
                    duration = timerInitialDurationSeconds,
                    notesText = timerNotes.ifEmpty { "Focus study block completed successfully." }
                )
                // Award XP and complete goals/tasks automatically
                timerRunningState = TimerState.STOPPED
                timerSecondsRemaining = timerInitialDurationSeconds
                timerNotes = ""
            }
        }
    }

    fun pauseTimer() {
        timerRunningState = TimerState.PAUSED
        timerJob?.cancel()
    }

    fun resumeTimer() {
        startTimer()
    }

    fun stopTimer() {
        timerRunningState = TimerState.STOPPED
        timerJob?.cancel()
        timerSecondsRemaining = timerInitialDurationSeconds
        timerNotes = ""
    }

    private fun saveSession(subjectName: String, duration: Long, notesText: String) {
        viewModelScope.launch {
            repository.addStudySession(subjectName, duration, notesText)
        }
    }


    // --- Habits Actions ---
    fun toggleHabit(habitId: Int, date: String) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, date)
        }
    }

    fun addCustomHabit(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addCustomHabit(name)
        }
    }

    fun deleteHabit(id: Int) {
        viewModelScope.launch {
            repository.deleteHabit(id)
        }
    }


    // --- Academic Actions ---
    fun addSubject(name: String, creditHours: Int, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addSubject(name, creditHours, colorHex)
        }
    }

    fun deleteSubject(id: Int) {
        viewModelScope.launch {
            repository.deleteSubject(id)
        }
    }

    fun addAcademicItem(type: String, subjectName: String, name: String, deadlineMillis: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addAcademicItem(type, subjectName, name, deadlineMillis)
        }
    }

    fun toggleAcademicItem(item: AcademicItem) {
        viewModelScope.launch {
            repository.toggleAcademicItemCompletion(item)
        }
    }

    fun updateAcademicGrade(item: AcademicItem, grade: Double?) {
        viewModelScope.launch {
            repository.updateAcademicItemGrade(item, grade)
        }
    }

    fun deleteAcademicItem(id: Int) {
        viewModelScope.launch {
            repository.deleteAcademicItem(id)
        }
    }


    // --- Goals Actions ---
    fun addGoal(type: String, name: String, deadlineMillis: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addGoal(type, name, deadlineMillis)
        }
    }

    fun toggleGoal(goal: Goal) {
        viewModelScope.launch {
            repository.toggleGoalCompletion(goal)
        }
    }

    fun updateGoalProgress(goal: Goal, progressPercent: Int) {
        viewModelScope.launch {
            repository.updateGoalProgress(goal, progressPercent)
        }
    }

    fun deleteGoal(id: Int) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }


    // --- AI Suggestions Action ---
    fun generateCoachInsights() {
        _isGeneratingAi.value = true
        viewModelScope.launch {
            try {
                repository.generateAiRecommendations(
                    sessionsList = sessions.value,
                    habitsList = habits.value,
                    habitHistoriesList = habitHistories.value,
                    goalsList = goals.value
                )
            } finally {
                _isGeneratingAi.value = false
            }
        }
    }

    // --- Notifications Actions ---
    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun deleteNotification(id: Int) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun updateNotificationSettings(timer: Boolean, habit: Boolean, deadline: Boolean, silent: Boolean) {
        viewModelScope.launch {
            val current = repository.getNotificationSettings()
            repository.updateNotificationSettings(
                current.copy(
                    timerReminders = timer,
                    habitReminders = habit,
                    deadlineAlerts = deadline,
                    silentMode = silent
                )
            )
            repository.generateContextNotifications()
        }
    }

    fun triggerContextNotificationsEvaluation() {
        viewModelScope.launch {
            repository.generateContextNotifications()
        }
    }

    // --- Collaborative Study Groups States & Actions ---
    private val _selectedGroupId = MutableStateFlow<Int?>(null)
    val selectedGroupId = _selectedGroupId.asStateFlow()

    private val _currentGroupMembers = MutableStateFlow<List<com.example.data.model.GroupMember>>(emptyList())
    val currentGroupMembers = _currentGroupMembers.asStateFlow()

    private val _currentGroupMessages = MutableStateFlow<List<com.example.data.model.GroupMessage>>(emptyList())
    val currentGroupMessages = _currentGroupMessages.asStateFlow()

    private var membersJob: Job? = null
    private var messagesJob: Job? = null

    fun selectGroup(id: Int?) {
        _selectedGroupId.value = id
        membersJob?.cancel()
        messagesJob?.cancel()
        if (id != null) {
            membersJob = viewModelScope.launch {
                repository.getGroupMembers(id).collect {
                    _currentGroupMembers.value = it
                }
            }
            messagesJob = viewModelScope.launch {
                repository.getGroupMessages(id).collect {
                    _currentGroupMessages.value = it
                }
            }
        } else {
            _currentGroupMembers.value = emptyList()
            _currentGroupMessages.value = emptyList()
        }
    }

    fun createStudyGroup(name: String, description: String, code: String) {
        viewModelScope.launch {
            val newId = repository.createStudyGroup(name, description, code)
            selectGroup(newId.toInt())
        }
    }

    fun joinStudyGroup(code: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            val group = repository.joinStudyGroup(code)
            if (group != null) {
                selectGroup(group.id)
                onCompleted(true)
            } else {
                onCompleted(false)
            }
        }
    }

    fun sendGroupChatMessage(groupId: Int, message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            repository.postGroupMessage(groupId, "You", message)
        }
    }

    fun shareStudySessionToGroup(groupId: Int, subjectName: String, durationSec: Long) {
        viewModelScope.launch {
            repository.shareSessionToGroup(groupId, subjectName, durationSec)
        }
    }

    fun runActiveGroupSimulation(groupId: Int) {
        viewModelScope.launch {
            repository.simulateActiveMembers(groupId)
        }
    }



    // --- Helper Metrics Class ---
    data class Metrics(
        val totalStudyHours: Float = 0f,
        val totalStudyMinutes: Long = 0L,
        val lifetimeXp: Int = 0,
        val levelInfo: GamificationEngine.LevelInfo = GamificationEngine.LevelInfo(1, "Rookie", 0, 1000, 0),
        val achievements: List<GamificationEngine.Achievement> = emptyList(),
        val todayHabitCompletionPercent: Int = 0,
        val longestStreak: Int = 0
    )
}

@Suppress("UNCHECKED_CAST")
class TrackerViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
            return TrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
