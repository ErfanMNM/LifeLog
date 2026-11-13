package com.example.ui.trips

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
    startLat: Double? = null,
    startLng: Double? = null,
    endLat: Double? = null,
    endLng: Double? = null,
    modifier: Modifier = Modifier
) {
    // Default to Real Interactive Map (OpenStreetMap + OSRM Road Routing)
    var useRealStreetMap by remember { mutableStateOf(true) }

    // Resolve effective coordinates for plotting on the real map
    val effectivePoints = remember(waypoints, startLat, startLng, endLat, endLng, startLabel, endLabel) {
        if (waypoints.size >= 2) {
            waypoints
        } else if (waypoints.size == 1) {
            val first = waypoints.first()
            val destLat = endLat ?: (first.latitude + 0.008)
            val destLng = endLng ?: (first.longitude + 0.007)
            if (isLiveTracking) {
                listOf(first)
            } else {
                listOf(
                    first,
                    RouteWaypoint(
                        latitude = destLat,
                        longitude = destLng,
                        timestamp = first.timestamp + 600_000L,
                        speedKmh = first.speedKmh,
                        label = endLabel.ifBlank { "Điểm đến" }
                    )
                )
            }
        } else {
            val sLat = startLat ?: 10.7769
            val sLng = startLng ?: 106.7009
            val eLat = endLat ?: 10.7938
            val eLng = endLng ?: 106.7181
            listOf(
                RouteWaypoint(sLat, sLng, System.currentTimeMillis(), 0.0, startLabel.ifBlank { "Điểm xuất phát" }),
                RouteWaypoint(eLat, eLng, System.currentTimeMillis(), 0.0, endLabel.ifBlank { "Điểm đến" })
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (useRealStreetMap) {
                LeafletStreetMapView(
                    waypoints = effectivePoints,
                    startLabel = startLabel,
                    endLabel = endLabel,
                    isLiveTracking = isLiveTracking,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                VectorRouteCanvas(
                    waypoints = effectivePoints,
                    isLiveTracking = isLiveTracking,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top-right Map Mode Toggle Pill (Bản đồ thực tế vs Sơ đồ GPS)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { useRealStreetMap = !useRealStreetMap }
                    .testTag("toggle_map_mode_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (useRealStreetMap) Icons.Default.Map else Icons.Default.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (useRealStreetMap) "Bản đồ đường phố (Đổi)" else "Sơ đồ GPS (Đổi sang Map)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            // Bottom Start -> End Overlay Label
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
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
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = startLabel.ifBlank { "Điểm đi" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }

                Text(
                    text = " ➔ ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isLiveTracking) MaterialTheme.colorScheme.primary else AccentRose)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = endLabel.ifBlank { if (isLiveTracking) "Vị trí hiện tại" else "Điểm đến" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
private fun LeafletStreetMapView(
    waypoints: List<RouteWaypoint>,
    startLabel: String,
    endLabel: String,
    isLiveTracking: Boolean,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(waypoints, startLabel, endLabel, isLiveTracking) {
        buildLeafletRouteMapHtml(
            waypoints = waypoints,
            startLabel = startLabel,
            endLabel = endLabel,
            isLiveTracking = isLiveTracking
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                webViewClient = WebViewClient()
                // Allow smooth panning and zooming inside scrollable Compose containers
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                    false
                }
            }
        },
        update = { webView ->
            val currentTag = webView.tag as? String
            if (currentTag != htmlContent) {
                webView.tag = htmlContent
                webView.loadDataWithBaseURL(
                    "https://openstreetmap.org",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

private fun buildLeafletRouteMapHtml(
    waypoints: List<RouteWaypoint>,
    startLabel: String,
    endLabel: String,
    isLiveTracking: Boolean
): String {
    val pointsJsArray = waypoints.mapIndexed { idx, pt ->
        val safeLabel = (pt.label.ifBlank {
            when (idx) {
                0 -> startLabel.ifBlank { "Điểm xuất phát" }
                waypoints.lastIndex -> endLabel.ifBlank { if (isLiveTracking) "Vị trí hiện tại" else "Điểm đến" }
                else -> "Mốc #${idx + 1}"
            }
        }).replace("\"", "\\\"").replace("'", "\\'").replace("\n", " ")

        val speedStr = if (pt.speedKmh > 0.0) String.format(Locale.US, "%.1f km/h", pt.speedKmh) else ""
        val timeStr = DateUtils.formatTime(pt.timestamp)
        "{lat: ${pt.latitude}, lng: ${pt.longitude}, label: '$safeLabel', speed: '$speedStr', time: '$timeStr'}"
    }.joinToString(",\n")

    val endColor = if (isLiveTracking) "#2563EB" else "#E11D48"

    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        html, body, #map {
            height: 100%;
            width: 100%;
            margin: 0;
            padding: 0;
            background: #EAE6DF;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        }
        .leaflet-control-attribution {
            display: none !important;
        }
        .pin-badge {
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            color: #fff;
            font-weight: 800;
            font-size: 11px;
            border: 2.5px solid #fff;
            box-shadow: 0 2px 6px rgba(0,0,0,0.35);
        }
        .speed-tag {
            background: rgba(29, 78, 216, 0.92);
            color: #fff;
            padding: 2px 5px;
            border-radius: 6px;
            font-size: 10px;
            font-weight: 700;
            white-space: nowrap;
            border: 1px solid #fff;
            box-shadow: 0 1px 4px rgba(0,0,0,0.25);
        }
    </style>
</head>
<body>
<div id="map"></div>
<script>
    var pts = [$pointsJsArray];
    var defaultCenter = pts.length > 0 ? [pts[0].lat, pts[0].lng] : [10.7769, 106.7009];
    var map = L.map('map', {
        zoomControl: true,
        attributionControl: false
    }).setView(defaultCenter, 14);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19
    }).addTo(map);

    var latlngs = pts.map(function(p) { return [p.lat, p.lng]; });

    // Draw initial polyline connecting GPS points immediately
    var glowLine = null;
    var mainLine = null;
    if (latlngs.length >= 2) {
        glowLine = L.polyline(latlngs, {
            color: '#1D4ED8',
            weight: 10,
            opacity: 0.25,
            lineCap: 'round',
            lineJoin: 'round'
        }).addTo(map);

        mainLine = L.polyline(latlngs, {
            color: '#1D4ED8',
            weight: 5,
            opacity: 0.92,
            lineCap: 'round',
            lineJoin: 'round'
        }).addTo(map);

        map.fitBounds(mainLine.getBounds(), { padding: [28, 28], maxZoom: 16 });
    } else if (latlngs.length === 1) {
        map.setView(latlngs[0], 15);
    }

    // Add markers for Start, Intermediate Checkpoints, and End
    pts.forEach(function(p, idx) {
        var isStart = (idx === 0);
        var isEnd = (idx === pts.length - 1 && pts.length > 1);
        var bg = isStart ? '#059669' : (isEnd ? '$endColor' : '#1D4ED8');
        var size = (isStart || isEnd) ? 24 : 18;
        var text = isStart ? 'A' : (isEnd ? 'B' : (idx + ''));

        var icon = L.divIcon({
            className: '',
            html: '<div class="pin-badge" style="width:' + size + 'px;height:' + size + 'px;background:' + bg + ';">' + text + '</div>',
            iconSize: [size + 4, size + 4],
            iconAnchor: [(size + 4)/2, (size + 4)/2]
        });

        var popupHtml = '<b>' + p.label + '</b>';
        if (p.time) popupHtml += '<br/><small>🕒 ' + p.time + '</small>';
        if (p.speed) popupHtml += '<br/><b style="color:#059669;">⚡ ' + p.speed + '</b>';

        L.marker([p.lat, p.lng], { icon: icon }).addTo(map).bindPopup(popupHtml);
    });

    // Query OSRM Road Routing API to snap route onto real streets when >= 2 distinct points
    if (pts.length >= 2) {
        var coordsParam = pts.slice(0, 15).map(function(p) {
            return p.lng + ',' + p.lat;
        }).join(';');
        var osrmUrl = 'https://router.project-osrm.org/route/v1/driving/' + coordsParam + '?overview=full&geometries=geojson';

        fetch(osrmUrl)
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.routes && data.routes.length > 0) {
                    var coords = data.routes[0].geometry.coordinates.map(function(c) {
                        return [c[1], c[0]];
                    });
                    if (coords.length >= 2) {
                        if (glowLine) map.removeLayer(glowLine);
                        if (mainLine) map.removeLayer(mainLine);

                        glowLine = L.polyline(coords, {
                            color: '#1D4ED8',
                            weight: 11,
                            opacity: 0.25,
                            lineCap: 'round',
                            lineJoin: 'round'
                        }).addTo(map);

                        mainLine = L.polyline(coords, {
                            color: '#1D4ED8',
                            weight: 5,
                            opacity: 0.95,
                            lineCap: 'round',
                            lineJoin: 'round'
                        }).addTo(map);

                        map.fitBounds(mainLine.getBounds(), { padding: [28, 28], maxZoom: 16 });
                    }
                }
            })
            .catch(function() {
                // Keep direct GPS polyline on offline/fallback
            });
    }
</script>
</body>
</html>
    """.trimIndent()
}

@Composable
private fun VectorRouteCanvas(
    waypoints: List<RouteWaypoint>,
    isLiveTracking: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padX = 36.dp.toPx()
        val padY = 30.dp.toPx()

        val cols = 6
        val rows = 4
        for (i in 1 until cols) {
            val x = (w / cols) * i
            drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
        }
        for (j in 1 until rows) {
            val y = (h / rows) * j
            drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
        }

        val plotW = (w - padX * 2).coerceAtLeast(10f)
        val plotH = (h - padY * 2).coerceAtLeast(10f)

        val mappedPoints: List<Offset> = if (waypoints.size >= 2) {
            val minLat = waypoints.minOf { it.latitude }
            val maxLat = waypoints.maxOf { it.latitude }
            val minLng = waypoints.minOf { it.longitude }
            val maxLng = waypoints.maxOf { it.longitude }

            val latSpan = (maxLat - minLat).coerceAtLeast(0.0005)
            val lngSpan = (maxLng - minLng).coerceAtLeast(0.0005)
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
            listOf(
                Offset(padX, h - padY),
                Offset(padX + plotW * 0.35f, padY + plotH * 0.35f),
                Offset(padX + plotW * 0.68f, padY + plotH * 0.55f),
                Offset(w - padX, padY)
            )
        }

        if (mappedPoints.size >= 2) {
            val routePath = Path().apply {
                moveTo(mappedPoints.first().x, mappedPoints.first().y)
                for (i in 1 until mappedPoints.size) {
                    lineTo(mappedPoints[i].x, mappedPoints[i].y)
                }
            }

            drawPath(
                path = routePath,
                color = primaryColor.copy(alpha = 0.22f),
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            drawPath(
                path = routePath,
                brush = Brush.linearGradient(colors = listOf(AccentEmerald, primaryColor, AccentAmber)),
                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            drawPath(
                path = routePath,
                color = Color.White.copy(alpha = 0.65f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            for (i in 1 until mappedPoints.size - 1) {
                val pt = mappedPoints[i]
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = primaryColor, radius = 3.5.dp.toPx(), center = pt)
            }

            val startPt = mappedPoints.first()
            drawCircle(color = AccentEmerald.copy(alpha = 0.28f), radius = 12.dp.toPx(), center = startPt)
            drawCircle(color = Color.White, radius = 7.dp.toPx(), center = startPt)
            drawCircle(color = AccentEmerald, radius = 5.dp.toPx(), center = startPt)

            val endPt = mappedPoints.last()
            val endColor = if (isLiveTracking) primaryColor else AccentRose
            drawCircle(color = endColor.copy(alpha = 0.3f), radius = 14.dp.toPx(), center = endPt)
            drawCircle(color = Color.White, radius = 8.dp.toPx(), center = endPt)
            drawCircle(color = endColor, radius = 5.5.dp.toPx(), center = endPt)
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
            // 1. Average Speed
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
    val context = LocalContext.current
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
                .fillMaxWidth(0.95f)
                .padding(vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
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
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bản đồ Lộ trình & Vận tốc",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Street Map with Route Polyline & Markers
                RouteMapCanvas(
                    waypoints = waypoints,
                    startLabel = trip.startLocation,
                    endLabel = trip.endLocation.ifBlank { trip.startLocation },
                    isLiveTracking = false,
                    startLat = trip.startLatitude,
                    startLng = trip.startLongitude,
                    endLat = trip.endLatitude,
                    endLng = trip.endLongitude,
                    modifier = Modifier.height(250.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Open in Google Maps button
                OutlinedButton(
                    onClick = {
                        try {
                            val originParam = if (trip.startLatitude != null && trip.startLongitude != null) {
                                "${trip.startLatitude},${trip.startLongitude}"
                            } else {
                                Uri.encode(trip.startLocation)
                            }
                            val destParam = if (trip.endLatitude != null && trip.endLongitude != null) {
                                "${trip.endLatitude},${trip.endLongitude}"
                            } else {
                                Uri.encode(trip.endLocation.ifBlank { trip.startLocation })
                            }
                            val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$originParam&destination=$destParam")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mở dẫn đường trên Google Maps", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                                    text = "Điểm xuất phát [A] (${DateUtils.formatTime(trip.startTime)})",
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
                                        text = "Điểm đến [B] (${if (trip.endTime > 0) DateUtils.formatTime(trip.endTime) else "Hoàn thành"})",
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
