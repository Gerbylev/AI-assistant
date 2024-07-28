package cc.endpoint

import cc.App
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/api")
class ChatEndpoint {

    @GetMapping("/chat")
    fun getMessage(@RequestParam message: String): ResponseEntity<Unit> {
//        App.app.sendMessage(message)
        return ResponseEntity(Unit, HttpStatus.valueOf(200))
    }
}