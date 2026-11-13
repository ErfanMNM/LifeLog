package com.example.data.repository

import com.example.data.db.CategoryDao
import com.example.data.db.NoteDao
import com.example.data.db.ReminderDao
import com.example.data.db.TaskDao
import com.example.data.db.TimestampPhotoDao
import com.example.data.db.TripDao
import com.example.data.model.Category
import com.example.data.model.Note
import com.example.data.model.Reminder
import com.example.data.model.Task
import com.example.data.model.TaskStatus
import com.example.data.model.TaskSubtaskSerializer
import com.example.data.model.TimestampPhoto
import com.example.data.model.Trip
import kotlinx.coroutines.flow.Flow

class PersonalRepository(
    private val noteDao: NoteDao,
    private val tripDao: TripDao,
    private val timestampPhotoDao: TimestampPhotoDao,
    private val categoryDao: CategoryDao,
    private val reminderDao: ReminderDao,
    private val taskDao: TaskDao
) {
    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun insertOrUpdateCategory(category: Category): Long {
        return if (category.id == 0L) {
            categoryDao.insert(category)
        } else {
            categoryDao.update(category)
            category.id
        }
    }
    suspend fun deleteCategory(id: Long) = categoryDao.deleteById(id)

    // Reminders
    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()
    val activeReminderCount: Flow<Int> = reminderDao.getActiveReminderCount()
    fun getUpcomingReminders(currentMillis: Long): Flow<List<Reminder>> =
        reminderDao.getUpcomingReminders(currentMillis)

    suspend fun getReminderById(id: Long): Reminder? = reminderDao.getReminderById(id)
    suspend fun insertOrUpdateReminder(reminder: Reminder): Long {
        return if (reminder.id == 0L) {
            reminderDao.insert(reminder)
        } else {
            reminderDao.update(reminder)
            reminder.id
        }
    }
    suspend fun setReminderCompleted(id: Long, isCompleted: Boolean) =
        reminderDao.setCompleted(id, isCompleted)
    suspend fun deleteReminder(id: Long) = reminderDao.deleteById(id)

    // Notes
    val activeNotes: Flow<List<Note>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()
    val activeNoteCount: Flow<Int> = noteDao.getActiveNoteCount()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)
    suspend fun insertOrUpdateNote(note: Note): Long {
        return if (note.id == 0L) {
            noteDao.insert(note)
        } else {
            noteDao.update(note)
            note.id
        }
    }
    suspend fun deleteNote(id: Long) = noteDao.deleteById(id)
    suspend fun togglePinNote(id: Long, isPinned: Boolean) = noteDao.setPinned(id, isPinned)
    suspend fun toggleArchiveNote(id: Long, isArchived: Boolean) = noteDao.setArchived(id, isArchived)

    // Trips
    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips()
    val activeTrip: Flow<Trip?> = tripDao.getActiveTrip()
    val totalDistance: Flow<Double> = tripDao.getTotalDistance()
    val totalCost: Flow<Long> = tripDao.getTotalCost()
    val completedTripCount: Flow<Int> = tripDao.getCompletedTripCount()

    suspend fun getTripById(id: Long): Trip? = tripDao.getTripById(id)
    suspend fun insertOrUpdateTrip(trip: Trip): Long {
        return if (trip.id == 0L) {
            tripDao.insert(trip)
        } else {
            tripDao.update(trip)
            trip.id
        }
    }
    suspend fun deleteTrip(id: Long) = tripDao.deleteById(id)

    // Timestamp Photos
    val allPhotos: Flow<List<TimestampPhoto>> = timestampPhotoDao.getAllPhotos()
    val photoCount: Flow<Int> = timestampPhotoDao.getPhotoCount()

    fun getPhotosForTrip(tripId: Long): Flow<List<TimestampPhoto>> = timestampPhotoDao.getPhotosForTrip(tripId)
    fun getPhotosForNote(noteId: Long): Flow<List<TimestampPhoto>> = timestampPhotoDao.getPhotosForNote(noteId)
    suspend fun insertPhoto(photo: TimestampPhoto): Long = timestampPhotoDao.insert(photo)
    suspend fun deletePhoto(id: Long) = timestampPhotoDao.deleteById(id)

    // Tasks (Work Tasks)
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val activeTasks: Flow<List<Task>> = taskDao.getActiveTasks()
    val pendingTaskCount: Flow<Int> = taskDao.getPendingTaskCount()
    val completedTaskCount: Flow<Int> = taskDao.getCompletedTaskCount()
    val urgentTaskCount: Flow<Int> = taskDao.getUrgentTaskCount()

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun insertOrUpdateTask(task: Task): Long {
        return if (task.id == 0L) {
            taskDao.insert(task)
        } else {
            taskDao.update(task)
            task.id
        }
    }

    suspend fun toggleTaskComplete(task: Task) {
        val newStatus = if (task.isCompleted) TaskStatus.TODO.name else TaskStatus.COMPLETED.name
        val completedTime = if (task.isCompleted) 0L else System.currentTimeMillis()
        taskDao.updateStatus(task.id, newStatus, completedTime)
    }

    suspend fun updateTaskStatus(id: Long, status: String) {
        val completedTime = if (status == TaskStatus.COMPLETED.name) System.currentTimeMillis() else 0L
        taskDao.updateStatus(id, status, completedTime)
    }

    suspend fun toggleSubtask(task: Task, subtaskIndex: Int) {
        val currentSubtasks = task.getSubtasks().toMutableList()
        if (subtaskIndex in currentSubtasks.indices) {
            val item = currentSubtasks[subtaskIndex]
            currentSubtasks[subtaskIndex] = item.copy(isCompleted = !item.isCompleted)
            val updatedJson = TaskSubtaskSerializer.serialize(currentSubtasks)
            val updatedTask = task.copy(subtasksJson = updatedJson)
            taskDao.update(updatedTask)
        }
    }

    suspend fun deleteTask(id: Long) = taskDao.deleteById(id)
}
