#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_WORKSPACE="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
WORKSPACE="${1:-$DEFAULT_WORKSPACE}"
FAILURES=0

pass() {
  printf 'PASS  %s\n' "$1"
}

fail() {
  printf 'FAIL  %s\n' "$1" >&2
  FAILURES=$((FAILURES + 1))
}

require_file() {
  if [[ -f "$WORKSPACE/$1" ]]; then
    pass "$1 exists"
  else
    fail "$1 is missing"
  fi
}

printf 'Verifying workspace: %s\n' "$WORKSPACE"

require_file "pom.xml"
require_file "docs/spec/sell-stocks-spec.md"
require_file "docs/spec/domain-class-diagram.puml"
require_file "plan.md"

REQUIRED_TYPES=(
  "src/main/java/com/neueda/portfolio/domain/model/Portfolio.java"
  "src/main/java/com/neueda/portfolio/domain/model/Holding.java"
  "src/main/java/com/neueda/portfolio/domain/model/Lot.java"
  "src/main/java/com/neueda/portfolio/domain/vo/Money.java"
  "src/main/java/com/neueda/portfolio/domain/vo/Price.java"
  "src/main/java/com/neueda/portfolio/domain/vo/ShareQuantity.java"
  "src/main/java/com/neueda/portfolio/domain/vo/Ticker.java"
  "src/main/java/com/neueda/portfolio/domain/vo/PortfolioId.java"
  "src/main/java/com/neueda/portfolio/domain/vo/HoldingId.java"
  "src/main/java/com/neueda/portfolio/domain/vo/LotId.java"
  "src/main/java/com/neueda/portfolio/domain/vo/SellResult.java"
  "src/main/java/com/neueda/portfolio/domain/exception/InvalidQuantityException.java"
  "src/main/java/com/neueda/portfolio/domain/exception/InvalidAmountException.java"
  "src/main/java/com/neueda/portfolio/domain/exception/ConflictQuantityException.java"
  "src/main/java/com/neueda/portfolio/domain/exception/InvalidTickerException.java"
  "src/main/java/com/neueda/portfolio/domain/exception/HoldingNotFoundException.java"
)

for required_type in "${REQUIRED_TYPES[@]}"; do
  require_file "$required_type"
done

if [[ -f "$WORKSPACE/pom.xml" ]]; then
  dependency_count="$(grep -c '<dependency>' "$WORKSPACE/pom.xml" || true)"
  if [[ "$dependency_count" == "1" ]] && grep -q 'junit-jupiter' "$WORKSPACE/pom.xml"; then
    pass "JUnit 5 is the single declared dependency"
  else
    fail "pom.xml must declare exactly one dependency: JUnit 5"
  fi

  if grep -Eqi 'spring|hibernate|jakarta\.persistence|javax\.persistence' "$WORKSPACE/pom.xml"; then
    fail "pom.xml contains a framework or persistence dependency"
  else
    pass "no framework or persistence dependency found"
  fi
fi

if [[ -d "$WORKSPACE/src/main/java" ]]; then
  numeric_matches="$(grep -R -nE '(^|[^[:alnum:]_])(double|float)([^[:alnum:]_]|$)' \
    "$WORKSPACE/src/main/java" 2>/dev/null \
    | grep -vE ':[[:digit:]]+:[[:space:]]*(//|/\*|\*)' || true)"
  if [[ -n "$numeric_matches" ]]; then
    fail "production code contains double or float: $numeric_matches"
  else
    pass "production code contains no double or float declarations"
  fi
fi

if [[ -d "$WORKSPACE/src/test" ]]; then
  missing_criteria=()
  for criterion in $(seq -w 1 24); do
    if ! grep -R -Eqi "AC[-_ ]?${criterion}" "$WORKSPACE/src/test"; then
      missing_criteria+=("AC-$criterion")
    fi
  done
  if [[ ${#missing_criteria[@]} -eq 0 ]]; then
    pass "AC-01 through AC-24 are traceable in test source"
  else
    fail "missing test markers: ${missing_criteria[*]}"
  fi
else
  fail "src/test is missing"
fi

if [[ -f "$WORKSPACE/pom.xml" ]]; then
  if (cd "$WORKSPACE" && mvn test); then
    pass "mvn test completed successfully"
  else
    fail "mvn test failed"
  fi

  REPORTS=("$WORKSPACE"/target/surefire-reports/TEST-*.xml)
  if [[ -e "${REPORTS[0]}" ]]; then
    total_tests=0
    total_failures=0
    total_errors=0
    for report in "${REPORTS[@]}"; do
      tests="$(grep -o 'tests="[0-9]*"' "$report" | head -1 | tr -cd '0-9')"
      failures="$(grep -o 'failures="[0-9]*"' "$report" | head -1 | tr -cd '0-9')"
      errors="$(grep -o 'errors="[0-9]*"' "$report" | head -1 | tr -cd '0-9')"
      total_tests=$((total_tests + ${tests:-0}))
      total_failures=$((total_failures + ${failures:-0}))
      total_errors=$((total_errors + ${errors:-0}))
    done
    if [[ "$total_tests" -eq 36 && "$total_failures" -eq 0 && "$total_errors" -eq 0 ]]; then
      pass "Surefire reports exactly 36 passing tests"
    else
      fail "Surefire totals: tests=$total_tests failures=$total_failures errors=$total_errors; expected 36/0/0"
    fi
  else
    fail "Surefire XML reports were not produced"
  fi
fi

if [[ "$FAILURES" -gt 0 ]]; then
  printf '%s verification check(s) failed.\n' "$FAILURES" >&2
  exit 1
fi

printf 'All automated checks passed. Complete the independent semantic review as well.\n'
