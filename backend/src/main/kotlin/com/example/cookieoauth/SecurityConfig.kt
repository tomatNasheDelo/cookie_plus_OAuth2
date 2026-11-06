package com.example.cookieoauth

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.OAuth2AuthorizeRequest
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusReturningLogoutSuccessHandler
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler

@Configuration
class SecurityConfig(
    @Value("\${app.frontend-url}") private val frontendUrl: String,
    @Value("\${app.cors.allowed-origins}") private val allowedOrigins: String,
    @Value("\${server.servlet.session.cookie.secure:false}") private val secureCookie: Boolean,
    @Value("\${server.servlet.session.cookie.same-site:Lax}") private val sameSite: String,
) {
    @Bean
    fun csrfRepository(): CookieCsrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse().apply {
        setCookieCustomizer { cookie -> cookie.path("/").secure(secureCookie).sameSite(sameSite) }
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity, csrfRepository: CookieCsrfTokenRepository): SecurityFilterChain {
        val requestHandler = CsrfTokenRequestAttributeHandler()
        http
            .cors(Customizer.withDefaults())
            .csrf { csrf -> csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(requestHandler) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/", "/error", "/api/csrf", "/api/me", "/oauth2/**", "/login/**").permitAll()
                    .requestMatchers("/api/oauth/**").authenticated()
                    .anyRequest().permitAll()
            }
            .oauth2Login { oauth ->
                oauth.successHandler(SimpleUrlAuthenticationSuccessHandler(frontendUrl))
            }
            .logout { logout ->
                logout.logoutUrl("/api/logout")
                    .logoutSuccessHandler(HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
            }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = this@SecurityConfig.allowedOrigins.split(',').map(String::trim).filter(String::isNotBlank)
            allowedMethods = listOf("GET", "POST", "OPTIONS")
            allowedHeaders = listOf("Content-Type", "X-XSRF-TOKEN")
            allowCredentials = true
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/api/**", config) }
    }

    @Bean
    fun authorizedClientManager(
        registrations: ClientRegistrationRepository,
        authorizedClients: OAuth2AuthorizedClientService,
    ): OAuth2AuthorizedClientManager {
        val provider = OAuth2AuthorizedClientProviderBuilder.builder()
            .authorizationCode()
            .refreshToken()
            .build()
        return AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClients).apply {
            setAuthorizedClientProvider(provider)
        }
    }
}
