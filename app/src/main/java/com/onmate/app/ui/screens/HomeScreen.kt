package com.onmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onmate.app.MainViewModel
import com.onmate.app.UiState
import com.onmate.app.domain.AlertReason
import com.onmate.app.domain.Compartment
import com.onmate.app.domain.Feedback
import com.onmate.app.domain.TripPhase
import com.onmate.app.domain.Zone
import com.onmate.app.ui.components.AreaLook
import com.onmate.app.ui.components.CarMap
import com.onmate.app.ui.components.NavTab
import com.onmate.app.ui.components.OnCard
import com.onmate.app.ui.components.Pill
import com.onmate.app.ui.components.PrimaryButton
import com.onmate.app.ui.components.ScreenHeader
import com.onmate.app.ui.components.SecondaryButton
import com.onmate.app.ui.components.ZoneRow
import com.onmate.app.ui.theme.OnColors

@Composable
fun HomeScreen(st: UiState, vm: MainViewModel) {
    val zoneLooks = mutableMapOf<Zone, AreaLook>()
    val items = mutableMapOf<Compartment, String>()
    val prompt = st.itemPrompt
    val candidateZones = st.candidates.map { it.zone }.toSet()

    when (st.phase) {
        TripPhase.PARKED, TripPhase.DRIVING -> {
            st.openedBefore.filter { it.learnable }.forEach { zoneLooks[it] = AreaLook.ACTIVE }
            if (prompt != null) zoneLooks[prompt.zone] = AreaLook.ACTIVE
        }
        TripPhase.ARRIVED, TripPhase.EXITING -> {
            st.openedBefore.filter { it.learnable }.forEach { z ->
                zoneLooks[z] = when {
                    z in candidateZones -> if (st.phase == TripPhase.EXITING) AreaLook.BLINK else AreaLook.WARN
                    else -> AreaLook.OK
                }
            }
        }
    }
    st.openedBefore.forEach { z ->
        val seat = seatCompartmentOf(z) ?: return@forEach
        val item = itemFor(st, z) ?: return@forEach
        val reopened = st.phase != TripPhase.PARKED && z in st.openedAfter
        items[seat] = if (reopened) "$item · 꺼냄 확인" else item
    }
    Row(Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        CarMap(looksFor(zoneLooks), Modifier.fillMaxHeight(), items = items)
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when {
                st.phase == TripPhase.EXITING && st.candidates.isNotEmpty() -> ExitPanel(st, vm)
                st.phase == TripPhase.ARRIVED || st.phase == TripPhase.EXITING -> {
                    ArrivedPanel(st)
                    LearningCard(st) { vm.selectTab(NavTab.HABIT) }
                }
                prompt != null -> ItemInputPanel(st, vm)
                else -> {
                    ParkedPanel(st)
                    LearningCard(st) { vm.selectTab(NavTab.HABIT) }
                }
            }
        }
    }
}

@Composable
private fun ParkedPanel(st: UiState) {
    ScreenHeader("정차 중", "P단 · 주차 브레이크")
    OnCard(Modifier.fillMaxWidth()) {
        Text("이번 트립에서 연 칸", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        val opened = st.openedBefore.filter { it.learnable }
        if (opened.isEmpty()) {
            Text("아직 연 칸이 없어요. 문이나 트렁크를 열면 자동으로 기록돼요.", fontSize = 15.sp, color = OnColors.Muted)
        } else {
            opened.forEach { z ->
                ZoneRow(
                    title = listOfNotNull(z.label, itemFor(st, z)).joinToString(" · "),
                    subtitle = "출발 전 열림 " + (st.openedAtMs[z]?.let(::hhmm) ?: ""),
                    status = "기록됨", statusColor = OnColors.Blue, background = OnColors.Fill,
                )
            }
        }
    }
}

@Composable
private fun ArrivedPanel(st: UiState) {
    ScreenHeader("도착 · 하차 전", "P단 · 주차 브레이크")
    val minutes = st.departedAtMs?.let { ((System.currentTimeMillis() - it) / 60_000L).coerceAtLeast(1) }
    val route = listOfNotNull(st.origin, st.destination).joinToString(" → ")
    Text(
        "이번 트립: $route" + (st.departedAtMs?.let { " · ${hhmm(it)} 출발" } ?: "") + (minutes?.let { " · ${it}분 주행" } ?: ""),
        fontSize = 16.sp, color = OnColors.Muted,
    )
    OnCard(Modifier.fillMaxWidth()) {
        Text("이번 트립에서 연 칸", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        val opened = st.openedBefore.filter { it.learnable }
        if (opened.isEmpty()) Text("출발 전에 연 칸이 없어요.", fontSize = 15.sp, color = OnColors.Muted)
        opened.forEach { z ->
            val reopened = z in st.openedAfter
            ZoneRow(
                title = listOfNotNull(z.label, itemFor(st, z)).joinToString(" · "),
                subtitle = "출발 전 열림 " + (st.openedAtMs[z]?.let(::hhmm) ?: ""),
                status = if (reopened) "도착 후 열림" else "도착 후 아직 안 열림",
                statusColor = if (reopened) OnColors.Blue else OnColors.Orange,
                background = if (reopened) OnColors.Fill else OnColors.OrangeBg,
            )
        }
    }
}

@Composable
private fun LearningCard(st: UiState, onDetail: () -> Unit) {
    OnCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("학습 현황", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("습관 자세히 보기", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnColors.Blue, modifier = Modifier.clickable(onClick = onDetail))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("학습 기간", "${st.learnedDays}일", Modifier.weight(1f))
            Stat("찾은 습관", "${st.habitsFound}개", Modifier.weight(1f))
            Stat("이번 달 챙긴 물건", "${st.tookItThisMonth}번", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(OnColors.Fill).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, fontSize = 13.sp, color = OnColors.Muted)
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

/** ①-2 출발 전 물건 입력 */
@Composable
private fun ItemInputPanel(st: UiState, vm: MainViewModel) {
    val prompt = st.itemPrompt ?: return
    var custom by remember(prompt.zone) { mutableStateOf<String?>(null) }

    ScreenHeader("출발 전", "P단 · ${prompt.zone.label} 문 닫힘", badgeBg = OnColors.Surface, badgeColor = OnColors.Blue)
    Text("방금 연 칸에 무엇을 두셨나요?", fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Text(
        "한 번만 눌러주면 내릴 때 물건 이름으로 알려드려요.\n출발하면 이 화면은 자동으로 닫혀요.",
        fontSize = 15.sp, color = OnColors.Muted, lineHeight = 22.sp,
    )
    if (prompt.suggestion != null) {
        OnCard(Modifier.fillMaxWidth(), background = OnColors.BlueBg, border = OnColors.BlueChip, padding = PaddingValues(22.dp), spacing = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("추천", OnColors.Navy, OnColors.Surface, fontSize = 12.sp)
                Text("최근 3번 모두 이 칸에 두신 물건", fontSize = 14.sp, color = OnColors.Ink2)
            }
            PrimaryButton("${prompt.suggestion} 맞아요", Modifier.fillMaxWidth()) { vm.answerItemPrompt(prompt.suggestion) }
        }
    }
    Text("다른 물건이에요", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    val chips = prompt.others + "+ 직접 입력"
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        chips.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { label ->
                    Pill(
                        label, OnColors.Surface, if (label.startsWith("+")) OnColors.Muted else OnColors.Ink,
                        border = OnColors.Line2, fontSize = 15.sp, bold = false,
                        padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    ) { if (label.startsWith("+")) custom = "" else vm.answerItemPrompt(label) }
                }
            }
        }
    }
    val c = custom
    if (c != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = c, onValueChange = { custom = it }, singleLine = true, label = { Text("물건 이름") }, modifier = Modifier.weight(1f))
            PrimaryButton("확인", Modifier.width(100.dp), height = 56.dp) { if (c.isNotBlank()) vm.answerItemPrompt(c.trim()) }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SecondaryButton("아무것도 안 뒀어요", Modifier.weight(1f), height = 52.dp) { vm.answerItemPrompt(null) }
        Box(Modifier.width(120.dp).height(52.dp).clickable { vm.skipItemPrompt() }, contentAlignment = Alignment.Center) {
            Text("건너뛰기", fontSize = 17.sp, color = OnColors.Muted)
        }
    }
    Text("주행 중에는 표시되지 않아요 · 설정에서 끌 수 있어요", fontSize = 13.sp, color = OnColors.Muted)
}

/** ③-2 하차 중 · 칸 깜빡임 */
@Composable
private fun ExitPanel(st: UiState, vm: MainViewModel) {
    val first = st.candidates.first()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.width(5.dp).height(34.dp).background(OnColors.Line))
        Text("하차 중", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("운전석 문 열림", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OnColors.Ink2)
    }
    Text("${first.zone.label.eulReul()} 확인하세요", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = OnColors.Red)
    Text(
        "출발 전에 ${first.zone.label.eulReul()} 열었는데 아직 다시 열지 않았어요. 물건이 있을 것 같은 칸이 깜빡여요.",
        fontSize = 18.sp, color = OnColors.Ink2, lineHeight = 28.sp,
    )
    OnCard(Modifier.fillMaxWidth(), spacing = 14.dp) {
        Text("깜빡이는 칸", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        st.candidates.forEach { c ->
            val rate = c.stat?.rate
            val sub = buildString {
                st.openedAtMs[c.zone]?.let { append("출발 전 열림 ").append(hhmm(it)) }
                if (c.reason == AlertReason.HABIT && rate != null) append(" · 습관 일치율 ${(rate * 100).toInt()}%")
                else append(" · 기본 규칙")
            }
            ZoneRow(listOfNotNull(c.zone.label, c.item).joinToString(" · "), sub, "아직 안 열림", OnColors.Orange, OnColors.OrangeBg)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                "· 해당 칸 문을 열면 깜빡임이 멈춰요",
                "· 여러 칸이면 모두 깜빡여요 (일치율 높은 칸이 위쪽 목록)",
                "· 시동을 끄고 문을 잠그면 화면이 꺼져요",
            ).forEach { Text(it, fontSize = 14.sp, color = OnColors.Muted) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton("챙겼어요", Modifier.weight(1f)) { vm.answerExit(Feedback.TOOK_IT) }
        SecondaryButton("두고 갈게요", Modifier.width(220.dp)) { vm.answerExit(Feedback.LEFT_INTENTIONALLY) }
    }
    Spacer(Modifier.height(4.dp))
}
