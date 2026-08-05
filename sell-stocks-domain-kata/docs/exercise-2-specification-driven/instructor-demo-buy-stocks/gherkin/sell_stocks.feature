Feature: Sell stocks
  As an investor with existing stock holdings
  I want to sell shares of a specific stock by providing the ticker symbol and quantity
  So that I can realize profits, cut losses, or rebalance my portfolio

  # Translated 1:1 from sell-stocks-spec.md — every Scenario title carries its AC id so
  # instructors can point at a JUnit test, an acceptance-criterion row, and a Gherkin
  # scenario for the exact same behaviour, side by side.

  Background:
    Given a portfolio for owner "Alice" with a cash balance of 0.00
    And Alice holds a lot of 10 shares of "AAPL" bought at 100.00
    And Alice holds a lot of 5 shares of "AAPL" bought at 120.00
    And the current market price for "AAPL" is 150.00

  @sell @happy-path
  Scenario: AC-01 - Sale consumed entirely from a single lot
    When Alice sells 8 shares of "AAPL" at 150.00
    Then the sale succeeds with proceeds 1200.00, cost basis 800.00 and profit 400.00
    And the "AAPL" lots are now 2 @ 100.00 and 5 @ 120.00

  @sell @happy-path
  Scenario: AC-02 - Sale consumed across multiple lots, emptied lot removed
    When Alice sells 12 shares of "AAPL" at 150.00
    Then the sale succeeds with proceeds 1800.00, cost basis 1240.00 and profit 560.00
    And the "AAPL" lots are now 3 @ 120.00

  @sell @boundary
  Scenario: AC-03 - Smallest possible sale
    When Alice sells 1 share of "AAPL" at 150.00
    Then the sale succeeds with proceeds 150.00, cost basis 100.00 and profit 50.00
    And the "AAPL" lots are now 9 @ 100.00 and 5 @ 120.00

  @sell @boundary
  Scenario: AC-04 - The sale exactly exhausts the oldest lot
    When Alice sells 10 shares of "AAPL" at 150.00
    Then the sale succeeds with proceeds 1500.00, cost basis 1000.00 and profit 500.00
    And the "AAPL" lots are now 5 @ 120.00

  @sell @boundary
  Scenario: AC-05 - The sale liquidates the entire position
    When Alice sells 15 shares of "AAPL" at 150.00
    Then the sale succeeds with proceeds 2250.00, cost basis 1600.00 and profit 650.00
    And Alice no longer holds "AAPL"

  @sell @happy-path
  Scenario: AC-06 - A loss is a valid, expected outcome
    When Alice sells 8 shares of "AAPL" at 90.00
    Then the sale succeeds with proceeds 720.00, cost basis 800.00 and profit -80.00

  @sell @fifo
  Scenario: AC-07 - FIFO consumes the oldest lot first
    When Alice sells 8 shares of "AAPL" at 150.00
    Then the cost basis is 800.00, computed at 100.00 per share, not at 120.00
    And the lot bought at 120.00 still has all 5 of its shares

  @sell @happy-path
  Scenario: AC-08 - Proceeds are credited to the cash balance
    When Alice sells 8 shares of "AAPL" at 150.00
    Then Alice's cash balance increases from 0.00 to 1200.00

  @sell @invariant
  Scenario: AC-09 - Profit is always proceeds minus cost basis
    When Alice sells 8 shares of "AAPL" at 150.00
    Then the reported profit equals the reported proceeds minus the reported cost basis

  @sell @rejection
  Scenario: AC-10 - Rejected - quantity of zero
    When Alice attempts to sell 0 shares of "AAPL"
    Then the sale is rejected with an invalid-quantity error
    And nothing is sold

  @sell @rejection
  Scenario: AC-11 - Rejected - negative quantity
    When Alice attempts to sell -5 shares of "AAPL"
    Then the sale is rejected with an invalid-quantity error

  @sell @rejection
  Scenario: AC-12 - Rejected - selling more shares than are held
    When Alice attempts to sell 16 shares of "AAPL" at 150.00
    Then the sale is rejected with a conflict error reporting "Available: 15, Requested: 16"

  @sell @rejection
  Scenario: AC-13 - Rejected - the portfolio does not hold that ticker
    When Alice attempts to sell 5 shares of "MSFT" at 150.00
    Then the sale is rejected with a holding-not-found error

  @sell @rejection
  Scenario: AC-14 - Rejected - non-positive sale price
    When Alice attempts to sell shares of "AAPL" at a price of 0.00
    Then the sale is rejected with an invalid-amount error

  @sell @rejection @validation-order
  Scenario: AC-15 - Quantity is validated before the holding is looked up
    When Alice attempts to sell 0 shares of "MSFT"
    Then the sale is rejected with an invalid-quantity error, not a holding-not-found error

  @sell @rejection @state
  Scenario: AC-16 - A rejected sale leaves the holding untouched
    When Alice attempts to sell 16 shares of "AAPL" at 150.00
    Then the sale is rejected
    And the "AAPL" lots are still 10 @ 100.00 and 5 @ 120.00
    And Alice's cash balance is still 0.00

  @sell @rejection @state
  Scenario: AC-17 - A rejected sale of an unheld ticker leaves the portfolio untouched
    When Alice attempts to sell 5 shares of "MSFT"
    Then the sale is rejected
    And Alice's cash balance is still 0.00
    And the "AAPL" lots are still 10 @ 100.00 and 5 @ 120.00

  @sell @rejection @lot
  Scenario: AC-18 - A lot cannot be reduced below zero
    Given a single lot of 10 shares at 100.00
    When the lot is reduced by 11 shares
    Then the reduction is rejected with a conflict error
    And the lot still has 10 remaining shares

  @sell @rejection @lot
  Scenario: AC-19 - A lot cannot be created with a non-positive share count
    When a lot is created with 0 shares at 100.00
    Then the creation is rejected with an invalid-quantity error

  @sell @rejection @ticker
  Scenario Outline: AC-20 - Rejected - malformed ticker
    When a ticker is built from "<symbol>"
    Then it is rejected with an invalid-ticker error

    Examples:
      | symbol  |
      | aapl    |
      | Aapl    |
      | TOOLONG |
      | 123     |
      | AA1     |
      | A-B     |
      |         |

  @sell @boundary @ticker
  Scenario Outline: AC-21 - Boundary - well-formed ticker
    When a ticker is built from "<symbol>"
    Then it is accepted and keeps the symbol "<symbol>"

    Examples:
      | symbol |
      | A      |
      | AA     |
      | AAPL   |
      | GOOGL  |

  @sell @state @lifecycle
  Scenario: AC-22 - Selling the whole position removes the holding
    When Alice sells all 15 shares of "AAPL" at 150.00
    Then the sale succeeds and credits 2250.00
    When Alice attempts to sell 1 more share of "AAPL"
    Then the sale is rejected with a holding-not-found error, not a conflict error

  @sell @buy @lifecycle
  Scenario: AC-23 - Buying again after full liquidation starts a fresh holding
    Given Alice's "AAPL" position was just fully sold
    When Alice buys 4 shares of "AAPL" at 130.00
    Then a new "AAPL" holding exists with exactly one lot: 4 @ 130.00

  @buy @rejection @state
  Scenario: AC-24 - A rejected purchase leaves no holding behind
    Given Alice does not hold "MSFT"
    When Alice attempts to buy 0 shares of "MSFT"
    Then the purchase is rejected with an invalid-quantity error
    And Alice still does not hold "MSFT"
