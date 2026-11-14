package ai.deulleo.mobile.ui

import ai.deulleo.mobile.MobileState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).clip(CircleShape).background(DColor.Bg).clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = DColor.Text, modifier = Modifier.size(20.dp)) }
}

/** 동선 지도: 도로를 따라가는 오늘 경로와 순서 */
@Composable
fun MapScreen(state: MobileState, onBack: () -> Unit, onSendToCar: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            RouteMap(state.home, state.rows, Modifier.fillMaxSize())
            BackButton(onBack, Modifier.padding(start = 18.dp, top = 28.dp))
        }
        Column(Modifier.fillMaxWidth().background(DColor.Bg).padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 26.dp)) {
            Text("${state.leaveAt} 출발 · ${state.homeArrive} 집 도착", color = DColor.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(state.dateLabel, color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
            state.rows.forEachIndexed { i, row ->
                Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    NumberDot((i + 1).toString())
                    Text(row.title, color = DColor.Text, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    Text(row.start, color = DColor.Sub, fontSize = 14.sp)
                }
                Divider()
            }
            PrimaryButton("차에서 이어서 안내", Modifier.padding(top = 16.dp).fillMaxWidth(), onSendToCar)
        }
    }
}

/**
 * 차로 넘기기: 폰에서 확인한 동선이 차에서 어떻게 이어지는지 보여 줍니다.
 * 차량 화면 미리보기의 문장은 차량 앱과 같은 에이전트가 만든 탑승 브리핑입니다.
 */
@Composable
fun HandoffScreen(state: MobileState, onBack: () -> Unit, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 28.dp),
    ) {
        BackButton(onBack, Modifier.background(DColor.Surf, CircleShape))
        Text("그랜저로 보냈어요", color = DColor.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
        Text("차에 타면 들러가 먼저 이렇게 말해요.", color = DColor.Sub, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))

        // 차량 디스플레이 미리보기
        val shape = RoundedCornerShape(14.dp)
        Column(
            Modifier.padding(top = 20.dp).fillMaxWidth().clip(shape).background(DColor.Surf).border(1.dp, DColor.Line, shape).padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("차량 화면 · 탑승 브리핑", color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("P", color = DColor.Sub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(state.briefing, color = DColor.Text, fontSize = 15.sp, lineHeight = 23.sp, modifier = Modifier.padding(top = 10.dp))
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("\"응\" 이라고 답하면 안내 시작", color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(
                    "출발", color = DColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(DColor.Text).padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }

        Step("1", "시동을 걸면", "기어가 P일 때 브리핑이 자동으로 시작돼요.")
        Step("2", "달리는 동안", "화면에는 다음 목적지, 도착 시각, 주차만 크게 보여요.")
        Step("3", "바꾸고 싶을 때", "\"케이크도 사야 해\", \"꽃집은 빼 줘\"처럼 말하면 돼요.")

        Spacer(Modifier.padding(top = 24.dp))
        PrimaryButton("확인", Modifier.fillMaxWidth(), onDone)
    }
}

@Composable
private fun Step(no: String, title: String, body: String) {
    Row(Modifier.padding(top = 18.dp)) {
        NumberDot(no)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = DColor.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = DColor.Sub, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
