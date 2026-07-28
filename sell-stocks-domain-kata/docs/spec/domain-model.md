# Domain model

The structure the Sell Stocks behaviour relies on: which classes exist, what they hold, and
what they can do. The behaviour itself is in [`sell-stocks-spec.md`](sell-stocks-spec.md).

The same model is drawn two ways — pick whichever you read more fluently:

* [`domain-class-diagram.puml`](domain-class-diagram.puml) — object-oriented classes.
* [`domain-er-diagram.puml`](domain-er-diagram.puml) — the same thing as database tables.
  See [Reading the model as tables](#reading-the-model-as-tables) below for what changes
  between the two.

## Entities

**Portfolio** *(aggregate root)* — `id: PortfolioId`, `owner: String`, `balance: Money`,
`holdings: Map<Ticker, Holding>`, `createdAt: LocalDateTime`.
Key methods: `sell(Ticker, ShareQuantity, Price) -> SellResult` (throws
`InvalidQuantityException` if the quantity is ≤ 0, `HoldingNotFoundException` if the ticker
is not held), `buy(Ticker, ShareQuantity, Price)`, `getHolding(Ticker)` (throws
`HoldingNotFoundException` if absent), `getBalance()`.
All state changes to `Holding` and `Lot` must pass through `Portfolio`.

**Holding** *(entity)* — `id: HoldingId`, `ticker: Ticker`, `lots: List<Lot>` ordered by
purchase date.
Key methods: `sell(ShareQuantity, Price) -> SellResult` (FIFO accounting; throws
`ConflictQuantityException` if there are not enough shares; removes empty lots),
`buy(ShareQuantity, Price)`, `getTotalShares()`, `getLots()`.

**Lot** *(entity)* — `id: LotId`, `initialShares: ShareQuantity`,
`remainingShares: ShareQuantity`, `unitPrice: Price`, `purchasedAt: LocalDateTime`.
Key methods: `reduce(ShareQuantity)` (throws `ConflictQuantityException` if the quantity
exceeds the remaining shares), `calculateCostBasis(ShareQuantity) -> Money`,
`getRemainingShares()`, `isEmpty()`.

## Value objects

Immutable, and validated at construction — an invalid value can never exist, so no operation
has to defend against one.

| Value object    | Wraps                           | Validation                                                          | Purpose                                         |
| --------------- | ------------------------------- | -------------------------------------------------------------------- | ----------------------------------------------- |
| `Money`         | `BigDecimal` (scale 2, HALF_UP) | Not null                                                            | Monetary amounts                                |
| `Price`         | `BigDecimal` (scale 2, HALF_UP) | Must be > 0 (`InvalidAmountException`)                              | Per-share price                                 |
| `ShareQuantity` | `int`                           | Must be ≥ 0 (`InvalidQuantityException`); selling requires > 0      | Number of shares                                |
| `Ticker`        | `String`                        | Must match `^[A-Z]{1,5}$` (`InvalidTickerException`) — see §6.1 of [`sell-stocks-spec.md`](sell-stocks-spec.md) | Stock symbol           |
| `PortfolioId`   | `String`                        | Not null, not blank                                                 | Portfolio identity                              |
| `HoldingId`     | `String`                        | Not null, not blank                                                 | Holding identity                                |
| `LotId`         | `String`                        | Not null, not blank                                                 | Lot identity                                    |
| `SellResult`    | `Money` × 3                     | —                                                                   | Sale outcome: `proceeds`, `costBasis`, `profit` |

Money is always `BigDecimal` at scale 2, `HALF_UP` — never `double` or `float`.

## Relationships

* A **Portfolio** contains 0..* **Holdings**, indexed by `Ticker`.
* A **Holding** contains **Lots**, ordered chronologically; FIFO consumes the oldest first.

The exceptions named above are described in [`error-contract.md`](error-contract.md).

---

## Reading the model as tables

If class diagrams are not your native language, [`domain-er-diagram.puml`](domain-er-diagram.puml)
shows the same model as three tables. The structure maps over almost directly:

| Class diagram                       | ER diagram                                            |
| ----------------------------------- | ----------------------------------------------------- |
| `Portfolio`, `Holding`, `Lot`       | the `portfolio`, `holding` and `lot` tables           |
| `PortfolioId`, `HoldingId`, `LotId` | primary keys                                          |
| composition (`*--`)                 | a foreign key with `ON DELETE CASCADE`                |
| `Map<Ticker, Holding>`              | `UNIQUE (portfolio_id, ticker)`                       |
| `Money`, `Price`                    | `DECIMAL(19,2)` columns                               |
| `ShareQuantity`                     | an `INTEGER` column with `CHECK (>= 0)`               |
| `Ticker`                            | a `VARCHAR(5)` column                                 |
| `lots` ordered by `purchasedAt`     | `ORDER BY purchased_at` — see the warning below       |

The structure translates cleanly. The *rules* do not — the FIFO order, the money formulas and
the "nothing changes unless the whole sale succeeds" guarantee have no equivalent in a schema.
What is lost, why it matters, and what to do about it is written up in
[`er-model-limitations.md`](er-model-limitations.md).
