package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AppDatabase
import com.example.data.model.Note
import com.example.data.model.Reminder
import com.example.data.model.Task
import com.example.data.model.TimestampPhoto
import com.example.data.repository.PersonalRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.ScreenTab
import com.example.ui.calendar.CalendarScreen
import com.example.ui.camera.PhotoDetailDialog
import com.example.ui.camera.TimestampCameraScreen
import com.example.ui.components.LifeLogBottomNav
import com.example.ui.components.LifeLogTopBar
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.notes.CategoryManagementDialog
import com.example.ui.notes.NoteEditDialog
import com.example.ui.notes.NotesScreen
import com.example.ui.reminders.ReminderEditDialog
import com.example.ui.reminders.RemindersScreen
import com.example.ui.tasks.TaskEditDialog
import com.example.ui.tasks.TasksScreen
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeSetting
import com.example.ui.trips.QuickMissionCheckInDialog
import com.example.ui.trips.TripFinishDialog
import com.example.ui.trips.TripStartDialog
import com.example.ui.trips.TripsScreen

import com.example.ui.components.ThemeSelectDialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MainRoot()
        }
    }
}

@Composable
fun MainRoot() {
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val database = remember { AppDatabase.getDatabase(context, coroutineScope) }
    val repository = remember {
        PersonalRepository(
            noteDao = database.noteDao(),
            tripDao = database.tripDao(),
            timestampPhotoDao = database.timestampPhotoDao(),
            categoryDao = database.categoryDao(),
            reminderDao = database.reminderDao(),
            taskDao = database.taskDao()
        )
    }

    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(repository, context)
    )

    val themeSetting by viewModel.themeSetting.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeSetting) {
        ThemeSetting.WARM_LIGHT, ThemeSetting.CLEAN_LIGHT -> false
        ThemeSetting.DARK -> true
        ThemeSetting.SYSTEM -> isSystemDark
    }

    MyApplicationTheme(themeSetting = themeSetting) {
        MainApp(
            viewModel = viewModel,
            isDarkTheme = isDark,
            themeSetting = themeSetting,
            onToggleTheme = { viewModel.toggleTheme() },
            onSelectTheme = { viewModel.setThemeSetting(it) }
        )
    }
}

@Composable
fun MainApp(
    viewModel: MainViewModel,
    isDarkTheme: Boolean = false,
    themeSetting: ThemeSetting = ThemeSetting.WARM_LIGHT,
    onToggleTheme: () -> Unit = {},
    onSelectTheme: (ThemeSetting) -> Unit = {}
) {
    // Request permissions on startup (Location + Notifications on Android 13+)
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.refreshLocation()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissionsToRequest.toTypedArray())
    }

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()
    val isFetchingLocation by viewModel.isFetchingLocation.collectAsStateWithLifecycle()

    // Categories state
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    // Notes state
    val notes by viewModel.activeNotes.collectAsStateWithLifecycle()
    val activeNoteCount by viewModel.activeNoteCount.collectAsStateWithLifecycle()
    val searchQuery by viewModel.noteSearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedNoteCategory.collectAsStateWithLifecycle()

    // Reminders state
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    val activeReminderCount by viewModel.activeReminderCount.collectAsStateWithLifecycle()

    // Tasks state
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val pendingTaskCount by viewModel.pendingTaskCount.collectAsStateWithLifecycle()
    val completedTaskCount by viewModel.completedTaskCount.collectAsStateWithLifecycle()
    val urgentTaskCount by viewModel.urgentTaskCount.collectAsStateWithLifecycle()
    val taskFilter by viewModel.taskFilter.collectAsStateWithLifecycle()
    val selectedProject by viewModel.selectedProject.collectAsStateWithLifecycle()
    val allProjects by viewModel.allProjects.collectAsStateWithLifecycle()
    val taskSearchQuery by viewModel.taskSearchQuery.collectAsStateWithLifecycle()

    // Trips & Missions state
    val trips by viewModel.allTrips.collectAsStateWithLifecycle()
    val activeTrip by viewModel.activeTrip.collectAsStateWithLifecycle()
    val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsStateWithLifecycle()
    val totalDistance by viewModel.totalDistance.collectAsStateWithLifecycle()
    val totalCost by viewModel.totalCost.collectAsStateWithLifecycle()
    val completedTripCount by viewModel.completedTripCount.collectAsStateWithLifecycle()
    val monthMissionsStat by viewModel.monthMissionsStat.collectAsStateWithLifecycle()
    val allMissionTags by viewModel.allMissionTags.collectAsStateWithLifecycle()

    // Photos state
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val photoCount by viewModel.photoCount.collectAsStateWithLifecycle()

    // Quick Dialog states
    var quickCreateNoteOpen by remember { mutableStateOf(false) }
    var selectedNoteForEdit by remember { mutableStateOf<Note?>(null) }
    var quickCreateReminderOpen by remember { mutableStateOf(false) }
    var selectedReminderForEdit by remember { mutableStateOf<Reminder?>(null) }
    var prefilledReminderTime by remember { mutableLongStateOf(0L) }
    var quickCreateTaskOpen by remember { mutableStateOf(false) }
    var selectedTaskForEdit by remember { mutableStateOf<Task?>(null) }
    var isCategoryManagementOpen by remember { mutableStateOf(false) }
    var quickCheckInOpen by remember { mutableStateOf(false) }
    var quickStartTripOpen by remember { mutableStateOf(false) }
    var finishActiveTripOpen by remember { mutableStateOf(false) }
    var selectedPhotoForDetail by remember { mutableStateOf<TimestampPhoto?>(null) }
    var isThemeDialogOpen by remember { mutableStateOf(false) }

    // Back handling: If on a sub-tab, return to DASHBOARD first
    BackHandler(enabled = currentTab != ScreenTab.DASHBOARD) {
        viewModel.setTab(ScreenTab.DASHBOARD)
    }

    Scaffold(
        topBar = {
            LifeLogTopBar(
                currentTab = currentTab,
                location = currentLocation,
                isFetchingLocation = isFetchingLocation,
                isDarkTheme = isDarkTheme,
                themeSetting = themeSetting,
                onToggleTheme = onToggleTheme,
                onOpenThemeDialog = { isThemeDialogOpen = true },
                onRefreshLocation = { viewModel.refreshLocation() },
                onOpenPhotos = { viewModel.setTab(ScreenTab.PHOTOS) }
            )
        },
        bottomBar = {
            LifeLogBottomNav(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.DASHBOARD -> {
                    DashboardScreen(
                        notes = notes,
                        trips = trips,
                        activeTrip = activeTrip,
                        photos = photos,
                        reminders = reminders,
                        tasks = allTasks,
                        activeNoteCount = activeNoteCount,
                        totalDistance = totalDistance,
                        totalCost = totalCost,
                        photoCount = photoCount,
                        activeReminderCount = activeReminderCount,
                        currentLocation = currentLocation,
                        currentSpeedKmh = currentSpeedKmh,
                        onRecordWaypoint = { viewModel.recordCurrentWaypoint() },
                        onNavigateToTab = { viewModel.setTab(it) },
                        onStartTripClick = {
                            if (activeTrip != null) {
                                viewModel.setTab(ScreenTab.TRIPS)
                            } else {
                                quickCheckInOpen = true
                            }
                        },
                        onFinishActiveTrip = { finishActiveTripOpen = true },
                        onCancelActiveTrip = { activeTrip?.let { viewModel.cancelActiveTrip(it) } },
                        onCreateNoteClick = { quickCreateNoteOpen = true },
                        onCreateReminderClick = {
                            prefilledReminderTime = 0L
                            quickCreateReminderOpen = true
                        },
                        onCreateTaskClick = { quickCreateTaskOpen = true },
                        onToggleReminderComplete = { viewModel.toggleReminderComplete(it) },
                        onToggleTaskComplete = { viewModel.toggleTaskComplete(it) },
                        onNoteClick = { selectedNoteForEdit = it },
                        onTaskClick = { selectedTaskForEdit = it },
                        onPhotoClick = { selectedPhotoForDetail = it }
                    )
                }

                ScreenTab.TASKS -> {
                    TasksScreen(
                        tasks = tasks,
                        allTasks = allTasks,
                        pendingCount = pendingTaskCount,
                        completedCount = completedTaskCount,
                        urgentCount = urgentTaskCount,
                        currentFilter = taskFilter,
                        selectedProject = selectedProject,
                        availableProjects = allProjects,
                        searchQuery = taskSearchQuery,
                        onFilterChange = { viewModel.setTaskFilter(it) },
                        onProjectChange = { viewModel.setSelectedProject(it) },
                        onSearchChange = { viewModel.setTaskSearchQuery(it) },
                        onToggleComplete = { viewModel.toggleTaskComplete(it) },
                        onUpdateStatus = { task, st -> viewModel.updateTaskStatus(task, st) },
                        onToggleSubtask = { task, idx -> viewModel.toggleTaskSubtask(task, idx) },
                        onEditTask = { selectedTaskForEdit = it },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onCreateTaskClick = { quickCreateTaskOpen = true }
                    )
                }

                ScreenTab.CALENDAR -> {
                    CalendarScreen(
                        notes = notes,
                        reminders = reminders,
                        trips = trips,
                        photos = photos,
                        tasks = allTasks,
                        onNoteClick = { selectedNoteForEdit = it },
                        onReminderClick = { selectedReminderForEdit = it },
                        onToggleReminder = { viewModel.toggleReminderComplete(it) },
                        onTaskClick = { selectedTaskForEdit = it },
                        onToggleTask = { viewModel.toggleTaskComplete(it) },
                        onPhotoClick = { selectedPhotoForDetail = it },
                        onCreateReminderForDate = { timestamp ->
                            prefilledReminderTime = timestamp
                            quickCreateReminderOpen = true
                        },
                        onCreateNoteClick = { quickCreateNoteOpen = true }
                    )
                }

                ScreenTab.NOTES -> {
                    NotesScreen(
                        notes = notes,
                        availableCategories = categories,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        onSearchChange = { viewModel.setNoteSearchQuery(it) },
                        onCategoryChange = { viewModel.setSelectedNoteCategory(it) },
                        onSaveNote = { note, title, content, cats, colorHex, isPinned, checklists, photo ->
                            viewModel.saveNote(
                                id = note?.id ?: 0L,
                                title = title,
                                content = content,
                                categories = cats,
                                colorHex = colorHex,
                                isPinned = isPinned,
                                checklists = checklists,
                                linkedPhotoPath = photo
                            )
                        },
                        onToggleChecklist = { note, index ->
                            viewModel.toggleChecklistItem(note, index)
                        },
                        onTogglePin = { viewModel.togglePinNote(it) },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onSaveCategory = { id, name, hex ->
                            viewModel.saveCategory(id, name, hex)
                        },
                        onDeleteCategory = { id ->
                            viewModel.deleteCategory(id)
                        }
                    )
                }

                ScreenTab.REMINDERS -> {
                    RemindersScreen(
                        reminders = reminders,
                        onSaveReminder = { id, title, desc, time, sound, vibe ->
                            viewModel.saveReminder(id, title, desc, time, sound, vibe)
                        },
                        onToggleComplete = { viewModel.toggleReminderComplete(it) },
                        onDeleteReminder = { viewModel.deleteReminder(it) }
                    )
                }

                ScreenTab.TRIPS -> {
                    TripsScreen(
                        trips = trips,
                        activeTrip = activeTrip,
                        totalDistance = totalDistance,
                        totalCost = totalCost,
                        completedCount = completedTripCount,
                        currentLocation = currentLocation,
                        currentSpeedKmh = currentSpeedKmh,
                        availableTags = allMissionTags,
                        monthStat = monthMissionsStat,
                        onNextMonth = { viewModel.nextStatMonth() },
                        onPrevMonth = { viewModel.prevStatMonth() },
                        onCurrentMonth = { viewModel.setCurrentStatMonth() },
                        onRecordWaypoint = { viewModel.recordCurrentWaypoint() },
                        onQuickCheckIn = { tag, loc, cost, notes, mode ->
                            viewModel.quickCheckIn(tag, loc, null, null, cost, notes, mode)
                        },
                        onStartTrip = { title, startLoc, mode, purpose, tag ->
                            viewModel.startTrip(title, startLoc, mode, purpose, tag)
                        },
                        onFinishActiveTrip = { trip, endLoc, dist, cost, notes, photo ->
                            viewModel.finishActiveTrip(trip, endLoc, dist, cost, notes, photo)
                        },
                        onCancelActiveTrip = { viewModel.cancelActiveTrip(it) },
                        onSaveManualTrip = { title, startLoc, endLoc, mode, startT, endT, dist, cost, purpose, notes, tag ->
                            viewModel.saveManualTrip(title, startLoc, endLoc, mode, startT, endT, dist, cost, purpose, notes, tag)
                        },
                        onDeleteTrip = { viewModel.deleteTrip(it) }
                    )
                }

                ScreenTab.PHOTOS -> {
                    TimestampCameraScreen(
                        photos = photos,
                        currentLocation = currentLocation,
                        isFetchingLocation = isFetchingLocation,
                        onRefreshLocation = { viewModel.refreshLocation() },
                        onProcessPhoto = { filePath, style, caption, tag ->
                            viewModel.processAndSaveStampedPhoto(
                                sourceTempFilePath = filePath,
                                style = style,
                                caption = caption,
                                tag = tag
                            )
                        },
                        onDeletePhoto = { viewModel.deletePhoto(it) }
                    )
                }
            }
        }
    }

    // Quick Note Create / Edit Dialog
    if (quickCreateNoteOpen || selectedNoteForEdit != null) {
        val target = selectedNoteForEdit
        NoteEditDialog(
            note = target,
            availableCategories = categories,
            onDismiss = {
                quickCreateNoteOpen = false
                selectedNoteForEdit = null
            },
            onManageCategoriesClick = {
                isCategoryManagementOpen = true
            },
            onSave = { id, title, content, cats, colorHex, isPinned, checklists, photo ->
                viewModel.saveNote(
                    id = id,
                    title = title,
                    content = content,
                    categories = cats,
                    colorHex = colorHex,
                    isPinned = isPinned,
                    checklists = checklists,
                    linkedPhotoPath = photo
                )
                quickCreateNoteOpen = false
                selectedNoteForEdit = null
            }
        )
    }

    // Category Management Dialog
    if (isCategoryManagementOpen) {
        CategoryManagementDialog(
            categories = categories,
            onDismiss = { isCategoryManagementOpen = false },
            onSaveCategory = { id, name, hex ->
                viewModel.saveCategory(id, name, hex)
            },
            onDeleteCategory = { id ->
                viewModel.deleteCategory(id)
            }
        )
    }

    // Quick Reminder Create / Edit Dialog
    if (quickCreateReminderOpen || selectedReminderForEdit != null) {
        val target = selectedReminderForEdit ?: if (prefilledReminderTime > 0L) {
            Reminder(
                title = "",
                triggerTime = prefilledReminderTime,
                enableSound = true,
                enableVibration = true
            )
        } else null

        ReminderEditDialog(
            reminder = target,
            onDismiss = {
                quickCreateReminderOpen = false
                selectedReminderForEdit = null
                prefilledReminderTime = 0L
            },
            onSaveReminder = { id, title, desc, time, sound, vibe ->
                viewModel.saveReminder(id, title, desc, time, sound, vibe)
                quickCreateReminderOpen = false
                selectedReminderForEdit = null
                prefilledReminderTime = 0L
            }
        )
    }

    // Quick Task Create / Edit Dialog
    if (quickCreateTaskOpen || selectedTaskForEdit != null) {
        TaskEditDialog(
            task = selectedTaskForEdit,
            availableProjects = allProjects,
            onDismiss = {
                quickCreateTaskOpen = false
                selectedTaskForEdit = null
            },
            onSave = { id, title, desc, status, priority, dueDate, project, subtasks, estHours ->
                viewModel.saveTask(
                    id = id,
                    title = title,
                    description = desc,
                    status = status,
                    priority = priority,
                    dueDate = dueDate,
                    project = project,
                    subtasks = subtasks,
                    estimatedHours = estHours
                )
                quickCreateTaskOpen = false
                selectedTaskForEdit = null
            }
        )
    }

    // Quick Check-in Dialog
    if (quickCheckInOpen) {
        QuickMissionCheckInDialog(
            currentLocation = currentLocation,
            availableTags = allMissionTags,
            onDismiss = { quickCheckInOpen = false },
            onCheckIn = { tag, loc, cost, notes, mode ->
                viewModel.quickCheckIn(tag, loc, null, null, cost, notes, mode)
                quickCheckInOpen = false
                viewModel.setTab(ScreenTab.TRIPS)
            }
        )
    }

    // Quick Start Trip Dialog
    if (quickStartTripOpen) {
        TripStartDialog(
            currentLocation = currentLocation,
            availableTags = allMissionTags,
            onDismiss = { quickStartTripOpen = false },
            onStartTrip = { title, startLoc, mode, purpose, tag ->
                viewModel.startTrip(title, startLoc, mode, purpose, tag)
                viewModel.setTab(ScreenTab.TRIPS)
                quickStartTripOpen = false
            }
        )
    }

    // Finish Active Trip Dialog
    if (finishActiveTripOpen && activeTrip != null) {
        val trip = activeTrip!!
        TripFinishDialog(
            trip = trip,
            currentLocation = currentLocation,
            onDismiss = { finishActiveTripOpen = false },
            onFinishTrip = { endLoc, dist, cost, notes ->
                viewModel.finishActiveTrip(trip, endLoc, dist, cost, notes, null)
                finishActiveTripOpen = false
            }
        )
    }

    // Photo Detail Dialog
    if (selectedPhotoForDetail != null) {
        PhotoDetailDialog(
            photo = selectedPhotoForDetail!!,
            onDismiss = { selectedPhotoForDetail = null },
            onDelete = {
                viewModel.deletePhoto(it)
                selectedPhotoForDetail = null
            }
        )
    }

    // Theme & Anti-glare Selection Dialog
    if (isThemeDialogOpen) {
        ThemeSelectDialog(
            currentTheme = themeSetting,
            onDismiss = { isThemeDialogOpen = false },
            onSelectTheme = { selected ->
                onSelectTheme(selected)
                isThemeDialogOpen = false
            }
        )
    }
}
