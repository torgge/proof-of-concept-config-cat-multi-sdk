package com.example.configcat.model

import com.fasterxml.jackson.annotation.JsonProperty

data class UserResponse(
    val userId: String,
    val email: String,
    val profile: UserProfile,
    val features: UserFeatures,
    val metadata: ResponseMetadata,
)

data class UserProfile(
    val firstName: String,
    val lastName: String,
    val preferredLanguage: String,
    val avatarUrl: String?,
)

data class UserFeatures(
    val betaFeaturesEnabled: Boolean,
    val premiumAccount: Boolean,
    val uiVersion: String,
    val maxFileUploadSize: Long,
)

data class PaymentResponse(
    val transactionId: String,
    val amount: Double,
    val currency: String,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val features: PaymentFeatures,
    val metadata: ResponseMetadata,
)

data class PaymentMethod(
    val type: String,
    val processor: String,
    val last4Digits: String?,
)

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class PaymentFeatures(
    val expressCheckoutEnabled: Boolean,
    val recurringPaymentsEnabled: Boolean,
    val fraudDetectionLevel: String,
    val paymentMethodsAvailable: List<String>,
)

data class ResponseMetadata(
    val correlationId: String,
    val timestamp: String,
    @JsonProperty("configcat_evaluations")
    val configCatEvaluations: List<FeatureFlagEvaluation>,
)

data class FeatureFlagEvaluation(
    val project: String,
    val flagKey: String,
    val value: Any,
    val evaluatedAt: String,
)
