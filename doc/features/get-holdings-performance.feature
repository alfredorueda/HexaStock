# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Get Holdings Performance (US-09)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the Get Holdings Performance
# use case in a human-readable format using Gherkin syntax.
#
# It serves as the CANONICAL BEHAVIOURAL SPECIFICATION for US-09. The Gherkin
# scenarios here define the expected system behaviour independently of any
# implementation detail — they describe WHAT the system does, not HOW.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-09.AC-1 through US-09.AC-3)
#     that Java tests reference via the @SpecificationRef annotation.
#   - This creates an explicit, navigable chain:
#
#       Requirement  →  Scenario (.feature)  →  Test (JUnit)  →  Code
#
#   - When a test is annotated with @SpecificationRef(value = "US-09.AC-1"),
#     a reader can look up this file to understand the business behaviour
#     the test is verifying.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Referenced by annotated tests via @SpecificationRef.
#
# Scenario IDs:
#   US-09.AC-1 → Scenario: Getting holdings performance for a portfolio with holdings
#   US-09.AC-2 → Scenario: Getting holdings performance for an empty portfolio
#   US-09.AC-3 → Scenario: Getting holdings performance for a non-existent portfolio
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Get Holdings Performance (US-09)

  As an investor monitoring my investments
  I want to see a performance summary of each stock in my portfolio
  So that I can assess how my investments are doing

  Scenario: Getting holdings performance for a portfolio with holdings
    Given a portfolio exists for owner "Alice"
    And Alice has deposited $50000
    And Alice has bought 10 shares of AAPL at $150.00
    When I GET /api/portfolios/{id}/holdings
    Then I receive 200 OK with a JSON array of holding performance objects
    And each object contains:
      | Field                | Description                              |
      | ticker               | Stock symbol                             |
      | quantity             | Total shares ever purchased              |
      | remaining            | Shares currently held (after sells)      |
      | averagePurchasePrice | Weighted average of all purchase prices  |
      | currentPrice         | Live market price                        |
      | unrealizedGain       | Gain/loss on shares still held           |
      | realizedGain         | Gain/loss from completed sales           |

  Scenario: Getting holdings performance for an empty portfolio
    Given a newly created portfolio with no holdings
    When I GET /api/portfolios/{id}/holdings
    Then I receive 200 OK with an empty array []

  Scenario: Getting holdings performance for a non-existent portfolio
    Given a non-existent portfolio ID
    When I GET /api/portfolios/{id}/holdings
    Then I receive 404 Not Found with ProblemDetail:
      | Field  | Value               |
      | title  | Portfolio Not Found  |
      | status | 404                  |
