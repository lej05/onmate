package com.onmate.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** 피그마 ONMATE 와이어프레임 색상 */
object OnColors {
    val Navy = Color(0xFF002C5F)          // 주요 버튼
    val Blue = Color(0xFF1D4ED8)          // 링크·선택·정상
    val BlueBg = Color(0xFFEFF6FF)
    val BlueChip = Color(0xFFDBEAFE)
    val Orange = Color(0xFFC2410C)        // 경고
    val OrangeText = Color(0xFF9A3412)
    val OrangeBg = Color(0xFFFFF7ED)
    val OrangeStrong = Color(0xFFFDBA74)  // 깜빡임 켜짐
    val Red = Color(0xFFE5361B)           // 하차 중 강조 문구
    val Ink = Color(0xFF18181B)
    val Ink2 = Color(0xFF3F3F46)
    val Muted = Color(0xFF52525B)
    val Line = Color(0xFFD4D4D8)
    val Line2 = Color(0xFFA1A1AA)
    val Stroke = Color(0xFF71717A)
    val Surface = Color(0xFFFFFFFF)
    val Fill = Color(0xFFF4F4F5)
    val Fill2 = Color(0xFFE4E4E7)

    val Background = Brush.verticalGradient(listOf(Color(0xFFF6F6F7), Color(0xFFD6DCE5)))
}

@Composable
fun OnMateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = OnColors.Navy,
            onPrimary = Color.White,
            secondary = OnColors.Blue,
            background = OnColors.Fill,
            surface = OnColors.Surface,
            onSurface = OnColors.Ink,
            error = OnColors.Orange,
        ),
        content = content,
    )
}
