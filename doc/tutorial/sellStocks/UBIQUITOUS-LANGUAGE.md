# A Shared Vocabulary for HexaStock

**Goal:** explain a sale using the same terms as the code.

**Prerequisite:** none beyond the [first portfolio example](../../learning/START-HERE.md). **Reading time:** about 6 minutes.

## Start with the business words

Suppose a portfolio buys 10 shares at 100.00 each, then 5 shares at 120.00 each. It later sells 12 shares at 150.00 each.

| Term | Meaning in this project | Example |
|---|---|---|
| Portfolio | Cash and holdings managed together | One investor's learning portfolio |
| Ticker | The symbol identifying a stock | `AAPL` |
| Holding | The shares of one ticker within a portfolio | The portfolio's AAPL holding |
| Lot | Shares bought in one purchase, with a purchase time and unit price | 10 shares bought at 100.00 |
| Remaining quantity | Shares from a lot that have not been sold | 3 shares left in the second lot |
| FIFO | First In, First Out: consume the oldest remaining lots first | Take 10 from the first lot, then 2 from the second |
| Proceeds | Money received from the sale before any extensions such as fees | 12 × 150.00 = 1800.00 |
| Cost basis | Purchase cost of the shares consumed | 10 × 100.00 + 2 × 120.00 = 1240.00 |
| Realized profit | Proceeds minus cost basis for the completed sale | 560.00 |
| Unrealized gain | Current value minus purchase cost of shares still held | Depends on the current quote |

This is the accounting model used by the teaching project. It does not implement every rule of a brokerage or tax system.

## Give technical terms a concrete meaning

An **entity** has an identity that persists as its state changes. A lot remains the same lot after some of its shares are sold.

A **value object** represents a value rather than an independent identity. `Money`, `Price`, `ShareQuantity` and `Ticker` make meanings explicit at method boundaries. Their constructors and factories validate particular conditions; the types alone do not guarantee every rule of a sale.

An **aggregate** is a group of objects updated together to protect business rules. Here, `Portfolio` is its **root**, and `Holding` and `Lot` are children. The intended update path begins at the root.

An **invariant** is a condition that must hold after a successful operation. For example, a sale must not make the remaining share quantity negative.

A **use case** is an action the application offers, such as selling shares. A **port** is an interface describing an interaction with the application or a capability it needs. An **adapter** connects such an interface to HTTP, a database or another external technology.

## Why call this a ubiquitous language?

In DDD, a *ubiquitous language* is a shared vocabulary used by the people discussing a model and by its code, tests and documentation. Its purpose is to reduce ambiguity within a defined context. Begin with agreement on what a term means, then choose names that preserve that meaning.

For a sale, the same three quantities appear in `SellResult`, the REST response and the tests: `proceeds`, `costBasis` and `profit`. This makes it easier to compare an example with a running result. Names may differ at a technical boundary when the mapping is explicit; identical spelling everywhere is not itself proof of a correct model.

## Distinguish two meanings of transaction

A **financial transaction** records a deposit, withdrawal, purchase or sale. A **database transaction** groups persistence operations so that they commit or roll back together. A single database transaction may update the portfolio and store one financial transaction record.

Use the qualifier when discussing both concepts. Neither term needs to disappear from the model to avoid confusion.

## Trace an example to evidence

The [sale feature](../../features/sell-stocks.feature) describes FIFO behavior. Domain tests exercise the lot calculations; REST integration tests exercise the complete request. `@SpecificationRef` identifies the requirement a test is intended to cover. It is a reference, not an automatic proof that every assertion required by the scenario is present.

When reviewing a change, compare the input quantities, calculation and resulting state across the specification, test and implementation. Correct names help that review; numerical assertions supply the evidence.

## Check your understanding

Does a profit of 560.00 mean that cash increases by 560.00?

**Answer:** no. Cash increases by the proceeds, 1800.00. Profit subtracts the historical cost of the shares sold; that cost was paid when the shares were purchased.

Continue with [Stage 1: specify the sale](SELL-STOCK-01-BEHAVIOUR.md).

## References

- Evans, Eric. *Domain-Driven Design: Tackling Complexity in the Heart of Software.* Addison-Wesley, 2003. (See chapter 2 on Ubiquitous Language.)
- Fowler, Martin. "Ubiquitous Language." *martinfowler.com*, 2006. https://martinfowler.com/bliki/UbiquitousLanguage.html
- Vernon, Vaughn. *Implementing Domain-Driven Design.* Addison-Wesley, 2013. (See chapter 1 on language and context.)

