package com.onmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onmate.app.MainViewModel
import com.onmate.app.UiState
import com.onmate.app.domain.Zone
import com.onmate.app.ui.components.OnCard
import com.onmate.app.ui.components.Pill
import com.onmate.app.ui.theme.OnColors

/**
 * 시연용 차량 조작 패널 (가짜 데이터). 오른쪽 아래 'DEMO'를 눌러 열고 닫는다.
 * 실제 차량/Virtual Console 연동 후에는 숨기거나 디버그 빌드에서만 보이게 한다.
 */
@Composable
fun DemoPanel(st: UiState, vm: MainViewModel, modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (st.showDemo) {
            OnCard(Modifier.width(420.dp), padding = PaddingValues(16.dp), spacing = 10.dp) {
                Text("시연용 차량 조작 · 현재: ${st.phase.name}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OnColors.Ink2)
                Group("출발 전 (열고 닫기)") {
                    Btn("뒷좌석 왼쪽") { vm.demoOpenClose(Zone.REAR_LEFT) }
                    Btn("뒷좌석 오른쪽") { vm.demoOpenClose(Zone.REAR_RIGHT) }
                    Btn("트렁크") { vm.demoOpenClose(Zone.TRUNK) }
                    Btn("조수석") { vm.demoOpenClose(Zone.PASSENGER) }
                }
                Group("주행") {
                    Btn("출발") { vm.demoDepart() }
                    Btn("도착(P·주차브레이크)") { vm.demoArrive() }
                }
                Group("하차") {
                    Btn("운전석 문 열기") { vm.demoOpen(Zone.DRIVER) }
                    Btn("뒷좌석 왼쪽 열기") { vm.demoOpen(Zone.REAR_LEFT) }
                    Btn("트렁크 열기") { vm.demoOpen(Zone.TRUNK) }
                }
                Group("기타") {
                    Btn("트립 마감") { vm.finishTrip() }
                    Btn("문 모두 닫기") { Zone.entries.forEach { vm.demoClose(it) } }
                    Btn("자동 시나리오") { vm.demoScenario() }
                }
            }
        }
        Pill(if (st.showDemo) "DEMO 닫기" else "DEMO", OnColors.Ink2, OnColors.Surface, fontSize = 12.sp) { vm.toggleDemo() }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 12.sp, color = OnColors.Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

@Composable
private fun Btn(label: String, onClick: () -> Unit) {
    Pill(label, OnColors.Fill, OnColors.Ink, border = OnColors.Line2, fontSize = 12.sp, bold = false,
        padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp), onClick = onClick)
}
