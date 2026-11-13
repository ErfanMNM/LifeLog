package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Category
import com.example.data.model.Note
import com.example.data.model.Reminder
import com.example.data.model.TimestampPhoto
import com.example.data.model.Trip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.data.model.RouteWaypoint
import com.example.data.model.RouteWaypointSerializer
import com.example.data.model.Task

@Database(
    entities = [Note::class, Trip::class, TimestampPhoto::class, Category::class, Reminder::class, Task::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tripDao(): TripDao
    abstract fun timestampPhotoDao(): TimestampPhotoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifelog_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val now = System.currentTimeMillis()
            val oneDayMillis = 86_400_000L

            // Initial Default Categories
            database.categoryDao().insert(Category(name = "Cá nhân", colorHex = "#F59E0B", isDefault = true))
            database.categoryDao().insert(Category(name = "Công việc", colorHex = "#0284C7", isDefault = true))
            database.categoryDao().insert(Category(name = "Tài chính", colorHex = "#10B981", isDefault = true))
            database.categoryDao().insert(Category(name = "Học tập", colorHex = "#8B5CF6", isDefault = true))
            database.categoryDao().insert(Category(name = "Ý tưởng", colorHex = "#EC4899", isDefault = true))
            database.categoryDao().insert(Category(name = "Quan trọng", colorHex = "#EF4444", isDefault = true))

            // Initial Reminders
            database.reminderDao().insert(
                Reminder(
                    title = "Nghỉ ngơi & uống nước 💧",
                    description = "Đứng dậy vươn vai sau 1 tiếng làm việc trên máy tính.",
                    triggerTime = now + 3600000,
                    enableSound = true,
                    enableVibration = true
                )
            )
            database.reminderDao().insert(
                Reminder(
                    title = "Kiểm tra danh sách công việc ngày mai 📋",
                    description = "Ghi chú lại các đầu việc quan trọng và lịch hẹn.",
                    triggerTime = now + 7200000,
                    enableSound = true,
                    enableVibration = false
                )
            )

            // Initial Notes
            database.noteDao().insert(
                Note(
                    title = "Chào mừng bạn đến với LifeLog! 📝",
                    content = "LifeLog giúp bạn ghi chú công việc, theo dõi lộ trình di chuyển hàng ngày và chụp ảnh đóng dấu timestamp thực tế (ngày giờ + GPS) chống gian lận.",
                    category = "Cá nhân",
                    categoriesJson = "Cá nhân;;;Ý tưởng",
                    colorHex = "#FEF3C7",
                    isPinned = true,
                    checklistItemsJson = "Khám phá giao diện Tổng quan:::true|||Thử tạo ghi chú đầu tiên:::true|||Bắt đầu một chuyến đi di chuyển:::false|||Chụp một bức ảnh có đóng dấu timestamp:::false",
                    createdTimestamp = now,
                    updatedTimestamp = now
                )
            )

            database.noteDao().insert(
                Note(
                    title = "Kế hoạch tuần: Gặp đối tác & bảo dưỡng xe",
                    content = "1. Họp dự án lúc 9h sáng thứ 3 tại Quận 1.\n2. Thay dầu xe máy định kỳ (mốc 5000km).\n3. Chụp ảnh biên nhận và đồng hồ xe để lưu trữ.",
                    category = "Công việc",
                    categoriesJson = "Công việc;;;Quan trọng",
                    colorHex = "#E0E7FF",
                    isPinned = false,
                    checklistItemsJson = "Chuẩn bị tài liệu báo cáo:::true|||Kiểm tra áp suất lốp xe:::false|||Chụp ảnh lưu km công tơ mét:::false",
                    createdTimestamp = now - 3600000,
                    updatedTimestamp = now - 3600000
                )
            )

            // Initial Trips with Route Waypoints & Speed
            val sampleWaypoints = listOf(
                RouteWaypoint(10.7938, 106.7181, now - 7200000, 0.0, "Xuất phát: Nguyễn Hữu Cảnh"),
                RouteWaypoint(10.7865, 106.7098, now - 6750000, 32.5, "Cầu Thủ Thiêm / Tôn Đức Thắng"),
                RouteWaypoint(10.7789, 106.7052, now - 6100000, 26.8, "Nguyễn Huệ, Quận 1"),
                RouteWaypoint(10.7716, 106.7044, now - 5400000, 21.0, "Đích: Tòa nhà Bitexco, Quận 1")
            )
            database.tripDao().insert(
                Trip(
                    title = "Đi công tác sáng (Nhà -> Bitexco)",
                    startLocation = "Nguyễn Hữu Cảnh, Bình Thạnh",
                    endLocation = "Tòa nhà Bitexco, Quận 1",
                    transportMode = "Xe máy",
                    startTime = now - 7200000,
                    endTime = now - 5400000,
                    distanceKm = 6.8,
                    costVnd = 15000,
                    purpose = "Công tác",
                    notes = "Giao thông thuận lợi, tuyến Nguyễn Hữu Cảnh - Tôn Đức Thắng",
                    status = "COMPLETED",
                    startLatitude = 10.7938,
                    startLongitude = 106.7181,
                    endLatitude = 10.7716,
                    endLongitude = 106.7044,
                    createdTimestamp = now - 7200000,
                    tagName = "MSI",
                    avgSpeedKmh = 27.2,
                    maxSpeedKmh = 42.5,
                    routePointsJson = RouteWaypointSerializer.serialize(sampleWaypoints)
                )
            )

            // Initial Work Tasks
            database.taskDao().insert(
                Task(
                    title = "Hoàn thành báo cáo tiến độ dự án tuần",
                    description = "Tổng hợp KPI, phân tích số liệu công việc và chuẩn bị slide thuyết trình cho ban giám đốc.",
                    status = "IN_PROGRESS",
                    priority = "URGENT",
                    dueDate = now + 14400000, // 4 hours later
                    project = "Báo cáo",
                    subtasksJson = "Thu thập số liệu doanh số:::true|||Soạn slide PowerPoint:::true|||Gửi email kiểm duyệt nội bộ:::false",
                    estimatedHours = 3.5,
                    createdTimestamp = now - 7200000
                )
            )

            database.taskDao().insert(
                Task(
                    title = "Họp triển khai tính năng mới với team kỹ thuật",
                    description = "Thảo luận kiến trúc hệ thống, phân công task cho sprint tiếp theo và rà soát bug.",
                    status = "TODO",
                    priority = "HIGH",
                    dueDate = now + 86400000, // Tomorrow
                    project = "Họp hành",
                    subtasksJson = "Chuẩn bị tài liệu mô tả yêu cầu (BRD):::true|||Đặt phòng họp trực tuyến:::false|||Ghi chép biên bản cuộc họp:::false",
                    estimatedHours = 1.5,
                    createdTimestamp = now - 3600000
                )
            )

            database.taskDao().insert(
                Task(
                    title = "Nghiên cứu & tối ưu trải nghiệm người dùng (UX)",
                    description = "Cải thiện giao diện nhập liệu, thêm insets bàn phím và bảng điều khiển trực quan.",
                    status = "COMPLETED",
                    priority = "MEDIUM",
                    dueDate = now - 3600000,
                    project = "Phát triển",
                    subtasksJson = "Thiết kế mockup giao diện:::true|||Thử nghiệm trên máy thực tế:::true|||Lấy phản hồi từ người dùng:::true",
                    estimatedHours = 4.0,
                    createdTimestamp = now - 86400000,
                    completedTimestamp = now - 3600000
                )
            )
        }
    }
}
