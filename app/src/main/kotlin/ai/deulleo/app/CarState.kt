package ai.deulleo.app

import ai.deulleo.core.SampleDay
import ai.deulleo.core.model.Errand
import ai.deulleo.core.model.MinuteOfDay
import ai.deulleo.core.model.Plan
import ai.deulleo.core.vehicle.UiMode
import ai.deulleo.core.vehicle.VehicleSnapshot

/** 음성 상태. Pleos SDK에 의존하지 않도록 화면 상태와 함께 둡니다. */
enum class VoiceState { IDLE, LISTENING, SPEAKING }

/** 화면이 그리는 상태 전부. */
data class UiState(
    val mode: UiMode = UiMode.MANAGE,
    val plan: Plan? = null,
    val errands: List<Errand> = emptyList(),
    val lastSpeech: String = "",
    val partialSpeech: String = "",
    val voiceState: VoiceState = VoiceState.IDLE,
    val vehicle: VehicleSnapshot = VehicleSnapshot(),
    val tripStarted: Boolean = false,
    val arrivedAtStop: Boolean = false,
    val clock: MinuteOfDay = SampleDay.START_AT,
    /** 시연 시나리오: true = 평일 하교 픽업, false = 주말 볼일 */
    val pickupScenario: Boolean = false,
    /** 마지막으로 동승 예정자에게 보낸 알림(시연 화면 표시용) */
    val lastNotice: String = "",
    /** 연결 점검: SDK 이름과 상태. 시연 도구에서 보여 줍니다. */
    val sdkStatus: List<Pair<String, String>> = emptyList(),
)

