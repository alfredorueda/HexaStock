package cat.gencat.agaur.hexastock.portfolios.model.transaction;

import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.*;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain unit tests for the Transaction sealed hierarchy.
 *
 * <p>Validates self-validation in each record constructor,
 * default accessor behaviour, factory methods, and exhaustive
 * switch coverage for the four subtypes.</p>
 */
@DisplayName("Transaction sealed hierarchy")
class TransactionTest {

    private static final PortfolioId PID = PortfolioId.generate();
    private static final TransactionId TID = TransactionId.generate();
    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final Ticker AAPL = Ticker.of("AAPL");

    // ── DepositTransaction ──────────────────────────────────────────────

    @Nested
    @DisplayName("DepositTransaction")
    class DepositTests {

        @Test
        @DisplayName("valid deposit has correct type and defaults")
        void validDeposit() {
            var tx = new DepositTransaction(TID, PID, Money.of("100.00"), NOW);
            assertAll(
                    () -> assertEquals(TransactionType.DEPOSIT, tx.type()),
                    () -> assertEquals(Money.of("100.00"), tx.totalAmount()),
                    () -> assertNull(tx.ticker(), "deposit has no ticker"),
                    () -> assertEquals(ShareQuantity.ZERO, tx.quantity()),
                    () -> assertNull(tx.unitPrice(), "deposit has no unit price"),
                    () -> assertEquals(Money.ZERO, tx.profit())
            );
        }

        @Test
        @DisplayName("factory method generates id and timestamp")
        void factoryMethod() {
            var tx = Transaction.createDeposit(PID, Money.of("500.00"));
            assertAll(
                    () -> assertNotNull(tx.id()),
                    () -> assertEquals(PID, tx.portfolioId()),
                    () -> assertEquals(Money.of("500.00"), tx.totalAmount()),
                    () -> assertNotNull(tx.createdAt())
            );
        }

        @Test
        @DisplayName("rejects non-positive amount")
        void rejectsZeroAmount() {
            Money zero = Money.of("0.00");
            assertThrows(IllegalArgumentException.class,
                    () -> new DepositTransaction(TID, PID, zero, NOW));
        }

        @Test
        @DisplayName("rejects negative amount")
        void rejectsNegativeAmount() {
            Money negative = Money.of("-10.00");
            assertThrows(IllegalArgumentException.class,
                    () -> new DepositTransaction(TID, PID, negative, NOW));
        }
    }

    // ── WithdrawalTransaction ───────────────────────────────────────────

    @Nested
    @DisplayName("WithdrawalTransaction")
    class WithdrawalTests {

        @Test
        @DisplayName("valid withdrawal has correct type and defaults")
        void validWithdrawal() {
            var tx = new WithdrawalTransaction(TID, PID, Money.of("50.00"), NOW);
            assertAll(
                    () -> assertEquals(TransactionType.WITHDRAWAL, tx.type()),
                    () -> assertNull(tx.ticker()),
                    () -> assertEquals(ShareQuantity.ZERO, tx.quantity())
            );
        }

        @Test
        @DisplayName("factory method generates id and timestamp")
        void factoryMethod() {
            var tx = Transaction.createWithdrawal(PID, Money.of("200.00"));
            assertNotNull(tx.id());
            assertEquals(Money.of("200.00"), tx.totalAmount());
        }

        @Test
        @DisplayName("rejects zero amount")
        void rejectsZeroAmount() {
            Money zero = Money.of("0.00");
            assertThrows(IllegalArgumentException.class,
                    () -> new WithdrawalTransaction(TID, PID, zero, NOW));
        }
    }

    // ── PurchaseTransaction ─────────────────────────────────────────────

    @Nested
    @DisplayName("PurchaseTransaction")
    class PurchaseTests {

        @Test
        @DisplayName("valid purchase carries ticker, quantity, unitPrice")
        void validPurchase() {
            var tx = new PurchaseTransaction(TID, PID, AAPL,
                    ShareQuantity.of(10), Price.of("150.00"),
                    Money.of("1500.00"), NOW);
            assertAll(
                    () -> assertEquals(TransactionType.PURCHASE, tx.type()),
                    () -> assertEquals(AAPL, tx.ticker()),
                    () -> assertEquals(ShareQuantity.of(10), tx.quantity()),
                    () -> assertEquals(Price.of("150.00"), tx.unitPrice()),
                    () -> assertEquals(Money.ZERO, tx.profit(), "purchase has no profit")
            );
        }

        @Test
        @DisplayName("factory method computes totalAmount from price × quantity")
        void factoryComputesTotalAmount() {
            var tx = Transaction.createPurchase(PID, AAPL,
                    ShareQuantity.of(5), Price.of("200.00"));
            assertEquals(Money.of("1000.00"), tx.totalAmount());
        }

        @Test
        @DisplayName("rejects zero quantity")
        void rejectsZeroQuantity() {
            Price unitPrice = Price.of("100.00");
            Money totalAmount = Money.of("0.00");
            assertThrows(IllegalArgumentException.class,
                    () -> new PurchaseTransaction(TID, PID, AAPL,
                            ShareQuantity.ZERO, unitPrice,
                            totalAmount, NOW));
        }
    }

    // ── SaleTransaction ─────────────────────────────────────────────────

    @Nested
    @DisplayName("SaleTransaction")
    class SaleTests {

        @Test
        @DisplayName("valid sale carries profit")
        void validSale() {
            var tx = new SaleTransaction(TID, PID, AAPL,
                    ShareQuantity.of(5), Price.of("200.00"),
                    Money.of("1000.00"), Money.of("250.00"), NOW);
            assertAll(
                    () -> assertEquals(TransactionType.SALE, tx.type()),
                    () -> assertEquals(Money.of("250.00"), tx.profit()),
                    () -> assertEquals(AAPL, tx.ticker())
            );
        }

        @Test
        @DisplayName("rejects zero quantity")
        void rejectsZeroQuantity() {
            Price unitPrice = Price.of("100.00");
            Money totalAmount = Money.of("0.00");
            assertThrows(IllegalArgumentException.class,
                    () -> new SaleTransaction(TID, PID, AAPL,
                            ShareQuantity.ZERO, unitPrice,
                            totalAmount, Money.ZERO, NOW));
        }
    }

    // ── Sealed hierarchy exhaustiveness ─────────────────────────────────

    @Nested
    @DisplayName("Exhaustive switch")
    class ExhaustiveSwitch {

        @Test
        @DisplayName("switch expression covers all four subtypes")
        void exhaustiveSwitch() {
            Transaction[] txs = {
                    Transaction.createDeposit(PID, Money.of("100.00")),
                    Transaction.createWithdrawal(PID, Money.of("50.00")),
                    Transaction.createPurchase(PID, AAPL, ShareQuantity.of(1), Price.of("10.00")),
                    Transaction.createSale(PID, AAPL, ShareQuantity.of(1), Price.of("15.00"),
                            Money.of("15.00"), Money.of("5.00"))
            };

            for (Transaction tx : txs) {
                String label = switch (tx) {
                    case DepositTransaction d -> "DEPOSIT";
                    case WithdrawalTransaction w -> "WITHDRAWAL";
                    case PurchaseTransaction p -> "PURCHASE";
                    case SaleTransaction s -> "SALE";
                };
                assertEquals(tx.type().name(), label);
            }
        }
    }
}
