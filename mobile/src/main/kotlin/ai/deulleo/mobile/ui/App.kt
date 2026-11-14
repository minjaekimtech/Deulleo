package ai.deulleo.mobile.ui

import ai.deulleo.mobile.MobileStore
import ai.deulleo.mobile.Overlay
import ai.deulleo.mobile.TODAY
import ai.deulleo.mobile.Tab
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 폰 앱 전체. 안드로이드 전용 API를 쓰지 않아 데스크톱에서도 그대로 렌더링해 확인할 수 있습니다. */
@Composable
fun DeulleoMobileApp(store: MobileStore, screenshotRecognized: Boolean = false) {
    val state = store.state
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DColor.Text, onPrimary = DColor.Ink, background = DColor.Bg, surface = DColor.Surf,
            onSurface = DColor.Text, outline = DColor.Line,
        ),
    ) {
        Box(Modifier.fillMaxSize().background(DColor.Bg)) {
            when (state.overlay) {
                Overlay.MAP -> MapScreen(state, onBack = store::back, onSendToCar = { store.open(Overlay.HANDOFF) })
                Overlay.HANDOFF -> HandoffScreen(state, onBack = store::back, onDone = store::close)
                else -> {
                    when (state.tab) {
                        Tab.HOME -> HomeScreen(
                            state,
                            onToday = { store.selectDay(TODAY) },
                            onOpenMap = { store.open(Overlay.MAP) },
                            onOpenDetail = store::openDetail,
                            onRemoveCare = { store.remove("care-TIRE_PRESSURE") },
                            onRestoreCare = store::restoreCare,
                            onAcceptReco = store::acceptRecommendation,
                            onDismissReco = store::dismissRecommendation,
                        )
                        Tab.CALENDAR -> CalendarScreen(state, onOpenDay = store::selectDay)
                        Tab.PROFILE -> ProfileScreen(state, store::toggleTaste, store::setRecommendOn, store::setCarConnected)
                    }
                    if (state.tab == Tab.HOME) AddButton(Modifier.align(Alignment.BottomEnd)) { store.open(Overlay.ADD) }
                    TabBar(state.tab, store::selectTab, Modifier.align(Alignment.BottomCenter))
                }
            }

            when (state.overlay) {
                Overlay.ADD -> BottomSheet(store::close) { AddSheet(store::addItem) { store.open(Overlay.SCREENSHOT) } }
                Overlay.SCREENSHOT -> BottomSheet(store::close) { ScreenshotSheet(store::addFromScreenshot, screenshotRecognized) }
                Overlay.DETAIL -> state.detail?.let { row ->
                    BottomSheet(store::close) { DetailSheet(row, state.alternatives, store::swapTo) { store.remove(row.id) } }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun AddButton(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .padding(end = 24.dp, bottom = 88.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(DColor.Text)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = DColor.Ink, modifier = Modifier.size(18.dp))
        Text("추가", color = DColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun TabBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(DColor.Bg)) {
        Divider()
        Row(Modifier.fillMaxWidth().height(66.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            TabItem(Icons.Filled.DateRange, "캘린더", current == Tab.CALENDAR) { onSelect(Tab.CALENDAR) }
            TabItem(Icons.Filled.Home, "동선", current == Tab.HOME) { onSelect(Tab.HOME) }
            TabItem(Icons.Filled.Person, "내 정보", current == Tab.PROFILE) { onSelect(Tab.PROFILE) }
        }
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) DColor.Text else DColor.Sub
    Column(Modifier.width(80.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Text(label, color = color, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
