# ConfigCat Dual Project Integration Demo

This is a proof-of-concept Spring Boot application that demonstrates how to integrate two separate ConfigCat projects within a single application, using MDC (Mapped Diagnostic Context) for comprehensive logging.

## Overview

The application showcases:
- **Dual ConfigCat Integration**: Two separate ConfigCat projects (User Management & Payment)
- **Feature Flag Evaluation**: Different feature toggles for different business domains
- **Two Environment Configuration**: Local and Production environments
- **Environment Variable Management**: Automatic .env file loading via Gradle
- **MDC Logging**: Comprehensive request tracing with correlation IDs
- **Structured Logging**: JSON-formatted logs with ConfigCat evaluation details
- **OpenAPI/Swagger Documentation**: Interactive API documentation with examples
- **Observability**: Prometheus metrics and Grafana dashboards

## Architecture

```
┌─────────────────────────────────────────┐
│  Spring Boot Application                │
│  ┌───────────────┐  ┌─────────────────┐ │
│  │ User Controller│  │Payment Controller│ │
│  └───────┬───────┘  └─────────┬───────┘ │
│          │                    │         │
│  ┌───────▼────────────────────▼───────┐ │
│  │    FeatureToggleService            │ │
│  └───────┬────────────────────┬───────┘ │
│          │                    │         │
│  ┌───────▼───────┐  ┌─────────▼───────┐ │
│  │UserMgmt Client│  │Payment Client   │ │
│  │(ConfigCat)    │  │(ConfigCat)      │ │
│  └───────────────┘  └─────────────────┘ │
└─────────────────────────────────────────┘
```

## Quick Start

### Prerequisites
- Java 21
- Gradle 8.5+
- Docker & Docker Compose (optional)

### 1. Configure Environment Variables

Copy the example file and add your SDK keys:
```bash
cp .env.example .env
```

Edit `.env` with your actual ConfigCat SDK keys:
```bash
# Spring Profile Configuration
SPRING_PROFILES_ACTIVE=local

# ConfigCat SDK Keys - Local Environment
CONFIGCAT_USER_MANAGEMENT_SDK_KEY_LOCAL=your-local-user-management-sdk-key
CONFIGCAT_PAYMENT_SDK_KEY_LOCAL=your-local-payment-sdk-key

# ConfigCat SDK Keys - Production Environment
CONFIGCAT_USER_MANAGEMENT_SDK_KEY_PRODUCTION=your-production-user-management-sdk-key
CONFIGCAT_PAYMENT_SDK_KEY_PRODUCTION=your-production-payment-sdk-key
```

### 2. Run the Application

The Gradle build is configured to automatically load environment variables from the `.env` file:

```bash
# Run with local environment (default)
./gradlew bootRun

# Run with production environment
# (change SPRING_PROFILES_ACTIVE=production in .env first)
./gradlew bootRun

# Or run with Docker Compose (includes Prometheus & Grafana)
docker-compose up -d
```

When you run `./gradlew bootRun`, you'll see:
```
Loading environment variables from .env file:
  - CONFIGCAT_PAYMENT_SDK_KEY_LOCAL
  - CONFIGCAT_PAYMENT_SDK_KEY_PRODUCTION
  - CONFIGCAT_USER_MANAGEMENT_SDK_KEY_LOCAL
  - CONFIGCAT_USER_MANAGEMENT_SDK_KEY_PRODUCTION
  - SPRING_PROFILES_ACTIVE
```

### 3. Test the API

Run the automated test suite:

```bash
./test-api.sh
```

This script will test all endpoints and verify:
- ✅ Health checks
- ✅ User Management API
- ✅ Payment Processing API
- ✅ OpenAPI documentation
- ✅ Swagger UI

## API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **OpenAPI YAML**: http://localhost:8080/api-docs.yaml

## Endpoints

### User Management Endpoint
**GET** `/api/users/{userId}`

Features controlled by User Management ConfigCat project:
- `beta_features_enabled`: Enables beta features for user
- `premium_account_features`: Premium account capabilities
- `ui_version`: UI version (v1, v2, v3)

**Headers:**
- `X-User-Country`: User's country (affects feature flags)
- `X-User-Subscription`: Subscription type (basic, premium, enterprise)
- `X-Correlation-ID`: Optional correlation ID (auto-generated if not provided)

**Example:**
```bash
curl -X GET "http://localhost:8080/api/users/user123" \
  -H "X-User-Country: BR" \
  -H "X-User-Subscription: premium"
```

**Response:**
```json
{
  "userId": "user123",
  "email": "user123@example.com",
  "profile": {
    "firstName": "User",
    "lastName": "user123",
    "preferredLanguage": "pt-BR",
    "avatarUrl": null
  },
  "features": {
    "betaFeaturesEnabled": false,
    "premiumAccount": false,
    "uiVersion": "v1",
    "maxFileUploadSize": 10485760
  },
  "metadata": {
    "correlationId": "f3683219-f370-4afa-a8e1-c15fb06a8ea2",
    "timestamp": "2025-10-05T11:10:58.610127Z",
    "configcat_evaluations": [
      {
        "project": "user-management",
        "flagKey": "beta_features_enabled",
        "value": false,
        "evaluatedAt": "2025-10-05T11:10:58.610127Z"
      }
    ]
  }
}
```

### Payment Processing Endpoint
**POST** `/api/payments/process`

Features controlled by Payment ConfigCat project:
- `express_checkout_enabled`: Express checkout functionality
- `recurring_payments_enabled`: Recurring payment support
- `fraud_detection_level`: Fraud detection level (standard, high, strict)

**Headers:**
- `X-User-ID`: User identifier
- `X-User-Country`: User's country
- `X-Payment-Provider`: Payment provider preference

**Example:**
```bash
curl -X POST "http://localhost:8080/api/payments/process" \
  -H "Content-Type: application/json" \
  -H "X-User-Country: BR" \
  -H "X-Payment-Provider: stripe" \
  -d '{
    "amount": 99.99,
    "currency": "BRL",
    "paymentMethod": "credit_card",
    "cardNumber": "4242424242424242",
    "isRecurring": false
  }'
```

**Response:**
```json
{
  "transactionId": "7bca60c9-cb91-45ff-a4de-9cc35c3ffd7c",
  "amount": 99.99,
  "currency": "BRL",
  "method": {
    "type": "credit_card",
    "processor": "stripe_brazil",
    "last4Digits": "4242"
  },
  "status": "COMPLETED",
  "features": {
    "expressCheckoutEnabled": false,
    "recurringPaymentsEnabled": false,
    "fraudDetectionLevel": "standard",
    "paymentMethodsAvailable": ["credit_card", "debit_card", "pix", "boleto"]
  },
  "metadata": {
    "correlationId": "d37244b9-0d0e-4b73-8a38-671ad43b957f",
    "timestamp": "2025-10-05T11:11:33.789911Z",
    "configcat_evaluations": [
      {
        "project": "payment",
        "flagKey": "express_checkout_enabled",
        "value": false,
        "evaluatedAt": "2025-10-05T11:11:33.789911Z"
      }
    ]
  }
}
```

## ConfigCat Setup

### 1. Create Two ConfigCat Projects

1. **User Management Project**
   - Create flags: `beta_features_enabled`, `premium_account_features`, `ui_version`
   - Set up targeting rules based on user attributes
   - Create two environments: Local and Production

2. **Payment Project**
   - Create flags: `express_checkout_enabled`, `recurring_payments_enabled`, `fraud_detection_level`
   - Set up targeting rules based on country, amount, etc.
   - Create two environments: Local and Production

### 2. Configure SDK Keys

The application supports **two environments**: Local and Production.

**Using .env file (Recommended)**

This is the preferred method. The Gradle build automatically loads all variables from `.env`:

```bash
# Copy the example file
cp .env.example .env

# Edit .env with your actual SDK keys
nano .env
```

Your `.env` file should contain:
```bash
# Set active profile (local or production)
SPRING_PROFILES_ACTIVE=local

# Local Environment SDK Keys
CONFIGCAT_USER_MANAGEMENT_SDK_KEY_LOCAL=configcat-sdk-1/xxxxx/local-key
CONFIGCAT_PAYMENT_SDK_KEY_LOCAL=configcat-sdk-1/xxxxx/local-key

# Production Environment SDK Keys
CONFIGCAT_USER_MANAGEMENT_SDK_KEY_PRODUCTION=configcat-sdk-1/xxxxx/prod-key
CONFIGCAT_PAYMENT_SDK_KEY_PRODUCTION=configcat-sdk-1/xxxxx/prod-key
```

**Alternative: Direct environment variables**

```bash
export SPRING_PROFILES_ACTIVE=local
export CONFIGCAT_USER_MANAGEMENT_SDK_KEY_LOCAL="your-key"
export CONFIGCAT_PAYMENT_SDK_KEY_LOCAL="your-key"
export CONFIGCAT_USER_MANAGEMENT_SDK_KEY_PRODUCTION="your-key"
export CONFIGCAT_PAYMENT_SDK_KEY_PRODUCTION="your-key"
```

### Environment Profiles

The application is configured with two environments:

#### Local Environment
- **Profile**: `local` (default)
- **Log Level**: INFO
- **Poll Interval**: 30 seconds
- **Purpose**: Development and local testing
- **SDK Keys**: Uses `*_SDK_KEY_LOCAL` variables

#### Production Environment
- **Profile**: `production`
- **Log Level**: WARNING (reduced logging)
- **Poll Interval**: 60 seconds (optimized for production)
- **Purpose**: Production deployment
- **SDK Keys**: Uses `*_SDK_KEY_PRODUCTION` variables

### Switching Environments

**Method 1: Change .env file**
```bash
# Edit .env and change:
SPRING_PROFILES_ACTIVE=production
# Then run
./gradlew bootRun
```

**Method 2: Override via command line**
```bash
SPRING_PROFILES_ACTIVE=production ./gradlew bootRun
```

**Method 3: Using Spring Boot arguments**
```bash
./gradlew bootRun --args='--spring.profiles.active=production'
```

## Testing the Integration

### Automated Test Suite

Run the comprehensive test script:
```bash
./test-api.sh
```

This will test:
1. ✅ Health endpoint
2. ✅ User Management API (with feature flag evaluation)
3. ✅ Payment API - Brazilian user with Stripe
4. ✅ Payment API - US user with recurring payment
5. ✅ OpenAPI documentation
6. ✅ Swagger UI accessibility

### Manual Testing

#### 1. Test User Management Features
```bash
# Test basic user (no premium features)
curl "http://localhost:8080/api/users/basic-user" \
  -H "X-User-Country: US" \
  -H "X-User-Subscription: basic"

# Test premium user (premium features enabled)
curl "http://localhost:8080/api/users/premium-user" \
  -H "X-User-Country: BR" \
  -H "X-User-Subscription: premium"
```

#### 2. Test Payment Features
```bash
# Test Brazilian payment with PIX/Boleto support
curl -X POST "http://localhost:8080/api/payments/process" \
  -H "Content-Type: application/json" \
  -H "X-User-Country: BR" \
  -H "X-Payment-Provider: stripe" \
  -d '{"amount": 99.99, "currency": "BRL", "paymentMethod": "credit_card", "cardNumber": "4242424242424242", "isRecurring": false}'

# Test US payment with PayPal/Apple Pay support
curl -X POST "http://localhost:8080/api/payments/process" \
  -H "Content-Type: application/json" \
  -H "X-User-Country: US" \
  -d '{"amount": 29.99, "currency": "USD", "paymentMethod": "credit_card", "cardNumber": "5555555555554444", "isRecurring": true}'
```

#### 3. Monitor Logs
```bash
# Follow application logs
tail -f logs/configcat-demo.log

# Or using Docker
docker-compose logs -f configcat-demo
```

## MDC Logging Features

The application automatically tracks:

### Request Context
- `correlationId`: Unique identifier for request tracing
- `requestUri`: Request URI
- `requestMethod`: HTTP method
- `userId`: User ID from headers
- `sessionId`: Session ID from headers

### ConfigCat Context
- `configcat.project`: Which ConfigCat project was used
- `configcat.flag`: Which feature flag was evaluated
- `configcat.result`: The result of the evaluation

### Log Example
```json
{
  "timestamp": "2025-10-05T08:11:33.790-03",
  "level": "INFO",
  "logger": "c.e.c.service.FeatureToggleService",
  "message": "Feature flag evaluated: project=payment, flag=express_checkout_enabled, userId=null, result=false, correlationId=d37244b9-0d0e-4b73-8a38-671ad43b957f",
  "correlation_id": "d37244b9-0d0e-4b73-8a38-671ad43b957f",
  "request_uri": "/api/payments/process",
  "request_method": "POST",
  "configcat_project": "payment",
  "configcat_flag": "express_checkout_enabled"
}
```

## Monitoring & Observability

### Health Checks
- **Application Health**: http://localhost:8080/actuator/health
- **ConfigCat Status**: Included in health check responses

### Metrics
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **Grafana Dashboard**: http://localhost:3000 (admin/admin)

### Available Endpoints
- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus

### Common Metrics
- Feature flag evaluation counts
- Feature flag evaluation duration
- Feature flag evaluation errors
- HTTP request metrics
- JVM metrics

## Code Quality

### Run Tests
```bash
./gradlew test
```

### Code Analysis
```bash
# Kotlin lint
./gradlew ktlintCheck

# Detekt static analysis
./gradlew detekt
```

### Build
```bash
./gradlew build
```

## Key Implementation Details

### Dual Client Configuration
- Two separate `ConfigCatClient` beans with different qualifiers
- Independent polling intervals per environment
- Proper client lifecycle management
- ConfigCat SDK version: 9.0.0 (latest API)

### Environment-Based Configuration
- Separate SDK keys for Local and Production environments
- Environment-specific polling intervals (30s local, 60s production)
- Environment-specific log levels (INFO local, WARNING production)
- Automatic environment variable loading via Gradle

### Feature Flag Service
- Unified service interface for both projects
- User context building with attributes
- Comprehensive error handling and fallbacks
- Detailed logging for all evaluations

### Gradle .env Integration
- Custom `loadEnvFile()` function in build.gradle.kts
- Automatic parsing of .env file
- Environment variables loaded before bootRun
- Visual confirmation of loaded variables

### MDC Integration
- Automatic correlation ID generation
- Request/response header propagation
- ConfigCat evaluation context tracking
- Filter-based implementation for all requests

### Structured Logging
- JSON-formatted logs for easy parsing
- ConfigCat-specific log fields
- Performance metrics for flag evaluations
- Logstash encoder for log aggregation

### OpenAPI Documentation
- SpringDoc OpenAPI 3 integration
- Comprehensive endpoint documentation
- Request/response examples
- Schema annotations on all models
- Interactive Swagger UI

## Technologies

- **Framework**: Spring Boot 3.2.0
- **Language**: Kotlin 1.9.21
- **Feature Flags**: ConfigCat Java SDK 9.0.0
- **Logging**: Logback + Logstash Encoder
- **Documentation**: SpringDoc OpenAPI 3
- **Metrics**: Micrometer + Prometheus
- **Observability**: Grafana
- **Build Tool**: Gradle 8.5+

## Troubleshooting

### Common Issues

1. **Environment Variables Not Loading**
   - Ensure `.env` file exists in project root
   - Check file format (KEY=value, no spaces around =)
   - Verify Gradle output shows "Loading environment variables from .env file"
   - Try cleaning and rebuilding: `./gradlew clean bootRun`

2. **SDK Key Not Found**
   - Verify SDK keys in .env file match your ConfigCat dashboard
   - Check environment variable names match exactly
   - Ensure ConfigCat projects are active
   - Verify correct profile is active (local vs production)

3. **Feature Flags Not Working**
   - Check flag names match exactly in ConfigCat dashboard
   - Verify targeting rules in ConfigCat
   - Review user attributes being passed
   - Check which environment (Local/Production) you're using

4. **Logging Issues**
   - Ensure logs directory is writable
   - Check logback-spring.xml configuration
   - Verify MDC context is being set

5. **LogLevel Error**
   - ConfigCat SDK 9.0.0 uses `WARNING` instead of `WARN`
   - Valid levels: `NO_LOG`, `ERROR`, `WARNING`, `INFO`, `DEBUG`

### Debug Mode
Enable debug logging to see detailed ConfigCat operations:
```yaml
logging:
  level:
    com.configcat: DEBUG
    com.example.configcat: DEBUG
```

Or add to your `.env`:
```bash
LOGGING_LEVEL_COM_CONFIGCAT=DEBUG
LOGGING_LEVEL_COM_EXAMPLE_CONFIGCAT=DEBUG
```

## Project Structure

```
.
├── .env                                   # Environment variables (git-ignored)
├── .env.example                          # Environment variables template
├── build.gradle.kts                      # Gradle build with .env loading
├── src/
│   ├── main/
│   │   ├── kotlin/com/example/configcat/
│   │   │   ├── ConfigCatDemoApplication.kt           # Main application
│   │   │   ├── config/
│   │   │   │   ├── ConfigCatConfiguration.kt         # Dual ConfigCat client setup
│   │   │   │   ├── MdcConfiguration.kt               # MDC logging filter
│   │   │   │   └── OpenApiConfiguration.kt           # Swagger/OpenAPI config
│   │   │   ├── controller/
│   │   │   │   ├── UserController.kt                 # User management endpoints
│   │   │   │   └── PaymentController.kt              # Payment processing endpoints
│   │   │   ├── service/
│   │   │   │   └── FeatureToggleService.kt           # Feature flag evaluation
│   │   │   └── model/
│   │   │       └── UserResponse.kt                   # Response models
│   │   └── resources/
│   │       ├── application.yml                       # Multi-environment config
│   │       └── logback-spring.xml                    # Logging configuration
│   └── test/
│       ├── kotlin/com/example/configcat/
│       │   └── ConfigCatDemoApplicationTests.kt
│       └── resources/
│           └── application-test.yml                  # Test configuration
├── docker-compose.yml                    # Docker setup with monitoring
├── test-api.sh                          # Automated API test script
└── README.md                            # This file
```

## Configuration Files

### application.yml Structure
```yaml
# Common configuration
spring:
  application:
    name: configcat-demo
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

---
# Local Environment
spring:
  config:
    activate:
      on-profile: local
configcat:
  log-level: INFO
  user-management:
    sdk-key: ${CONFIGCAT_USER_MANAGEMENT_SDK_KEY_LOCAL}
    poll-interval-seconds: 30
  payment:
    sdk-key: ${CONFIGCAT_PAYMENT_SDK_KEY_LOCAL}
    poll-interval-seconds: 30

---
# Production Environment  
spring:
  config:
    activate:
      on-profile: production
configcat:
  log-level: WARNING
  user-management:
    sdk-key: ${CONFIGCAT_USER_MANAGEMENT_SDK_KEY_PRODUCTION}
    poll-interval-seconds: 60
  payment:
    sdk-key: ${CONFIGCAT_PAYMENT_SDK_KEY_PRODUCTION}
    poll-interval-seconds: 60
```

## Further Enhancements

Potential improvements for production use:
- Circuit breaker for ConfigCat API calls
- Redis caching for flag evaluations
- Custom metrics for business KPIs
- Integration with APM tools (DataDog, New Relic)
- Automated testing with different flag combinations
- A/B testing analytics
- Feature flag audit trail
- Additional environments (Staging, QA)
- Environment-specific feature flag overrides

## Contributing

This is a proof-of-concept project demonstrating ConfigCat integration patterns. Feel free to use it as a reference for your own implementations.

## License

MIT License - see LICENSE file for details.

## References

- [ConfigCat Documentation](https://configcat.com/docs/)
- [ConfigCat Java SDK](https://configcat.com/docs/sdk-reference/java/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [SpringDoc OpenAPI](https://springdoc.org/)
- [Gradle Documentation](https://docs.gradle.org/)
