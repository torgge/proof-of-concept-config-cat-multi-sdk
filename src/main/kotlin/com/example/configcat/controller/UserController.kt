package com.example.configcat.controller

import com.example.configcat.model.*
import com.example.configcat.service.FeatureToggleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

private val logger = KotlinLogging.logger {}

@Tag(
    name = "User Management",
    description = "User profile endpoints with ConfigCat feature flag integration for user-related features",
)
@RestController
@RequestMapping("/api/users")
class UserController(
    private val featureToggleService: FeatureToggleService,
) {
    @Operation(
        summary = "Get user profile with feature flags",
        description = """
            Retrieves user profile information with dynamically evaluated feature flags from the
            User Management ConfigCat project. Features are evaluated based on user attributes
            such as country and subscription tier.

            **Feature Flags Evaluated:**
            - `beta_features_enabled`: Whether beta features are available for the user
            - `premium_account_features`: Whether premium account features are enabled
            - `ui_version`: Which UI version to display (e.g., "v1", "v2")

            The response includes feature flag evaluation results with timestamps and the correlation ID
            for request tracing across logs.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User profile retrieved successfully with evaluated feature flags",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = UserResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Premium User in Brazil",
                                value = """{
  "userId": "user123",
  "email": "[email protected]",
  "profile": {
    "firstName": "User",
    "lastName": "user123",
    "preferredLanguage": "pt-BR",
    "avatarUrl": "https://premium-avatars.example.com/user123"
  },
  "features": {
    "betaFeaturesEnabled": true,
    "premiumAccount": true,
    "uiVersion": "v2",
    "maxFileUploadSize": 104857600
  },
  "metadata": {
    "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "timestamp": "2025-10-05T10:30:00.123Z",
    "configcat_evaluations": [
      {
        "project": "user-management",
        "flagKey": "beta_features_enabled",
        "value": true,
        "evaluatedAt": "2025-10-05T10:30:00.123Z"
      }
    ]
  }
}""",
                            ),
                        ],
                    ),
                ],
            ),
        ],
    )
    @GetMapping("/{userId}")
    fun getUserProfile(
        @Parameter(description = "Unique user identifier", example = "user123", required = true)
        @PathVariable
        userId: String,
        @Parameter(description = "User's email address", example = "[email protected]")
        @RequestParam(required = false)
        email: String?,
        @Parameter(
            description = "User's country code for feature targeting",
            example = "BR",
            schema = Schema(allowableValues = ["US", "BR", "UK", "DE"]),
        )
        @RequestHeader(value = "X-User-Country", required = false)
        country: String?,
        @Parameter(
            description = "User's subscription tier",
            example = "premium",
            schema = Schema(allowableValues = ["basic", "premium", "enterprise"]),
        )
        @RequestHeader(value = "X-User-Subscription", required = false)
        subscription: String?,
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
