# API Behavior, Limits and Extension Exercises

This page distinguishes the implemented baseline from work that would require a new contract and implementation.

## Representation conventions

Creation returns `cashBalance` and `currency`; portfolio lookup returns `balance` and `createdAt`. Financial transactions are flat DTOs. A purchase has `profit: 0.00`, not null; cash transactions have quantity zero, null ticker and null unit price.

Amounts use decimal arithmetic in the domain and serialize as JSON numbers. Clients should use appropriate decimal representations when exact accounting values matter. `Money` rounds to scale two with `HALF_UP`; this is not the same rounding mode as `HALF_EVEN`.

Portfolio and financial-transaction timestamps use `LocalDateTime` without a timezone. Quote timestamps use `Instant`. Creation and quote responses hardcode `USD`; there is no multi-currency domain or conversion service.

## Current limitations

| Area | Behavior on `main` |
|---|---|
| Transaction filtering | `type` is accepted but ignored. |
| Pagination | List and history endpoints return full collections. |
| Report price failures | Provider errors propagate; no service-level zero-price fallback. |
| Holding lifecycle | Fully depleted lots are removed; an empty holding remains. |
| Historical allocations | Sale records do not preserve each consumed lot ID and allocation. |
| HTTP replay | No idempotency-key mechanism prevents duplicate submitted operations. |
| Quote freshness | External adapters can return cached quotes. |
| Input errors | The custom advice covers listed application exceptions, not every malformed request. |

The domain's reconstruction hooks and public child methods also mean that intended aggregate encapsulation is not enforced by every available mutation path. Study [the aggregate boundary](../architecture/aggregates/01-CONSISTENCY-BOUNDARY.md) before extending it.

## Reasonable extension exercises

Choose one exercise at a time:

1. **Filtering and pagination:** define allowed filters, stable ordering, page size and behavior for an unknown portfolio; then update the port, adapters and tests.
2. **Explicit quote freshness:** decide which timestamp represents the source quote and how stale data is rejected or displayed.
3. **Validation contract:** define missing, null and blank fields, precedence when several inputs are invalid, and the intended HTTP representation.
4. **History evidence:** specify whether per-lot sale allocations must survive depletion before designing storage.
5. **Alternative business rules:** define fees, settlement eligibility or lot selection in a separate extension contract.

Watchlists, settlement, reservations, fee accounting, tax reports and benchmark comparisons are not baseline endpoints. Advanced design documents may discuss them; their presence in documentation or feature files is not implementation evidence.

## Turn former ambiguities into a review exercise

For a sale, compare the actual behavior with a proposed alternative:

- Invalid ticker syntax currently fails at the controller boundary. What test would distinguish that from checking portfolio existence first?
- A complete sale currently leaves an empty holding. What API, reporting and persistence changes would be needed to remove it instead?

Write a short decision and examples for the alternative. Keep them labeled as proposed behavior. The current behavior is available in the [sale reference](US-07-SELL-STOCKS.md); the exercise is to reason about a deliberate change, not to guess which implementation exists.

[Back to the API reference](../stock-portfolio-api-specification.md)
