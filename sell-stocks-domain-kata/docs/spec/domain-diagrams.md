# Domain diagrams (Mermaid)

The same model as [`domain-class-diagram.puml`](domain-class-diagram.puml) and
[`domain-er-diagram.puml`](domain-er-diagram.puml), written in Mermaid so it renders inline
on GitHub, in most IDE markdown previews, and anywhere else that will not run PlantUML.

Rendered images are in [`png/`](png/) if your viewer shows neither.

---

## 1. Class diagram — the object-oriented view

![Class diagram](png/domain-class-diagram.png)

```mermaid
classDiagram
    direction TB

    namespace DomainModel {
        class Portfolio {
            <<aggregate root>>
            -PortfolioId id
            -String owner
            -Money balance
            -Map~Ticker,Holding~ holdings
            -LocalDateTime createdAt
            +sell(Ticker, ShareQuantity, Price) SellResult
            +buy(Ticker, ShareQuantity, Price) void
            +getHolding(Ticker) Holding
            +getBalance() Money
        }

        class Holding {
            <<entity>>
            -HoldingId id
            -Ticker ticker
            -List~Lot~ lots
            +sell(ShareQuantity, Price) SellResult
            +buy(ShareQuantity, Price) void
            +getTotalShares() ShareQuantity
            +getLots() List~Lot~
        }

        class Lot {
            <<entity>>
            -LotId id
            -ShareQuantity initialShares
            -ShareQuantity remainingShares
            -Price unitPrice
            -LocalDateTime purchasedAt
            +reduce(ShareQuantity) void
            +calculateCostBasis(ShareQuantity) Money
            +getRemainingShares() ShareQuantity
            +isEmpty() boolean
        }
    }

    namespace ValueObjects {
        class Money {
            <<value object>>
            -BigDecimal amount
            +add(Money) Money
            +subtract(Money) Money
        }

        class Price {
            <<value object>>
            -BigDecimal amount
            +multiply(ShareQuantity) Money
        }

        class ShareQuantity {
            <<value object>>
            -int value
            +min(ShareQuantity) ShareQuantity
            +subtract(ShareQuantity) ShareQuantity
            +isPositive() boolean
            +isZero() boolean
        }

        class Ticker {
            <<value object>>
            -String symbol
        }

        class SellResult {
            <<value object>>
            -Money proceeds
            -Money costBasis
            -Money profit
        }

        class PortfolioId {
            <<value object>>
            -String value
        }

        class HoldingId {
            <<value object>>
            -String value
        }

        class LotId {
            <<value object>>
            -String value
        }
    }

    Portfolio "1" *-- "0..*" Holding : contains
    Holding "1" *-- "1..*" Lot : contains, ordered FIFO

    Portfolio ..> SellResult : returns
    Holding ..> SellResult : returns
    Price ..> Money : multiply produces

    note for Portfolio "Aggregate root. Every change to a Holding or a Lot passes through here."
    note for Holding "Lots are kept in purchase order. FIFO consumes the oldest first."
    note for SellResult "profit = proceeds - costBasis. Computed and returned, never stored."
```

> Which value object each entity uses is already visible in its field list, so those
> dependency arrows are left out — only the two relationships you cannot read off a field
> are drawn: what a sale *returns*, and what multiplying a price *produces*.

> **Note on one relationship.** `Holding *-- "1..*" Lot` really is *one* or more. Selling an
> entire position consumes every lot, and the portfolio then drops the holding altogether — so
> a holding you can reach from a portfolio always has at least one lot. See
> [`sell-stocks-spec.md`](sell-stocks-spec.md) §6.2.

---

## 2. Entity-relationship diagram — the same model as tables

![ER diagram](png/domain-er-diagram.png)

```mermaid
erDiagram
    PORTFOLIO ||--o{ HOLDING : "owns"
    HOLDING   ||--|{ LOT     : "was bought as"

    PORTFOLIO {
        char portfolio_id PK "CHAR(36)"
        varchar owner "VARCHAR(100), NOT NULL"
        decimal balance "DECIMAL(19,2), NOT NULL, default 0.00"
        timestamp created_at "NOT NULL"
    }

    HOLDING {
        char holding_id PK "CHAR(36)"
        char portfolio_id FK "CHAR(36), NOT NULL"
        varchar ticker "VARCHAR(5), NOT NULL"
        constraint unique_ticker_per_portfolio "UNIQUE (portfolio_id, ticker)"
    }

    LOT {
        char lot_id PK "CHAR(36)"
        char holding_id FK "CHAR(36), NOT NULL"
        int initial_shares "CHECK (> 0)"
        int remaining_shares "CHECK (>= 0 AND <= initial_shares)"
        decimal unit_price "DECIMAL(19,2), CHECK (> 0)"
        timestamp purchased_at "NOT NULL, indexed with holding_id"
    }
```

What this view cannot show — FIFO ordering, the money formulas, the aggregate boundary, and
the all-or-nothing guarantee of a failed sale — is written up in
[`er-model-limitations.md`](er-model-limitations.md). Read that before concluding the schema
tells the whole story; it is roughly a fifth of it.

---

## Regenerating the images

The PNGs in [`png/`](png/) are rendered from the fenced blocks above, which are the single
source — edit the Mermaid here, never the images.

```bash
cd sell-stocks-domain-kata
./docs/spec/render-diagrams.sh
```

The script needs Node and network access on first run (it fetches `@mermaid-js/mermaid-cli`
via `npx`). It is a convenience, not part of `mvn test`; the build never depends on it.
