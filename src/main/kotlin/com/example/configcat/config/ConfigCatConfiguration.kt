package com.example.configcat.config

import com.configcat.ConfigCatClient
import com.configcat.LogLevel
import com.configcat.PollingModes
import mu.KotlinLogging
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

private val logger = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(ConfigCatProperties::class)
class ConfigCatConfiguration(
    private val configCatProperties: ConfigCatProperties,
) {
    @Bean
    @Primary
    fun userManagementConfigCatClient(): ConfigCatClient {
        logger.info { "Initializing ConfigCat client for User Management project: ${configCatProperties.userManagement.sdkKey.take(8)}..." }

        return ConfigCatClient.get(configCatProperties.userManagement.sdkKey) { options ->
            options.pollingMode(PollingModes.autoPoll(configCatProperties.userManagement.pollIntervalSeconds.toInt()))
            options.logLevel(LogLevel.valueOf(configCatProperties.logLevel))
        }
    }

    @Bean
    fun paymentConfigCatClient(): ConfigCatClient {
        logger.info { "Initializing ConfigCat client for Payment project: ${configCatProperties.payment.sdkKey.take(8)}..." }

        return ConfigCatClient.get(configCatProperties.payment.sdkKey) { options ->
            options.pollingMode(PollingModes.autoPoll(configCatProperties.payment.pollIntervalSeconds.toInt()))
            options.logLevel(LogLevel.valueOf(configCatProperties.logLevel))
        }
    }
}

@ConfigurationProperties(prefix = "configcat")
data class ConfigCatProperties(
    val logLevel: String = "INFO",
    val userManagement: ProjectConfig = ProjectConfig(),
    val payment: ProjectConfig = ProjectConfig(),
) {
    data class ProjectConfig(
        val sdkKey: String = "",
        val pollIntervalSeconds: Long = 30,
    )
}
