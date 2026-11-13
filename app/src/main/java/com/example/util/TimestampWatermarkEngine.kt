package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import com.example.data.model.WatermarkStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimestampWatermarkEngine {

    suspend fun processAndStampImage(
        context: Context,
        inputFilePath: String,
        style: WatermarkStyle,
        caption: String,
        tag: String,
        timestamp: Long,
        latitude: Double?,
        longitude: Double?,
        address: String?
    ): File = withContext(Dispatchers.IO) {
        // 1. Decode original bitmap respecting orientation
        val rawBitmap = loadRotatedBitmap(inputFilePath)
        val workingBitmap = rawBitmap.copy(Bitmap.Config.ARGB_8888, true)
        rawBitmap.recycle()

        // 2. Draw watermark
        val canvas = Canvas(workingBitmap)
        drawWatermarkOnCanvas(
            canvas = canvas,
            width = workingBitmap.width,
            height = workingBitmap.height,
            style = style,
            caption = caption,
            tag = tag,
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            address = address
        )

        // 3. Save to output directory
        val outputDir = File(context.filesDir, "timestamp_photos").apply { mkdirs() }
        val fileName = "STAMP_${System.currentTimeMillis()}.jpg"
        val outputFile = File(outputDir, fileName)

        FileOutputStream(outputFile).use { fos ->
            workingBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }
        workingBitmap.recycle()

        outputFile
    }

    private fun loadRotatedBitmap(filePath: String): Bitmap {
        val bitmap = BitmapFactory.decodeFile(filePath)
            ?: throw IllegalStateException("Không thể đọc tệp ảnh tại: $filePath")

        val exif = try {
            ExifInterface(filePath)
        } catch (e: Exception) {
            null
        }

        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL

        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        return if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            bitmap.recycle()
            rotated
        } else {
            bitmap
        }
    }

    fun drawWatermarkOnCanvas(
        canvas: Canvas,
        width: Int,
        height: Int,
        style: WatermarkStyle,
        caption: String,
        tag: String,
        timestamp: Long,
        latitude: Double?,
        longitude: Double?,
        address: String?
    ) {
        val baseScale = (width.coerceAtMost(height) / 800f).coerceIn(1f, 4f)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
        val dateViStr = SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale("vi", "VN")).format(Date(timestamp))

        val gpsStr = if (latitude != null && longitude != null) {
            String.format(Locale.US, "GPS: %.5f, %.5f", latitude, longitude)
        } else null

        val addrStr = address?.takeIf { it.isNotBlank() }
        val noteStr = caption.takeIf { it.isNotBlank() }

        when (style) {
            WatermarkStyle.MODERN_BADGE -> {
                drawModernBadge(
                    canvas, width, height, baseScale,
                    timeText = dateViStr,
                    tag = tag,
                    gps = gpsStr,
                    address = addrStr,
                    caption = noteStr
                )
            }
            WatermarkStyle.CLASSIC_ORANGE -> {
                drawClassicOrange(
                    canvas, width, height, baseScale,
                    dateStr = dateStr,
                    tag = tag,
                    gps = gpsStr,
                    address = addrStr,
                    caption = noteStr
                )
            }
            WatermarkStyle.MINIMAL_CLEAN -> {
                drawMinimalClean(
                    canvas, width, height, baseScale,
                    dateStr = dateViStr,
                    tag = tag,
                    gps = gpsStr,
                    address = addrStr,
                    caption = noteStr
                )
            }
            WatermarkStyle.OFFICIAL_CHECKIN -> {
                drawOfficialCheckin(
                    canvas, width, height, baseScale,
                    dateStr = dateViStr,
                    tag = tag,
                    gps = gpsStr,
                    address = addrStr,
                    caption = noteStr
                )
            }
        }
    }

    private fun drawModernBadge(
        canvas: Canvas,
        width: Int,
        height: Int,
        scale: Float,
        timeText: String,
        tag: String,
        gps: String?,
        address: String?,
        caption: String?
    ) {
        val pad = 24f * scale
        val cardMargin = 20f * scale
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E2E8F0")
            textSize = 15f * scale
            typeface = Typeface.DEFAULT
        }
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 14f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val lines = mutableListOf<String>()
        lines.add(timeText)
        if (!tag.isBlank()) lines.add("[$tag] ${caption ?: ""}".trim())
        if (!gps.isNullOrBlank()) lines.add(gps)
        if (!address.isNullOrBlank()) lines.add(address)

        val lineHeight = 26f * scale
        val cardWidth = (width - cardMargin * 2).coerceAtMost(650f * scale)
        val cardHeight = pad * 2 + lines.size * lineHeight

        val left = cardMargin
        val bottom = height - cardMargin
        val top = bottom - cardHeight
        val right = left + cardWidth

        // Draw card background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D90F172A") // Deep slate semi-transparent
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            style = Paint.Style.STROKE
            strokeWidth = 2f * scale
        }
        val rectF = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rectF, 16f * scale, 16f * scale, bgPaint)
        canvas.drawRoundRect(rectF, 16f * scale, 16f * scale, borderPaint)

        // Accent indicator bar
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#10B981") // Emerald accent
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(left + 8f * scale, top + 14f * scale, left + 14f * scale, bottom - 14f * scale),
            4f * scale, 4f * scale, accentPaint
        )

        var textY = top + pad + 18f * scale
        val textX = left + pad + 8f * scale

        // Line 1: Time
        canvas.drawText(timeText, textX, textY, titlePaint)
        textY += lineHeight + 4f * scale

        // Other lines
        if (!tag.isBlank()) {
            val tagText = "🏷️ $tag${if (!caption.isNullOrBlank()) " • $caption" else ""}"
            canvas.drawText(truncate(tagText, 45), textX, textY, tagPaint)
            textY += lineHeight
        }

        if (!gps.isNullOrBlank()) {
            canvas.drawText("📍 $gps", textX, textY, subPaint)
            textY += lineHeight
        }

        if (!address.isNullOrBlank()) {
            canvas.drawText("🏢 ${truncate(address, 48)}", textX, textY, subPaint)
        }
    }

    private fun drawClassicOrange(
        canvas: Canvas,
        width: Int,
        height: Int,
        scale: Float,
        dateStr: String,
        tag: String,
        gps: String?,
        address: String?,
        caption: String?
    ) {
        val margin = 28f * scale
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF7A00") // Classic digital camera orange
            textSize = 32f * scale
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(4f * scale, 2f * scale, 2f * scale, Color.BLACK)
        }
        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFA500")
            textSize = 16f * scale
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setShadowLayer(3f * scale, 1f * scale, 1f * scale, Color.BLACK)
        }

        var textY = height - margin
        val textX = margin

        if (!address.isNullOrBlank()) {
            canvas.drawText(truncate(address, 50), textX, textY, infoPaint)
            textY -= 24f * scale
        }
        if (!gps.isNullOrBlank()) {
            canvas.drawText(gps, textX, textY, infoPaint)
            textY -= 24f * scale
        }
        if (!caption.isNullOrBlank() || !tag.isBlank()) {
            val note = if (!caption.isNullOrBlank()) "[$tag] $caption" else "[$tag]"
            canvas.drawText(truncate(note, 45), textX, textY, infoPaint)
            textY -= 26f * scale
        }
        canvas.drawText(dateStr, textX, textY, timePaint)
    }

    private fun drawMinimalClean(
        canvas: Canvas,
        width: Int,
        height: Int,
        scale: Float,
        dateStr: String,
        tag: String,
        gps: String?,
        address: String?,
        caption: String?
    ) {
        val margin = 24f * scale
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(6f * scale, 0f, 2f * scale, Color.parseColor("#B3000000"))
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            textSize = 14f * scale
            typeface = Typeface.DEFAULT
            setShadowLayer(4f * scale, 0f, 1f * scale, Color.parseColor("#B3000000"))
        }

        var y = height - margin
        val x = margin

        if (!address.isNullOrBlank()) {
            canvas.drawText("📍 ${truncate(address, 50)}", x, y, subPaint)
            y -= 22f * scale
        }
        if (!gps.isNullOrBlank()) {
            canvas.drawText(gps, x, y, subPaint)
            y -= 22f * scale
        }
        val title = if (!caption.isNullOrBlank()) "$dateStr | $caption" else "$dateStr | $tag"
        canvas.drawText(truncate(title, 45), x, y, textPaint)
    }

    private fun drawOfficialCheckin(
        canvas: Canvas,
        width: Int,
        height: Int,
        scale: Float,
        dateStr: String,
        tag: String,
        gps: String?,
        address: String?,
        caption: String?
    ) {
        val margin = 20f * scale
        val cardW = (width - margin * 2).coerceAtMost(600f * scale)
        val cardH = 170f * scale
        val left = margin
        val bottom = height - margin
        val top = bottom - cardH
        val right = left + cardW

        // Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EE1E293B")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 3f * scale
        }
        val cardRect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(cardRect, 12f * scale, 12f * scale, bgPaint)
        canvas.drawRoundRect(cardRect, 12f * scale, 12f * scale, borderPaint)

        // Header banner
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0284C7")
            style = Paint.Style.FILL
        }
        val headerRect = RectF(left, top, right, top + 36f * scale)
        canvas.drawRoundRect(headerRect, 12f * scale, 12f * scale, headerPaint)
        // flat bottom for header
        canvas.drawRect(RectF(left, top + 24f * scale, right, top + 36f * scale), headerPaint)

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("✓ XÁC THỰC HIỆN TRƯỜNG • LIFELOG", left + 14f * scale, top + 24f * scale, headerTextPaint)

        val contentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f * scale
            typeface = Typeface.DEFAULT
        }
        val boldContentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            textSize = 16f * scale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var textY = top + 60f * scale
        val textX = left + 16f * scale

        canvas.drawText("⏰ Thời gian: $dateStr", textX, textY, boldContentPaint)
        textY += 24f * scale

        canvas.drawText("🏷️ Mục đích: $tag ${if (!caption.isNullOrBlank()) "($caption)" else ""}", textX, textY, contentPaint)
        textY += 24f * scale

        if (!gps.isNullOrBlank()) {
            canvas.drawText("🌐 Tọa độ: $gps", textX, textY, contentPaint)
            textY += 24f * scale
        }

        if (!address.isNullOrBlank()) {
            canvas.drawText("📍 Địa điểm: ${truncate(address, 46)}", textX, textY, contentPaint)
        }
    }

    private fun truncate(text: String, maxChars: Int): String {
        return if (text.length > maxChars) text.take(maxChars - 3) + "..." else text
    }
}
