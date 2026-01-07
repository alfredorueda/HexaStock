# HexaStock Tutorial: Selling Stocks Use Case

## 📚 Tutorial Overview

This tutorial reverse-engineers the **SELL STOCK** use case from the HexaStock codebase to teach:
- **Hexagonal Architecture (Ports & Adapters)**
- **Domain-Driven Design (DDD)**
- **Aggregate Root Pattern**
- **FIFO Accounting Implementation**

**Critical:** Every code snippet, class name, and architectural decision is verified from the actual codebase. Nothing is invented or assumed.

---

## 📖 Main Document

**[SELL-STOCK-TUTORIAL.md](./SELL-STOCK-TUTORIAL.md)**

This is the comprehensive tutorial covering:
1. Domain context and business rules
2. REST endpoint analysis
3. Hexagonal Architecture mapping
4. Step-by-step execution trace
5. **Why application services orchestrate and aggregates protect invariants** ⭐
6. Transactionality and consistency
7. Error flows and exception handling
8. Key takeaways and exercises

---

## 📊 PlantUML Diagrams

All diagrams are in the `diagrams/` folder:

### Architecture & Flow Diagrams

1. **`sell-http-to-port.puml`**
   - Shows how HTTP request reaches the primary port
   - Demonstrates dependency inversion

2. **`sell-application-service.puml`**
   - Application service orchestration
   - Calls to secondary ports (PortfolioPort, StockPriceProviderPort, TransactionPort)

3. **`sell-domain-fifo.puml`**
   - Domain model enforcing invariants
   - FIFO algorithm implementation
   - Portfolio → Holding → Lot delegation

4. **`sell-persistence-adapter.puml`**
   - Domain model to JPA entity mapping
   - Adapter implementation pattern

### DDD Core Concept

5. **`sell-orchestrator-vs-aggregate.puml`** ⭐ **MOST IMPORTANT**
   - Shows correct pattern: Service → Aggregate Root → Entities
   - Shows anti-pattern: Service directly manipulating entities
   - Visual explanation of DDD aggregate boundaries

### Error Handling

6. **`sell-error-portfolio-not-found.puml`**
   - PortfolioNotFoundException → HTTP 404

7. **`sell-error-invalid-quantity.puml`**
   - InvalidQuantityException → HTTP 400

8. **`sell-error-sell-more-than-owned.puml`**
   - ConflictQuantityException → HTTP 409

---

## 🎯 How to Use This Tutorial

### For Students

1. **Read the tutorial first:** Start with `SELL-STOCK-TUTORIAL.md` section by section
2. **Follow the code:** Use the file paths to locate actual implementation in `src/main/java/`
3. **View the diagrams:** Use a PlantUML viewer or IDE plugin to visualize the `.puml` files
4. **Complete exercises:** See section 12 of the tutorial
5. **Compare with tests:** Check `src/test/java/` for integration and unit tests

### For Instructors

1. **Present section 6** ("Why Application Services Orchestrate...") as a standalone lecture
2. **Use `sell-orchestrator-vs-aggregate.puml`** to visually explain DDD aggregate patterns
3. **Show the anti-pattern** in the diagram to demonstrate what NOT to do
4. **Run integration tests** to show the system working end-to-end
5. **Assign exercises** from section 12 as homework

---

## 🔍 Key Learning Points

### Hexagonal Architecture

- **Ports** define contracts (interfaces)
- **Adapters** implement contracts (REST controller, JPA repository)
- **Dependency inversion:** Core depends on abstractions, not implementations
- **Testability:** Can swap adapters without changing core logic

### Domain-Driven Design

- **Aggregate Root** (Portfolio) protects invariants
- **Entities** (Holding, Lot) are controlled by the root
- **Application Services** orchestrate, **never** contain business logic
- **Domain Exceptions** represent business rule violations

### FIFO Accounting

- Implemented in `Holding.sell()` method
- Sells from oldest lots first
- Calculates cost basis from original purchase prices
- Protected by aggregate root boundaries

---

## 🛠️ Viewing Diagrams

### Option 1: VS Code
Install the "PlantUML" extension and preview `.puml` files.

### Option 2: IntelliJ IDEA
Install the "PlantUML integration" plugin.

### Option 3: Online
Copy diagram content to http://www.plantuml.com/plantuml/uml/

### Option 4: Command Line
```bash
# Install PlantUML
brew install plantuml  # macOS
# or download from https://plantuml.com/download

# Generate PNG
plantuml diagrams/sell-orchestrator-vs-aggregate.puml

# Generate SVG
plantuml -tsvg diagrams/sell-orchestrator-vs-aggregate.puml
```

---

## 📁 File Structure

```
doc/tutorial/
├── README.md (this file)
├── SELL-STOCK-TUTORIAL.md (main tutorial)
└── diagrams/
    ├── sell-http-to-port.puml
    ├── sell-application-service.puml
    ├── sell-domain-fifo.puml
    ├── sell-persistence-adapter.puml
    ├── sell-orchestrator-vs-aggregate.puml ⭐
    ├── sell-error-portfolio-not-found.puml
    ├── sell-error-invalid-quantity.puml
    └── sell-error-sell-more-than-owned.puml
```

---

## ✅ Verified Code References

All referenced code exists in:
- `src/main/java/cat/gencat/agaur/hexastock/adapter/in/PortfolioRestController.java`
- `src/main/java/cat/gencat/agaur/hexastock/application/service/PortfolioStockOperationsService.java`
- `src/main/java/cat/gencat/agaur/hexastock/application/port/in/PortfolioStockOperationsUseCase.java`
- `src/main/java/cat/gencat/agaur/hexastock/model/Portfolio.java`
- `src/main/java/cat/gencat/agaur/hexastock/model/Holding.java`
- `src/main/java/cat/gencat/agaur/hexastock/model/Lot.java`

---

**Generated by reverse-engineering the HexaStock codebase - January 2025**
