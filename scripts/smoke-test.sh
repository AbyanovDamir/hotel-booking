#!/usr/bin/env sh
set -eu

BASE_URL=${BASE_URL:-http://localhost:8080}
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM

request() {
  method=$1
  path=$2
  expected=$3
  body=${4:-}
  shift 4 || true
  response="$TMP_DIR/response.json"
  if [ -n "$body" ]; then
    status=$(curl -sS -o "$response" -w '%{http_code}' -X "$method" "$BASE_URL$path" \
      -H 'Content-Type: application/json' "$@" --data "$body")
  else
    status=$(curl -sS -o "$response" -w '%{http_code}' -X "$method" "$BASE_URL$path" "$@")
  fi
  if [ "$status" != "$expected" ]; then
    echo "FAILED $method $path: expected $expected, got $status"
    cat "$response"
    exit 1
  fi
  echo "OK $method $path -> $status"
}

echo "Waiting for API Gateway..."
attempt=0
until curl -fsS "$BASE_URL/actuator/health" > /dev/null 2>&1; do
  attempt=$((attempt + 1))
  if [ "$attempt" -ge 60 ]; then
    echo "Gateway did not become healthy in time"
    exit 1
  fi
  sleep 2
done

request GET /actuator/health 200 ""
request POST /api/auth/token 200 '{"username":"user","password":"user"}'
TOKEN=$(sed -n 's/.*"token":"\([^"]*\)".*/\1/p' "$TMP_DIR/response.json")
if [ -z "$TOKEN" ]; then
  echo "Authentication response does not contain a token"
  exit 1
fi

AUTH_HEADER="Authorization: Bearer $TOKEN"
request GET '/api/rooms/recommend?startDate=2099-01-10&endDate=2099-01-12' 200 "" -H "$AUTH_HEADER"

KEY="smoke-$(date +%s)"
BOOKING='{"autoSelect":true,"startDate":"2099-01-10","endDate":"2099-01-12"}'
request POST /api/bookings 201 "$BOOKING" -H "$AUTH_HEADER" -H "Idempotency-Key: $KEY"
FIRST_RESPONSE=$(cat "$TMP_DIR/response.json")
request POST /api/bookings 201 "$BOOKING" -H "$AUTH_HEADER" -H "Idempotency-Key: $KEY"
SECOND_RESPONSE=$(cat "$TMP_DIR/response.json")
if [ "$FIRST_RESPONSE" != "$SECOND_RESPONSE" ]; then
  echo "Repeated Idempotency-Key returned a different booking"
  exit 1
fi

request GET '/api/bookings?page=0&size=20' 200 "" -H "$AUTH_HEADER"
echo "All smoke tests passed"
