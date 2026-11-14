package ai.deulleo.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 공모전 포스터의 밤하늘·빛줄기 색감을 한 단계 밝게 옮긴 팔레트. */
object DColor {
    val Bg0 = Color(0xFF111727)
    val Bg1 = Color(0xFF1E2740)
    val Card = Color(0xFF232C45)
    val Sheet = Color(0xFF222B44)
    val Line = Color(0x42AAB0CD)
    val Sub = Color(0xFFA3A9C2)
    val Text = Color(0xFFF2F4FA)
    val Lav = Color(0xFFA9A4FF)
    val Ice = Color(0xFF8FD3FF)
    val Ink = Color(0xFF0B0F18)
    val Warn = Color(0xFFFFB4B4)
}

val AccentBrush = Brush.linearGradient(listOf(DColor.Lav, DColor.Ice))
val BackgroundBrush = Brush.verticalGradient(listOf(DColor.Bg0, DColor.Bg1))

@Composable
fun ScreenHeader(title: String, subtitle: String, onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, color = DColor.Text, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = DColor.Sub, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DColor.Card)
                .border(1.dp, DColor.Line, RoundedCornerShape(14.dp))
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Menu, contentDescription = "메뉴", tint = DColor.Text) }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text, color = DColor.Sub, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
    )
}

@Composable
fun DCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DColor.Card)
            .border(1.dp, DColor.Line, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        content = content,
    )
}

/** 선택형 칩. 선택되면 포스터의 빛줄기 그라데이션으로 채웁니다. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val base = Modifier.clip(shape)
    val styled = if (selected) base.background(AccentBrush) else base.border(1.dp, DColor.Line, shape)
    Text(
        text,
        color = if (selected) DColor.Ink else DColor.Text,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = styled.clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 7.dp),
    )
}

@Composable
fun Tag(text: String, color: Color) {
    val shape = RoundedCornerShape(50)
    Text(
        text, color = color, fontSize = 11.sp,
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.55f), shape).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun AccentButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AccentBrush)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = DColor.Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
fun NumberDot(label: String) {
    Box(
        Modifier.size(24.dp).clip(CircleShape).background(AccentBrush),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = DColor.Ink, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
fun VSpace(h: Int) = Spacer(Modifier.height(h.dp))
