package com.onmate.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

/**
 * 현대자동차 좌우조합 로고 (CI 규정 준수).
 *
 * CI 규정
 * - 좌우조합 로고(심볼 + 워드마크) 사용 권장
 * - 최소 크기: 가로 20mm ≈ 126dp (160dp = 1인치 = 25.4mm 기준)
 * - 최소 공간: 로고 상하좌우에 심볼 높이(H)만큼 여백. 좌우조합 로고는 심볼 높이 ≈ 로고 전체 높이
 *
 * 로고 파일은 직접 그리지 말고 현대자동차 공식 CI 자료에서 받은 파일만 사용한다.
 * `app/src/main/res/drawable/hyundai_logo.png`(또는 .xml 벡터)로 넣으면 표시되고, 없으면 아무것도 그리지 않는다.
 * 공모전 앱에서 로고를 써도 되는지는 주최 측에 먼저 확인할 것.
 */
@SuppressLint("DiscouragedApi")
@Composable
fun BrandLogo(width: Dp = 160.dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val id = remember { ctx.resources.getIdentifier("hyundai_logo", "drawable", ctx.packageName) }
    if (id == 0) return

    val painter = painterResource(id)
    val w = max(width, MIN_WIDTH)
    val ratio = painter.intrinsicSize.let { if (it.width > 0f) it.height / it.width else 0.2f }
    val symbolHeight = w * ratio // 좌우조합 로고: 심볼 높이 ≈ 로고 높이

    Image(
        painter = painter,
        contentDescription = "현대자동차",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .padding(symbolHeight) // 최소 공간 H
            .width(w),
    )
}

/** 좌우조합 로고 최소 크기 20mm */
private val MIN_WIDTH = 126.dp
