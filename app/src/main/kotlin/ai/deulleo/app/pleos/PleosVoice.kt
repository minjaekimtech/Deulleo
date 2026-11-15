package ai.deulleo.app.pleos

import ai.deulleo.app.BuildConfig
import ai.deulleo.app.VoiceState
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitListener
import ai.pleos.playground.stt.listener.OnServerConnectionListener as SttServerListener
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInitListener
import ai.pleos.playground.tts.listener.OnServerConnectionListener as TtsServerListener


/**
 * SpeechToText SDK v3.3.1 + TextToSpeech SDK v3.3.1 래퍼.
 *
 * Pleos Playground 자격 증명(local.properties의 pleos.clientId/clientSecret)이 있으면 HYBRID 모드
 * (서버 인식 우선, 네트워크가 끊기면 On-device로 자동 전환)로, 없으면 ON_DEVICE 모드로 동작합니다.
 */
class PleosVoice(context: Context) {

    private val appContext = context.applicationContext

    private val hasCredentials = BuildConfig.PLEOS_CLIENT_ID.isNotBlank() && BuildConfig.PLEOS_CLIENT_SECRET.isNotBlank()

    private val stt = SpeechToText(context.applicationContext, if (hasCredentials) SttMode.HYBRID else SttMode.ON_DEVICE)
    private val tts = TextToSpeech(context.applicationContext, if (hasCredentials) TtsMode.HYBRID else TtsMode.ON_DEVICE)

    private val _utterances = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** 최종 인식된 문장 */
    val utterances: SharedFlow<String> = _utterances.asSharedFlow()

    private val _partial = MutableStateFlow("")
    /** 인식 중인 문장(화면 표시용) */
    val partial: StateFlow<String> = _partial.asStateFlow()

    private val _state = MutableStateFlow(VoiceState.IDLE)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val resultListener = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() { _state.value = VoiceState.LISTENING }
        override fun onUpdated(text: String, completed: Boolean) {
            _partial.value = text
            if (completed && text.isNotBlank()) _utterances.tryEmit(text)
        }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onEndedRecognition() { if (_state.value == VoiceState.LISTENING) _state.value = VoiceState.IDLE }
        override fun onError(errorType: SttError) {
            Log.w(TAG, "STT 오류: $errorType")
            _state.value = VoiceState.IDLE
            _sttStatus.value = "오류($errorType) · 시연 도구의 입력을 쓰세요"
        }
    }

    private val ttsListener = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() { _state.value = VoiceState.SPEAKING }
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() { _state.value = VoiceState.IDLE }
        override fun onStop() { _state.value = VoiceState.IDLE }
        override fun onError(errorType: TtsError) {
            Log.w(TAG, "TTS 오류: $errorType")
            _state.value = VoiceState.IDLE
            fallBackToPlatformTts("$errorType")
        }
    }

    private val _sttStatus = MutableStateFlow("초기화 중")
    private val _ttsStatus = MutableStateFlow("초기화 중")
    /** 연결 점검용 */
    val sttStatus: StateFlow<String> = _sttStatus.asStateFlow()
    val ttsStatus: StateFlow<String> = _ttsStatus.asStateFlow()

    /** Pleos 음성 합성을 쓸 수 없을 때 쓰는 안드로이드 기본 음성 합성 */
    private var platformTts: android.speech.tts.TextToSpeech? = null
    @Volatile private var usePlatformTts = false

    fun start() {
        try {
            stt.addListener(resultListener)
            stt.initialize(object : SttInitListener {
                override fun onSuccess() {
                    _sttStatus.value = "연결됨"
                    runCatching { stt.setLocale(Locale.KOREA) }
                }
                override fun onFailure(errorType: SttError) { _sttStatus.value = "사용 불가($errorType) · 시연 도구의 입력을 쓰세요" }
            })
        } catch (e: Exception) {
            Log.w(TAG, "음성 인식 초기화 실패: ${e.message}")
            _sttStatus.value = "사용 불가(${e.message ?: e.javaClass.simpleName}) · 시연 도구의 입력을 쓰세요"
        }
        try {
            tts.addEventListener(ttsListener)
            tts.initialize(object : TtsInitListener {
                override fun onSuccess() {
                    _ttsStatus.value = "연결됨"
                    runCatching { tts.setLocale(Locale.KOREA) }
                }
                override fun onFailure(errorType: TtsError) { fallBackToPlatformTts("$errorType") }
            })
        } catch (e: Exception) {
            Log.w(TAG, "음성 합성 초기화 실패: ${e.message}")
            fallBackToPlatformTts(e.message ?: e.javaClass.simpleName)
        }
        if (hasCredentials) runCatching { registerApps() }
    }

    /** Pleos TextToSpeech를 쓸 수 없으면 안드로이드 기본 음성 합성으로 바꿉니다. 시연이 소리 없이 끊기지 않게 하기 위한 대비입니다. */
    private fun fallBackToPlatformTts(reason: String) {
        if (usePlatformTts) return
        usePlatformTts = true
        _ttsStatus.value = "Pleos 사용 불가($reason) · 기본 음성으로 대체"
        platformTts = android.speech.tts.TextToSpeech(appContext) { result ->
            if (result == android.speech.tts.TextToSpeech.SUCCESS) {
                platformTts?.language = Locale.KOREA
            } else {
                _ttsStatus.value = "음성 합성 사용 불가($reason) · 화면 글자로만 안내"
            }
        }
    }

    /** 주행 중 응답은 짧게: 화면 대신 귀로 듣는 정보입니다. */
    fun speak(text: String) {
        runCatching { stt.stop() }
        if (usePlatformTts) {
            runCatching { platformTts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "deulleo") }
            return
        }
        try {
            tts.speak(text, true)
        } catch (e: Exception) {
            Log.w(TAG, "speak 실패: ${e.message}")
            fallBackToPlatformTts(e.message ?: e.javaClass.simpleName)
            runCatching { platformTts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_ADD, null, "deulleo") }
        }
    }

    /** 마이크 버튼(또는 Gleo 호출) → 한 문장 듣기 */
    fun listen() {
        try {
            tts.stop()
            _partial.value = ""
            stt.request()
        } catch (e: Exception) {
            Log.w(TAG, "listen 실패: ${e.message}")
        }
    }

    fun stop() {
        try {
            stt.removeListener(resultListener)
            stt.release()
            tts.removeEventListener(ttsListener)
            tts.release()
            platformTts?.shutdown()
        } catch (e: Exception) {
            Log.w(TAG, "음성 SDK 해제 실패: ${e.message}")
        }
    }

    private fun registerApps() {
        stt.registerApp(BuildConfig.PLEOS_CLIENT_ID, BuildConfig.PLEOS_CLIENT_SECRET, object : SttServerListener {
            override fun onConnected() { Log.i(TAG, "STT 서버 연결") }
            override fun onFailed(errorType: SttError) { Log.w(TAG, "STT 서버 연결 실패: $errorType") }
        })
        tts.registerApp(BuildConfig.PLEOS_CLIENT_ID, BuildConfig.PLEOS_CLIENT_SECRET, object : TtsServerListener {
            override fun onConnected() { Log.i(TAG, "TTS 서버 연결") }
            override fun onFailed(errorType: TtsError) { Log.w(TAG, "TTS 서버 연결 실패: $errorType") }
        })
    }

    private companion object { const val TAG = "Deulleo" }
}
