# PROPOSED EXTENSION — not implemented by main and not executed by Cucumber.
# Scenario identifiers are retained for future traceability, not current coverage.
# All relative dates use valuation date 2026-03-18 and a Monday-Friday calendar
# with no holidays. Settlement occurs two business dates after purchase in this
# teaching policy; this is not a statement of current market regulation.
# Explicit scenario fees override the optional 0.1% teaching fee policy.
# Proposed endpoint fragments are relative to /api/portfolios/{portfolioId}.
# Read doc/tutorial/richVsAnemicDomainModel/03-SETTLEMENT-EXERCISE.md first.

Feature: Reserved Lot Handling

  As a portfolio manager
  I want to reserve specific lots (e.g., for collateral)
  So that reserved lots are excluded from sales and eligibility queries

  Background:
    Given the valuation date is 2026-03-18
    And business days are Monday to Friday with no holidays
    And a portfolio exists for owner "Alice" with balance $50,000.00

  # ────────────────────────────────────────────────────────────────
  # Domain-Level Reservation Rules
  # ────────────────────────────────────────────────────────────────

  Scenario: Reserved lot should not be available for sale even if settled (LOT-SETTLE-02)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And the AAPL lot is settled
    When the lot is reserved
    Then Lot.isAvailableForSale() returns false
    And Lot.availableShares() returns 0

  Scenario: Skip reserved lots during FIFO selling (RESERVE-01)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And Alice bought 5 shares of AAPL at $120.00 five days ago
    And both lots are settled
    When the oldest lot (10 shares) is reserved
    Then only 5 shares are eligible for sale
    And trying to settlement-sell 8 shares is rejected with "Insufficient Eligible Shares"

  Scenario: Sell from non-reserved lots in FIFO order (RESERVE-02)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And Alice bought 5 shares of AAPL at $120.00 five days ago
    And both lots are settled
    When the oldest lot (10 shares) is reserved
    And Alice settlement-sells 5 shares of AAPL at $130.00 with no fee
    Then the cost basis is $600.00 (from lot2 at $120.00)
    And the profit is $50.00
    And the reserved lot still has 10 shares untouched

  Scenario: Unreserving a lot makes it available again (RESERVE-03)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And the lot is settled
    When the lot is reserved
    Then eligible shares are 0
    When the lot is unreserved
    Then eligible shares are 10

  Scenario: Distinguish total shares from eligible shares (ELIGIBLE-01)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And Alice bought 5 shares of AAPL at $110.00 just now
    Then total AAPL shares are 15
    And eligible AAPL shares are 10 (only settled lot)
    When the settled lot is reserved
    Then eligible AAPL shares are 0

  Scenario: Eligibility query agrees with reservation policy (DRIFT-REST-01)
    Given Alice has two settled AAPL lots containing 10 and 5 shares
    And the oldest lot is reserved
    When Alice queries the proposed eligible-shares endpoint
    Then the eligible count is 5
    And the same state permits selling at most 5 shares
