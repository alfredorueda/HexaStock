# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — FIFO Settlement Selling & Lot Lifecycle (US-13)
# ═══════════════════════════════════════════════════════════════════════════════
#
# Settlement-aware selling uses FIFO order, consuming only settled,
# non-reserved lots. Partial lot consumption leaves remaining shares.
# Lots with zero remaining quantity are removed from the holding.
#
# Scenario IDs referenced by @SpecificationRef:
#   FIFO-01 → shouldSellSettledLotsInFifoOrder
#   FIFO-02 → shouldLeaveUnsettledLotIntact
#   PARTIAL-01 → shouldSellPartialLotCorrectly
#   ELIGIBLE-02 → shouldReturnZeroWhenAllUnsettled
#   ELIGIBLE-03 → shouldCountOnlySettledAndUnreservedShares
#   LOT-SETTLE-01 → lotShouldNotBeSettledBeforeSettlementDate
#   ATOMIC-01 → fullScenarioAtomicCorrectness
# ═══════════════════════════════════════════════════════════════════════════════

Feature: FIFO Settlement-Aware Selling

  As a portfolio manager
  I want lots consumed in first-in-first-out order with settlement gating
  So that sales only touch settled, unreserved lots and produce correct accounting

  Background:
    Given a portfolio exists for owner "Alice" with balance $50,000.00

  # ────────────────────────────────────────────────────────────────
  # FIFO Ordering
  # ────────────────────────────────────────────────────────────────

  Scenario: Sell settled lots in FIFO order (FIFO-01)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And Alice bought 5 shares of AAPL at $120.00 five days ago
    And both lots are settled
    When Alice settlement-sells 12 shares of AAPL at $130.00 with no fee
    Then lot1 is fully consumed (10 shares)
    And lot2 has 3 shares remaining
    And cost basis is $1,240.00 (10×100 + 2×120)
    And profit is $320.00

  Scenario: Unsettled lots are left intact during FIFO scan (FIFO-02)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And Alice bought 5 shares of AAPL at $120.00 just now
    When Alice settlement-sells 8 shares of AAPL at $130.00 with no fee
    Then lot1 has 2 shares remaining
    And lot2 (unsettled) still has 5 shares
    And cost basis is $800.00 (8×100)

  # ────────────────────────────────────────────────────────────────
  # Partial Lot Consumption
  # ────────────────────────────────────────────────────────────────

  Scenario: Partially consume a lot (PARTIAL-01)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And the lot is settled
    When Alice settlement-sells 3 shares of AAPL at $150.00 with no fee
    Then the lot has 7 remaining shares
    And cost basis is $300.00 (3×100)
    And proceeds are $450.00
    And profit is $150.00

  # ────────────────────────────────────────────────────────────────
  # Eligibility Checks
  # ────────────────────────────────────────────────────────────────

  Scenario: Zero eligible when all lots unsettled (ELIGIBLE-02)
    Given Alice bought 10 shares of AAPL at $100.00 just now
    Then eligible AAPL shares are 0
    And trying to settlement-sell any shares is rejected with "Insufficient Eligible Shares"

  Scenario: Only settled and unreserved shares count as eligible (ELIGIBLE-03)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And Alice bought 5 shares of AAPL at $120.00 five days ago
    And Alice bought 3 shares of AAPL at $130.00 just now
    And the first two lots are settled
    When the oldest lot is reserved
    Then total shares are 18
    And eligible shares are 5 (only lot2: settled and unreserved)

  # ────────────────────────────────────────────────────────────────
  # Lot Settlement Lifecycle
  # ────────────────────────────────────────────────────────────────

  Scenario: Lot is not settled before T+2 settlement date (LOT-SETTLE-01)
    Given Alice bought 10 shares of AAPL at $100.00 today
    Then the lot's settlement date is today + 2 business days
    And Lot.isSettled(today) returns false
    And Lot.isSettled(settlementDate) returns true

  # ────────────────────────────────────────────────────────────────
  # Atomic End-to-End Consistency
  # ────────────────────────────────────────────────────────────────

  Scenario: Full atomic scenario: buy, settle, reserve, sell, verify (ATOMIC-01)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And Alice bought 5 shares of AAPL at $120.00 five days ago
    And Alice bought 3 shares of AAPL at $130.00 just now
    And the first two lots are settled

    # Reserve the oldest lot
    When the oldest lot (10 shares) is reserved
    Then eligible shares are 5

    # Sell within eligible
    When Alice settlement-sells 5 shares of AAPL at $140.00 with fee $0.70
    Then cost basis is $600.00 (5×120 from lot2)
    And net proceeds are $699.30 (700 − 0.70)
    And profit is $99.30 (699.30 − 600)
    And the accounting identity holds: profit == netProceeds − costBasis
    And the reserved lot (10 shares at $100.00) is untouched
    And the unsettled lot (3 shares at $130.00) is untouched
