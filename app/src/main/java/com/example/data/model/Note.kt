package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Cá nhân", // Legacy single category fallback
    val categoriesJson: String = "", // Comma-separated or serialized list of category names
    val colorHex: String = "#FEF3C7", // Pastel accent
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val checklistItemsJson: String = "", // serialized checklist: "text1:::true|||text2:::false"
    val linkedTripId: Long? = null,
    val linkedPhotoPath: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis()
) {
    fun getCategoriesList(): List<String> {
        if (categoriesJson.isNotBlank()) {
            val list = categoriesJson.split(";;;").map { it.trim() }.filter { it.isNotEmpty() }
            if (list.isNotEmpty()) return list
        }
        return if (category.isNotBlank()) listOf(category) else listOf("Cá nhân")
    }

    fun hasCategory(cat: String): Boolean {
        return getCategoriesList().any { it.equals(cat, ignoreCase = true) }
    }
}

data class ChecklistItem(
    val text: String,
    val isDone: Boolean
)

object ChecklistSerializer {
    fun parse(serialized: String): List<ChecklistItem> {
        if (serialized.isBlank()) return emptyList()
        return serialized.split("|||").mapNotNull { entry ->
            val parts = entry.split(":::")
            if (parts.size >= 2) {
                ChecklistItem(text = parts[0], isDone = parts[1].toBoolean())
            } else if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                ChecklistItem(text = parts[0], isDone = false)
            } else null
        }
    }

    fun serialize(items: List<ChecklistItem>): String {
        return items.joinToString("|||") { "${it.text.replace(":::", " ").replace("|||", " ")}:::${it.isDone}" }
    }
}
