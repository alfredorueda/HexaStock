# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Get Transaction History (US-08)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the Get Transaction History
# use case in a human-readable format using Gherkin syntax.
#
# It serves as the CANONICAL BEHAVIOURAL SPECIFICATION for US-08. The Gherkin
# scenarios here define the expected system behaviour independently of any
# implementation detail — they describe WHAT the system does, not HOW.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-08.AC-1, US-08.AC-2)
#     that Java tests reference via the @SpecificationRef annotation.
#   - This creates an explicit, navigable chain:
#
#       Requirement  →  Scenario (.feature)  →  Test (JUnit)  →  Code
#
#   - When a test is annotated with @SpecificationRef(value = "US-08.AC-1"),
#     a reader can look up this file to understand the business behaviour
#     the test is verifying.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Referenced by annotated tests via @SpecificationRef.
#
# Scenario IDs:
#   US-08.AC-1 → Scenario: Retrieving transaction history for a portfolio with transactions
#   US-08.AC-2 → Scenario: Retrieving transaction history with type filter parameter
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Get Transaction History (US-08)

  As an investor
  I want to view my portfolio's transaction history
  So that I can review past financial activities

  Scenario: Retrieving transaction history for a portfolio with transactions
    Given a portfolio exists for owner "Alice"
    And Alice has deposited $10000
    And Alice has bought 5 shares of AAPL
    When I GET /api/portfolios/{id}/transactions
    Then I receive 200 OK with a JSON array of transaction objects
    And the array contains at least a DEPOSIT and a PURCHASE transaction
    And each transaction object wraps the full Transaction domain object

  Scenario: Retrieving transaction history with type filter parameter
    Given a portfolio exists for owner "Alice"
    And Alice has deposited $10000 and bought 5 shares of AAPL
    When I GET /api/portfolios/{id}/transactions?type=PURCHASE
    Then I receive 200 OK with a JSON array

  # Note: the type query parameter is accepted by the controller but
  # is currently NOT used for filtering — all transactions are returned
  # regardless of the type value. See the specification for details.
