# ═══════════════════════════════════════════════════════════════════════════════
# BEHAVIOURAL SPECIFICATION — Activate / Deactivate Watchlist (US-WL-03)
# ═══════════════════════════════════════════════════════════════════════════════
#
# This file describes how a watchlist is toggled in/out of Market Sentinel
# monitoring.
#
# This file is NOT executed by Cucumber or any BDD framework.
# It is a specification document — the tests are the executable layer.
#
# Scenario IDs:
#   US-WL-03.AC-1 → Scenario: Deactivating an active watchlist
#   US-WL-03.AC-2 → Scenario: Activating an inactive watchlist
# ═══════════════════════════════════════════════════════════════════════════════

Feature: Activate / Deactivate Watchlist (US-WL-03)

  Background:
    Given a watchlist exists with id "wl-1"

  Scenario: Deactivating an active watchlist
    When I POST /api/watchlists/wl-1/deactivation
    Then I receive 200 OK
    And the watchlist is inactive

  Scenario: Activating an inactive watchlist
    Given watchlist "wl-1" is inactive
    When I POST /api/watchlists/wl-1/activation
    Then I receive 200 OK
    And the watchlist is active

