package com.example.configcat.controller

import com.example.configcat.model.*
import com.example.configcat.service.FeatureToggleService
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/users")
class UserController(
    private val featureToggleService: FeatureToggleService,
) {
    @GetMapping("/{userId}")
    fun getUserProfile(
        @PathVariable userId: String,
        @RequestParam(required = false) email: String?,
        @RequestHeader(value = "X-User-Country", required = false) country: String?,
        @RequestHeader(value = "X-User-Subscription", required = false) subscription: String?,
    ): ResponseEntity<UserResponse> {
        val correlationId = MDC.get("correlationId") ?: "unknown"

        logger.info { "Getting user profile for userId=$userId, correlationId=$correlationId" }

        val userAttributes =
            buildMap {
                country?.let { put("country", it) }
                subscription?.let { put("subscription", it) }
            }

        val evaluations = mutableListOf<FeatureFlagEvaluation>()
        val timestamp = Instant.now().toString()

        // Evaluate User Management features
        val betaFeaturesEnabled =
            featureToggleService.isUserManagementFeatureEnabled(
                flagKey = "beta_features_enabled",
                userId = userId,
                email = email,
                attributes = userAttributes,
            )
        evaluations.add(FeatureFlagEvaluation("user-management", "beta_features_enabled", betaFeaturesEnabled, timestamp))

        val premiumAccount =
            featureToggleService.isUserManagementFeatureEnabled(
                flagKey = "premium_account_features",
                userId = userId,
                email = email,
                attributes = userAttributes,
            )
        evaluations.add(FeatureFlagEvaluation("user-management", "premium_account_features", premiumAccount, timestamp))

        val uiVersion =
            featureToggleService.getUserManagementStringFlag(
                flagKey = "ui_version",
                defaultValue = "v1",
                userId = userId,
                email = email,
                attributes = userAttributes,
            )
        evaluations.add(FeatureFlagEvaluation("user-management", "ui_version", uiVersion, timestamp))

        // Dynamic file upload size based on subscription
        val maxFileUploadSize =
            when {
                premiumAccount -> 100L * 1024 * 1024 // 100MB for premium
                betaFeaturesEnabled -> 50L * 1024 * 1024 // 50MB for beta users
                else -> 10L * 1024 * 1024 // 10MB for regular users
            }

        val response =
            UserResponse(
                userId = userId,
                email = email ?: "user$userId@example.com",
                profile =
                    UserProfile(
                        firstName = "User",
                        lastName = userId,
                        preferredLanguage = if (country == "BR") "pt-BR" else "en-US",
                        avatarUrl = if (premiumAccount) "https://premium-avatars.example.com/$userId" else null,
                    ),
                features =
                    UserFeatures(
                        betaFeaturesEnabled = betaFeaturesEnabled,
                        premiumAccount = premiumAccount,
                        uiVersion = uiVersion,
                        maxFileUploadSize = maxFileUploadSize,
                    ),
                metadata =
                    ResponseMetadata(
                        correlationId = correlationId,
                        timestamp = timestamp,
                        configCatEvaluations = evaluations,
                    ),
            )

        logger.info {
            "User profile response generated: userId=$userId, " +
                "betaFeatures=$betaFeaturesEnabled, premium=$premiumAccount, " +
                "uiVersion=$uiVersion, correlationId=$correlationId"
        }

        return ResponseEntity.ok(response)
    }
}
