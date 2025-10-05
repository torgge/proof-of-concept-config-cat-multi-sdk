# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a complete Spring Boot proof-of-concept application demonstrating dual ConfigCat project integration with comprehensive MDC logging. The application showcases how to use two separate ConfigCat projects within a single Spring Boot application.

## Project Structure

```
src/main/kotlin/com/example/configcat/
├── ConfigCatDemoApplication.kt           # Main application class
├── config/
│   ├── ConfigCatConfiguration.kt         # Dual ConfigCat client configuration
│   └── MdcConfiguration.kt               # MDC logging filter setup
├── controller/
│   ├── UserController.kt                 # User management endpoint
│   └── PaymentController.kt              # Payment processing endpoint
├── service/
│   └── FeatureToggleService.kt           # Feature flag evaluation service
└── model/
    └── UserResponse.kt                   # Response models
```

## Common Commands

### Build & Test
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Check code quality
./gradlew detekt ktlintCheck
```

### Development
```bash
# Run the application locally
./gradlew bootRun

# Run with specific profile
./gradlew bootRun --args='--spring.profiles.active=local'

# Run with Docker Compose (includes Prometheus/Grafana)
docker-compose up -d

# View logs
tail -f logs/configcat-demo.log
# or
docker-compose logs -f configcat-demo
```

### Testing Endpoints
```bash
# Test User Management endpoint
curl "http://localhost:8080/api/users/user123" \
  -H "X-User-Country: BR" \
  -H "X-User-Subscription: premium"

# Test Payment endpoint
curl -X POST "http://localhost:8080/api/payments/process" \
  -H "Content-Type: application/json" \
  -H "X-User-Country: US" \
  -d '{"amount": 99.99, "currency": "USD", "paymentMethod": "credit_card"}'
```

## Architecture

The application implements:

### Dual ConfigCat Integration
- **User Management ConfigCat Project**: Controls user-related features
- **Payment ConfigCat Project**: Controls payment-related features
- Two separate `ConfigCatClient` beans with independent configurations

### MDC Logging
- Automatic correlation ID generation and propagation
- Request context tracking (URI, method, user info)
- ConfigCat evaluation context (project, flag, result)
- Structured JSON logging with all context information

### Feature Flags
- **User Management**: `beta_features_enabled`, `premium_account_features`, `ui_version`
- **Payment**: `express_checkout_enabled`, `recurring_payments_enabled`, `fraud_detection_level`

## Configuration

### ConfigCat Setup
Update `src/main/resources/application.yml` with your SDK keys:

```yaml
configcat:
  user-management:
    sdk-key: "YOUR_USER_MANAGEMENT_SDK_KEY"
  payment:
    sdk-key: "YOUR_PAYMENT_SDK_KEY"
```

Or use environment variables:
```bash
export CONFIGCAT_USER_MANAGEMENT_SDK_KEY="your_key_here"
export CONFIGCAT_PAYMENT_SDK_KEY="your_key_here"
```

### Environment Profiles
- `local`: Debug logging, faster polling (10s)
- `development`: Standard configuration for dev environment
- `production`: Optimized for production with longer polling (60s)

## Key Technologies

- **Framework**: Spring Boot 3.2 with Kotlin
- **Feature Flags**: ConfigCat Java Client 9.0.0
- **Logging**: Structured JSON logging with Logback + Logstash encoder
- **Observability**: Micrometer + Prometheus + Grafana
- **Build Tool**: Gradle with Kotlin DSL
- **Testing**: JUnit 5 + MockK
- **Containerization**: Docker with multi-stage builds

## Monitoring & Observability

### Available Endpoints
- Application: `http://localhost:8080`
- Health Check: `http://localhost:8080/actuator/health`
- Prometheus Metrics: `http://localhost:8080/actuator/prometheus`
- Grafana Dashboard: `http://localhost:3000` (admin/admin)

### Log Analysis
All logs include comprehensive MDC context:
- `correlationId`: Request tracing
- `configcat.project`: Which ConfigCat project was used
- `configcat.flag`: Which feature flag was evaluated
- `configcat.result`: Evaluation result

## Development Workflow

1. **Update Feature Flags**: Configure flags in ConfigCat dashboard
2. **Test Locally**: Use curl commands or Postman to test endpoints
3. **Monitor Logs**: Check structured logs for flag evaluations
4. **Verify Metrics**: Use Grafana to monitor flag evaluation metrics
5. **Code Quality**: Run `./gradlew detekt ktlintCheck` before commits

## Implementation Highlights

### FeatureToggleService.kt:27-89
Centralized service for evaluating feature flags from both ConfigCat projects with comprehensive logging and error handling.

### MdcConfiguration.kt:25-65
Filter that automatically sets up MDC context for every request, including correlation ID generation and ConfigCat evaluation tracking.

### ConfigCatConfiguration.kt:15-35
Configuration for two separate ConfigCat clients with different polling intervals and logging levels.

## Troubleshooting

### Common Issues
1. **SDK Key Errors**: Verify keys in application.yml or environment variables
2. **Feature Flags Not Working**: Check flag names and targeting rules in ConfigCat
3. **Missing Logs**: Ensure logs directory exists and is writable

### Debug Mode
Enable detailed ConfigCat logging:
```yaml
logging:
  level:
    com.configcat: DEBUG
```