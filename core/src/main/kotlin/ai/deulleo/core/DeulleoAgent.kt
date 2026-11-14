package ai.deulleo.core

import ai.deulleo.core.brief.Josa
import ai.deulleo.core.model.Errand
import ai.deulleo.core.model.ErrandKind
import ai.deulleo.core.model.GeoPoint
import ai.deulleo.core.model.MinuteOfDay
import ai.deulleo.core.model.Place
import ai.deulleo.core.model.Plan
import ai.deulleo.core.model.toClock
import ai.deulleo.core.planner.HaversineEstimator
import ai.deulleo.core.planner.ItemCategories
import ai.deulleo.core.planner.PlaceCatalog
import ai.deulleo.core.planner.RoutePlanner
import ai.deulleo.core.planner.TravelTimeEstimator
import ai.deulleo.core.vehicle.VehicleCareRules
import ai.deulleo.core.vehicle.VehicleSnapshot
import ai.deulleo.core.voice.IntentParser
import ai.deulleo.core.voice.RuleBasedIntentParser
import ai.deulleo.core.voice.VoiceIntent

/** 에이전트가 앱에 요청하는 동작. */
sealed interface AgentAction {
    /** 계획대로 내비게이션 안내를 시작(또는 갱신)합니다. */
    data object StartNavigation : AgentAction
    data object None : AgentAction
}

/** 태울 사람(아이·부모님)의 폰으로 보내는 알림. 예: "엄마 4분 뒤 후문 도착" */
data class CompanionNotice(val to: String, val text: String)

/** 에이전트 응답: 음성으로 읽을 문장 + 갱신된 계획 + 앱이 할 동작 + (선택) 동승 예정자 알림. */
data class AgentReply(
    val speech: String,
    val plan: Plan?,
    val action: AgentAction = AgentAction.None,
    val notice: CompanionNotice? = null,
)

/**
 * 들러 에이전트의 두뇌.
 *
 * 오늘의 일정·할 일과 차량 상태를 받아 계획을 세우고([briefing]), 주행 중 발화로 계획을 고칩니다([onUtterance]).
 * 안드로이드·Pleos SDK에 의존하지 않는 순수 Kotlin이라 JVM에서 단위 테스트할 수 있습니다.
 * 서비스 단계에서는 [parser]를 LLM 도구 호출로, [catalog]를 카카오 로컬 API로 교체합니다.
 */
class DeulleoAgent(
    private val home: GeoPoint,
    private val catalog: PlaceCatalog,
    private val planner: RoutePlanner = RoutePlanner(),
    private val parser: IntentParser = RuleBasedIntentParser(),
    private val homeBy: MinuteOfDay? = null,
    private val travel: TravelTimeEstimator = HaversineEstimator(),
) {
    /** 픽업은 주차 없이 승하차 지점에 잠깐 서므로 주차 여유 시간을 1분만 둡니다. */
    private val curbTravel: TravelTimeEstimator = HaversineEstimator(parkingOverheadMin = 1)

    private val errands = mutableListOf<Errand>()
    private var careAdded = false

    var plan: Plan? = null
        private set

    val remainingErrands: List<Errand> get() = errands.toList()

    fun setErrands(list: List<Errand>) {
        errands.clear()
        errands += list
        careAdded = false
    }

    /** 탑승 브리핑: 차량 관리 할 일을 더하고 계획을 세운 뒤 제안 문장을 만듭니다. */
    fun briefing(now: MinuteOfDay, here: GeoPoint, vehicle: VehicleSnapshot): AgentReply {
        val careNotes = mutableListOf<String>()
        if (!careAdded) {
            for (need in VehicleCareRules.needsFrom(vehicle)) {
                val anchor = errands.firstOrNull { it.kind == ErrandKind.FIXED }?.place?.location ?: here
                val place = catalog.search(need.placeCategory, anchor, 1).firstOrNull() ?: continue
                errands += Errand("care-${need.name}", need.title, ErrandKind.VEHICLE_CARE, place, need.stayMinutes)
                careNotes += need.title
            }
            careAdded = true
        }
        val p = replan(now, here) ?: return AgentReply("오늘 볼일을 모두 시간 안에 처리하기 어려워요. 일정을 확인해 주세요.", null)

        val sb = StringBuilder()
        errands.firstOrNull { it.kind == ErrandKind.FIXED }?.let {
            sb.append("오늘 ${it.fixedStart!!.toClock()} ${it.title} 일정이 있어요. ")
        }
        errands.firstOrNull { it.kind == ErrandKind.PICKUP }?.let {
            sb.append("${it.fixedStart!!.toClock()}에 ${Josa.iGa(it.title)} 있어요. ")
            it.waitSpot?.let { w -> sb.append("일찍 도착하면 도로 대신 ${w.name}에서 기다리시면 돼요. ") }
        }
        if (careNotes.isNotEmpty()) {
            sb.append("차량 점검이 필요해서 ${Josa.eulReul(careNotes.joinToString(", "))} 경로에 넣었어요. ")
        }
        val perishable = p.stops.filter { it.errand.perishable }
        if (perishable.isNotEmpty() && perishable.last() == p.stops.last()) {
            sb.append("${Josa.eunNeun(perishable.last().errand.title)} 차에 오래 두지 않도록 마지막에 넣었어요. ")
        }
        p.nextStop?.let { first ->
            sb.append("첫 목적지 ${first.errand.place.name}에 ${first.arriveAt.toClock()} 도착 예정이에요")
            first.errand.place.parking?.takeIf { it.note.isNotBlank() }?.let { pk -> sb.append(", 주차는 ${pk.name}(${pk.note})") }
            sb.append(". ")
        }
        sb.append("집에는 ${p.homeArriveAt.toClock()}쯤 도착해요. 이대로 갈까요?")
        return AgentReply(sb.toString(), p)
    }

    /** 발화 처리. */
    fun onUtterance(text: String, now: MinuteOfDay, here: GeoPoint): AgentReply =
        when (val intent = parser.parse(text)) {
            VoiceIntent.Confirm -> AgentReply("좋아요, 안내를 시작할게요.", plan, AgentAction.StartNavigation)
            VoiceIntent.Reject -> AgentReply("알겠어요. 바꾸고 싶은 걸 말씀해 주세요.", plan)
            VoiceIntent.NextStop -> {
                val next = plan?.nextStop
                if (next == null) AgentReply("남은 볼일이 없어요. 집으로 안내할게요.", plan)
                else AgentReply("다음은 ${next.errand.place.name}, ${next.arriveAt.toClock()} 도착 예정이에요.", plan)
            }
            is VoiceIntent.AddErrand -> addErrand(intent.item, now, here)
            is VoiceIntent.RemoveErrand -> removeErrand(intent.item, now, here)
            is VoiceIntent.Delay -> delay(intent.minutes, now, here)
            VoiceIntent.CompanionReady -> onCompanionReady(now, here)
            is VoiceIntent.Unknown -> AgentReply("죄송해요, 다시 한번 말씀해 주세요.", plan)
        }

    /** 현재(첫 번째) 볼일을 마쳤을 때 호출합니다. 예: 볼일 장소에서 출발(P→D) 감지 */
    fun completeCurrentStop(): Errand? {
        val done = plan?.nextStop?.errand ?: return null
        errands.removeAll { it.id == done.id }
        plan = plan?.let { it.copy(stops = it.stops.drop(1)) }
        return done
    }

    /** 화면에서 품목을 직접 추가합니다(음성 "…도 사야 해"와 같은 동작). */
    fun addItem(item: String, now: MinuteOfDay, here: GeoPoint): AgentReply = addErrand(item.trim(), now, here)

    /** 이미 장소와 시각이 정해진 일정을 넣습니다(예: 예약 문자에서 인식한 일정). */
    fun addErrandDirect(errand: Errand, now: MinuteOfDay, here: GeoPoint): AgentReply {
        if (errands.any { it.id == errand.id }) return AgentReply("", plan)
        val trial = planner.plan(here, now, home, errands + errand, homeBy)
            ?: return AgentReply("${Josa.eulReul(errand.title)} 넣으면 시간이 맞지 않아요.", plan)
        errands += errand
        plan = trial
        val moved = rebalance(now, here)
        return AgentReply(listOfNotNull("${Josa.eulReul(errand.title)} 동선에 넣었어요.", moved).joinToString(" "), plan)
    }

    /**
     * 일정이 바뀐 뒤, 가게가 정해지지 않은 할 일을 더 가까운 같은 업종 가게로 옮기면 귀가가 빨라지는지 봅니다.
     * 8분 이상 빨라질 때만 옮기고, 무엇을 왜 바꿨는지 문장으로 돌려줍니다.
     */
    fun rebalance(now: MinuteOfDay, here: GeoPoint): String? {
        val current = plan ?: return null
        var best: Triple<Int, Place, Plan>? = null
        errands.forEachIndexed { i, e ->
            if (e.kind != ErrandKind.FLEXIBLE) return@forEachIndexed
            for (anchor in errands.filter { it.id != e.id }) {
                for (alt in catalog.search(e.place.category, anchor.place.location, 2)) {
                    if (alt.id == e.place.id) continue
                    val trial = errands.toMutableList().also { it[i] = e.copy(place = alt) }
                    val p = planner.plan(here, now, home, trial, homeBy) ?: continue
                    if (p.homeArriveAt <= current.homeArriveAt - 8 && (best == null || p.homeArriveAt < best!!.third.homeArriveAt)) {
                        best = Triple(i, alt, p)
                    }
                }
            }
        }
        val (i, alt, p) = best ?: return null
        val moved = errands[i]
        errands[i] = moved.copy(place = alt)
        plan = p
        val neighbor = errands.firstOrNull {
            it.id != moved.id && HaversineEstimator.haversineKm(it.place.location, alt.location) < 0.3
        }
        return if (neighbor != null) "${Josa.eunNeun(moved.title)} ${neighbor.place.name} 옆 ${Josa.euroRo(alt.name)} 옮겼어요. 주차는 한 번이면 돼요."
        else "${Josa.eunNeun(moved.title)} 더 가까운 ${Josa.euroRo(alt.name)} 옮겼어요."
    }

    /** 가게를 [newPlace]로 바꾸면 계획이 어떻게 되는지 미리 봅니다(실제로 바꾸지는 않음). */
    fun previewSwap(errandId: String, newPlace: Place, now: MinuteOfDay, here: GeoPoint): Plan? {
        val i = errands.indexOfFirst { it.id == errandId }
        if (i < 0) return null
        return planner.plan(here, now, home, errands.toMutableList().also { it[i] = it[i].copy(place = newPlace) }, homeBy)
    }

    /** [place]에 [stayMinutes]분 들르면 계획이 어떻게 되는지 미리 봅니다(실제로 넣지는 않음). */
    fun previewVisit(place: Place, stayMinutes: Int, now: MinuteOfDay, here: GeoPoint): Plan? =
        planner.plan(here, now, home, errands + Errand("preview", place.name, ErrandKind.FLEXIBLE, place, stayMinutes), homeBy)

    /** 같은 볼일을 다른 가게에서 하도록 바꿉니다. */
    fun swapPlace(errandId: String, newPlace: Place, now: MinuteOfDay, here: GeoPoint): AgentReply {
        val i = errands.indexOfFirst { it.id == errandId }
        if (i < 0) return AgentReply("", plan)
        val trial = errands.toMutableList().also { it[i] = it[i].copy(place = newPlace) }
        val p = planner.plan(here, now, home, trial, homeBy)
            ?: return AgentReply("${Josa.euroRo(newPlace.name)} 바꾸면 시간이 맞지 않아요.", plan)
        errands.clear(); errands += trial
        plan = p
        return AgentReply("${Josa.euroRo(newPlace.name)} 바꿨어요.", plan)
    }

    /** 화면에서 일정을 지웁니다. */
    fun removeById(id: String, now: MinuteOfDay, here: GeoPoint): AgentReply {
        val removed = errands.filter { it.id == id }
        if (removed.isEmpty()) return AgentReply("", plan)
        errands.removeAll(removed)
        val p = replan(now, here)
        return AgentReply("${Josa.eulReul(removed.first().title)} 뺐어요.", p)
    }

    // ---- 만남(픽업) ----

    /**
     * 픽업 장소 근처에 일찍 도착했을 때의 안내.
     * 학원 앞 도로에서 기다리는 대신 대기 지점으로 보내고, 남은 시간이 충분하면 근처 볼일을 제안합니다.
     */
    fun waitingAdvice(now: MinuteOfDay, here: GeoPoint): AgentReply {
        val pickup = plan?.nextStop?.errand?.takeIf { it.kind == ErrandKind.PICKUP }
            ?: return AgentReply("지금 기다릴 픽업 일정이 없어요.", plan)
        val left = pickup.fixedStart!! - now
        if (left <= 3) return AgentReply("곧 끝날 시간이에요. ${pickup.place.parking?.name ?: pickup.place.name}로 가시면 돼요.", plan)

        val sb = StringBuilder("끝나려면 ${left}분 남았어요. ")
        val spot = pickup.waitSpot
        if (spot != null) {
            sb.append("${pickup.place.name} 앞 도로 대신 ${spot.name}(차로 ${curbTravel.minutes(here, spot.location)}분)에서 기다리세요. ")
            val back = curbTravel.minutes(spot.location, pickup.place.location)
            val errand = errands.firstOrNull {
                it.kind == ErrandKind.FLEXIBLE &&
                    HaversineEstimator.haversineKm(it.place.location, spot.location) < 0.5 &&
                    it.stayMinutes + back + 5 <= left
            }
            if (errand != null) sb.append("그 사이 ${errand.place.name}에서 ${Josa.eulReul(errand.title.removeSuffix(" 사기"))} 하실 시간도 돼요. ")
        }
        val who = pickup.companion ?: "상대방"
        sb.append("${Josa.iGa(who)} 끝났다고 알리면 바로 출발 안내할게요.")
        return AgentReply(sb.toString(), plan)
    }

    /** 태울 사람이 준비됐다는 신호(아이 폰의 "끝났어" 버튼, 학원 하원 알림, 또는 운전자의 "끝났대"). */
    fun onCompanionReady(now: MinuteOfDay, here: GeoPoint): AgentReply {
        val pickup = errands.firstOrNull { it.kind == ErrandKind.PICKUP }
            ?: return AgentReply("기다리는 픽업 일정이 없어요.", plan)
        val minutes = curbTravel.minutes(here, pickup.place.location)
        val point = pickup.place.parking?.name ?: pickup.place.name
        val who = pickup.companion ?: "상대방"
        return AgentReply(
            speech = "${Josa.iGa(who)} 준비됐대요. 지금 출발하면 ${minutes}분 뒤 $point 도착이에요. ${who}에게도 알렸어요.",
            plan = plan,
            action = AgentAction.StartNavigation,
            notice = CompanionNotice(who, "${minutes}분 뒤 $point 도착해요. 그쪽에서 기다려 줘. (${(now + minutes).toClock()} 도착 예정)"),
        )
    }

    // ---- 내부 동작 ----

    private fun addErrand(item: String, now: MinuteOfDay, here: GeoPoint): AgentReply {
        val anchor = plan?.stops?.lastOrNull()?.errand?.place?.location ?: here
        val candidates = catalog.search(item, anchor, 3)
        if (candidates.isEmpty()) return AgentReply("주변에서 $item 파는 곳을 찾지 못했어요.", plan)

        // 후보마다 계획을 세워 보고 가장 점수가 좋은 곳을 고릅니다.
        var bestPlace: Place? = null
        var bestPlan: Plan? = null
        for (place in candidates) {
            val e = Errand("add-${place.id}", "$item 사기", ErrandKind.FLEXIBLE, place, 10, perishable = ItemCategories.isPerishable(item))
            val p = planner.plan(here, now, home, errands + e, homeBy) ?: continue
            if (bestPlan == null || p.score < bestPlan.score) { bestPlan = p; bestPlace = place }
        }
        if (bestPlace == null || bestPlan == null) return AgentReply("$item 사는 곳을 넣으면 시간이 맞지 않아요.", plan)

        val before = plan?.homeArriveAt
        errands += Errand("add-${bestPlace.id}", "$item 사기", ErrandKind.FLEXIBLE, bestPlace, 10, perishable = ItemCategories.isPerishable(item))
        plan = bestPlan

        val neighbor = errands.firstOrNull {
            it.place.id != bestPlace.id && HaversineEstimator.haversineKm(it.place.location, bestPlace.location) < 0.3
        }
        val sb = StringBuilder()
        if (neighbor != null) sb.append("${neighbor.place.category} 근처 ${Josa.eulReul(bestPlace.name)} 넣었어요. 주차는 한 번만 하시면 돼요. ")
        else sb.append("${Josa.eulReul(bestPlace.name)} 경로에 넣었어요. ")
        sb.append("집 도착은 ${bestPlan.homeArriveAt.toClock()}")
        if (before != null && bestPlan.homeArriveAt > before) sb.append("(${bestPlan.homeArriveAt - before}분 늦어짐)")
        sb.append("이에요.")
        return AgentReply(sb.toString(), plan, AgentAction.StartNavigation)
    }

    private fun removeErrand(item: String, now: MinuteOfDay, here: GeoPoint): AgentReply {
        val removed = errands.filter { it.title.contains(item) || it.place.category.contains(item) || it.place.name.contains(item) }
        if (removed.isEmpty()) return AgentReply("'$item'에 해당하는 볼일이 없어요.", plan)
        errands.removeAll(removed)
        val p = replan(now, here)
        return AgentReply("${Josa.eulReul(removed.joinToString { it.title })} 뺐어요. 집 도착은 ${p?.homeArriveAt?.toClock() ?: "-"}이에요.", p, AgentAction.StartNavigation)
    }

    private fun delay(minutes: Int, now: MinuteOfDay, here: GeoPoint): AgentReply {
        val start = now + minutes
        val beforeIds = errands.map { it.place.id }.toSet()
        val p = replan(start, here, allowSubstitutes = true)
            ?: return AgentReply("${minutes}분 늦어지면 남은 볼일을 모두 하기 어려워요. 뺄 볼일을 말씀해 주세요.", plan)
        val swapped = errands.filter { it.place.id !in beforeIds }
        val sb = StringBuilder("${minutes}분 늦어지는 걸로 다시 계산했어요. ")
        swapped.forEach { sb.append("원래 가게는 마감 시간이 맞지 않아 ${Josa.euroRo(it.place.name)} 바꿨어요. ") }
        sb.append("집 도착은 ${p.homeArriveAt.toClock()} 예정이에요.")
        return AgentReply(sb.toString(), p, AgentAction.StartNavigation)
    }

    /**
     * 계획을 다시 세웁니다. [allowSubstitutes]가 true이고 계획이 불가능하면,
     * FLEXIBLE 볼일의 가게를 같은 업종의 다른 가게로 바꿔 가며 가능한 계획을 찾습니다.
     */
    private fun replan(now: MinuteOfDay, here: GeoPoint, allowSubstitutes: Boolean = true): Plan? {
        planner.plan(here, now, home, errands, homeBy)?.let { plan = it; return it }
        if (!allowSubstitutes) return null

        var best: Pair<List<Errand>, Plan>? = null
        val flexibleIdx = errands.indices.filter { errands[it].kind == ErrandKind.FLEXIBLE }
        for (i in flexibleIdx) {
            val e = errands[i]
            for (alt in catalog.search(e.place.category, e.place.location, 5)) {
                if (alt.id == e.place.id) continue
                val trial = errands.toMutableList().also { it[i] = e.copy(id = "sub-${alt.id}", place = alt) }
                val p = planner.plan(here, now, home, trial, homeBy) ?: continue
                if (best == null || p.score < best.second.score) best = trial to p
            }
        }
        val found = best ?: return null
        errands.clear(); errands += found.first
        plan = found.second
        return found.second
    }
}
