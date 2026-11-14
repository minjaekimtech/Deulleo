package ai.deulleo.mobile

import ai.deulleo.mobile.ui.DeulleoMobileApp
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {

    private val vm: MobileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val store = vm.store
            BackHandler(enabled = store.state.overlay != Overlay.NONE) { store.back() }
            DeulleoMobileApp(store)
        }
    }
}
