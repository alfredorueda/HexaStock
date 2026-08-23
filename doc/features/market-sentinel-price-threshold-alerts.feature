# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Market Sentinel: Price Threshold Alerts (US-MS-01)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the read-side behaviour of the Market Sentinel detection
# algorithm for Level 1 price-threshold alerts.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-MS-01.AC-1 through US-MS-01.AC-3)
#     that Java tests reference via the @SpecificationRef annotation.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Scenario IDs:
#   US-MS-01.AC-1 → Scenario: Fetching prices once per distinct ticker in a cycle
#   US-MS-01.AC-2 → Scenario: Trigger rule is threshold >= currentPrice
#   US-MS-01.AC-3 → Scenario: Multiple alerts for same ticker can all trigger
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Market Sentinel — Price Threshold Alerts (US-MS-01)

  Background:
    Given watchlists exist and are active

  Scenario: Fetching prices once per distinct ticker in a cycle
    Given two active watchlists contain alerts for ticker AAPL
    When Market Sentinel runs one detection cycle
    Then the stock price for AAPL is fetched exactly once

  Scenario: Trigger rule is threshold >= currentPrice
    Given an active watchlist contains an alert for AAPL at 150.00
    And the current market price for AAPL is 140.00
    When Market Sentinel runs one detection cycle
    Then a buy signal is emitted for AAPL at threshold 150.00 with current 140.00

  Scenario: Multiple alerts for same ticker can all trigger
    Given an active watchlist contains alerts for AAPL at 150.00 and 140.00
    And the current market price for AAPL is 128.00
    When Market Sentinel runs one detection cycle
    Then 2 buy signals are emitted for AAPL

