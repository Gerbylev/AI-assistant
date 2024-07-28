package cc.services.connectors

import cc.services.AppUI
import cc.smartspeech.SaluteAsrv2
import cc.smartspeech.SmartSpeechGrpcKt
import cc.utils.consoleRunner
import com.fasterxml.jackson.databind.util.ArrayBuilders.IntBuilder
import com.google.protobuf.Duration
import com.google.protobuf.kotlin.toByteString
import com.mlp.sdk.utils.JSON
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import jakarta.annotation.PostConstruct
import javafx.application.Application
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.opus.Opus.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.socket.WebSocketSession
import java.io.File
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

interface ASRSession {
    fun pushAudio(buffer: ByteArray)
}

interface ASRService {
    fun createASRSession(sessionId: String, cb: ASRSessionCallback): ASRSession
}

interface ASRSessionCallback {
    fun speech(text: String)
}


@Service
class SmartSpeech: ASRService{

    override fun createASRSession(sessionId: String, cb: ASRSessionCallback): ASRSession {
        val asrSession = ASRSessionImpl(sessionId)
        asrSession.callback = cb
        asrSession.run()
        return asrSession
    }

    class ASRSessionImpl(val sessionId: String):ASRSession{

        lateinit var callback: ASRSessionCallback

        private val accessToken = getAccessTokenForSber()
        private val channelBuilder = ManagedChannelBuilder
            .forTarget("smartspeech.sber.ru")
            .enableRetry()
        private val newChannel = channelBuilder.build()
        private val ss = SmartSpeechGrpcKt.SmartSpeechCoroutineStub(newChannel)

        private val queue = ArrayBlockingQueue<ByteArray>(1000)

        override fun pushAudio(buffer: ByteArray) {
            queue.put(buffer)
        }

        fun run () {
            transcribeVoice()
        }

        private val opts = SaluteAsrv2.RecognitionOptions.newBuilder().also {opts ->
            opts.setAudioEncoding(SaluteAsrv2.RecognitionOptions.AudioEncoding.PCM_S16LE)
            opts.setSampleRate(48000)
            opts.setEnablePartialResults(SaluteAsrv2.OptionalBool.newBuilder().setEnable(true))
            opts.setEnableVad(SaluteAsrv2.OptionalBool.newBuilder().setEnable(true))
            opts.setHints(SaluteAsrv2.Hints.newBuilder().setEouTimeout(Duration.newBuilder().setNanos(1000*1000000)))
        }.build()

        @Volatile var mute = false
        fun mute() {
            if (!mute) log.info("mute")
            mute = true
        }

        fun unmute() {
            if (mute) log.info("unmute")
            mute = false
        }

        private fun transcribeVoice(){
            val requestFlow: Flow<SaluteAsrv2.RecognitionRequest> = flow {
                val initialRequest = SaluteAsrv2.RecognitionRequest.newBuilder()
                    .setOptions(opts)
                    .build()
                emit(initialRequest) // Emits the result of the request to the flow
                log.debug("asrFlow started")

                while (!mute) {
                    val ba = queue.poll()
                    if (ba != null) {
                        val req = SaluteAsrv2.RecognitionRequest.newBuilder()
                        req.setAudioChunk(ba.toByteString())
                        emit(req.build()) // Emits the result of the request to the flow
                    }
                }
            }

            val headers = Metadata()
            val key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
            headers.put(key, "Bearer $accessToken")
            GlobalScope.launch(Dispatchers.IO) {
                while (true) {
                    try {
                        while (mute) {
                            delay(100)
                        }
                        val res = ss.recognize(requests = requestFlow, headers = headers)

                        res.collect {
                            if (it.transcription != null) {
                                val t = it.transcription.resultsList.getOrNull(0)?.text
                                if (!t.isNullOrEmpty()) {
                                    if (it.transcription.eou) {
                                        callback.speech(t)
                                        println(t)
                                    } else {
                                        println(" ...... $t")
                                    }
                                }
                            }
                        }
                    } catch (e: Throwable) {
                        log.error("error in asr loop", e)
                        delay(100)
                    }
                }
            }
        }


        private fun getAccessTokenForSber(): String {
            val formBody = FormBody.Builder()
                .add("scope", "SALUTE_SPEECH_PERS")
                .build()

            val request = Request.Builder()
                .url("https://ngw.devices.sberbank.ru:9443/api/v2/oauth")
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .addHeader("RqUID", UUID.randomUUID().toString())
                .addHeader("Authorization", "Basic OWI5YzRkMDQtMmNjNy00Zjk0LWEzNDYtZDU1OGM4YmY4MjQ3OjcwNjFmOGVmLTY0ZTgtNDcxNy1hYzZjLTk2OTkwYzdiNTRlZg==")
                .build()

            val client = OkHttpClient.Builder().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                println("Cannot get new oauth token, response code: ${response.code}, response body: ${response.body?.string()}")
                throw RuntimeException("Unexpected code ${response.code}")
            }

            val responseData = requireNotNull(response.body) { "Response body is null ${response.body}" }
            return JSON.parse(responseData.string())["access_token"]!!.asText()
        }
    }


    companion object {
        val log = LoggerFactory.getLogger(SmartSpeech::class.java)
    }

}
