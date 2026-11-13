package com.example.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Note
import com.example.data.model.Reminder
import com.example.data.model.Task
import com.example.data.model.TaskPriority
import com.example.data.model.TimestampPhoto
import com.example.data.model.Trip
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate500
import com.example.util.CalendarDay
import com.example.util.CalendarUtils
import com.example.util.DateUtils
import java.io.File
import java.util.Calendar

@Composable
fun CalendarScreen(
    notes: List<Note>,
    reminders: List<Reminder>,
    trips: List<Trip>,
    photos: List<TimestampPhoto>,
    tasks: List<Task> = emptyList(),
    onNoteClick: (Note) -> Unit,
    onReminderClick: (Reminder) -> Unit,
    onToggleReminder: (Reminder) -> Unit,
    onPhotoClick: (TimestampPhoto) -> Unit,
    onTaskClick: (Task) -> Unit = {},
    onToggleTask: (Task) -> Unit = {},
    onCreateReminderForDate: (Long) -> Unit,
    onCreateNoteClick: () -> Unit
) {
    val initialCal = remember { Calendar.getInstance() }
    var currentYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) } // 0-based
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedAgendaFilter by remember { mutableStateOf("Tất cả") }

    val daysInMonth = remember(currentYear, currentMonth) {
        CalendarUtils.getMonthDays(currentYear, currentMonth)
    }

    // Filter items for selected day
    val dayTasks = remember(selectedTimestamp, tasks) {
        tasks.filter { it.dueDate > 0L && CalendarUtils.isSameDay(it.dueDate, selectedTimestamp) }
    }
    val dayReminders = remember(selectedTimestamp, reminders) {
        reminders.filter { CalendarUtils.isSameDay(it.triggerTime, selectedTimestamp) }
    }
    val dayNotes = remember(selectedTimestamp, notes) {
        notes.filter { CalendarUtils.isSameDay(it.createdTimestamp, selectedTimestamp) || CalendarUtils.isSameDay(it.updatedTimestamp, selectedTimestamp) }
    }
    val dayTrips = remember(selectedTimestamp, trips) {
        trips.filter { CalendarUtils.isSameDay(it.startTime, selectedTimestamp) }
    }
    val dayPhotos = remember(selectedTimestamp, photos) {
        photos.filter { CalendarUtils.isSameDay(it.timestamp, selectedTimestamp) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Month / Year Navigation Header
        item {
            Card(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = CalendarUtils.formatMonthYear(currentYear, currentMonth),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val today = Calendar.getInstance()
                                        currentYear = today.get(Calendar.YEAR)
                                        currentMonth = today.get(Calendar.MONTH)
                                        selectedTimestamp = today.timeInMillis
                                    }
                                    .testTag("calendar_today_btn")
                            ) {
                                Text(
                                    text = "Hôm nay",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    if (currentMonth == 0) {
                                        currentMonth = 11
                                        currentYear -= 1
                                    } else {
                                        currentMonth -= 1
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("calendar_prev_month_btn")
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Tháng trước")
                            }

                            IconButton(
                                onClick = {
                                    if (currentMonth == 11) {
                                        currentMonth = 0
                                        currentYear += 1
                                    } else {
                                        currentMonth += 1
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("calendar_next_month_btn")
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Tháng sau")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Days of week header (T2 -> CN)
                    val weekdays = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        weekdays.forEachIndexed { idx, day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (idx == 6) AccentRose else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Month Calendar Grid (7 columns per row)
                    val rows = daysInMonth.chunked(7)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rows.forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                week.forEach { day ->
                                    val isSelected = CalendarUtils.isSameDay(day.timestamp, selectedTimestamp)
                                    val hasReminders = reminders.any { CalendarUtils.isSameDay(it.triggerTime, day.timestamp) }
                                    val hasNotes = notes.any { CalendarUtils.isSameDay(it.createdTimestamp, day.timestamp) || CalendarUtils.isSameDay(it.updatedTimestamp, day.timestamp) }
                                    val hasTrips = trips.any { CalendarUtils.isSameDay(it.startTime, day.timestamp) }
                                    val hasPhotos = photos.any { CalendarUtils.isSameDay(it.timestamp, day.timestamp) }

                                    CalendarDayCell(
                                        day = day,
                                        isSelected = isSelected,
                                        hasReminders = hasReminders,
                                        hasNotes = hasNotes,
                                        hasTrips = hasTrips,
                                        hasPhotos = hasPhotos,
                                        onClick = { selectedTimestamp = day.timestamp },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Legend indicators row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendIndicator(color = AccentAmber, label = "Lời nhắc")
                        Spacer(modifier = Modifier.width(12.dp))
                        LegendIndicator(color = PrimaryBlue, label = "Ghi chú")
                        Spacer(modifier = Modifier.width(12.dp))
                        LegendIndicator(color = AccentEmerald, label = "Di chuyển")
                        Spacer(modifier = Modifier.width(12.dp))
                        LegendIndicator(color = AccentPurple, label = "Ảnh")
                    }
                }
            }
        }

        // Selected Date Agenda Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = CalendarUtils.formatSelectedDayHeader(selectedTimestamp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val totalActivities = dayTasks.size + dayReminders.size + dayNotes.size + dayTrips.size + dayPhotos.size
                        Text(
                            text = if (totalActivities > 0)
                                "$totalActivities hoạt động (${dayTasks.size} việc, ${dayReminders.size} nhắc nhở, ${dayNotes.size} ghi chú, ${dayTrips.size} chuyến đi, ${dayPhotos.size} ảnh)"
                            else "Không có hoạt động nào",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { onCreateReminderForDate(selectedTimestamp) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(AccentAmber.copy(alpha = 0.15f), CircleShape)
                                .testTag("add_reminder_on_calendar_btn")
                        ) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = "Thêm lời nhắc ngày này",
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onCreateNoteClick,
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .testTag("add_note_on_calendar_btn")
                        ) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = "Thêm ghi chú ngày này",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Agenda Filters
                val agendaFilters = listOf("Tất cả", "Công việc", "Lời nhắc", "Ghi chú", "Chuyến đi", "Ảnh")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(agendaFilters) { filter ->
                        FilterChip(
                            selected = selectedAgendaFilter == filter,
                            onClick = { selectedAgendaFilter = filter },
                            label = { Text(filter, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // Agenda Items List
        val showAll = selectedAgendaFilter == "Tất cả"

        val hasAnyItem = (dayTasks.isNotEmpty() && (showAll || selectedAgendaFilter == "Công việc")) ||
            (dayReminders.isNotEmpty() && (showAll || selectedAgendaFilter == "Lời nhắc")) ||
            (dayNotes.isNotEmpty() && (showAll || selectedAgendaFilter == "Ghi chú")) ||
            (dayTrips.isNotEmpty() && (showAll || selectedAgendaFilter == "Chuyến đi")) ||
            (dayPhotos.isNotEmpty() && (showAll || selectedAgendaFilter == "Ảnh"))

        if (!hasAnyItem) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Today,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Không có hoạt động nào trong ngày này",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onCreateReminderForDate(selectedTimestamp) }
                            ) {
                                Text("+ Đặt lời nhắc")
                            }
                            Button(
                                onClick = onCreateNoteClick
                            ) {
                                Text("+ Tạo ghi chú")
                            }
                        }
                    }
                }
            }
        } else {
            // Work Tasks Section
            if (dayTasks.isNotEmpty() && (showAll || selectedAgendaFilter == "Công việc")) {
                item {
                    Text(
                        text = "📋 Hạn chót công việc (${dayTasks.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(dayTasks, key = { "task_${it.id}" }) { t ->
                    val priorityColor = when (t.priority) {
                        TaskPriority.URGENT.name -> AccentRose
                        TaskPriority.HIGH.name -> AccentAmber
                        TaskPriority.LOW.name -> Slate500
                        else -> PrimaryBlue
                    }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTaskClick(t) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = t.isCompleted,
                                onCheckedChange = { onToggleTask(t) },
                                colors = CheckboxDefaults.colors(checkedColor = AccentEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = priorityColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = t.taskPriority.label,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = priorityColor,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = t.project,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = t.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (t.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                if (t.description.isNotBlank()) {
                                    Text(
                                        text = t.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Reminders Section
            if (dayReminders.isNotEmpty() && (showAll || selectedAgendaFilter == "Lời nhắc")) {
                item {
                    Text(
                        text = "⏰ Lời nhắc hẹn (${dayReminders.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(dayReminders, key = { "rem_${it.id}" }) { rem ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onReminderClick(rem) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = rem.isCompleted,
                                onCheckedChange = { onToggleReminder(rem) },
                                colors = CheckboxDefaults.colors(checkedColor = AccentEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rem.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (rem.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Text(
                                    text = "⏰ ${DateUtils.formatTime(rem.triggerTime)} ${if (rem.description.isNotBlank()) "• ${rem.description}" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // Notes Section
            if (dayNotes.isNotEmpty() && (showAll || selectedAgendaFilter == "Ghi chú")) {
                item {
                    Text(
                        text = "📝 Ghi chú (${dayNotes.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(dayNotes, key = { "note_${it.id}" }) { note ->
                    val cardBg = try {
                        Color(android.graphics.Color.parseColor(note.colorHex))
                    } catch (_: Exception) { MaterialTheme.colorScheme.surface }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNoteClick(note) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = note.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = note.getCategoriesList().firstOrNull() ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.6f)
                                )
                            }
                            if (note.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Black.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Trips Section
            if (dayTrips.isNotEmpty() && (showAll || selectedAgendaFilter == "Chuyến đi")) {
                item {
                    Text(
                        text = "🚗 Chuyến đi di chuyển (${dayTrips.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentEmerald,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(dayTrips, key = { "trip_${it.id}" }) { trip ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.BusinessCenter,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (trip.tagName.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AccentEmerald.copy(alpha = 0.15f),
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = trip.tagName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentEmerald,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = trip.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = trip.startLocation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = DateUtils.formatTime(trip.startTime),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Photos Section
            if (dayPhotos.isNotEmpty() && (showAll || selectedAgendaFilter == "Ảnh")) {
                item {
                    Text(
                        text = "📷 Ảnh Timestamp (${dayPhotos.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                items(dayPhotos, key = { "photo_${it.id}" }) { photo ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPhotoClick(photo) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (File(photo.filePath).exists()) {
                                    AsyncImage(
                                        model = File(photo.filePath),
                                        contentDescription = photo.caption,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = AccentPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🏷️ ${photo.tag} ${if (photo.caption.isNotBlank()) "• ${photo.caption}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = photo.locationAddress,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = DateUtils.formatTime(photo.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    hasReminders: Boolean,
    hasNotes: Boolean,
    hasTrips: Boolean,
    hasPhotos: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    day.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (day.isToday && !isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                } else Modifier
            )
            .clickable { onClick() }
            .testTag("cal_day_${day.dayOfMonth}_${day.month}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val textColor = when {
            isSelected -> Color.White
            !day.isCurrentMonth -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            day.isToday -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }

        Text(
            text = "${day.dayOfMonth}",
            fontSize = 13.sp,
            fontWeight = if (day.isToday || isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            color = textColor
        )

        // Activity Dots
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasReminders) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else AccentAmber)
                )
            }
            if (hasNotes) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else PrimaryBlue)
                )
            }
            if (hasTrips) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else AccentEmerald)
                )
            }
            if (hasPhotos) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else AccentPurple)
                )
            }
        }
    }
}

@Composable
fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
    }
}
