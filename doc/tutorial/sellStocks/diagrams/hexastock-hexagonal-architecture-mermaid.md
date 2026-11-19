# HexaStock — Hexagonal Architecture (Mermaid)

> **Alternative diagram** — This Mermaid version provides a simplified, browser-native view of the same hexagonal architecture described in the [PlantUML diagram](Rendered/hexastock-hexagonal-architecture.svg). The architectural style is Alistair Cockburn's Hexagonal Architecture (Ports and Adapters); the visual layout draws on the teaching clarity of [Tom Hombergs' Reflectoring diagram](https://reflectoring.io/spring-hexagonal/), emphasising clean layer separation and human readability.
>
> This is editable Mermaid source. For the book's primary, pre-rendered view, use the [architecture lesson](../SELL-STOCK-04-HTTP-AND-PORTS.md).

```mermaid
---
title: "HexaStock — Hexagonal Architecture (Sell Stock Use Case)"
---
flowchart LR
    %% ── Color scheme (matches PlantUML version) ──
    classDef driving fill:#FFF3CD,stroke:#C9971C,color:#333
    classDef svcNode fill:#DAEAF6,stroke:#4682B4,color:#333
    classDef domNode fill:#C8E6C9,stroke:#388E3C,color:#333
    classDef driven fill:#F8D7DA,stroke:#C0392B,color:#333
    classDef infraNode fill:#E9ECEF,stroke:#6C757D,color:#333

    %% ── LEFT: Driving Adapter ──
    ctrl("<b>PortfolioRestController</b><br/><i>Driving Adapter · REST</i>"):::driving

    %% ── CENTER: Application Core (the hexagon) ──
    subgraph core["⬡  Application Core"]
        direction TB

        inPort(["<b>PortfolioStockOperationsUseCase</b><br/><i>Inbound Port</i>"])

        svc("<b>PortfolioStockOperationsService</b><br/><i>Application Service · @Transactional</i>"):::svcNode

        subgraph domain["Domain Model"]
            direction LR
            portfolio("<b>Portfolio</b><br/><i>aggregate root</i>"):::domNode
            holding("<b>Holding</b><br/><i>entity</i>"):::domNode
            lot("<b>Lot</b><br/><i>entity</i>"):::domNode
            sellResult("<b>SellResult</b><br/><i>value object</i>"):::domNode
        end

        outPort1(["<b>PortfolioPort</b><br/><i>Outbound Port</i>"])
        outPort2(["<b>StockPriceProviderPort</b><br/><i>Outbound Port</i>"])
        outPort3(["<b>TransactionPort</b><br/><i>Outbound Port</i>"])
    end

    %% ── RIGHT: Driven Adapters ──
    persist("<b>JpaPortfolioRepository</b><br/><b>JpaTransactionRepository</b><br/><i>Persistence Adapters · JPA</i>"):::driven
    market("<b>FinhubStockPriceAdapter</b><br/><b>AlphaVantageStockPriceAdapter</b><br/><i>Market Adapters</i>"):::driven

    %% ── FAR RIGHT: External Systems ──
    db[("MySQL")]:::infraNode
    apis("☁ Stock Price APIs"):::infraNode

    %% ── Wiring: all dependencies point inward ──
    ctrl -- "calls" --> inPort
    svc -. "implements" .-> inPort
    svc -- "delegates to" --> portfolio
    portfolio --> holding
    holding --> lot
    holding --> sellResult
    svc -- "uses" --> outPort1
    svc -- "uses" --> outPort2
    svc -- "uses" --> outPort3
    persist -. "implements" .-> outPort1
    persist -. "implements" .-> outPort3
    market -. "implements" .-> outPort2
    persist --> db
    market --> apis

    %% ── Subgraph styling ──
    style core fill:#E8F4FD,stroke:#4682B4,stroke-width:2px,color:#333
    style domain fill:#D4EDDA,stroke:#388E3C,stroke-width:2px,color:#333
```

### Reading Guide

| Layer | Color | What it contains | Maven Module |
|---|---|---|---|
| **Driving Adapters** | Yellow | REST controllers that initiate use cases | `adapters-inbound-rest` |
| **Application Core** | Blue | Inbound ports, application services, domain model, outbound ports | `application` + `domain` |
| **Domain Model** | Green | Aggregates, entities, value objects — pure business logic | `domain` |
| **Driven Adapters** | Red | JPA persistence and external API clients | `adapters-outbound-*` |

**Arrow conventions:**

- **Solid arrows (──▶)** — runtime calls and delegation
- **Dashed arrows (- -▶)** — implementation (adapter implements port interface)

**Value Objects** used throughout the domain: `Money`, `Price`, `ShareQuantity`, `Ticker`, `PortfolioId`.

Implementation arrows point from implementations toward their interfaces. Solid arrows show selected runtime interactions, not a complete dependency graph. This view selects the JPA path; `main` also provides a MongoDB persistence adapter. The domain production code has no framework dependencies; test dependencies are separate.
