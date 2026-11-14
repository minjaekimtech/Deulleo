package ai.deulleo.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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

/**
 * 절제된 무채색 팔레트. 색이 있는 곳은 동선(경로 선)을 그리는 [Acc] 하나뿐입니다.
 * 그라데이션·글로우·이모지를 쓰지 않고, 카드 대신 얇은 구분선으로 정보를 나눕니다.
 */
object DColor {
    val Bg = Color(0xFF14171C)
    val Surf = Color(0xFF1C2026)
    val Line = Color(0xFF2C313A)
    val Sub = Color(0xFF8B929E)
    val Text = Color(0xFFF4F5F7)
    val Acc = Color(0xFF7CC4E8)
    val Warn = Color(0xFFE8A87C)
    val Ink = Color(0xFF14171C)
}

/** 화면 맨 위: 왼쪽 작은 라벨(날짜·화면 이름), 오른쪽 메뉴 */
@Composable
fun TopBar(label: String, onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = DColor.Sub, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(
            Icons.Filled.Menu, contentDescription = "메뉴", tint = DColor.Text,
            modifier = Modifier.size(24.dp).clickable(onClick = onMenu),
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 26.dp, bottom = 4.dp))
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(DColor.Line))
}

/** 선택형 칩: 선택되면 테두리와 글자가 밝아집니다. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        text,
        color = if (selected) DColor.Text else DColor.Sub,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, if (selected) DColor.Text else DColor.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DColor.Text)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = DColor.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun NumberDot(label: String) {
    Box(
        Modifier.size(22.dp).clip(CircleShape).background(DColor.Text),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = DColor.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun VSpace(h: Int) {
    Spacer(Modifier.height(h.dp))
}
