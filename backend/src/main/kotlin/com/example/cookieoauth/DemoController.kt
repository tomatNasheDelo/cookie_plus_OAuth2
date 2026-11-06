package com.example.cookieoauth

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.web.csrf.CsrfToken
import java.time.Instant

@RestController
class DemoController(private val authorizedClientManager: OAuth2AuthorizedClientManager) {
    @GetMapping("/api/csrf")
    fun csrf(token: CsrfToken): CsrfResponse = CsrfResponse(token.token, token.headerName)

    @GetMapping("/api/me")
    fun me(@AuthenticationPrincipal user: OidcUser?): UserResponse = if (user == null) {
        UserResponse(false, null, null)
    } else {
        UserResponse(true, user.name, user.email)
    }

    @GetMapping("/api/oauth/refresh-check")
    fun refreshCheck(@AuthenticationPrincipal user: OidcUser): TokenStatusResponse {
        val authorized = authorizedClientManager.authorize(
            OAuth2AuthorizeRequest.withClientRegistrationId("keycloak").principal(user).build(),
        ) ?: error("OAuth2 client authorization unavailable")
        val token = authorized.accessToken
        return TokenStatusResponse(
            "Backend использовал OAuth2AuthorizedClientManager. Если access token истёк, он попробует обновить его refresh token.",
            token.expiresAt,
            authorized.refreshToken != null,
        )
    }

    @PostMapping("/api/demo/change-state")
    fun changeState(): DemoActionResponse = DemoActionResponse(
        "Изменяющий запрос принят. Spring Security проверил CSRF header до входа в controller.",
    )
}

data class CsrfResponse(val token: String, val headerName: String)
data class UserResponse(val authenticated: Boolean, val name: String?, val email: String?)
data class TokenStatusResponse(val note: String, val accessTokenExpiresAt: Instant?, val refreshTokenPresent: Boolean)
data class DemoActionResponse(val message: String)
