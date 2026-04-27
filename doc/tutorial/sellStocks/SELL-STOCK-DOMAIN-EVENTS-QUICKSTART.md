# Sell Stocks → Domain Events — Quickstart (lectura ≤ 5 min)

> Versión condensada y esquemática del [ejercicio completo](SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md).
> Pensado para que los participantes puedan empezar a teclear sin leer 25 páginas.
> Trabajamos en **dos iteraciones**: primero la versión simple en memoria (V1), después la endurecida con persistencia del *event publication registry* (V2).

---

## 0. TL;DR

- **Hoy** [`PortfolioStockOperationsService.sellStock(...)`](../../../application/src/main/java/cat/gencat/agaur/hexastock/portfolios/application/service/PortfolioStockOperationsService.java) hace dos cosas en una transacción síncrona:
  1. `portfolio.sell(...)` + `portfolioPort.savePortfolio(...)`
  2. `Transaction.createSale(...)` + `transactionPort.save(...)`
- **Objetivo:** quitar (2) del servicio y dejar que un `@ApplicationModuleListener` lo haga *después del commit*, reaccionando a un nuevo `StockSoldEvent`.
- **Patrón guía:** mismo que `WatchlistAlertTriggeredEvent` → `WatchlistAlertNotificationListener` (ya en producción y verificado en vivo).

```
ANTES (síncrono)                     DESPUÉS (event-driven)
──────────────────                   ──────────────────────
sellStock()                          sellStock()
  ├─ portfolio.sell()                  ├─ portfolio.sell()
  ├─ portfolioPort.save()              ├─ portfolioPort.save()
  └─ transactionPort.save()  ✗         └─ publish(StockSoldEvent)  ──► [COMMIT]
                                                                          │
                                                              [task-N] ◄──┘
                                                              SaleTransactionRecordingListener
                                                                └─ transactionPort.save()
```

---

## 1. Reglas del juego (no negociables)

| Regla | Por qué |
|---|---|
| `sellStock(...)` devuelve el mismo `SellResult` y respeta los mismos invariantes. | Los tests existentes deben seguir verdes. |
| El evento es un `record` Java, sin imports de Spring/JPA. | Se publica como API del módulo `portfolios`. |
| Publica con `DomainEventPublisher` (puerto), nunca con `ApplicationEventPublisher`. | Hexagonal: el dominio no depende de Spring. |
| Listener anotado con `@ApplicationModuleListener`. | = `@TransactionalEventListener(AFTER_COMMIT) + @Async + @Transactional(REQUIRES_NEW)`. |
| Inyectar `Clock` en el servicio (no `Instant.now()`). | Tests deterministas. |
| `ModulithVerificationTest` y `HexagonalArchitectureTest` siguen verdes. | No romper la arquitectura. |

---

# 🅰️ Iteración 1 — Versión “simple”: eventos en memoria

> **Qué aporta:** desacoplamiento, separación de responsabilidades, base para nuevos consumidores.
> **Qué NO aporta:** durabilidad. Si el proceso muere entre el commit y la entrega, **el evento se pierde** y no se grabará el `Transaction`.

## 1.1 Pasos (≈ 90 minutos en pareja)

1. **Crear el evento** en
   `application/src/main/java/cat/gencat/agaur/hexastock/portfolios/events/StockSoldEvent.java`

   ```java
   public record StockSoldEvent(
           PortfolioId portfolioId,
           String ownerName,
           Ticker ticker,
           ShareQuantity quantity,
           Price salePrice,
           Money proceeds,
           Money realisedProfit,
           Instant occurredOn
   ) {
       public StockSoldEvent {
           Objects.requireNonNull(portfolioId, "portfolioId is required");
           // ... requireNonNull para todos los campos
       }
   }
   ```

2. **Exponer el paquete como API publicada** —
   `bootstrap/.../portfolios/events/package-info.java`:

   ```java
   @NamedInterface("events")
   package cat.gencat.agaur.hexastock.portfolios.events;

   import org.springframework.modulith.NamedInterface;
   ```

3. **Modificar el servicio** [`PortfolioStockOperationsService.sellStock(...)`](../../../application/src/main/java/cat/gencat/agaur/hexastock/portfolios/application/service/PortfolioStockOperationsService.java):

   ```java
   SellResult sellResult = portfolio.sell(ticker, quantity, price);
   portfolioPort.savePortfolio(portfolio);

   eventPublisher.publish(new StockSoldEvent(
           portfolioId, portfolio.getOwnerName(),
           ticker, quantity, price,
           sellResult.proceeds(), sellResult.profit(),
           clock.instant()));

   return sellResult;
   //  👉 BORRAR las dos líneas:
   //     Transaction tx = Transaction.createSale(...);
   //     transactionPort.save(tx);
   ```

4. **Crear el listener** `SaleTransactionRecordingListener` (paquete `portfolios.application.service`):

   ```java
   @Component
   public class SaleTransactionRecordingListener {
       private final TransactionPort transactionPort;
       public SaleTransactionRecordingListener(TransactionPort p) { this.transactionPort = p; }

       @ApplicationModuleListener
       public void on(StockSoldEvent e) {
           transactionPort.save(Transaction.createSale(
               e.portfolioId(), e.ticker(), e.quantity(),
               e.salePrice(), e.proceeds(), e.realisedProfit()));
       }
   }
   ```

5. **Tests obligatorios** (ver §1.3).

## 1.2 Checklist rápido

- [ ] `StockSoldEvent` es `record`, sin imports de Spring/JPA, con `requireNonNull`.
- [ ] `package-info.java` con `@NamedInterface("events")`.
- [ ] `sellStock(...)` ya **NO** llama a `transactionPort.save(...)`.
- [ ] `sellStock(...)` publica **una sola vez** vía `DomainEventPublisher`.
- [ ] Listener con `@ApplicationModuleListener`.
- [ ] `Clock` inyectado en el servicio.
- [ ] `./mvnw test` verde.

## 1.3 Tests mínimos

| # | Tipo | Qué verifica |
|---|---|---|
| 1 | Unit del listener | Stub `TransactionPort` → `on(event)` → guarda 1 `Transaction` con los campos esperados. |
| 2 | Service test (modificar el existente) | El servicio publica el evento y **NO** llama a `transactionPort.save(...)`. Usa publisher fake. |
| 3 | Integration test | Calcado de [`NotificationsEventFlowIntegrationTest`](../../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/notifications/NotificationsEventFlowIntegrationTest.java). Awaitility → exactamente 1 `Transaction` persistido tras la venta. |
| 4 | ArchUnit / verificación | Todos los tipos en `portfolios.events` son `record`. |

## 1.4 Limitación conocida (la que motiva V2)

> **Si el JVM cae entre el commit del `Portfolio` y la ejecución del listener, el `Transaction` no se escribe. Punto.**
>
> El bus de Spring Modulith es *in-process*. El *event publication registry* por defecto es **en memoria** → no sobrevive a un restart.
>
> Para auditoría / contabilidad de una venta bursátil, esto **no es aceptable en producción**. → vamos a V2.

---

# 🅱️ Iteración 2 — Versión “producción-ready”: registry persistente

> **Idea clave (gracias al participante):** *"al menos guardar el evento en la base de datos para asegurar que la transacción de la venta termina registrándose en auditoría"*.
>
> Spring Modulith ya tiene esa pieza: el **Event Publication Registry** persistente. La publicación se guarda en una tabla/colección **dentro de la misma transacción** que el commit del `Portfolio`. Si el listener falla o el proceso muere, la publicación queda marcada como *incompleta* y se reintenta al rearrancar.

## 2.1 Qué cambia respecto a V1

| Aspecto | V1 (memoria) | V2 (persistente) |
|---|---|---|
| Dependencia Maven | `spring-modulith-starter-core` | `+ spring-modulith-starter-jpa` (o `-mongodb`) |
| Tabla / colección | — | `event_publication` |
| Persistencia de la publicación | RAM | Misma TX que `savePortfolio` (atómico) |
| Reentrega tras crash | ❌ | ✅ al arrancar |
| Reentrega manual | ❌ | ✅ vía `IncompleteEventPublications` |
| Código de aplicación | — | **No cambia** (¡la magia!) |

## 2.2 Pasos (≈ 30 minutos)

1. **Añadir la dependencia** en `bootstrap/pom.xml` (perfil JPA):

   ```xml
   <dependency>
       <groupId>org.springframework.modulith</groupId>
       <artifactId>spring-modulith-starter-jpa</artifactId>
   </dependency>
   ```

   Para el perfil Mongo: `spring-modulith-starter-mongodb`.

2. **Auto-creación del schema** — añadir en `application-jpa.properties`:

   ```properties
   spring.modulith.events.jdbc.schema-initialization.enabled=true
   ```

   *(Mongo: nada extra; las colecciones se crean a demanda.)*

3. **Verificar** la nueva tabla `event_publication` tras el primer arranque:

   ```bash
   docker exec -it hexastock-mysql-1 mysql -uroot -proot hexastock -e "DESCRIBE event_publication;"
   ```

4. **Verificar el flujo de recuperación** (test manual, ≈ 2 min):

   ```bash
   # 1. Provocar una venta y matar la app antes de que el listener acabe.
   #    (Truco rápido: meter un Thread.sleep(15_000) temporal en el listener.)
   curl -X POST .../api/portfolios/{id}/sales -d '{"ticker":"AAPL","quantity":5}'
   # 2. Mientras duerme, kill -9 al proceso.
   # 3. Arrancar de nuevo. Comprobar:
   SELECT * FROM event_publication WHERE completion_date IS NULL;
   # 4. Modulith reintenta automáticamente al arrancar → la fila se completa.
   # 5. SELECT COUNT(*) FROM transactions WHERE type='SALE' AND ...;  → 1
   ```

5. **Añadir un test** que use `IncompleteEventPublications` para forzar una reentrega programática:

   ```java
   @Autowired IncompleteEventPublications incomplete;
   incomplete.resubmitIncompletePublications(p -> true);
   ```

## 2.3 Garantías que aporta V2

- **Atomicidad publicación + estado de negocio.** La fila en `event_publication` se inserta dentro de la misma TX que `savePortfolio`. No se puede commitear el `Portfolio` sin commitear la promesa de entrega del evento.
- **At-least-once.** El listener puede ejecutarse más de una vez si hay un crash entre la ejecución y el `markCompleted`. → **el listener debe ser idempotente** (ver §2.4).
- **Recuperación automática al arranque** + reentrega manual desde código.
- **Trazabilidad.** Cada publicación lleva `id`, `event_type`, `serialised_event`, `publication_date`, `completion_date`.

## 2.4 Idempotencia del listener — patrón rápido

Como el evento puede entregarse 2+ veces, hay que evitar dobles `Transaction`:

```java
@ApplicationModuleListener
public void on(StockSoldEvent e) {
    if (transactionPort.existsByEventId(e.eventId())) return;   // dedupe
    transactionPort.save(Transaction.createSale(..., e.eventId()));
}
```

→ Para que esto funcione, el evento debe llevar un `UUID eventId` estable (no un timestamp).

## 2.5 Checklist V2

- [ ] Dependencia `spring-modulith-starter-jpa` (o `-mongodb`) añadida.
- [ ] Tabla `event_publication` existe y se rellena en cada venta.
- [ ] Tras `kill -9` durante el listener, al arrancar la fila se completa sola.
- [ ] El listener es idempotente (dedupe por `eventId`).
- [ ] Tests V1 siguen verdes; nuevo test de reentrega también.

---

## 3. Errores típicos a evitar (V1 y V2)

| ❌ Error | ✅ Solución |
|---|---|
| Dejar `transactionPort.save(...)` en el servicio Y añadir el listener → 2 `Transaction` por venta. | Quitar la llamada del servicio a la vez que se introduce el listener. |
| Poner el evento en `cat.gencat.agaur.hexastock.portfolios` (raíz del módulo). | Sub-paquete `events/` con `@NamedInterface("events")`. |
| Usar `@EventListener` o `@TransactionalEventListener` directamente. | `@ApplicationModuleListener` (es más estricto y más correcto). |
| Llamar `Instant.now()` en el servicio. | Inyectar `Clock`. |
| Olvidar `@EnableAsync` (V1 y V2). | Ya está en [`HexaStockApplication`](../../../bootstrap/src/main/java/cat/gencat/agaur/hexastock/HexaStockApplication.java). |
| En V2: listener no idempotente → dobles `Transaction` tras una reentrega. | Dedupe por `eventId`. |

---

## 4. Stretch (si sobra tiempo)

Publicar también un `LotSoldEvent` por **cada lote consumido** bajo FIFO (1 venta → N eventos de lote, además del único `StockSoldEvent` agregado).
Diseño completo en [doc/consultancy/05-DOMAIN-EVENTS-ROADMAP.md §5.2](../../consultancy/05-DOMAIN-EVENTS-ROADMAP.md).

---

## 5. Referencias rápidas

- 📘 Versión larga del ejercicio: [SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md](SELL-STOCK-DOMAIN-EVENTS-EXERCISE.md)
- 🧭 Roadmap de eventos: [05-DOMAIN-EVENTS-ROADMAP.md](../../consultancy/05-DOMAIN-EVENTS-ROADMAP.md)
- 🔬 Deep dive (Watchlists): [04-DOMAIN-EVENTS-DEEP-DIVE.md](../../consultancy/04-DOMAIN-EVENTS-DEEP-DIVE.md)
- 🧪 Test plantilla: [`NotificationsEventFlowIntegrationTest`](../../../bootstrap/src/test/java/cat/gencat/agaur/hexastock/notifications/NotificationsEventFlowIntegrationTest.java)
- 🧹 Cheatsheet: [CHEATSHEET.md](../../consultancy/CHEATSHEET.md)
- 🛰️ Demo en vivo (referencia): [DEMO-WATCHLIST-NOTIFICATION-FLOW.md](../../consultancy/monday-session/DEMO-WATCHLIST-NOTIFICATION-FLOW.md)
- 📚 Spring Modulith — Event Publication Registry: <https://docs.spring.io/spring-modulith/reference/events.html#publication-registry>
