package com.onmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onmate.app.UiState
import com.onmate.app.domain.Feedback
import com.onmate.app.domain.Trip
import com.onmate.app.ui.components.OnCard
import com.onmate.app.ui.components.Pill
import com.onmate.app.ui.theme.OnColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** ⑥ 트립 기록 */
@Composable
fun LogScreen(st: UiState) {
    val dayFmt = SimpleDateFormat("M월 d일 (E)", Locale.KOREA)
    val groups = st.trips.groupBy { dayFmt.format(Date(it.departedAtMs)) }
    Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("트립 기록", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("트립마다 출발 전과 도착 후에 연 칸을 자동으로 기록해요.", fontSize = 16.sp, color = OnColors.Muted)
        }
        OnCard(Modifier.fillMaxWidth().weight(1f), padding = PaddingValues(horizontal = 28.dp, vertical = 8.dp), spacing = 0.dp) {
            LazyColumn {
                groups.forEach { (day, trips) ->
                    item(key = "d$day") {
                        Text(day, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnColors.Muted, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
                    }
                    trips.forEach { t -> item(key = t.id) { TripRow(t) } }
                }
            }
        }
    }
}

@Composable
private fun TripRow(t: Trip) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(hhmm(t.departedAtMs), fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
        Text(listOfNotNull(t.origin, t.situation.destination).joinToString(" → "), fontSize = 16.sp, modifier = Modifier.width(200.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val recs = t.records.filter { it.openedBefore }
            if (recs.isEmpty()) Text("출발 전에 연 칸 없음", fontSize = 15.sp, color = OnColors.Muted)
            recs.forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(r.zone.label, fontSize = 15.sp)
                    Text("열림 →", fontSize = 15.sp, color = OnColors.Muted)
                    Text(
                        if (r.openedAfter) "다시 열림" else "안 열림",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = if (r.openedAfter) OnColors.Blue else OnColors.Red,
                    )
                }
            }
        }
        Box(Modifier.width(170.dp), contentAlignment = Alignment.Center) {
            val fb = t.records.mapNotNull { it.feedback }.firstOrNull()
            when (fb) {
                Feedback.TOOK_IT -> Pill("알림 · 챙겼어요", OnColors.Red, OnColors.Surface, modifier = Modifier.fillMaxWidth())
                Feedback.LEFT_INTENTIONALLY -> Pill("알림 · 일부러 둠", OnColors.Surface, OnColors.Ink2, border = OnColors.Line2, modifier = Modifier.fillMaxWidth())
                null -> Pill("정상", OnColors.Fill2, OnColors.Ink2, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
