# Exercise 2, Parts One and Two — a worked instructor demonstration

> **What this is.** A complete, end-to-end run of Exercise 2 — both Part One (build from a given
> specification) and Part Two (write your own specification, then build from it) — carried out
> the way a diligent student would, with every intermediate artefact kept and documented. It is
> **instructor material**, meant to be shown to students as a worked example, not distributed as
> the exercise itself. It does not replace or modify [`reference-solution/`](../reference-solution/),
> which remains the Part-One-only reference the exercise ships with.
>
> Everything in this folder was produced today, in one session, following the same discipline the
> exercise asks of students: specifications first, then a spec-driven prompt, then an
> independently generated implementation, then verification against the specifications — not
> against the instructor's memory of what was asked for.

## Why this exists

Exercise 2 makes a claim: once behaviour and structure live in versioned specification files,
the implementation becomes a mechanical, repeatable step — and, interestingly, **different
sessions (or different models) can independently regenerate the same behaviour with different
incidental code**, because the specification pins down *what* the system does, not *how* the
code is shaped internally.

That claim is worth demonstrating, not just asserting. This folder does three things:

1. It plays out **Part Two honestly** — writing `buy-stocks-spec.md` the way a student is asked
   to, as a model answer, so instructors have something concrete to compare student submissions
   against.
2. It adds a **BDD/Gherkin layer** on top of the specifications, as an optional extra, showing
   the direct correspondence between an acceptance criterion, a Gherkin scenario, and a JUnit
   test — the same correspondence the main exercise materials describe in prose but don't show
   side by side.
3. It generates a **second, independent implementation** of the whole domain (sell + buy) from
   the specifications alone, deliberately kept blind to the existing `reference-solution/`, so
   the two can be compared. They agree on every observable behaviour and disagree on several
   incidental design choices — which is exactly what a specification is supposed to allow.

## Folder contents

```text
instructor-demo-buy-stocks/
├── PROCESS.md                          this file
├── spec/
│   ├── buy-stocks-spec.md              Part Two model answer: the spec a student should produce
│   └── domain-class-diagram-with-buy.puml   the class diagram, extended with deposit(...)
├── gherkin/
│   ├── sell_stocks.feature             AC-01..24 from sell-stocks-spec.md, as Gherkin
│   └── buy_stocks.feature              AC-01..12 from buy-stocks-spec.md, as Gherkin
└── solution/                           an independently generated Maven project (sell + buy)
    ├── pom.xml
    ├── src/main/java/com/neueda/portfolio/domain/   17 classes: 3 entities, 8 value objects, 6 exceptions
    └── src/test/java/com/neueda/portfolio/domain/   3 test classes, 58 executions, all green
```

## Step 1 — Writing the Part Two specification

`spec/buy-stocks-spec.md` follows the same shape as the shipped `sell-stocks-spec.md`: a user
story, numbered preconditions, a prose statement of the rule, a financial-definitions table, and
an acceptance-criteria table with a starting context, an action, and an exact expected result per
row — twelve criteria, AC-01 through AC-12, numbered independently of the sell spec's AC-01–24.

The five stakeholder decisions it encodes were already settled in the student-facing exercise
materials (deposit-before-spend, `InsufficientFundsException` on overdraft, append-only lots,
reuse of the existing validation exceptions, no ghosts after full liquidation) — turning a
decision list into a spec with exact numbers and worked examples is precisely the skill Part Two
is teaching, so this file is deliberately built to the same rigour as the original, not a
shortcut version of it.

## Step 2 — Extending the class diagram

`spec/domain-class-diagram-with-buy.puml` is the original `domain-class-diagram.puml` plus
exactly three additions, all called out in a comment at the top of the file so a reviewer can
verify nothing else moved:

1. `+ deposit(amount: Money): void` on `Portfolio`.
2. A note documenting `InsufficientFundsException` and the check-before-mutate rule.
3. A note documenting that `buy(...)` always appends a new lot and never merges.

## Step 3 — The BDD layer: specification → Gherkin → test

The exercise materials mention that the acceptance-criteria table format *is* Given/When/Then in
disguise, but stop short of writing it out — by design, wiring up Cucumber is explicitly called a
step beyond the exercise. This folder takes that one step further, purely as a teaching artefact:
`gherkin/sell_stocks.feature` and `gherkin/buy_stocks.feature` translate all 36 acceptance
criteria into tagged Gherkin scenarios, one-to-one, with a shared `Background` for the baseline
holding so the scenarios read the way a business stakeholder would actually describe them.

Here is the same behaviour at all three layers, side by side, for one criterion:

**Specification (`sell-stocks-spec.md`, AC-02):**

> Sell 12 shares of AAPL at 150.00 → proceeds 1800.00, cost basis 1240.00, profit 560.00. FIFO
> took 10 shares from Lot #1 at 100.00 and then 2 shares from Lot #2 at 120.00. Lot #1 is fully
> depleted and removed.

**Gherkin (`gherkin/sell_stocks.feature`):**

```gherkin
Scenario: AC-02 - Sale consumed across multiple lots, emptied lot removed
  When Alice sells 12 shares of "AAPL" at 150.00
  Then the sale succeeds with proceeds 1800.00, cost basis 1240.00 and profit 560.00
  And the "AAPL" lots are now 3 @ 120.00
```

**JUnit (`solution/src/test/java/.../SellStocksAcceptanceTest.java`, generated independently):**

```java
@Test
@DisplayName("AC-02: sale consumed across multiple lots, emptied lot removed")
void ac02_multiLotSaleRemovesEmptiedLot() {
    Portfolio portfolio = baselinePortfolio();

    SellResult result = portfolio.sell(AAPL, qty(12), price("150.00"));

    assertMoney(result.proceeds(), "1800.00");
    assertMoney(result.costBasis(), "1240.00");
    assertMoney(result.profit(), "560.00");
    List<Lot> lots = portfolio.getHolding(AAPL).getLots();
    assertEquals(1, lots.size());
    assertLot(lots.get(0), 3, "120.00");
}
```

Three independent artefacts, three different notations, the same numbers throughout. That
agreement is the entire argument for specification-driven development — not that any one of
these documents is clever, but that they cannot quietly drift apart.

The Gherkin files are not wired to Cucumber and do not execute — they are a translation exercise,
not a second test suite. Actually running them would mean adding `cucumber-junit-platform-engine`
and step-definition classes, which is real, worthwhile follow-up work but was kept out of scope
here to match the "not part of this exercise" stance the main materials already take on tooling.

## Step 4 — Generating an independent implementation

`solution/` was produced by a **separate agent session that never saw this conversation and was
explicitly instructed not to open `reference-solution/`.** It was given exactly the four spec
files above and the same technical constraints the student-facing prompt uses (Java 21, standalone
Maven, JUnit 5 only, package `com.neueda.portfolio.domain`, `BigDecimal` throughout), and asked to
implement, test, and report back — the same shape of prompt a student sends their own assistant,
just aimed at a session with no other context to lean on.

Result: **58 test executions, 0 failures, 0 errors, `BUILD SUCCESS`.**

```text
[INFO] Results:
[INFO] Tests run: 58, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

36 acceptance criteria (24 sell + 12 buy) map to 36 test methods; parameterised tests for the
criteria that list multiple example inputs (malformed tickers, non-positive amounts, etc.) bring
the execution count to 58. Every test's `@DisplayName` carries its AC id.

## Step 5 — Comparing the two implementations

Both `reference-solution/` and `solution/` implement the sell-stocks domain from the identical
`sell-stocks-spec.md` and `domain-class-diagram.puml` (this one additionally implements Part
Two, which `reference-solution/` predates). Here is what stayed identical and what didn't:

**Identical, because the specification pins it down:**
- Every financial result for every worked example (FIFO order, cost basis, profit, cash balance).
- The full class list, every field, every method signature — the diagram allows no freedom here.
- Every exception type and every rejection scenario, including validation order (AC-15) and the
  no-partial-mutation guarantee (AC-16, AC-17).
- Package name, build tooling, dependency list — the technical constraints pin these down too.

**Different, because the specification is silent here — and correctly so:**
- Value objects are Java `record`s in `solution/`, not classes with explicit accessors.
- Test class organisation differs (`@Nested` groupings by category vs. by another shape) —
  compare `reference-solution/src/test/.../SellStocksAcceptanceTest.java` with
  `solution/src/test/.../SellStocksAcceptanceTest.java` directly.
- `FIFO` ordering is implemented as list-insertion order in `solution/` rather than a sort on
  `purchasedAt` — behaviourally identical, structurally simpler.

**One genuine specification/diagram conflict, surfaced rather than hidden:**

`sell-stocks-spec.md` AC-01 and AC-02 describe the sale result as reporting "ticker AAPL,
quantity 8" — but `domain-class-diagram.puml` draws `SellResult` with exactly three fields:
`proceeds`, `costBasis`, `profit`. The generating agent followed the instruction to prefer the
diagram for structure over the spec's prose when the two disagree, so `SellResult` stays a
three-field record and the ticker/quantity claims in the ACs are verified indirectly (through
which holding was touched and by how much). This is worth putting in front of students directly:
**the two authoritative files disagreed, and the resolution rule in the prompt decided the
outcome — nobody chose silently.** `reference-solution/` resolves the same tension the same way,
for the same reason, which is itself a small confirmation that the resolution rule is doing its
job consistently.

## How to use this with students

- Show the AC → Gherkin → JUnit triptych in **Step 3** as a concrete answer to "what does
  behaviour-driven development actually look like here?" without adding Cucumber to the course.
- Use **Step 5**'s conflict as a live example of the debrief question Exercise 2 already asks:
  *"what happens when the two specifications disagree?"* — this is a real instance, not a
  hypothetical one.
- Hand out `spec/buy-stocks-spec.md` **after** grading student submissions for Part Two, as a
  model answer to compare against — not before, for the same reason `reference-solution/` is
  withheld until Part One is assessed.
- If a pair finishes early, "run the same prompt again in a fresh session and diff the result
  against your first one" is a good extension — this folder is the worked proof that the
  exercise stays honest when you do.
