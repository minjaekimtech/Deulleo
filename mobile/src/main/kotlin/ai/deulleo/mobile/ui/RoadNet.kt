package ai.deulleo.mobile.ui

import ai.deulleo.core.model.GeoPoint
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot

/**
 * 시연용 도로망(성수·자양 일대를 본뜬 가상 도로).
 *
 * 실제 지도 SDK 대신, 살짝 비뚤어진 격자에 일부 길을 끊고 대각선 길을 섞어 실제 동네 도로처럼 보이게 만든 그래프입니다.
 * 경로는 이 그래프 위의 최단 경로라서 직선 연결이 아니라 도로를 따라 꺾입니다.
 * 2차 단계에서 카카오맵 SDK와 길찾기 API의 실제 도로 경로로 교체합니다.
 */
object RoadNet {
    const val MIN_LAT = 37.5300; const val MAX_LAT = 37.5540
    const val MIN_LNG = 127.0400; const val MAX_LNG = 127.0900
    private const val COLS = 10
    private const val ROWS = 8
    private val MAJOR_ROWS = setOf(1, 3, 5)
    private val MAJOR_COLS = setOf(1, 4, 7)

    data class Road(val a: Int, val b: Int, val major: Boolean)

    val nodes: List<GeoPoint>
    val roads: List<Road>
    private val adjacency: List<MutableList<Int>>

    private fun hash(a: Int, b: Int, salt: Int): Int {
        var h = a * 73856093 xor b * 19349663 xor salt * 83492791
        h = h xor (h ushr 13); h *= 0x5bd1e995; h = h xor (h ushr 15)
        return abs(h)
    }

    private fun index(c: Int, r: Int) = r * COLS + c

    init {
        val cellLat = (MAX_LAT - MIN_LAT) / (ROWS - 1)
        val cellLng = (MAX_LNG - MIN_LNG) / (COLS - 1)
        val n = ArrayList<GeoPoint>(COLS * ROWS)
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val jx = (hash(c, r, 1) % 1000 / 1000.0 - 0.5) * 0.3
            val jy = (hash(c, r, 2) % 1000 / 1000.0 - 0.5) * 0.3
            // 도로 전체가 살짝 기울어 보이도록 열 위치에 따라 위도를 조금 밀어 줍니다.
            val tilt = (c - COLS / 2) * 0.06
            n += GeoPoint(MIN_LAT + (r + jy + tilt) * cellLat, MIN_LNG + (c + jx) * cellLng)
        }
        nodes = n
        val list = ArrayList<Road>()
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            if (c + 1 < COLS) {
                val major = r in MAJOR_ROWS
                if (major || hash(c, r, 3) % 6 != 0) list += Road(index(c, r), index(c + 1, r), major)
            }
            if (r + 1 < ROWS) {
                val major = c in MAJOR_COLS
                if (major || hash(c, r, 4) % 6 != 0) list += Road(index(c, r), index(c, r + 1), major)
            }
            if (c + 1 < COLS && r + 1 < ROWS && hash(c, r, 5) % 11 == 0) list += Road(index(c, r), index(c + 1, r + 1), false)
        }
        roads = list
        adjacency = List(nodes.size) { mutableListOf<Int>() }
        roads.forEach { adjacency[it.a] += it.b; adjacency[it.b] += it.a }
    }

    private fun dist(a: GeoPoint, b: GeoPoint): Double =
        hypot(a.lat - b.lat, (a.lng - b.lng) * cos(Math.toRadians(a.lat)))

    private fun nearest(p: GeoPoint): Int = nodes.indices.minByOrNull { dist(nodes[it], p) } ?: 0

    private fun shortest(from: Int, to: Int): List<Int> {
        val best = DoubleArray(nodes.size) { Double.MAX_VALUE }
        val prev = IntArray(nodes.size) { -1 }
        val done = BooleanArray(nodes.size)
        best[from] = 0.0
        repeat(nodes.size) {
            var u = -1
            for (i in nodes.indices) if (!done[i] && best[i] < Double.MAX_VALUE && (u < 0 || best[i] < best[u])) u = i
            if (u < 0 || u == to) return@repeat
            done[u] = true
            for (v in adjacency[u]) {
                val d = best[u] + dist(nodes[u], nodes[v])
                if (d < best[v]) { best[v] = d; prev[v] = u }
            }
        }
        if (best[to] == Double.MAX_VALUE) return listOf(from, to)
        val path = ArrayList<Int>()
        var at = to
        while (at >= 0) { path += at; at = prev[at] }
        return path.reversed()
    }

    /** [points]를 순서대로 지나는, 도로를 따라가는 경로. */
    fun route(points: List<GeoPoint>): List<GeoPoint> {
        if (points.size < 2) return points
        val out = ArrayList<GeoPoint>()
        out += points.first()
        for (i in 1 until points.size) {
            shortest(nearest(points[i - 1]), nearest(points[i])).forEach { out += nodes[it] }
            out += points[i]
        }
        return out
    }
}
