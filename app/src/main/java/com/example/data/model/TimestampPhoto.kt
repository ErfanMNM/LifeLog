package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timestamp_photos")
data class TimestampPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val caption: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDateTime: String = "",
    val locationAddress: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val watermarkStyle: String = WatermarkStyle.MODERN_BADGE.name,
    val tag: String = "Cá nhân", // Check-in, Công việc, Lộ trình, Hóa đơn, Cá nhân, Điểm danh
    val linkedTripId: Long? = null,
    val linkedNoteId: Long? = null,
    val createdTimestamp: Long = System.currentTimeMillis()
)

enum class WatermarkStyle(val title: String, val description: String) {
    MODERN_BADGE("Huy hiệu hiện đại", "Nền tối mờ, ngày giờ to rõ, tọa độ GPS & địa chỉ"),
    CLASSIC_ORANGE("Chữ số cam cổ điển", "Font số điện tử màu cam nổi bật góc dưới"),
    MINIMAL_CLEAN("Tối giản thanh lịch", "Viền mảnh, chữ trắng bóng mờ trang nhã"),
    OFFICIAL_CHECKIN("Thẻ chấm công / Nhật ký", "Khung chi tiết: Dự án, vị trí, người thực hiện, thời gian thực")
}
