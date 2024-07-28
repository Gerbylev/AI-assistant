package cc.config

import cc.endpoint.webSocket.MyWebSocketHandler
import cc.services.VoiceControllerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val voiceFactory: VoiceControllerFactory,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(MyWebSocketHandler(voiceFactory), "/ws").setAllowedOrigins("*")
    }
}