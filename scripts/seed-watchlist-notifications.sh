#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
READINESS_PATH="${READINESS_PATH:-/api-docs}"
OWNER_NAME="${OWNER_NAME:-alice}"
LIST_NAME="${LIST_NAME:-Tech}"
TICKER="${TICKER:-AAPL}"
THRESHOLD_PRICE="${THRESHOLD_PRICE:-9999.00}"
WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS:-60}"
CURL_MAX_TIME_SECONDS="${CURL_MAX_TIME_SECONDS:-5}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

wait_for_app() {
  local start_ts now elapsed
  start_ts="$(date +%s)"

  echo "Waiting for HexaStock at ${BASE_URL}${READINESS_PATH} ..."
  while true; do
    if curl --silent --show-error --fail --max-time "${CURL_MAX_TIME_SECONDS}" \
      "${BASE_URL}${READINESS_PATH}" >/dev/null 2>&1; then
      echo "Application is ready."
      return 0
    fi

    now="$(date +%s)"
    elapsed="$((now - start_ts))"
    if [ "${elapsed}" -ge "${WAIT_TIMEOUT_SECONDS}" ]; then
      echo "Timed out after ${WAIT_TIMEOUT_SECONDS}s waiting for ${BASE_URL}${READINESS_PATH}" >&2
      exit 1
    fi

    sleep 1
  done
}

post_json() {
  local path="$1"
  local body="$2"

  curl --silent --show-error --fail \
    --max-time "${CURL_MAX_TIME_SECONDS}" \
    -X POST \
    -H "Content-Type: application/json" \
    -d "${body}" \
    "${BASE_URL}${path}"
}

parse_json_field() {
  local field_name="$1"

  python3 -c 'import json, sys; print(json.load(sys.stdin)[sys.argv[1]])' "${field_name}"
}

require_command curl
require_command python3

wait_for_app

echo "Creating portfolio for owner=${OWNER_NAME} ..."
portfolio_response="$(post_json "/api/portfolios" "{\"ownerName\":\"${OWNER_NAME}\"}")"
portfolio_id="$(printf '%s' "${portfolio_response}" | parse_json_field "id")"
echo "Portfolio created: ${portfolio_id}"

echo "Creating watchlist listName=${LIST_NAME} ..."
watchlist_response="$(post_json "/api/watchlists" "{\"ownerName\":\"${OWNER_NAME}\",\"listName\":\"${LIST_NAME}\"}")"
watchlist_id="$(printf '%s' "${watchlist_response}" | parse_json_field "id")"
echo "Watchlist created: ${watchlist_id}"

echo "Adding guaranteed alert ticker=${TICKER} threshold=${THRESHOLD_PRICE} ..."
post_json "/api/watchlists/${watchlist_id}/alerts" \
  "{\"ticker\":\"${TICKER}\",\"thresholdPrice\":\"${THRESHOLD_PRICE}\"}" >/dev/null
echo "Alert added."

echo "Activating watchlist ..."
curl --silent --show-error --fail --max-time "${CURL_MAX_TIME_SECONDS}" \
  -X POST "${BASE_URL}/api/watchlists/${watchlist_id}/activation" >/dev/null
echo "Watchlist activated."

echo "Sampling current price twice ..."
sample_price_1="$(curl --silent --show-error --fail --max-time "${CURL_MAX_TIME_SECONDS}" \
  "${BASE_URL}/api/stocks/${TICKER}")"
sample_price_2="$(curl --silent --show-error --fail --max-time "${CURL_MAX_TIME_SECONDS}" \
  "${BASE_URL}/api/stocks/${TICKER}")"

cat <<EOF

Seed completed.
  portfolioId = ${portfolio_id}
  watchlistId = ${watchlist_id}
  ownerName   = ${OWNER_NAME}
  listName    = ${LIST_NAME}
  ticker      = ${TICKER}
  threshold   = ${THRESHOLD_PRICE}
  samplePrice1 = ${sample_price_1}
  samplePrice2 = ${sample_price_2}

The scheduler should now emit notification logs for this watchlist.
Watch the app output for:
  WATCHLIST_ALERT_LISTENER_RECEIVED
  WATCHLIST_ALERT
EOF
