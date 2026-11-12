package com.onmate.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onmate.app.ui.theme.OnColors

@Composable
fun OnCard(
    modifier: Modifier = Modifier,
    background: Color = OnColors.Surface,
    border: Color = OnColors.Line,
    padding: PaddingValues = PaddingValues(24.dp),
    spacing: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(20.dp))
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

@Composable
fun Pill(
    text: String,
    background: Color,
    color: Color,
    modifier: Modifier = Modifier,
    border: Color? = null,
    fontSize: TextUnit = 13.sp,
    bold: Boolean = true,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(999.dp)
    var m = modifier.clip(shape).background(background)
    if (border != null) m = m.border(1.5.dp, border, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m.padding(padding), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontSize = fontSize, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, height: Dp = 60.dp, onClick: () -> Unit) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(OnColors.Navy)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, height: Dp = 60.dp, onClick: () -> Unit) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(OnColors.Surface)
            .border(1.5.dp, OnColors.Line2, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = OnColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
}

/** 화면 제목 + 상태 칩 */
@Composable
fun ScreenHeader(title: String, badge: String? = null, badgeBg: Color = OnColors.Fill2, badgeColor: Color = OnColors.Ink2) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = OnColors.Ink)
        if (badge != null) Pill(badge, badgeBg, badgeColor, fontSize = 14.sp, padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp))
    }
}

/** 칸 한 줄 (이번 트립에서 연 칸, 깜빡이는 칸 등) */
@Composable
fun ZoneRow(title: String, subtitle: String, status: String, statusColor: Color, background: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OnColors.Ink)
            Text(subtitle, fontSize = 14.sp, color = OnColors.Muted)
        }
        Text(status, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = statusColor)
    }
}
