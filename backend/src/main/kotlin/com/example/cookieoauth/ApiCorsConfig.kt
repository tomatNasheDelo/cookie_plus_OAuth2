package com.example.cookieoauth

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ApiCorsConfig(@Value("\${app.cors.allowed-origins}") private val origins: String) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(*origins.split(',').map(String::trim).filter(String::isNotBlank).toTypedArray())
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowedHeaders("Content-Type", "X-XSRF-TOKEN")
            .allowCredentials(true)
            .maxAge(3600)
    }
}
