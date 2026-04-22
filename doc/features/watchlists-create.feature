# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Create Watchlist (US-WL-01)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes the functional behaviour of the Create Watchlist use case
# in a human-readable format using Gherkin syntax.
#
# It serves as the CANONICAL BEHAVIOURAL SPECIFICATION for US-WL-01. The Gherkin
# scenarios here define the expected system behaviour independently of any
# implementation detail — they describe WHAT the system does, not HOW.
#
# WHY a .feature file?
#   - Gherkin is readable by developers and non-developers alike.
#   - Each scenario has a stable identifier (US-WL-01.AC-1 through US-WL-01.AC-3)
#     that Java tests reference via the @SpecificationRef annotation.
#   - This creates an explicit, navigable chain:
#
#       Requirement  →  Scenario (.feature)  →  Test (JUnit)  →  Code
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Referenced by annotated tests via @SpecificationRef.
#
# Scenario IDs:
#   US-WL-01.AC-1 → Scenario: Creating a new watchlist with valid fields
#   US-WL-01.AC-2 → Scenario: Creating a watchlist with blank owner name
#   US-WL-01.AC-3 → Scenario: Creating a watchlist with blank list name
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Create Watchlist (US-WL-01)

  As an investor
  I want to create a watchlist
  So that I can define price alerts and monitor them

  Scenario: Creating a new watchlist with valid fields
    When I POST /api/watchlists with {"ownerName":"alice","listName":"Tech","telegramChatId":"123456"}
    Then I receive 201 Created
    And the response contains a Location header pointing to /api/watchlists/{id}
    And the response body contains:
      | Field          | Value   |
      | ownerName      | alice   |
      | listName       | Tech    |
      | active         | true    |
      | telegramChatId | 123456  |
    And the watchlist contains no alerts

  Scenario: Creating a watchlist with blank owner name
    When I POST /api/watchlists with {"ownerName":"","listName":"Tech","telegramChatId":"123456"}
    Then I receive 400 Bad Request

  Scenario: Creating a watchlist with blank list name
    When I POST /api/watchlists with {"ownerName":"alice","listName":"","telegramChatId":"123456"}
    Then I receive 400 Bad Request

