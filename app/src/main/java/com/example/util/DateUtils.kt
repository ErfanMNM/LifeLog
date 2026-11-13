package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val fullDateTimeFormat = SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale("vi", "VN"))
    private val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
    private val timestampWatermarkFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val currencyFormat = DecimalFormat("#,###")

    fun formatFullDateTime(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return fullDateTimeFormat.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatDayMonth(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return SimpleDateFormat("dd/MM", Locale("vi", "VN")).format(Date(timestamp))
    }

    fun formatMonthYear(year: Int, monthZeroIndexed: Int): String {
        return "Tháng ${monthZeroIndexed + 1}/$year"
    }

    fun formatTime(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return timeFormat.format(Date(timestamp))
    }

    fun formatWatermarkDateTime(timestamp: Long): String {
        return timestampWatermarkFormat.format(Date(timestamp))
    }

    fun formatDuration(durationMillis: Long): String {
        if (durationMillis <= 0) return "0 phút"
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes} phút ${seconds}s"
            else -> "${seconds} giây"
        }
    }

    fun formatStopwatch(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatVnd(amount: Long): String {
        return "${currencyFormat.format(amount)} ₫"
    }

    fun formatDistance(km: Double): String {
        val df = DecimalFormat("#,##0.0")
        return "${df.format(km)} km"
    }

    fun formatSpeed(kmh: Double): String {
        val df = DecimalFormat("#,##0.0")
        return "${df.format(kmh.coerceAtLeast(0.0))} km/h"
    }

    fun calculateAvgSpeedKmh(distanceKm: Double, durationMillis: Long): Double {
        if (distanceKm <= 0.0 || durationMillis <= 0L) return 0.0
        val hours = durationMillis.toDouble() / 3_600_000.0
        if (hours <= 0.0001) return 0.0
        return (distanceKm / hours).coerceIn(0.0, 300.0)
    }
}
