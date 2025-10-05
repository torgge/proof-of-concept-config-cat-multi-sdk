package com.example.configcat.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "User profile response with evaluated feature flags")
data class UserResponse(
    @Schema(description = "Unique user identifier", example = "user123")
    val userId: String,
    @Schema(description = "User's email address", example = "[email protected]")
    val email: String,
    @Schema(description = "User profile information")
    val profile: UserProfile,
    @Schema(description = "Feature flags evaluated for this user")
    val features: UserFeatures,
    @Schema(description = "Request metadata including correlation ID and feature flag evaluations")
    val metadata: ResponseMetadata,
)

@Schema(description = "User profile information")
data class UserProfile(
    @Schema(description = "User's first name", example = "John")
    val firstName: String,
    @Schema(description = "User's last name", example = "Doe")
    val lastName: String,
    @Schema(description = "Preferred language code", example = "pt-BR")
    val preferredLanguage: String,
    @Schema(description = "URL to user's avatar image (premium users only)", example = "https://premium-avatars.example.com/user123", nullable = true)
    val avatarUrl: String?,
)

@Schema(description = "Feature flags evaluated for the user from User Management ConfigCat project")
data class UserFeatures(
    @Schema(description = "Whether beta features are enabled for this user", example = "true")
    val betaFeaturesEnabled: Boolean,
    @Schema(description = "Whether user has premium account features", example = "true")
    val premiumAccount: Boolean,
    @Schema(description = "UI version to display", example = "v2")
    val uiVersion: String,
    @Schema(description = "Maximum file upload size in bytes", example = "104857600")
    val maxFileUploadSize: Long,
)

@Schema(description = "Payment processing response with evaluated feature flags")
data class PaymentResponse(
    @Schema(description = "Unique transaction identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val transactionId: String,
    @Schema(description = "Payment amount", example = "99.99")
    val amount: Double,
    @Schema(description = "Currency code", example = "USD")
    val currency: String,
    @Schema(description = "Payment method details")
    val method: PaymentMethod,
    @Schema(description = "Current payment status", example = "COMPLETED")
    val status: PaymentStatus,
    @Schema(description = "Feature flags evaluated for this payment")
    val features: PaymentFeatures,
    @Schema(description = "Request metadata including correlation ID and feature flag evaluations")
    val metadata: ResponseMetadata,
)

@Schema(description = "Payment method information")
data class PaymentMethod(
    @Schema(description = "Payment method type", example = "credit_card")
    val type: String,
    @Schema(description = "Payment processor used", example = "stripe_brazil")
    val processor: String,
    @Schema(description = "Last 4 digits of card number", example = "4242", nullable = true)
    val last4Digits: String?,
)

@Schema(description = "Payment processing status")
enum class PaymentStatus {
    @Schema(description = "Payment is pending approval or processing")
    PENDING,

    @Schema(description = "Payment is currently being processed")
    PROCESSING,

    @Schema(description = "Payment completed successfully")
    COMPLETED,

    @Schema(description = "Payment failed")
    FAILED,

    @Schema(description = "Payment was cancelled")
    CANCELLED,
}

@Schema(description = "Feature flags evaluated for the payment from Payment ConfigCat project")
data class PaymentFeatures(
    @Schema(description = "Whether express checkout is enabled", example = "true")
    val expressCheckoutEnabled: Boolean,
    @Schema(description = "Whether recurring payments are enabled", example = "true")
    val recurringPaymentsEnabled: Boolean,
    @Schema(description = "Fraud detection level", example = "high")
    val fraudDetectionLevel: String,
    @Schema(description = "List of available payment methods for this transaction", example = "[\"credit_card\", \"pix\", \"boleto\"]")
    val paymentMethodsAvailable: List<String>,
)

@Schema(description = "Request metadata with correlation tracking and ConfigCat evaluation details")
data class ResponseMetadata(
    @Schema(description = "Correlation ID for request tracing", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    val correlationId: String,
    @Schema(description = "Request timestamp in ISO 8601 format", example = "2025-10-05T10:30:00.123Z")
    val timestamp: String,
    @Schema(description = "List of all ConfigCat feature flag evaluations performed for this request")
    @JsonProperty("configcat_evaluations")
    val configCatEvaluations: List<FeatureFlagEvaluation>,
)

@Schema(description = "Individual feature flag evaluation result")
data class FeatureFlagEvaluation(
    @Schema(description = "ConfigCat project name", example = "user-management")
    val project: String,
    @Schema(description = "Feature flag key", example = "beta_features_enabled")
    val flagKey: String,
    @Schema(description = "Evaluated value of the feature flag", example = "true")
    val value: Any,
    @Schema(description = "Timestamp when flag was evaluated", example = "2025-10-05T10:30:00.123Z")
    val evaluatedAt: String,
)
