Feature: Buy stocks
  As an investor
  I want to deposit cash and buy shares of a specific stock by providing the ticker, quantity,
  and price
  So that I can build a position to sell later, using money I actually have

  # Translated 1:1 from buy-stocks-spec.md (Exercise 2, Part Two model answer).
  # AC ids here are scoped to buy-stocks-spec.md and do not continue AC-01..24 from
  # sell_stocks.feature.

  Background:
    Given a portfolio for owner "Alice" with a cash balance of 0.00
    And Alice holds nothing

  @buy @deposit @happy-path
  Scenario: AC-01 - A deposit increases the cash balance
    When Alice deposits 1000.00
    Then Alice's cash balance is exactly 1000.00

  @buy @deposit @rejection
  Scenario Outline: AC-02 - Rejected - non-positive deposit
    When Alice attempts to deposit <amount>
    Then the deposit is rejected with an invalid-amount error
    And Alice's cash balance is still 0.00

    Examples:
      | amount |
      | 0.00   |
      | -50.00 |

  @buy @happy-path
  Scenario: AC-03 - First purchase of a ticker creates a new holding
    Given Alice has deposited 1000.00
    When Alice buys 5 shares of "AAPL" at 100.00
    Then Alice's cash balance is 500.00
    And a new "AAPL" holding exists with exactly one lot: 5 @ 100.00

  @buy @happy-path @money
  Scenario: AC-04 - Purchase deducts the exact cost from the balance
    Given Alice has deposited 1000.00
    When Alice buys 5 shares of "AAPL" at 100.00
    Then Alice's cash balance is exactly 500.00

  @buy @happy-path @lot-order
  Scenario: AC-05 - A second purchase appends a second lot, in order
    Given Alice has deposited 1000.00
    And Alice has bought 5 shares of "AAPL" at 100.00
    When Alice buys 3 more shares of "AAPL" at 110.00
    Then Alice's cash balance is 170.00
    And the "AAPL" lots are, in order, 5 @ 100.00 then 3 @ 110.00

  @buy @lot-order
  Scenario: AC-06 - Buying never merges lots, even at an identical price
    Given Alice has deposited 1000.00
    And Alice has bought 5 shares of "AAPL" at 100.00
    When Alice buys 5 more shares of "AAPL" at 100.00
    Then the "AAPL" holding has two separate lots of 5 @ 100.00 each

  @buy @rejection @money
  Scenario: AC-07 - Rejected - insufficient funds
    Given Alice has deposited 1000.00
    And Alice has bought 5 shares of "AAPL" at 100.00
    When Alice attempts to buy 4 shares of "AAPL" at 130.00
    Then the purchase is rejected with an insufficient-funds error reporting "Available: 500.00, Required: 520.00"
    And Alice's cash balance is still 500.00
    And the "AAPL" lots are still just 5 @ 100.00

  @buy @rejection
  Scenario Outline: AC-08 - Rejected - non-positive quantity
    Given Alice has deposited 1000.00
    When Alice attempts to buy <quantity> shares of "AAPL" at 100.00
    Then the purchase is rejected with an invalid-quantity error
    And Alice's cash balance is still 1000.00

    Examples:
      | quantity |
      | 0        |
      | -3       |

  @buy @rejection @ticker
  Scenario Outline: AC-09 - Rejected - malformed ticker
    Given Alice has deposited 1000.00
    When Alice attempts to buy 5 shares of "<symbol>" at 100.00
    Then the purchase is rejected with an invalid-ticker error
    And Alice's cash balance is still 1000.00

    Examples:
      | symbol  |
      | aapl    |
      | TOOLONG |
      |         |

  @buy @rejection
  Scenario Outline: AC-10 - Rejected - non-positive price
    Given Alice has deposited 1000.00
    When Alice attempts to buy 5 shares of "AAPL" at <price>
    Then the purchase is rejected with an invalid-amount error

    Examples:
      | price  |
      | 0.00   |
      | -10.00 |

  @buy @rejection @state
  Scenario: AC-11 - A rejected purchase leaves no partial state behind
    Given Alice has deposited 1000.00
    And Alice does not hold "AAPL"
    When Alice attempts to buy 0 shares of "AAPL" at 100.00
    Then the purchase is rejected
    And Alice still does not hold "AAPL"

  @buy @lifecycle
  Scenario: AC-12 - Buying again after full liquidation starts a fresh holding
    Given Alice's "AAPL" position was fully sold for 2250.00
    When Alice buys 4 shares of "AAPL" at 130.00
    Then a new "AAPL" holding exists with exactly one lot: 4 @ 130.00
    And it carries no lots from the previous position
