#!/bin/bash

# ============================================================
# CloudTracker API Test Script
# Tests: register → login → write value → read value
# ============================================================

BASE_URL="http://localhost:3000"   # ← change to your server
USERNAME="testuser"
PASSWORD="testpassword123"
TEST_KEY="hello"
TEST_VALUE='{"message": "Hello from curl!", "number": 42, "active": true}'

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_step() { echo -e "\n${BLUE}▶ $1${NC}"; }
print_ok()   { echo -e "${GREEN}✓ $1${NC}"; }
print_err()  { echo -e "${RED}✗ $1${NC}"; exit 1; }
print_info() { echo -e "${YELLOW}  $1${NC}"; }

echo -e "${BLUE}==============================${NC}"
echo -e "${BLUE}  CloudTracker API Test       ${NC}"
echo -e "${BLUE}  Server: $BASE_URL           ${NC}"
echo -e "${BLUE}==============================${NC}"

# ------------------------------------------------------------
# 1. REGISTER
# ------------------------------------------------------------
print_step "1. Registering user '$USERNAME'..."

REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"$USERNAME\", \"password\": \"$PASSWORD\"}")

echo "  Response: $REGISTER_RESPONSE"

# Accept success or "already taken" (so script is re-runnable)
if echo "$REGISTER_RESPONSE" | grep -q '"success"'; then
  print_ok "Registered successfully"
elif echo "$REGISTER_RESPONSE" | grep -q "already taken"; then
  print_ok "User already exists — continuing to login"
else
  print_err "Registration failed: $REGISTER_RESPONSE"
fi

# ------------------------------------------------------------
# 2. LOGIN
# ------------------------------------------------------------
print_step "2. Logging in as '$USERNAME'..."

LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"$USERNAME\", \"password\": \"$PASSWORD\"}")

echo "  Response: $LOGIN_RESPONSE"

# Extract token (works without jq)
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  print_err "Login failed — no token received: $LOGIN_RESPONSE"
fi

print_ok "Logged in — token received"
print_info "Token: ${TOKEN:0:40}..."

# ------------------------------------------------------------
# 3. WRITE TEST VALUE
# ------------------------------------------------------------
print_step "3. Writing test value to key '$TEST_KEY'..."
print_info "Payload: $TEST_VALUE"

WRITE_RESPONSE=$(curl -s -X POST "$BASE_URL/data/$TEST_KEY" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "$TEST_VALUE")

echo "  Response: $WRITE_RESPONSE"

if echo "$WRITE_RESPONSE" | grep -q '"success"'; then
  print_ok "Value written successfully"
else
  print_err "Write failed: $WRITE_RESPONSE"
fi

# ------------------------------------------------------------
# 4. READ TEST VALUE
# ------------------------------------------------------------
print_step "4. Reading back key '$TEST_KEY'..."

READ_RESPONSE=$(curl -s -X GET "$BASE_URL/data/$TEST_KEY" \
  -H "Authorization: Bearer $TOKEN")

echo "  Response: $READ_RESPONSE"

if echo "$READ_RESPONSE" | grep -q "Hello from curl!"; then
  print_ok "Value read back successfully"
else
  print_err "Read failed or value mismatch: $READ_RESPONSE"
fi

# ------------------------------------------------------------
# 5. LIST ALL KEYS
# ------------------------------------------------------------
print_step "5. Listing all keys for '$USERNAME'..."

LIST_RESPONSE=$(curl -s -X GET "$BASE_URL/data" \
  -H "Authorization: Bearer $TOKEN")

echo "  Response: $LIST_RESPONSE"
print_ok "Keys listed"

# ------------------------------------------------------------
# DONE
# ------------------------------------------------------------
echo -e "\n${GREEN}=============================="
echo -e "  All tests passed! ✓"
echo -e "==============================${NC}\n"
