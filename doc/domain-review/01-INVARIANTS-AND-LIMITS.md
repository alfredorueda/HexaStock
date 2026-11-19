# 1. Inspect invariants and their limits

**Goal:** support a review claim with source and an observable example. An invariant is a rule that valid operations must preserve.

## Follow the supported operation path

`Portfolio` owns cash and a map of holdings; each `Holding` owns ordered lots. Buy and sell coordinate cash with lot changes. Transaction history is stored separately, and application services save both the portfolio and its new financial transaction within the configured database transaction.

| Claim | Current enforcement | Boundary to inspect |
|---|---|---|
| Deposits and withdrawals are positive | `Portfolio.deposit` / `withdraw` | `Money` itself allows negative values for other uses |
| Purchases require enough cash | `Portfolio.buy` | Public construction can accept a negative initial balance |
| Sell quantity is positive | `Portfolio.sell` | `ShareQuantity.of(0)` is valid state; direct child calls differ |
| Shares sold cannot exceed shares held | `Holding.sell` | Concurrent requests also need persistence coordination |
| Lots are consumed FIFO | Insertion traversal; rehydration inserts by `purchasedAt` | Equal timestamps have no explicit secondary tie-breaker |
| Depleted lots leave current state | Iterator removal during sale | Empty holdings remain; transactions are separate history |
| Profit follows selected cost basis | `SellResult.of` computes proceeds minus cost | Public record construction can supply an independent profit |

Inspect [Portfolio.java](https://github.com/alfredorueda/HexaStock/blob/main/domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Portfolio.java), [Holding.java](https://github.com/alfredorueda/HexaStock/blob/main/domain/src/main/java/cat/gencat/agaur/hexastock/model/portfolio/Holding.java), and their focused tests. An operation path can be well guarded without every public construction or mutation path enforcing the same contract.

## Encapsulation is more than an unmodifiable collection

`Portfolio.getHoldings()` and `Holding.getLots()` return list copies. Callers cannot add or remove elements through those lists, but the contained `Holding` and `Lot` instances remain mutable. `getHolding()` also returns the actual holding. Therefore “all mutations can only pass through the root” is stronger than the API currently guarantees.

A possible hardening exercise is to expose immutable observations and restrict child mutation. First identify mapper and test callers, define a reconstitution path, and check that persistence round trips still work. Documentation alone cannot enforce access restrictions.

## Value objects reduce, but do not eliminate, invalid states

`Money`, `Price`, `ShareQuantity`, and `Ticker` distinguish meanings that primitives would conflate. Inspect validation and normalization order: `Price` checks positivity before rounding to two decimals, so a tiny positive input can normalize to zero. Decide whether that is allowed before proposing a behavior change.

Records provide final component fields, not automatic deep immutability or complete validation. A record containing a mutable collection needs defensive handling. A public canonical constructor is still available even if a convenience factory computes derived fields. See the [Java 21 Record contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Record.html).

## History and reporting

The sealed `Transaction` hierarchy has four record variants: deposit, withdrawal, purchase, and sale. Variant fields express their different meanings. Interface accessors and the flat REST DTO still use null/zero defaults for inapplicable fields; a sealed hierarchy does not eliminate every nullable representation.

Constructors validate required values, but do not prove that a recorded transaction corresponds to a committed portfolio operation. A transaction boundary protects atomic persistence when correctly configured; it cannot detect a programmer omitting the history write. Verify both records and state in integration tests, including rollback cases.

`HoldingPerformanceCalculator` combines historical purchases and realized profit with current remaining lots. This is a deliberate combination of different measures, not intrinsically a modeling defect. It does require consistent inputs. The [reporting lessons](../tutorial/portfolioReporting/HOLDINGS-PERFORMANCE-AT-SCALE.md) explain the calculation and alternatives.

## Scope is part of correctness

The project models integer shares and single-currency amounts with simplified quoting and FIFO accounting. Fees, taxes, settlement restrictions, dividends, corporate actions, order execution, and authentication are not baseline capabilities. The settlement exercise is hypothetical; do not infer universal brokerage or legal rules from it.

**Checkpoint:** produce one supported invariant claim, one API escape path, and one proposed improvement. Keep the latter separate from a description of existing behavior.

Next: [compare model alternatives](02-MODEL-ALTERNATIVES.md).
