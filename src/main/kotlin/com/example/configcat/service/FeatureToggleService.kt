package com.example.configcat.service

import com.configcat.ConfigCatClient
import com.configcat.User
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class FeatureToggleService(
    @Qualifier("userManagementConfigCatClient") private val userManagementClient: ConfigCatClient,
    @Qualifier("paymentConfigCatClient") private val paymentClient: ConfigCatClient,
) {
    fun isUserManagementFeatureEnabled(
        flagKey: String,
        userId: String? = null,
        email: String? = null,
        attributes: Map<String, String> = emptyMap(),
    ): Boolean {
        return evaluateFlag(
            client = userManagementClient,
            project = "user-management",
            flagKey = flagKey,
            userId = userId,
            email = email,
            attributes = attributes,
        )
    }

    fun getUserManagementStringFlag(
        flagKey: String,
        defaultValue: String,
        userId: String? = null,
        email: String? = null,
        attributes: Map<String, String> = emptyMap(),
    ): String {
        return evaluateStringFlag(
            client = userManagementClient,
            project = "user-management",
            flagKey = flagKey,
            defaultValue = defaultValue,
            userId = userId,
            email = email,
            attributes = attributes,
        )
    }

    fun isPaymentFeatureEnabled(
        flagKey: String,
        userId: String? = null,
        email: String? = null,
        attributes: Map<String, String> = emptyMap(),
    ): Boolean {
        return evaluateFlag(
            client = paymentClient,
            project = "payment",
            flagKey = flagKey,
            userId = userId,
            email = email,
            attributes = attributes,
        )
    }

    fun getPaymentStringFlag(
        flagKey: String,
        defaultValue: String,
        userId: String? = null,
        email: String? = null,
        attributes: Map<String, String> = emptyMap(),
    ): String {
        return evaluateStringFlag(
            client = paymentClient,
            project = "payment",
            flagKey = flagKey,
            defaultValue = defaultValue,
            userId = userId,
            email = email,
            attributes = attributes,
        )
    }

    private fun evaluateFlag(
        client: ConfigCatClient,
        project: String,
        flagKey: String,
        userId: String?,
        email: String?,
        attributes: Map<String, String>,
    ): Boolean {
        val correlationId = MDC.get("correlationId") ?: "unknown"

        MDC.put("configcat.project", project)
        MDC.put("configcat.flag", flagKey)

        return try {
            val user = createUser(userId, email, attributes)
            val result = client.getValue(Boolean::class.java, flagKey, user, false)

            logger.info {
                "Feature flag evaluated: project=$project, flag=$flagKey, userId=$userId, result=$result, correlationId=$correlationId"
            }

            MDC.put("configcat.result", result.toString())
            result
        } catch (e: Exception) {
            logger.error(e) {
                "Error evaluating feature flag: project=$project, flag=$flagKey, userId=$userId, correlationId=$correlationId"
            }
            false
        } finally {
            MDC.remove("configcat.project")
            MDC.remove("configcat.flag")
            MDC.remove("configcat.result")
        }
    }

    private fun evaluateStringFlag(
        client: ConfigCatClient,
        project: String,
        flagKey: String,
        defaultValue: String,
        userId: String?,
        email: String?,
        attributes: Map<String, String>,
    ): String {
        val correlationId = MDC.get("correlationId") ?: "unknown"

        MDC.put("configcat.project", project)
        MDC.put("configcat.flag", flagKey)

        return try {
            val user = createUser(userId, email, attributes)
            val result = client.getValue(String::class.java, flagKey, user, defaultValue)

            logger.info {
                "String flag evaluated: project=$project, flag=$flagKey, userId=$userId, result=$result, correlationId=$correlationId"
            }

            MDC.put("configcat.result", result)
            result
        } catch (e: Exception) {
            logger.error(e) {
                "Error evaluating string flag: project=$project, flag=$flagKey, userId=$userId, correlationId=$correlationId"
            }
            defaultValue
        } finally {
            MDC.remove("configcat.project")
            MDC.remove("configcat.flag")
            MDC.remove("configcat.result")
        }
    }

    private fun createUser(
        userId: String?,
        email: String?,
        attributes: Map<String, String>,
    ): User? {
        if (userId == null && email == null && attributes.isEmpty()) {
            return null
        }

        val userBuilder = User.newBuilder()

        email?.let { userBuilder.email(it) }

        if (attributes.isNotEmpty()) {
            userBuilder.custom(attributes.mapValues<String, String, Any> { it.value })
        }

        return userBuilder.build(userId ?: "anonymous")
    }
}
