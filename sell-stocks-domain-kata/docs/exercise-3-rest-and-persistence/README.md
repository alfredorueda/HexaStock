# Exercise 3 — REST API and persistence

> **Status: planned. Nothing here is built yet.** This file records the shape of the exercise so
> it can be reviewed and argued with before any code exists — which is the habit Exercise 2 is
> trying to teach.

## The goal

Take the domain from Exercise 2 **unchanged** and put a real application around it: Spring Boot,
a small REST API, and a database through JPA. No authentication, no security layer.

Still specification-driven. The prompt stays small; the new decisions go into new specification
files and decision records.

## What must not change

The domain. `Portfolio`, `Holding`, `Lot`, the value objects, the exceptions, and all 24
acceptance criteria stay exactly as they are, and the 36 existing tests keep passing untouched.

If the domain *has* to be edited to fit Spring or JPA, that is the most interesting result the
exercise can produce, and it should be discussed rather than quietly worked around.

## Architecture

A plain **three-layer** application:

```
controller  ->  service  ->  repository  ->  database
                   |
                 domain (unchanged)
```

Deliberately **not hexagonal**. The point is to reach that complexity later, having felt why —
not to start there because it is the fashionable answer.

## The API

Enough endpoints to exercise selling, and no more:

| Method | Path | Purpose |
| ------ | ---- | ------- |
| `POST` | `/api/portfolios` | Create a portfolio |
| `GET` | `/api/portfolios/{id}` | Fetch one, with its holdings |
| `POST` | `/api/portfolios/{id}/purchases` | Buy shares — sets up the state a sale needs |
| `POST` | `/api/portfolios/{id}/sales` | **Sell shares.** The endpoint the whole kata exists for |

No authentication. No pagination. No filtering. Adding them is a different exercise.

## Where the existing specifications finally get tested

Two things written down in Exercise 2 are currently only documentation. Exercise 3 turns both
into running, testable behaviour — that is the main reason to do it.

**The error contract.** [`error-contract.md`](../exercise-2-specification-driven/spec/error-contract.md)
maps each domain exception to an HTTP status. Nothing enforces that today. Here it becomes an
exception handler with tests:

| Exception | Status |
| --------- | ------ |
| `InvalidQuantityException`, `InvalidAmountException`, `InvalidTickerException` | 400 |
| `HoldingNotFoundException` | 404 |
| `ConflictQuantityException` | 409 |

Plus `PortfolioNotFoundException` → 404, which a domain-only kata had no place to throw.

**The schema.** [`schema.sql`](../exercise-2-specification-driven/spec/schema.sql) already
defines the three tables and runs on SQLite and PostgreSQL. Exercise 3 decides whether JPA
generates the schema or the schema is authoritative and JPA must match it.

## Specifications to write before any code

The same discipline as Exercise 2: the behaviour goes in files first.

1. **`api-contract.md`** — endpoints, request and response bodies, status codes, and HTTP-level
   acceptance criteria in the AC-01 style.
2. **`persistence-mapping.md`** — how `Money`, `Price`, `Ticker` and `ShareQuantity` become
   columns; how a `Portfolio` and its lots are loaded and saved.
3. **ADRs** — one per technology decision, each recording what was chosen, what was rejected,
   and why. Spring Boot, JPA, three-layer over hexagonal, the database, no authentication.

## Open questions to settle first

Decide these in the specifications, not mid-implementation:

- **Do JPA annotations go on the domain classes, or are there separate entity classes with
  mapping code between them?** The first is fast and couples the domain to a framework. The
  second keeps the domain pure and costs a translation layer. This is the central decision of
  the exercise.
- **Where does the transaction boundary sit?** AC-16 says a rejected sale changes nothing. Today
  that comes from validating before mutating; with a database it also needs a transaction.
- **What happens when two sales of the same holding run concurrently?** Nothing in the domain
  or the schema prevents both from reading 15 shares and each selling 10. Optimistic locking
  with a version column is the usual answer, and it needs its own acceptance criteria.
- **Is the whole aggregate loaded on every sale?** A holding with thousands of lots makes that
  choice expensive.

## What this exercise actually tests

Not whether Spring works. It tests whether a domain that was specified carefully survives contact
with infrastructure **without being edited**.

If it does, that is the argument for Exercise 2 made concrete. If it does not, the place where it
broke is worth more than the exercise.
