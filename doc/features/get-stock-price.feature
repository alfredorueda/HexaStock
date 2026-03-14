# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Get Stock Price (US-10)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the Get Stock Price use case
# in a human-readable format using Gherkin syntax.
#
# It serves as the CANONICAL BEHAVIOURAL SPECIFICATION for US-10. The Gherkin
# scenarios here define the expected system behaviour independently of any
# implementation detail — they describe WHAT the system does, not HOW.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-10.AC-1, US-10.AC-2)
#     that Java tests reference via the @SpecificationRef annotation.
#   - This creates an explicit, navigable chain:
#
#       Requirement  →  Scenario (.feature)  →  Test (JUnit)  →  Code
#
#   - When a test is annotated with @SpecificationRef(value = "US-10.AC-1"),
#     a reader can look up this file to understand the business behaviour
#     the test is verifying.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Referenced by annotated tests via @SpecificationRef.
#
# Scenario IDs:
#   US-10.AC-1 → Scenario: Getting the current price for a valid stock ticker
#   US-10.AC-2 → Scenario: Getting the price for an invalid ticker format
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Get Stock Price (US-10)

  As an investor
  I want to look up the current market price of a stock
  So that I can make informed trading decisions

  Scenario: Getting the current price for a valid stock ticker
    Given a valid ticker symbol "AAPL"
    When I GET /api/stocks/AAPL
    Then I receive 200 OK
    And the response body contains:
      | Field    | Value  |
      | symbol   | AAPL   |
      | price    | (current market price) |
      | time     | (timestamp)            |
      | currency | USD                    |

  Scenario: Getting the price for an invalid ticker format
    Given an invalid ticker format "aapl_invalid"
    When I GET /api/stocks/aapl_invalid
    Then I receive 400 Bad Request with ProblemDetail:
      | Field  | Value          |
      | title  | Invalid Ticker |
      | status | 400            |
