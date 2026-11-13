package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority(val label: String, val level: Int) {
    LOW("Thấp", 1),
    MEDIUM("Trung bình", 2),
    HIGH("Cao", 3),
    URGENT("Khẩn cấp", 4);

    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}

enum class TaskStatus(val label: String) {
    TODO("Cần làm"),
    IN_PROGRESS("Đang làm"),
    COMPLETED("Hoàn thành");

    companion object {
        fun fromString(value: String): TaskStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TODO
        }
    }
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val status: String = TaskStatus.TODO.name, // TODO, IN_PROGRESS, COMPLETED
    val priority: String = TaskPriority.MEDIUM.name, // LOW, MEDIUM, HIGH, URGENT
    val dueDate: Long = 0L, // Millis timestamp, 0 if none
    val project: String = "Công việc", // Project / Category tag: "Dự án", "Báo cáo", "Họp", etc.
    val subtasksJson: String = "", // serialized subtasks: "title:::true|||title2:::false"
    val estimatedHours: Double = 0.0,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val completedTimestamp: Long = 0L
) {
    val isCompleted: Boolean
        get() = status == TaskStatus.COMPLETED.name

    val taskStatus: TaskStatus
        get() = TaskStatus.fromString(status)

    val taskPriority: TaskPriority
        get() = TaskPriority.fromString(priority)

    fun getSubtasks(): List<TaskSubtask> {
        return TaskSubtaskSerializer.parse(subtasksJson)
    }

    val subtasksProgress: Pair<Int, Int> // (completedCount, totalCount)
        get() {
            val list = getSubtasks()
            if (list.isEmpty()) return Pair(0, 0)
            return Pair(list.count { it.isCompleted }, list.size)
        }
}

data class TaskSubtask(
    val title: String,
    val isCompleted: Boolean
)

object TaskSubtaskSerializer {
    fun parse(serialized: String): List<TaskSubtask> {
        if (serialized.isBlank()) return emptyList()
        return serialized.split("|||").mapNotNull { entry ->
            val parts = entry.split(":::")
            if (parts.size >= 2) {
                TaskSubtask(title = parts[0], isCompleted = parts[1].toBoolean())
            } else if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                TaskSubtask(title = parts[0], isCompleted = false)
            } else null
        }
    }

    fun serialize(items: List<TaskSubtask>): String {
        return items.joinToString("|||") { "${it.title.replace(":::", " ").replace("|||", " ")}:::${it.isCompleted}" }
    }
}
