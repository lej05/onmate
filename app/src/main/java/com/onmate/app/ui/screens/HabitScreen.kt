package com.onmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onmate.app.UiState
import com.onmate.app.domain.AlertDecider
import com.onmate.app.domain.HabitLearner
import com.onmate.app.domain.HabitStat
import com.onmate.app.domain.SituationKey
import com.onmate.app.domain.Zone
import com.onmate.app.ui.components.OnCard
import com.onmate.app.ui.components.Pill
import com.onmate.app.ui.theme.OnColors

private val decider = AlertDecider()

fun situationTitle(s: SituationKey): String =
    "${s.dayType.label} ${s.timeSlot.label}" + (s.destination?.let { " · $it" } ?: "")

/** ④ 습관 대시보드 */
@Composable
fun HabitScreen(st: UiState, onSelect: (SituationKey) -> Unit) {
    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("내 하차 습관", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("출발 전에 연 칸을 도착 후 다시 여는 비율이에요. 70% 이상이면 알림 대상이 돼요.", fontSize = 16.sp, color = OnColors.Muted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            st.habitSituations.forEach { s ->
                val active = s == st.habitSituation
                Pill(
                    situationTitle(s),
                    if (active) OnColors.Ink else OnColors.Surface,
                    if (active) OnColors.Surface else OnColors.Ink,
                    border = if (active) null else OnColors.Line2,
                    fontSize = 16.sp, bold = active,
                    padding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                ) { onSelect(s) }
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            OnCard(Modifier.weight(1f).fillMaxHeight(), padding = PaddingValues(horizontal = 24.dp, vertical = 8.dp), spacing = 0.dp) {
                val rows = Zone.learnableZones
                    .mapNotNull { z -> st.habitStats[z] }
                    .sortedWith(compareByDescending<HabitStat> { it.isLearned }.thenByDescending { it.openedCount })
                rows.forEachIndexed { i, stat ->
                    HabitRow(stat, st.tags[stat.zone]?.item)
                    if (i < rows.lastIndex) HorizontalDivider(color = OnColors.Fill2)
                }
            }
            Column(Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OnCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp), spacing = 10.dp) {
                    Text("알림 기준", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "기록 3회 미만: 출발 전에 연 칸을 안 열면 알림\n기록 3회 이상: 재개방률 70% 이상인 칸만 알림",
                        fontSize = 14.sp, color = OnColors.Ink2, lineHeight = 22.sp,
                    )
                }
                OnCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp), spacing = 12.dp) {
                    Text("알림 줄인 상황", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    val learner = HabitLearner()
                    val reduced = st.habitSituations.flatMap { s ->
                        learner.stats(st.trips, s).values.filter { it.intentionalCount > 0 }.map { s to it }
                    }
                    if (reduced.isEmpty()) Text("아직 없어요", fontSize = 14.sp, color = OnColors.Muted)
                    reduced.forEach { (s, stat) ->
                        Column {
                            Text("${situationTitle(s)} · ${stat.zone.label}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("'일부러 두는 거예요' ${stat.intentionalCount}번 선택", fontSize = 14.sp, color = OnColors.Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitRow(stat: HabitStat, item: String?) {
    val rate = stat.rate ?: 0f
    val target = stat.isLearned && rate >= decider.thresholdFor(stat)
    Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.width(200.dp)) {
            Text(stat.zone.label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(item ?: "태그 없음", fontSize = 14.sp, color = OnColors.Muted)
        }
        // 5칸 막대
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val filled = if (stat.isLearned) Math.round(rate * 5) else 0
            repeat(5) { i ->
                val color = when {
                    !stat.isLearned -> Color.Transparent
                    i < filled -> if (target) OnColors.Navy else OnColors.Line2
                    else -> OnColors.Fill2
                }
                var m = Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(color)
                if (!stat.isLearned) m = m.border(1.dp, OnColors.Line2, RoundedCornerShape(7.dp))
                Box(m)
            }
        }
        Column(Modifier.width(110.dp)) {
            if (stat.isLearned) {
                Text("${(rate * 100).toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${stat.openedCount}회 중 ${stat.reopenedCount}회", fontSize = 13.sp, color = OnColors.Muted)
            } else {
                Text("학습 중", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${stat.openedCount}회 기록", fontSize = 13.sp, color = OnColors.Muted)
            }
        }
        Box(Modifier.width(120.dp), contentAlignment = Alignment.Center) {
            when {
                !stat.isLearned -> Pill("기본 규칙", OnColors.Surface, OnColors.Ink2, border = OnColors.Line2)
                target -> Pill("알림 대상", OnColors.BlueChip, OnColors.Blue)
                else -> Pill("알림 안 함", OnColors.Fill2, OnColors.Ink2)
            }
        }
    }
}
