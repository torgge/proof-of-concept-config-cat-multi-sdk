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
MAX_WAIT_TIME=120
CLEANUP_ON_EXIT=true

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  ConfigCat Demo - API Test Suite                       ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Function to cleanup on exit
cleanup() {
    if [ "$CLEANUP_ON_EXIT" = true ]; then
        echo -e "\n${YELLOW}🧹 Cleaning up Docker resources...${NC}"

        # Stop and remove containers, networks, and volumes
        echo -e "${YELLOW}  → Stopping and removing containers...${NC}"
        docker-compose down -v

        # Remove dangling volumes
        echo -e "${YELLOW}  → Removing unused volumes...${NC}"
        docker volume prune -f 2>/dev/null || true

        # Remove dangling images
        echo -e "${YELLOW}  → Removing dangling images...${NC}"
        docker image prune -f 2>/dev/null || true

        # Remove project-specific images
        echo -e "${YELLOW}  → Removing project images...${NC}"
        docker rmi spring-boot-config-cat-configcat-demo 2>/dev/null || true

        # Clean build cache (optional, keeps future builds faster)
        echo -e "${YELLOW}  → Removing build cache...${NC}"
        docker builder prune -f 2>/dev/null || true

        echo -e "${GREEN}✅ Cleanup completed - All Docker resources reclaimed${NC}"
    fi
}

# Register cleanup function
trap cleanup EXIT

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${RED}❌ Error: .env file not found!${NC}"
    echo -e "${YELLOW}Please create a .env file from .env.example:${NC}"
    echo -e "  cp .env.example .env"
    exit 1
fi

echo -e "${BLUE}📋 Step 1: Checking Docker status...${NC}"
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Error: Docker is not running!${NC}"
    echo -e "${YELLOW}Please start Docker Desktop and try again.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker is running${NC}\n"

echo -e "${BLUE}📋 Step 2: Checking for port conflicts...${NC}"
# First, stop any existing docker-compose services
docker-compose down -v 2>/dev/null || true

# Check if port 8080 is in use
PORT_IN_USE=$(lsof -ti:8080 2>/dev/null || true)
if [ -n "$PORT_IN_USE" ]; then
    echo -e "${YELLOW}⚠️  Port 8080 is in use by process(es): $PORT_IN_USE${NC}"
    echo -e "${YELLOW}Attempting to stop conflicting processes...${NC}"

    # Check if it's a Gradle bootRun process
    if ps -p $PORT_IN_USE | grep -q gradle; then
        echo -e "${YELLOW}Found Gradle bootRun process, stopping it...${NC}"
        kill $PORT_IN_USE 2>/dev/null || true
        sleep 3
    # Check if it's a Docker container
    elif ps -p $PORT_IN_USE | grep -q docker; then
        echo -e "${YELLOW}Found Docker process using port, stopping all containers...${NC}"
        docker stop $(docker ps -q) 2>/dev/null || true
        sleep 3
    else
        echo -e "${RED}❌ Port 8080 is in use by another process.${NC}"
        echo -e "${YELLOW}Please stop the process manually:${NC}"
        lsof -i:8080
        exit 1
    fi

    # Verify port is now free
    PORT_STILL_IN_USE=$(lsof -ti:8080 2>/dev/null || true)
    if [ -n "$PORT_STILL_IN_USE" ]; then
        echo -e "${RED}❌ Failed to free port 8080${NC}"
        exit 1
    fi
fi
echo -e "${GREEN}✅ Port 8080 is available${NC}\n"

echo -e "${BLUE}📋 Step 3: Starting services with docker-compose...${NC}"
docker-compose down -v 2>/dev/null || true
docker-compose up -d --build

echo -e "${GREEN}✅ Services started${NC}\n"

echo -e "${BLUE}📋 Step 4: Waiting for application to be healthy...${NC}"
ELAPSED=0
HEALTHY=false

while [ $ELAPSED -lt $MAX_WAIT_TIME ]; do
    if curl -f -s "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
        HEALTHY=true
        break
    fi
    echo -e "${YELLOW}⏳ Waiting for application... (${ELAPSED}s/${MAX_WAIT_TIME}s)${NC}"
    sleep 5
    ELAPSED=$((ELAPSED + 5))
done

if [ "$HEALTHY" = false ]; then
    echo -e "${RED}❌ Application did not become healthy within ${MAX_WAIT_TIME} seconds${NC}"
    echo -e "${YELLOW}📋 Application logs:${NC}"
    docker-compose logs configcat-demo
    exit 1
fi

echo -e "${GREEN}✅ Application is healthy (took ${ELAPSED}s)${NC}\n"

# Test counter
PASSED=0
FAILED=0

# Function to run a test
run_test() {
    local test_name=$1
    local test_command=$2

    echo -e "${BLUE}🧪 Testing: ${test_name}${NC}"

    if eval "$test_command"; then
        echo -e "${GREEN}✅ PASSED: ${test_name}${NC}\n"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}❌ FAILED: ${test_name}${NC}\n"
        FAILED=$((FAILED + 1))
    fi
}

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Running API Tests                                     ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Test 1: Health Check
run_test "Health Endpoint" \
    "curl -f -s ${BASE_URL}/actuator/health | grep -q '\"status\":\"UP\"'"

# Test 2: Prometheus Metrics
run_test "Prometheus Metrics Endpoint" \
    "curl -f -s ${BASE_URL}/actuator/prometheus | grep -q 'jvm_memory_used_bytes'"

# Test 3: OpenAPI Documentation
run_test "OpenAPI JSON Documentation" \
    "curl -f -s ${BASE_URL}/v3/api-docs | grep -q '\"openapi\":\"3'"

# Test 4: Swagger UI
run_test "Swagger UI Accessibility" \
    "curl -f -s ${BASE_URL}/swagger-ui.html > /dev/null"

# Test 5: User Management - Basic User
run_test "User Management API - Basic User" \
    "curl -f -s -X GET '${BASE_URL}/api/users/basic-user' \
        -H 'X-User-Country: US' \
        -H 'X-User-Subscription: basic' | grep -q '\"userId\":\"basic-user\"'"

# Test 6: User Management - Premium User
run_test "User Management API - Premium User" \
    "curl -f -s -X GET '${BASE_URL}/api/users/premium-user' \
        -H 'X-User-Country: BR' \
        -H 'X-User-Subscription: premium' | grep -q '\"userId\":\"premium-user\"'"

# Test 7: Payment API - Brazilian User
run_test "Payment API - Brazilian User with Stripe" \
    "curl -f -s -X POST '${BASE_URL}/api/payments/process' \
        -H 'Content-Type: application/json' \
        -H 'X-User-Country: BR' \
        -H 'X-Payment-Provider: stripe' \
        -d '{\"amount\": 99.99, \"currency\": \"BRL\", \"paymentMethod\": \"credit_card\", \"cardNumber\": \"4242424242424242\", \"isRecurring\": false}' \
        | grep -q '\"currency\":\"BRL\"'"

# Test 8: Payment API - US User with Recurring Payment
run_test "Payment API - US User with Recurring Payment" \
    "curl -f -s -X POST '${BASE_URL}/api/payments/process' \
        -H 'Content-Type: application/json' \
        -H 'X-User-Country: US' \
        -d '{\"amount\": 29.99, \"currency\": \"USD\", \"paymentMethod\": \"credit_card\", \"cardNumber\": \"5555555555554444\", \"isRecurring\": true}' \
        | grep -q '\"currency\":\"USD\"'"

# Test 9: ConfigCat Evaluation in Response
run_test "ConfigCat Evaluation in User Response" \
    "curl -f -s -X GET '${BASE_URL}/api/users/test-user' \
        -H 'X-User-Country: US' | grep -q 'configcat_evaluations'"

# Test 10: ConfigCat Evaluation in Payment Response
run_test "ConfigCat Evaluation in Payment Response" \
    "curl -f -s -X POST '${BASE_URL}/api/payments/process' \
        -H 'Content-Type: application/json' \
        -H 'X-User-Country: BR' \
        -d '{\"amount\": 50.00, \"currency\": \"BRL\", \"paymentMethod\": \"credit_card\", \"cardNumber\": \"4242424242424242\", \"isRecurring\": false}' \
        | grep -q 'configcat_evaluations'"

# Test 11: Correlation ID Propagation
run_test "Correlation ID Propagation" \
    "RESPONSE=\$(curl -s -X GET '${BASE_URL}/api/users/test-user' \
        -H 'X-Correlation-ID: test-correlation-123' \
        -H 'X-User-Country: US'); \
    echo \"\$RESPONSE\" | grep -q 'test-correlation-123'"

# Test 12: Multiple Feature Flags Evaluation
run_test "Multiple Feature Flags Evaluation" \
    "curl -f -s -X GET '${BASE_URL}/api/users/premium-user' \
        -H 'X-User-Country: BR' \
        -H 'X-User-Subscription: premium' \
        | grep -q '\"features\":'"

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Test Results Summary                                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

TOTAL=$((PASSED + FAILED))
echo -e "Total Tests:  ${TOTAL}"
echo -e "${GREEN}Passed:       ${PASSED}${NC}"

if [ $FAILED -gt 0 ]; then
    echo -e "${RED}Failed:       ${FAILED}${NC}"
    echo ""
    echo -e "${RED}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  ❌ TESTS FAILED                                       ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════════════════════╝${NC}"

    echo -e "\n${YELLOW}📋 Check application logs for details:${NC}"
    echo -e "  docker-compose logs configcat-demo"
else
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  ✅ ALL TESTS PASSED!                                  ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════╝${NC}"
fi

echo ""
echo -e "${BLUE}🌐 Services Currently Running:${NC}"
echo -e "  Application:  ${BASE_URL}"
echo -e "  Swagger UI:   ${BASE_URL}/swagger-ui.html"
echo -e "  Prometheus:   http://localhost:9090"
echo -e "  Grafana:      http://localhost:3000 (admin/admin)"
echo ""
echo -e "${YELLOW}💡 Useful commands:${NC}"
echo -e "  View logs:         docker-compose logs -f configcat-demo"
echo -e "  Follow all logs:   docker-compose logs -f"
echo -e "  Stop services:     docker-compose down"
echo ""

# Ask user for cleanup decision
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Cleanup Options                                       ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}What would you like to do?${NC}"
echo ""
echo -e "  ${GREEN}1)${NC} Clean up NOW - Stop all services and reclaim Docker resources"
echo -e "  ${BLUE}2)${NC} Keep services RUNNING - I want to check logs/application first"
echo -e "  ${RED}3)${NC} Exit WITHOUT cleanup - Leave everything as is"
echo ""

# Set default behavior for non-interactive environments (CI/CD)
if [ ! -t 0 ]; then
    echo -e "${YELLOW}Non-interactive mode detected. Using default: Clean up now${NC}"
    CLEANUP_CHOICE="1"
else
    read -p "Enter your choice [1-3] (default: 2): " CLEANUP_CHOICE
    CLEANUP_CHOICE=${CLEANUP_CHOICE:-2}
fi

case $CLEANUP_CHOICE in
    1)
        echo ""
        echo -e "${YELLOW}🧹 Starting cleanup process...${NC}"
        CLEANUP_ON_EXIT=true
        ;;
    2)
        echo ""
        echo -e "${BLUE}📋 Services will remain running for inspection${NC}"
        echo ""
        echo -e "${YELLOW}When you're done, run these commands to clean up:${NC}"
        echo -e "  ${GREEN}cd /Users/gbonespirito/Development/spring-boot-config-cat${NC}"
        echo -e "  ${GREEN}docker-compose down -v${NC}"
        echo -e "  ${GREEN}docker volume prune -f${NC}"
        echo -e "  ${GREEN}docker image prune -f${NC}"
        echo -e "  ${GREEN}docker rmi spring-boot-config-cat-configcat-demo${NC}"
        echo -e "  ${GREEN}docker builder prune -f${NC}"
        echo ""
        echo -e "${BLUE}Or run this one-liner:${NC}"
        echo -e "  ${GREEN}docker-compose down -v && docker volume prune -f && docker image prune -f && docker rmi spring-boot-config-cat-configcat-demo 2>/dev/null && docker builder prune -f${NC}"
        echo ""
        CLEANUP_ON_EXIT=false

        # Exit successfully without cleanup
        if [ $FAILED -gt 0 ]; then
            exit 1
        else
            exit 0
        fi
        ;;
    3)
        echo ""
        echo -e "${YELLOW}⚠️  Exiting without cleanup - All services remain running${NC}"
        echo -e "${YELLOW}Docker resources will NOT be reclaimed${NC}"
        CLEANUP_ON_EXIT=false

        # Exit successfully without cleanup
        if [ $FAILED -gt 0 ]; then
            exit 1
        else
            exit 0
        fi
        ;;
    *)
        echo ""
        echo -e "${RED}Invalid choice. Defaulting to option 2 (Keep services running)${NC}"
        CLEANUP_ON_EXIT=false

        # Exit successfully without cleanup
        if [ $FAILED -gt 0 ]; then
            exit 1
        else
            exit 0
        fi
        ;;
esac

# If we get here, user chose cleanup (option 1)
# The cleanup function will be called automatically via trap EXIT
if [ $FAILED -gt 0 ]; then
    exit 1
else
    exit 0
fi
