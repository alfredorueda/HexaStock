# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — List All Portfolios (US-03)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the List All Portfolios use
# case in a human-readable format using Gherkin syntax.
#
# It serves as the CANONICAL BEHAVIOURAL SPECIFICATION for US-03. The Gherkin
# scenarios here define the expected system behaviour independently of any
# implementation detail — they describe WHAT the system does, not HOW.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-03.AC-1, US-03.AC-2)
#     that Java tests reference via the @SpecificationRef annotation.
#   - This creates an explicit, navigable chain:
#
#       Requirement  →  Scenario (.feature)  →  Test (JUnit)  →  Code
#
#   - When a test is annotated with @SpecificationRef(value = "US-03.AC-1"),
#     a reader can look up this file to understand the business behaviour
#     the test is verifying.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Referenced by annotated tests via @SpecificationRef.
#
# Scenario IDs:
#   US-03.AC-1 → Scenario: Listing all portfolios when several exist
#   US-03.AC-2 → Scenario: Listing portfolios when none exist
# ═══════════════════════════════════════════════════════════════════════════════

Feature: List All Portfolios (US-03)

  As an administrator or investor
  I want to list all portfolios in the system
  So that I can get an overview of all accounts

  Scenario: Listing all portfolios when several exist
    Given portfolios exist for owners "Alice", "Bob", and "Charlie"
    And Alice has deposited $1000, Bob has deposited $2500, Charlie has deposited $0
    When I GET /api/portfolios
    Then I receive 200 OK with a JSON array containing all three portfolios
    And each entry contains id, ownerName, balance, createdAt
    And the balances match:
      | Owner   | Balance |
      | Alice   | 1000.00 |
      | Bob     | 2500.00 |
      | Charlie |    0.00 |

  Scenario: Listing portfolios when none exist
    Given no portfolios exist in the system
    When I GET /api/portfolios
    Then I receive 200 OK with an empty array []
