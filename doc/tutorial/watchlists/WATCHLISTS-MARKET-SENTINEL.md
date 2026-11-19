# Watchlists and Market Sentinel

**Status: proposed student extension.** `main` implements portfolios, trading, and reporting. It does not implement the watchlist classes, scheduler, endpoints, or persistence described here. Names introduced in these lessons are design proposals, not existing APIs.

Build a small price-alert system in stages. An alert reports that a user-defined condition matched an observed price; it does not execute a trade or establish that buying is advisable.

Prerequisites: [sell-stock tutorial](../sellStocks/SELL-STOCK-TUTORIAL.md), [dependency inversion](../DEPENDENCY-INVERSION-STOCK-SELLING.md), and basic SQL joins.

| Stage | Deliverable | Suggested session |
|---|---|---:|
| [1. Model watchlist commands](01-WATCHLIST-COMMANDS.md) | Aggregate rules and unit tests | 45–60 min |
| [2. Detect threshold matches](02-DETECTION-AND-PORTS.md) | A service tested through ports | 45–60 min |
| [3. Persist and verify](03-PERSISTENCE-AND-TESTS.md) | Adapter contracts and query tests | 45–60 min |
| [4. Extend alert behavior](04-PROGRESSIVE-EXTENSIONS.md) | One optional extension with explicit semantics | 45–60 min |
| [Design review](MARKET-SENTINEL-ARCHITECTURAL-REVIEW.md) | Evidence for reliability and scale decisions | Advanced |

Stages 1–3 form the required Level 1 assignment. Complete and demonstrate them before selecting an extension. These are planning estimates, not completion deadlines.

## Learning objective

Use one model to protect watchlist edits and a smaller projection to detect matching alerts. Both can share a database. Separate command and query models do not require event sourcing or separate stores; adopting them should solve a concrete modeling or query need.

The design begins with a single scheduler and console notifications. It intentionally allows repeated matches across cycles. Reliable user-facing delivery needs further decisions, developed in stage 4 and the design review.

## Attribution

The progressive alert ideas were inspired by Stock Alarm's *How to Set Stock Price Alerts*, contributed by Lluna de Gea. The engineering contracts in these lessons are teaching proposals, not recommendations for an investment strategy.

## Special Acknowledgment

This tutorial benefited from the proactive contribution of **Lluna de Gea** (LinkedIn: [https://www.linkedin.com/in/lluna-de-gea/](https://www.linkedin.com/in/lluna-de-gea/)).

Lluna worked on this project with seriousness and dedication, completing the assigned exercises with a level of commitment that went well beyond the scope set by the professor. Her engagement with the subject was genuine and sustained throughout.

Most notably, she independently identified and recommended the article [_How to Set Stock Price Alerts_](https://pro.stockalarm.io/blog/how-to-set-stock-price-alerts) published by Stock Alarm Pro. That recommendation proved directly valuable: it provided the conceptual foundation for the progressive alert-design best practices integrated into Levels 2 through 5 of this tutorial. Without her initiative, that dimension of the tutorial would not exist in its current form.

Proactive contributions of this kind — where a student brings external knowledge into the learning process and improves the shared resource for everyone — are precisely what make a course richer. They deserve to be recognised explicitly.


[Book home](../../BOOK-HOME.md)
