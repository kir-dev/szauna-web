package hu.kirdev.szaunaweb

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/test")
class TestController {

    @GetMapping("/me")
    fun getMyProfile(@AuthenticationPrincipal principal: String): Map<String, Any> {
        return mapOf(
            "message" to "Sikeres hitelesítés a JWT cookie-val!",
            "authSub" to principal
        )
    }

    @GetMapping("/admin")
    fun getAdminSecret(): Map<String, String> {
        return mapOf("secret" to "Csak adminok láthatják ezt a szauna beállítást.")
    }
}