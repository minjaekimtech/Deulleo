package ai.deulleo.mobile.ui

import ai.deulleo.mobile.ALL_DRIVING_STYLES
import ai.deulleo.mobile.ALL_TASTES
import ai.deulleo.mobile.MobileState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

/** 시연용: 오늘(11/14) 외의 날짜에 들어 있는 일정 */
private val SAMPLE_EVENTS: Map<Int, List<Pair<String, String>>> = mapOf(
    3 to listOf("16:30" to "지우 학원 하원"),
    5 to listOf("16:30" to "지우 학원 하원"),
    10 to listOf("16:30" to "지우 학원 하원"),
    12 to listOf("16:30" to "지우 학원 하원", "19:00" to "세탁물 찾기"),
    17 to listOf("16:30" to "지우 학원 하원"),
    19 to listOf("16:30" to "지우 학원 하원"),
    21 to listOf("11:00" to "엄마 생신 모임"),
    24 to listOf("16:30" to "지우 학원 하원"),
    26 to listOf("16:30" to "지우 학원 하원"),
)
private const val TODAY = 14

/** ④ 캘린더: 월간 달력 + 선택한 날의 일정 */
@Composable
fun CalendarScreen(state: MobileState, onMenu: () -> Unit) {
    var selected by remember { mutableIntStateOf(TODAY) }
    val month = YearMonth.of(2026, 11)
    val firstOffset = LocalDate.of(2026, 11, 1).dayOfWeek.value % 7 // 일요일 = 0
    val days = month.lengthOfMonth()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 120.dp),
    ) {
        ScreenHeader("캘린더", "일정이 있는 날에 점이 표시돼요", onMenu)
        VSpace(18)
        DCard {
            Text("2026년 11월", color = DColor.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach {
                    Text(it, color = DColor.Sub, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val cells = firstOffset + days
            val rows = (cells + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    for (c in 0 until 7) {
                        val day = r * 7 + c - firstOffset + 1
                        Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                            if (day in 1..days) {
                                val isSel = day == selected
                                val hasEvent = day == TODAY || SAMPLE_EVENTS.containsKey(day)
                                val circle = Modifier.size(36.dp).clip(CircleShape)
                                Box(
                                    (if (isSel) circle.background(AccentBrush) else circle).clickable { selected = day },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        day.toString(),
                                        color = if (isSel) DColor.Ink else DColor.Text,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Normal,
                                    )
                                }
                                if (hasEvent && !isSel) {
                                    Box(Modifier.align(Alignment.BottomCenter).size(4.dp).clip(CircleShape).background(DColor.Lav))
                                }
                            }
                        }
                    }
                }
            }
        }

        SectionLabel("11월 ${selected}일")
        val events: List<Pair<String, String>> =
            if (selected == TODAY) state.cards.map { it.time to it.title } else SAMPLE_EVENTS[selected].orEmpty()
        if (events.isEmpty()) {
            Text("이 날은 일정이 없어요.", color = DColor.Sub, fontSize = 14.sp)
        }
        events.forEach { (time, title) ->
            DCard(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(time, color = DColor.Ice, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
                    Text(title, color = DColor.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** ⑤ 내 정보: 자주 가는 곳 · 이동 성향 · 취향 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    state: MobileState,
    onMenu: () -> Unit,
    onToggleStyle: (String) -> Unit,
    onToggleTaste: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 120.dp),
    ) {
        ScreenHeader("내 정보", "들러가 추천할 때 참고해요", onMenu)

        SectionLabel("자주 가는 곳")
        PlaceRow("집", state.homeAddress)
        PlaceRow("회사", state.workAddress)
        state.favorites.forEachIndexed { i, name ->
            DCard(Modifier.padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).border(1.5.dp, DColor.Lav, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text((i + 1).toString(), color = DColor.Lav, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Text(name, color = DColor.Text, fontSize = 15.sp, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }

        SectionLabel("이동 성향")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ALL_DRIVING_STYLES.forEach { s -> Chip(s, s in state.drivingStyles) { onToggleStyle(s) } }
        }

        SectionLabel("취향 · 가는 길 추천에 쓰여요")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ALL_TASTES.forEach { t -> Chip(t, t in state.tastes) { onToggleTaste(t) } }
        }
    }
}

@Composable
private fun PlaceRow(label: String, value: String) {
    DCard(Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.width(48.dp))
            Text(value, color = DColor.Text, fontSize = 15.sp)
        }
    }
}

/** 메뉴(≡): 차량 연결, 가는 길 추천 켜기/끄기 */
@Composable
fun MenuSheet(
    state: MobileState,
    onRecommend: (Boolean) -> Unit,
    onCar: (Boolean) -> Unit,
) {
    Text("메뉴", color = DColor.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp))
    ToggleRow("가는 길 추천", "취향에 맞는 곳을 동선 사이에 제안해요", state.recommendOn, onRecommend)
    ToggleRow(
        "차량 연결",
        if (state.carConnected) "연결됨 · 차량 경고가 일정에 반영돼요" else "연결 안 됨",
        state.carConnected, onCar,
    )
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    DCard(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = DColor.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(desc, color = DColor.Sub, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = checked, onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = DColor.Lav, checkedThumbColor = DColor.Ink),
            )
        }
    }
}

/** 화면 아래에서 올라오는 시트(일정 추가·메뉴 공용). 바깥을 누르면 닫힙니다. */
@Composable
fun BottomSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(DColor.Ink.copy(alpha = 0.6f)).clickable(onClick = onDismiss)) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(DColor.Sheet)
                .clickable(enabled = false) {}
                .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 30.dp),
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                    .size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(4.dp)).background(DColor.Line),
            )
            content()
        }
    }
}
