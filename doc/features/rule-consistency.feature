# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Rule Consistency & Drift Detection (US-14)
# ═══════════════════════════════════════════════════════════════════════════════
#
# In the anemic-domain-model branch, business rules are duplicated between
# entities and services. As the codebase evolves, rules drift apart:
#   Flaw #1: Lot.isAvailableForSale() omits reservation check
#   Flaw #2: Portfolio.sellWithSettlement() omits fee from profit
#
# These flaws are INTENTIONAL on the anemic branch to demonstrate the
# architectural risk of duplicated, unencapsulated business logic.
#
# This feature file documents the expected failures that prove rule drift.
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Rule Consistency and Drift Detection

  As a software architect
  I want to detect when domain rules drift from service rules
  So that I can demonstrate why encapsulation matters

  # ────────────────────────────────────────────────────────────────
  # Domain-Level Test Failures (both branches run same tests)
  # ────────────────────────────────────────────────────────────────

  @anemic-branch-only @expected-failure
  Scenario: Reserved lot check missing from Lot entity (DOMAIN-DRIFT-01)
    Given a settled lot of 10 shares at $100.00
    When the lot is reserved
    Then Lot.isAvailableForSale() should return false
    But on the anemic branch it returns true
    Because isAvailableForSale() checks isSettled() but NOT !reserved
    # Affects tests: reservedLotShouldNotBeAvailable, shouldSkipReservedLots,
    #   shouldSellFromNonReservedLots, shouldAllowUnreservingLot,
    #   shouldDistinguishTotalFromEligible

  @anemic-branch-only @expected-failure
  Scenario: Fee omitted from profit calculation in Portfolio (DOMAIN-DRIFT-02)
    Given a portfolio with 10 settled shares of AAPL at $100.00
    When selling 10 shares at $110.00 with fee $1.10
    Then profit should be $98.90 (= grossProceeds − fee − costBasis)
    But on the anemic branch profit is $100.00 (= grossProceeds − costBasis)
    Because Portfolio.sellWithSettlement() computes:
      profit = grossProceeds.subtract(costBasis)
    Instead of using SellResult.withFee() which correctly computes:
      profit = grossProceeds − fee − costBasis
    # Affects tests: shouldDeductFeeFromProceeds, shouldMaintainAccountingIdentity,
    #   fullScenarioAtomicCorrectness

  # ────────────────────────────────────────────────────────────────
  # Failing Domain Tests Summary (8 of 19 fail on anemic branch)
  # ────────────────────────────────────────────────────────────────

  @anemic-branch-only @expected-failure
  Scenario: Domain tests expose both flaws simultaneously
    Given the SettlementAwareSellTest suite with 19 tests
    When run against the rich-domain-model branch
    Then all 19 pass

    When run against the anemic-domain-model branch
    Then 8 tests fail:
      | Test Name                             | Root Cause  |
      | reservedLotShouldNotBeAvailable        | DRIFT-01    |
      | shouldSkipReservedLots                 | DRIFT-01    |
      | shouldSellFromNonReservedLots          | DRIFT-01    |
      | shouldAllowUnreservingLot              | DRIFT-01    |
      | shouldDistinguishTotalFromEligible     | DRIFT-01    |
      | shouldDeductFeeFromProceeds            | DRIFT-02    |
      | shouldMaintainAccountingIdentity       | DRIFT-02    |
      | fullScenarioAtomicCorrectness          | DRIFT-01+02 |

  # ────────────────────────────────────────────────────────────────
  # Integration-Level Rule Inconsistency
  # (Additional endpoints on anemic branch delegate to flawed domain)
  # ────────────────────────────────────────────────────────────────

  @anemic-branch-only @expected-failure
  Scenario: Inline service logic vs. domain method produce different results
    Given Alice bought 10 shares of AAPL at $100.00 via REST
    And all lots are settled
    And the oldest lot is reserved

    # Path A: POST /sell (uses correct inline service logic)
    When Alice sells via POST /{id}/sell with settlement
    Then the service correctly checks settlement AND reservation
    And the sale is rejected because eligible shares = 0

    # Path B: GET /eligible-shares (delegates to flawed domain)
    When Alice queries GET /{id}/holdings/AAPL/eligible-shares
    Then the endpoint returns 10 (incorrectly includes reserved lot)
    Because it delegates to Holding.getEligibleShares()
    Which calls Lot.isAvailableForSale()
    Which only checks isSettled(), not !reserved

  @anemic-branch-only @expected-failure
  Scenario: Two sell paths produce different profit for same sale
    Given Alice bought 10 shares of AAPL at $100.00 via REST
    And all lots are settled

    # Path A: POST /sell (uses correct inline service logic with SellResult.withFee())
    # Would compute: profit = 1100 − 1.10 − 1000 = $98.90

    # Path B: POST /aggregate-settlement-sales (delegates to Portfolio.sellWithSettlement())
    When Alice sells via POST /{id}/aggregate-settlement-sales at $110.00
    Then the aggregate path returns profit = $100.00
    Because Portfolio.sellWithSettlement() omits fee from profit
    And the accounting identity is violated:
      costBasis + profit ≠ netProceeds
      $1000 + $100 = $1100 ≠ $1098.90
