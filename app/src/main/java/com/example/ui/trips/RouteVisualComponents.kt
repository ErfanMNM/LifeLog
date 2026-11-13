package com.example.ui.trips

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.RouteWaypoint
import com.example.data.model.Trip
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.util.DateUtils
import java.util.Locale

@Composable
fun RouteMapCanvas(
    waypoints: List<RouteWaypoint>,
    startLabel: String,
    endLabel: String,
    isLiveTracking: Boolean = false,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceVariantColor.copy(alpha = 0.55f),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padX = 36.dp.toPx()
                val padY = 30.dp.toPx()

                // 1. Subtle coordinate map grid
                val cols = 6
                val rows = 4
                for (i in 1 until cols) {
                    val x = (w / cols) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                }
                for (j in 1 until rows) {
                    val y = (h / rows) * j
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // 2. Map waypoints to canvas coordinates
                val plotW = (w - padX * 2).coerceAtLeast(10f)
                val plotH = (h - padY * 2).coerceAtLeast(10f)

                val mappedPoints: List<Offset> = if (waypoints.size >= 2) {
                    val minLat = waypoints.minOf { it.latitude }
                    val maxLat = waypoints.maxOf { it.latitude }
                    val minLng = waypoints.minOf { it.longitude }
                    val maxLng = waypoints.maxOf { it.longitude }

                    val latSpan = (maxLat - minLat).coerceAtLeast(0.0005)
                    val lngSpan = (maxLng - minLng).coerceAtLeast(0.0005)

                    // If all points are identical, spread them along a natural curve
                    val allSame = (maxLat - minLat) < 0.00001 && (maxLng - minLng) < 0.00001
                    if (allSame) {
                        waypoints.mapIndexed { idx, _ ->
                            val frac = idx.toFloat() / (waypoints.size - 1).coerceAtLeast(1)
                            val x = padX + frac * plotW
                            val curveOffset = kotlin.math.sin(frac * Math.PI).toFloat() * (plotH * 0.28f)
                            val y = h - padY - (frac * plotH * 0.6f) - curveOffset
                            Offset(x, y)
                        }
                    } else {
                        waypoints.map { pt ->
                            val normX = ((pt.longitude - minLng) / lngSpan).toFloat().coerceIn(0f, 1f)
                            val normY = ((pt.latitude - minLat) / latSpan).toFloat().coerceIn(0f, 1f)
                            Offset(
                                x = padX + normX * plotW,
                                y = (h - padY) - normY * plotH
                            )
                        }
                    }
                } else {
                    // Default visual route curve from Start to End when fewer than 2 GPS fixes
                    listOf(
                        Offset(padX, h - padY),
                        Offset(padX + plotW * 0.35f, padY + plotH * 0.35f),
                        Offset(padX + plotW * 0.68f, padY + plotH * 0.55f),
                        Offset(w - padX, padY)
                    )
                }

                // 3. Draw Route Path Shadow + Active Polyline
                if (mappedPoints.size >= 2) {
                    val routePath = Path().apply {
                        moveTo(mappedPoints.first().x, mappedPoints.first().y)
                        for (i in 1 until mappedPoints.size) {
                            lineTo(mappedPoints[i].x, mappedPoints[i].y)
                        }
                    }

                    // Outer glow
                    drawPath(
                        path = routePath,
                        color = primaryColor.copy(alpha = 0.22f),
                        style = Stroke(
                            width = 12.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Main route line
                    drawPath(
                        path = routePath,
                        brush = Brush.linearGradient(
                            colors = listOf(AccentEmerald, primaryColor, AccentAmber)
                        ),
                        style = Stroke(
                            width = 4.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Dashed center line
                    drawPath(
                        path = routePath,
                        color = Color.White.copy(alpha = 0.65f),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )

                    // Intermediate points
                    for (i in 1 until mappedPoints.size - 1) {
                        val pt = mappedPoints[i]
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 3.5.dp.toPx(),
                            center = pt
                        )
                    }

                    // Start Point (Green)
                    val startPt = mappedPoints.first()
                    drawCircle(
                        color = AccentEmerald.copy(alpha = 0.28f),
                        radius = 12.dp.toPx(),
                        center = startPt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = startPt
                    )
                    drawCircle(
                        color = AccentEmerald,
                        radius = 5.dp.toPx(),
                        center = startPt
                    )

                    // End / Current Point
                    val endPt = mappedPoints.last()
                    val endColor = if (isLiveTracking) primaryColor else AccentRose
                    drawCircle(
                        color = endColor.copy(alpha = 0.3f),
                        radius = 14.dp.toPx(),
                        center = endPt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = endPt
                    )
                    drawCircle(
                        color = endColor,
                        radius = 5.5.dp.toPx(),
                        center = endPt
                    )
                }
            }

            // Top-left Route Badge
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (waypoints.isNotEmpty()) "${waypoints.size} mốc tọa độ GPS" else "Tuyến đường di chuyển",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            // Top-right Compass Badge
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "GPS Track",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }

            // Bottom Start -> End Overlay Label
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = startLabel.ifBlank { "Điểm đi" },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = " ➔ ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isLiveTracking) MaterialTheme.colorScheme.primary else AccentRose)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = endLabel.ifBlank { if (isLiveTracking) "Vị trí hiện tại" else "Điểm đến" },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedTelemetryStrip(
    currentOrMaxSpeedKmh: Double,
    avgSpeedKmh: Double,
    distanceKm: Double,
    isLive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Average Speed (Highlighted as requested)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Tốc độ TB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = DateUtils.formatSpeed(avgSpeedKmh),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // 2. Current / Max Speed
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isLive) "Vận tốc hiện tại" else "Vận tốc tối đa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = DateUtils.formatSpeed(currentOrMaxSpeedKmh),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentEmerald
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // 3. Distance
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Quãng đường",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = DateUtils.formatDistance(distanceKm),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentAmber
                )
            }
        }
    }
}

@Composable
fun TripRouteDetailDialog(
    trip: Trip,
    onDismiss: () -> Unit
) {
    val waypoints = trip.getRouteWaypoints()
    val effectiveAvgSpeed = trip.getEffectiveAvgSpeedKmh()
    val effectiveMaxSpeed = maxOf(
        trip.maxSpeedKmh,
        waypoints.maxOfOrNull { it.speedKmh } ?: 0.0,
        effectiveAvgSpeed
    )
    val durationMillis = trip.getDurationMillis()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Chi tiết Lộ trình & Vận tốc",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = trip.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Visual Route Map Canvas
                RouteMapCanvas(
                    waypoints = waypoints,
                    startLabel = trip.startLocation,
                    endLabel = trip.endLocation.ifBlank { trip.startLocation },
                    isLiveTracking = false,
                    modifier = Modifier.height(185.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Speed & Distance Telemetry Strip
                SpeedTelemetryStrip(
                    currentOrMaxSpeedKmh = effectiveMaxSpeed,
                    avgSpeedKmh = effectiveAvgSpeed,
                    distanceKm = trip.distanceKm,
                    isLive = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Route Summary Info Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Điểm xuất phát (${DateUtils.formatTime(trip.startTime)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = trip.startLocation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (trip.endLocation.isNotBlank()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = AccentRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Điểm đến (${if (trip.endTime > 0) DateUtils.formatTime(trip.endTime) else "Hoàn thành"})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = trip.endLocation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (durationMillis > 0L) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tổng thời gian di chuyển:",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = DateUtils.formatDuration(durationMillis),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Waypoints Timeline
                if (waypoints.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Nhật ký tọa độ & tốc độ trên tuyến (${waypoints.size} điểm):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            waypoints.forEachIndexed { idx, pt ->
                                val isFirst = idx == 0
                                val isLast = idx == waypoints.lastIndex
                                val dotColor = when {
                                    isFirst -> AccentEmerald
                                    isLast -> AccentRose
                                    else -> MaterialTheme.colorScheme.primary
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = pt.label.ifBlank {
                                                    when {
                                                        isFirst -> "Bắt đầu hành trình"
                                                        isLast -> "Điểm kết thúc"
                                                        else -> "Mốc lộ trình #${idx + 1}"
                                                    }
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${DateUtils.formatTime(pt.timestamp)} • GPS: ${String.format(Locale.US, "%.4f, %.4f", pt.latitude, pt.longitude)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    if (pt.speedKmh > 0.0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        ) {
                                            Text(
                                                text = DateUtils.formatSpeed(pt.speedKmh),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_route_detail_btn")
                ) {
                    Text("Đóng")
                }
            }
        }
    }
}
