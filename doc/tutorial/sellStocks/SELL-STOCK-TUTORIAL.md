# Sell Stock Deep Dive

Follow one stock sale from its specification to a tested HTTP response. This learning path uses HexaStock's Java 21 code to explain Domain-Driven Design, Hexagonal Architecture and requirements traceability one step at a time.

## How to use this tutorial

Work through **one stage per sitting**. Each page has one goal, a small task and an answer check. Stop at the checkpoint; optional references and exercises can wait until you finish the core path. The times below include reading and a short code inspection, not environment installation.

You should be comfortable with Java classes, interfaces, collections and basic JUnit. Spring knowledge helps from Stage 4 onwards. For Stages 1–3, you need only JDK 21 and the repository's Maven wrapper; no database, Docker or market API key is needed.

This is a guided study of the existing implementation on **`main`**, not an instruction to rebuild the application from scratch. Java listings are excerpts unless explicitly described as complete classes. Follow the linked source for imports and surrounding setup. All stages reuse the same two purchase lots and the same sale price.

## Learning path

| Stage | What you will be able to do | Study time |
| --- | --- | --- |
| [1. Specify the sale](SELL-STOCK-01-BEHAVIOUR.md) | Calculate the result before reading implementation code. | 10–15 min |
| [2. Model and test the aggregate](SELL-STOCK-02-DOMAIN-TEST.md) | Express the sale as an observable domain behaviour. | 15–20 min |
| [3. Follow the FIFO algorithm](SELL-STOCK-03-FIFO.md) | Trace every lot mutation and explain the invariants. | 15–20 min |
| [4. Cross the HTTP boundary](SELL-STOCK-04-HTTP-AND-PORTS.md) | Map a request onto the ports and Maven modules. | 10–15 min |
| [5. Coordinate the use case](SELL-STOCK-05-ORCHESTRATION.md) | Separate application orchestration from domain decisions. | 10–15 min |
| [6. Save and reload the aggregate](SELL-STOCK-06-PERSISTENCE.md) | Explain mapping, identity, FIFO order and query costs. | 15–20 min |
| [7. Protect consistency](SELL-STOCK-07-CONSISTENCY.md) | Distinguish business invariants, transactions and concurrency. | 15–20 min |
| [8. Trace failures](SELL-STOCK-08-ERRORS.md) | Locate each failure and explain its HTTP response. | 10–15 min |
| [9. Verify the complete flow](SELL-STOCK-09-VERIFICATION.md) | Connect deterministic HTTP tests to the specification. | 15–20 min |

For a longer workshop, group Stages 1–3 (behaviour and domain), 4–6 (application and adapters), and 7–9 (consistency and verification). The checkpoints remain useful stopping places within each session.

## The example we keep throughout

Alice holds two AAPL lots: **10 shares bought at 100.00**, followed by **5 shares bought at 120.00**. The sale price is **150.00**. Selling 12 shares consumes the first lot and two shares from the second: proceeds **1800.00**, cost basis **1240.00**, profit **560.00**, with **3 shares remaining**.

A second scenario sells 8 shares from a fresh copy of the same starting state. The two scenarios are independent; they are never consecutive sales on one portfolio.

The model assumes USD, whole shares and no fees or taxes. This path describes the FIFO sell flow implemented in the main Java modules. Settlement-aware selling and alternative lot-selection policies in other teaching materials are extensions, not rules of this baseline flow.

The [original hexagonal architecture diagram](SELL-STOCK-04-HTTP-AND-PORTS.md#the-original-hexagonal-view) introduces the architecture in Stage 4.

## Optional practice and reference

* [Exercises](SELL-STOCK-EXERCISES.md) — choose a task after the relevant stage; coding extensions change the baseline requirements.
* [References and further reading](SELL-STOCK-REFERENCE.md) — the original bibliography, companion chapters, source map and toolchain.
* [Project overview](HEXASTOCK-PROJECT-OVERVIEW.md) — the broader system.
* [Domain-only companion](SELL-STOCK-DOMAIN-TUTORIAL.md) — an alternative, longer treatment of the domain.

The original tutorial URL remains the entry point to the series. If you arrived from an older section reference, use the stage map above: specification is in Stage 1, domain tests in Stage 2, the execution trace in Stages 3–6, transactions in Stage 7, errors in Stage 8, and integration tests in Stage 9.
