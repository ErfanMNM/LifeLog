package com.example.ui.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Reminder
import com.example.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderEditDialog(
    reminder: Reminder?,
    onDismiss: () -> Unit,
    onSaveReminder: (
        id: Long,
        title: String,
        description: String,
        triggerTime: Long,
        enableSound: Boolean,
        enableVibration: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(reminder?.title ?: "") }
    var description by remember { mutableStateOf(reminder?.description ?: "") }
    var enableSound by remember { mutableStateOf(reminder?.enableSound ?: true) }
    var enableVibration by remember { mutableStateOf(reminder?.enableVibration ?: true) }

    // Date & Time calculation
    val initialCalendar = remember {
        Calendar.getInstance().apply {
            if (reminder != null) {
                timeInMillis = reminder.triggerTime
            } else {
                // Default: 1 hour from now
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        }
    }

    var selectedYear by remember { mutableIntStateOf(initialCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(initialCalendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(initialCalendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableIntStateOf(initialCalendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(initialCalendar.get(Calendar.MINUTE)) }

    fun calculateTriggerTime(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, selectedMonth)
        cal.set(Calendar.DAY_OF_MONTH, selectedDay)
        cal.set(Calendar.HOUR_OF_DAY, selectedHour)
        cal.set(Calendar.MINUTE, selectedMinute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .systemBarsPadding()
            .imePadding()
            .padding(vertical = 12.dp),
        title = {
            Text(
                text = if (reminder == null) "⏰ Đặt lời nhắc mới" else "⏰ Chỉnh sửa lời nhắc",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề lời nhắc") },
                    placeholder = { Text("vd: Họp dự án, Uống thuốc, Gọi điện...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả / Chi tiết (tùy chọn)") },
                    placeholder = { Text("Nội dung nhắc nhở khi đến giờ...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_desc_input")
                )

                // Date Presets
                Text(
                    text = "Ngày nhắc hẹn:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val nowCal = Calendar.getInstance()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presets = listOf(
                        "Hôm nay" to 0,
                        "Ngày mai" to 1,
                        "2 ngày nữa" to 2,
                        "Cuối tuần" to (Calendar.SATURDAY - nowCal.get(Calendar.DAY_OF_WEEK) + 7) % 7
                    )

                    presets.forEach { (label, dayOffset) ->
                        val targetCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }
                        val isSelected = selectedDay == targetCal.get(Calendar.DAY_OF_MONTH) &&
                            selectedMonth == targetCal.get(Calendar.MONTH)

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedYear = targetCal.get(Calendar.YEAR)
                                selectedMonth = targetCal.get(Calendar.MONTH)
                                selectedDay = targetCal.get(Calendar.DAY_OF_MONTH)
                            },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }

                // Time Presets
                Text(
                    text = "Giờ nhắc hẹn:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val timePresets = listOf(
                        "08:00 (Sáng)" to Pair(8, 0),
                        "12:00 (Trưa)" to Pair(12, 0),
                        "14:30 (Chiều)" to Pair(14, 30),
                        "18:00 (Tối)" to Pair(18, 0),
                        "21:00 (Đêm)" to Pair(21, 0)
                    )

                    timePresets.forEach { (label, time) ->
                        val isSelected = selectedHour == time.first && selectedMinute == time.second
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedHour = time.first
                                selectedMinute = time.second
                            },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        )
                    }
                }

                // Custom Time Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = "%02d".format(selectedHour),
                        onValueChange = {
                            val v = it.toIntOrNull()
                            if (v != null && v in 0..23) selectedHour = v
                        },
                        label = { Text("Giờ (0-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = "%02d".format(selectedMinute),
                        onValueChange = {
                            val v = it.toIntOrNull()
                            if (v != null && v in 0..59) selectedMinute = v
                        },
                        label = { Text("Phút (0-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Scheduled preview card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Thời gian nhắc nhở dự kiến:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = DateUtils.formatFullDateTime(calculateTriggerTime()),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Sound & Vibration Toggles
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Tùy chỉnh thông báo:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Sound Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (enableSound) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Âm thanh thông báo", fontWeight = FontWeight.Medium)
                                    Text(
                                        if (enableSound) "Phát chuông khi đến giờ" else "Tắt chuông",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Switch(
                                checked = enableSound,
                                onCheckedChange = { enableSound = it },
                                modifier = Modifier.testTag("reminder_sound_switch")
                            )
                        }

                        // Vibration Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = if (enableVibration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Rung thông báo", fontWeight = FontWeight.Medium)
                                    Text(
                                        if (enableVibration) "Rung máy khi đến giờ" else "Không rung",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Switch(
                                checked = enableVibration,
                                onCheckedChange = { enableVibration = it },
                                modifier = Modifier.testTag("reminder_vibrate_switch")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val trigger = calculateTriggerTime()
                        onSaveReminder(
                            reminder?.id ?: 0L,
                            title.trim(),
                            description.trim(),
                            trigger,
                            enableSound,
                            enableVibration
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("save_reminder_btn")
            ) {
                Text("Lưu lời nhắc")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
