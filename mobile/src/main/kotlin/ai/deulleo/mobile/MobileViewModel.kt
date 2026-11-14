package ai.deulleo.mobile

import ai.deulleo.core.DeulleoAgent
import ai.deulleo.core.SampleDay
import ai.deulleo.core.model.ErrandKind
import ai.deulleo.core.model.GeoPoint
import ai.deulleo.core.model.Plan
import ai.deulleo.core.planner.HaversineEstimator
import ai.deulleo.core.planner.SamplePlaceCatalog
import ai.deulleo.core.vehicle.GearState
import ai.deulleo.core.vehicle.VehicleSnapshot
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class Tab { CALENDAR, HOME, PROFILE }

/** 홈 타임라인의 카드 한 장. */
data class StopCard(
    val id: String,
    val time: String,
    val title: String,
    val detail: String,
    val kind: ErrandKind,
    /** 이전 지점에서 이 카드까지 차로 걸리는 시간(분) */
    val legMinutes: Int,
    val location: GeoPoint,
)

/** 취향 기반 "가는 길에" 추천. */
data class Recommendation(val item: String, val title: String, val detail: String)

data class MobileState(
    val tab: Tab = Tab.HOME,
    val showMap: Boolean = false,
    val showAddSheet: Boolean = false,
    val showMenu: Boolean = false,
    val cards: List<StopCard> = emptyList(),
    val homeArrive: String = "",
    val firstDepart: String = "",
    val totalDriveMinutes: Int = 0,
    val message: String = "",
    val recommendation: Recommendation? = null,
    // 메뉴
    val recommendOn: Boolean = true,
    val carConnected: Boolean = true,
    // 내 정보
    val homeAddress: String = "서울 광진구 ○○로 12",
    val workAddress: String = "서울 성동구 ○○길 45",
    val favorites: List<String> = listOf("들러헤어 성수점", "푸른수학학원", "자양 마트"),
    val drivingStyles: Set<String> = setOf("주차 쉬운 곳 우선", "일정 사이 여유 10분"),
    val tastes: Set<String> = setOf("말차", "베이커리"),
    val home: GeoPoint = SampleDay.home,
)

val ALL_DRIVING_STYLES = listOf("주차 쉬운 곳 우선", "빠른 길 우선", "일정 사이 여유 10분", "브리핑 짧게")
val ALL_TASTES = listOf("말차", "베이커리", "드라이브스루", "조용한 카페")

/**
 * 폰 앱의 상태. 계획은 차량 앱과 같은 core 모듈의 [DeulleoAgent]가 세웁니다.
 * 프로토타입은 시연용 하루(토요일 14:10)를 쓰고, 다음 단계에서 캘린더·스크린샷 인식·서버 동기화로 바꿉니다.
 */
class MobileViewModel : ViewModel() {

    private val catalog = SamplePlaceCatalog()
    private var agent = DeulleoAgent(SampleDay.home, catalog)
    private val travel = HaversineEstimator()
    private val now = SampleDay.START_AT
    private val here = SampleDay.home
    private var recoDismissed = false

    private val _state = MutableStateFlow(MobileState())
    val state: StateFlow<MobileState> = _state.asStateFlow()

    init { rebuildDay() }

    // ---- 화면 이동 ----
    fun selectTab(tab: Tab) = _state.update { it.copy(tab = tab, showMap = false) }
    fun openMap(open: Boolean) = _state.update { it.copy(showMap = open) }
    fun openAddSheet(open: Boolean) = _state.update { it.copy(showAddSheet = open) }
    fun openMenu(open: Boolean) = _state.update { it.copy(showMenu = open) }

    // ---- 일정 ----
    fun addItem(text: String) {
        if (text.isBlank()) return
        val reply = agent.addItem(text, now, here)
        _state.update { it.copy(showAddSheet = false) }
        publish(reply.plan, reply.speech)
    }

    fun remove(id: String) {
        val reply = agent.removeById(id, now, here)
        publish(reply.plan, reply.speech)
    }

    fun acceptRecommendation() {
        val reco = _state.value.recommendation ?: return
        recoDismissed = true
        val reply = agent.addItem(reco.item, now, here)
        publish(reply.plan, reply.speech)
    }

    fun dismissRecommendation() {
        recoDismissed = true
        _state.update { it.copy(recommendation = null) }
    }

    // ---- 메뉴 · 내 정보 ----
    fun setRecommendOn(on: Boolean) {
        _state.update { it.copy(recommendOn = on) }
        refreshRecommendation()
    }

    /** 차량이 연결되면 차량 상태(시연: 타이어 저압 경고)가 일정에 반영됩니다. */
    fun setCarConnected(on: Boolean) {
        _state.update { it.copy(carConnected = on) }
        rebuildDay()
    }

    fun toggleDrivingStyle(s: String) = _state.update {
        it.copy(drivingStyles = if (s in it.drivingStyles) it.drivingStyles - s else it.drivingStyles + s)
    }

    fun toggleTaste(t: String) {
        _state.update { it.copy(tastes = if (t in it.tastes) it.tastes - t else it.tastes + t) }
        refreshRecommendation()
    }

    // ---- 내부 ----
    private fun rebuildDay() {
        agent = DeulleoAgent(SampleDay.home, catalog)
        agent.setErrands(SampleDay.errands(catalog))
        val vehicle = VehicleSnapshot(gear = GearState.PARK, lowTirePressure = _state.value.carConnected)
        val reply = agent.briefing(now, here, vehicle)
        publish(reply.plan, "")
    }

    private fun publish(plan: Plan?, message: String) {
        val p = plan ?: agent.plan
        if (p == null) {
            _state.update { it.copy(message = message) }
            return
        }
        var prev = here
        var drive = 0
        val cards = p.stops.map { stop ->
            val e = stop.errand
            val leg = travel.minutes(prev, e.place.location)
            drive += leg
            prev = e.place.location
            val begin = stop.departAt - e.stayMinutes
            val parking = e.place.parking?.takeIf { it.note.isNotBlank() }?.let { pk -> "${pk.name} · ${pk.note}" }
            val detail = when (e.kind) {
                ErrandKind.VEHICLE_CARE -> e.place.name
                else -> listOfNotNull(e.place.name.takeIf { it != e.title }, parking).joinToString(" · ")
            }
            StopCard(e.id, clock(begin), e.title, detail, e.kind, leg, e.place.location)
        }
        drive += travel.minutes(prev, SampleDay.home)
        _state.update {
            it.copy(cards = cards, homeArrive = clock(p.homeArriveAt), firstDepart = clock(now), totalDriveMinutes = drive, message = message)
        }
        refreshRecommendation()
    }

    private fun refreshRecommendation() {
        val s = _state.value
        val hasCafe = agent.remainingErrands.any { it.place.category == "카페" }
        val show = s.recommendOn && !recoDismissed && "말차" in s.tastes && !hasCafe && s.cards.isNotEmpty()
        val cafe = catalog.search("말차", SampleDay.home, 1).firstOrNull()
        _state.update {
            it.copy(
                recommendation = if (show && cafe != null) {
                    Recommendation("말차", "가는 길에 ${cafe.name}", "말차 라떼 · 평점 ${cafe.rating ?: "-"}")
                } else null,
            )
        }
    }

    private fun clock(m: Int): String = "%d:%02d".format(m / 60, m % 60)
}
