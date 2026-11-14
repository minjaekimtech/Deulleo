package ai.deulleo.mobile.ui

import ai.deulleo.core.model.GeoPoint
import ai.deulleo.mobile.StopRow
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos

private val Land = Color(0xFF20252C)
private val Water = Color(0xFF1B2A36)
private val Park = Color(0xFF1F2B27)
private val MinorRoad = Color(0xFF2C323B)
private val MajorRoad = Color(0xFF3A414D)
private val RoadLabel = Color(0xFF6A727F)

private val ROAD_NAMES = listOf(
    "아차산로" to GeoPoint(37.5345, 127.0640),
    "성수이로" to GeoPoint(37.5415, 127.0500),
    "자양로" to GeoPoint(37.5412, 127.0755),
    "광나루로" to GeoPoint(37.5478, 127.0620),
)

/**
 * 오늘의 동선 지도. 집에서 출발해 [stops]를 순서대로 들르고 집으로 돌아오는 경로를 [RoadNet] 도로 위에 그립니다.
 * 색이 있는 요소는 경로 선 하나뿐입니다.
 */
@Composable
fun RouteMap(home: GeoPoint, stops: List<StopRow>, modifier: Modifier = Modifier, showLabels: Boolean = true) {
    val measurer = rememberTextMeasurer()
    val route = remember(home, stops) { RoadNet.route(listOf(home) + stops.map { it.location } + home) }

    Canvas(modifier) {
        drawRect(Land)

        // 경로 전체가 들어오도록 화면을 맞춥니다(위도·경도 비율 유지).
        val focus = if (stops.isEmpty()) listOf(home) else route
        val lngScale = cos(Math.toRadians(home.lat))
        val minLat = focus.minOf { it.lat }; val maxLat = focus.maxOf { it.lat }
        val minLng = focus.minOf { it.lng }; val maxLng = focus.maxOf { it.lng }
        val pad = if (showLabels) 56.dp.toPx() else 28.dp.toPx()
        val spanLat = (maxLat - minLat).coerceAtLeast(0.006)
        val spanLng = ((maxLng - minLng) * lngScale).coerceAtLeast(0.006)
        val scale = minOf((size.height - 2 * pad) / spanLat, (size.width - 2 * pad) / spanLng).toFloat()
        val midLat = (minLat + maxLat) / 2; val midLng = (minLng + maxLng) / 2
        fun project(p: GeoPoint) = Offset(
            x = size.width / 2 + ((p.lng - midLng) * lngScale).toFloat() * scale,
            y = size.height / 2 - (p.lat - midLat).toFloat() * scale,
        )

        // 한강(남쪽)과 공원
        val river = Path().apply {
            val left = project(GeoPoint(37.5318, RoadNet.MIN_LNG - 0.02))
            moveTo(left.x, left.y)
            val c1 = project(GeoPoint(37.5300, 127.0560)); val c2 = project(GeoPoint(37.5345, 127.0700))
            val right = project(GeoPoint(37.5322, RoadNet.MAX_LNG + 0.02))
            cubicTo(c1.x, c1.y, c2.x, c2.y, right.x, right.y)
            lineTo(right.x, size.height + 400f); lineTo(left.x, size.height + 400f); close()
        }
        drawPath(river, Water)
        val parkA = project(GeoPoint(37.5505, 127.0775)); val parkB = project(GeoPoint(37.5470, 127.0850))
        drawRoundRect(Park, parkA, Size(parkB.x - parkA.x, parkB.y - parkA.y), CornerRadius(10.dp.toPx()))

        // 도로
        val soften = PathEffect.cornerPathEffect(6.dp.toPx())
        for (major in listOf(false, true)) {
            val color = if (major) MajorRoad else MinorRoad
            val width = (if (major) 7.dp else 3.5.dp).toPx()
            RoadNet.roads.filter { it.major == major }.forEach { road ->
                drawLine(color, project(RoadNet.nodes[road.a]), project(RoadNet.nodes[road.b]), width, StrokeCap.Round)
            }
        }
        if (showLabels) {
            ROAD_NAMES.forEach { (name, at) ->
                drawText(measurer, name, project(at), TextStyle(color = RoadLabel, fontSize = 10.sp))
            }
        }

        // 경로
        if (stops.isNotEmpty()) {
            val path = Path().apply {
                val first = project(route.first())
                moveTo(first.x, first.y)
                route.drop(1).forEach { p -> project(p).let { lineTo(it.x, it.y) } }
            }
            drawPath(path, DColor.Ink, style = Stroke(9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = soften))
            drawPath(path, DColor.Acc, style = Stroke(4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = soften))
        }

        // 집
        val h = project(home)
        val hs = 11.dp.toPx()
        drawRoundRect(DColor.Ink, Offset(h.x - hs - 2.dp.toPx(), h.y - hs - 2.dp.toPx()), Size(2 * hs + 4.dp.toPx(), 2 * hs + 4.dp.toPx()), CornerRadius(7.dp.toPx()))
        drawRoundRect(DColor.Text, Offset(h.x - hs, h.y - hs), Size(2 * hs, 2 * hs), CornerRadius(5.dp.toPx()))
        centered(measurer, "집", h, TextStyle(color = DColor.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold))

        // 번호 핀과 이름
        stops.forEachIndexed { i, stop ->
            val c = project(stop.location)
            drawCircle(DColor.Ink, 13.dp.toPx(), c)
            drawCircle(DColor.Text, 11.dp.toPx(), c)
            centered(measurer, (i + 1).toString(), c, TextStyle(color = DColor.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            if (showLabels) label(measurer, "${stop.placeName}  ${stop.start}", c, above = i % 2 == 1)
        }
    }
}

private fun DrawScope.centered(measurer: TextMeasurer, text: String, at: Offset, style: TextStyle) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
}

/** 핀 옆에 붙는 이름표. 화면 밖으로 나가지 않게 좌우를 고르고, 이웃 핀과 겹치지 않게 위아래를 번갈아 둡니다. */
private fun DrawScope.label(measurer: TextMeasurer, text: String, pin: Offset, above: Boolean) {
    val layout = measurer.measure(text, TextStyle(color = DColor.Text, fontSize = 11.sp, fontWeight = FontWeight.Medium))
    val padX = 7.dp.toPx(); val padY = 4.dp.toPx()
    val w = layout.size.width + 2 * padX; val hgt = layout.size.height + 2 * padY
    var x = pin.x + 16.dp.toPx()
    if (x + w > size.width - 6.dp.toPx()) x = pin.x - 16.dp.toPx() - w
    val y = if (above) pin.y - hgt - 6.dp.toPx() else pin.y + 6.dp.toPx()
    drawRoundRect(DColor.Ink.copy(alpha = 0.88f), Offset(x, y), Size(w, hgt), CornerRadius(6.dp.toPx()))
    drawText(layout, topLeft = Offset(x + padX, y + padY))
}
