package com.onmate.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onmate.app.ui.theme.OnColors

enum class NavTab(val label: String) { HOME("홈"), HABIT("습관"), LOG("기록"), TAG("물건 태그") }

@Composable
fun NavRail(selected: NavTab, onSelect: (NavTab) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(112.dp)
            .fillMaxHeight()
            .background(OnColors.Surface)
            .padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("ONMATE", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = OnColors.Navy, letterSpacing = 1.sp)
        Spacer(Modifier.height(20.dp))
        NavTab.entries.forEach { tab ->
            val active = tab == selected
            val color = if (active) OnColors.Blue else OnColors.Muted
            Column(
                Modifier
                    .size(width = 88.dp, height = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) OnColors.BlueChip else Color.Transparent)
                    .clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                NavIcon(tab, color)
                Spacer(Modifier.height(6.dp))
                Text(tab.label, fontSize = 14.sp, color = color, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
        }
        // 현대자동차 로고는 CI 최소 크기(20mm ≈ 126dp)+여백 규정 때문에 폭 112dp인 여기엔 넣지 않는다.
        // 시작 화면(SplashScreen)의 BrandLogo 참고.
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun NavIcon(tab: NavTab, color: Color) {
    Canvas(Modifier.size(24.dp)) {
        val s = size.width / 24f
        val stroke = Stroke(width = 2f * s, cap = StrokeCap.Round)
        when (tab) {
            NavTab.HOME -> { // 차량 디스플레이 모양
                drawRoundRect(color, Offset(3 * s, 5 * s), Size(18 * s, 12 * s), CornerRadius(3 * s), style = stroke)
                drawLine(color, Offset(9 * s, 20 * s), Offset(15 * s, 20 * s), 2f * s, StrokeCap.Round)
            }
            NavTab.HABIT -> {
                drawRoundRect(color, Offset(4 * s, 10 * s), Size(4 * s, 10 * s), CornerRadius(1.5f * s), style = stroke)
                drawRoundRect(color, Offset(10 * s, 4 * s), Size(4 * s, 16 * s), CornerRadius(1.5f * s), style = stroke)
                drawRoundRect(color, Offset(16 * s, 8 * s), Size(4 * s, 12 * s), CornerRadius(1.5f * s), style = stroke)
            }
            NavTab.LOG -> {
                drawCircle(color, 9 * s, Offset(12 * s, 12 * s), style = stroke)
                drawLine(color, Offset(12 * s, 7 * s), Offset(12 * s, 12 * s), 2f * s, StrokeCap.Round)
                drawLine(color, Offset(12 * s, 12 * s), Offset(15 * s, 14 * s), 2f * s, StrokeCap.Round)
            }
            NavTab.TAG -> drawTag(color, s, stroke)
        }
    }
}

private fun DrawScope.drawTag(color: Color, s: Float, stroke: Stroke) {
    val p = Path().apply {
        moveTo(3 * s, 12 * s); lineTo(3 * s, 4 * s); lineTo(11 * s, 4 * s)
        lineTo(21 * s, 14 * s); lineTo(13 * s, 22 * s); close()
    }
    drawPath(p, color, style = stroke)
    drawCircle(color, 1.5f * s, Offset(7.5f * s, 7.5f * s))
}
