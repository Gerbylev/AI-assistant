package cc.services.connectors


import ELResponse
import TTSResult
import TTSService
import TTSServiceCallback
import TTSSession
import cc.services.AppUI
import cc.services.CailaClient
import cc.utils.consoleRunner
import com.mlp.api.TypeInfo
import com.mlp.api.datatypes.chatgpt.ChatCompletionRequest
import com.mlp.api.datatypes.chatgpt.ChatCompletionResult
import com.mlp.api.datatypes.chatgpt.ChatMessage
import com.mlp.api.datatypes.chatgpt.ChatRole
import com.mlp.gate.ClientResponseProto
import com.mlp.sdk.Payload
import com.mlp.sdk.datatypes.tts.TtsRequest
import com.mlp.sdk.datatypes.tts.TtsRequest.AudioFormatOptions.AudioEncoding.LINEAR16_PCM
import com.mlp.sdk.datatypes.tts.TtsResponse
import com.mlp.sdk.utils.JSON
import javafx.application.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.io.File
import java.util.*
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioSystem


// TODO: пока что он не работает как надо
@Service
class AimyvoiceConnector(
    private val caila: CailaClient,
): TTSService {

    override fun createTTSSession(sessionId: String, callback: TTSServiceCallback): TTSSession {
        val tts = TTSSessionImpl(sessionId, caila)
        tts.callback = callback

        return tts
    }

    class TTSSessionImpl(sessionId: String, private var caila: CailaClient):TTSSession{
        private val log = LoggerFactory.getLogger(this.javaClass)

        lateinit var callback: TTSServiceCallback

        override fun send(text: String, final: Boolean, order: Long) {
            runBlocking {
                tts(text)
            }
        }

        private suspend fun tts(query: String) {
            if (query.isBlank()) {
                return
            }
            val req = TtsRequest(query, "Tatiana", TtsRequest.AudioFormatOptions(audioEncoding = LINEAR16_PCM, sampleRateHertz = 22050, chunkSizeKb = null))
            try {
                val res = caila.grpcClient.predict(
                    "just-ai",
                    "aimyvoice-multispeaker",
                    buildPayload(req),
                    null,
                    authToken = caila.grpcClient.config.clientToken!!
                )

                val r = JSON.parse(res.data, TtsResponse::class.java)
                val a = Base64.getDecoder().decode(r.audio_base64)
                val iss = AudioSystem.getAudioInputStream(a.inputStream())
                val dd = iss.readBytes()

                callback.receive(TTSResult(r.text, dd, true, iss.format))
            } catch (e: Exception) {
                log.error("error on tts", e)
            }
        }
    }

}

//fun main(args: Array<String>) {
//    val context = consoleRunner("voice-demo.yml")
//
////    val demo = context.getBean(VoiceDemo::class.java)
//    val ac = context.getBean(AimyvoiceConnector::class.java)
//    ac.callback(object : TTSServiceCallback {
//        override fun receive(res: ELResponse) {
//            println(System.currentTimeMillis())
//            println(res.audio)
//            val ad = Base64.getDecoder().decode(res.audio)
//            File("./aimy.pcm").writeBytes(ad)
//        }
//    })
//    runBlocking {
//        println(System.currentTimeMillis())
//        ac.tts("Привет дай сто котлет")
//    }
//}
