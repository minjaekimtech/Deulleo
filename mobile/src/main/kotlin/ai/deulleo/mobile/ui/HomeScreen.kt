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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** ① 홈: 동선 지도 미리보기 + 오늘 일정 타임라인 */
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
            .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 170.dp),
    ) {
        ScreenHeader("Hi, 오늘도 들러요", "11월 14일 토요일", onMenu)

        // 지도 미리보기
        Box(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DColor.Line, RoundedCornerShape(20.dp))
                .clickable(onClick = onOpenMap),
        ) {
            RouteMap(state.home, state.cards, Modifier.fillMaxSize(), showLabels = false)
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DColor.Bg0.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "일정 ${state.cards.size}곳 · 이동 ${state.totalDriveMinutes}분 · 집 ${state.homeArrive}",
                    color = DColor.Text, fontSize = 13.sp,
                )
                Text("지도 크게 ›", color = DColor.Ice, fontSize = 13.sp)
            }
        }

        if (state.message.isNotBlank()) {
            Text(
                state.message, color = DColor.Ice, fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        SectionLabel("오늘 일정")
        if (state.cards.isEmpty()) {
            Text("오늘 일정이 없어요. 아래 + 버튼으로 추가해 보세요.", color = DColor.Sub, fontSize = 14.sp)
        } else {
            Timeline(state.cards, state.recommendation, onRemove, onAcceptReco, onDismissReco)
        }
    }
}

@Composable
private fun Timeline(
    cards: List<StopCard>,
    reco: Recommendation?,
    onRemove: (String) -> Unit,
    onAcceptReco: () -> Unit,
    onDismissReco: () -> Unit,
) {
    val lineBrush = Brush.verticalGradient(listOf(DColor.Lav, DColor.Ice))
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                // 일정을 잇는 빛나는 동선
                val x = 8.dp.toPx()
                val top = Offset(x, 16.dp.toPx())
                val bottom = Offset(x, size.height - 24.dp.toPx())
                drawLine(lineBrush, top, bottom, 9.dp.toPx(), StrokeCap.Round, alpha = 0.25f)
                drawLine(lineBrush, top, bottom, 3.dp.toPx(), StrokeCap.Round)
            }
            .padding(start = 26.dp),
    ) {
        // 추천은 동선 중간(두 번째 일정 뒤)에 끼워 넣습니다.
        val recoIndex = if (cards.size >= 2) 2 else cards.size
        cards.forEachIndexed { i, card ->
            if (reco != null && i == recoIndex) RecoCard(reco, onAcceptReco, onDismissReco)
            Text("↓ 차로 ${card.legMinutes}분", color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(vertical = 7.dp))
            StopCardView(card, onRemove)
        }
        if (reco != null && recoIndex >= cards.size) RecoCard(reco, onAcceptReco, onDismissReco)
    }
}

@Composable
private fun TimelineDot() {
    Box(
        Modifier
            .offset(x = (-24).dp, y = 18.dp)
            .size(13.dp)
            .clip(CircleShape)
            .background(DColor.Bg0)
            .border(3.dp, DColor.Lav, CircleShape),
    )
}

@Composable
private fun StopCardView(card: StopCard, onRemove: (String) -> Unit) {
    val (tag, tagColor) = when (card.kind) {
        ErrandKind.FIXED -> "예약" to DColor.Ice
        ErrandKind.FLEXIBLE -> "할 일" to DColor.Lav
        ErrandKind.VEHICLE_CARE -> "차량" to DColor.Warn
        ErrandKind.PICKUP -> "픽업" to Color(0xFF9BE7C4)
    }
    Box {
        DCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.time, color = DColor.Ice, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Tag(tag, tagColor)
                // 예약은 실수로 지우지 않도록 삭제 버튼을 두지 않습니다.
                if (card.kind != ErrandKind.FIXED) {
                    Icon(
                        Icons.Filled.Close, contentDescription = "삭제", tint = DColor.Sub,
                        modifier = Modifier.padding(start = 8.dp).size(18.dp).clickable { onRemove(card.id) },
                    )
                }
            }
            Text(card.title, color = DColor.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            if (card.detail.isNotBlank()) {
                Text(card.detail, color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
        TimelineDot()
    }
}

@Composable
private fun RecoCard(reco: Recommendation, onAccept: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.padding(top = 10.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(DColor.Lav.copy(alpha = 0.08f))
                .drawBehind {
                    drawRoundRect(
                        DColor.Lav.copy(alpha = 0.6f),
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                    )
                }
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reco.title, color = DColor.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Tag("추천", DColor.Lav)
            }
            Text(reco.detail, color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
            Row(Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("넣기", selected = true, onClick = onAccept)
                Chip("괜찮아요", selected = false, onClick = onDismiss)
            }
        }
        TimelineDot()
    }
}

/** ② 동선 지도: 도로를 따라가는 오늘 경로 + 순서 목록 */
@Composable
fun MapScreen(state: MobileState, onBack: () -> Unit, onSendToCar: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            RouteMap(state.home, state.cards, Modifier.fillMaxSize())
            Row(Modifier.padding(start = 18.dp, top = 28.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DColor.Card)
                        .border(1.dp, DColor.Line, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로", tint = DColor.Text) }
                Spacer(Modifier.width(10.dp))
                Text(
                    "오늘의 동선 · ${state.totalDriveMinutes}분",
                    color = DColor.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DColor.Card)
                        .border(1.dp, DColor.Line, RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(DColor.Sheet)
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
        ) {
            state.cards.forEachIndexed { i, card ->
                Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    NumberDot((i + 1).toString())
                    Text(card.title, color = DColor.Text, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    Text("${card.time} · ${card.legMinutes}분", color = DColor.Sub, fontSize = 13.sp)
                }
            }
            Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberDot("집")
                Text("집 도착", color = DColor.Text, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp).weight(1f))
                Text(state.homeArrive, color = DColor.Sub, fontSize = 13.sp)
            }
            AccentButton("차에서 이어서 안내", Modifier.padding(top = 10.dp).fillMaxWidth(), onSendToCar)
        }
    }
}
