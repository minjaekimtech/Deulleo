package ai.deulleo.mobile.ui

import ai.deulleo.mobile.MobileState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
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

        // 차량 디스플레이(가로 화면) 미리보기: 브리핑 앞부분과 말하는 중 표시
        val shape = RoundedCornerShape(12.dp)
        Column(
            Modifier.padding(top = 20.dp).fillMaxWidth().aspectRatio(16f / 9f).clip(shape).background(Color.Black)
                .border(1.dp, DColor.Line, shape).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("들러 · 탑승 브리핑", color = DColor.Sub, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("정차 중(P)", color = DColor.Sub, fontSize = 11.sp)
            }
            val firstTwo = state.briefing.split(". ").take(3).joinToString(". ").let { if (it.endsWith(".") || it.endsWith("?")) it else "$it." }
            Text(firstTwo, color = DColor.Text, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp).weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 말하는 중 표시(음성 파형)
                Row(Modifier.height(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(6, 12, 18, 9, 15, 7, 13, 5).forEach { h ->
                        Box(Modifier.padding(end = 3.dp).width(2.5.dp).height(h.dp).background(DColor.Acc, RoundedCornerShape(2.dp)))
                    }
                }
                Text("\"응\"이라고 답하면 안내를 시작해요", color = DColor.Sub, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Text(
            "달리는 동안에는 다음 목적지와 도착 시각만 크게 보이고, 바꿀 일이 생기면 \"케이크도 사야 해\"처럼 말하면 돼요.",
            color = DColor.Sub, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 18.dp),
        )

        Spacer(Modifier.padding(top = 24.dp))
        PrimaryButton("확인", Modifier.fillMaxWidth(), onDone)
    }
}

