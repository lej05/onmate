package com.onmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onmate.app.MainViewModel
import com.onmate.app.UiState
import com.onmate.app.domain.Compartment
import com.onmate.app.domain.ItemSuggester
import com.onmate.app.ui.components.AreaLook
import com.onmate.app.ui.components.CarMap
import com.onmate.app.ui.components.OnCard
import com.onmate.app.ui.components.Pill
import com.onmate.app.ui.components.PrimaryButton
import com.onmate.app.ui.components.SecondaryButton
import com.onmate.app.ui.theme.OnColors

/** ⑤ 물건 태그: 칸마다 자주 두는 물건을 한 번만 등록 */
@Composable
fun TagScreen(st: UiState, vm: MainViewModel) {
    val zone = st.tagZone
    val saved = st.tags[zone]
    var text by remember(zone, saved) { mutableStateOf(saved?.item ?: "") }
    var notify by remember(zone, saved) { mutableStateOf(saved?.notify ?: true) }

    val items = mutableMapOf<Compartment, String>()
    st.tags.values.forEach { t -> seatCompartmentOf(t.zone)?.let { items[it] = t.item } }

    Row(Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        CarMap(
            looksFor(mapOf(zone to AreaLook.ACTIVE)),
            Modifier.fillMaxHeight(),
            items = items,
            onTap = { c -> vm.selectTagZone(c.zone) },
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("물건 태그", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Text("칸에 자주 두는 물건을 한 번만 알려주세요. 알림에 물건 이름이 나와요.", fontSize = 16.sp, color = OnColors.Muted)
            }
            OnCard(Modifier.fillMaxWidth(), padding = PaddingValues(28.dp), spacing = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("선택한 칸", fontSize = 15.sp, color = OnColors.Muted)
                    Text(zone.label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text("지도에서 문·트렁크 쪽 칸을 누르면 바꿀 수 있어요 (운전석 제외)", fontSize = 13.sp, color = OnColors.Muted)
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, singleLine = true,
                    label = { Text("물건 이름") }, placeholder = { Text("이름을 입력해주세요") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("자주 쓰는 물건", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                val chips = ItemSuggester().frequentItems(st.trips, limit = 5)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    chips.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { label ->
                                val on = label == text
                                Pill(
                                    label,
                                    if (on) OnColors.BlueChip else OnColors.Surface,
                                    if (on) OnColors.Blue else OnColors.Ink,
                                    border = if (on) OnColors.Blue else OnColors.Line2,
                                    fontSize = 15.sp, bold = on,
                                    padding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                ) { text = label }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = notify, onCheckedChange = { notify = it })
                    Text("이 칸 두고 내림 알림 받기", fontSize = 16.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton("저장", Modifier.weight(1f)) { vm.saveTag(text, notify) }
                SecondaryButton("태그 지우기", Modifier.width(160.dp)) { vm.removeTag() }
            }
            Text("정차 중에만 편집할 수 있어요", fontSize = 13.sp, color = OnColors.Muted)
        }
    }
}
