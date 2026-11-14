package ai.deulleo.mobile.ui

import ai.deulleo.core.model.ErrandKind
import ai.deulleo.mobile.MobileState
import ai.deulleo.mobile.Recommendation
import ai.deulleo.mobile.StopCard
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 홈(오늘): 요약 → 동선 지도 미리보기 → 시간표 형태의 일정 목록 */
@Composable
fun HomeScreen(
    state: MobileState,
    onMenu: () -> Unit,
    onOpenMap: () -> Unit,
    onRemove: (String) -> Unit,
    onAcceptReco: () -> Unit,
    onDismissReco: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 160.dp),
    ) {
        TopBar("11월 14일 토요일", onMenu)

        Text(
            if (state.cards.isEmpty()) "오늘은 일정이 없어요" else "오늘 ${state.cards.size}곳,",
            color = DColor.Text, fontSize = 25.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp),
        )
        if (state.cards.isNotEmpty()) {
            Text("지금 출발하면 여유 있어요", color = DColor.Sub, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }

        Row(Modifier.padding(top = 18.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Metric("이동", "${state.totalDriveMinutes}분")
            Metric("첫 출발", state.firstDepart)
            Metric("집 도착", state.homeArrive)
        }
        Divider()

        // 동선 지도 미리보기
        Box(
            Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenMap),
        ) {
            RouteMap(state.home, state.cards, Modifier.fillMaxSize(), showLabels = false)
            Text(
                "지도 보기", color = DColor.Text, fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DColor.Bg.copy(alpha = 0.85f))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }

        if (state.message.isNotBlank()) {
            Text(state.message, color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp))
        }

        VSpace(8)
        state.cards.forEachIndexed { i, card ->
            if (i > 0) {
                Text(
                    "차로 ${card.legMinutes}분", color = DColor.Sub, fontSize = 12.sp,
                    modifier = Modifier.padding(start = 58.dp, top = 7.dp, bottom = 7.dp),
                )
                Divider()
            }
            StopRow(card, onRemove)
            Divider()
        }

        state.recommendation?.let { SuggestionRow(it, onAcceptReco, onDismissReco) }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, color = DColor.Sub, fontSize = 12.sp)
        Text(value, color = DColor.Text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun StopRow(card: StopCard, onRemove: (String) -> Unit) {
    val kind = when (card.kind) {
        ErrandKind.FIXED -> "예약"
        ErrandKind.FLEXIBLE -> "할 일"
        ErrandKind.VEHICLE_CARE -> "차량 알림"
        ErrandKind.PICKUP -> "픽업"
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.Top) {
        Text(card.time, color = DColor.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(58.dp))
        Column(Modifier.weight(1f)) {
            Text(card.title, color = DColor.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (card.detail.isNotBlank()) {
                Text(card.detail, color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Text(
            kind,
            color = if (card.kind == ErrandKind.VEHICLE_CARE) DColor.Warn else DColor.Sub,
            fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp),
        )
        // 예약은 실수로 지우지 않도록 삭제 버튼을 두지 않습니다.
        if (card.kind != ErrandKind.FIXED) {
            Icon(
                Icons.Filled.Close, contentDescription = "삭제", tint = DColor.Sub,
                modifier = Modifier.padding(start = 10.dp, top = 2.dp).size(16.dp).clickable { onRemove(card.id) },
            )
        }
    }
}

/** 취향 기반 "가는 길에" 제안: 장식 없이 한 줄로 */
@Composable
private fun SuggestionRow(reco: Recommendation, onAccept: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(DColor.Surf)
            .border(1.dp, DColor.Line, shape)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(reco.title, color = DColor.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(reco.detail, color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text("닫기", color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
        Text(
            "추가", color = DColor.Acc, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onAccept).padding(8.dp),
        )
    }
}

/** 동선 지도: 도로를 따라가는 오늘 경로 + 순서 목록 */
@Composable
fun MapScreen(state: MobileState, onBack: () -> Unit, onSendToCar: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            RouteMap(state.home, state.cards, Modifier.fillMaxSize())
            Box(
                Modifier
                    .padding(start = 18.dp, top = 28.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DColor.Bg)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = DColor.Text, modifier = Modifier.size(20.dp)) }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(DColor.Bg)
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 26.dp),
        ) {
            Text("오늘의 동선", color = DColor.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "${state.cards.size}곳 · 이동 ${state.totalDriveMinutes}분 · ${state.homeArrive} 집 도착",
                color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp, bottom = 10.dp),
            )
            state.cards.forEachIndexed { i, card ->
                Row(Modifier.padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    NumberDot((i + 1).toString())
                    Text(card.title, color = DColor.Text, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    Text(card.time, color = DColor.Sub, fontSize = 14.sp)
                }
                Divider()
            }
            PrimaryButton("차에서 이어서 안내", Modifier.padding(top = 16.dp).fillMaxWidth(), onSendToCar)
        }
    }
}
