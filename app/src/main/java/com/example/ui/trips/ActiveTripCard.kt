package com.example.ui.trips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trip
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRose
import com.example.util.DateUtils
import kotlinx.coroutines.delay

@Composable
fun ActiveTripCard(
    activeTrip: Trip,
    currentSpeedKmh: Double = 0.0,
    onRecordWaypoint: () -> Unit = {},
    onFinishTrip: () -> Unit,
    onCancelTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(activeTrip.id) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val elapsedMillis = (currentTime - activeTrip.startTime).coerceAtLeast(1000L)
    val waypoints = remember(activeTrip.routePointsJson) { activeTrip.getRouteWaypoints() }

    // Live Average Speed: use recorded avgSpeedKmh or compute from distance / elapsed time
    val liveAvgSpeed = remember(activeTrip.avgSpeedKmh, activeTrip.distanceKm, elapsedMillis) {
        val calcByTime = DateUtils.calculateAvgSpeedKmh(activeTrip.distanceKm, elapsedMillis)
        if (activeTrip.avgSpeedKmh > 0.0 && calcByTime > 0.0) {
            (activeTrip.avgSpeedKmh + calcByTime) / 2.0
        } else if (activeTrip.avgSpeedKmh > 0.0) {
            activeTrip.avgSpeedKmh
        } else {
            calcByTime
        }
    }

    val displayedCurrentSpeed = if (currentSpeedKmh > 0.0) {
        currentSpeedKmh
    } else {
        waypoints.lastOrNull()?.speedKmh ?: 0.0
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ĐANG THEO DÕI LỘ TRÌNH GPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }

                // Transport Mode Chip
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${activeTrip.transportMode} • ${activeTrip.purpose}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = activeTrip.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Xuất phát: ${activeTrip.startLocation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Route Map Visualization
            RouteMapCanvas(
                waypoints = waypoints,
                startLabel = activeTrip.startLocation,
                endLabel = waypoints.lastOrNull()?.label?.ifBlank { "Đang di chuyển" } ?: "Đang di chuyển",
                isLiveTracking = true,
                modifier = Modifier.height(135.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Live Speed & Distance Strip (Tốc độ TB, Vận tốc hiện tại, Quãng đường)
            SpeedTelemetryStrip(
                currentOrMaxSpeedKmh = displayedCurrentSpeed,
                avgSpeedKmh = liveAvgSpeed,
                distanceKm = activeTrip.distanceKm,
                isLive = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timer & Record Checkpoint Row
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = DateUtils.formatStopwatch(elapsedMillis),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedButton(
                        onClick = onRecordWaypoint,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("record_waypoint_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocationAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ghi mốc đường (${waypoints.size})", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelTrip,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cancel_active_trip_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccentRose
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hủy")
                }

                Button(
                    onClick = onFinishTrip,
                    modifier = Modifier
                        .weight(2f)
                        .testTag("finish_active_trip_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hoàn thành lộ trình")
                }
            }
        }
    }
}
