# ConfigCat Dual Project Integration Demo

This is a proof-of-concept Spring Boot application that demonstrates how to integrate two separate ConfigCat projects within a single application, using MDC (Mapped Diagnostic Context) for comprehensive logging.

## Overview

The application showcases:
- **Dual ConfigCat Integration**: Two separate ConfigCat projects (User Management & Payment)
- **Feature Flag Evaluation**: Different feature toggles for different business domains
- **MDC Logging**: Comprehensive request tracing with correlation IDs
- **Structured Logging**: JSON-formatted logs with ConfigCat evaluation details
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
  -H "X-User-Subscription: premium" \
  -H "X-Correlation-ID: test-123"
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
  -H "X-User-ID: user123" \
  -H "X-User-Country: US" \
  -H "X-Payment-Provider: stripe" \
  -d '{
    "amount": 99.99,
    "currency": "USD",
    "paymentMethod": "credit_card",
    "cardNumber": "4111111111111111",
    "isRecurring": false
  }'
```

## ConfigCat Setup

### 1. Create Two ConfigCat Projects

1. **User Management Project**
   - Create flags: `beta_features_enabled`, `premium_account_features`, `ui_version`
   - Set up targeting rules based on user attributes

2. **Payment Project**
   - Create flags: `express_checkout_enabled`, `recurring_payments_enabled`, `fraud_detection_level`
   - Set up targeting rules based on country, amount, etc.

### 2. Configure SDK Keys

Update `application.yml` with your ConfigCat SDK keys:

```yaml
configcat:
  user-management:
    sdk-key: "YOUR_USER_MANAGEMENT_SDK_KEY"
  payment:
    sdk-key: "YOUR_PAYMENT_SDK_KEY"
```

Or set environment variables:
```bash
export CONFIGCAT_USER_MANAGEMENT_SDK_KEY="YOUR_USER_MANAGEMENT_SDK_KEY"
export CONFIGCAT_PAYMENT_SDK_KEY="YOUR_PAYMENT_SDK_KEY"
```

## Running the Application

### Local Development

1. **Prerequisites:**
   - Java 21
   - Gradle 8.5+
   - Docker & Docker Compose (optional)

2. **Run with Gradle:**
   ```bash
   ./gradlew bootRun
   ```

3. **Run with Docker Compose:**
   ```bash
   docker-compose up -d
   ```

### Environment Profiles

- **local**: Debug logging, shorter polling intervals
- **development**: Standard logging for dev environment
- **production**: Optimized for production with longer polling intervals

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
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "logger": "c.e.c.s.FeatureToggleService",
  "message": "Feature flag evaluated: project=user-management, flag=beta_features_enabled, userId=user123, result=true, correlationId=abc-123",
  "correlation_id": "abc-123",
  "user_id": "user123",
  "request_uri": "/api/users/user123",
  "request_method": "GET",
  "configcat_project": "user-management",
  "configcat_flag": "beta_features_enabled",
  "configcat_result": "true"
}
```

## Monitoring & Observability

### Health Checks
- **Application Health**: `http://localhost:8080/actuator/health`
- **ConfigCat Status**: Included in health check responses

### Metrics
- **Prometheus Metrics**: `http://localhost:8080/actuator/prometheus`
- **Grafana Dashboard**: `http://localhost:3000` (admin/admin)

### Common Metrics
- Feature flag evaluation counts
- Feature flag evaluation duration
- Feature flag evaluation errors
- HTTP request metrics
- JVM metrics

## Testing the Integration

### 1. Test User Management Features
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

### 2. Test Payment Features
```bash
# Test small payment (basic features)
curl -X POST "http://localhost:8080/api/payments/process" \
  -H "Content-Type: application/json" \
  -H "X-User-Country: US" \
  -d '{"amount": 25.00, "currency": "USD", "paymentMethod": "credit_card"}'

# Test large payment (enhanced fraud detection)
curl -X POST "http://localhost:8080/api/payments/process" \
  -H "Content-Type: application/json" \
  -H "X-User-Country: BR" \
  -d '{"amount": 2500.00, "currency": "BRL", "paymentMethod": "credit_card"}'
```

### 3. Monitor Logs
```bash
# Follow application logs
tail -f logs/configcat-demo.log

# Or using Docker
docker-compose logs -f configcat-demo
```

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
- Independent polling intervals and configurations
- Proper client lifecycle management

### Feature Flag Service
- Unified service interface for both projects
- User context building with attributes
- Comprehensive error handling and fallbacks

### MDC Integration
- Automatic correlation ID generation
- Request/response header propagation
- ConfigCat evaluation context tracking

### Structured Logging
- JSON-formatted logs for easy parsing
- ConfigCat-specific log fields
- Performance metrics for flag evaluations

## Troubleshooting

### Common Issues

1. **SDK Key Not Found**
   - Verify SDK keys in application.yml
   - Check environment variable names
   - Ensure ConfigCat projects are active

2. **Feature Flags Not Working**
   - Check flag names match exactly
   - Verify targeting rules in ConfigCat dashboard
   - Review user attributes being passed

3. **Logging Issues**
   - Ensure logs directory is writable
   - Check logback-spring.xml configuration
   - Verify MDC context is being set

### Debug Mode
Enable debug logging to see detailed ConfigCat operations:
```yaml
logging:
  level:
    com.configcat: DEBUG
    com.example.configcat: DEBUG
```

## Further Enhancements

Potential improvements for production use:
- Circuit breaker for ConfigCat API calls
- Redis caching for flag evaluations
- Custom metrics for business KPIs
- Integration with APM tools (DataDog, New Relic)
- Automated testing with different flag combinations