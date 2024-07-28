package cc.services.connectors

import TTSResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine


@Service
@ConditionalOnProperty("AppUI")
class MyAudioLine {
    @Volatile var isSpeak = false
    fun getAudioStream(callback: ASRSession) {
        log.debug("opening audio line ...")
        val format = AudioFormat(48000.0f, 16, 1, true, false)
        val line = AudioSystem.getTargetDataLine(format)

        line.open(format, line.bufferSize)
        line.start()
        log.debug("line is ready ...")
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val ba = ByteArray(8000)
                line.read(ba, 0, 8000)
                callback.pushAudio(ba)
            }
        }
    }


    suspend fun audioOutput(ttsResult: TTSResult){
        while (true){
            if (isSpeak){
                delay(100)
            }else{
                isSpeak = true


                val info = DataLine.Info(SourceDataLine::class.java, ttsResult.audioFormat)
                val line = AudioSystem.getLine(info) as SourceDataLine
                line.open(ttsResult.audioFormat, 4096);
                line.start();
                ttsResult.audioPCM?.let { line.write(ttsResult.audioPCM, 0, it.size) }
                isSpeak = false
                break
            }
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(SmartSpeech::class.java)
    }

}
