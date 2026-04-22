# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Watchlist Alerts (US-WL-02)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of adding/removing price-threshold
# alerts inside a watchlist.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-WL-02.AC-1 through US-WL-02.AC-6)
#     that Java tests reference via the @SpecificationRef annotation.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Scenario IDs:
#   US-WL-02.AC-1 → Scenario: Adding a price-threshold alert entry
#   US-WL-02.AC-2 → Scenario: Adding an alert with non-positive threshold
#   US-WL-02.AC-3 → Scenario: Adding an exact duplicate alert (ticker + threshold)
#   US-WL-02.AC-4 → Scenario: Adding multiple alerts for the same ticker (ladder-ready)
#   US-WL-02.AC-5 → Scenario: Removing a specific alert (ticker + threshold)
#   US-WL-02.AC-6 → Scenario: Removing all alerts for a ticker
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Watchlist Alerts (US-WL-02)

  Background:
    Given a watchlist exists with id "wl-1"

  Scenario: Adding a price-threshold alert entry
    When I POST /api/watchlists/wl-1/alerts with {"ticker":"AAPL","thresholdPrice":"150.00"}
    Then I receive 200 OK
    And the response body contains an alert for AAPL at 150.00

  Scenario: Adding an alert with non-positive threshold
    When I POST /api/watchlists/wl-1/alerts with {"ticker":"AAPL","thresholdPrice":"0.00"}
    Then I receive 400 Bad Request

  Scenario: Adding an exact duplicate alert (ticker + threshold)
    Given the watchlist already contains alert (ticker AAPL, threshold 150.00)
    When I POST /api/watchlists/wl-1/alerts with {"ticker":"AAPL","thresholdPrice":"150.00"}
    Then I receive 409 Conflict with ProblemDetail:
      | Field  | Value          |
      | title  | Duplicate Alert |
      | status | 409            |

  Scenario: Adding multiple alerts for the same ticker (ladder-ready)
    When I add alerts for AAPL at 150.00, 140.00, and 130.00
    Then the watchlist contains 3 alerts for ticker "AAPL"

  Scenario: Removing a specific alert (ticker + threshold)
    Given the watchlist contains alerts for AAPL at 150.00 and 140.00
    When I DELETE /api/watchlists/wl-1/alerts?ticker=AAPL&thresholdPrice=150.00
    Then I receive 200 OK
    And the watchlist contains an alert for AAPL at 140.00

  Scenario: Removing all alerts for a ticker
    Given the watchlist contains alerts for AAPL at 150.00 and 140.00
    And the watchlist contains alert for GOOGL at 120.00
    When I DELETE /api/watchlists/wl-1/alerts/AAPL
    Then I receive 200 OK
    And the watchlist does not contain any alerts for ticker "AAPL"

