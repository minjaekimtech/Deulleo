package ai.deulleo.mobile.ui

import ai.deulleo.core.model.ErrandKind
import ai.deulleo.mobile.MobileState
import ai.deulleo.mobile.Recommendation
import ai.deulleo.mobile.StopRow
import ai.deulleo.mobile.VehicleNotice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 홈: "몇 시에 나가면 되는지"를 가장 크게 보여 주고, 그 아래에 차량 알림 → 동선 지도 → 시간표를 둡니다.
 * 시간표의 각 줄에는 그 순서·그 가게인 이유가 한 줄씩 붙습니다.
 */
@Composable
fun HomeScreen(
    state: MobileState,
    onToday: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onRemoveCare: () -> Unit,
    onRestoreCare: () -> Unit,
    onAcceptReco: () -> Unit,
    onDismissReco: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 150.dp),
    ) {
        TopBar(state.dateLabel, if (state.isToday) null else "오늘로", onToday)

        if (state.rows.isEmpty()) {
            Text("일정이 없는 날이에요", color = DColor.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Text("아래 추가 버튼으로 들를 곳을 넣어 보세요.", color = DColor.Sub, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            return@Column
        }

        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.Bottom) {
            Text(state.leaveAt, color = DColor.Text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Text("에 출발하세요", color = DColor.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp, bottom = 9.dp))
        }
        Text(
            "${state.headlineSub} · ${state.homeArrive} 집 도착",
            color = DColor.Sub, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp),
        )

        if (state.notice.isNotBlank()) NoticeBar(state.notice)
        state.vehicle?.let { VehicleCard(it, onRemoveCare, onRestoreCare) }

        // 동선 지도 미리보기
        Box(
            Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(168.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenMap),
        ) {
            RouteMap(state.home, state.rows, Modifier.fillMaxSize(), showLabels = false)
            Text(
                "지도에서 보기", color = DColor.Text, fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DColor.Bg.copy(alpha = 0.88f))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }

        // 시간표
        VSpace(10)
        EndpointRow(state.leaveAt, "집에서 출발")
        state.rows.forEach { row ->
            LegRow(row.legMinutes, row.slackMinutes)
            StopRowView(row) { onOpenDetail(row.id) }
        }
        LegRow(state.homeLegMinutes, 0)
        EndpointRow(state.homeArrive, "집 도착")

        state.recommendation?.let { SuggestionRow(it, onAcceptReco, onDismissReco) }
    }
}

/** 계획이 바뀐 직후 무엇이 달라졌는지 알려 줍니다. */
@Composable
private fun NoticeBar(text: String) {
    Row(
        Modifier.padding(top = 14.dp).fillMaxWidth().height(IntrinsicSize.Min).clip(RoundedCornerShape(8.dp)).background(DColor.Surf),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(DColor.Acc))
        Text(text, color = DColor.Text, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
    }
}

/** 차가 알려 준 상태. 화면에서 주황색을 쓰는 유일한 곳입니다. */
@Composable
private fun VehicleCard(v: VehicleNotice, onRemove: () -> Unit, onRestore: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.padding(top = 16.dp).fillMaxWidth().clip(shape).background(DColor.Surf).border(1.dp, DColor.Line, shape)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(DColor.Warn))
            Text("내 차 · 그랜저", color = DColor.Warn, fontSize = 12.sp, modifier = Modifier.padding(start = 7.dp).weight(1f))
            Text(
                if (v.included) "오늘은 빼기" else "다시 넣기",
                color = DColor.Text, fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = if (v.included) onRemove else onRestore).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Text(v.title, color = DColor.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp))
        Text(v.detail, color = DColor.Sub, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp, end = 8.dp))
    }
}

private val TimeWidth = 60.dp

@Composable
private fun EndpointRow(time: String, label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(time, color = DColor.Sub, fontSize = 14.sp, modifier = Modifier.width(TimeWidth))
        Text(label, color = DColor.Sub, fontSize = 14.sp)
    }
    Divider()
}

@Composable
private fun LegRow(minutes: Int, slack: Int) {
    val text = if (slack >= 5) "차로 ${minutes}분 · ${slack}분 여유" else "차로 ${minutes}분"
    Text(text, color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(start = TimeWidth, top = 7.dp, bottom = 7.dp))
    Divider()
}

@Composable
private fun StopRowView(row: StopRow, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            .background(if (row.changed) DColor.Surf else DColor.Bg)
            .clickable(onClick = onClick),
    ) {
        // 방금 바뀐 줄은 왼쪽에 표시합니다.
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (row.changed) DColor.Acc else DColor.Bg))
        Row(Modifier.weight(1f).padding(top = 14.dp, bottom = 14.dp, end = 2.dp)) {
            Column(Modifier.width(TimeWidth - 3.dp)) {
                Text(row.start, color = DColor.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(row.end, color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(row.title, color = DColor.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (row.placeName != row.title) {
                    Text(row.placeName, color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
                }
                row.parking?.let {
                    Text(if (row.kind == ErrandKind.PICKUP) it else "주차 $it", color = DColor.Sub, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
                }
                row.reason?.let {
                    Text(it, color = DColor.Soft, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
            Text(
                row.source,
                color = if (row.kind == ErrandKind.VEHICLE_CARE) DColor.Warn else DColor.Sub,
                fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp),
            )
        }
    }
    Divider()
}

/** 취향 기반 "가는 길에" 제안 */
@Composable
private fun SuggestionRow(reco: Recommendation, onAccept: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.padding(top = 18.dp).fillMaxWidth().clip(shape).border(1.dp, DColor.Line, shape)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(reco.title, color = DColor.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(reco.detail, color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text("괜찮아요", color = DColor.Sub, fontSize = 13.sp, modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp))
        Text("넣기", color = DColor.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAccept).padding(8.dp))
    }
}
