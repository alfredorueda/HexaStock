# PROPOSED EXTENSION — not implemented by main and not executed by Cucumber.
# Scenario identifiers are retained for future traceability, not current coverage.
# All relative dates use valuation date 2026-03-18 and a Monday-Friday calendar
# with no holidays. Settlement occurs two business dates after purchase in this
# teaching policy; this is not a statement of current market regulation.
# Explicit scenario fees override the optional 0.1% teaching fee policy.
# Proposed endpoint fragments are relative to /api/portfolios/{portfolioId}.
# Read doc/tutorial/richVsAnemicDomainModel/03-SETTLEMENT-EXERCISE.md first.

Feature: Settlement Fee Accounting

  As a portfolio manager
  I want fees to be correctly deducted from profits
  So that the accounting identity holds: profit = netProceeds − costBasis

  Background:
    Given the valuation date is 2026-03-18
    And business days are Monday to Friday with no holidays
    And a portfolio exists for owner "Alice" with balance $50,000.00

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

  Scenario: Every supported sale path maintains the accounting identity (DRIFT-REST-02)
    Given two identical portfolios each hold 10 settled AAPL shares bought at $100.00
    When each portfolio sells 10 shares at $110.00 with fee $1.10 through a different supported path
    Then each result has gross proceeds $1,100.00
    And each result has net proceeds $1,098.90
    And each result has cost basis $1,000.00
    And each result has profit $98.90
