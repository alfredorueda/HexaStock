# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Get Portfolio (US-02)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the Get Portfolio use case
# in a human-readable format using Gherkin syntax.
#
# It serves as the CANONICAL BEHAVIOURAL SPECIFICATION for US-02. The Gherkin
# scenarios here define the expected system behaviour independently of any
# implementation detail — they describe WHAT the system does, not HOW.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-02.AC-1, US-02.AC-2)
#     that Java tests reference via the @SpecificationRef annotation.
#   - This creates an explicit, navigable chain:
#
#       Requirement  →  Scenario (.feature)  →  Test (JUnit)  →  Code
#
#   - When a test is annotated with @SpecificationRef(value = "US-02.AC-1"),
#     a reader can look up this file to understand the business behaviour
#     the test is verifying.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Referenced by annotated tests via @SpecificationRef.
#
# Scenario IDs:
#   US-02.AC-1 → Scenario: Retrieving an existing portfolio
#   US-02.AC-2 → Scenario: Retrieving a non-existent portfolio
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Get Portfolio (US-02)

  As an investor
  I want to retrieve my portfolio details
  So that I can see my current balance and account information

  Scenario: Retrieving an existing portfolio
    Given a portfolio exists for owner "Alice"
    When I GET /api/portfolios/{id}
    Then I receive 200 OK
    And the response body contains:
      | Field     | Value              |
      | id        | (the portfolio ID) |
      | ownerName | Alice              |
      | balance   | (current balance)  |
      | createdAt | (timestamp)        |

  Scenario: Retrieving a non-existent portfolio
    Given no portfolio exists with the given ID
    When I GET /api/portfolios/{id}
    Then I receive 404 Not Found with ProblemDetail:
      | Field  | Value               |
      | title  | Portfolio Not Found  |
      | status | 404                  |
