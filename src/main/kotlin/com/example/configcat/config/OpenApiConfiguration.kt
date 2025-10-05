package com.example.configcat.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("ConfigCat Multi-SDK Demo API")
                    .version("1.0.0")
                    .description(
                        """
                        # ConfigCat Multi-SDK Integration Demo

                        This API demonstrates a Spring Boot application with dual ConfigCat project integration,
                        showcasing how to use two separate ConfigCat projects within a single application.

                        ## Features
                        - **Dual ConfigCat Integration**: Separate SDK clients for User Management and Payment projects
                        - **Feature Flag Evaluation**: Context-aware feature flag evaluation with user targeting
                        - **Comprehensive MDC Logging**: Automatic correlation ID tracking and request context logging
                        - **Structured JSON Logging**: All logs include ConfigCat evaluation context

                        ## ConfigCat Projects
                        - **User Management**: Controls user-related features (beta features, premium accounts, UI version)
                        - **Payment**: Controls payment-related features (express checkout, recurring payments, fraud detection)

                        ## Authentication
                        No authentication required for this demo API.

                        ## Custom Headers
                        - `X-User-Country`: User's country code for targeting (e.g., "US", "BR")
                        - `X-User-Subscription`: User's subscription tier (e.g., "premium", "basic")
                        - `X-Payment-Provider`: Payment provider for targeting (e.g., "stripe", "paypal")
                        - `X-Correlation-ID`: Optional correlation ID (auto-generated if not provided)
                        """.trimIndent(),
                    )
                    .contact(
                        Contact()
                            .name("ConfigCat Multi-SDK Demo")
                            .url("https://github.com/torgge/proof-of-concept-config-cat-multi-sdk"),
                    )
                    .license(
                        License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT"),
                    ),
            )
            .servers(
                listOf(
                    Server()
                        .url("http://localhost:8080")
                        .description("Local Development Server"),
                    Server()
                        .url("http://localhost:8080")
                        .description("Docker Environment"),
                ),
            )
    }
}
