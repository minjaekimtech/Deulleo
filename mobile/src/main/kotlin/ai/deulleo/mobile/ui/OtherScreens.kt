package ai.deulleo.mobile.ui

import ai.deulleo.mobile.ALL_TASTES
import ai.deulleo.mobile.MobileState
import ai.deulleo.mobile.PICKUP_DAYS
import ai.deulleo.mobile.TODAY
import ai.deulleo.mobile.weekdayOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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

/** 시연용 달력 내용: 날짜별로 들어 있는 일정 (시각, 이름, 출처) */
private fun eventsOf(day: Int): List<Triple<String, String, String>> = when {
    day == TODAY -> listOf(
        Triple("15:00", "미용실", "캘린더"),
        Triple("18:30", "엄마 생신 저녁", "캘린더"),
        Triple("할 일", "엄마 생신 꽃 사기", "메모"),
    )
    day in PICKUP_DAYS -> listOf(Triple("16:30", "지우 학원 하원", "시간표 · 매주 화·목"), Triple("할 일", "장보기", "메모"))
    else -> emptyList()
}

private const val DAYS_IN_MONTH = 30
private const val FIRST_OFFSET = 0 // 2026년 11월 1일은 일요일

/** 캘린더: 월간 달력에서 날짜를 고르면 그날 들어 있는 일정과 동선으로 가는 버튼이 나옵니다. */
@Composable
fun CalendarScreen(state: MobileState, onOpenDay: (Int) -> Unit) {
    var selected by remember { mutableIntStateOf(state.day) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 110.dp),
    ) {
        TopBar("캘린더")
        Text("2026년 11월", color = DColor.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 18.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach {
                Text(it, color = DColor.Sub, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        val rows = (FIRST_OFFSET + DAYS_IN_MONTH + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - FIRST_OFFSET + 1
                    Box(Modifier.weight(1f).height(42.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..DAYS_IN_MONTH) {
                            val isSel = day == selected
                            val circle = Modifier.size(34.dp).clip(CircleShape)
                            Box(
                                (if (isSel) circle.background(DColor.Text) else circle).clickable { selected = day },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    day.toString(),
                                    color = when { isSel -> DColor.Ink; day < TODAY -> DColor.Sub; else -> DColor.Text },
                                    fontSize = 14.sp,
                                    fontWeight = if (isSel || day == TODAY) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                            if (eventsOf(day).isNotEmpty() && !isSel) {
                                Box(Modifier.align(Alignment.BottomCenter).size(4.dp).clip(CircleShape).background(DColor.Acc))
                            }
                        }
                    }
                }
            }
        }

        SectionLabel("11월 ${selected}일 ${weekdayOf(selected)}요일" + if (selected == TODAY) " · 오늘" else "")
        Divider()
        val events = eventsOf(selected)
        if (events.isEmpty()) {
            Text("들어 있는 일정이 없어요.", color = DColor.Sub, fontSize = 14.sp, modifier = Modifier.padding(top = 14.dp))
        }
        events.forEach { (time, title, source) ->
            Row(Modifier.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(time, color = if (time == "할 일") DColor.Sub else DColor.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(60.dp))
                Text(title, color = DColor.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(source, color = DColor.Sub, fontSize = 12.sp)
            }
            Divider()
        }
        if (selected == TODAY || selected in PICKUP_DAYS) {
            PrimaryButton("이 날 동선 보기", Modifier.padding(top = 18.dp).fillMaxWidth()) { onOpenDay(selected) }
        }
    }
}

/** 내 정보: 자주 가는 곳, 취향, 연결 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    state: MobileState,
    onToggleTaste: (String) -> Unit,
    onRecommend: (Boolean) -> Unit,
    onCar: (Boolean) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 110.dp),
    ) {
        TopBar("내 정보")

        SectionLabel("자주 가는 곳")
        Divider()
        InfoRow("집", state.homeAddress)
        InfoRow("회사", state.workAddress)

        SectionLabel("단골")
        Divider()
        state.favorites.forEach { name ->
            Text(name, color = DColor.Text, fontSize = 15.sp, modifier = Modifier.padding(vertical = 13.dp))
            Divider()
        }

        SectionLabel("취향")
        Text("가는 길에 들를 만한 곳을 고를 때 참고해요.", color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.padding(bottom = 10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ALL_TASTES.forEach { t -> Chip(t, t in state.tastes) { onToggleTaste(t) } }
        }

        SectionLabel("연결")
        Divider()
        ToggleRow("내 차 · 그랜저", if (state.carConnected) "타이어·연료 상태를 일정에 반영해요" else "연결하면 차 상태를 일정에 반영해요", state.carConnected, onCar)
        ToggleRow("가는 길 추천", "취향에 맞는 곳을 동선 사이에 제안해요", state.recommendOn, onRecommend)
        InfoRow("캘린더", "기기 캘린더 · 장소가 있는 일정을 가져와요")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DColor.Sub, fontSize = 14.sp, modifier = Modifier.width(60.dp))
        Text(value, color = DColor.Text, fontSize = 15.sp)
    }
    Divider()
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = DColor.Text, fontSize = 15.sp)
            Text(desc, color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = DColor.Text, checkedThumbColor = DColor.Ink,
                uncheckedTrackColor = DColor.Surf, uncheckedThumbColor = DColor.Sub, uncheckedBorderColor = DColor.Line,
            ),
        )
    }
    Divider()
}
