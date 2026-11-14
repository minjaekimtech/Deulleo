package ai.deulleo.mobile

import ai.deulleo.mobile.ui.BottomSheet
import ai.deulleo.mobile.ui.CalendarScreen
import ai.deulleo.mobile.ui.DColor
import ai.deulleo.mobile.ui.Divider
import ai.deulleo.mobile.ui.PrimaryButton
import ai.deulleo.mobile.ui.HomeScreen
import ai.deulleo.mobile.ui.MapScreen
import ai.deulleo.mobile.ui.MenuSheet
import ai.deulleo.mobile.ui.ProfileScreen
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    private val vm: MobileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            MaterialTheme(colorScheme = darkColorScheme(primary = DColor.Text, background = DColor.Bg, surface = DColor.Surf)) {
                DeulleoMobileApp(state, vm)
            }
        }
    }
}

@Composable
private fun DeulleoMobileApp(state: MobileState, vm: MobileViewModel) {
    val context = LocalContext.current
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    BackHandler(enabled = state.showMap || state.showAddSheet || state.showMenu) {
        when {
            state.showAddSheet -> vm.openAddSheet(false)
            state.showMenu -> vm.openMenu(false)
            else -> vm.openMap(false)
        }
    }

    Box(Modifier.fillMaxSize().background(DColor.Bg)) {
        if (state.showMap) {
            MapScreen(
                state,
                onBack = { vm.openMap(false) },
                onSendToCar = { toast("차량 앱으로 오늘 동선을 보냈어요 (시연)") },
            )
        } else {
            when (state.tab) {
                Tab.HOME -> HomeScreen(
                    state,
                    onMenu = { vm.openMenu(true) },
                    onOpenMap = { vm.openMap(true) },
                    onRemove = vm::remove,
                    onAcceptReco = vm::acceptRecommendation,
                    onDismissReco = vm::dismissRecommendation,
                )
                Tab.CALENDAR -> CalendarScreen(state, onMenu = { vm.openMenu(true) })
                Tab.PROFILE -> ProfileScreen(
                    state,
                    onMenu = { vm.openMenu(true) },
                    onToggleStyle = vm::toggleDrivingStyle,
                    onToggleTaste = vm::toggleTaste,
                )
            }

            // 추가 버튼 (홈에서만)
            if (state.tab == Tab.HOME) {
                Row(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 92.dp)
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .background(DColor.Text)
                        .clickable { vm.openAddSheet(true) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = DColor.Ink, modifier = Modifier.size(18.dp))
                    Text("추가", color = DColor.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                }
            }

            TabBar(state.tab, vm::selectTab, Modifier.align(Alignment.BottomCenter))
        }

        if (state.showAddSheet) {
            BottomSheet(onDismiss = { vm.openAddSheet(false) }) {
                AddSheetContent(onAdd = vm::addItem, onComingSoon = { toast("다음 단계에서 연결돼요") })
            }
        }
        if (state.showMenu) {
            BottomSheet(onDismiss = { vm.openMenu(false) }) {
                MenuSheet(state, onRecommend = vm::setRecommendOn, onCar = vm::setCarConnected)
            }
        }
    }
}

@Composable
private fun TabBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(DColor.Bg)) {
        Divider()
        Row(
            Modifier.fillMaxWidth().height(68.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(Icons.Filled.DateRange, "캘린더", current == Tab.CALENDAR) { onSelect(Tab.CALENDAR) }
            TabItem(Icons.Filled.Home, "오늘", current == Tab.HOME) { onSelect(Tab.HOME) }
            TabItem(Icons.Filled.Person, "내 정보", current == Tab.PROFILE) { onSelect(Tab.PROFILE) }
        }
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) DColor.Text else DColor.Sub
    Column(
        Modifier.width(80.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Text(label, color = color, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

/** 추가 시트: 직접 입력은 동작하고, 스크린샷·음성·캘린더는 다음 단계에서 연결합니다. */
@Composable
private fun AddSheetContent(onAdd: (String) -> Unit, onComingSoon: () -> Unit) {
    var text by remember { mutableStateOf("") }
    Column {
        Text("일정 추가", color = DColor.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = { Text("살 것이나 들를 곳 (예: 케이크, 약, 장보기)") },
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton("동선에 넣기", Modifier.padding(top = 10.dp, bottom = 10.dp).fillMaxWidth()) {
            onAdd(text)
            text = ""
        }
        AddOption("스크린샷으로 추가", "예약 문자·카톡·시간표를 올리면 자동 인식", onComingSoon)
        AddOption("말로 추가", "\"토요일에 엄마 생신 꽃 사야 해\"", onComingSoon)
        AddOption("캘린더에서 가져오기", "장소가 있는 일정을 불러와요", onComingSoon)
    }
}

@Composable
private fun AddOption(title: String, desc: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp)) {
        Text(title, color = DColor.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(desc, color = DColor.Sub, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
    Divider()
}
