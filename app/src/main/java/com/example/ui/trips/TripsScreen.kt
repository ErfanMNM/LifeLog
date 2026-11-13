package com.example.ui.trips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trip
import com.example.ui.MonthMissionsStat
import com.example.ui.TagMonthlyStat
import com.example.ui.components.StatCard
import androidx.compose.material.icons.filled.Speed
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryBlue
import com.example.util.DateUtils
import com.example.util.LocationResult

@Composable
fun TripsScreen(
    trips: List<Trip>,
    activeTrip: Trip?,
    totalDistance: Double,
    totalCost: Long,
    completedCount: Int,
    currentLocation: LocationResult?,
    currentSpeedKmh: Double = 0.0,
    availableTags: List<String> = emptyList(),
    monthStat: MonthMissionsStat? = null,
    onNextMonth: () -> Unit = {},
    onPrevMonth: () -> Unit = {},
    onCurrentMonth: () -> Unit = {},
    onRecordWaypoint: () -> Unit = {},
    onQuickCheckIn: (tagName: String, location: String, cost: Long, notes: String, mode: String) -> Unit = { _, _, _, _, _ -> },
    onStartTrip: (title: String, startLocation: String, transportMode: String, purpose: String, tagName: String) -> Unit,
    onFinishActiveTrip: (Trip, String, Double, Long, String, String?) -> Unit,
    onCancelActiveTrip: (Trip) -> Unit,
    onSaveManualTrip: (String, String, String, String, Long, Long, Double, Long, String, String, String) -> Unit,
    onDeleteTrip: (Long) -> Unit
) {
    var selectedScreenTab by remember { mutableIntStateOf(0) } // 0: Lộ trình & Nhật ký, 1: Thống kê tháng
    var showQuickCheckInDialog by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var selectedFilterTag by remember { mutableStateOf("Tất cả") }
    var selectedTripForRouteDetail by remember { mutableStateOf<Trip?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Navigation Tabs: Lộ trình & Công tác vs Thống kê tháng
        TabRow(
            selectedTabIndex = selectedScreenTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedScreenTab == 0,
                onClick = { selectedScreenTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lộ trình & Công tác", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_missions_log")
            )
            Tab(
                selected = selectedScreenTab == 1,
                onClick = { selectedScreenTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thống kê theo tháng", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_missions_monthly_stats")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedScreenTab == 0) {
            // TAB 0: LỘ TRÌNH & NHẬT KÝ CÔNG TÁC
            MissionsLogView(
                trips = trips,
                activeTrip = activeTrip,
                currentSpeedKmh = currentSpeedKmh,
                totalDistance = totalDistance,
                totalCost = totalCost,
                completedCount = completedCount,
                availableTags = availableTags,
                selectedFilterTag = selectedFilterTag,
                onFilterTagChange = { selectedFilterTag = it },
                onOpenQuickCheckIn = { showQuickCheckInDialog = true },
                onOpenStartTrip = { showStartDialog = true },
                onOpenManualTrip = { showManualDialog = true },
                onRecordWaypoint = onRecordWaypoint,
                onFinishActiveTrip = { showFinishDialog = true },
                onCancelActiveTrip = { onCancelActiveTrip(it) },
                onViewRouteDetail = { selectedTripForRouteDetail = it },
                onDeleteTrip = onDeleteTrip
            )
        } else {
            // TAB 1: THỐNG KÊ THEO THÁNG (MSI, Samsung, ...)
            MonthlyMissionsStatsView(
                monthStat = monthStat,
                onNextMonth = onNextMonth,
                onPrevMonth = onPrevMonth,
                onCurrentMonth = onCurrentMonth,
                onOpenQuickCheckIn = { showQuickCheckInDialog = true }
            )
        }
    }

    if (selectedTripForRouteDetail != null) {
        TripRouteDetailDialog(
            trip = selectedTripForRouteDetail!!,
            onDismiss = { selectedTripForRouteDetail = null }
        )
    }

    if (showQuickCheckInDialog) {
        QuickMissionCheckInDialog(
            currentLocation = currentLocation,
            availableTags = availableTags,
            onDismiss = { showQuickCheckInDialog = false },
            onCheckIn = { tag, loc, cost, notes, mode ->
                onQuickCheckIn(tag, loc, cost, notes, mode)
                showQuickCheckInDialog = false
            }
        )
    }

    if (showStartDialog) {
        TripStartDialog(
            currentLocation = currentLocation,
            availableTags = availableTags,
            onDismiss = { showStartDialog = false },
            onStartTrip = { title, startLoc, mode, purpose, tag ->
                onStartTrip(title, startLoc, mode, purpose, tag)
                showStartDialog = false
            }
        )
    }

    if (showFinishDialog && activeTrip != null) {
        TripFinishDialog(
            trip = activeTrip,
            currentLocation = currentLocation,
            onDismiss = { showFinishDialog = false },
            onFinishTrip = { endLoc, dist, cost, notes ->
                onFinishActiveTrip(activeTrip, endLoc, dist, cost, notes, null)
                showFinishDialog = false
            }
        )
    }

    if (showManualDialog) {
        ManualTripDialog(
            currentLocation = currentLocation,
            availableTags = availableTags,
            onDismiss = { showManualDialog = false },
            onSaveTrip = { title, startLoc, endLoc, mode, startT, endT, dist, cost, purpose, notes, tag ->
                onSaveManualTrip(title, startLoc, endLoc, mode, startT, endT, dist, cost, purpose, notes, tag)
                showManualDialog = false
            }
        )
    }
}

@Composable
private fun MissionsLogView(
    trips: List<Trip>,
    activeTrip: Trip?,
    currentSpeedKmh: Double,
    totalDistance: Double,
    totalCost: Long,
    completedCount: Int,
    availableTags: List<String>,
    selectedFilterTag: String,
    onFilterTagChange: (String) -> Unit,
    onOpenQuickCheckIn: () -> Unit,
    onOpenStartTrip: () -> Unit,
    onOpenManualTrip: () -> Unit,
    onRecordWaypoint: () -> Unit,
    onFinishActiveTrip: () -> Unit,
    onCancelActiveTrip: (Trip) -> Unit,
    onViewRouteDetail: (Trip) -> Unit,
    onDeleteTrip: (Long) -> Unit
) {
    // Overall average speed across trips with valid speed
    val overallAvgSpeed = remember(trips) {
        val speeds = trips.filter { it.status == "COMPLETED" }
            .map { it.getEffectiveAvgSpeedKmh() }
            .filter { it > 0.0 }
        if (speeds.isNotEmpty()) speeds.average() else 0.0
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Overview Route & Speed Summary Row
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Tổng quãng đường",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = DateUtils.formatDistance(totalDistance),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Tốc độ TB các tuyến",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = DateUtils.formatSpeed(overallAvgSpeed),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentEmerald
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Active Trip Card if currently in progress
        if (activeTrip != null) {
            ActiveTripCard(
                activeTrip = activeTrip,
                currentSpeedKmh = currentSpeedKmh,
                onRecordWaypoint = onRecordWaypoint,
                onFinishTrip = onFinishActiveTrip,
                onCancelTrip = { onCancelActiveTrip(activeTrip) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            // Primary Actions: Track Route & Speed vs Quick Check-in
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenStartTrip,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("start_route_tracking_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Đo lộ trình & Tốc độ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenQuickCheckIn,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_checkin_button")
                ) {
                    Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check-in vị trí", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onOpenManualTrip,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nhập lộ trình thủ công (Tính vận tốc TB)", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Tag Filter Chips (Tất cả, MSI, Samsung, ...)
        val filterTags = listOf("Tất cả") + availableTags
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterTags) { tag ->
                val isSelected = selectedFilterTag == tag
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterTagChange(tag) },
                    label = { Text(tag) },
                    leadingIcon = if (tag != "Tất cả") {
                        {
                            Icon(
                                Icons.Default.LocalOffer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter trips by status and tag
        val completedTrips = trips.filter { it.status == "COMPLETED" }
        val displayedTrips = if (selectedFilterTag == "Tất cả") {
            completedTrips
        } else {
            completedTrips.filter { it.tagName.equals(selectedFilterTag, ignoreCase = true) }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Danh sách lộ trình & công tác (${displayedTrips.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (totalCost > 0) {
                Text(
                    text = "Chi phí: ${DateUtils.formatVnd(totalCost)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentAmber
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (displayedTrips.isEmpty() && activeTrip == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BusinessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (selectedFilterTag == "Tất cả") "Chưa có lượt công tác nào" else "Không có công tác cho tag '$selectedFilterTag'",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nhấn 'Check-in ngay' để lưu địa điểm đang đứng & gắn tag.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(displayedTrips, key = { it.id }) { trip ->
                    MissionHistoryCardItem(
                        trip = trip,
                        onViewRoute = { onViewRouteDetail(trip) },
                        onDelete = { onDeleteTrip(trip.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthlyMissionsStatsView(
    monthStat: MonthMissionsStat?,
    onNextMonth: () -> Unit,
    onPrevMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onOpenQuickCheckIn: () -> Unit
) {
    if (monthStat == null) return

    Column(modifier = Modifier.fillMaxSize()) {
        // Month Selector Bar (< Tháng 11/2026 >)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Tháng trước")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCurrentMonth() }
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = monthStat.monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Tháng sau")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Month Overview KPI Grid (4 Cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Số ngày đi",
                value = "${monthStat.totalDays} ngày",
                subtitle = "Tổng ngày công tác",
                icon = Icons.Default.CalendarMonth,
                iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Lượt check-in",
                value = "${monthStat.totalTrips} lượt",
                subtitle = "${monthStat.tagStats.size} đối tác / tag",
                icon = Icons.Default.BusinessCenter,
                iconBgColor = AccentEmerald.copy(alpha = 0.2f),
                iconColor = AccentEmerald,
                modifier = Modifier.weight(1f)
            )
        }

        if (monthStat.totalCost > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentAmber.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tổng chi phí công tác tháng:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = DateUtils.formatVnd(monthStat.totalCost),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section Title: Chi tiết theo từng Tag
        Text(
            text = "Thống kê chi tiết theo đối tác / Tag:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (monthStat.tagStats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Không có ghi nhận công tác trong ${monthStat.monthLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onOpenQuickCheckIn) {
                        Text("Check-in cho tháng này")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(monthStat.tagStats, key = { it.tagName }) { tagStat ->
                    TagMonthlyStatCard(tagStat = tagStat)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagMonthlyStatCard(tagStat: TagMonthlyStat) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tag_stat_card_${tagStat.tagName}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Tag Name badge + Days Count Pill
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
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BusinessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tagStat.tagName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${tagStat.tripCount} lượt check-in",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Days Count Highlight (e.g. "3 ngày công tác")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentEmerald.copy(alpha = 0.15f),
                    border = null
                ) {
                    Text(
                        text = "${tagStat.daysCount} ngày",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = AccentEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Specific Dates Visited Row: "Ngày đi: 01/11, 02/11, 05/11"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Các ngày: ",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    tagStat.formattedDates.forEach { dateStr ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Locations summary
            if (tagStat.sampleLocations.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = tagStat.sampleLocations.joinToString("; "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Cost if available
            if (tagStat.totalCost > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chi phí: ${DateUtils.formatVnd(tagStat.totalCost)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber
                    )
                }
            }

            // Toggle Expand to see check-in details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(if (isExpanded) "Ẩn chi tiết" else "Xem các lần đến", fontSize = 12.sp)
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expandable List of Trips for this Tag
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    tagStat.trips.forEach { trip ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = trip.startLocation,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (trip.notes.isNotBlank()) {
                                        Text(
                                            text = trip.notes,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                Text(
                                    text = DateUtils.formatFullDateTime(trip.startTime),
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
fun MissionHistoryCardItem(
    trip: Trip,
    onViewRoute: () -> Unit = {},
    onDelete: () -> Unit
) {
    val icon = when {
        trip.transportMode.contains("Xe máy", ignoreCase = true) -> Icons.Default.TwoWheeler
        trip.transportMode.contains("Ô tô", ignoreCase = true) || trip.transportMode.contains("Taxi", ignoreCase = true) -> Icons.Default.DirectionsCar
        trip.transportMode.contains("Xe buýt", ignoreCase = true) -> Icons.Default.DirectionsBus
        trip.transportMode.contains("Đi bộ", ignoreCase = true) -> Icons.Default.DirectionsWalk
        trip.transportMode.contains("Xe đạp", ignoreCase = true) -> Icons.Default.DirectionsBike
        else -> Icons.Default.DirectionsCar
    }

    val effectiveAvgSpeed = remember(trip) { trip.getEffectiveAvgSpeedKmh() }
    val durationMillis = remember(trip) { trip.getDurationMillis() }
    val waypointsCount = remember(trip.routePointsJson) { trip.getRouteWaypoints().size }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onViewRoute() }
            .testTag("trip_card_${trip.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Tag chip + Title + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = trip.transportMode,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (trip.tagName.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = trip.tagName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = trip.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${trip.transportMode} • ${trip.purpose} • ${DateUtils.formatFullDateTime(trip.startTime)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa công tác",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Route Start -> End Display
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = AccentEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val routeText = if (trip.endLocation.isNotBlank() && !trip.endLocation.equals(trip.startLocation, ignoreCase = true)) {
                    "${trip.startLocation}  ➔  ${trip.endLocation}"
                } else {
                    trip.startLocation
                }
                Text(
                    text = routeText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Telemetry & Average Speed Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Distance
                    Text(
                        text = "📏 ${DateUtils.formatDistance(trip.distanceKm)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Average Speed (Highlighted)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentEmerald.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "⚡ TB: ${DateUtils.formatSpeed(effectiveAvgSpeed)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Duration or Cost
                    if (durationMillis > 0L) {
                        Text(
                            text = "⏱️ ${DateUtils.formatDuration(durationMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else if (trip.costVnd > 0) {
                        Text(
                            text = "💰 ${DateUtils.formatVnd(trip.costVnd)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentAmber
                        )
                    }
                }
            }

            // Bottom row: Cost + View Route Detail CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (trip.notes.isNotBlank()) {
                    Text(
                        text = trip.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else if (trip.costVnd > 0) {
                    Text(
                        text = "Chi phí: ${DateUtils.formatVnd(trip.costVnd)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                TextButton(
                    onClick = onViewRoute,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (waypointsCount > 0) "Xem tuyến đường ($waypointsCount mốc)" else "Xem lộ trình & vận tốc",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
