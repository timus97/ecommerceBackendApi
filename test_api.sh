#!/usr/bin/env bash
# =============================================================================
#  test_api.sh  –  Full Lifecycle API Test Suite
# =============================================================================
#
#  This script handles the entire test lifecycle:
#  1. Finds a free port.
#  2. Builds and starts the Spring Boot app in 'test' profile (auto-seeded).
#  3. Waits for the app to be ready.
#  4. Runs 60+ API assertions.
#  5. Shuts down the app and cleans up.
#
#  Usage:
#    cd /path/to/E-Commerce-Backend
#    ../test_api.sh
#
#  Or from project root:
#    ./test_api.sh
#
#  The script will:
#    - Find an available port automatically
#    - Build the application with 'test' profile
#    - Start the Spring Boot application
#    - Wait for startup completion and data seeding
#    - Execute 60+ comprehensive API tests
#    - Display results with pass/fail statistics
#    - Clean up and shut down the application
# =============================================================================

set -euo pipefail

# ── Configuration ────────────────────────────────────────────────────────────
# Determine if script is in project root or in E-Commerce-Backend directory
if [[ -f "mvnw" && -d "E-Commerce-Backend" ]]; then
    # Running from project root
    APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/E-Commerce-Backend" && pwd)"
elif [[ -f "mvnw" && -f "pom.xml" ]]; then
    # Running from E-Commerce-Backend directory
    APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
else
    # Fallback to script location
    APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
fi

LOG_FILE="${APP_DIR}/test_run.log"
PASS=0
FAIL=0
TODAY=$(date +%d-%m-%Y)
APP_PID=""

# ── Colours ──────────────────────────────────────────────────────────────────
GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
CYAN="\033[0;36m"
RESET="\033[0m"

# ── Lifecycle Helpers ────────────────────────────────────────────────────────

cleanup() {
    echo -e "\n${YELLOW}Stopping the application...${RESET}"
    if [[ -n "${APP_PID:-}" ]]; then
        kill "$APP_PID" 2>/dev/null || true
        wait "$APP_PID" 2>/dev/null || true
    fi
    echo -e "${GREEN}Cleanup complete.${RESET}"
}

trap cleanup EXIT

find_free_port() {
    # Try to find a free port using python3, fallback to a random port if fails
    python3 -c 'import socket; s=socket.socket(); s.bind(("", 0)); print(s.getsockname()[1]); s.close()' 2>/dev/null || echo $((RANDOM % 1000 + 8000))
}

wait_for_app() {
    local port=$1
    local url="http://localhost:${port}/swagger-ui.html"
    echo -ne "${YELLOW}Waiting for app to start on port ${port}...${RESET}"
    
    local count=0
    local max=60
    while true; do
        # Try to reach the app
        if curl -s --connect-timeout 2 --max-time 2 "$url" >/dev/null 2>&1; then
            echo -e " ${GREEN}READY!${RESET}"
            break
        fi
        
        echo -ne "."
        sleep 2
        ((count++))
        
        if [[ $count -eq $max ]]; then
            echo -e "\n${RED}Timeout waiting for app to start. Check ${LOG_FILE}${RESET}"
            echo -e "${RED}Last 20 lines of log:${RESET}"
            tail -20 "$LOG_FILE"
            exit 1
        fi
        
        # Check if process is still running
        if ! kill -0 "$APP_PID" 2>/dev/null; then
            echo -e "\n${RED}Application failed to start. Check ${LOG_FILE}${RESET}"
            echo -e "${RED}Last 20 lines of log:${RESET}"
            tail -20 "$LOG_FILE"
            exit 1
        fi
    done
    
    # Wait a bit more for DataSeeder to finish (it runs on start)
    echo -ne "${YELLOW}Waiting for DataSeeder to complete...${RESET}"
    local seeder_count=0
    while [[ $seeder_count -lt 30 ]]; do
        if grep -q "DataSeeder: DONE" "$LOG_FILE" 2>/dev/null; then
            echo -e " ${GREEN}DONE!${RESET}"
            break
        fi
        echo -ne "."
        sleep 1
        ((seeder_count++))
        if ! kill -0 "$APP_PID" 2>/dev/null; then 
            echo -e " ${YELLOW}(app stopped)${RESET}"
            break
        fi
    done
}

# ── Test Helpers ─────────────────────────────────────────────────────────────

section() { echo -e "\n${CYAN}══════════════════════════════════════════${RESET}"; \
            echo -e "${CYAN}  $1${RESET}"; \
            echo -e "${CYAN}══════════════════════════════════════════${RESET}"; }

assert() {
    local label="$1"
    local expected="$2"
    local actual="$3"
    local snippet="${4:-}"
    local body="${5:-}"

    local status_ok=false
    local body_ok=true

    [[ "$actual" == "$expected" ]] && status_ok=true

    if [[ -n "$snippet" && -n "$body" ]]; then
        echo "$body" | grep -qi "$snippet" || body_ok=false
    fi

    if $status_ok && $body_ok; then
        echo -e "  ${GREEN}✓ PASS${RESET}  [$actual]  $label"
        ((PASS++)) || true
    else
        echo -e "  ${RED}✗ FAIL${RESET}  [got=$actual exp=$expected]  $label"
        if [[ -n "$snippet" ]] && ! $body_ok; then
            echo -e "         ${RED}Body did not contain: '$snippet'${RESET}"
        fi
        if [[ -n "$body" ]]; then
            local response_preview=$(echo "$body" | head -c 300 | tr '\n' ' ')
            echo -e "         Response: $response_preview"
        fi
        ((FAIL++)) || true
    fi
}

RESP_BODY=""
curl_json() {
    local method="$1"; shift
    local path="$1";   shift
    local tmp
    tmp=$(mktemp)
    local status
    
    # Build curl command with proper headers and data handling
    local curl_cmd="curl -s -o \"$tmp\" -w \"%{http_code}\" -X \"$method\" -H \"Content-Type: application/json\""
    
    # Process additional arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -H)
                # Add custom header
                curl_cmd="$curl_cmd -H \"$2\""
                shift 2
                ;;
            -d)
                # Add data
                curl_cmd="$curl_cmd -d '$2'"
                shift 2
                ;;
            *)
                shift
                ;;
        esac
    done
    
    # Add the URL at the end
    curl_cmd="$curl_cmd \"${BASE_URL}${path}\""
    
    # Execute the curl command
    status=$(eval "$curl_cmd")
    RESP_BODY=$(cat "$tmp")
    rm -f "$tmp"
    echo "$status"
}

extract() {
    local field="$1"
    # Try to extract string value from JSON
    echo "$RESP_BODY" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" \
        | head -1 | sed 's/.*: *"\(.*\)"/\1/' || echo ""
}

extract_int() {
    local field="$1"
    # Try to extract integer value from JSON
    echo "$RESP_BODY" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*[0-9]*" \
        | head -1 | grep -o '[0-9]*$' || echo ""
}

# Helper to check if response contains text (case insensitive)
contains_text() {
    local text="$1"
    echo "$RESP_BODY" | grep -qi "$text" && return 0 || return 1
}

# ── Main Execution ───────────────────────────────────────────────────────────

echo -e "${YELLOW}Starting E-Commerce API Test Suite Lifecycle${RESET}"
echo "────────────────────────────────────────────"

# 1. Find free port
PORT=$(find_free_port)
BASE_URL="http://localhost:${PORT}"

# 2. Start Application
echo -e "${YELLOW}Building and starting application on port ${PORT}...${RESET}"
echo "Logs are being redirected to ${LOG_FILE}"

# Run in background
cd "$APP_DIR"
./mvnw spring-boot:run \
    -Dspring-boot.run.profiles=test \
    -Dspring-boot.run.arguments="--server.port=${PORT}" \
    > "$LOG_FILE" 2>&1 &
APP_PID=$!

# 3. Wait for readiness
wait_for_app "$PORT"

echo -e "\n${YELLOW}Running API Tests against ${BASE_URL}${RESET}"
echo "────────────────────────────────────────────"

# =============================================================================
section "1. SELLER – Registration & Login"
# =============================================================================

# 1.1 Register new seller (not a seeded one – fresh mobile/email)
STATUS=$(curl_json POST /register/seller -d '{
  "firstName":"Test",
  "lastName":"Seller",
  "mobile":"7000000001",
  "emailId":"test.seller@example.com",
  "password":"TestSell1"
}')
assert "Register new seller" 201 "$STATUS" "sellerId" "$RESP_BODY"
NEW_SELLER_ID=$(extract_int sellerId)

# 1.2 Duplicate seller registration
STATUS=$(curl_json POST /register/seller -d '{
  "firstName":"Test",
  "lastName":"Seller",
  "mobile":"7000000001",
  "emailId":"test.seller@example.com",
  "password":"TestSell1"
}')
assert "Duplicate seller → 400" 400 "$STATUS"

# 1.3 Login seller (seeded seller1)
STATUS=$(curl_json POST /login/seller -d '{
  "mobileId":"9876543210",
  "password":"Seller@123"
}')
assert "Login seeded seller1" 202 "$STATUS" "token" "$RESP_BODY"
SELLER1_TOKEN=$(extract token)

# 1.4 Login the newly registered seller
STATUS=$(curl_json POST /login/seller -d '{
  "mobileId":"7000000001",
  "password":"TestSell1"
}')
assert "Login new seller" 202 "$STATUS" "token" "$RESP_BODY"
NEW_SELLER_TOKEN=$(extract token)

# 1.5 Invalid seller login
STATUS=$(curl_json POST /login/seller -d '{
  "mobileId":"9876543210",
  "password":"WrongPass1"
}')
assert "Invalid seller password → 400" 400 "$STATUS"

# =============================================================================
section "2. CUSTOMER – Registration & Login"
# =============================================================================

# 2.1 Register new customer
STATUS=$(curl_json POST /register/customer -d '{
  "firstName":"Test",
  "lastName":"Customer",
  "mobileNo":"7000000002",
  "emailId":"test.customer@example.com",
  "password":"TestCust1"
}')
assert "Register new customer" 201 "$STATUS" "customerId" "$RESP_BODY"

# 2.2 Duplicate customer
STATUS=$(curl_json POST /register/customer -d '{
  "firstName":"Test",
  "lastName":"Customer",
  "mobileNo":"7000000002",
  "emailId":"test.customer@example.com",
  "password":"TestCust1"
}')
assert "Duplicate customer → 400" 400 "$STATUS"

# 2.3 Login seeded customer1 (has address + credit card)
STATUS=$(curl_json POST /login/customer -d '{
  "mobileId":"9123456780",
  "password":"Amit@1234"
}')
assert "Login seeded customer1" 202 "$STATUS" "token" "$RESP_BODY"
CUST1_TOKEN=$(extract token)

# 2.4 Login seeded customer2
STATUS=$(curl_json POST /login/customer -d '{
  "mobileId":"9234567801",
  "password":"Sneha@5678"
}')
assert "Login seeded customer2" 202 "$STATUS" "token" "$RESP_BODY"
CUST2_TOKEN=$(extract token)

# 2.5 Invalid customer login
STATUS=$(curl_json POST /login/customer -d '{
  "mobileId":"9123456780",
  "password":"WrongPass1"
}')
assert "Invalid customer password → 400" 400 "$STATUS"

# =============================================================================
section "3. SELLER – Profile & Update"
# =============================================================================

# 3.1 Get all sellers (public)
STATUS=$(curl_json GET /sellers)
assert "GET /sellers" 200 "$STATUS" "sellerId" "$RESP_BODY"

# 3.2 Get seller by id
STATUS=$(curl_json GET /seller/1)
assert "GET /seller/1" 200 "$STATUS"

# 3.3 Get seller by mobile (requires seller token)
STATUS=$(curl_json GET "/seller?mobile=9876543210" -H "token: $SELLER1_TOKEN")
assert "GET /seller?mobile=9876543210" 200 "$STATUS" "mobile" "$RESP_BODY"

# 3.4 Get currently logged-in seller
STATUS=$(curl_json GET /seller/current -H "token: $SELLER1_TOKEN")
assert "GET /seller/current" 200 "$STATUS" "emailId" "$RESP_BODY"

# 3.5 Update seller (full body)
STATUS=$(curl_json PUT /seller \
  -H "token: $SELLER1_TOKEN" \
  -d '{
    "firstName":"Ravi",
    "lastName":"Sharma Updated",
    "mobile":"9876543210",
    "emailId":"ravi.sharma@techstore.com",
    "password":"Seller@123"
  }')
assert "PUT /seller (update)" 202 "$STATUS"

# 3.6 Update seller mobile
STATUS=$(curl_json PUT /seller/update/mobile \
  -H "token: $NEW_SELLER_TOKEN" \
  -d '{"mobileId":"7000000001","password":"TestSell1","newMobile":"7000000099"}')
assert "PUT /seller/update/mobile" 202 "$STATUS"

# 3.7 Update seller password (logs out, new token needed)
STATUS=$(curl_json PUT /seller/update/password \
  -H "token: $NEW_SELLER_TOKEN" \
  -d '{"mobileId":"7000000099","password":"TestSell2"}')
assert "PUT /seller/update/password" 202 "$STATUS"

# Re-login new seller with updated credentials
STATUS=$(curl_json POST /login/seller -d '{
  "mobileId":"7000000099",
  "password":"TestSell2"
}')
assert "Re-login new seller after password change" 202 "$STATUS" "token" "$RESP_BODY"
NEW_SELLER_TOKEN=$(extract token)

# =============================================================================
section "4. PRODUCTS – CRUD & Filters"
# =============================================================================

# 4.1 Get all products (public)
STATUS=$(curl_json GET /products)
assert "GET /products (all)" 200 "$STATUS" "productId" "$RESP_BODY"

# 4.2 Get product by id
STATUS=$(curl_json GET /product/1)
assert "GET /product/1" 302 "$STATUS"

# 4.3 Get products by category – ELECTRONICS
STATUS=$(curl_json GET /products/ELECTRONICS)
assert "GET /products/ELECTRONICS" 200 "$STATUS"

# 4.4 Get products by category – BOOKS
STATUS=$(curl_json GET /products/BOOKS)
assert "GET /products/BOOKS" 200 "$STATUS"

# 4.5 Get products by category – FASHION
STATUS=$(curl_json GET /products/FASHION)
assert "GET /products/FASHION" 200 "$STATUS"

# 4.6 Get products by category – FURNITURE
STATUS=$(curl_json GET /products/FURNITURE)
assert "GET /products/FURNITURE" 200 "$STATUS"

# 4.7 Get products by category – GROCERIES
STATUS=$(curl_json GET /products/GROCERIES)
assert "GET /products/GROCERIES" 200 "$STATUS"

# 4.8 Get products by status – AVAILABLE
STATUS=$(curl_json GET /products/status/AVAILABLE)
assert "GET /products/status/AVAILABLE" 200 "$STATUS"

# 4.9 Get products by status – OUTOFSTOCK
STATUS=$(curl_json GET /products/status/OUTOFSTOCK)
assert "GET /products/status/OUTOFSTOCK" 200 "$STATUS"

# 4.10 Get products by seller id
STATUS=$(curl_json GET /products/seller/1)
assert "GET /products/seller/1" 200 "$STATUS"

# 4.11 Add product (requires seller token)
STATUS=$(curl_json POST /products \
  -H "token: $SELLER1_TOKEN" \
  -d '{
    "productName":"Test Laptop",
    "price":55000.0,
    "description":"A test laptop for API testing",
    "manufacturer":"TestBrand",
    "quantity":10,
    "category":"ELECTRONICS",
    "status":"AVAILABLE"
  }')
assert "POST /products (add product)" 202 "$STATUS" "productId" "$RESP_BODY"
NEW_PRODUCT_ID=$(extract_int productId)

# 4.12 Update product (full)
STATUS=$(curl_json PUT /products -d '{
  "productId":'"$NEW_PRODUCT_ID"',
  "productName":"Test Laptop Pro",
  "price":59999.0,
  "description":"Updated test laptop",
  "manufacturer":"TestBrand",
  "quantity":8,
  "category":"ELECTRONICS",
  "status":"AVAILABLE"
}')
assert "PUT /products (update)" 200 "$STATUS"

# 4.13 Update product quantity only
STATUS=$(curl_json PUT /products/"$NEW_PRODUCT_ID" -d '{"quantity":15}')
assert "PUT /products/{id} (update qty)" 202 "$STATUS"

# 4.14 Delete product
STATUS=$(curl_json DELETE /product/"$NEW_PRODUCT_ID")
assert "DELETE /product/{id}" 200 "$STATUS"

# =============================================================================
section "5. CUSTOMER – Profile & Updates"
# =============================================================================

# 5.1 Get current customer details
STATUS=$(curl_json GET /customer/current -H "token: $CUST1_TOKEN")
assert "GET /customer/current" 202 "$STATUS" "customerId" "$RESP_BODY"

# 5.2 Get all customers (requires seller token)
STATUS=$(curl_json GET /customers -H "token: $SELLER1_TOKEN")
assert "GET /customers (seller token)" 202 "$STATUS"

# 5.3 Get all customers with customer token → should fail
STATUS=$(curl_json GET /customers -H "token: $CUST1_TOKEN")
assert "GET /customers (customer token) → 400" 400 "$STATUS"

# 5.4 Update customer (general)
STATUS=$(curl_json PUT /customer \
  -H "token: $CUST1_TOKEN" \
  -d '{
    "firstName":"Amit Updated",
    "lastName":"Verma",
    "mobileNo":"9123456780",
    "emailId":"amit.verma@gmail.com",
    "password":"Amit@1234"
  }')
assert "PUT /customer (update)" 202 "$STATUS"

# 5.5 Update credit card
STATUS=$(curl_json PUT /customer/update/card \
  -H "token: $CUST1_TOKEN" \
  -d '{
    "cardNumber":"4111111111111111",
    "cardValidity":"12/26",
    "cardCVV":"123"
  }')
assert "PUT /customer/update/card" 202 "$STATUS" "cardNumber" "$RESP_BODY"

# 5.6 Add/update home address
STATUS=$(curl_json PUT "/customer/update/address?type=home" \
  -H "token: $CUST1_TOKEN" \
  -d '{
    "streetNo":"12",
    "buildingName":"Sunrise Apartments",
    "locality":"Koramangala",
    "city":"Bangalore",
    "state":"KARNATAKA",
    "pincode":"560034"
  }')
assert "PUT /customer/update/address?type=home" 202 "$STATUS"

# 5.7 Add work address
STATUS=$(curl_json PUT "/customer/update/address?type=work" \
  -H "token: $CUST1_TOKEN" \
  -d '{
    "streetNo":"55",
    "buildingName":"Tech Tower",
    "locality":"MG Road",
    "city":"Bangalore",
    "state":"KARNATAKA",
    "pincode":"560001"
  }')
assert "PUT /customer/update/address?type=work" 202 "$STATUS"

# 5.8 Delete address
STATUS=$(curl_json DELETE "/customer/delete/address?type=work" \
  -H "token: $CUST1_TOKEN")
assert "DELETE /customer/delete/address?type=work" 202 "$STATUS"

# 5.9 Update credentials (mobile/email)
STATUS=$(curl_json PUT /customer/update/credentials \
  -H "token: $CUST2_TOKEN" \
  -d '{
    "mobileNo":"9234567801",
    "emailId":"sneha.patel.updated@gmail.com"
  }')
assert "PUT /customer/update/credentials" 202 "$STATUS"

# =============================================================================
section "6. CART – Add / View / Remove / Clear"
# =============================================================================

# 6.1 Add product 1 to cart
STATUS=$(curl_json POST /cart/add \
  -H "token: $CUST1_TOKEN" \
  -d '{"productId":1,"quantity":2}')
assert "POST /cart/add (product 1, qty 2)" 201 "$STATUS" "cartItems" "$RESP_BODY"

# 6.2 Add product 2 to cart
STATUS=$(curl_json POST /cart/add \
  -H "token: $CUST1_TOKEN" \
  -d '{"productId":2,"quantity":1}')
assert "POST /cart/add (product 2, qty 1)" 201 "$STATUS"

# 6.3 Add product 6 (book) to cart
STATUS=$(curl_json POST /cart/add \
  -H "token: $CUST1_TOKEN" \
  -d '{"productId":6,"quantity":3}')
assert "POST /cart/add (product 6, qty 3)" 201 "$STATUS"

# 6.4 View cart
STATUS=$(curl_json GET /cart -H "token: $CUST1_TOKEN")
assert "GET /cart" 202 "$STATUS" "cartTotal" "$RESP_BODY"

# 6.5 Remove one product from cart
STATUS=$(curl_json DELETE /cart \
  -H "token: $CUST1_TOKEN" \
  -d '{"productId":6,"quantity":1}')
assert "DELETE /cart (remove product 6)" 200 "$STATUS"

# 6.6 Clear cart (will be refilled for order test)
STATUS=$(curl_json DELETE /cart/clear -H "token: $CUST1_TOKEN")
assert "DELETE /cart/clear" 202 "$STATUS"

# =============================================================================
section "7. WISHLIST – Add / View / Check / Remove / Move-to-Cart"
# =============================================================================

# 7.1 Add product 4 (jeans) to wishlist
STATUS=$(curl_json POST /wishlist/4 -H "token: $CUST1_TOKEN")
assert "POST /wishlist/4 (add to wishlist)" 201 "$STATUS" "productId" "$RESP_BODY"

# 7.2 Add product 5 (Nike shoes) to wishlist
STATUS=$(curl_json POST /wishlist/5 -H "token: $CUST1_TOKEN")
assert "POST /wishlist/5 (add to wishlist)" 201 "$STATUS"

# 7.3 Add product 7 (Effective Java) to wishlist
STATUS=$(curl_json POST /wishlist/7 -H "token: $CUST1_TOKEN")
assert "POST /wishlist/7 (add to wishlist)" 201 "$STATUS"

# 7.4 Duplicate add → should fail
STATUS=$(curl_json POST /wishlist/4 -H "token: $CUST1_TOKEN")
assert "POST /wishlist/4 duplicate → 400" 400 "$STATUS"

# 7.5 View wishlist (sorted newest first)
STATUS=$(curl_json GET /wishlist -H "token: $CUST1_TOKEN")
assert "GET /wishlist" 200 "$STATUS" "wishlistItemId" "$RESP_BODY"

# 7.6 Check product 4 is wishlisted
STATUS=$(curl_json GET /wishlist/4/check -H "token: $CUST1_TOKEN")
assert "GET /wishlist/4/check → wishlisted=true" 200 "$STATUS" "true" "$RESP_BODY"

# 7.7 Check product 9 is NOT wishlisted
STATUS=$(curl_json GET /wishlist/9/check -H "token: $CUST1_TOKEN")
assert "GET /wishlist/9/check → wishlisted=false" 200 "$STATUS" "false" "$RESP_BODY"

# 7.8 Remove product 7 from wishlist
STATUS=$(curl_json DELETE /wishlist/7 -H "token: $CUST1_TOKEN")
assert "DELETE /wishlist/7" 200 "$STATUS" "message" "$RESP_BODY"

# 7.9 Move product 5 from wishlist to cart
STATUS=$(curl_json POST /wishlist/5/move-to-cart -H "token: $CUST1_TOKEN")
assert "POST /wishlist/5/move-to-cart" 200 "$STATUS" "cartItems" "$RESP_BODY"

# 7.10 Confirm product 5 no longer in wishlist
STATUS=$(curl_json GET /wishlist/5/check -H "token: $CUST1_TOKEN")
assert "GET /wishlist/5/check after move → false" 200 "$STATUS" "false" "$RESP_BODY"

# 7.11 Remove non-existent wishlist item → 400
STATUS=$(curl_json DELETE /wishlist/999 -H "token: $CUST1_TOKEN")
assert "DELETE /wishlist/999 (not in wishlist) → 400" 400 "$STATUS"

# =============================================================================
section "8. ORDERS – Place / Get / Update / Cancel"
# =============================================================================

# Prepare cart for order: add products 1 and 2
STATUS=$(curl_json POST /cart/add \
  -H "token: $CUST1_TOKEN" \
  -d '{"productId":1,"quantity":1}')
assert "Cart setup: add product 1" 201 "$STATUS"

STATUS=$(curl_json POST /cart/add \
  -H "token: $CUST1_TOKEN" \
  -d '{"productId":2,"quantity":1}')
assert "Cart setup: add product 2" 201 "$STATUS"

# 8.1 Place order (SUCCESS – correct card details)
STATUS=$(curl_json POST /order/place \
  -H "token: $CUST1_TOKEN" \
  -d '{
    "cardNumber":{
      "cardNumber":"4111111111111111",
      "cardValidity":"12/26",
      "cardCVV":"123"
    },
    "addressType":"home"
  }')
assert "POST /order/place (SUCCESS)" 201 "$STATUS" "orderStatus" "$RESP_BODY"
ORDER1_ID=$(extract_int orderId)

# 8.2 Place order with wrong card → PENDING
# First refill cart
STATUS=$(curl_json POST /cart/add \
  -H "token: $CUST1_TOKEN" \
  -d '{"productId":6,"quantity":1}')
assert "Cart setup for PENDING order" 201 "$STATUS"

STATUS=$(curl_json POST /order/place \
  -H "token: $CUST1_TOKEN" \
  -d '{
    "cardNumber":{
      "cardNumber":"9999999999999999",
      "cardValidity":"01/25",
      "cardCVV":"000"
    },
    "addressType":"home"
  }')
assert "POST /order/place (PENDING – wrong card)" 201 "$STATUS" "PENDING" "$RESP_BODY"
ORDER2_ID=$(extract_int orderId)

# 8.3 Get all orders
STATUS=$(curl_json GET /orders)
assert "GET /orders (all)" 200 "$STATUS"

# 8.4 Get order by id
STATUS=$(curl_json GET /orders/"$ORDER1_ID")
assert "GET /orders/$ORDER1_ID" 200 "$STATUS" "orderId" "$RESP_BODY"

# 8.5 Get customer by order id
STATUS=$(curl_json GET /customer/"$ORDER1_ID")
assert "GET /customer/{orderId}" 200 "$STATUS" "customerId" "$RESP_BODY"

# 8.6 Get orders by date (today)
STATUS=$(curl_json GET "/orders/by/date?date=$TODAY")
assert "GET /orders/by/date?date=$TODAY" 200 "$STATUS"

# 8.7 Get customer's own orders
STATUS=$(curl_json GET /customer/orders -H "token: $CUST1_TOKEN")
assert "GET /customer/orders" 202 "$STATUS"

# 8.8 Update PENDING order to SUCCESS (correct card this time)
STATUS=$(curl_json PUT /orders/"$ORDER2_ID" \
  -H "token: $CUST1_TOKEN" \
  -d '{
    "cardNumber":{
      "cardNumber":"4111111111111111",
      "cardValidity":"12/26",
      "cardCVV":"123"
    },
    "addressType":"home"
  }')
assert "PUT /orders/$ORDER2_ID (update to SUCCESS)" 202 "$STATUS" "SUCCESS" "$RESP_BODY"

# 8.9 Cancel order
STATUS=$(curl_json DELETE /orders/"$ORDER1_ID" -H "token: $CUST1_TOKEN")
assert "DELETE /orders/$ORDER1_ID (cancel)" 200 "$STATUS" "CANCELLED" "$RESP_BODY"

# 8.10 Cancel already-cancelled order → 400
STATUS=$(curl_json DELETE /orders/"$ORDER1_ID" -H "token: $CUST1_TOKEN")
assert "DELETE /orders/$ORDER1_ID (already cancelled) → 400" 400 "$STATUS"

# =============================================================================
section "9. REVIEWS – Add / Update / Delete / List / Summary"
# =============================================================================

# 9.1 Add review to product 1
STATUS=$(curl_json POST /products/1/reviews \
  -H "token: $CUST1_TOKEN" \
  -d '{
    "productId":1,
    "rating":5,
    "title":"Excellent phone!",
    "comment":"This Samsung phone is absolutely brilliant. Fast, great camera, long battery life."
  }')
assert "POST /products/1/reviews (add review)" 201 "$STATUS" "reviewId" "$RESP_BODY"
REVIEW1_ID=$(extract_int reviewId)

# 9.2 Add second review (different customer)
STATUS=$(curl_json POST /products/1/reviews \
  -H "token: $CUST2_TOKEN" \
  -d '{
    "productId":1,
    "rating":4,
    "title":"Very good value",
    "comment":"Great phone for the price. Camera is outstanding and performance is top notch."
  }')
assert "POST /products/1/reviews (second review)" 201 "$STATUS"
REVIEW2_ID=$(extract_int reviewId)

# 9.3 Add review to product 6 (book)
STATUS=$(curl_json POST /products/6/reviews \
  -H "token: $CUST1_TOKEN" \
  -d '{
    "productId":6,
    "rating":5,
    "title":"Must-read for developers",
    "comment":"Every developer should read this book. Clean Code principles changed the way I write software."
  }')
assert "POST /products/6/reviews (book review)" 201 "$STATUS"

# 9.4 Update review
STATUS=$(curl_json PUT /reviews/"$REVIEW1_ID" \
  -H "token: $CUST1_TOKEN" \
  -d '{
    "productId":1,
    "rating":4,
    "title":"Updated: Great phone",
    "comment":"After using it for a month, still very happy. Slightly reduced rating due to heating issues."
  }')
assert "PUT /reviews/$REVIEW1_ID (update)" 200 "$STATUS" "rating" "$RESP_BODY"

# 9.5 Get paginated reviews for product 1
STATUS=$(curl_json GET "/products/1/reviews?page=0&size=5&sortBy=createdAt&direction=desc")
assert "GET /products/1/reviews (paginated)" 200 "$STATUS" "content" "$RESP_BODY"

# 9.6 Get reviews sorted by rating ascending
STATUS=$(curl_json GET "/products/1/reviews?page=0&size=10&sortBy=rating&direction=asc")
assert "GET /products/1/reviews (sort by rating asc)" 200 "$STATUS"

# 9.7 Get review summary for product 1
STATUS=$(curl_json GET /products/1/reviews/summary)
assert "GET /products/1/reviews/summary" 200 "$STATUS" "averageRating" "$RESP_BODY"

# 9.8 Delete review (soft delete)
STATUS=$(curl_json DELETE /reviews/"$REVIEW2_ID" -H "token: $CUST2_TOKEN")
assert "DELETE /reviews/$REVIEW2_ID (soft delete)" 200 "$STATUS"

# =============================================================================
section "10. SELLER – Delete"
# =============================================================================

# 10.1 Delete new seller
STATUS=$(curl_json DELETE /seller/"$NEW_SELLER_ID" -H "token: $NEW_SELLER_TOKEN")
assert "DELETE /seller/$NEW_SELLER_ID" 200 "$STATUS"

# =============================================================================
section "11. CUSTOMER – Password Update & Delete"
# =============================================================================

# 11.1 Update customer password (logs out automatically)
# Login customer3 first
STATUS=$(curl_json POST /login/customer -d '{
  "mobileId":"9345678012",
  "password":"Rahul@9012"
}')
assert "Login customer3" 202 "$STATUS" "token" "$RESP_BODY"
CUST3_TOKEN=$(extract token)

STATUS=$(curl_json PUT /customer/update/password \
  -H "token: $CUST3_TOKEN" \
  -d '{
    "mobileId":"9345678012",
    "password":"Rahul@NewPass1"
  }')
assert "PUT /customer/update/password" 202 "$STATUS" "message" "$RESP_BODY"

# Re-login with new password
STATUS=$(curl_json POST /login/customer -d '{
  "mobileId":"9345678012",
  "password":"Rahul@NewPass1"
}')
assert "Re-login customer3 after password change" 202 "$STATUS" "token" "$RESP_BODY"
CUST3_TOKEN=$(extract token)

# 11.2 Delete customer account
STATUS=$(curl_json DELETE /customer \
  -H "token: $CUST3_TOKEN" \
  -d '{
    "mobileId":"9345678012",
    "password":"Rahul@NewPass1"
  }')
assert "DELETE /customer (delete account)" 202 "$STATUS" "message" "$RESP_BODY"

# =============================================================================
section "12. LOGOUT"
# =============================================================================

# 12.1 Logout customer1
STATUS=$(curl_json POST /logout/customer -d "{\"token\":\"$CUST1_TOKEN\"}")
assert "POST /logout/customer (customer1)" 202 "$STATUS"

# 12.2 Use expired token → should fail
STATUS=$(curl_json GET /customer/current -H "token: $CUST1_TOKEN")
assert "Expired token → 400" 400 "$STATUS"

# 12.3 Logout customer2
STATUS=$(curl_json POST /logout/customer -d "{\"token\":\"$CUST2_TOKEN\"}")
assert "POST /logout/customer (customer2)" 202 "$STATUS"

# 12.4 Logout seller1
STATUS=$(curl_json POST /logout/seller -d "{\"token\":\"$SELLER1_TOKEN\"}")
assert "POST /logout/seller (seller1)" 202 "$STATUS"

# =============================================================================
section "13. EDGE CASES & ERROR HANDLING"
# =============================================================================

# 13.1 Non-existent product
STATUS=$(curl_json GET /product/99999)
assert "GET /product/99999 (not found) → 404" 404 "$STATUS"

# 13.2 Invalid category enum
STATUS=$(curl_json GET /products/INVALID_CATEGORY)
assert "GET /products/INVALID_CATEGORY → 400 or 500" 500 "$STATUS"

# 13.3 No token header → 400
STATUS=$(curl_json GET /customer/current)
assert "GET /customer/current (no token) → 400" 400 "$STATUS"

# 13.4 Invalid token string
STATUS=$(curl_json GET /cart -H "token: invalid-token-xyz")
assert "GET /cart (invalid token) → 400" 400 "$STATUS"

# 13.5 Add product with missing required fields
STATUS=$(curl_json POST /products \
  -H "token: $SELLER1_TOKEN" \
  -d '{"price":100.0}')
assert "POST /products (missing fields) → 400" 400 "$STATUS"

# 13.6 Register customer with invalid mobile
STATUS=$(curl_json POST /register/customer -d '{
  "firstName":"Bad",
  "lastName":"Mobile",
  "mobileNo":"12345",
  "emailId":"bad@test.com",
  "password":"BadMob@12"
}')
assert "Register with invalid mobile → 400" 400 "$STATUS"

# 13.7 Register customer with invalid email
STATUS=$(curl_json POST /register/customer -d '{
  "firstName":"Bad",
  "lastName":"Email",
  "mobileNo":"7111111111",
  "emailId":"not-an-email",
  "password":"BadEml@12"
}')
assert "Register with invalid email → 400" 400 "$STATUS"

# 13.8 Register customer with weak password
STATUS=$(curl_json POST /register/customer -d '{
  "firstName":"Weak",
  "lastName":"Pass",
  "mobileNo":"7222222222",
  "emailId":"weak@test.com",
  "password":"weak"
}')
assert "Register with weak password → 400" 400 "$STATUS"

# =============================================================================
# SUMMARY
# =============================================================================
TOTAL=$((PASS + FAIL))
echo ""
echo -e "${CYAN}══════════════════════════════════════════${RESET}"
echo -e "${CYAN}  TEST SUMMARY${RESET}"
echo -e "${CYAN}══════════════════════════════════════════${RESET}"
echo -e "  Total  : $TOTAL"
echo -e "  ${GREEN}Passed : $PASS${RESET}"
if [ "$FAIL" -gt 0 ]; then
    echo -e "  ${RED}Failed : $FAIL${RESET}"
else
    echo -e "  ${GREEN}Failed : $FAIL${RESET}"
fi
echo ""

if [ "$FAIL" -eq 0 ]; then
    echo -e "${GREEN}  ✔  All tests passed!${RESET}"
    exit 0
else
    echo -e "${RED}  ✘  Some tests failed. Check output above.${RESET}"
    exit 1
fi
