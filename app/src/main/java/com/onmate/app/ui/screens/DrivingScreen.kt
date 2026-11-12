package com.onmate.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onmate.app.UiState
import com.onmate.app.ui.components.OnCard
import com.onmate.app.ui.components.Pill
import com.onmate.app.ui.theme.OnColors

/** ② 주행 중: 조작 없이 기록 상태만 보여준다 */
@Composable
fun DrivingScreen(st: UiState) {
    Box(Modifier.fillMaxSize()) {
        Text(
            "ONMATE", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = OnColors.Navy, letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 40.dp, top = 36.dp),
        )
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Pill("주행 중 · 기록만 해요", OnColors.Ink2, OnColors.Surface, fontSize = 16.sp, padding = PaddingValues(horizontal = 18.dp, vertical = 8.dp))
            CarIcon()
            Text("주행 중에는 화면을 띄우지 않아요", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = OnColors.Ink)
            Text("도착하면 이번 트립에서 연 칸을 확인해 드릴게요", fontSize = 20.sp, color = OnColors.Muted)
            val opened = st.openedBefore.filter { it.learnable }
            if (opened.isNotEmpty()) {
                OnCard(padding = PaddingValues(horizontal = 32.dp, vertical = 22.dp), spacing = 14.dp) {
                    Text("출발 전에 연 칸", fontSize = 15.sp, color = OnColors.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        opened.forEach { z ->
                            Pill(
                                listOfNotNull(z.label, itemFor(st, z)).joinToString(" · "),
                                OnColors.BlueChip, OnColors.Blue, fontSize = 18.sp,
                                padding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
            val dest = st.destination
            if (dest != null) {
                Text(
                    dest + (st.remainingMin?.let { " · 남은 시간 ${it}분" } ?: ""),
                    fontSize = 15.sp, color = OnColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun CarIcon() {
    Canvas(Modifier.size(96.dp)) {
        val s = size.width / 24f
        val c = OnColors.Ink
        drawRoundRect(c, Offset(4 * s, 9 * s), Size(16 * s, 7 * s), CornerRadius(2 * s))
        drawLine(c, Offset(6 * s, 9 * s), Offset(8 * s, 5 * s), 2 * s, StrokeCap.Round)
        drawLine(c, Offset(8 * s, 5 * s), Offset(16 * s, 5 * s), 2 * s, StrokeCap.Round)
        drawLine(c, Offset(16 * s, 5 * s), Offset(18 * s, 9 * s), 2 * s, StrokeCap.Round)
        drawCircle(c, 2 * s, Offset(7.5f * s, 17 * s))
        drawCircle(c, 2 * s, Offset(16.5f * s, 17 * s))
    }
}
