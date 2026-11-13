package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.ChecklistItem
import com.example.data.model.ChecklistSerializer
import com.example.data.model.Note
import com.example.data.model.Reminder
import com.example.data.model.TimestampPhoto
import com.example.data.model.Trip
import com.example.data.model.WatermarkStyle
import com.example.data.repository.PersonalRepository
import com.example.reminder.ReminderScheduler
import com.example.util.DateUtils
import com.example.util.LocationResult
import com.example.util.LocationServiceHelper
import com.example.util.TimestampWatermarkEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.example.data.model.RouteWaypoint
import com.example.data.model.RouteWaypointSerializer
import com.example.data.model.Task
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TaskSubtask
import com.example.data.model.TaskSubtaskSerializer
import com.example.util.CalendarUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest

enum class ScreenTab(val title: String, val iconLabel: String) {
    DASHBOARD("Tổng quan", "dashboard"),
    TASKS("Công việc", "assignment"),
    CALENDAR("Lịch", "calendar_month"),
    NOTES("Ghi chú", "note_alt"),
    REMINDERS("Nhắc nhở", "alarm"),
    TRIPS("Công tác", "business_center"),
    PHOTOS("Ảnh Timestamp", "photo_camera")
}

data class TagMonthlyStat(
    val tagName: String,
    val daysCount: Int,
    val formattedDates: List<String>,
    val tripCount: Int,
    val totalCost: Long,
    val sampleLocations: List<String>,
    val trips: List<Trip>
)

data class MonthMissionsStat(
    val year: Int,
    val month: Int, // 0-based
    val monthLabel: String,
    val totalDays: Int,
    val totalTrips: Int,
    val totalCost: Long,
    val tagStats: List<TagMonthlyStat>
)

class MainViewModel(
    private val repository: PersonalRepository,
    private val context: Context
) : ViewModel() {

    private val locationHelper = LocationServiceHelper(context)

    // Current Tab
    private val _currentTab = MutableStateFlow(ScreenTab.DASHBOARD)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    // App Theme Setting (Persisted, defaults to WARM_LIGHT anti-glare)
    private val prefs = context.getSharedPreferences("lifelog_theme_prefs", Context.MODE_PRIVATE)
    private val _themeSetting = MutableStateFlow(
        when (prefs.getString("selected_theme", "WARM_LIGHT")) {
            "DARK" -> com.example.ui.theme.ThemeSetting.DARK
            "CLEAN_LIGHT" -> com.example.ui.theme.ThemeSetting.CLEAN_LIGHT
            "SYSTEM" -> com.example.ui.theme.ThemeSetting.SYSTEM
            else -> com.example.ui.theme.ThemeSetting.WARM_LIGHT
        }
    )
    val themeSetting: StateFlow<com.example.ui.theme.ThemeSetting> = _themeSetting.asStateFlow()

    fun setThemeSetting(setting: com.example.ui.theme.ThemeSetting) {
        _themeSetting.value = setting
        prefs.edit().putString("selected_theme", setting.name).apply()
    }

    fun toggleTheme() {
        val next = when (_themeSetting.value) {
            com.example.ui.theme.ThemeSetting.WARM_LIGHT -> com.example.ui.theme.ThemeSetting.DARK
            com.example.ui.theme.ThemeSetting.DARK -> com.example.ui.theme.ThemeSetting.WARM_LIGHT
            com.example.ui.theme.ThemeSetting.CLEAN_LIGHT -> com.example.ui.theme.ThemeSetting.DARK
            com.example.ui.theme.ThemeSetting.SYSTEM -> com.example.ui.theme.ThemeSetting.WARM_LIGHT
        }
        setThemeSetting(next)
    }

    // Location
    private val _currentLocation = MutableStateFlow<LocationResult?>(null)
    val currentLocation: StateFlow<LocationResult?> = _currentLocation.asStateFlow()

    private val _isFetchingLocation = MutableStateFlow(false)
    val isFetchingLocation: StateFlow<Boolean> = _isFetchingLocation.asStateFlow()

    fun refreshLocation() {
        viewModelScope.launch {
            _isFetchingLocation.value = true
            val loc = locationHelper.getCurrentLocation()
            _currentLocation.value = loc
            _isFetchingLocation.value = false
        }
    }

    // Categories
    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveCategory(id: Long, name: String, colorHex: String) {
        viewModelScope.launch {
            val cat = Category(
                id = id,
                name = name.trim(),
                colorHex = colorHex
            )
            repository.insertOrUpdateCategory(cat)
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    // Reminders
    val allReminders: StateFlow<List<Reminder>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeReminderCount: StateFlow<Int> = repository.activeReminderCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun saveReminder(
        id: Long,
        title: String,
        description: String,
        triggerTime: Long,
        enableSound: Boolean,
        enableVibration: Boolean
    ) {
        viewModelScope.launch {
            val reminder = Reminder(
                id = id,
                title = title.trim(),
                description = description.trim(),
                triggerTime = triggerTime,
                enableSound = enableSound,
                enableVibration = enableVibration,
                isCompleted = false
            )
            val newId = repository.insertOrUpdateReminder(reminder)
            val scheduledReminder = reminder.copy(id = if (id == 0L) newId else id)
            ReminderScheduler.scheduleReminder(context, scheduledReminder)
        }
    }

    fun toggleReminderComplete(reminder: Reminder) {
        viewModelScope.launch {
            val newCompleted = !reminder.isCompleted
            repository.setReminderCompleted(reminder.id, newCompleted)
            if (newCompleted) {
                ReminderScheduler.cancelReminder(context, reminder.id)
            } else {
                ReminderScheduler.scheduleReminder(context, reminder.copy(isCompleted = false))
            }
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(context, id)
            repository.deleteReminder(id)
        }
    }

    // Work Tasks (Quản lý Task công việc)
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingTaskCount: StateFlow<Int> = repository.pendingTaskCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedTaskCount: StateFlow<Int> = repository.completedTaskCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val urgentTaskCount: StateFlow<Int> = repository.urgentTaskCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Task Filter: "ALL", "TODAY", "TODO", "IN_PROGRESS", "COMPLETED", "URGENT"
    private val _taskFilter = MutableStateFlow("ALL")
    val taskFilter: StateFlow<String> = _taskFilter.asStateFlow()

    fun setTaskFilter(filter: String) {
        _taskFilter.value = filter
    }

    // Task Project filter
    private val _selectedProject = MutableStateFlow("ALL")
    val selectedProject: StateFlow<String> = _selectedProject.asStateFlow()

    fun setSelectedProject(project: String) {
        _selectedProject.value = project
    }

    // Task Search Query
    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    fun setTaskSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    // Filtered Tasks
    val filteredTasks: StateFlow<List<Task>> = combine(
        allTasks,
        _taskFilter,
        _selectedProject,
        _taskSearchQuery
    ) { tasks, filter, project, query ->
        tasks.filter { task ->
            // Filter by Status / Time / Urgency
            val matchesFilter = when (filter) {
                "TODAY" -> task.dueDate > 0L && CalendarUtils.isToday(task.dueDate)
                "TODO" -> task.status == TaskStatus.TODO.name
                "IN_PROGRESS" -> task.status == TaskStatus.IN_PROGRESS.name
                "COMPLETED" -> task.status == TaskStatus.COMPLETED.name
                "URGENT" -> task.priority == TaskPriority.URGENT.name && !task.isCompleted
                else -> true
            }

            // Filter by Project
            val matchesProject = if (project == "ALL" || project.isBlank()) {
                true
            } else {
                task.project.equals(project, ignoreCase = true)
            }

            // Filter by Search Query
            val matchesSearch = if (query.isBlank()) {
                true
            } else {
                task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.project.contains(query, ignoreCase = true)
            }

            matchesFilter && matchesProject && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All distinct projects
    val allProjects: StateFlow<List<String>> = allTasks.combine(_taskFilter) { tasks, _ ->
        val defaultProjects = listOf("Công việc", "Dự án", "Báo cáo", "Họp hành", "Phát triển", "Khách hàng")
        (tasks.map { it.project.trim() }.filter { it.isNotBlank() } + defaultProjects).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Công việc", "Dự án", "Báo cáo", "Họp hành", "Phát triển", "Khách hàng"))

    fun saveTask(
        id: Long,
        title: String,
        description: String,
        status: String,
        priority: String,
        dueDate: Long,
        project: String,
        subtasks: List<TaskSubtask>,
        estimatedHours: Double
    ) {
        viewModelScope.launch {
            val subtasksJson = TaskSubtaskSerializer.serialize(subtasks)
            val task = Task(
                id = id,
                title = title.trim(),
                description = description.trim(),
                status = status,
                priority = priority,
                dueDate = dueDate,
                project = if (project.isNotBlank()) project.trim() else "Công việc",
                subtasksJson = subtasksJson,
                estimatedHours = estimatedHours,
                completedTimestamp = if (status == TaskStatus.COMPLETED.name) System.currentTimeMillis() else 0L
            )
            repository.insertOrUpdateTask(task)
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            repository.toggleTaskComplete(task)
        }
    }

    fun updateTaskStatus(task: Task, newStatus: String) {
        viewModelScope.launch {
            repository.updateTaskStatus(task.id, newStatus)
        }
    }

    fun toggleTaskSubtask(task: Task, subtaskIndex: Int) {
        viewModelScope.launch {
            repository.toggleSubtask(task, subtaskIndex)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
        }
    }

    // Notes
    private val _noteSearchQuery = MutableStateFlow("")
    val noteSearchQuery: StateFlow<String> = _noteSearchQuery.asStateFlow()

    private val _selectedNoteCategory = MutableStateFlow("Tất cả")
    val selectedNoteCategory: StateFlow<String> = _selectedNoteCategory.asStateFlow()

    fun setNoteSearchQuery(query: String) {
        _noteSearchQuery.value = query
    }

    fun setSelectedNoteCategory(category: String) {
        _selectedNoteCategory.value = category
    }

    val activeNotes: StateFlow<List<Note>> = combine(
        repository.activeNotes,
        _noteSearchQuery,
        _selectedNoteCategory
    ) { notes, query, category ->
        notes.filter { note ->
            val matchesQuery = query.isBlank() ||
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true)
            val matchesCategory = category == "Tất cả" || note.hasCategory(category)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNoteCount: StateFlow<Int> = repository.activeNoteCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun saveNote(
        id: Long,
        title: String,
        content: String,
        categories: List<String>,
        colorHex: String,
        isPinned: Boolean,
        checklists: List<ChecklistItem>,
        linkedPhotoPath: String?
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val primaryCat = categories.firstOrNull() ?: "Cá nhân"
            val note = Note(
                id = id,
                title = title.trim(),
                content = content.trim(),
                category = primaryCat,
                categoriesJson = categories.joinToString(";;;"),
                colorHex = colorHex,
                isPinned = isPinned,
                checklistItemsJson = ChecklistSerializer.serialize(checklists),
                linkedPhotoPath = linkedPhotoPath,
                createdTimestamp = if (id == 0L) now else now,
                updatedTimestamp = now
            )
            repository.insertOrUpdateNote(note)
        }
    }

    fun toggleChecklistItem(note: Note, itemIndex: Int) {
        viewModelScope.launch {
            val items = ChecklistSerializer.parse(note.checklistItemsJson).toMutableList()
            if (itemIndex in items.indices) {
                val current = items[itemIndex]
                items[itemIndex] = current.copy(isDone = !current.isDone)
                val updatedNote = note.copy(
                    checklistItemsJson = ChecklistSerializer.serialize(items),
                    updatedTimestamp = System.currentTimeMillis()
                )
                repository.insertOrUpdateNote(updatedNote)
            }
        }
    }

    fun togglePinNote(note: Note) {
        viewModelScope.launch {
            repository.togglePinNote(note.id, !note.isPinned)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    // Trips & Missions (Công tác)
    val allTrips: StateFlow<List<Trip>> = repository.allTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeTrip: StateFlow<Trip?> = repository.activeTrip
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalDistance: StateFlow<Double> = repository.totalDistance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCost: StateFlow<Long> = repository.totalCost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val completedTripCount: StateFlow<Int> = repository.completedTripCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Selected Month for Mission Stats (Default: Current Year and Month)
    private val initCal = Calendar.getInstance()
    private val _statYear = MutableStateFlow(initCal.get(Calendar.YEAR))
    val statYear: StateFlow<Int> = _statYear.asStateFlow()

    private val _statMonth = MutableStateFlow(initCal.get(Calendar.MONTH))
    val statMonth: StateFlow<Int> = _statMonth.asStateFlow()

    fun nextStatMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _statYear.value)
            set(Calendar.MONTH, _statMonth.value)
            add(Calendar.MONTH, 1)
        }
        _statYear.value = cal.get(Calendar.YEAR)
        _statMonth.value = cal.get(Calendar.MONTH)
    }

    fun prevStatMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _statYear.value)
            set(Calendar.MONTH, _statMonth.value)
            add(Calendar.MONTH, -1)
        }
        _statYear.value = cal.get(Calendar.YEAR)
        _statMonth.value = cal.get(Calendar.MONTH)
    }

    fun setCurrentStatMonth() {
        val cal = Calendar.getInstance()
        _statYear.value = cal.get(Calendar.YEAR)
        _statMonth.value = cal.get(Calendar.MONTH)
    }

    // Monthly Statistics Flow
    val monthMissionsStat: StateFlow<MonthMissionsStat> = combine(
        allTrips,
        _statYear,
        _statMonth
    ) { trips, year, month ->
        val cal = Calendar.getInstance()
        val tripsInMonth = trips.filter { trip ->
            cal.timeInMillis = trip.startTime
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }

        val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayDisplayFormat = SimpleDateFormat("dd/MM", Locale("vi", "VN"))

        // Distinct overall mission days in month
        val overallDaysCount = tripsInMonth.map { dayKeyFormat.format(Date(it.startTime)) }.distinct().size
        val totalTrips = tripsInMonth.size
        val totalCost = tripsInMonth.sumOf { it.costVnd }

        // Group by tagName (treat blank as "Khác")
        val groupedByTag = tripsInMonth.groupBy {
            val tag = it.tagName.trim()
            if (tag.isBlank()) "Khác" else tag
        }

        val tagStatsList = groupedByTag.map { (tag, tagTrips) ->
            val dayGroups = tagTrips.groupBy { dayKeyFormat.format(Date(it.startTime)) }
            val sortedDayKeys = dayGroups.keys.sorted()
            val formattedDates = sortedDayKeys.map { dayKey ->
                val sampleTrip = dayGroups[dayKey]!!.first()
                dayDisplayFormat.format(Date(sampleTrip.startTime))
            }
            val sampleLocations = tagTrips.map { it.startLocation }.filter { it.isNotBlank() }.distinct().take(3)
            val cost = tagTrips.sumOf { it.costVnd }

            TagMonthlyStat(
                tagName = tag,
                daysCount = sortedDayKeys.size,
                formattedDates = formattedDates,
                tripCount = tagTrips.size,
                totalCost = cost,
                sampleLocations = sampleLocations,
                trips = tagTrips.sortedByDescending { it.startTime }
            )
        }.sortedByDescending { it.daysCount }

        MonthMissionsStat(
            year = year,
            month = month,
            monthLabel = DateUtils.formatMonthYear(year, month),
            totalDays = overallDaysCount,
            totalTrips = totalTrips,
            totalCost = totalCost,
            tagStats = tagStatsList
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthMissionsStat(
            year = initCal.get(Calendar.YEAR),
            month = initCal.get(Calendar.MONTH),
            monthLabel = DateUtils.formatMonthYear(initCal.get(Calendar.YEAR), initCal.get(Calendar.MONTH)),
            totalDays = 0,
            totalTrips = 0,
            totalCost = 0L,
            tagStats = emptyList()
        )
    )

    // Distinct mission tags used in trips + suggestions
    val allMissionTags: StateFlow<List<String>> = allTrips.map { trips ->
        val userTags = trips.map { it.tagName.trim() }.filter { it.isNotBlank() }
        val presets = listOf("MSI", "Samsung", "Viettel", "FPT", "Chi nhánh HCM", "Nhà máy", "Khách hàng A")
        (userTags + presets).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("MSI", "Samsung", "Viettel", "FPT", "Chi nhánh HCM", "Nhà máy", "Khách hàng A"))

    fun quickCheckIn(
        tagName: String,
        location: String,
        lat: Double? = null,
        lng: Double? = null,
        costVnd: Long = 0L,
        notes: String = "",
        transportMode: String = "Xe máy",
        customTitle: String = ""
    ) {
        viewModelScope.launch {
            val loc = _currentLocation.value
            val resolvedLocation = location.ifBlank { loc?.address ?: "Vị trí hiện tại" }
            val cleanTag = tagName.trim()
            val title = when {
                customTitle.isNotBlank() -> customTitle.trim()
                cleanTag.isNotBlank() -> "Công tác tại $cleanTag"
                else -> "Check-in công tác lúc ${DateUtils.formatTime(System.currentTimeMillis())}"
            }
            val trip = Trip(
                title = title,
                startLocation = resolvedLocation,
                endLocation = resolvedLocation,
                transportMode = transportMode,
                purpose = "Công tác",
                notes = notes.trim(),
                costVnd = costVnd,
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                distanceKm = 0.0,
                status = "COMPLETED",
                startLatitude = lat ?: loc?.latitude,
                startLongitude = lng ?: loc?.longitude,
                endLatitude = lat ?: loc?.latitude,
                endLongitude = lng ?: loc?.longitude,
                tagName = cleanTag
            )
            repository.insertOrUpdateTrip(trip)
        }
    }

    // Real-time Speed & Route Tracking State
    private val _currentSpeedKmh = MutableStateFlow(0.0)
    val currentSpeedKmh: StateFlow<Double> = _currentSpeedKmh.asStateFlow()

    private var routeTrackingJob: Job? = null

    private fun observeActiveTripRouteTracking() {
        viewModelScope.launch {
            activeTrip.collectLatest { trip ->
                routeTrackingJob?.cancel()
                if (trip != null && trip.status == "IN_PROGRESS") {
                    routeTrackingJob = viewModelScope.launch {
                        locationHelper.trackLocationFlow(intervalMs = 4000L).collect { point ->
                            _currentSpeedKmh.value = point.speedKmh
                            val latestTrip = activeTrip.value ?: return@collect
                            val currentWaypoints = latestTrip.getRouteWaypoints().toMutableList()
                            val lastPoint = currentWaypoints.lastOrNull()

                            val deltaKm = if (lastPoint != null) {
                                LocationServiceHelper.calculateDistanceKm(
                                    lastPoint.latitude,
                                    lastPoint.longitude,
                                    point.latitude,
                                    point.longitude
                                )
                            } else {
                                0.0
                            }

                            // Record new point if first point or moved >= 8 meters
                            if (lastPoint == null || deltaKm >= 0.008) {
                                val newWaypoint = RouteWaypoint(
                                    latitude = point.latitude,
                                    longitude = point.longitude,
                                    timestamp = point.timestamp,
                                    speedKmh = point.speedKmh,
                                    label = if (currentWaypoints.isEmpty()) "Điểm xuất phát" else "Mốc GPS #${currentWaypoints.size + 1}"
                                )
                                currentWaypoints.add(newWaypoint)
                                val updatedDistance = (latestTrip.distanceKm + deltaKm).coerceAtLeast(0.0)
                                val elapsedMs = (point.timestamp - latestTrip.startTime).coerceAtLeast(1000L)
                                val speedByDist = DateUtils.calculateAvgSpeedKmh(updatedDistance, elapsedMs)
                                val speedSamples = currentWaypoints.map { it.speedKmh }.filter { it > 0.0 }
                                val avgSample = if (speedSamples.isNotEmpty()) speedSamples.average() else 0.0
                                val updatedAvgSpeed = when {
                                    speedByDist > 0.0 && avgSample > 0.0 -> (speedByDist + avgSample) / 2.0
                                    speedByDist > 0.0 -> speedByDist
                                    else -> avgSample
                                }
                                val updatedMaxSpeed = maxOf(
                                    latestTrip.maxSpeedKmh,
                                    point.speedKmh,
                                    updatedAvgSpeed
                                )

                                repository.insertOrUpdateTrip(
                                    latestTrip.copy(
                                        distanceKm = updatedDistance,
                                        avgSpeedKmh = updatedAvgSpeed,
                                        maxSpeedKmh = updatedMaxSpeed,
                                        routePointsJson = RouteWaypointSerializer.serialize(currentWaypoints)
                                    )
                                )
                            }
                        }
                    }
                } else {
                    _currentSpeedKmh.value = 0.0
                }
            }
        }
    }

    fun recordCurrentWaypoint() {
        viewModelScope.launch {
            val trip = activeTrip.value ?: return@launch
            val freshLoc = locationHelper.getCurrentLocation() ?: _currentLocation.value
            val now = System.currentTimeMillis()
            val currentWaypoints = trip.getRouteWaypoints().toMutableList()
            val lastPt = currentWaypoints.lastOrNull()

            val lat = freshLoc?.latitude ?: lastPt?.latitude ?: trip.startLatitude ?: 10.7769
            val lng = freshLoc?.longitude ?: lastPt?.longitude ?: trip.startLongitude ?: 106.7009
            val addr = freshLoc?.address ?: "Mốc lộ trình #${currentWaypoints.size + 1}"

            val deltaKm = if (lastPt != null) {
                LocationServiceHelper.calculateDistanceKm(lastPt.latitude, lastPt.longitude, lat, lng)
            } else {
                0.0
            }

            val instantSpeed = when {
                (freshLoc?.speedKmh ?: 0.0) > 0.0 -> freshLoc!!.speedKmh
                _currentSpeedKmh.value > 0.0 -> _currentSpeedKmh.value
                lastPt != null && deltaKm > 0.0 -> {
                    DateUtils.calculateAvgSpeedKmh(deltaKm, (now - lastPt.timestamp).coerceAtLeast(1000L))
                }
                else -> 0.0
            }

            currentWaypoints.add(
                RouteWaypoint(
                    latitude = lat,
                    longitude = lng,
                    timestamp = now,
                    speedKmh = instantSpeed,
                    label = addr
                )
            )

            val newDistance = trip.distanceKm + deltaKm
            val elapsedMs = (now - trip.startTime).coerceAtLeast(1000L)
            val newAvgSpeed = DateUtils.calculateAvgSpeedKmh(newDistance, elapsedMs).let {
                if (it > 0.0) it else trip.avgSpeedKmh
            }
            val newMaxSpeed = maxOf(trip.maxSpeedKmh, instantSpeed, newAvgSpeed)

            repository.insertOrUpdateTrip(
                trip.copy(
                    distanceKm = newDistance,
                    avgSpeedKmh = newAvgSpeed,
                    maxSpeedKmh = newMaxSpeed,
                    routePointsJson = RouteWaypointSerializer.serialize(currentWaypoints)
                )
            )
        }
    }

    fun startTrip(
        title: String,
        startLocation: String,
        transportMode: String,
        purpose: String,
        tagName: String = ""
    ) {
        viewModelScope.launch {
            val loc = _currentLocation.value ?: locationHelper.getCurrentLocation()
            val now = System.currentTimeMillis()
            val resolvedStart = startLocation.ifBlank { loc?.address ?: "Điểm xuất phát" }
            val initialWaypoints = if (loc != null) {
                listOf(
                    RouteWaypoint(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        timestamp = now,
                        speedKmh = loc.speedKmh,
                        label = "Xuất phát: $resolvedStart"
                    )
                )
            } else emptyList()

            val trip = Trip(
                title = title.ifBlank { "Chuyến đi lúc ${DateUtils.formatTime(now)}" },
                startLocation = resolvedStart,
                transportMode = transportMode,
                purpose = purpose,
                startTime = now,
                status = "IN_PROGRESS",
                startLatitude = loc?.latitude,
                startLongitude = loc?.longitude,
                tagName = tagName.trim(),
                routePointsJson = RouteWaypointSerializer.serialize(initialWaypoints)
            )
            repository.insertOrUpdateTrip(trip)
        }
    }

    fun finishActiveTrip(
        trip: Trip,
        endLocation: String,
        distanceKm: Double,
        costVnd: Long,
        notes: String,
        photoProofPath: String?
    ) {
        viewModelScope.launch {
            val loc = _currentLocation.value ?: locationHelper.getCurrentLocation()
            val now = System.currentTimeMillis()
            val resolvedEnd = endLocation.ifBlank { loc?.address ?: "Điểm đến" }
            val waypoints = trip.getRouteWaypoints().toMutableList()
            val lastPt = waypoints.lastOrNull()

            var finalDistance = if (distanceKm > 0.0) distanceKm else trip.distanceKm
            if (loc != null) {
                if (lastPt != null && finalDistance <= 0.0) {
                    finalDistance += LocationServiceHelper.calculateDistanceKm(
                        lastPt.latitude,
                        lastPt.longitude,
                        loc.latitude,
                        loc.longitude
                    )
                }
                waypoints.add(
                    RouteWaypoint(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        timestamp = now,
                        speedKmh = _currentSpeedKmh.value,
                        label = "Đích: $resolvedEnd"
                    )
                )
            }

            val durationMs = (now - trip.startTime).coerceAtLeast(1000L)
            val computedAvgSpeed = DateUtils.calculateAvgSpeedKmh(finalDistance, durationMs).let {
                if (it > 0.0) it else trip.avgSpeedKmh
            }
            val computedMaxSpeed = maxOf(
                trip.maxSpeedKmh,
                computedAvgSpeed,
                waypoints.maxOfOrNull { it.speedKmh } ?: 0.0
            )

            val finished = trip.copy(
                endLocation = resolvedEnd,
                endTime = now,
                distanceKm = finalDistance,
                costVnd = costVnd,
                notes = notes,
                status = "COMPLETED",
                endLatitude = loc?.latitude,
                endLongitude = loc?.longitude,
                photoProofPath = photoProofPath,
                avgSpeedKmh = computedAvgSpeed,
                maxSpeedKmh = computedMaxSpeed,
                routePointsJson = RouteWaypointSerializer.serialize(waypoints)
            )
            repository.insertOrUpdateTrip(finished)
        }
    }

    fun cancelActiveTrip(trip: Trip) {
        viewModelScope.launch {
            repository.deleteTrip(trip.id)
        }
    }

    fun saveManualTrip(
        title: String,
        startLocation: String,
        endLocation: String,
        transportMode: String,
        startTime: Long,
        endTime: Long,
        distanceKm: Double,
        costVnd: Long,
        purpose: String,
        notes: String,
        tagName: String = ""
    ) {
        viewModelScope.launch {
            val resolvedEndTime = if (endTime > startTime) endTime else startTime + 1800000L
            val durationMs = (resolvedEndTime - startTime).coerceAtLeast(60_000L)
            val computedAvgSpeed = DateUtils.calculateAvgSpeedKmh(distanceKm, durationMs)
            val loc = _currentLocation.value
            val startLat = loc?.latitude ?: 10.7769
            val startLng = loc?.longitude ?: 106.7009
            val offsetDeg = (distanceKm * 0.006).coerceIn(0.002, 0.25)

            val waypoints = if (distanceKm > 0.0) {
                listOf(
                    RouteWaypoint(
                        latitude = startLat,
                        longitude = startLng,
                        timestamp = startTime,
                        speedKmh = 0.0,
                        label = "Xuất phát: ${startLocation.trim()}"
                    ),
                    RouteWaypoint(
                        latitude = startLat + offsetDeg * 0.5,
                        longitude = startLng + offsetDeg * 0.4,
                        timestamp = startTime + durationMs / 2,
                        speedKmh = computedAvgSpeed * 1.15,
                        label = "Giữa tuyến đường"
                    ),
                    RouteWaypoint(
                        latitude = startLat + offsetDeg,
                        longitude = startLng + offsetDeg * 0.85,
                        timestamp = resolvedEndTime,
                        speedKmh = computedAvgSpeed,
                        label = "Đích: ${endLocation.trim()}"
                    )
                )
            } else emptyList()

            val trip = Trip(
                title = title.trim(),
                startLocation = startLocation.trim(),
                endLocation = endLocation.trim(),
                transportMode = transportMode,
                startTime = startTime,
                endTime = resolvedEndTime,
                distanceKm = distanceKm,
                costVnd = costVnd,
                purpose = purpose,
                notes = notes.trim(),
                status = "COMPLETED",
                startLatitude = startLat,
                startLongitude = startLng,
                endLatitude = startLat + offsetDeg,
                endLongitude = startLng + offsetDeg * 0.85,
                tagName = tagName.trim(),
                avgSpeedKmh = computedAvgSpeed,
                maxSpeedKmh = (computedAvgSpeed * 1.25).coerceAtMost(120.0),
                routePointsJson = RouteWaypointSerializer.serialize(waypoints)
            )
            repository.insertOrUpdateTrip(trip)
        }
    }

    fun deleteTrip(id: Long) {
        viewModelScope.launch {
            repository.deleteTrip(id)
        }
    }

    // Timestamp Photos
    val photos: StateFlow<List<TimestampPhoto>> = repository.allPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val photoCount: StateFlow<Int> = repository.photoCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun processAndSaveStampedPhoto(
        sourceTempFilePath: String,
        style: WatermarkStyle,
        caption: String,
        tag: String,
        linkedTripId: Long? = null,
        linkedNoteId: Long? = null,
        onSuccess: (TimestampPhoto) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val loc = _currentLocation.value

                val stampedFile = TimestampWatermarkEngine.processAndStampImage(
                    context = context,
                    inputFilePath = sourceTempFilePath,
                    style = style,
                    caption = caption,
                    tag = tag,
                    timestamp = now,
                    latitude = loc?.latitude,
                    longitude = loc?.longitude,
                    address = loc?.address
                )

                val entity = TimestampPhoto(
                    filePath = stampedFile.absolutePath,
                    caption = caption.trim(),
                    timestamp = now,
                    formattedDateTime = DateUtils.formatFullDateTime(now),
                    locationAddress = loc?.address ?: "Vị trí không xác định",
                    latitude = loc?.latitude,
                    longitude = loc?.longitude,
                    watermarkStyle = style.name,
                    tag = tag,
                    linkedTripId = linkedTripId,
                    linkedNoteId = linkedNoteId
                )

                val newId = repository.insertPhoto(entity)
                onSuccess(entity.copy(id = newId))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePhoto(photo: TimestampPhoto) {
        viewModelScope.launch {
            try {
                val file = File(photo.filePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
            repository.deletePhoto(photo.id)
        }
    }

    init {
        refreshLocation()
        observeActiveTripRouteTracking()
    }
}

class MainViewModelFactory(
    private val repository: PersonalRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
