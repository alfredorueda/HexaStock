# PROPOSED EXTENSION — not implemented by main and not executed by Cucumber.
# Scenario identifiers are retained for future traceability, not current coverage.
# All relative dates use valuation date 2026-03-18 and a Monday-Friday calendar
# with no holidays. Settlement occurs two business dates after purchase in this
# teaching policy; this is not a statement of current market regulation.
# Explicit scenario fees override the optional 0.1% teaching fee policy.
# Proposed endpoint fragments are relative to /api/portfolios/{portfolioId}.
# Read doc/tutorial/richVsAnemicDomainModel/03-SETTLEMENT-EXERCISE.md first.

Feature: Rule Consistency Across Callers

  As a developer
  I want callers to apply the same eligibility and fee policies
  So that adding an interface does not change the business result

  Scenario: Reservation is respected by the eligibility rule (DOMAIN-DRIFT-01)
    Given a settled lot contains 10 shares bought at $100.00
    When the lot is reserved
    Then the lot is not available for sale
    And its eligible quantity is 0

  Scenario: Profit includes the fee (DOMAIN-DRIFT-02)
    Given a portfolio holds 10 settled shares bought at $100.00
    When it sells 10 shares at $110.00 with fee $1.10
    Then profit is $98.90
    And cost basis plus profit plus fee equals gross proceeds

  Scenario: Tests detect deliberately seeded policy defects (DRIFT-TEST-01)
    Given tests assert reserved-lot exclusion and fee-adjusted profit
    When reservation exclusion is deliberately removed in an isolated exercise copy
    Then the reserved-lot test fails
    When the original rule is restored and fee subtraction is deliberately removed
    Then the fee-adjusted profit test fails

  Scenario: Query and command agree for the same state (DRIFT-PATH-01)
    Given a portfolio has only a settled reserved lot of 10 AAPL shares
    When eligibility is queried at the fixed valuation time
    Then eligible shares are 0
    When a sale is attempted against the unchanged state at the same time
    Then the sale is rejected without changing lots or cash

  Scenario: Equivalent sale paths return equivalent results (DRIFT-PATH-02)
    Given two identical portfolios hold 10 settled AAPL shares bought at $100.00
    When both sell 10 shares at $110.00 with fee $1.10 through different supported paths
    Then both results have profit $98.90
    And both balances increase by $1,098.90
