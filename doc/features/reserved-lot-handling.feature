# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Reserved Lot Handling (US-11)
# ═══════════════════════════════════════════════════════════════════════════════
#
# Lots can be reserved (e.g., used as collateral). Reserved lots must be
# excluded from settlement-aware selling and from eligible share counts.
#
# Scenario IDs referenced by @SpecificationRef:
#   RESERVE-01 → shouldSkipReservedLots
#   RESERVE-02 → shouldSellFromNonReservedLots
#   RESERVE-03 → shouldAllowUnreservingLot
#   LOT-SETTLE-02 → reservedLotShouldNotBeAvailable
#   ELIGIBLE-01 → shouldDistinguishTotalFromEligible
#   DRIFT-REST-01 → eligibleSharesShouldExcludeReservedLots
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Reserved Lot Handling

  As a portfolio manager
  I want to reserve specific lots (e.g., for collateral)
  So that reserved lots are excluded from sales and eligibility queries

  Background:
    Given a portfolio exists for owner "Alice" with balance $50,000.00

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

  # ────────────────────────────────────────────────────────────────
  # Integration-Level Rule Inconsistency (Anemic Model Only)
  # ────────────────────────────────────────────────────────────────

  @anemic-branch-only @expected-failure
  Scenario: Eligible shares query should exclude reserved lots (DRIFT-REST-01)
    Given Alice bought 10 shares of AAPL and 5 shares of AAPL via REST
    And all lots are settled (settlement dates backdated via JDBC)
    And the oldest lot is reserved via POST /holdings/AAPL/lots/{id}/reserve
    When Alice queries GET /holdings/AAPL/eligible-shares
    Then the expected eligible count is 5
    But the anemic model returns 15
    Because Lot.isAvailableForSale() only checks settlement, not reservation
    And the eligible-shares endpoint delegates to that flawed domain method
    While the sell endpoint uses correct inline service logic
