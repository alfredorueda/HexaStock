# PROPOSED EXTENSION — not implemented by main and not executed by Cucumber.
# Scenario identifiers are retained for future traceability, not current coverage.
# All relative dates use valuation date 2026-03-18 and a Monday-Friday calendar
# with no holidays. Settlement occurs two business dates after purchase in this
# teaching policy; this is not a statement of current market regulation.
# Explicit scenario fees override the optional 0.1% teaching fee policy.
# Proposed endpoint fragments are relative to /api/portfolios/{portfolioId}.
# Read doc/tutorial/richVsAnemicDomainModel/03-SETTLEMENT-EXERCISE.md first.

Feature: Settlement-Aware Stock Selling

  As a portfolio manager
  I want the system to enforce the T+2 settlement rule during sales
  So that only fully settled lots can participate in sales under this teaching policy

  Background:
    Given the valuation date is 2026-03-18
    And business days are Monday to Friday with no holidays
    And a portfolio exists for owner "Alice" with balance $50,000.00

  # ────────────────────────────────────────────────────────────────
  # Settlement Gate — Domain Level
  # ────────────────────────────────────────────────────────────────

  Scenario: Reject sell when eligible quantity is insufficient (SETTLE-01)
    Given Alice bought 10 shares of AAPL at $100.00 five days ago
    And Alice bought 10 shares of AAPL at $110.00 just now
    When Alice tries to settlement-sell 15 shares of AAPL at $120.00
    Then the sell is rejected with "Insufficient Eligible Shares"
    And only the first lot contributes 10 eligible shares

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
