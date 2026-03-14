# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Deposit Funds (US-04)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the Deposit Funds use case
# in a human-readable format using Gherkin syntax.
#
# It serves as the CANONICAL BEHAVIOURAL SPECIFICATION for US-04. The Gherkin
# scenarios here define the expected system behaviour independently of any
# implementation detail — they describe WHAT the system does, not HOW.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-04.AC-1 through US-04.AC-4)
#     that Java tests reference via the @SpecificationRef annotation.
#   - This creates an explicit, navigable chain:
#
#       Requirement  →  Scenario (.feature)  →  Test (JUnit)  →  Code
#
#   - When a test is annotated with @SpecificationRef(value = "US-04.AC-1"),
#     a reader can look up this file to understand the business behaviour
#     the test is verifying.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Referenced by annotated tests via @SpecificationRef.
#
# Scenario IDs:
#   US-04.AC-1 → Scenario: Depositing a positive amount
#   US-04.AC-2 → Scenario: Depositing zero amount
#   US-04.AC-3 → Scenario: Depositing a negative amount
#   US-04.AC-4 → Scenario: Depositing to a non-existent portfolio
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Deposit Funds (US-04)

  As an investor managing my portfolio
  I want to add money to my portfolio's cash balance
  So that I have funds available for future stock purchases

  Scenario: Depositing a positive amount
    Given an existing portfolio with balance $5000
    When I POST /api/portfolios/{id}/deposits with {"amount": 2000}
    Then I receive 200 OK
    And the portfolio balance becomes $7000

  Scenario: Depositing zero amount
    Given an existing portfolio
    When I deposit amount 0
    Then I receive 400 Bad Request with ProblemDetail:
      | Field  | Value          |
      | title  | Invalid Amount |
      | status | 400            |

  Scenario: Depositing a negative amount
    Given an existing portfolio
    When I deposit amount -100
    Then I receive 400 Bad Request with ProblemDetail:
      | Field  | Value          |
      | title  | Invalid Amount |
      | status | 400            |

  Scenario: Depositing to a non-existent portfolio
    Given a non-existent portfolio ID
    When I deposit any amount
    Then I receive 404 Not Found with ProblemDetail:
      | Field  | Value               |
      | title  | Portfolio Not Found  |
      | status | 404                  |
