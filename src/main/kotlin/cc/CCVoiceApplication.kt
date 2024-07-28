package cc

import cc.services.AppUI
import cc.utils.consoleRunner
import javafx.application.Application
import javafx.stage.Stage
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan


@SpringBootApplication
@ComponentScan(basePackages = ["cc"])
class SpringApp


class App: Application() {
    override fun start(primaryStage: Stage) {
        app.start(primaryStage)
    }

    companion object {
        lateinit var app: AppUI
    }
}

object StartAppUI {
    @JvmStatic
    fun main(args: Array<String>) {
    val cfg = System.getProperty("conf") ?: "src/main/conf/voice-demo.yml"
    val context = consoleRunner(cfg)

    App.app = context.getBean(AppUI::class.java)

    Application.launch(App::class.java)
    }
}

object StartWebSocketUI{
    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("spring.profiles.active", "dev,local")
        System.setProperty("spring.config.location", "src/main/conf/")
        System.setProperty("config.location", "src/main/conf/")
        System.setProperty("logging.config", "src/main/conf/logback.xml")
        System.setProperty("log.dir", "/tmp/")
        System.setProperty("user.timezone", "UTC")
        System.setProperty("file.encoding", "UTF-8")
        val springApp = SpringApplication.run(SpringApp::class.java, *args)
    }
}
