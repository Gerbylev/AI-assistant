import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream

//package cc.services.connectors
//
//import com.mlp.sdk.utils.JSON
//import okhttp3.*
//import org.slf4j.LoggerFactory
//import org.springframework.stereotype.Service

class TTSResult(
    val text: String?,
    val audioPCM: ByteArray?,
    val isFinal: Boolean,
    val audioFormat: AudioFormat
)

interface TTSServiceCallback {
    suspend fun receive(res: TTSResult)
}

interface TTSSession {
    fun send(text: String, final: Boolean, order: Long)
}

interface TTSService {
    fun createTTSSession(sessionId: String, callback: TTSServiceCallback): TTSSession
}

data class ELVoiceSettings(
    val stability: Double,
    val similarity_boost: Double,
    val style: Int,
    val use_speaker_boost: Boolean
)

data class ELGenerationConfig(
    val chunk_length_schedule: List<Int>
)

data class ELRequest(
    val text: String,
    val voice_settings: ELVoiceSettings? = null,
    val generation_config: ELGenerationConfig? = null,
    val try_trigger_generation: Boolean? = null,
    val flush: Boolean? = null
)

//data class ELResponse(
//    val text: String?,
//    val audio: AudioInputStream,
//    val isFinal: Boolean,
//)

data class ELResponse(
    val audio: String?,
    val isFinal: Boolean,
)

//@Service
//class ELConnector(): TTSService {
//    val voiceId = "N2lVS1w4EtoT3dr4eOWO"
//    val modelId = "eleven_multilingual_v2"
//
//    val socketUrl = "wss://api.elevenlabs.io/v1/text-to-speech/${voiceId}/stream-input?model_id=${modelId}&output_format=pcm_22050"
//
//    val params = cc.services.connectors.ELRequest(
//        text = " ",
//        voice_settings = ELVoiceSettings(
//            stability = 1.0,
//            similarity_boost = 0.7,
//            style = 0,
//            use_speaker_boost = true
//        ),
//        generation_config = ELGenerationConfig(
//            chunk_length_schedule = listOf(50, 70, 120, 160)
//        )
//    )

//    private val lock = this
//    @Volatile
//    private var webSocket: WebSocket? = null
//    val client = OkHttpClient()
//
//    override fun start() {
//        synchronized(this) {
//            if (webSocket == null) {
//                val request = Request.Builder()
//                    .url(url = socketUrl)
//                    .header("xi-api-key", "sk_ba80ba8b70ab2913f3829e96ea2ed20946729bcef5467718")
//                    .build()
//                this.webSocket = client.newWebSocket(request, webSocketListener)
//
//                send(params)
//            }
//        }
//    }
//
//    override fun send(text: String, final: Boolean, order: Long) {
//        if (text.isNotEmpty()) {
//            this.send(ELRequest(text = "$text ", flush = true))
//        }
//        if (final) {
//            this.send(ELRequest(text = ""))
//        }
//    }
//
//    private fun send(req: ELRequest) {
//        val text = JSON.stringify(req)
//        log.debug("Sending: $text")
//        var counter = 30
//        while (webSocket == null && counter-- > 0) {
//            Thread.sleep(100)
//        }
//        webSocket?.send(text) ?: log.error("ws is closed!!!")
//    }
//
//    override fun stop() {
//        webSocket?.close(1008, "client interrupted")
//        webSocket = null
//    }
//
//    private val webSocketListener = object : WebSocketListener() {
//        //called when connection succeeded
//        //we are sending a message just after the socket is opened
//        override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
//            log.debug("onOpen")
//        }
//
//        //called when text message received
//        override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
//            log.debug("Receiving: ${text.subSequence(0, 100.coerceAtMost(text.length))} ...")
//            kotlin.runCatching {
//                callback.receive(JSON.parse(text, ELResponse::class.java))
//            }.onFailure {
//                log.error("", it)
//            }
//
//        }
//
//        //called when binary message received
//        override fun onClosing(webSocket0: okhttp3.WebSocket, code: Int, reason: String) {
//            log.debug("onClosing()")
//            synchronized(lock) {
//                if (webSocket == webSocket0) {
//                    webSocket = null
//                }
//            }
//        }
//
//        override fun onClosed(webSocket0: okhttp3.WebSocket, code: Int, reason: String) {
//            log.debug("onClosed()")
//            synchronized(lock) {
//                if (webSocket == webSocket0) {
//                    webSocket = null
//                }
//            }
//        }
//
//        override fun onFailure(
//            webSocket0: okhttp3.WebSocket, t: Throwable, response: Response?
//        ) {
//            log.error("onFailure()", t)
//            synchronized(lock) {
//                if (webSocket == webSocket0) {
//                    webSocket = null
//                }
//            }
//        }
//    }
//
//    lateinit var callback: TTSServiceCallback
//    override fun callback(cb: TTSServiceCallback) {
//        this.callback = cb
//    }
//
//    companion object {
//        private val log = LoggerFactory.getLogger("EL")
//    }
//
//}