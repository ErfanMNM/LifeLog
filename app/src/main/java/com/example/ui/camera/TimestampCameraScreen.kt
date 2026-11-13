package com.example.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.TimestampPhoto
import com.example.data.model.WatermarkStyle
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.util.DateUtils
import com.example.util.LocationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimestampCameraScreen(
    photos: List<TimestampPhoto>,
    currentLocation: LocationResult?,
    isFetchingLocation: Boolean,
    onRefreshLocation: () -> Unit,
    onProcessPhoto: (sourceFilePath: String, style: WatermarkStyle, caption: String, tag: String) -> Unit,
    onDeletePhoto: (TimestampPhoto) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Camera, 1: Gallery
    var selectedStyle by remember { mutableStateOf(WatermarkStyle.MODERN_BADGE) }
    var selectedTag by remember { mutableStateOf("Check-in") }
    var caption by remember { mutableStateOf("") }
    var selectedDetailPhoto by remember { mutableStateOf<TimestampPhoto?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Live clock for watermark preview
    var liveTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            liveTimestamp = System.currentTimeMillis()
            delay(1000)
        }
    }

    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Gallery picker fallback
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                try {
                    val tempFile = copyUriToTempFile(context, uri)
                    if (tempFile != null) {
                        onProcessPhoto(tempFile.absolutePath, selectedStyle, caption, selectedTag)
                        snackbarHostState.showSnackbar("Đã đóng dấu timestamp cho ảnh thành công! ✨")
                        caption = ""
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Lỗi xử lý ảnh: ${e.localizedMessage}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Camera Intent fallback (in case CameraX needs fallback)
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraFile by remember { mutableStateOf<File?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraFile != null) {
            coroutineScope.launch {
                isProcessing = true
                try {
                    onProcessPhoto(tempCameraFile!!.absolutePath, selectedStyle, caption, selectedTag)
                    snackbarHostState.showSnackbar("Đã chụp và đóng dấu timestamp thành công! ✨")
                    caption = ""
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Lỗi: ${e.localizedMessage}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tabs: Camera vs Gallery
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("📷 Chụp ảnh Timestamp") },
                modifier = Modifier.testTag("camera_tab_btn")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("🖼️ Thư viện ảnh (${photos.size})") },
                modifier = Modifier.testTag("gallery_tab_btn")
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedSubTab == 0) {
                // Camera View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp)
                ) {
                    // Camera Preview Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black)
                    ) {
                        if (hasCameraPermission) {
                            CameraXView(
                                modifier = Modifier.fillMaxSize(),
                                onCaptureReady = { captureHelper ->
                                    // captureHelper callback
                                },
                                onCapturePhoto = { file ->
                                    coroutineScope.launch {
                                        isProcessing = true
                                        try {
                                            onProcessPhoto(file.absolutePath, selectedStyle, caption, selectedTag)
                                            snackbarHostState.showSnackbar("Đã chụp và đóng dấu timestamp thành công! ✨")
                                            caption = ""
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Lỗi: ${e.localizedMessage}")
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                isProcessing = isProcessing,
                                liveTimestamp = liveTimestamp,
                                selectedStyle = selectedStyle,
                                selectedTag = selectedTag,
                                caption = caption,
                                location = currentLocation,
                                onOpenPhotoPicker = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onSystemCamera = {
                                    val file = File(context.cacheDir, "camera_snap_${System.currentTimeMillis()}.jpg")
                                    tempCameraFile = file
                                    tempCameraUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    takePictureLauncher.launch(tempCameraUri!!)
                                }
                            )
                        } else {
                            // Permission Request Banner
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Yêu cầu quyền Camera",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ứng dụng cần quyền Camera để chụp ảnh và đóng dấu thời gian, tọa độ GPS thực tế.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    },
                                    modifier = Modifier.testTag("request_camera_perm_btn")
                                ) {
                                    Text("Cấp quyền Camera")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Text("Hoặc chọn ảnh từ máy để đóng dấu", color = Color.White.copy(alpha = 0.9f))
                                }
                            }
                        }
                    }

                    // Bottom Controls & Settings
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Watermark Style Selector
                            Text(
                                text = "Mẫu đóng dấu:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(WatermarkStyle.entries) { style ->
                                    FilterChip(
                                        selected = selectedStyle == style,
                                        onClick = { selectedStyle = style },
                                        label = { Text(style.title, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Tag Selector
                            val tags = listOf("Check-in", "Công việc", "Lộ trình", "Hóa đơn", "Cá nhân")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(tags) { tag ->
                                    FilterChip(
                                        selected = selectedTag == tag,
                                        onClick = { selectedTag = tag },
                                        label = { Text(tag, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Caption Text
                            OutlinedTextField(
                                value = caption,
                                onValueChange = { caption = it },
                                placeholder = { Text("Ghi chú bổ sung (vd: Họp với khách, số km xe)...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("photo_caption_input")
                            )
                        }
                    }
                }
            } else {
                // Gallery View
                if (photos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Collections,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Chưa có ảnh đóng dấu nào",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Chuyển sang tab Chụp ảnh để chụp ảnh có đóng dấu ngày giờ và tọa độ GPS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(photos, key = { it.id }) { photo ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.85f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedDetailPhoto = photo }
                                    .testTag("gallery_photo_${photo.id}")
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = File(photo.filePath),
                                        contentDescription = photo.caption,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Overlay gradient badge
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .padding(8.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "🏷️ ${photo.tag}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF38BDF8)
                                            )
                                            Text(
                                                text = photo.formattedDateTime,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 70.dp)
            )
        }
    }

    if (selectedDetailPhoto != null) {
        PhotoDetailDialog(
            photo = selectedDetailPhoto!!,
            onDismiss = { selectedDetailPhoto = null },
            onDelete = {
                onDeletePhoto(it)
                selectedDetailPhoto = null
            }
        )
    }
}

@Composable
fun CameraXView(
    modifier: Modifier = Modifier,
    onCaptureReady: (ImageCapture) -> Unit,
    onCapturePhoto: (File) -> Unit,
    isProcessing: Boolean,
    liveTimestamp: Long,
    selectedStyle: WatermarkStyle,
    selectedTag: String,
    caption: String,
    location: LocationResult?,
    onOpenPhotoPicker: () -> Unit,
    onSystemCamera: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
                        imageCapture = capture
                        onCaptureReady(capture)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { previewView ->
                // Camera lifecycle update if lens changes
            },
            modifier = Modifier.fillMaxSize()
        )

        // Live Watermark Preview Overlay on viewfinder
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 90.dp, end = 12.dp)
        ) {
            LiveWatermarkBadge(
                style = selectedStyle,
                timestamp = liveTimestamp,
                tag = selectedTag,
                caption = caption,
                location = location
            )
        }

        // Top Quick Controls (Camera flip, System camera fallback)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE TIMESTAMP",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Đổi camera",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Shutter and secondary buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pick from device gallery
            IconButton(
                onClick = onOpenPhotoPicker,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("pick_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Chọn từ thư viện",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Big Shutter Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(enabled = !isProcessing) {
                        val capture = imageCapture
                        if (capture != null) {
                            val tempFile = File(context.cacheDir, "snap_${System.currentTimeMillis()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                            capture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        onCapturePhoto(tempFile)
                                    }

                                    override fun onError(exc: ImageCaptureException) {
                                        exc.printStackTrace()
                                        // fallback to system camera
                                        onSystemCamera()
                                    }
                                }
                            )
                        } else {
                            onSystemCamera()
                        }
                    }
                    .testTag("capture_photo_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                    )
                }
            }

            // System camera shortcut
            IconButton(
                onClick = onSystemCamera,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("system_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Máy ảnh hệ thống",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun LiveWatermarkBadge(
    style: WatermarkStyle,
    timestamp: Long,
    tag: String,
    caption: String,
    location: LocationResult?
) {
    val dateStr = DateUtils.formatFullDateTime(timestamp)
    val gpsStr = if (location != null) "GPS: %.5f, %.5f".format(location.latitude, location.longitude) else null
    val addrStr = location?.address

    when (style) {
        WatermarkStyle.MODERN_BADGE -> {
            Surface(
                color = Color(0xD90F172A),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4B5563)),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp)) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(AccentEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = dateStr,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "🏷️ $tag ${if (caption.isNotBlank()) "• $caption" else ""}",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (gpsStr != null) {
                            Text(
                                text = "📍 $gpsStr",
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp
                            )
                        }
                        if (!addrStr.isNullOrBlank()) {
                            Text(
                                text = "🏢 $addrStr",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        WatermarkStyle.CLASSIC_ORANGE -> {
            Column(modifier = Modifier.widthIn(max = 280.dp)) {
                Text(
                    text = DateUtils.formatWatermarkDateTime(timestamp),
                    color = Color(0xFFFF7A00),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (caption.isNotBlank() || tag.isNotBlank()) {
                    Text(
                        text = "[$tag] $caption",
                        color = Color(0xFFFFA500),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
                if (gpsStr != null) {
                    Text(
                        text = gpsStr,
                        color = Color(0xFFFFA500),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
                if (!addrStr.isNullOrBlank()) {
                    Text(
                        text = addrStr,
                        color = Color(0xFFFFA500),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        WatermarkStyle.MINIMAL_CLEAN -> {
            Column(modifier = Modifier.widthIn(max = 280.dp)) {
                Text(
                    text = "$dateStr | $tag",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                if (addrStr != null) {
                    Text(
                        text = "📍 $addrStr",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        WatermarkStyle.OFFICIAL_CHECKIN -> {
            Surface(
                color = Color(0xEE1E293B),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "✓ XÁC THỰC HIỆN TRƯỜNG • LIFELOG",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "⏰ $dateStr",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "🏷️ Mục đích: $tag",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 10.sp
                    )
                    if (addrStr != null) {
                        Text(
                            text = "📍 $addrStr",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun copyUriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File(context.cacheDir, "picked_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { out ->
            inputStream.copyTo(out)
        }
        inputStream.close()
        tempFile
    } catch (e: Exception) {
        null
    }
}
