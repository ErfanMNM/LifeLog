package com.example.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.TransportMode
import com.example.data.model.Trip
import com.example.util.LocationResult

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Surface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickMissionCheckInDialog(
    currentLocation: LocationResult?,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onCheckIn: (tagName: String, location: String, costVnd: Long, notes: String, transportMode: String) -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var locationText by remember { mutableStateOf(currentLocation?.address ?: "") }
    var costText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var transportMode by remember { mutableStateOf(TransportMode.MOTORBIKE.displayName) }

    val tagSuggestions = remember(availableTags) {
        val defaultList = listOf("MSI", "Samsung", "Viettel", "FPT", "Chi nhánh HCM", "Nhà máy", "Khách hàng A")
        (availableTags + defaultList).distinct().filter { it.isNotBlank() }
    }

    val transportModes = TransportMode.entries.map { it.displayName }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BusinessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Check-in Công tác",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Lưu địa điểm đang đứng & gắn Tag đối tác / công ty để tiện theo dõi thống kê theo tháng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Current Location Field
                OutlinedTextField(
                    value = locationText,
                    onValueChange = { locationText = it },
                    label = { Text("Địa điểm đang đứng (GPS)") },
                    placeholder = { Text("Địa chỉ nơi bạn đang công tác...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkin_location_input")
                )

                // Tag Name Field
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tên Tag đối tác / công ty (Bắt buộc)") },
                    placeholder = { Text("VD: MSI, Samsung, FPT...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkin_tag_input")
                )

                // Tag Suggestions
                Text(
                    text = "Gợi ý Tag nhanh:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tagSuggestions.forEach { tag ->
                        val isSelected = tagName.equals(tag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { tagName = tag },
                            label = { Text(tag) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                // Cost input
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it },
                    label = { Text("Chi phí công tác (VNĐ - tùy chọn)") },
                    placeholder = { Text("VD: 50000 (xăng, ăn trưa...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú nội dung công tác") },
                    placeholder = { Text("Gặp anh Nam, bàn giao máy, ký biên bản...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Transport Mode
                Text(
                    text = "Phương tiện di chuyển:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    transportModes.forEach { mode ->
                        FilterChip(
                            selected = transportMode == mode,
                            onClick = { transportMode = mode },
                            label = { Text(mode) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costText.toLongOrNull() ?: 0L
                    val finalTag = tagName.trim().ifBlank { "Công tác" }
                    onCheckIn(
                        finalTag,
                        locationText.trim().ifBlank { "Vị trí hiện tại" },
                        cost,
                        notes.trim(),
                        transportMode
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_checkin_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Lưu Check-in ngay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripStartDialog(
    currentLocation: LocationResult?,
    availableTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onStartTrip: (title: String, startLocation: String, transportMode: String, purpose: String, tagName: String) -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var startLocation by remember { mutableStateOf(currentLocation?.address ?: "") }
    var transportMode by remember { mutableStateOf(TransportMode.MOTORBIKE.displayName) }
    var purpose by remember { mutableStateOf("Công việc") }

    val transportModes = TransportMode.entries.map { it.displayName }
    val purposes = listOf("Công tác", "Công việc", "Cá nhân", "Gặp khách hàng", "Giao hàng")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🚀 Bắt đầu chuyến đi mới",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = {
                        tagName = it
                        if (title.isBlank() || title.startsWith("Công tác")) {
                            title = if (it.isNotBlank()) "Công tác $it" else ""
                        }
                    },
                    label = { Text("Tên Tag đối tác / công ty (VD: MSI)") },
                    placeholder = { Text("MSI, Samsung, FPT...") },
                    leadingIcon = {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_trip_tag_input")
                )

                if (availableTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableTags.take(6).forEach { tag ->
                            FilterChip(
                                selected = tagName.equals(tag, ignoreCase = true),
                                onClick = {
                                    tagName = tag
                                    if (title.isBlank() || title.startsWith("Công tác")) {
                                        title = "Công tác $tag"
                                    }
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề chuyến đi / Mục tiêu") },
                    placeholder = { Text("Ví dụ: Đi gặp đối tác MSI...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_trip_title_input")
                )

                OutlinedTextField(
                    value = startLocation,
                    onValueChange = { startLocation = it },
                    label = { Text("Điểm xuất phát") },
                    placeholder = { Text("Vị trí bắt đầu...") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Phương tiện di chuyển",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    transportModes.forEach { mode ->
                        FilterChip(
                            selected = transportMode == mode,
                            onClick = { transportMode = mode },
                            label = { Text(mode) }
                        )
                    }
                }

                Text(
                    text = "Mục đích chuyến đi",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    purposes.forEach { p ->
                        FilterChip(
                            selected = purpose == p,
                            onClick = { purpose = p },
                            label = { Text(p) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank {
                        if (tagName.isNotBlank()) "Công tác $tagName" else "Chuyến đi $transportMode"
                    }
                    onStartTrip(
                        finalTitle,
                        startLocation.ifBlank { "Điểm xuất phát" },
                        transportMode,
                        purpose,
                        tagName.trim()
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_start_trip_btn")
            ) {
                Text("Bắt đầu di chuyển")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun TripFinishDialog(
    trip: Trip,
    currentLocation: LocationResult?,
    onDismiss: () -> Unit,
    onFinishTrip: (endLocation: String, distanceKm: Double, costVnd: Long, notes: String) -> Unit
) {
    var endLocation by remember { mutableStateOf(currentLocation?.address ?: "") }
    var distanceKmText by remember {
        mutableStateOf(
            if (trip.distanceKm > 0.0) String.format(java.util.Locale.US, "%.2f", trip.distanceKm) else ""
        )
    }
    var costVndText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val elapsedMillis = (System.currentTimeMillis() - trip.startTime).coerceAtLeast(60_000L)
    val parsedDist = distanceKmText.toDoubleOrNull() ?: trip.distanceKm
    val calculatedAvgSpeed = remember(parsedDist, elapsedMillis, trip.avgSpeedKmh) {
        val byTime = com.example.util.DateUtils.calculateAvgSpeedKmh(parsedDist, elapsedMillis)
        if (byTime > 0.0) byTime else trip.avgSpeedKmh
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🏁 Hoàn thành lộ trình",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                // Live calculated Average Speed & Duration Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Thời gian di chuyển",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = com.example.util.DateUtils.formatDuration(elapsedMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Tốc độ trung bình",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = com.example.util.DateUtils.formatSpeed(calculatedAvgSpeed),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = endLocation,
                    onValueChange = { endLocation = it },
                    label = { Text("Điểm đến (Kết thúc)") },
                    placeholder = { Text("Nhập địa chỉ hoặc vị trí...") },
                    leadingIcon = {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = distanceKmText,
                        onValueChange = { distanceKmText = it },
                        label = { Text("Quãng đường (km)") },
                        placeholder = { Text("vd: 8.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trip_finish_distance_input")
                    )

                    OutlinedTextField(
                        value = costVndText,
                        onValueChange = { costVndText = it },
                        label = { Text("Chi phí (VNĐ)") },
                        placeholder = { Text("vd: 30000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("trip_finish_cost_input")
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú lộ trình") },
                    placeholder = { Text("Tuyến đường đi, tình trạng giao thông, tiền xăng...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dist = distanceKmText.toDoubleOrNull() ?: trip.distanceKm
                    val cost = costVndText.toLongOrNull() ?: 0L
                    onFinishTrip(
                        endLocation.ifBlank { "Điểm đến" },
                        dist,
                        cost,
                        notes
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_finish_trip_btn")
            ) {
                Text("Lưu & Kết thúc")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualTripDialog(
    currentLocation: LocationResult?,
    availableTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSaveTrip: (
        title: String,
        startLocation: String,
        endLocation: String,
        transportMode: String,
        startTime: Long,
        endTime: Long,
        distanceKm: Double,
        costVnd: Long,
        purpose: String,
        notes: String,
        tagName: String
    ) -> Unit
) {
    var tagName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var startLocation by remember { mutableStateOf(currentLocation?.address ?: "") }
    var endLocation by remember { mutableStateOf("") }
    var transportMode by remember { mutableStateOf(TransportMode.MOTORBIKE.displayName) }
    var distanceKmText by remember { mutableStateOf("") }
    var durationMinutesText by remember { mutableStateOf("25") }
    var costVndText by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("Công tác") }
    var notes by remember { mutableStateOf("") }

    val transportModes = TransportMode.entries.map { it.displayName }
    val purposes = listOf("Công tác", "Công việc", "Cá nhân", "Gặp khách hàng", "Giao hàng")

    val distVal = distanceKmText.toDoubleOrNull() ?: 0.0
    val durationMinVal = (durationMinutesText.toDoubleOrNull() ?: 25.0).coerceAtLeast(1.0)
    val durationMillis = (durationMinVal * 60_000L).toLong()
    val previewAvgSpeed = com.example.util.DateUtils.calculateAvgSpeedKmh(distVal, durationMillis)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ghi nhận lộ trình & công tác",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = {
                        tagName = it
                        if (title.isBlank() || title.startsWith("Công tác")) {
                            title = if (it.isNotBlank()) "Công tác $it" else ""
                        }
                    },
                    label = { Text("Tên Tag đối tác / công ty (VD: MSI)") },
                    placeholder = { Text("MSI, Samsung, FPT...") },
                    leadingIcon = {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (availableTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        availableTags.take(6).forEach { tag ->
                            FilterChip(
                                selected = tagName.equals(tag, ignoreCase = true),
                                onClick = {
                                    tagName = tag
                                    if (title.isBlank() || title.startsWith("Công tác")) {
                                        title = "Công tác $tag"
                                    }
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề lộ trình / công tác") },
                    placeholder = { Text("Ví dụ: Đi gặp đối tác MSI...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = startLocation,
                    onValueChange = { startLocation = it },
                    label = { Text("Điểm xuất phát (Từ đâu)") },
                    placeholder = { Text("Nơi bắt đầu...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endLocation,
                    onValueChange = { endLocation = it },
                    label = { Text("Điểm đến (Đến đâu)") },
                    placeholder = { Text("Nơi kết thúc...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Phương tiện", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    transportModes.forEach { mode ->
                        FilterChip(
                            selected = transportMode == mode,
                            onClick = { transportMode = mode },
                            label = { Text(mode) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = distanceKmText,
                        onValueChange = { distanceKmText = it },
                        label = { Text("Quãng đường (km)") },
                        placeholder = { Text("vd: 12.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = durationMinutesText,
                        onValueChange = { durationMinutesText = it },
                        label = { Text("Thời gian (phút)") },
                        placeholder = { Text("vd: 25") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live Calculated Average Speed Preview
                if (distVal > 0.0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ Tốc độ trung bình tự tính:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = com.example.util.DateUtils.formatSpeed(previewAvgSpeed),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = costVndText,
                    onValueChange = { costVndText = it },
                    label = { Text("Chi phí (VNĐ)") },
                    placeholder = { Text("vd: 50000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Mục đích", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    purposes.forEach { p ->
                        FilterChip(
                            selected = purpose == p,
                            onClick = { purpose = p },
                            label = { Text(p) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú tuyến đường") },
                    placeholder = { Text("Ghi chú cung đường đi, nội dung làm việc...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val dist = distanceKmText.toDoubleOrNull() ?: 0.0
                    val cost = costVndText.toLongOrNull() ?: 0L
                    val finalTitle = title.ifBlank {
                        if (tagName.isNotBlank()) "Công tác $tagName" else "Chuyến đi $transportMode"
                    }
                    onSaveTrip(
                        finalTitle,
                        startLocation.ifBlank { "Điểm xuất phát" },
                        endLocation.ifBlank { startLocation.ifBlank { "Địa điểm" } },
                        transportMode,
                        now - durationMillis,
                        now,
                        dist,
                        cost,
                        purpose,
                        notes,
                        tagName.trim()
                    )
                    onDismiss()
                }
            ) {
                Text("Lưu lộ trình")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
