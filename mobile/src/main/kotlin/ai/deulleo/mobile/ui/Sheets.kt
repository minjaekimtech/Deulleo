package ai.deulleo.mobile.ui

import ai.deulleo.mobile.Alternative
import ai.deulleo.mobile.StopRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** 화면 아래에서 올라오는 시트. 바깥을 누르면 닫힙니다. */
@Composable
fun BottomSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(DColor.Ink.copy(alpha = 0.62f)).clickable(onClick = onDismiss)) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(DColor.Surf)
                .clickable(enabled = false) {}
                .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 28.dp),
        ) { content() }
    }
}

@Composable
private fun SheetTitle(title: String, subtitle: String? = null) {
    Text(title, color = DColor.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    if (subtitle != null) Text(subtitle, color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
    VSpace(12)
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
        Text(label, color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.width(72.dp))
        Text(value, color = DColor.Text, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
    Divider()
}

/** 일정 한 줄을 눌렀을 때: 시간·장소·주차·이유, 그리고 다른 가게로 바꾸기 */
@Composable
fun DetailSheet(row: StopRow, alternatives: List<Alternative>, onSwap: (String) -> Unit, onRemove: () -> Unit) {
    SheetTitle(row.title, "${row.placeName} · ${row.source}에서 가져옴")
    Divider()
    Field("시간", "${row.start}–${row.end}")
    if (row.address.isNotBlank()) Field("주소", row.address)
    Field("영업", row.hours)
    row.parking?.let { Field("주차", it + if (row.walkMinutes > 0) " · 도보 ${row.walkMinutes}분" else "") }
    row.reason?.let { Field("이유", it) }

    if (alternatives.isNotEmpty()) {
        Text("다른 가게로 바꾸기", color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp, bottom = 2.dp))
        alternatives.forEach { alt ->
            Row(
                Modifier.fillMaxWidth().clickable { onSwap(alt.placeId) }.padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(alt.name, color = DColor.Text, fontSize = 15.sp)
                    Text(alt.detail, color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                Text("바꾸기", color = DColor.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Divider()
        }
    }
    Text(
        "오늘 일정에서 빼기", color = DColor.Sub, fontSize = 14.sp,
        modifier = Modifier.padding(top = 14.dp).clickable(onClick = onRemove).padding(vertical = 6.dp),
    )
}

/** 추가: 직접 입력하거나, 예약 문자 스크린샷에서 가져옵니다. */
@Composable
fun AddSheet(onAdd: (String) -> Unit, onScreenshot: () -> Unit) {
    var text by remember { mutableStateOf("") }
    SheetTitle("들를 곳 추가", "살 것이나 가게 종류만 적으면 동선에 맞는 곳을 찾아 넣어요")
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        placeholder = { Text("예: 케이크, 약, 장보기", color = DColor.Sub) },
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton("동선에 넣기", Modifier.padding(top = 10.dp).fillMaxWidth()) {
        onAdd(text)
        text = ""
    }
    Row(
        Modifier.padding(top = 14.dp).fillMaxWidth().clickable(onClick = onScreenshot).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("스크린샷에서 가져오기", color = DColor.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("예약 문자·카톡을 찍어 두면 일정으로 바꿔요", color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text("열기", color = DColor.Text, fontSize = 13.sp)
    }
}

/**
 * 스크린샷 인식(시연): 예약 확정 문자를 읽어 무엇·언제·어디서를 뽑아 보여 주고, 확인하면 동선에 넣습니다.
 * 실제 인식(멀티모달 LLM)은 다음 단계에서 연결하며, 지금은 정해진 샘플 문자로 흐름을 보여 줍니다.
 */
@Composable
fun ScreenshotSheet(onConfirm: () -> Unit, startRecognized: Boolean = false) {
    var recognized by remember { mutableStateOf(startRecognized) }
    LaunchedEffect(Unit) {
        if (!recognized) { delay(1300); recognized = true }
    }
    SheetTitle("스크린샷에서 가져오기", "최근 스크린샷 1장")

    // 샘플 예약 문자
    val shape = RoundedCornerShape(10.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(DColor.Bg).border(1.dp, DColor.Line, shape).padding(14.dp)) {
        Text("모퉁이 베이커리", color = DColor.Sub, fontSize = 12.sp)
        Text(
            "[예약 확정] 홀케이크 1호\n11월 14일(토) 오후 5:00 픽업\n서울 광진구 자양로\n변경은 하루 전까지 가능합니다.",
            color = DColor.Text, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 6.dp),
        )
    }

    if (!recognized) {
        Text("일정을 찾는 중이에요", color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
        LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = DColor.Text, trackColor = DColor.Line)
        VSpace(60)
    } else {
        Text("이렇게 읽었어요", color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp))
        Field("할 일", "케이크 찾기")
        Field("언제", "11월 14일 토요일 17:00")
        Field("어디서", "모퉁이 베이커리")
        PrimaryButton("동선에 넣기", Modifier.padding(top = 16.dp).fillMaxWidth(), onConfirm)
    }
}
