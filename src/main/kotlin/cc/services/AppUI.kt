package cc.services

import ELResponse
import TTSResult
import cc.services.connectors.MyAudioLine
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlinx.coroutines.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import javax.sound.sampled.AudioSystem


@OptIn(DelicateCoroutinesApi::class)
@Service
@ConditionalOnProperty("AppUI")
final class AppUI(
    val voiceFactory: VoiceControllerFactory,
    val myAudioLine: MyAudioLine,
    ): VoiceControllerCallback {
    lateinit var userTextField: TextField
    lateinit var historyField: TextArea
    lateinit var sendButton: Button

    val voiceController = voiceFactory.createVoiceController("app-ui", this);



    fun start(primaryStage: Stage) {
        val vbox = VBox(5.0)

        val scene = Scene(vbox, 500.0, 800.0)
        primaryStage.scene = scene
        primaryStage.title = "CC Agent!"

        vbox.children.add(Label("Dialog:"))

        historyField = TextArea()
        historyField.isEditable = false
        historyField.isWrapText = true
        vbox.children.add(historyField)

        VBox.setVgrow(historyField, Priority.ALWAYS)

        val userName = Label("User:")
        vbox.children.add(userName)

        userTextField = TextField()
        userTextField.text = ""
        vbox.children.add(userTextField)

        sendButton = Button()
        sendButton.text = "Send"
        sendButton.setOnAction {
            sendMessage(userTextField.text)
        }
        userTextField.setOnKeyPressed {
            if (it.code == KeyCode.ENTER) {
                sendMessage(userTextField.text)
            }
        }
        vbox.children.add(sendButton)

        userTextField.requestFocus()
        primaryStage.show()

        myAudioLine.getAudioStream(voiceController)
//        asr.start()

        sendMessage("Привет")
    }

    override fun asrResult(text: String) {
        if (state != "idle") {
            runBlocking {
//                voiceController.interrupt()
//                state = "idle"
//                sendButton.text = "Send!"
            }
        }
        sendMessage(text)
    }

    @Volatile private var state = "idle"
    fun sendMessage(query: String) {
        if (state == "idle") {
            appendAndScroll( "user: $query\n")
            userTextField.text = ""

            GlobalScope.launch(Dispatchers.Main) {
//                voiceController.user(query)
            }
            state = "busy"
        } else {
            GlobalScope.launch(Dispatchers.Main) {
//                voiceController.interrupt()
//                state = "idle"
//                sendButton.text = "Send!"
            }
        }
    }

    override fun startGeneration() {
        appendAndScroll("bot: ")
//        sendButton.text = "Stop <>"
    }

    override suspend fun ttsResult(ttsResult: TTSResult) {
        myAudioLine.audioOutput(ttsResult)
    }

    override fun delta(text: String) {
        appendAndScroll(text)
    }

    override fun textGenerationComplete() {
        appendAndScroll("\n\n")
    }

    override fun audioComplete() {
        GlobalScope.launch(Dispatchers.Main) {
            sendButton.text = "Send!"
            state = "idle"
        }
    }

    private fun appendAndScroll(text: String) {
        historyField.text += text
        historyField.scrollTop = Double.MAX_VALUE
    }
}
