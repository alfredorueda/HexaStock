# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Settlement Fee Accounting (US-12)
# ═══════════════════════════════════════════════════════════════════════════════
#
# Settlement-aware sales apply a fee (FEE_RATE = 0.001 = 0.1%).
# Correct accounting: profit = grossProceeds − fee − costBasis
# Balance receives:   netProceeds = grossProceeds − fee
#
# Scenario IDs referenced by @SpecificationRef:
#   FEE-01 → shouldCalculateNetProceedsAfterFee
#   FEE-02 → shouldDeductFeeFromProceeds
#   FEE-03 → shouldMaintainAccountingIdentity
#   FEE-04 → shouldReturnZeroProfitWhenFeeAbsorbsGain
#   DRIFT-REST-02 → aggregateSellShouldMaintainAccountingIdentity
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Settlement Fee Accounting

  As a portfolio manager
  I want fees to be correctly deducted from profits
  So that the accounting identity holds: profit = netProceeds − costBasis

  Background:
    Given a portfolio exists for owner "Alice" with balance $50,000.00

  # ────────────────────────────────────────────────────────────────
  # Domain-Level Fee Calculations
  # ────────────────────────────────────────────────────────────────

  Scenario: Net proceeds should deduct fee from gross proceeds (FEE-01)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And all lots are settled
    When Alice settlement-sells 10 shares of AAPL at $110.00 with fee $1.10
    Then gross proceeds are $1,100.00
    And net proceeds are $1,098.90
    And the balance increases by $1,098.90

  Scenario: Fee should be deducted when computing profit (FEE-02)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And all lots are settled
    When Alice settlement-sells 10 shares of AAPL at $110.00 with fee $1.10
    Then the profit is $98.90 (= $1,100.00 − $1.10 − $1,000.00)
    And the profit is NOT $100.00 (which omits the fee)

  Scenario: Accounting identity must hold: profit = netProceeds − costBasis (FEE-03)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And all lots are settled
    When Alice settlement-sells 10 shares of AAPL at $110.00 with fee $1.10
    Then netProceeds = $1,098.90
    And costBasis = $1,000.00
    And profit = $98.90
    And the identity holds: profit == netProceeds − costBasis

  Scenario: Zero-profit sale when fee absorbs gain (FEE-04)
    Given Alice bought 10 shares of AAPL at $100.00 ten days ago
    And all lots are settled
    When Alice settlement-sells 10 shares of AAPL at $100.10 with fee $1.00
    Then gross proceeds are $1,001.00
    And net proceeds are $1,000.00
    And cost basis is $1,000.00
    And profit is $0.00

  # ────────────────────────────────────────────────────────────────
  # Integration-Level Fee Accounting Drift (Anemic Model Only)
  # ────────────────────────────────────────────────────────────────

  @anemic-branch-only @expected-failure
  Scenario: Aggregate sell endpoint should maintain accounting identity (DRIFT-REST-02)
    Given Alice bought 10 shares of AAPL at $100.00 via REST
    And all lots are settled (settlement dates backdated via JDBC)
    When Alice sells 10 shares at $110.00 via POST /aggregate-settlement-sales
    Then gross proceeds = $1,100.00
    And  fee = $1.10
    And  costBasis = $1,000.00
    And  the response profit SHOULD be $98.90
    But  the anemic model returns profit = $100.00
    Because Portfolio.sellWithSettlement() computes: profit = grossProceeds − costBasis
    And the fee is omitted from the profit calculation
    So the accounting identity profit == netProceeds − costBasis is VIOLATED
    # identity check: expected costBasis + profit = netProceeds
    #   correct:  1000 + 98.90   = 1098.90 ✓
    #   anemic:   1000 + 100.00  = 1100.00 ≠ 1098.90 ✗
