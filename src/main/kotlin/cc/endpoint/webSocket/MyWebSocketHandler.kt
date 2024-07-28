package cc.endpoint.webSocket


import ELResponse
import TTSResult
import cc.services.VoiceControllerCallback
import cc.services.VoiceControllerFactory
import cc.services.connectors.ASRSession
import okio.ByteString.Companion.toByteString
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class MyWebSocketHandler (
    private val voiceFactory: VoiceControllerFactory,
): BinaryWebSocketHandler() {
    private val sessions = ConcurrentHashMap<String, ASRSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        println("Connection established with session ID: ${session.id}")
        val vc = voiceFactory.createVoiceController(session.id, VoiceSession(session))
        sessions[session.id] = vc
    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val asrSession = sessions[session.id]
        asrSession?.pushAudio(message.payload.toByteString().toByteArray())
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        println("Connection closed with session ID: ${session.id}, status: $status")
        sessions.remove(session.id)!! // .stop() ??
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        println("Transport error in session ID: ${session.id}, error: ${exception.message}")
        session.close(CloseStatus.SERVER_ERROR)
    }

    class VoiceSession(val webSocketSession: WebSocketSession): VoiceControllerCallback {
        override fun asrResult(text: String) {
            println("text рапознан")
        }

        override fun startGeneration() {
            println("начало генерации")
        }

        override fun delta(text: String) {
            println("какая то delta")
        }

        override fun textGenerationComplete() {
            println("ответ сгенерирован")
        }

        override suspend fun ttsResult(ttsResult: TTSResult) {
            if (ttsResult.audioPCM != null){
                webSocketSession.sendMessage(BinaryMessage(ttsResult.audioPCM, true))
            }

        }

        override fun audioComplete() {
        }
    }
}