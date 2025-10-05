package com.example.configcat.controller

import com.example.configcat.model.*
import com.example.configcat.service.FeatureToggleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@Tag(
    name = "Payment Processing",
    description = "Payment endpoints with ConfigCat feature flag integration for payment-related features",
)
@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val featureToggleService: FeatureToggleService,
) {
    @Operation(
        summary = "Process a payment with feature flags",
        description = """
            Processes a payment request with dynamically evaluated feature flags from the
            Payment ConfigCat project. Payment methods and fraud detection levels are determined
            based on user attributes such as country and payment provider.

            **Feature Flags Evaluated:**
            - `express_checkout_enabled`: Whether express checkout is available
            - `recurring_payments_enabled`: Whether recurring/subscription payments are supported
            - `fraud_detection_level`: Fraud detection level (standard, high, strict)

            **Dynamic Payment Methods:**
            The available payment methods vary by country and feature flags:
            - **Brazil (BR)**: PIX, Boleto, credit/debit cards
            - **United States (US)**: PayPal, Apple Pay, Google Pay, credit/debit cards
            - **Express Checkout**: One-click payment (when enabled)
            - **Recurring**: Subscription payment (when enabled and isRecurring=true)

            The response includes complete payment details, available methods, and all feature flag
            evaluation results with timestamps.
        """,
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Payment processed successfully with evaluated feature flags",
                content = [
                    Content(
                        mediaType = "application/json",
                        schema = Schema(implementation = PaymentResponse::class),
                        examples = [
                            ExampleObject(
                                name = "Payment in Brazil with Express Checkout",
                                value = """{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 99.99,
  "currency": "BRL",
  "method": {
    "type": "credit_card",
    "processor": "stripe_brazil",
    "last4Digits": "4242"
  },
  "status": "COMPLETED",
  "features": {
    "expressCheckoutEnabled": true,
    "recurringPaymentsEnabled": true,
    "fraudDetectionLevel": "high",
    "paymentMethodsAvailable": ["credit_card", "debit_card", "pix", "boleto", "one_click_payment"]
  },
  "metadata": {
    "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "timestamp": "2025-10-05T10:30:00.123Z",
    "configcat_evaluations": [
      {
        "project": "payment",
        "flagKey": "express_checkout_enabled",
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
    @PostMapping("/process")
    fun processPayment(
        @SwaggerRequestBody(
            description = "Payment request details",
            required = true,
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = PaymentRequest::class),
                    examples = [
                        ExampleObject(
                            name = "Credit Card Payment",
                            value = """{
  "amount": 99.99,
  "currency": "USD",
  "paymentMethod": "credit_card",
  "cardNumber": "4242424242424242",
  "isRecurring": false
}""",
                        ),
                        ExampleObject(
                            name = "Recurring Subscription Payment",
                            value = """{
  "amount": 29.99,
  "currency": "BRL",
  "paymentMethod": "credit_card",
  "cardNumber": "5555555555554444",
  "isRecurring": true
}""",
                        ),
                    ],
                ),
            ],
        )
        @RequestBody
        request: PaymentRequest,
        @Parameter(description = "User identifier for feature targeting", example = "user123")
        @RequestHeader(value = "X-User-ID", required = false)
        userId: String?,
        @Parameter(
            description = "User's country code for payment method availability",
            example = "BR",
            schema = Schema(allowableValues = ["US", "BR", "UK", "DE"]),
        )
        @RequestHeader(value = "X-User-Country", required = false)
        country: String?,
        @Parameter(
            description = "Payment provider preference",
            example = "stripe",
            schema = Schema(allowableValues = ["stripe", "paypal", "adyen"]),
        )
        @RequestHeader(value = "X-Payment-Provider", required = false)
        paymentProvider: String?,
    ): ResponseEntity<PaymentResponse> {
        val correlationId = MDC.get("correlationId") ?: "unknown"

        logger.info {
            "Processing payment: amount=${request.amount}, currency=${request.currency}, " +
                "userId=$userId, country=$country, correlationId=$correlationId"
        }

        val userAttributes =
            buildMap {
                country?.let { put("country", it) }
                paymentProvider?.let { put("payment_provider", it) }
                put("amount_range", getAmountRange(request.amount))
                put("currency", request.currency)
            }

        val evaluations = mutableListOf<FeatureFlagEvaluation>()
        val timestamp = Instant.now().toString()

        // Evaluate Payment features
        val expressCheckoutEnabled =
            featureToggleService.isPaymentFeatureEnabled(
                flagKey = "express_checkout_enabled",
                userId = userId,
                attributes = userAttributes,
            )
        evaluations.add(FeatureFlagEvaluation("payment", "express_checkout_enabled", expressCheckoutEnabled, timestamp))

        val recurringPaymentsEnabled =
            featureToggleService.isPaymentFeatureEnabled(
                flagKey = "recurring_payments_enabled",
                userId = userId,
                attributes = userAttributes,
            )
        evaluations.add(FeatureFlagEvaluation("payment", "recurring_payments_enabled", recurringPaymentsEnabled, timestamp))

        val fraudDetectionLevel =
            featureToggleService.getPaymentStringFlag(
                flagKey = "fraud_detection_level",
                defaultValue = "standard",
                userId = userId,
                attributes = userAttributes,
            )
        evaluations.add(FeatureFlagEvaluation("payment", "fraud_detection_level", fraudDetectionLevel, timestamp))

        val paymentMethodsAvailable =
            buildList {
                add("credit_card")
                add("debit_card")

                if (country == "BR") {
                    add("pix")
                    add("boleto")
                }

                if (country == "US") {
                    add("paypal")
                    add("apple_pay")
                    add("google_pay")
                }

                if (expressCheckoutEnabled) {
                    add("one_click_payment")
                }

                if (recurringPaymentsEnabled && request.isRecurring) {
                    add("subscription_payment")
                }
            }

        val paymentProcessor =
            when {
                country == "BR" && paymentProvider == "stripe" -> "stripe_brazil"
                country == "US" && expressCheckoutEnabled -> "stripe_express"
                fraudDetectionLevel == "high" -> "secure_processor"
                else -> "default_processor"
            }

        val transactionId = UUID.randomUUID().toString()
        val paymentStatus = simulatePaymentProcessing(request, fraudDetectionLevel)

        val response =
            PaymentResponse(
                transactionId = transactionId,
                amount = request.amount,
                currency = request.currency,
                method =
                    PaymentMethod(
                        type = request.paymentMethod,
                        processor = paymentProcessor,
                        last4Digits = request.cardNumber?.takeLast(4),
                    ),
                status = paymentStatus,
                features =
                    PaymentFeatures(
                        expressCheckoutEnabled = expressCheckoutEnabled,
                        recurringPaymentsEnabled = recurringPaymentsEnabled,
                        fraudDetectionLevel = fraudDetectionLevel,
                        paymentMethodsAvailable = paymentMethodsAvailable,
                    ),
                metadata =
                    ResponseMetadata(
                        correlationId = correlationId,
                        timestamp = timestamp,
                        configCatEvaluations = evaluations,
                    ),
            )

        logger.info {
            "Payment processed: transactionId=$transactionId, status=$paymentStatus, " +
                "processor=$paymentProcessor, fraudLevel=$fraudDetectionLevel, " +
                "expressCheckout=$expressCheckoutEnabled, correlationId=$correlationId"
        }

        return ResponseEntity.ok(response)
    }

    private fun getAmountRange(amount: Double): String {
        return when {
            amount < 50.0 -> "low"
            amount < 500.0 -> "medium"
            amount < 5000.0 -> "high"
            else -> "very_high"
        }
    }

    private fun simulatePaymentProcessing(
        request: PaymentRequest,
        fraudDetectionLevel: String,
    ): PaymentStatus {
        // Simulate different outcomes based on fraud detection level
        return when (fraudDetectionLevel) {
            "high" -> {
                // High fraud detection might delay processing
                if (request.amount > 1000.0) PaymentStatus.PENDING else PaymentStatus.COMPLETED
            }
            "strict" -> {
                // Strict mode might require additional verification
                PaymentStatus.PENDING
            }
            else -> {
                // Standard processing
                PaymentStatus.COMPLETED
            }
        }
    }
}

@Schema(description = "Payment request details")
data class PaymentRequest(
    @Schema(description = "Payment amount", example = "99.99", minimum = "0.01")
    val amount: Double,
    @Schema(description = "Currency code (ISO 4217)", example = "USD")
    val currency: String,
    @Schema(description = "Payment method type", example = "credit_card")
    val paymentMethod: String,
    @Schema(description = "Card number (for card payments)", example = "4242424242424242", nullable = true)
    val cardNumber: String? = null,
    @Schema(description = "Whether this is a recurring/subscription payment", example = "false")
    val isRecurring: Boolean = false,
)
