package ai.deulleo.mobile

import androidx.lifecycle.ViewModel

/** 화면 회전에도 상태가 유지되도록 [MobileStore]를 보관합니다. */
class MobileViewModel : ViewModel() {
    val store = MobileStore()
}
