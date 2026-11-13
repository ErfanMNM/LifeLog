package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startLocation: String,
    val endLocation: String = "",
    val transportMode: String = "Xe máy", // Xe máy, Ô tô, Xe buýt, Đi bộ, Xe đạp, Tàu hỏa, Taxi / Grab
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L,
    val distanceKm: Double = 0.0,
    val costVnd: Long = 0L,
    val purpose: String = "Công việc", // Công việc, Cá nhân, Về quê, Mua sắm, Du lịch
    val notes: String = "",
    val status: String = "COMPLETED", // COMPLETED, IN_PROGRESS, PLANNED
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val photoProofPath: String? = null,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val tagName: String = "", // Tên Tag công tác (ví dụ: MSI, Samsung, FPT, Khách hàng A)
    val avgSpeedKmh: Double = 0.0, // Tốc độ trung bình (km/h)
    val maxSpeedKmh: Double = 0.0, // Tốc độ tối đa (km/h)
    val routePointsJson: String = "" // Danh sách các điểm tọa độ trên tuyến đường
) {
    fun getRouteWaypoints(): List<RouteWaypoint> {
        return RouteWaypointSerializer.parse(routePointsJson)
    }

    fun getDurationMillis(): Long {
        return if (endTime > startTime) endTime - startTime else 0L
    }

    fun getEffectiveAvgSpeedKmh(): Double {
        if (avgSpeedKmh > 0.0) return avgSpeedKmh
        val duration = getDurationMillis()
        if (distanceKm > 0.0 && duration > 0L) {
            val hours = duration.toDouble() / 3_600_000.0
            if (hours > 0.0001) {
                return (distanceKm / hours).coerceIn(0.0, 300.0)
            }
        }
        return 0.0
    }
}

data class RouteWaypoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val speedKmh: Double = 0.0,
    val label: String = ""
)

object RouteWaypointSerializer {
    private const val ITEM_SEP = "|||"
    private const val FIELD_SEP = ":::"

    fun serialize(points: List<RouteWaypoint>): String {
        if (points.isEmpty()) return ""
        return points.joinToString(ITEM_SEP) { pt ->
            val safeLabel = pt.label.replace(ITEM_SEP, " ").replace(FIELD_SEP, " ")
            "${pt.latitude}${FIELD_SEP}${pt.longitude}${FIELD_SEP}${pt.timestamp}${FIELD_SEP}${pt.speedKmh}${FIELD_SEP}${safeLabel}"
        }
    }

    fun parse(json: String): List<RouteWaypoint> {
        if (json.isBlank()) return emptyList()
        return json.split(ITEM_SEP).mapNotNull { raw ->
            val parts = raw.split(FIELD_SEP)
            if (parts.size >= 4) {
                val lat = parts[0].toDoubleOrNull()
                val lng = parts[1].toDoubleOrNull()
                val time = parts[2].toLongOrNull() ?: 0L
                val speed = parts[3].toDoubleOrNull() ?: 0.0
                val label = if (parts.size >= 5) parts[4] else ""
                if (lat != null && lng != null) {
                    RouteWaypoint(
                        latitude = lat,
                        longitude = lng,
                        timestamp = time,
                        speedKmh = speed,
                        label = label
                    )
                } else null
            } else null
        }
    }
}

enum class TransportMode(val displayName: String, val iconName: String) {
    MOTORBIKE("Xe máy", "motorcycle"),
    CAR("Ô tô", "car"),
    BUS("Xe buýt", "directions_bus"),
    WALK("Đi bộ", "directions_walk"),
    BICYCLE("Xe đạp", "pedal_bike"),
    TAXI("Taxi / Grab", "local_taxi"),
    TRAIN("Tàu hỏa", "train");

    companion object {
        fun fromString(value: String): TransportMode {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: MOTORBIKE
        }
    }
}
