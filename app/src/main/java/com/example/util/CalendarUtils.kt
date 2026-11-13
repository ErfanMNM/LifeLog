package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarDay(
    val dayOfMonth: Int,
    val month: Int, // 0-based
    val year: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val timestamp: Long
)

object CalendarUtils {
    fun formatMonthYear(year: Int, month: Int): String {
        return "Tháng ${month + 1}, $year"
    }

    fun formatSelectedDayHeader(timestamp: Long): String {
        return try {
            val date = Date(timestamp)
            val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("vi", "VN"))
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
            val dayOfWeek = dayOfWeekFormat.format(date).replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale("vi", "VN")) else it.toString() 
            }
            "$dayOfWeek, ${dateFormat.format(date)}"
        } catch (_: Exception) {
            val date = Date(timestamp)
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
        }
    }

    fun getMonthDays(year: Int, month: Int): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH)
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

        // Day of week for the 1st day of month (Calendar.MONDAY is 2, SUNDAY is 1)
        var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert to Monday = 0 .. Sunday = 6
        var leadingDays = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        // Previous month filler days
        val prevCal = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -leadingDays)
        }
        for (i in 0 until leadingDays) {
            val d = prevCal.get(Calendar.DAY_OF_MONTH)
            val m = prevCal.get(Calendar.MONTH)
            val y = prevCal.get(Calendar.YEAR)
            days.add(
                CalendarDay(
                    dayOfMonth = d,
                    month = m,
                    year = y,
                    isCurrentMonth = false,
                    isToday = (y == todayYear && m == todayMonth && d == todayDay),
                    timestamp = prevCal.timeInMillis
                )
            )
            prevCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Current month days
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (d in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            days.add(
                CalendarDay(
                    dayOfMonth = d,
                    month = month,
                    year = year,
                    isCurrentMonth = true,
                    isToday = (year == todayYear && month == todayMonth && d == todayDay),
                    timestamp = cal.timeInMillis
                )
            )
        }

        // Next month filler days to complete rows (multiples of 7)
        val remaining = (7 - (days.size % 7)) % 7
        val nextCal = (cal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }
        for (i in 0 until remaining) {
            val d = nextCal.get(Calendar.DAY_OF_MONTH)
            val m = nextCal.get(Calendar.MONTH)
            val y = nextCal.get(Calendar.YEAR)
            days.add(
                CalendarDay(
                    dayOfMonth = d,
                    month = m,
                    year = y,
                    isCurrentMonth = false,
                    isToday = (y == todayYear && m == todayMonth && d == todayDay),
                    timestamp = nextCal.timeInMillis
                )
            )
            nextCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        return days
    }

    fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun isToday(timestamp: Long): Boolean {
        return isSameDay(timestamp, System.currentTimeMillis())
    }
}
