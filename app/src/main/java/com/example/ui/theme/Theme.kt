package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeSetting(val displayName: String, val subtitle: String) {
    WARM_LIGHT("Sáng êm mắt (Chống chói)", "Tông ấm dịu, chống chói và giảm mỏi mắt"),
    CLEAN_LIGHT("Sáng tự nhiên", "Tông xám nhạt trung tính, dịu nhẹ"),
    DARK("Giao diện tối", "Bảo vệ mắt ban đêm, tiết kiệm pin"),
    SYSTEM("Theo hệ thống", "Tự động đổi theo cài đặt thiết bị")
}

// 1. Dark Mode
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = Slate900,
    primaryContainer = PrimaryBlueDark,
    onPrimaryContainer = Color.White,
    secondary = AccentAmber,
    onSecondary = Slate900,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = AccentEmerald,
    onTertiary = Slate900,
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    outline = Slate600
)

// 2. Warm Light (Anti-glare / Eye-comfort Paper Tone) - Default Light
val WarmLightColorScheme = lightColorScheme(
    primary = Color(0xFF1D4ED8), // Deep royal blue, calm and mature
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = Color(0xFFF3F1EC), // Warm matte paper background, eliminates blue-light glare
    onBackground = Color(0xFF292524), // Warm charcoal, high readability without stark black glare
    surface = Color(0xFFFAF9F6), // Soft warm off-white card, never 100% blinding white
    onSurface = Color(0xFF292524),
    surfaceVariant = Color(0xFFE8E4DC),
    onSurfaceVariant = Color(0xFF57534E),
    outline = Color(0xFFD6D1C7),
    outlineVariant = Color(0xFFE4DFD6)
)

// 3. Clean Light (Soft Neutral Gray - Non-glare)
val CleanLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = PrimaryBlueDark,
    secondary = AccentAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = AccentEmerald,
    onTertiary = Color.White,
    background = Color(0xFFF1F3F5), // Matte soft gray background
    onBackground = Color(0xFF212529),
    surface = Color(0xFFFAFBFC), // Slightly tinted soft surface
    onSurface = Color(0xFF212529),
    surfaceVariant = Color(0xFFE9ECEF),
    onSurfaceVariant = Color(0xFF495057),
    outline = Color(0xFFCED4DA),
    outlineVariant = Color(0xFFDEE2E6)
)

@Composable
fun MyApplicationTheme(
    themeSetting: ThemeSetting = ThemeSetting.WARM_LIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            val isDark = when (themeSetting) {
                ThemeSetting.DARK -> true
                ThemeSetting.WARM_LIGHT, ThemeSetting.CLEAN_LIGHT -> false
                ThemeSetting.SYSTEM -> isSystemDark
            }
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeSetting == ThemeSetting.DARK -> DarkColorScheme
        themeSetting == ThemeSetting.WARM_LIGHT -> WarmLightColorScheme
        themeSetting == ThemeSetting.CLEAN_LIGHT -> CleanLightColorScheme
        themeSetting == ThemeSetting.SYSTEM -> if (isSystemDark) DarkColorScheme else WarmLightColorScheme
        else -> WarmLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
