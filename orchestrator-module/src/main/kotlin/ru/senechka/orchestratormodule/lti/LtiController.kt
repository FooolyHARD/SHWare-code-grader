package ru.senechka.orchestratormodule.lti

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/lti")
class LtiController(
    private val jwtService: JwtService,
    private val ltiLaunchService: LtiLaunchService
) {

    @GetMapping("/jwks")
    fun getJwks(): ResponseEntity<Map<String, Any>> {
        val jwk = jwtService.getPublicJwk()
        return ResponseEntity.ok(mapOf("keys" to listOf(jwk)))
    }

    @GetMapping("/login")
    fun initiateLogin(
        @RequestParam("iss") issuer: String,
        @RequestParam("login_hint") loginHint: String,
        @RequestParam("target_link_uri") targetLinkUri: String,
        @RequestParam("lti_message_hint", required = false) messageHint: String?
    ): ResponseEntity<Void> {
        val redirectUri = UriComponentsBuilder
            .fromHttpUrl("http://172.19.0.7:8080/api/v1/sso/authorize")
            .queryParam("redirectUrl", "http://localhost:3000/api/lti/launch")
            .build()
            .toUri()
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build()
    }

    @PostMapping("/launch")
    fun handleLaunch(
        request: HttpServletRequest,
        response: HttpServletResponse
    ) {
        val idToken = request.getParameter("id_token")

        if (idToken == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "id_token отсутствует")
            return
        }
        response.sendRedirect("http://localhost:3000/api/frontpage")
    }

}
