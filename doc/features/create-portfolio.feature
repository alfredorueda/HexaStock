# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Create Portfolio (US-01)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the Create Portfolio use case
# in a human-readable format using Gherkin syntax.
#
# It serves as the CANONICAL BEHAVIOURAL SPECIFICATION for US-01. The Gherkin
# scenarios here define the expected system behaviour independently of any
# implementation detail — they describe WHAT the system does, not HOW.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-01.AC-1)
#     that Java tests reference via the @SpecificationRef annotation.
#   - This creates an explicit, navigable chain:
#
#       Requirement  →  Scenario (.feature)  →  Test (JUnit)  →  Code
#
#   - When a test is annotated with @SpecificationRef(value = "US-01.AC-1"),
#     a reader can look up this file to understand the business behaviour
#     the test is verifying.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Referenced by annotated tests via @SpecificationRef.
#
# Scenario IDs:
#   US-01.AC-1 → Scenario: Creating a new portfolio with a valid owner name
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Create Portfolio (US-01)

  As an investor
  I want to create a new investment portfolio
  So that I can start managing my investments

  Scenario: Creating a new portfolio with a valid owner name
    Given a valid owner name "Alice"
    When I POST /api/portfolios with {"ownerName": "Alice"}
    Then I receive 201 Created
    And the response contains a Location header pointing to /api/portfolios/{id}
    And the response body contains:
      | Field       | Value |
      | id          | (generated UUID) |
      | ownerName   | Alice |
      | cashBalance | 0.00  |
      | currency    | USD   |
