package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThemeSetting

@Composable
fun ThemeSelectDialog(
    currentTheme: ThemeSetting,
    onDismiss: () -> Unit,
    onSelectTheme: (ThemeSetting) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Spa,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Giao diện & Chống chói mắt",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Chọn tông màu hiển thị dễ chịu nhất cho mắt bạn:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Warm Light (Anti-glare Eye-care)
                ThemeOptionCard(
                    title = "Sáng êm mắt (Chống chói)",
                    subtitle = "Tông ngà ấm dịu, lọc bớt ánh sáng xanh gắt, không lóa mắt (Khuyên dùng)",
                    icon = Icons.Default.Spa,
                    isSelected = currentTheme == ThemeSetting.WARM_LIGHT,
                    previewBg = Color(0xFFF3F1EC),
                    previewSurface = Color(0xFFFAF9F6),
                    previewAccent = Color(0xFF1D4ED8),
                    onClick = {
                        onSelectTheme(ThemeSetting.WARM_LIGHT)
                    },
                    isRecommended = true
                )

                // 2. Clean Light (Neutral soft gray)
                ThemeOptionCard(
                    title = "Sáng tự nhiên",
                    subtitle = "Tông xám nhạt nhẹ nhàng, hiện đại, thanh lịch",
                    icon = Icons.Default.LightMode,
                    isSelected = currentTheme == ThemeSetting.CLEAN_LIGHT,
                    previewBg = Color(0xFFF1F3F5),
                    previewSurface = Color(0xFFFAFBFC),
                    previewAccent = Color(0xFF0284C7),
                    onClick = {
                        onSelectTheme(ThemeSetting.CLEAN_LIGHT)
                    }
                )

                // 3. Dark Mode
                ThemeOptionCard(
                    title = "Giao diện tối",
                    subtitle = "Nền tối đen xám, bảo vệ mắt ban đêm và tiết kiệm pin",
                    icon = Icons.Default.DarkMode,
                    isSelected = currentTheme == ThemeSetting.DARK,
                    previewBg = Color(0xFF0F172A),
                    previewSurface = Color(0xFF1E293B),
                    previewAccent = Color(0xFF38BDF8),
                    onClick = {
                        onSelectTheme(ThemeSetting.DARK)
                    }
                )

                // 4. System Mode
                ThemeOptionCard(
                    title = "Theo hệ thống",
                    subtitle = "Tự động đổi theo cài đặt Sáng/Tối của điện thoại",
                    icon = Icons.Default.SettingsBrightness,
                    isSelected = currentTheme == ThemeSetting.SYSTEM,
                    previewBg = MaterialTheme.colorScheme.background,
                    previewSurface = MaterialTheme.colorScheme.surface,
                    previewAccent = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onSelectTheme(ThemeSetting.SYSTEM)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("confirm_theme_dialog_btn")
            ) {
                Text("Xong")
            }
        }
    )
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    previewBg: Color,
    previewSurface: Color,
    previewAccent: Color,
    onClick: () -> Unit,
    isRecommended: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Khuyên dùng",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Color swatches preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(previewBg)
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(previewSurface)
                            .border(0.5.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(previewAccent)
                    )
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Đã chọn",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
