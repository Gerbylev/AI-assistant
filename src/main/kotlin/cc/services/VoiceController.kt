package cc.services

import ELResponse
import TTSResult
import TTSServiceCallback
import TTSSession
import cc.services.VoiceControllerState.*
import cc.services.connectors.*
import com.mlp.api.datatypes.chatgpt.ChatMessage
import com.mlp.api.datatypes.chatgpt.ChatRole
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

enum class VoiceControllerState {
    IDLE,
    TEXT_GENERATION,
    PLAYBACK
}

class VoiceController(
    val sessionId: String,
    val model: String,
    val prompt: String?,
    val gptConnector: GPTConnector
): ASRSession, ASRSessionCallback, TTSServiceCallback {
    @Volatile var state = IDLE
    @Volatile var textGenerationIsActive = false
    @Volatile var playbackIsActive = false
    val history = ArrayDeque<ChatMessage>(5)

    lateinit var callback: VoiceControllerCallback
    lateinit var asrSession: ASRSession
    lateinit var ttsSession: TTSSession


    override fun pushAudio(buffer: ByteArray) {
        asrSession.pushAudio(buffer)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun speech(text: String) {
        callback.asrResult(text)
        GlobalScope.launch(Dispatchers.IO) {
            generateAnswer(text)
        }
    }

    override suspend fun receive(res: TTSResult) {
        callback.ttsResult(res)
    }

    @Volatile var order = 0L
    private suspend fun generateAnswer(query: String){
        println("начало генерации ответа")
        history.add(ChatMessage(ChatRole.user, query))
        state = TEXT_GENERATION
        callback.startGeneration()
        textGenerationIsActive = true
        log.info("start generation")
        gptConnector.gpt(history) {
//            if (interrupted) {
//                textGenerationIsActive = false
//                throw CancellationException()
//            }
            if (it != null) {
                if (history.last().role != ChatRole.assistant) {
                    history.add(ChatMessage(ChatRole.assistant, ""))
                }
                val c = history.removeLast()
                history.add(ChatMessage(ChatRole.assistant, c.content + it))

                playbackIsActive = true
                ttsSession.send(text = it, final = false, ++order)
                callback.delta(it)
            } else {
                log.info("finish text generation")
                state = PLAYBACK
                ttsSession.send(text = "", final = true, ++order)
                textGenerationIsActive = false
                callback.textGenerationComplete()
            }
        }
    }

//    @Volatile
//    var interrupted = false
//    suspend fun interrupt() {
//        log.info("interrupted ...")
//        interrupted = true
//        delay(300)
//
//        while (textGenerationIsActive) {
//            delay(100)
//        }
//        state = IDLE
//        interrupted = false
//        line = createALine()
//        expectedPlaybackStopMillis = null
//        log.info("interruption completed")
//    }

    private val log = LoggerFactory.getLogger("VC")
}

