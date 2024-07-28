package cc.services

//import cc.services.connectors.AimyvoiceConnector
import ELResponse
import TTSResult
import cc.services.connectors.ASRSession
import cc.services.connectors.AimyvoiceConnector
import cc.services.connectors.GPTConnector
import cc.services.connectors.SmartSpeech
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

interface VoiceControllerCallback {
    /**
     * Сообщает о появлении результата от распознавания
     */
    fun asrResult(text: String)
    /**
     * Сообщает о начале генерации текста ответа
     */
    fun startGeneration()

    /**
     * Передаёт кусочек текста ответа - стрим от GPT
     */
    fun delta(text: String)

    /**
     * Сообщает о том, что генерация текста завершена
     */
    fun textGenerationComplete()

    /**
     * Передаёт результат генерации для проигрывания пользователю
     */
    suspend fun ttsResult(elResponse: TTSResult)

    /**
     * Сообщает о том, что генерация аудио завершена
     */
    fun audioComplete()
}

@Service
class VoiceControllerFactory(
    val gpt: GPTConnector,
//    val tts: ELConnector,
    val tts: AimyvoiceConnector,
    val asr: SmartSpeech,

    @Value("\${caila.defaultModel.model}") val model: String,
    @Value("\${caila.defaultModel.systemPrompt}") val systemPrompt: String?
) {

    fun createVoiceController(sessionId: String, callback: VoiceControllerCallback): VoiceController {
        val vc = VoiceController(sessionId, model, systemPrompt, gpt)
        vc.callback = callback
        val asrSession = asr.createASRSession(sessionId, vc)
        val ttsSession = tts.createTTSSession(sessionId, vc)

        vc.asrSession = asrSession
        vc.ttsSession = ttsSession

        return vc
    }

}