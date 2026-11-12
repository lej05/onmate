package com.onmate.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.onmate.app.domain.Compartment
import com.onmate.app.ui.theme.OnColors

/** 수납함 표시 상태 */
enum class AreaLook { NORMAL, WARN, ACTIVE, OK, BLINK }

/**
 * 위에서 본 차량 실내 지도 (피그마 'Car Top View' 컴포넌트와 같은 좌표계 520×656).
 *
 * @param looks 수납함별 표시 상태. 없으면 NORMAL
 * @param items 뒷좌석·트렁크 칸 안에 두 번째 줄로 보여줄 물건 이름
 * @param onTap 칸을 눌렀을 때(물건 태그 화면)
 */
@Composable
fun CarMap(
    looks: Map<Compartment, AreaLook>,
    modifier: Modifier = Modifier,
    items: Map<Compartment, String> = emptyMap(),
    showCallouts: Boolean = true,
    onTap: ((Compartment) -> Unit)? = null,
) {
    val measurer = rememberTextMeasurer()
    val blink by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "blinkValue",
    )
    var m = modifier.aspectRatio(CarGeometry.WIDTH / CarGeometry.HEIGHT)
    if (onTap != null) {
        m = m.pointerInput(onTap) {
            detectTapGestures { pos ->
                val s = size.width / CarGeometry.WIDTH
                CarGeometry.hit(Offset(pos.x / s, pos.y / s))?.let(onTap)
            }
        }
    }
    Canvas(m) {
        val s = size.width / CarGeometry.WIDTH
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            drawCarBody(measurer)
            Compartment.entries.forEach { c ->
                drawArea(c, looks[c] ?: AreaLook.NORMAL, items[c], blink, measurer)
            }
            if (showCallouts) {
                CarGeometry.callouts.forEach { co -> drawCallout(co, looks[co.compartment] ?: AreaLook.NORMAL, measurer) }
            }
        }
    }
}

internal class AreaGeom(val rect: Rect, val radius: Float, val bottomRadius: Float = radius, val label: String? = null)
internal class Callout(val compartment: Compartment, val label: String, val left: Boolean, val cy: Float, val tx: Float, val ty: Float)

internal object CarGeometry {
    const val WIDTH = 520f
    const val HEIGHT = 656f

    private fun r(x: Float, y: Float, w: Float, h: Float) = Rect(x, y, x + w, y + h)

    val areas: Map<Compartment, AreaGeom> = mapOf(
        Compartment.DASHBOARD to AreaGeom(r(142f, 180f, 40f, 22f), 5f),
        Compartment.GLOVEBOX to AreaGeom(r(274f, 178f, 104f, 28f), 6f),
        Compartment.CONSOLE_FRONT to AreaGeom(r(245f, 216f, 30f, 26f), 6f),
        Compartment.CUP_HOLDER to AreaGeom(r(245f, 250f, 30f, 44f), 8f),
        Compartment.ARMREST to AreaGeom(r(245f, 302f, 30f, 74f), 8f),
        Compartment.DRIVER_DOOR_POCKET to AreaGeom(r(126f, 252f, 14f, 108f), 7f),
        Compartment.PASSENGER_DOOR_POCKET to AreaGeom(r(380f, 252f, 14f, 108f), 7f),
        Compartment.REAR_LEFT_DOOR_POCKET to AreaGeom(r(126f, 408f, 14f, 100f), 7f),
        Compartment.REAR_RIGHT_DOOR_POCKET to AreaGeom(r(380f, 408f, 14f, 100f), 7f),
        Compartment.REAR_LEFT_SEAT to AreaGeom(r(148f, 404f, 108f, 104f), 18f, label = "뒷좌석 왼쪽"),
        Compartment.REAR_RIGHT_SEAT to AreaGeom(r(264f, 404f, 108f, 104f), 18f, label = "뒷좌석 오른쪽"),
        Compartment.TRUNK to AreaGeom(r(142f, 562f, 236f, 62f), 14f, bottomRadius = 40f, label = "트렁크"),
    )

    val callouts = listOf(
        Callout(Compartment.DASHBOARD, "대시보드 수납함", true, 150f, 162f, 191f),
        Callout(Compartment.CONSOLE_FRONT, "콘솔 하단 수납함", true, 214f, 260f, 229f),
        Callout(Compartment.DRIVER_DOOR_POCKET, "운전석 도어 수납함", true, 306f, 133f, 306f),
        Callout(Compartment.REAR_LEFT_DOOR_POCKET, "뒷좌석 도어 수납함", true, 458f, 133f, 458f),
        Callout(Compartment.GLOVEBOX, "글로브박스", false, 150f, 340f, 192f),
        Callout(Compartment.CUP_HOLDER, "컵홀더", false, 230f, 260f, 272f),
        Callout(Compartment.PASSENGER_DOOR_POCKET, "조수석 도어 수납함", false, 306f, 387f, 306f),
        Callout(Compartment.ARMREST, "암레스트 수납함", false, 382f, 260f, 339f),
        Callout(Compartment.REAR_RIGHT_DOOR_POCKET, "뒷좌석 도어 수납함", false, 458f, 387f, 458f),
    )

    /** 좁은 도어 포켓도 누르기 쉽게 터치 영역을 넓혀서 판정 */
    fun hit(p: Offset): Compartment? =
        areas.entries.firstOrNull { (_, g) -> g.rect.inflate(10f).contains(p) }?.key
}

private data class LookColors(val fill: Color, val stroke: Color, val width: Float, val text: Color)

private fun lookColors(look: AreaLook, blink: Float): LookColors = when (look) {
    AreaLook.NORMAL -> LookColors(OnColors.Surface, OnColors.Stroke, 1.5f, OnColors.Ink2)
    AreaLook.WARN -> LookColors(OnColors.OrangeBg, OnColors.Orange, 2.5f, OnColors.OrangeText)
    AreaLook.ACTIVE -> LookColors(OnColors.BlueBg, OnColors.Blue, 3f, OnColors.Blue)
    AreaLook.OK -> LookColors(OnColors.BlueBg, OnColors.Blue, 2f, OnColors.Blue)
    AreaLook.BLINK -> LookColors(lerp(OnColors.OrangeBg, OnColors.OrangeStrong, blink), OnColors.Orange, 3f, OnColors.OrangeText)
}

private fun Path.quadTo(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float) {
    // 2차 곡선을 3차 곡선으로 변환 (API 버전 차이 없이 쓰기 위해)
    cubicTo(
        x0 + 2f / 3f * (x1 - x0), y0 + 2f / 3f * (y1 - y0),
        x2 + 2f / 3f * (x1 - x2), y2 + 2f / 3f * (y1 - y2),
        x2, y2,
    )
}

private fun DrawScope.drawCarBody(measurer: TextMeasurer) {
    val wheel = OnColors.Line2
    listOf(112f to 100f, 396f to 100f, 112f to 470f, 396f to 470f).forEach { (x, y) ->
        drawRoundRect(wheel, Offset(x, y), Size(12f, 64f), CornerRadius(6f))
    }
    listOf(94f, 398f).forEach { x ->
        drawOval(OnColors.Surface, Offset(x, 167f), Size(28f, 16f))
        drawOval(OnColors.Line2, Offset(x, 167f), Size(28f, 16f), style = Stroke(1.5f))
    }
    val body = Path().apply {
        moveTo(220f, 20f); lineTo(300f, 20f)
        quadTo(300f, 20f, 400f, 20f, 400f, 120f)
        lineTo(400f, 576f)
        quadTo(400f, 576f, 400f, 636f, 340f, 636f)
        lineTo(180f, 636f)
        quadTo(180f, 636f, 120f, 636f, 120f, 576f)
        lineTo(120f, 120f)
        quadTo(120f, 120f, 120f, 20f, 220f, 20f)
        close()
    }
    drawPath(body, OnColors.Surface)
    drawPath(body, OnColors.Stroke, style = Stroke(2f))

    val front = measurer.measure("차량 앞쪽", TextStyle(fontSize = 11f.toSp(), color = OnColors.Stroke))
    drawText(front, topLeft = Offset(260f - front.size.width / 2f, 60f))

    val windshield = Path().apply { moveTo(140f, 170f); lineTo(380f, 170f); lineTo(365f, 122f); lineTo(155f, 122f); close() }
    drawPath(windshield, OnColors.Fill); drawPath(windshield, OnColors.Line2, style = Stroke(1.5f))
    val rear = Path().apply { moveTo(145f, 536f); lineTo(375f, 536f); lineTo(365f, 554f); lineTo(155f, 554f); close() }
    drawPath(rear, OnColors.Fill); drawPath(rear, OnColors.Line2, style = Stroke(1.5f))

    drawRoundRect(OnColors.Fill, Offset(136f, 172f), Size(248f, 40f), CornerRadius(8f))
    drawRoundRect(OnColors.Line, Offset(136f, 172f), Size(248f, 40f), CornerRadius(8f), style = Stroke(1.5f))

    listOf(148f to "운전석", 278f to "조수석").forEach { (x, label) ->
        drawRoundRect(OnColors.Fill, Offset(x, 250f), Size(94f, 108f), CornerRadius(18f))
        drawRoundRect(OnColors.Line, Offset(x, 250f), Size(94f, 108f), CornerRadius(18f), style = Stroke(1.5f))
        drawRoundRect(OnColors.Fill2, Offset(x + 6f, 332f), Size(82f, 20f), CornerRadius(10f))
        val t = measurer.measure(label, TextStyle(fontSize = 12f.toSp(), color = OnColors.Stroke, fontWeight = FontWeight.Medium))
        drawText(t, topLeft = Offset(x + 47f - t.size.width / 2f, 282f))
    }
    drawCircle(OnColors.Stroke, 24f, Offset(195f, 230f), style = Stroke(3f))
    drawRoundRect(OnColors.Fill2, Offset(148f, 514f), Size(224f, 14f), CornerRadius(7f))
}

private fun DrawScope.drawArea(c: Compartment, look: AreaLook, item: String?, blink: Float, measurer: TextMeasurer) {
    val g = CarGeometry.areas[c] ?: return
    val col = lookColors(look, blink)
    val shape = Path().apply {
        addRoundRect(
            RoundRect(
                g.rect,
                topLeft = CornerRadius(g.radius), topRight = CornerRadius(g.radius),
                bottomRight = CornerRadius(g.bottomRadius), bottomLeft = CornerRadius(g.bottomRadius),
            )
        )
    }
    if (look == AreaLook.BLINK) {
        // 빛 번짐 효과
        drawRoundRect(
            OnColors.Orange.copy(alpha = 0.25f * blink),
            g.rect.topLeft - Offset(6f, 6f), Size(g.rect.width + 12f, g.rect.height + 12f),
            CornerRadius(g.radius + 6f),
        )
    }
    drawPath(shape, col.fill)
    drawPath(shape, col.stroke, style = Stroke(col.width))

    if (c == Compartment.CUP_HOLDER) {
        drawCircle(col.stroke, 8f, Offset(g.rect.center.x, g.rect.top + 13f), style = Stroke(1.5f))
        drawCircle(col.stroke, 8f, Offset(g.rect.center.x, g.rect.bottom - 13f), style = Stroke(1.5f))
    }
    val label = g.label ?: return
    val lines = listOfNotNull(label, item)
    val layouts = lines.mapIndexed { i, s ->
        measurer.measure(
            s,
            TextStyle(
                fontSize = (if (i == 0) 11f else 13f).toSp(),
                fontWeight = FontWeight.Bold,
                color = if (i == 0 && look == AreaLook.NORMAL) OnColors.Ink2 else col.text,
            ),
        )
    }
    val totalH = layouts.sumOf { it.size.height } + 4f * (layouts.size - 1)
    var y = g.rect.center.y - totalH / 2f
    layouts.forEach { l ->
        drawText(l, topLeft = Offset(g.rect.center.x - l.size.width / 2f, y))
        y += l.size.height + 4f
    }
}

private fun DrawScope.drawCallout(co: Callout, look: AreaLook, measurer: TextMeasurer) {
    val highlighted = look != AreaLook.NORMAL
    val color = when (look) {
        AreaLook.WARN, AreaLook.BLINK -> OnColors.Orange
        AreaLook.ACTIVE, AreaLook.OK -> OnColors.Blue
        AreaLook.NORMAL -> OnColors.Ink2
    }
    val bg = when (look) {
        AreaLook.WARN, AreaLook.BLINK -> OnColors.OrangeBg
        AreaLook.ACTIVE, AreaLook.OK -> OnColors.BlueBg
        AreaLook.NORMAL -> OnColors.Surface
    }
    val text = measurer.measure(co.label, TextStyle(fontSize = 11f.toSp(), fontWeight = FontWeight.Bold, color = color))
    val w = text.size.width + 20f
    val h = text.size.height + 10f
    val x = if (co.left) maxOf(0f, 108f - w) else 412f
    val top = co.cy - h / 2f
    val lineStartX = if (co.left) x + w else x
    drawLine(if (highlighted) color else OnColors.Line2, Offset(lineStartX, co.cy), Offset(co.tx, co.ty), strokeWidth = 1f)
    drawCircle(color, 4f, Offset(co.tx, co.ty))
    drawRoundRect(bg, Offset(x, top), Size(w, h), CornerRadius(8f))
    drawRoundRect(if (highlighted) color else OnColors.Line2, Offset(x, top), Size(w, h), CornerRadius(8f), style = Stroke(if (highlighted) 1.5f else 1f))
    drawText(text, topLeft = Offset(x + 10f, top + 5f))
}
