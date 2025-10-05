package com.example.configcat.controller

import com.example.configcat.model.*
import com.example.configcat.service.FeatureToggleService
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val featureToggleService: FeatureToggleService,
) {
    @PostMapping("/process")
    fun processPayment(
        @RequestBody request: PaymentRequest,
        @RequestHeader(value = "X-User-ID", required = false) userId: String?,
        @RequestHeader(value = "X-User-Country", required = false) country: String?,
        @RequestHeader(value = "X-Payment-Provider", required = false) paymentProvider: String?,
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

data class PaymentRequest(
    val amount: Double,
    val currency: String,
    val paymentMethod: String,
    val cardNumber: String? = null,
    val isRecurring: Boolean = false,
)
