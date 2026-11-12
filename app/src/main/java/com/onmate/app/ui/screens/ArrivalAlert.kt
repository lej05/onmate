package com.onmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onmate.app.UiState
import com.onmate.app.domain.AlertReason
import com.onmate.app.domain.Feedback
import com.onmate.app.ui.components.AreaLook
import com.onmate.app.ui.components.CarMap
import com.onmate.app.ui.components.PrimaryButton
import com.onmate.app.ui.components.SecondaryButton
import com.onmate.app.ui.theme.OnColors

/** ③ 하차 전 알림 (도착 직후, 운전석 문을 열기 전) */
@Composable
fun ArrivalAlert(st: UiState, onAnswer: (Feedback) -> Unit) {
    val c = st.candidates.firstOrNull() ?: return
    val stat = c.stat
    val zone = c.zone.label

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x9918181B))
            // 뒤쪽 화면 터치 막기
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(760.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(OnColors.Surface)
                .padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                CarMap(
                    looksFor(st.candidates.associate { it.zone to AreaLook.WARN }),
                    Modifier.width(150.dp),
                    showCallouts = false,
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(zone, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnColors.Red)
                    Text(
                        if (c.item != null) "${c.item}, 두고 내리시는 건 아니죠?" else "${zone}에 두고 내리시는 건 없나요?",
                        fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp,
                    )
                    val rate = stat?.rate
                    if (c.reason == AlertReason.HABIT && stat != null && rate != null) {
                        val where = st.destination?.let { "${it}에 도착하면" } ?: "이 시간에 도착하면"
                        Text("평소 $where ${zone.eulReul()} 여셨어요. 오늘은 아직 열지 않으셨어요.", fontSize = 18.sp, color = OnColors.Ink2, lineHeight = 28.sp)
                        Text("최근 ${stat.openedCount}번 중 ${stat.reopenedCount}번 다시 열었어요 (${(rate * 100).toInt()}%)", fontSize = 14.sp, color = OnColors.Muted)
                    } else {
                        Text("출발 전에 ${zone.eulReul()} 열었는데 아직 다시 열지 않았어요.", fontSize = 18.sp, color = OnColors.Ink2, lineHeight = 28.sp)
                        Text("아직 습관을 배우는 중이에요 (기록 ${stat?.openedCount ?: 0}회)", fontSize = 14.sp, color = OnColors.Muted)
                    }
                    if (st.candidates.size > 1) {
                        Text("외 ${st.candidates.size - 1}칸: " + st.candidates.drop(1).joinToString { it.zone.label }, fontSize = 14.sp, color = OnColors.Orange)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PrimaryButton("챙겼어요", Modifier.weight(1f), height = 64.dp) { onAnswer(Feedback.TOOK_IT) }
                SecondaryButton("두고 갈게요", Modifier.weight(1f), height = 64.dp) { onAnswer(Feedback.LEFT_INTENTIONALLY) }
            }
            Text(
                "P단과 주차 브레이크가 체결된 뒤에만 표시돼요 · 답변은 습관 학습에 반영돼요",
                fontSize = 13.sp, color = OnColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
