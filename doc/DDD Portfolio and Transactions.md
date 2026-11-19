# Portfolio and Transaction Aggregates

Why does a portfolio contain holdings and lots, but keep transaction history separately? This learning path answers that question in three short parts.

**Prerequisites:** the [domain vocabulary](tutorial/sellStocks/UBIQUITOUS-LANGUAGE.md) and the [domain-only sale tutorial](tutorial/sellStocks/SELL-STOCK-DOMAIN-TUTORIAL.md).

| Part | Question | Outcome |
|---|---|---|
| [1. Choose a consistency boundary](architecture/aggregates/01-CONSISTENCY-BOUNDARY.md) | Which state must change together? | Explain the current aggregate and service responsibilities. |
| [2. Query history separately](architecture/aggregates/02-HISTORY-AND-SCALE.md) | Why separate current state from history? | Identify current limitations and design a pagination extension. |
| [3. Compare alternatives](architecture/aggregates/03-ALTERNATIVES.md) | When would another design be justified? | Compare four options without treating one as universally correct. |

Each part takes about 8–10 minutes. Complete the checkpoint before continuing.

## Current implementation at a glance

`Portfolio` owns cash and a map of `Holding` objects keyed by `Ticker`. Each holding contains purchase lots. Financial transactions are separate immutable domain records associated by `PortfolioId`; they are not a collection inside `Portfolio`.

The application service coordinates the portfolio update and transaction recording within a database transaction. The outbound interface is `TransactionPort`, declared in `application`, not a domain-layer `TransactionRepository`.

This design separates responsibilities. It does **not** make active holdings bounded, implement pagination, provide a tamper-proof ledger or prove production scalability. The following chapters distinguish those potential improvements from the code on `main`.

## Further reading

- Evans, Eric. *Domain-Driven Design*. Addison-Wesley, 2003. Aggregate boundaries and invariants.
- Vernon, Vaughn. *Implementing Domain-Driven Design*. Addison-Wesley, 2013. Aggregate design and application services.
- Nilsson, Jimmy. *Applying Domain-Driven Design and Patterns*. Addison-Wesley, 2006. Modeling and implementation trade-offs.
- Fowler, Martin. *Patterns of Enterprise Application Architecture*. Addison-Wesley, 2002. Domain models and transaction boundaries.
- Fowler, Martin. [CQRS](https://martinfowler.com/bliki/CQRS.html), 2011. Separate read and update models and their costs.
- Young, Greg. [CQRS Documents](https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf), 2010. Additional background for the advanced alternative.
