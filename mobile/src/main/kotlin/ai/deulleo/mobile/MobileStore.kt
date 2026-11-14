package ai.deulleo.mobile

import ai.deulleo.core.DeulleoAgent
import ai.deulleo.core.SampleDay
import ai.deulleo.core.SamplePickupDay
import ai.deulleo.core.model.Errand
import ai.deulleo.core.model.ErrandKind
import ai.deulleo.core.model.GeoPoint
import ai.deulleo.core.model.Plan
import ai.deulleo.core.planner.HaversineEstimator
import ai.deulleo.core.planner.SamplePlaceCatalog
import ai.deulleo.core.vehicle.GearState
import ai.deulleo.core.vehicle.VehicleSnapshot
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Tab { CALENDAR, HOME, PROFILE }

/** 탭 위에 뜨는 화면·시트 */
enum class Overlay { NONE, ADD, SCREENSHOT, DETAIL, MAP, HANDOFF }

/** 홈 시간표의 한 줄 */
data class StopRow(
    val id: String,
    val start: String,
    val end: String,
    val title: String,
    val placeName: String,
    val address: String,
    val hours: String,
    val parking: String?,
    val walkMinutes: Int,
    /** 이 순서·이 가게인 이유. 에이전트가 무엇을 고려했는지 한 줄로 보여 줍니다. */
    val reason: String?,
    /** 일정이 어디서 왔는지: 캘린더, 문자, 메모, 차량 ... */
    val source: String,
    val kind: ErrandKind,
    val legMinutes: Int,
    val slackMinutes: Int,
    val location: GeoPoint,
    val category: String,
    val changed: Boolean,
)

/** 차량이 알려 준 상태(시연: 타이어 공기압) */
data class VehicleNotice(val title: String, val detail: String, val included: Boolean)

data class Alternative(val placeId: String, val name: String, val detail: String)

data class Recommendation(val item: String, val title: String, val detail: String)

data class MobileState(
    val tab: Tab = Tab.HOME,
    val overlay: Overlay = Overlay.NONE,
    val day: Int = TODAY,
    val dateLabel: String = "",
    val leaveAt: String = "",
    val headlineSub: String = "",
    val rows: List<StopRow> = emptyList(),
    val homeLegMinutes: Int = 0,
    val homeArrive: String = "",
    /** 계획이 바뀐 뒤 무엇이 달라졌는지 */
    val notice: String = "",
    val vehicle: VehicleNotice? = null,
    val recommendation: Recommendation? = null,
    val detailId: String? = null,
    val alternatives: List<Alternative> = emptyList(),
    /** 차에 타면 들리게 될 브리핑 */
    val briefing: String = "",
    val recommendOn: Boolean = true,
    val carConnected: Boolean = true,
    val tastes: Set<String> = setOf("말차", "베이커리"),
    val homeAddress: String = "서울 광진구 ○○로 12",
    val workAddress: String = "서울 성동구 ○○길 45",
    val favorites: List<String> = listOf("들러헤어 성수점", "푸른수학학원", "자양 마트"),
    val home: GeoPoint = SampleDay.home,
) {
    val detail: StopRow? get() = rows.firstOrNull { it.id == detailId }
    val isToday: Boolean get() = day == TODAY
}

const val TODAY = 14
val PICKUP_DAYS = setOf(3, 5, 10, 12, 17, 19, 24, 26)
val ALL_TASTES = listOf("말차", "베이커리", "드라이브스루", "조용한 카페")
private val WEEKDAYS = listOf("일", "월", "화", "수", "목", "금", "토")

/** 2026년 11월 [day]일의 요일. 11월 1일이 일요일입니다. */
fun weekdayOf(day: Int): String = WEEKDAYS[(day - 1) % 7]

/**
 * 폰 앱의 상태와 동작. 안드로이드에 의존하지 않아 데스크톱에서도 컴파일·렌더링해 검증합니다.
 * 계획은 차량 앱과 같은 core 모듈의 [DeulleoAgent]가 세웁니다.
 * 시연용으로 11월 14일(주말 볼일)과 화·목요일(학원 하원 픽업) 두 가지 하루를 제공합니다.
 */
class MobileStore {

    var state by mutableStateOf(MobileState())
        private set

    private val catalog = SamplePlaceCatalog()
    private val travel = HaversineEstimator()
    private var agent = DeulleoAgent(SampleDay.home, catalog)
    private val here = SampleDay.home
    private var startAt = SampleDay.START_AT
    private val sources = mutableMapOf<String, String>()
    private var recoDismissed = false
    private var careIncluded = true

    init { loadDay(TODAY) }

    // ---- 화면 이동 ----
    fun selectTab(tab: Tab) { state = state.copy(tab = tab, overlay = Overlay.NONE) }
    fun open(overlay: Overlay) { state = state.copy(overlay = overlay) }
    fun close() { state = state.copy(overlay = Overlay.NONE, detailId = null) }

    /** 뒤로 가기: 한 단계 앞 화면으로 */
    fun back() {
        state = when (state.overlay) {
            Overlay.HANDOFF -> state.copy(overlay = Overlay.MAP)
            Overlay.SCREENSHOT -> state.copy(overlay = Overlay.ADD)
            else -> state.copy(overlay = Overlay.NONE, detailId = null)
        }
    }

    fun openDetail(id: String) {
        val errand = agent.remainingErrands.firstOrNull { it.id == id } ?: return
        val alts = if (errand.kind == ErrandKind.FLEXIBLE) {
            catalog.search(errand.place.category, errand.place.location, 4)
                .filter { it.id != errand.place.id }
                .map { p ->
                    Alternative(p.id, p.name, listOfNotNull(hours(p.opensAt, p.closesAt), p.rating?.let { "평점 $it" }).joinToString(" · "))
                }
        } else emptyList()
        state = state.copy(overlay = Overlay.DETAIL, detailId = id, alternatives = alts)
    }

    /** 캘린더에서 날짜를 골라 그날의 동선을 봅니다. */
    fun selectDay(day: Int) {
        loadDay(day)
        state = state.copy(tab = Tab.HOME, overlay = Overlay.NONE)
    }

    // ---- 일정 바꾸기 ----
    fun addItem(text: String) {
        if (text.isBlank()) return
        val before = agent.plan
        val reply = agent.addItem(text, startAt, here)
        val added = agent.remainingErrands.lastOrNull()?.id
        added?.let { sources.getOrPut(it) { "직접 입력" } }
        state = state.copy(overlay = Overlay.NONE)
        publish(diff(before, reply.plan, reply.speech), added)
    }

    /** 예약 문자 스크린샷에서 인식한 일정을 넣습니다(시연: 17:00 케이크 찾기). */
    fun addFromScreenshot() {
        val before = agent.plan
        val bakery = catalog.places.first { it.id == "bakery-1" }
        val errand = Errand(SHOT_ID, "케이크 찾기", ErrandKind.FIXED, bakery, stayMinutes = 10, fixedStart = 17 * 60)
        sources[SHOT_ID] = "문자"
        val reply = agent.addErrandDirect(errand, startAt, here)
        state = state.copy(overlay = Overlay.NONE)
        publish(diff(before, reply.plan, reply.speech), SHOT_ID)
    }

    fun remove(id: String) {
        val before = agent.plan
        if (id == CARE_ID) careIncluded = false
        val reply = agent.removeById(id, startAt, here)
        state = state.copy(overlay = Overlay.NONE, detailId = null)
        publish(diff(before, reply.plan, reply.speech), null)
    }

    /** 뺐던 차량 점검을 다시 넣습니다. */
    fun restoreCare() {
        val before = agent.plan
        careIncluded = true
        val garage = catalog.places.first { it.id == "garage-1" }
        val reply = agent.addErrandDirect(Errand(CARE_ID, "타이어 공기압 점검", ErrandKind.VEHICLE_CARE, garage, 30), startAt, here)
        publish(diff(before, reply.plan, reply.speech), CARE_ID)
    }

    fun swapTo(placeId: String) {
        val id = state.detailId ?: return
        val place = catalog.places.firstOrNull { it.id == placeId } ?: return
        val before = agent.plan
        val reply = agent.swapPlace(id, place, startAt, here)
        state = state.copy(overlay = Overlay.NONE, detailId = null)
        publish(diff(before, reply.plan, reply.speech), id)
    }

    fun acceptRecommendation() {
        val reco = state.recommendation ?: return
        recoDismissed = true
        val before = agent.plan
        val reply = agent.addItem(reco.item, startAt, here)
        val added = agent.remainingErrands.lastOrNull()?.id
        added?.let { sources[it] = "추천" }
        publish(diff(before, reply.plan, reply.speech), added)
    }

    fun dismissRecommendation() {
        recoDismissed = true
        state = state.copy(recommendation = null)
    }

    // ---- 설정 ----
    fun setRecommendOn(on: Boolean) {
        state = state.copy(recommendOn = on)
        publish(state.notice, null)
    }

    fun setCarConnected(on: Boolean) {
        state = state.copy(carConnected = on)
        loadDay(state.day)
    }

    fun toggleTaste(t: String) {
        state = state.copy(tastes = if (t in state.tastes) state.tastes - t else state.tastes + t)
        publish(state.notice, null)
    }

    // ---- 내부 ----
    private fun loadDay(day: Int) {
        val pickup = day in PICKUP_DAYS
        sources.clear()
        recoDismissed = false
        careIncluded = true
        val errands = when {
            day == TODAY -> SampleDay.errands(catalog).also { sources["hair"] = "캘린더"; sources["flower"] = "메모" }
            pickup -> SamplePickupDay.errands(catalog).also { sources["pickup"] = "시간표"; sources["mart"] = "메모" }
            else -> emptyList()
        }
        sources[CARE_ID] = "차량"
        val base = if (pickup) SamplePickupDay.START_AT else SampleDay.START_AT
        // 차량 경고는 오늘만 반영합니다(오늘 아침 감지).
        val vehicle = VehicleSnapshot(gear = GearState.PARK, lowTirePressure = state.carConnected && day == TODAY)

        fun planFrom(start: Int): String {
            agent = DeulleoAgent(SampleDay.home, catalog)
            agent.setErrands(errands)
            startAt = start
            return agent.briefing(start, here, vehicle).speech
        }
        var briefing = planFrom(base)
        // 예약 전에 남는 시간이 있으면 그만큼 늦게 출발하도록 다시 계획합니다(5분 단위).
        val slack = agent.plan?.stops?.let { stops ->
            val firstFixed = stops.indexOfFirst { it.errand.kind == ErrandKind.FIXED || it.errand.kind == ErrandKind.PICKUP }
            if (firstFixed < 0) 0 else stops.take(firstFixed + 1).sumOf { (it.departAt - it.errand.stayMinutes) - it.arriveAt }
        } ?: 0
        if (slack >= 5) briefing = planFrom(base + slack / 5 * 5)

        state = state.copy(day = day, dateLabel = "11월 ${day}일 ${weekdayOf(day)}요일", briefing = briefing)
        publish("", null)
    }

    private fun publish(notice: String, changedId: String?) {
        val plan = agent.plan
        val stops = plan?.stops.orEmpty()
        var prev = here
        val rows = stops.mapIndexed { i, stop ->
            val e = stop.errand
            val leg = travel.minutes(prev, e.place.location)
            prev = e.place.location
            val begin = stop.departAt - e.stayMinutes
            StopRow(
                id = e.id,
                start = clock(begin),
                end = clock(stop.departAt),
                title = e.title,
                placeName = e.place.name,
                address = e.place.address.removeSuffix(" (가상)"),
                hours = hours(e.place.opensAt, e.place.closesAt),
                // 정비소처럼 따로 주차할 필요가 없는 곳은 주차 정보를 보여 주지 않습니다.
                parking = e.place.parking?.takeIf { it.note.isNotBlank() || it.walkMinutes > 0 }?.let { pk ->
                    listOfNotNull(pk.name, pk.note.takeIf { it.isNotBlank() }).joinToString(" · ")
                },
                walkMinutes = e.place.parking?.walkMinutes ?: 0,
                reason = reasonFor(i, stops.map { it.errand }, stop.departAt, begin - stop.arriveAt),
                source = sources[e.id] ?: "직접 입력",
                kind = e.kind,
                legMinutes = leg,
                slackMinutes = begin - stop.arriveAt,
                location = e.place.location,
                category = e.place.category,
                changed = e.id == changedId,
            )
        }
        val first = stops.firstOrNull { it.errand.kind == ErrandKind.FIXED || it.errand.kind == ErrandKind.PICKUP }
        val sub = when {
            rows.isEmpty() -> ""
            first != null -> "${clock(first.errand.fixedStart!!)} ${first.errand.title}에 맞췄어요"
            else -> "${rows.size}곳을 들러요"
        }
        val careInPlan = stops.any { it.errand.id == CARE_ID }
        val vehicle = if (state.carConnected && state.day == TODAY) {
            VehicleNotice(
                title = "뒤 왼쪽 타이어 29psi",
                detail = if (careInPlan) "권장 35psi · 오늘 아침 차가 감지해서 가는 길 정비소를 넣었어요"
                else "권장 35psi · 오늘 점검은 뺐어요",
                included = careInPlan,
            )
        } else null

        state = state.copy(
            rows = rows,
            leaveAt = clock(startAt),
            headlineSub = sub,
            homeLegMinutes = if (stops.isEmpty()) 0 else travel.minutes(prev, SampleDay.home),
            homeArrive = plan?.let { clock(it.homeArriveAt) } ?: "",
            notice = notice,
            vehicle = vehicle,
            recommendation = recommendation(rows),
        )
    }

    /** 이 일정이 왜 이 자리에 있는지 한 줄로. */
    private fun reasonFor(i: Int, errands: List<Errand>, endAt: Int, slack: Int): String? {
        val e = errands[i]
        fun near(o: Errand?) = o != null && HaversineEstimator.haversineKm(o.place.location, e.place.location) < 0.3
        val next = errands.getOrNull(i + 1)
        // 한 번 주차로 묶이는 것은 둘 다 가게일 때만입니다(정비소·픽업은 제외).
        val shop = { o: Errand? -> o != null && o.kind != ErrandKind.VEHICLE_CARE && o.kind != ErrandKind.PICKUP }
        val neighbor = shop(e) && listOf(errands.getOrNull(i - 1), next).any { shop(it) && near(it) }
        val lastShopping = errands.indexOfLast { it.kind == ErrandKind.FLEXIBLE } == i
        return when {
            e.kind == ErrandKind.VEHICLE_CARE -> "예약 전 빈 시간에 넣었어요"
            e.kind == ErrandKind.PICKUP -> e.waitSpot?.let { "일찍 도착하면 도로 대신 ${it.name}에서 기다려요" }
            e.kind == ErrandKind.FLEXIBLE && next?.kind == ErrandKind.PICKUP && near(next) -> "하원을 기다리는 동안 끝낼 수 있어요"
            neighbor -> "옆 일정과 한 번 주차로 끝나요"
            e.perishable && lastShopping -> "차에 오래 두지 않게 마지막에 들러요"
            e.kind == ErrandKind.FIXED && slack >= 5 -> "${slack}분 일찍 도착해요"
            e.place.closesAt - endAt in 0..45 -> "${clock(e.place.closesAt)} 마감 전에 도착해요"
            e.kind == ErrandKind.FLEXIBLE -> "가는 길에 있는 곳으로 골랐어요"
            else -> null
        }
    }

    private fun recommendation(rows: List<StopRow>): Recommendation? {
        val hasCafe = rows.any { it.category == "카페" }
        if (!state.recommendOn || recoDismissed || "말차" !in state.tastes || hasCafe || rows.isEmpty() || state.day != TODAY) return null
        val cafe = catalog.search("말차", SampleDay.home, 1).firstOrNull() ?: return null
        return Recommendation("말차", "가는 길에 ${cafe.name}", "말차 라떼 · 평점 ${cafe.rating ?: "-"}")
    }

    /** 계획이 바뀌었을 때 귀가 시각이 어떻게 달라졌는지 덧붙입니다. */
    private fun diff(before: Plan?, after: Plan?, speech: String): String {
        val a = before?.homeArriveAt; val b = after?.homeArriveAt
        val change = if (a != null && b != null && a != b) {
            val d = b - a
            "집 도착 ${clock(a)} → ${clock(b)} (${if (d > 0) "+" else ""}${d}분)"
        } else null
        val head = speech.substringBefore(" 집 도착").trim()
        return listOfNotNull(head.takeIf { it.isNotBlank() }, change).joinToString(" ")
    }

    private fun hours(open: Int, close: Int): String =
        if (open == 0 && close == 24 * 60) "24시간" else "${clock(open)}–${clock(close)}"

    private fun clock(m: Int): String = "%d:%02d".format(m / 60, m % 60)

    private companion object {
        const val CARE_ID = "care-TIRE_PRESSURE"
        const val SHOT_ID = "shot-cake"
    }
}
