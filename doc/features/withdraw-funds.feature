# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Withdraw Funds (US-05)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the Withdraw Funds use case
# in a human-readable format using Gherkin syntax.
#
# It serves as the CANONICAL BEHAVIOURAL SPECIFICATION for US-05. The Gherkin
# scenarios here define the expected system behaviour independently of any
# implementation detail — they describe WHAT the system does, not HOW.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-05.AC-1 through US-05.AC-6)
#     that Java tests reference via the @SpecificationRef annotation.
#   - This creates an explicit, navigable chain:
#
#       Requirement  →  Scenario (.feature)  →  Test (JUnit)  →  Code
#
#   - When a test is annotated with @SpecificationRef(value = "US-05.AC-1"),
#     a reader can look up this file to understand the business behaviour
#     the test is verifying.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Referenced by annotated tests via @SpecificationRef.
#
# Scenario IDs:
#   US-05.AC-1 → Scenario: Withdrawing a valid amount
#   US-05.AC-2 → Scenario: Withdrawing zero amount
#   US-05.AC-3 → Scenario: Withdrawing a negative amount
#   US-05.AC-4 → Scenario: Withdrawing more than the balance
#   US-05.AC-5 → Scenario: Withdrawing from a zero-balance portfolio
#   US-05.AC-6 → Scenario: Withdrawing from a non-existent portfolio
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Withdraw Funds (US-05)

  As an investor managing my portfolio
  I want to withdraw money from my portfolio's cash balance
  So that I can use these funds elsewhere

  Scenario: Withdrawing a valid amount
    Given a portfolio with balance $5000
    When I POST /api/portfolios/{id}/withdrawals with {"amount": 2000}
    Then I receive 200 OK
    And the portfolio balance becomes $3000

  Scenario: Withdrawing zero amount
    Given an existing portfolio
    When I withdraw amount 0
    Then I receive 400 Bad Request with ProblemDetail:
      | Field  | Value          |
      | title  | Invalid Amount |
      | status | 400            |

  Scenario: Withdrawing a negative amount
    Given an existing portfolio
    When I withdraw amount -50
    Then I receive 400 Bad Request with ProblemDetail:
      | Field  | Value          |
      | title  | Invalid Amount |
      | status | 400            |

  Scenario: Withdrawing more than the balance
    Given a portfolio with balance $100
    When I withdraw $200
    Then I receive 409 Conflict with ProblemDetail:
      | Field  | Value              |
      | title  | Insufficient Funds |
      | status | 409                |
      | detail | (contains "Insufficient funds") |

  Scenario: Withdrawing from a zero-balance portfolio
    Given a portfolio with balance $0
    When I withdraw $1
    Then I receive 409 Conflict with ProblemDetail:
      | Field  | Value              |
      | title  | Insufficient Funds |
      | status | 409                |

  Scenario: Withdrawing from a non-existent portfolio
    Given a non-existent portfolio ID
    When I withdraw any amount
    Then I receive 404 Not Found with ProblemDetail:
      | Field  | Value               |
      | title  | Portfolio Not Found  |
      | status | 404                  |
