package com.example.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Task
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TaskSubtask
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate500
import com.example.util.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    task: Task?,
    availableProjects: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        description: String,
        status: String,
        priority: String,
        dueDate: Long,
        project: String,
        subtasks: List<TaskSubtask>,
        estimatedHours: Double
    ) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var status by remember { mutableStateOf(task?.status ?: TaskStatus.TODO.name) }
    var priority by remember { mutableStateOf(task?.priority ?: TaskPriority.MEDIUM.name) }
    var dueDate by remember { mutableLongStateOf(task?.dueDate ?: 0L) }
    var project by remember { mutableStateOf(task?.project ?: "Công việc") }
    var estimatedHoursText by remember {
        mutableStateOf(if ((task?.estimatedHours ?: 0.0) > 0.0) task!!.estimatedHours.toString() else "")
    }

    val subtasks = remember {
        mutableStateListOf<TaskSubtask>().apply {
            if (task != null) {
                addAll(task.getSubtasks())
            }
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }

    val defaultSuggestedProjects = remember {
        val list = mutableListOf("Công việc", "Dự án", "Báo cáo", "Họp hành", "Phát triển", "Khách hàng", "Cá nhân")
        availableProjects.forEach { if (!list.contains(it) && it.isNotBlank()) list.add(it) }
        list
    }

    fun addSubtask() {
        if (newSubtaskText.isNotBlank()) {
            subtasks.add(TaskSubtask(newSubtaskText.trim(), false))
            newSubtaskText = ""
            coroutineScope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (task == null) "Thêm công việc mới" else "Chỉnh sửa công việc",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable form content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Title
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Tên công việc / Task *") },
                            placeholder = { Text("vd: Báo cáo tuần, Gửi hợp đồng...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_title_input")
                        )

                        // Description
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Mô tả chi tiết") },
                            placeholder = { Text("Yêu cầu cụ thể, người liên hệ, link tài liệu...") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_desc_input")
                        )

                        // Priority selection
                        Column {
                            Text(
                                text = "Mức độ ưu tiên",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val priorities = listOf(
                                    TaskPriority.LOW to Slate500,
                                    TaskPriority.MEDIUM to PrimaryBlue,
                                    TaskPriority.HIGH to AccentAmber,
                                    TaskPriority.URGENT to AccentRose
                                )
                                priorities.forEach { (p, col) ->
                                    val isSelected = priority == p.name
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) col else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { priority = p.name }
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) col else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = p.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Status selection
                        Column {
                            Text(
                                text = "Trạng thái thực hiện",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val statuses = listOf(
                                    TaskStatus.TODO to MaterialTheme.colorScheme.primary,
                                    TaskStatus.IN_PROGRESS to AccentAmber,
                                    TaskStatus.COMPLETED to AccentEmerald
                                )
                                statuses.forEach { (st, col) ->
                                    val isSelected = status == st.name
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) col.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { status = st.name }
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) col else Color.Transparent,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = st.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) col else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Project / Category Tag
                        Column {
                            Text(
                                text = "Dự án / Phân loại",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                defaultSuggestedProjects.forEach { proj ->
                                    val isSelected = project.equals(proj, ignoreCase = true)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { project = proj },
                                        label = { Text(proj, fontSize = 12.sp) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else null,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = project,
                                onValueChange = { project = it },
                                label = { Text("Tên dự án tùy chỉnh") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Due Date (Hạn chót)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hạn chót hoàn thành",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (dueDate > 0L) {
                                    TextButton(
                                        onClick = { dueDate = 0L },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Text("Xóa hạn chót", fontSize = 12.sp, color = AccentRose)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            // Quick buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val cal = Calendar.getInstance()
                                OutlinedButton(
                                    onClick = {
                                        cal.set(Calendar.HOUR_OF_DAY, 18)
                                        cal.set(Calendar.MINUTE, 0)
                                        dueDate = cal.timeInMillis
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Hôm nay", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        cal.add(Calendar.DAY_OF_YEAR, 1)
                                        cal.set(Calendar.HOUR_OF_DAY, 18)
                                        cal.set(Calendar.MINUTE, 0)
                                        dueDate = cal.timeInMillis
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Ngày mai", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        cal.add(Calendar.DAY_OF_YEAR, 3)
                                        cal.set(Calendar.HOUR_OF_DAY, 18)
                                        cal.set(Calendar.MINUTE, 0)
                                        dueDate = cal.timeInMillis
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+3 ngày", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            if (dueDate > 0L) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Hạn chót: ${DateUtils.formatShortDate(dueDate)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // Estimated Hours
                        OutlinedTextField(
                            value = estimatedHoursText,
                            onValueChange = { estimatedHoursText = it },
                            label = { Text("Số giờ dự kiến (giờ)") },
                            placeholder = { Text("vd: 2.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Subtasks checklist section
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Các bước thực hiện (Subtasks)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (subtasks.isNotEmpty()) {
                                    val completedCount = subtasks.count { it.isCompleted }
                                    Text(
                                        text = "$completedCount/${subtasks.size} hoàn thành",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Subtask items list
                            subtasks.forEachIndexed { index, subtask ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = subtask.isCompleted,
                                        onCheckedChange = { isChecked ->
                                            subtasks[index] = subtask.copy(isCompleted = isChecked)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = AccentEmerald
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = subtask.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { subtasks.removeAt(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Xóa bước",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Add subtask input row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newSubtaskText,
                                    onValueChange = { newSubtaskText = it },
                                    placeholder = { Text("Thêm bước con...", fontSize = 13.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { addSubtask() }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("task_subtask_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { addSubtask() },
                                    enabled = newSubtaskText.isNotBlank(),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dialog bottom action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Hủy")
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val estHours = estimatedHoursText.toDoubleOrNull() ?: 0.0
                                    onSave(
                                        task?.id ?: 0L,
                                        title.trim(),
                                        description.trim(),
                                        status,
                                        priority,
                                        dueDate,
                                        project.trim(),
                                        subtasks.toList(),
                                        estHours
                                    )
                                }
                            },
                            enabled = title.isNotBlank(),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("task_save_button")
                        ) {
                            Text(if (task == null) "Tạo công việc" else "Lưu thay đổi")
                        }
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (dueDate > 0L) dueDate else System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sel = datePickerState.selectedDateMillis
                        if (sel != null) {
                            // Set to 18:00 of that selected day
                            val cal = Calendar.getInstance().apply {
                                timeInMillis = sel
                                set(Calendar.HOUR_OF_DAY, 18)
                                set(Calendar.MINUTE, 0)
                            }
                            dueDate = cal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
