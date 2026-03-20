# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Settlement-Aware Selling (US-10)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the Settlement-Aware Sell
# feature in Gherkin syntax. It specifies the T+2 settlement gate, the
# interaction with reserved lots, transaction fees, and the accounting identity
# that must hold across all sell paths.
#
# Scenario IDs are referenced by Java tests via @SpecificationRef:
#   SETTLE-01 → shouldNotSellUnsettledLots
#   SETTLE-02 → shouldSellExactlySettledQuantity
#   SETTLE-03 → shouldReportEligibleSharesCorrectly
#   SETTLE-REST-01 → sellUnsettledLots_returns409
#   SETTLE-REST-02 → regularSellStillWorks
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Settlement-Aware Stock Selling

  As a portfolio manager
  I want the system to enforce the T+2 settlement rule during sales
  So that only fully settled lots can participate in regulated sell operations

  Background:
    Given a portfolio exists for owner "Alice" with balance $50,000.00

  # ────────────────────────────────────────────────────────────────
  # Settlement Gate — Domain Level
  # ────────────────────────────────────────────────────────────────

  Scenario: Reject sell when only unsettled lots exist (SETTLE-01)
    Given Alice bought 10 shares of AAPL at $100.00 five days ago
    And Alice bought 10 shares of AAPL at $110.00 just now
    When Alice tries to settlement-sell 15 shares of AAPL at $120.00
    Then the sell is rejected with "Insufficient Eligible Shares"
    Because only the first lot (10 shares) has passed the T+2 settlement period

  Scenario: Allow selling exactly the settled quantity (SETTLE-02)
    Given Alice bought 10 shares of AAPL at $100.00 five days ago
    And Alice bought 10 shares of AAPL at $110.00 just now
    When Alice settlement-sells 10 shares of AAPL at $120.00 with no fee
    Then the sale proceeds are $1,200.00
    And the cost basis is $1,000.00
    And the profit is $200.00
    And 10 unsettled shares of AAPL remain in the portfolio

  Scenario: Report eligible shares excluding unsettled lots (SETTLE-03)
    Given Alice bought 10 shares of AAPL at $100.00 five days ago
    And Alice bought 5 shares of AAPL at $110.00 just now
    Then the total shares of AAPL are 15
    And the eligible (settled) shares of AAPL are 10

  # ────────────────────────────────────────────────────────────────
  # Settlement Gate — Integration Level (REST)
  # ────────────────────────────────────────────────────────────────

  Scenario: Settlement-sale endpoint rejects unsettled lots with 409 (SETTLE-REST-01)
    Given Alice bought 10 shares of AAPL via REST
    When Alice calls POST /settlement-sales to sell 5 shares of AAPL
    Then the response status is 409
    And the response body contains title "Insufficient Eligible Shares"

  Scenario: Regular sale endpoint works regardless of settlement (SETTLE-REST-02)
    Given Alice bought 10 shares of AAPL via REST
    When Alice calls POST /sales to sell 5 shares of AAPL
    Then the response status is 200
    And the response contains proceeds > 0
