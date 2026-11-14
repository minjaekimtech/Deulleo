package ai.deulleo.mobile.ui

import ai.deulleo.core.model.GeoPoint
import ai.deulleo.mobile.StopCard
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

private val Land = Color(0xFF2A3552)
private val Block = Color(0xFF313D5E)
private val MinorRoad = Color(0xFF3E4A6C)
private val MajorRoad = Color(0xFF4D5A80)

/**
 * 시연용 동선 지도.
 *
 * 실제 지도 SDK 대신 직접 그립니다. 출발지·목적지 좌표를 화면에 투영하고, 각 지점을 지나는 가로·세로 도로를 깔아
 * 경로가 도로를 따라 꺾이도록 그립니다(직선 연결 아님). 2차 단계에서 카카오맵 SDK + 길찾기 API의 실제 도로 경로로 교체합니다.
 */
@Composable
fun RouteMap(start: GeoPoint, stops: List<StopCard>, modifier: Modifier = Modifier, showLabels: Boolean = true) {
    val points = remember(start, stops) { listOf(start) + stops.map { it.location } }
    Canvas(modifier) {
        drawRect(Land)
        if (points.size < 2) return@Canvas

        val pad = 44.dp.toPx()
        val minLat = points.minOf { it.lat }; val maxLat = points.maxOf { it.lat }
        val minLng = points.minOf { it.lng }; val maxLng = points.maxOf { it.lng }
        val spanLat = (maxLat - minLat).coerceAtLeast(1e-4)
        val spanLng = (maxLng - minLng).coerceAtLeast(1e-4)
        fun project(p: GeoPoint) = Offset(
            x = pad + ((p.lng - minLng) / spanLng).toFloat() * (size.width - 2 * pad),
            y = pad + ((maxLat - p.lat) / spanLat).toFloat() * (size.height - 2 * pad),
        )
        val pts = points.map(::project)

        drawBlocksAndMinorRoads()
        // 각 지점을 지나는 큰 도로
        val major = 11.dp.toPx()
        pts.forEach { p ->
            drawLine(MajorRoad, Offset(0f, p.y), Offset(size.width, p.y), major, StrokeCap.Round)
            drawLine(MajorRoad, Offset(p.x, 0f), Offset(p.x, size.height), major, StrokeCap.Round)
        }

        // 경로: 지점 사이를 가로 → 세로로 꺾어 도로 위를 지나고, 마지막에 출발지(집)로 돌아옵니다.
        val loop = pts + pts.first()
        val route = Path().apply {
            moveTo(loop.first().x, loop.first().y)
            for (i in 1 until loop.size) {
                lineTo(loop[i].x, loop[i - 1].y)
                lineTo(loop[i].x, loop[i].y)
            }
        }
        val brush = Brush.linearGradient(listOf(DColor.Ice, DColor.Lav), Offset(0f, size.height), Offset(size.width, 0f))
        drawPath(route, brush, alpha = 0.28f, style = Stroke(16.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(route, brush, style = Stroke(5.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // 집(출발·도착)
        drawCircle(DColor.Ice.copy(alpha = 0.3f), 14.dp.toPx(), pts.first())
        drawCircle(Color.White, 9.dp.toPx(), pts.first())
        drawCircle(DColor.Ice, 6.dp.toPx(), pts.first())
        if (showLabels) drawLabel("집", pts.first(), 11.dp.toPx())

        // 번호 핀
        stops.forEachIndexed { i, stop ->
            val c = pts[i + 1]
            drawCircle(Color.White, 13.dp.toPx(), c)
            drawCircle(DColor.Lav, 11.dp.toPx(), c)
            drawCentered((i + 1).toString(), c, 13.dp.toPx(), android.graphics.Color.rgb(11, 15, 24))
            if (showLabels) drawLabel("${stop.title.take(8)} ${stop.time}", c, 11.dp.toPx())
        }
    }
}

private fun DrawScope.drawBlocksAndMinorRoads() {
    val cols = 4; val rows = 6
    val cw = size.width / cols; val rh = size.height / rows
    val gap = 9.dp.toPx()
    for (r in 0 until rows) for (c in 0 until cols) {
        if ((r + c) % 3 == 0) {
            drawRoundRect(
                Block, Offset(c * cw + gap, r * rh + gap), Size(cw - 2 * gap, rh - 2 * gap),
                CornerRadius(8.dp.toPx()),
            )
        }
    }
    val minor = 4.dp.toPx()
    for (c in 1 until cols) drawLine(MinorRoad, Offset(c * cw, 0f), Offset(c * cw, size.height), minor)
    for (r in 1 until rows) drawLine(MinorRoad, Offset(0f, r * rh), Offset(size.width, r * rh), minor)
}

private fun DrawScope.drawCentered(text: String, at: Offset, sizePx: Float, argb: Int) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = argb
        textSize = sizePx
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }
    drawContext.canvas.nativeCanvas.drawText(text, at.x, at.y + sizePx * 0.36f, paint)
}

private fun DrawScope.drawLabel(text: String, at: Offset, sizePx: Float) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = sizePx
        isFakeBoldText = true
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.rgb(17, 23, 39))
    }
    val w = paint.measureText(text)
    // 화면 오른쪽 끝에서는 핀 왼쪽에 적습니다.
    val x = if (at.x + 16.dp.toPx() + w > size.width) at.x - 16.dp.toPx() - w else at.x + 16.dp.toPx()
    drawContext.canvas.nativeCanvas.drawText(text, x, at.y + 20.dp.toPx(), paint)
}
