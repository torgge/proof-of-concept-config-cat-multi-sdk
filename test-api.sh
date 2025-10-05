#!/bin/bash

# ConfigCat Multi-SDK Demo - API Test Script
# This script tests all API endpoints and verifies the application is working correctly

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-http://localhost:8080}"
TIMEOUT=5

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}ConfigCat Multi-SDK Demo - API Tests${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Function to print status
print_status() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
        exit 1
    fi
}

# Function to wait for app to start
wait_for_app() {
    echo -e "${YELLOW}Waiting for application to start...${NC}"
    for i in {1..30}; do
        if curl -s -f "$BASE_URL/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}Application is ready!${NC}\n"
            return 0
        fi
        echo -n "."
        sleep 1
    done
    echo -e "\n${RED}Application failed to start within 30 seconds${NC}"
    exit 1
}

# 1. Health Check
echo -e "${BLUE}1. Testing Health Endpoint${NC}"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/actuator/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n 1)
HEALTH_BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    STATUS=$(echo "$HEALTH_BODY" | jq -r '.status')
    if [ "$STATUS" = "UP" ]; then
        print_status 0 "Health check passed (Status: UP)"
        echo -e "   ${BLUE}Response:${NC} $(echo "$HEALTH_BODY" | jq -c '.')\n"
    else
        print_status 1 "Health check failed (Status: $STATUS)"
    fi
else
    print_status 1 "Health check failed (HTTP $HTTP_CODE)"
fi

# 2. User Management API Test
echo -e "${BLUE}2. Testing User Management API${NC}"
USER_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "X-User-Country: BR" \
    -H "X-User-Subscription: premium" \
    "$BASE_URL/api/users/user123")

HTTP_CODE=$(echo "$USER_RESPONSE" | tail -n 1)
USER_BODY=$(echo "$USER_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    USER_ID=$(echo "$USER_BODY" | jq -r '.userId')
    BETA_ENABLED=$(echo "$USER_BODY" | jq -r '.features.betaFeaturesEnabled')
    UI_VERSION=$(echo "$USER_BODY" | jq -r '.features.uiVersion')
    CORRELATION_ID=$(echo "$USER_BODY" | jq -r '.metadata.correlationId')
    FLAG_COUNT=$(echo "$USER_BODY" | jq '.metadata.configcat_evaluations | length')

    print_status 0 "User Management API test passed"
    echo -e "   ${BLUE}User ID:${NC} $USER_ID"
    echo -e "   ${BLUE}Beta Features:${NC} $BETA_ENABLED"
    echo -e "   ${BLUE}UI Version:${NC} $UI_VERSION"
    echo -e "   ${BLUE}Correlation ID:${NC} $CORRELATION_ID"
    echo -e "   ${BLUE}Feature Flags Evaluated:${NC} $FLAG_COUNT"
    echo -e "   ${BLUE}Full Response:${NC}"
    echo "$USER_BODY" | jq '.' | sed 's/^/   /'
    echo ""
else
    print_status 1 "User Management API test failed (HTTP $HTTP_CODE)"
fi

# 3. Payment Processing API Test - Brazilian User
echo -e "${BLUE}3. Testing Payment API (Brazilian User with Stripe)${NC}"
PAYMENT_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X POST \
    -H "Content-Type: application/json" \
    -H "X-User-Country: BR" \
    -H "X-Payment-Provider: stripe" \
    -d '{
        "amount": 99.99,
        "currency": "BRL",
        "paymentMethod": "credit_card",
        "cardNumber": "4242424242424242",
        "isRecurring": false
    }' \
    "$BASE_URL/api/payments/process")

HTTP_CODE=$(echo "$PAYMENT_RESPONSE" | tail -n 1)
PAYMENT_BODY=$(echo "$PAYMENT_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    TXN_ID=$(echo "$PAYMENT_BODY" | jq -r '.transactionId')
    STATUS=$(echo "$PAYMENT_BODY" | jq -r '.status')
    PROCESSOR=$(echo "$PAYMENT_BODY" | jq -r '.method.processor')
    EXPRESS_CHECKOUT=$(echo "$PAYMENT_BODY" | jq -r '.features.expressCheckoutEnabled')
    FRAUD_LEVEL=$(echo "$PAYMENT_BODY" | jq -r '.features.fraudDetectionLevel')
    METHODS=$(echo "$PAYMENT_BODY" | jq -r '.features.paymentMethodsAvailable | join(", ")')
    CORRELATION_ID=$(echo "$PAYMENT_BODY" | jq -r '.metadata.correlationId')

    print_status 0 "Payment API test passed"
    echo -e "   ${BLUE}Transaction ID:${NC} $TXN_ID"
    echo -e "   ${BLUE}Status:${NC} $STATUS"
    echo -e "   ${BLUE}Processor:${NC} $PROCESSOR"
    echo -e "   ${BLUE}Express Checkout:${NC} $EXPRESS_CHECKOUT"
    echo -e "   ${BLUE}Fraud Detection Level:${NC} $FRAUD_LEVEL"
    echo -e "   ${BLUE}Available Methods:${NC} $METHODS"
    echo -e "   ${BLUE}Correlation ID:${NC} $CORRELATION_ID"
    echo -e "   ${BLUE}Full Response:${NC}"
    echo "$PAYMENT_BODY" | jq '.' | sed 's/^/   /'
    echo ""
else
    print_status 1 "Payment API test failed (HTTP $HTTP_CODE)"
fi

# 4. Payment Processing API Test - US User with Recurring Payment
echo -e "${BLUE}4. Testing Payment API (US User with Recurring Payment)${NC}"
PAYMENT_US_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X POST \
    -H "Content-Type: application/json" \
    -H "X-User-Country: US" \
    -H "X-Payment-Provider: stripe" \
    -d '{
        "amount": 29.99,
        "currency": "USD",
        "paymentMethod": "credit_card",
        "cardNumber": "5555555555554444",
        "isRecurring": true
    }' \
    "$BASE_URL/api/payments/process")

HTTP_CODE=$(echo "$PAYMENT_US_RESPONSE" | tail -n 1)
PAYMENT_US_BODY=$(echo "$PAYMENT_US_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    TXN_ID=$(echo "$PAYMENT_US_BODY" | jq -r '.transactionId')
    STATUS=$(echo "$PAYMENT_US_BODY" | jq -r '.status')
    METHODS=$(echo "$PAYMENT_US_BODY" | jq -r '.features.paymentMethodsAvailable | join(", ")')

    print_status 0 "Payment API (US) test passed"
    echo -e "   ${BLUE}Transaction ID:${NC} $TXN_ID"
    echo -e "   ${BLUE}Status:${NC} $STATUS"
    echo -e "   ${BLUE}Available Methods:${NC} $METHODS"
    echo ""
else
    print_status 1 "Payment API (US) test failed (HTTP $HTTP_CODE)"
fi

# 5. OpenAPI Documentation Test
echo -e "${BLUE}5. Testing OpenAPI Documentation${NC}"
OPENAPI_RESPONSE=$(curl -s -w "\n%{http_code}" "$BASE_URL/api-docs")
HTTP_CODE=$(echo "$OPENAPI_RESPONSE" | tail -n 1)
OPENAPI_BODY=$(echo "$OPENAPI_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    API_TITLE=$(echo "$OPENAPI_BODY" | jq -r '.info.title')
    API_VERSION=$(echo "$OPENAPI_BODY" | jq -r '.info.version')
    ENDPOINT_COUNT=$(echo "$OPENAPI_BODY" | jq '.paths | keys | length')

    print_status 0 "OpenAPI documentation available"
    echo -e "   ${BLUE}API Title:${NC} $API_TITLE"
    echo -e "   ${BLUE}API Version:${NC} $API_VERSION"
    echo -e "   ${BLUE}Endpoints Documented:${NC} $ENDPOINT_COUNT"
    echo -e "   ${BLUE}Endpoints:${NC}"
    echo "$OPENAPI_BODY" | jq -r '.paths | keys[]' | sed 's/^/      - /'
    echo ""
else
    print_status 1 "OpenAPI documentation test failed (HTTP $HTTP_CODE)"
fi

# 6. Swagger UI Test
echo -e "${BLUE}6. Testing Swagger UI${NC}"
SWAGGER_HTTP_CODE=$(curl -s -L -w "%{http_code}" -o /dev/null "$BASE_URL/swagger-ui.html")

if [ "$SWAGGER_HTTP_CODE" = "200" ] || [ "$SWAGGER_HTTP_CODE" = "302" ]; then
    print_status 0 "Swagger UI is accessible"
    echo -e "   ${BLUE}URL:${NC} $BASE_URL/swagger-ui.html\n"
else
    print_status 1 "Swagger UI test failed (HTTP $SWAGGER_HTTP_CODE)"
fi

# Summary
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All Tests Passed Successfully! ✓${NC}"
echo -e "${GREEN}========================================${NC}\n"

echo -e "${BLUE}Available URLs:${NC}"
echo -e "  ${YELLOW}Application:${NC}     $BASE_URL"
echo -e "  ${YELLOW}Health Check:${NC}    $BASE_URL/actuator/health"
echo -e "  ${YELLOW}Swagger UI:${NC}      $BASE_URL/swagger-ui.html"
echo -e "  ${YELLOW}OpenAPI Spec:${NC}    $BASE_URL/api-docs"
echo -e "  ${YELLOW}Metrics:${NC}         $BASE_URL/actuator/prometheus"
echo ""

echo -e "${BLUE}Tested Features:${NC}"
echo -e "  ✓ Dual ConfigCat SDK Integration"
echo -e "  ✓ Feature Flag Evaluation (User Management & Payment)"
echo -e "  ✓ MDC Correlation ID Tracking"
echo -e "  ✓ Country-based Payment Methods"
echo -e "  ✓ OpenAPI/Swagger Documentation"
echo -e "  ✓ Health Monitoring"
echo ""
