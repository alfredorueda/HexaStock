# Sell Stocks — Domain Kata (US-07)

A standalone training module for selling stocks with FIFO lot consumption. It contains four
exercises, their specifications and instructor material, plus the worked domain solution for
Exercise 2.

## Run the Exercise 2 reference solution

```bash
cd sell-stocks-domain-kata/docs/exercise-2-specification-driven/reference-solution
mvn test
```

The reference solution uses Java 21, Maven, and **JUnit 5 as the only dependency** (test scope).
It is not part of the HexaStock reactor: it has no parent POM and no module entry elsewhere, so it
builds entirely on its own.

## What this step is (and is not)

The Exercise 2 reference solution implements the **domain and nothing else**. There is no REST API, no Spring, no
database, no DTOs, no controllers, no logging framework — just plain Java classes and tests.
The point is to get the *domain* right first, and to be able to prove it with tests that run
in milliseconds and need no infrastructure to start.

Because there is no HTTP layer, the specification's failure outcomes are expressed as
**domain exceptions** rather than status codes:

| Failure                                | Exception                   | (status a later API layer would return) |
| -------------------------------------- | --------------------------- | --------------------------------------- |
| Quantity is zero or negative           | `InvalidQuantityException`  | 400 Bad Request                         |
| Selling more shares than are held      | `ConflictQuantityException` | 409 Conflict                            |
| Ticker is not held by the portfolio    | `HoldingNotFoundException`  | 404 Not Found                           |
| Ticker is not 1–5 uppercase letters    | `InvalidTickerException`    | 400 Bad Request                         |
| Sale price is not positive             | `InvalidAmountException`    | 400 Bad Request                         |

All monetary values are `BigDecimal` at scale 2, `HALF_UP` — never `double` or `float`.

## The specifications this was built from

They all live inside this project, so it depends on no other module and no file elsewhere in
the workspace. One topic per file:

* [`spec/sell-stocks-spec.md`](docs/exercise-2-specification-driven/spec/sell-stocks-spec.md) — the **behaviour**:
  preconditions, the FIFO rule, the money definitions, and the 24 acceptance criteria
  (AC-01 … AC-24) that each map to exactly one test. **Start here.**
* [`spec/domain-model.md`](docs/exercise-2-specification-driven/spec/domain-model.md) — the **structure** in prose:
  entities, value objects, and what validates what.
* [`spec/domain-class-diagram.puml`](docs/exercise-2-specification-driven/spec/domain-class-diagram.puml) — the same
  structure as a class diagram.
* [`spec/schema.sql`](docs/exercise-2-specification-driven/spec/schema.sql) — the same model as **tables**: runnable DDL
  with the constraints made real, for anyone more at home with SQL than with objects.
* [`spec/domain-diagrams.md`](docs/exercise-2-specification-driven/spec/domain-diagrams.md) — both diagrams in **Mermaid**,
  which renders inline on GitHub and in most IDEs, with rendered PNGs in `docs/exercise-2-specification-driven/spec/png/`.
* [`spec/er-model-limitations.md`](docs/exercise-2-specification-driven/spec/er-model-limitations.md) — what the ER view
  cannot express, and why. Written for students, with a discussion guide for instructors.
* [`spec/error-contract.md`](docs/exercise-2-specification-driven/spec/error-contract.md) — which **exception** each
  failure raises, and with what message.

## Building it yourself

The [`reference-solution/`](docs/exercise-2-specification-driven/reference-solution/) directory is
the worked result of **Exercise 2**. See [`docs/`](docs/README.md) for the complete
four-exercise sequence. Exercise 1 builds the same domain from a prompt alone; Exercise 2 builds it
from specifications; Exercise 3 repeats Exercise 2 with versioned skills and specialised agents;
and Exercise 4 adds REST and persistence. See
[`docs/exercise-2-specification-driven/README.md`](docs/exercise-2-specification-driven/README.md)
for the prompt that regenerates that solution from its two specifications, what to check in the
result, and what to try next. That prompt is about thirty lines — the behaviour it would
otherwise have to spell out lives in the specifications instead.

## The FIFO rule

A sale consumes lots **oldest first**:

1. Start with the oldest lot.
2. Take `min(lot remaining shares, shares still to sell)`.
3. Reduce that lot by the amount taken.
4. Add `sharesTakenFromLot × lotPurchasePrice` to the cost basis.
5. If the lot reaches zero, it is removed.
6. Continue with the next-oldest lot until the requested quantity is filled.

Money definitions:

* `proceeds  = quantitySold × salePrice`
* `costBasis = Σ (sharesFromLotᵢ × purchasePriceᵢ)` — following FIFO
* `profit    = proceeds − costBasis` — negative is a valid result, it is a realized loss

The whole sale is validated **before** any lot is touched, so a rejected sale leaves the
holding and the cash balance exactly as they were.

## Worked numbers

Baseline: AAPL held as two lots in purchase order, **10 @ 100.00** then **5 @ 120.00**
(15 shares), sale price **150.00**, starting cash balance 0.00.

| Sell qty | Sale price | Lots consumed (FIFO)         | Proceeds | Cost basis | Profit  | Lots remaining              |
| -------- | ---------- | ---------------------------- | -------- | ---------- | ------- | --------------------------- |
| 1        | 150.00     | 1 @ 100.00                   | 150.00   | 100.00     | 50.00   | 9 @ 100.00, 5 @ 120.00      |
| 8        | 150.00     | 8 @ 100.00                   | 1200.00  | 800.00     | 400.00  | 2 @ 100.00, 5 @ 120.00      |
| 10       | 150.00     | 10 @ 100.00                  | 1500.00  | 1000.00    | 500.00  | 5 @ 120.00 (first lot gone) |
| 12       | 150.00     | 10 @ 100.00, then 2 @ 120.00 | 1800.00  | 1240.00    | 560.00  | 3 @ 120.00 (first lot gone) |
| 15       | 150.00     | 10 @ 100.00, then 5 @ 120.00 | 2250.00  | 1600.00    | 650.00  | none — the holding is removed |
| 8        | 90.00      | 8 @ 100.00                   | 720.00   | 800.00     | −80.00  | 2 @ 100.00, 5 @ 120.00      |

Proceeds are credited to the portfolio's cash balance: after selling 8 at 150.00 the balance
goes from 0.00 to 1200.00.

## Two rules worth knowing

* **A ticker is 1–5 uppercase letters**, validated by the `Ticker` value object and rejected
  with `InvalidTickerException`. Validation happens at construction, so a malformed ticker
  cannot exist anywhere in the program (AC-20, AC-21).
* **A position sold down to zero disappears.** When a sale consumes the last lot, the holding
  is removed from the portfolio: a later sale of that ticker is `HoldingNotFoundException`, and
  buying it again starts a fresh holding (AC-22, AC-23). The portfolio holds a ticker exactly
  when it owns at least one share of it.

## Roadmap (documented only — not built here)

### Roadmap A — Stack growth

The same domain, unchanged, gets progressively more stack around it:

1. **Plain-Java CLI** — a `main` that reads commands from the console and calls the domain
   directly. Proves the domain is usable without any framework.
2. **Spring + REST API** — controllers and a service layer over the same domain classes, with
   the exception-to-status mapping in the table above.
3. **Database as the data layer** — repositories persisting portfolios, holdings and lots.

Steps 2 and 3 are planned as
[Exercise 4](docs/exercise-4-rest-and-persistence/README.md).

A simple **3-layer** architecture (presentation → service → data), *not* hexagonal. The
hexagonal version is what the surrounding HexaStock workspace already demonstrates; the point
here is to arrive at that complexity deliberately, by feeling the limits of the simpler shape
first.

### Roadmap B — From prompting to specification-driven development

Right now the quality of this project sits in a long prompt. That does not scale: a prompt is
written once, reviewed by nobody, and thrown away. The direction of travel is that **the
prompt gets smaller while the quality moves into versioned specifications**:

* User stories and acceptance criteria in **plain language**, versioned as files.
* Entity-relationship and **class diagrams** as the source of truth for structure.
* **ADRs** recording the chosen tech stack and the technical rules to enforce.
* Whatever other artifacts the project needs — glossaries, API contracts, test data.

The context then lives in the repository: diffable, reviewable in a pull request, and owned by
the team rather than by whoever typed the prompt.

Exercise 3 applies the same argument to the **way the assistant works**. Project instructions,
skills, specialised agent roles, plans, and verification helpers become versioned context rather
than conventions remembered by one developer. See
[`docs/exercise-3-skills-and-agents/`](docs/exercise-3-skills-and-agents/README.md).
