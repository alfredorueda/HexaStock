# What an ER diagram cannot tell you

[`schema.sql`](schema.sql) defines this domain as three tables. It is a faithful description of
the **data**. It is not a description of the **behaviour** — and US-07 is almost entirely
behaviour.

This file is the bridge between the two views. The structural mapping between them is in
[`domain-model.md`](domain-model.md); this one is about what falls through the gap.

---

## Part 1 — For students

### The test: could you rebuild US-07 from the schema alone?

Open [`schema.sql`](schema.sql) and pretend it is all you were given. You can answer:

* How many holdings can a portfolio have? *Zero or more.*
* Can two holdings in one portfolio have the same ticker? *No —* `UNIQUE (portfolio_id, ticker)`.
* Can a lot have negative remaining shares? *No —* `CHECK (remaining_shares >= 0)`.
* What happens to lots when a portfolio is deleted? *They go too —* `ON DELETE CASCADE`.

You cannot answer a single one of these:

* When I sell 12 shares, **which lots** does it take them from?
* What is `profit`, and where does it come from?
* If I ask for more shares than I own, what happens — and does anything change before it fails?
* Is `remaining_shares` allowed to drop to zero, or does the row get deleted?

Every question in the second list is US-07. None of it is in the schema.

### The six things that do not survive the translation

**1. Behaviour has no representation at all.** `sell()`, `reduce()`, `calculateCostBasis()` —
there is no column, constraint or relationship that stands for a method. A table stores; it does
not compute. The schema can say a lot never goes below zero shares; it cannot say *which* lot a
sale consumes first. FIFO is invisible.

**2. Row order is not data.** `List<Lot>` is ordered by construction, so "oldest first" is a
property the object simply has. In a table, rows come back in whatever order the engine finds
convenient. "Oldest first" is only the convention `ORDER BY purchased_at ASC`, enforced by
nothing. Forget the `ORDER BY` and the cost basis comes out wrong **silently** — no error, no
constraint violation, just a number that is quietly not 1240.00 any more.

**3. Value objects dissolve into columns.** `Money`, `Price`, `ShareQuantity` and `Ticker` stop
being things and become types plus `CHECK` constraints. Two real consequences:

* *When validation happens changes.* A value object validates once, at construction, so an
  invalid price cannot exist anywhere in the program. A `CHECK` runs on write, and only for data
  on its way into the database — an in-memory object can hold nonsense right up until it is
  saved.
* *Type safety is gone.* `Ticker` and `owner` are different types in Java; the compiler refuses
  to confuse them. In the schema both are `VARCHAR` — swap them in a query and everything runs.

**4. The aggregate root stops existing.** The class diagram says every change to a `Holding` or
`Lot` should pass through `Portfolio`. No declarative constraint expresses that: `UPDATE lot SET
remaining_shares = 0` is legal SQL that nothing will refuse.

Worth being honest about — the Java version does not enforce it either. `Holding.sell` and
`Lot.reduce` are public in the class diagram, so `portfolio.getHolding(t).sell(...)` compiles and
drains the position without ever crediting the cash balance. In both models the aggregate
boundary is a rule people follow, not a rule the machine imposes. A database can get closer than
the schema alone suggests — with triggers, or by revoking write access and exposing only stored
procedures — but neither model gets it for free.

**5. Derived results are not storable state.** `SellResult` is not a table. `proceeds`,
`costBasis` and `profit` are computed by a sale and handed back to the caller; this kata never
stores them. Persisting them would be a separate `transaction` table — out of scope for US-07.
A recurring beginner instinct is to add a `profit` column to `holding`; resist it, and ask what
would keep it correct after the next sale.

**6. "Nothing changes unless the whole sale succeeds" needs a different mechanism.** AC-16 says
a rejected sale leaves every lot and the cash balance untouched. In the domain that is free: the
code validates everything *before* it mutates anything. Move to a database and the same guarantee
now comes from wrapping the work in a transaction and rolling back. Same promise, completely
different machinery — and if you forget it, a sale can fail halfway with the oldest lot already
consumed.

### What the ER diagram is genuinely better at

This is not a list of reasons to prefer class diagrams. Each view has its blind spot:

| An ER diagram shows you plainly            | A class diagram shows you plainly              |
| ------------------------------------------ | ---------------------------------------------- |
| Cardinality and optionality, precisely      | Behaviour — what each thing can *do*            |
| Which columns are indexed, and what is unique | Encapsulation — what is reachable from where  |
| How to query across entities for reporting  | Invariants that hold across a whole aggregate   |
| Storage cost and physical shape             | Which validations are impossible to bypass      |

An ER diagram is also **language-independent**: it is readable without knowing Java, which is
exactly why it is here.

### Try it yourself

1. Write the SQL for "sell 12 shares of AAPL" against the three tables. You will need a `SELECT
   ... ORDER BY purchased_at`, a loop or a window function, several `UPDATE`s and a `DELETE`.
   Compare its length to `Holding.sell()`.
2. Now deliberately drop the `ORDER BY` and recompute the cost basis on the baseline holding
   (10 @ 100.00, then 5 @ 120.00). What do you get instead of 1240.00? Did anything complain?
3. Try to write a `CHECK` constraint that enforces "a sale consumes the oldest lot first."
   When you conclude that you cannot, articulate precisely *why* — that sentence is the whole
   point of this document.

---

## Part 2 — For instructors

### Why this file exists

Students who are fluent in SQL but not in OO tend to read the ER diagram, feel that they have
understood the domain, and then be genuinely surprised that the test suite is 36 tests long. The
schema looks like the whole story; it is perhaps a fifth of it. Naming that gap early prevents
the misreading, and it sets up the architectural argument later in the course.

### Learning objectives

By the end of this discussion a student should be able to:

1. State at least three things a schema cannot express, with a concrete example of each from
   this domain.
2. Explain why "the lots are ordered" is a guarantee in the object model and merely a convention
   in the table model.
3. Recognise that validation timing differs between a value object and a `CHECK` constraint, and
   say why that matters.
4. Defend a position on where business rules should live — and know what it costs either way.

### Discussion prompts

**"The schema has `CHECK (remaining_shares >= 0)`. Doesn't that enforce US-07's rule about not
overselling?"**
No — and the distinction is worth drawing out. The `CHECK` catches the symptom one row at a time,
*after* the program has already decided how to distribute a sale across lots, and it surfaces as
a database error rather than `ConflictQuantityException` with the message the API contract
promises. AC-12 wants the request refused before anything is touched. Listen for students
noticing that a `CHECK` failing mid-sale is exactly the partial-mutation scenario AC-16 forbids.

**"Could we put the FIFO logic in a stored procedure?"**
Yes, technically — and this is the productive argument, not a wrong answer to shut down. Ask what
it would cost: unit-testing it needs a live database, the tests stop running in milliseconds,
the rule becomes invisible to anyone reading the Java, and it is now written in a language with
different tooling. Then ask the reverse: what does a stored procedure buy? Enforcement that no
other client can bypass. That trade is real, and it is the same trade that appears at Roadmap A
step 3.

**"Why not store `profit` on the holding? It would save recomputing it."**
Ask what keeps it correct after the *next* sale, after a correction, after a bug in one code
path. Denormalisation trades a computation for an invariant somebody must maintain forever. This
usually lands better than an abstract lecture on normalisation.

**"Which diagram is right?"**
Both. They answer different questions. A student who asks this is ready for the point that a
model is a lens, not the territory — and that choosing a lens hides something by design.

### Common misconceptions to watch for

| Misconception                                          | What to say                                                                                   |
| ------------------------------------------------------- | ----------------------------------------------------------------------------------------------- |
| "The ER diagram is the simplified version"             | It is not simpler, it is *different*. It carries physical detail the class diagram omits entirely. |
| "Rows come back in insertion order"                    | Not guaranteed by any SQL standard. This one is best cured by demonstration.                    |
| "Constraints make the domain classes unnecessary"      | Constraints validate one row on write. They cannot express FIFO or an aggregate boundary.        |
| "`SellResult` is missing from the schema — that's a bug" | It is a return value, not state. The absence is correct.                                        |
| "Cascade delete is the same as composition"            | Close, and a good approximation — but composition also forbids sharing a `Lot` between holdings, which the FK alone does not say. |

### Where this stops being theoretical

At **Roadmap A step 3** (see the [README](../../../README.md)), a real repository arrives and every
one of these limitations turns into a decision someone has to make: whether the ordering lives in
the query or the code, whether `Money` maps to one column or to a `@Embeddable`, whether the
aggregate is loaded whole or lazily, and where the transaction boundary sits. Students who have
already argued about the gap make those decisions deliberately instead of by default.

A good closing exercise: hand out the ER diagram alone and ask for the acceptance criteria. Then
show them [`sell-stocks-spec.md`](sell-stocks-spec.md) and count what they missed.
