package ai.deulleo.app.ui

import ai.deulleo.app.UiState
import ai.deulleo.app.VoiceState
import ai.deulleo.core.model.ErrandKind
import ai.deulleo.core.model.PlannedStop
import ai.deulleo.core.model.toClock
import ai.deulleo.core.vehicle.GearState
import ai.deulleo.core.vehicle.UiMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 차량 화면. 폰 앱과 같은 절제된 팔레트를 쓰고, Pleos 디자인 가이드의 즉시성(1~2초 안에 파악) 원칙에 맞춰
 * 주행 중에는 다음 목적지·도착 시각·주차 세 가지만 크게 보여 줍니다. 그라데이션·애니메이션·이모지는 쓰지 않습니다.
 */
private object C {
    val Bg = Color(0xFF14171C)
    val Surf = Color(0xFF1C2026)
    val Line = Color(0xFF2C313A)
    val Sub = Color(0xFF8B929E)
    val Soft = Color(0xFFBFC5CF)
    val Text = Color(0xFFF4F5F7)
    val Acc = Color(0xFF7CC4E8)
    val Warn = Color(0xFFE8A87C)
    val Ink = Color(0xFF14171C)
}

/** 화면에서 사용자가 할 수 있는 동작 */
data class UiActions(
    val brief: () -> Unit,
    val startTrip: () -> Unit,
    val listen: () -> Unit,
    val submitText: (String) -> Unit,
    val simulateTire: (Boolean) -> Unit,
    val simulateArrival: () -> Unit,
    val companionReady: () -> Unit,
    val loadScenario: (Boolean) -> Unit,
)

@Composable
fun DeulleoApp(state: UiState, actions: UiActions) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = C.Text, onPrimary = C.Ink, background = C.Bg, surface = C.Surf, onSurface = C.Text, outline = C.Line),
    ) {
        Column(Modifier.fillMaxSize().background(C.Bg).padding(horizontal = 40.dp, vertical = 28.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (state.mode) {
                    UiMode.MANAGE -> ManageScreen(state, actions)
                    UiMode.BRIEFING -> BriefingScreen(state, actions)
                    UiMode.DRIVING -> DrivingScreen(state, actions)
                    UiMode.ARRIVED -> ArrivedScreen(state, actions)
                }
            }
            DemoTools(state, actions)
        }
    }
}

// ---------------- 탑승 브리핑 (시동 직후 · P단): 왼쪽에 오늘 순서, 오른쪽에 들러의 말과 출발 버튼 ----------------
@Composable
private fun BriefingScreen(state: UiState, a: UiActions) {
    val plan = state.plan
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Label("오늘의 동선 · 정차 중(P)")
            if (plan != null) {
                Text(
                    "${plan.startAt.toClock()} 출발 · ${plan.homeArriveAt.toClock()} 집 도착",
                    color = C.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
                )
                Rule()
                plan.stops.forEachIndexed { i, s -> StopLine(i + 1, s) }
            }
        }
        Spacer(Modifier.width(40.dp))
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Label("들러")
            Text(state.lastSpeech, color = C.Text, fontSize = 22.sp, lineHeight = 33.sp, modifier = Modifier.padding(top = 8.dp).weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                VoiceStatus(state, Modifier.weight(1f))
                TalkButton(state.voiceState, a.listen)
                Spacer(Modifier.width(14.dp))
                Box(
                    Modifier.height(64.dp).clip(RoundedCornerShape(12.dp)).background(C.Text).clickable(onClick = a.startTrip).padding(horizontal = 44.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (state.tripStarted) "계속 안내" else "출발", color = C.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun StopLine(n: Int, s: PlannedStop) {
    val begin = s.departAt - s.errand.stayMinutes
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(C.Text), contentAlignment = Alignment.Center) {
            Text("$n", color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Text(begin.toClock(), color = C.Text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 14.dp).width(78.dp))
        Column(Modifier.weight(1f)) {
            Text(s.errand.title, color = C.Text, fontSize = 21.sp)
            val parking = s.errand.place.parking?.takeIf { it.note.isNotBlank() }?.let { " · ${it.name} ${it.note}" } ?: ""
            if (s.errand.place.name != s.errand.title || parking.isNotBlank()) {
                Text(s.errand.place.name + parking, color = C.Sub, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        val kind = when (s.errand.kind) {
            ErrandKind.FIXED -> "예약"; ErrandKind.FLEXIBLE -> "할 일"; ErrandKind.VEHICLE_CARE -> "차량"; ErrandKind.PICKUP -> "픽업"
        }
        Text(kind, color = if (s.errand.kind == ErrandKind.VEHICLE_CARE) C.Warn else C.Sub, fontSize = 15.sp)
    }
    Rule()
}

// ---------------- 주행 모드: 세 가지 정보만. 목록·스크롤·버튼 없음(말하기 제외) ----------------
@Composable
private fun DrivingScreen(state: UiState, a: UiActions) {
    val next = state.plan?.nextStop
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth()) {
            Label("다음 목적지", Modifier.weight(1f))
            val left = state.plan?.stops?.size ?: 0
            Label(if (left == 0) "남은 일정 없음" else "남은 일정 ${left}곳")
        }
        if (next == null) {
            Text("집", color = C.Text, fontSize = 64.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            Text("${state.plan?.homeArriveAt?.toClock() ?: ""} 도착", color = C.Acc, fontSize = 64.sp, fontWeight = FontWeight.Bold)
        } else {
            Text(next.errand.place.name, color = C.Text, fontSize = 64.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            // 도착 시각은 화면에서 색을 쓰는 유일한 정보입니다.
            Text("${next.arriveAt.toClock()} 도착", color = C.Acc, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            // 정비소처럼 따로 주차할 필요가 없는 곳은 주차 줄을 보여 주지 않습니다.
            next.errand.place.parking?.takeIf { it.note.isNotBlank() || it.walkMinutes > 0 }?.let { pk ->
                val walk = if (pk.walkMinutes > 0) " · 걸어서 ${pk.walkMinutes}분" else ""
                val prefix = if (next.errand.kind == ErrandKind.PICKUP) "" else "주차 "
                Text("$prefix${pk.name}${if (pk.note.isNotBlank()) " · ${pk.note}" else ""}$walk", color = C.Soft, fontSize = 26.sp, modifier = Modifier.padding(top = 12.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                VoiceStatus(state)
                // 방금 바뀐 내용은 한 문장으로만 보여 줍니다.
                if (state.tripStarted && state.lastSpeech.isNotBlank() && state.voiceState != VoiceState.LISTENING) {
                    Text(state.lastSpeech, color = C.Sub, fontSize = 20.sp, lineHeight = 28.sp, maxLines = 2, modifier = Modifier.padding(top = 4.dp, end = 24.dp))
                }
            }
            TalkButton(state.voiceState, a.listen)
        }
    }
}

// ---------------- 도착 모드: 주차 후 걸어가는 길 또는 픽업 대기 안내, 다음 일정 ----------------
@Composable
private fun ArrivedScreen(state: UiState, a: UiActions) {
    val stop = state.plan?.nextStop
    val following = state.plan?.stops?.getOrNull(1)
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1.2f).fillMaxHeight()) {
            Label(if (stop?.errand?.kind == ErrandKind.PICKUP) "픽업 대기 · 정차 중(P)" else "도착 · 정차 중(P)")
            Text(stop?.errand?.place?.name ?: "", color = C.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            Text(state.lastSpeech, color = C.Soft, fontSize = 22.sp, lineHeight = 33.sp, modifier = Modifier.padding(top = 14.dp))
            if (state.lastNotice.isNotBlank()) {
                Text("보낸 알림  ${state.lastNotice.removePrefix("→ ")}", color = C.Acc, fontSize = 17.sp, lineHeight = 25.sp, modifier = Modifier.padding(top = 14.dp))
            }
        }
        Spacer(Modifier.width(40.dp))
        Column(Modifier.weight(0.8f).fillMaxHeight()) {
            Label("다음")
            if (following != null) {
                Text(following.errand.place.name, color = C.Text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Text("${following.arriveAt.toClock()} 도착 예정", color = C.Sub, fontSize = 19.sp, modifier = Modifier.padding(top = 2.dp))
            } else {
                Text("집으로", color = C.Text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Text("${state.plan?.homeArriveAt?.toClock() ?: ""} 도착 예정", color = C.Sub, fontSize = 19.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                VoiceStatus(state, Modifier.weight(1f))
                TalkButton(state.voiceState, a.listen)
            }
        }
    }
}

// ---------------- 관리 모드: 아직 계획을 세우지 않았을 때 ----------------
@Composable
private fun ManageScreen(state: UiState, a: UiActions) {
    Column(Modifier.fillMaxSize()) {
        Label(if (state.pickupScenario) "11월 19일 목요일" else "11월 14일 토요일")
        Text("오늘 일정 ${state.errands.size}건", color = C.Text, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
        Rule()
        state.errands.forEach { e ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text(e.fixedStart?.toClock() ?: "할 일", color = if (e.fixedStart != null) C.Text else C.Sub, fontSize = 21.sp, modifier = Modifier.width(92.dp))
                Text(e.title, color = C.Text, fontSize = 21.sp, modifier = Modifier.weight(1f))
                Text(e.place.name, color = C.Sub, fontSize = 17.sp)
            }
            Rule()
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.height(60.dp).clip(RoundedCornerShape(12.dp)).background(C.Text).clickable(onClick = a.brief).padding(horizontal = 36.dp),
            contentAlignment = Alignment.Center,
        ) { Text("오늘 동선 짜기", color = C.Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
    }
}

// ---------------- 공통 요소 ----------------
@Composable
private fun Label(text: String, modifier: Modifier = Modifier) = Text(text, color = C.Sub, fontSize = 16.sp, modifier = modifier)

@Composable
private fun Rule() = Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))

/** 듣는 중·말하는 중 표시. 평소에는 무엇을 말할 수 있는지 한 줄로 알려 줍니다. */
@Composable
private fun VoiceStatus(state: UiState, modifier: Modifier = Modifier) {
    val (text, color) = when (state.voiceState) {
        VoiceState.LISTENING -> (state.partialSpeech.ifBlank { "듣고 있어요" }) to C.Text
        VoiceState.SPEAKING -> "말하는 중" to C.Sub
        VoiceState.IDLE -> "\"케이크도 사야 해\"처럼 말해 보세요" to C.Sub
    }
    Text(text, color = color, fontSize = 18.sp, modifier = modifier)
}

@Composable
private fun TalkButton(voice: VoiceState, onClick: () -> Unit) {
    val listening = voice == VoiceState.LISTENING
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.height(64.dp).clip(shape)
            .background(if (listening) C.Acc else C.Bg)
            .border(1.5.dp, if (listening) C.Acc else C.Sub, shape)
            .clickable(onClick = onClick).padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) { Text(if (listening) "듣는 중" else "말하기", color = if (listening) C.Ink else C.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
}

/**
 * 시연 도구: 에뮬레이터에서 마이크·차량 신호 없이도 흐름을 보여 주기 위한 조작. 정차 중에만, 펼쳤을 때만 보입니다.
 */
@Composable
private fun DemoTools(state: UiState, a: UiActions) {
    if (state.vehicle.gear == GearState.DRIVE || state.vehicle.gear == GearState.REVERSE) return
    var open by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Text(if (open) "시연 도구 닫기" else "시연 도구", color = C.Sub, fontSize = 13.sp, modifier = Modifier.clickable { open = !open }.padding(vertical = 4.dp))
        if (!open) return@Column
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            ToolButton(if (state.vehicle.lowTirePressure) "타이어 경고 끄기" else "타이어 경고 켜기") { a.simulateTire(!state.vehicle.lowTirePressure) }
            ToolButton("도착 처리", a.simulateArrival)
            if (state.pickupScenario) ToolButton("아이: 끝났어", a.companionReady)
            ToolButton(if (state.pickupScenario) "주말 볼일로" else "하교 픽업으로") { a.loadScenario(!state.pickupScenario) }
            Text("기어 ${state.vehicle.gear} · 연료 ${state.vehicle.fuelPercent ?: "-"}%", color = C.Sub, fontSize = 13.sp)
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text, onValueChange = { text = it.take(60) }, singleLine = true,
                placeholder = { Text("말 대신 입력 (예: 케이크도 사야 해)", color = C.Sub) }, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            ToolButton("보내기") { if (text.isNotBlank()) a.submitText(text.trim()); text = "" }
        }
    }
}

@Composable
private fun ToolButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        label, color = C.Text, fontSize = 14.sp,
        modifier = Modifier.padding(end = 8.dp).clip(shape).border(1.dp, C.Line, shape).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
