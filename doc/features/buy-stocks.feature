# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Buy Stocks (US-06)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the Buy Stocks use case
# in a human-readable format using Gherkin syntax.
#
# It serves as the CANONICAL BEHAVIOURAL SPECIFICATION for US-06. The Gherkin
# scenarios here define the expected system behaviour independently of any
# implementation detail — they describe WHAT the system does, not HOW.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-06.AC-1 through US-06.AC-8)
#     that Java tests reference via the @SpecificationRef annotation.
#   - This creates an explicit, navigable chain:
#
#       Requirement  →  Scenario (.feature)  →  Test (JUnit)  →  Code
#
#   - When a test is annotated with @SpecificationRef(value = "US-06.AC-1"),
#     a reader can look up this file to understand the business behaviour
#     the test is verifying.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Referenced by annotated tests via @SpecificationRef.
#
# Scenario IDs:
#   US-06.AC-1 → Scenario: Buying stock with sufficient funds
#   US-06.AC-2 → Scenario: Buying more shares of an already-held stock
#   US-06.AC-3 → Scenario: Buying stock with insufficient funds
#   US-06.AC-4 → Scenario: Buying stock with zero quantity
#   US-06.AC-5 → Scenario: Buying stock with negative quantity
#   US-06.AC-6 → Scenario: Buying stock with an invalid ticker
#   US-06.AC-7 → Scenario: Buying stock with an empty ticker
#   US-06.AC-8 → Scenario: Buying stock on a non-existent portfolio
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Buy Stocks (US-06)

  As an investor with a portfolio
  I want to purchase shares of a specific stock by providing the ticker symbol and quantity
  So that I can build my investment portfolio

  Background:
    Given a portfolio exists for owner "Alice"
    And the portfolio has a cash balance of $50000

  Scenario: Buying stock with sufficient funds
    When I POST /api/portfolios/{id}/purchases with {"ticker":"AAPL","quantity":5}
    Then I receive 200 OK
    And the balance decreases by (5 x market price)
    And a holding for AAPL appears with 5 remaining shares

  Scenario: Buying more shares of an already-held stock
    Given the portfolio already holds 5 shares of AAPL
    When I buy 3 more shares of AAPL
    Then a new lot is added to the existing AAPL holding
    And AAPL shows 8 remaining shares total

  Scenario: Buying stock with insufficient funds
    Given the portfolio has insufficient funds for the purchase
    When I buy stock
    Then I receive 409 Conflict with ProblemDetail:
      | Field  | Value              |
      | title  | Insufficient Funds |
      | status | 409                |
      | detail | (contains "Insufficient funds") |

  Scenario: Buying stock with zero quantity
    When I buy with quantity 0
    Then I receive 400 Bad Request with ProblemDetail:
      | Field  | Value            |
      | title  | Invalid Quantity |
      | status | 400              |
      | detail | (contains "Quantity must be positive") |

  Scenario: Buying stock with negative quantity
    When I buy with quantity -5
    Then I receive 400 Bad Request with ProblemDetail:
      | Field  | Value            |
      | title  | Invalid Quantity |
      | status | 400              |
      | detail | (contains "Quantity must be positive") |

  Scenario: Buying stock with an invalid ticker
    When I buy with ticker "ZZZZ_INVALID"
    Then I receive 400 Bad Request with ProblemDetail:
      | Field  | Value          |
      | title  | Invalid Ticker |
      | status | 400            |
      | detail | (contains "ZZZZ_INVALID") |
    And no holding is created for "ZZZZ_INVALID"

  Scenario: Buying stock with an empty ticker
    When I buy with ticker ""
    Then I receive 400 Bad Request with ProblemDetail:
      | Field  | Value          |
      | title  | Invalid Ticker |
      | status | 400            |

  Scenario: Buying stock on a non-existent portfolio
    Given a non-existent portfolio ID
    When I buy stock
    Then I receive 404 Not Found with ProblemDetail:
      | Field  | Value               |
      | title  | Portfolio Not Found  |
      | status | 404                  |
