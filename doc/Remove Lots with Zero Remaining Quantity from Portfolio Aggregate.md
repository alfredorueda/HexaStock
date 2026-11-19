# Why Depleted Lots Are Removed

**Goal:** distinguish current investment state from the historical evidence needed for reporting.

**Prerequisite:** [the FIFO example](tutorial/sellStocks/SELL-STOCK-03-FIFO.md). **Reading time:** about 5 minutes.

## The decision in the current code

A lot records shares acquired in one purchase. During `Holding.sell`, each consumed lot has its remaining quantity reduced. When that quantity reaches zero, the iterator removes the lot from the holding's list.

For example, buying 10 shares at 100.00 and 5 at 120.00, then selling 12, leaves one lot: 3 shares from the second purchase. The first lot no longer participates in future sales.

This keeps the sale model focused on shares still available to sell. It reduces retained state, but does not impose a maximum aggregate size: a portfolio can continue accumulating active lots.

## A holding can still be empty

The current implementation removes depleted lots, **not the holding itself**. After all shares of a ticker are sold, the portfolio may still contain a holding with zero lots and zero total shares.

That distinction matters when predicting later behavior. A request to sell from an empty existing holding can fail for insufficient quantity; it is not necessarily the same case as a ticker absent from the portfolio.

## What history is preserved?

The application service records purchases and sales separately from the portfolio. A `SaleTransaction` stores the ticker, quantity, unit price, proceeds, profit and creation time. It does not store the IDs and quantities of every lot consumed.

Consequently, the current model supports transaction history and financial summaries, but the transaction record alone is not a complete per-lot audit record. Do not promise exact reconstruction of original lot identities after depletion without an explicit allocation-history design.

If such evidence becomes a requirement, possible extensions include storing sale-to-lot allocations or maintaining a separate historical lot representation. Specify the required queries, retention and consistency before choosing an implementation.

## Compare the alternatives

| Choice | Benefit | Cost |
|---|---|---|
| Keep depleted lots in the active holding | Historical lot details remain nearby. | Every load may include increasing amounts of inactive state; algorithms must distinguish it. |
| Remove depleted lots, as now | Future sale logic works with remaining lots. | Historical allocation evidence needs a separate representation if required. |

Neither choice alone establishes scalability or audit compliance. Measure actual loads and define historical requirements. The aggregate boundary answers which state is needed for current business rules; it does not prescribe every reporting representation.

## Checkpoint

After selling the final 3 shares from the example, how many lots and holdings remain for that ticker?

**Answer:** zero lots, but the existing holding remains. A new purchase can add a lot to it.

Continue with [portfolio and transaction boundaries](DDD%20Portfolio%20and%20Transactions.md).

## References

- Evans, Eric. *Domain-Driven Design*. Addison-Wesley, 2003. Aggregates and consistency boundaries.
- Vernon, Vaughn. *Implementing Domain-Driven Design*. Addison-Wesley, 2013. Aggregate design.
- Nilsson, Jimmy. *Applying Domain-Driven Design and Patterns*. Addison-Wesley, 2006. Domain and persistence trade-offs.
