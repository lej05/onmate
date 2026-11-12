package com.onmate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onmate.app.ui.components.BrandLogo
import com.onmate.app.ui.theme.OnColors

/**
 * 시작 화면. 현대자동차 로고는 CI 최소 크기(20mm)와 최소 공간(심볼 높이)을 지키기 위해
 * 폭 112dp인 내비게이션 대신 여기에 둔다.
 */
@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("ONMATE", fontSize = 56.sp, fontWeight = FontWeight.Medium, color = OnColors.Navy, letterSpacing = 4.sp)
            Text("내리기 전에, 온메이트가 먼저 챙겨요", fontSize = 18.sp, color = OnColors.Muted)
        }
        BrandLogo(width = 160.dp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}
