package cat.gencat.agaur.hexastock.model;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import cat.gencat.agaur.hexastock.model.exception.InsufficientEligibleSharesException;
import cat.gencat.agaur.hexastock.model.exception.InsufficientFundsException;
import cat.gencat.agaur.hexastock.model.exception.HoldingNotFoundException;
import cat.gencat.agaur.hexastock.model.exception.InvalidQuantityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Domain-level tests for the settlement-aware FIFO selling feature.
 *
 * <p>These tests verify that the rich domain model enforces all invariants
 * related to settlement, reservation, fees, and accounting consistency.
 * They run without Spring context — pure domain logic.</p>
 *
 * <p>The same test class will be copied to the anemic-domain-model branch.
 * Tests that rely on aggregate-enforced invariants will <strong>naturally fail</strong>
 * on the anemic branch, demonstrating the architectural difference.</p>
 */
@DisplayName("Settlement-Aware FIFO Selling — Domain Tests")
class SettlementAwareSellTest {

    private static final Ticker AAPL = new Ticker("AAPL");
    private static final Ticker MSFT = new Ticker("MSFT");

    private Portfolio portfolio;
    private PortfolioId portfolioId;

    /** A point in time 3 days after initial lot purchases — lots are settled. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 18, 12, 0);
    /** A point in time only 1 day after purchase — lots are NOT settled. */
    private static final LocalDateTime TOO_EARLY = NOW.minusDays(2);

    @BeforeEach
    void setUp() {
        portfolioId = PortfolioId.generate();
        portfolio = new Portfolio(portfolioId, "Alice", Money.of("50000.00"), NOW.minusDays(30));
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Helpers — create lots with controlled timestamps
    // ──────────────────────────────────────────────────────────────────────

    /** Buys shares at a specific point in time so we can control settlement. */
    private void buyAt(Ticker ticker, int qty, double price, LocalDateTime purchaseTime) {
        Holding holding = getOrCreateHolding(ticker);
        Lot lot = new Lot(
                LotId.generate(),
                ShareQuantity.of(qty), ShareQuantity.of(qty),
                Price.of(price), purchaseTime,
                purchaseTime.plusDays(Lot.SETTLEMENT_DAYS), false
        );
        holding.addLotFromPersistence(lot);
        Money cost = Price.of(price).multiply(ShareQuantity.of(qty));
        // Simulate balance deduction for the buy
        portfolio.withdraw(cost);
    }

    private Holding getOrCreateHolding(Ticker ticker) {
        try {
            return portfolio.getHolding(ticker);
        } catch (HoldingNotFoundException e) {
            Holding h = Holding.create(ticker);
            portfolio.addHolding(h);
            return h;
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  1. Settlement Gate
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Settlement Gate")
    class SettlementGateTests {

        @Test
        @DisplayName("Should sell only settled lots, rejecting unsettled ones")
        @SpecificationRef(value = "SETTLE-01", level = TestLevel.DOMAIN)
        void shouldNotSellUnsettledLots() {
            // Buy 10 shares 5 days ago (settled) and 10 shares today (unsettled)
            buyAt(AAPL, 10, 100.00, NOW.minusDays(5));
            buyAt(AAPL, 10, 110.00, NOW);  // purchased NOW → settles at NOW+2

            // At NOW, only the first lot (10 shares) is settled
            // Trying to sell 15 should fail — only 10 eligible
            assertThrows(InsufficientEligibleSharesException.class, () ->
                    portfolio.sellWithSettlement(AAPL, ShareQuantity.of(15), Price.of(120.00),
                            Money.ZERO, NOW));
        }

        @Test
        @DisplayName("Should allow selling exactly the settled quantity")
        @SpecificationRef(value = "SETTLE-02", level = TestLevel.DOMAIN)
        void shouldSellExactlySettledQuantity() {
            buyAt(AAPL, 10, 100.00, NOW.minusDays(5)); // settled
            buyAt(AAPL, 10, 110.00, NOW);               // not settled

            SellResult result = portfolio.sellWithSettlement(
                    AAPL, ShareQuantity.of(10), Price.of(120.00), Money.ZERO, NOW);

            assertEquals(Money.of("1200.00"), result.proceeds());
            assertEquals(Money.of("1000.00"), result.costBasis());
            assertEquals(Money.of("200.00"), result.profit());

            // 10 unsettled shares remain
            assertEquals(ShareQuantity.of(10),
                    portfolio.getHolding(AAPL).getTotalShares());
        }

        @Test
        @DisplayName("Should report eligible shares correctly excluding unsettled lots")
        @SpecificationRef(value = "SETTLE-03", level = TestLevel.DOMAIN)
        void shouldReportEligibleSharesCorrectly() {
            buyAt(AAPL, 10, 100.00, NOW.minusDays(5)); // settled
            buyAt(AAPL, 5, 110.00, NOW);                // not settled

            Holding holding = portfolio.getHolding(AAPL);
            assertEquals(ShareQuantity.of(15), holding.getTotalShares());
            assertEquals(ShareQuantity.of(10), holding.getEligibleShares(NOW));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  2. Reserved Lots
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Reserved Lot Gate")
    class ReservedLotTests {

        @Test
        @DisplayName("Should skip reserved lots during FIFO selling")
        @SpecificationRef(value = "RESERVE-01", level = TestLevel.DOMAIN)
        void shouldSkipReservedLots() {
            // Buy two lots: lot1 (oldest, 10 shares) and lot2 (5 shares)
            buyAt(AAPL, 10, 100.00, NOW.minusDays(10)); // settled
            buyAt(AAPL, 5, 120.00, NOW.minusDays(5));    // settled

            // Reserve the oldest lot
            Holding holding = portfolio.getHolding(AAPL);
            Lot oldestLot = holding.getLots().get(0);
            portfolio.reserveLot(AAPL, oldestLot.getId());

            // Only 5 shares eligible (lot2), so selling 8 should fail
            assertThrows(InsufficientEligibleSharesException.class, () ->
                    portfolio.sellWithSettlement(AAPL, ShareQuantity.of(8), Price.of(130.00),
                            Money.ZERO, NOW));
        }

        @Test
        @DisplayName("Should sell from non-reserved lots skipping reserved ones in FIFO order")
        @SpecificationRef(value = "RESERVE-02", level = TestLevel.DOMAIN)
        void shouldSellFromNonReservedLots() {
            buyAt(AAPL, 10, 100.00, NOW.minusDays(10)); // lot1 — will be reserved
            buyAt(AAPL, 5, 120.00, NOW.minusDays(5));    // lot2 — available

            Holding holding = portfolio.getHolding(AAPL);
            Lot lot1 = holding.getLots().get(0);
            portfolio.reserveLot(AAPL, lot1.getId());

            // Sell 5 shares — should come from lot2 (skipping lot1)
            SellResult result = portfolio.sellWithSettlement(
                    AAPL, ShareQuantity.of(5), Price.of(130.00), Money.ZERO, NOW);

            assertEquals(Money.of("650.00"), result.proceeds());
            // Cost basis from lot2: 5 × 120 = 600
            assertEquals(Money.of("600.00"), result.costBasis());
            assertEquals(Money.of("50.00"), result.profit());

            // Reserved lot1 still has 10 shares
            holding = portfolio.getHolding(AAPL);
            assertEquals(ShareQuantity.of(10), holding.getTotalShares());
            assertTrue(holding.getLots().get(0).isReserved());
        }

        @Test
        @DisplayName("Should allow unreserving a lot to make it available again")
        @SpecificationRef(value = "RESERVE-03", level = TestLevel.DOMAIN)
        void shouldAllowUnreservingLot() {
            buyAt(AAPL, 10, 100.00, NOW.minusDays(10));

            Holding holding = portfolio.getHolding(AAPL);
            Lot lot = holding.getLots().get(0);

            portfolio.reserveLot(AAPL, lot.getId());
            assertEquals(ShareQuantity.of(0), holding.getEligibleShares(NOW));

            portfolio.unreserveLot(AAPL, lot.getId());
            assertEquals(ShareQuantity.of(10), holding.getEligibleShares(NOW));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  3. Fees and Accounting
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Fees and Accounting")
    class FeeTests {

        @Test
        @DisplayName("Should deduct fee from proceeds and reflect in SellResult")
        @SpecificationRef(value = "FEE-01", level = TestLevel.DOMAIN)
        void shouldDeductFeeFromProceeds() {
            buyAt(AAPL, 10, 100.00, NOW.minusDays(5));

            Money fee = Money.of("5.00");
            SellResult result = portfolio.sellWithSettlement(
                    AAPL, ShareQuantity.of(10), Price.of(120.00), fee, NOW);

            // Gross proceeds: 10 × 120 = 1200
            assertEquals(Money.of("1200.00"), result.proceeds());
            // Net proceeds: 1200 - 5 = 1195
            assertEquals(Money.of("1195.00"), result.netProceeds());
            // Cost basis: 10 × 100 = 1000
            assertEquals(Money.of("1000.00"), result.costBasis());
            // Profit = netProceeds - costBasis = 1195 - 1000 = 195
            assertEquals(Money.of("195.00"), result.profit());
            // Fee recorded
            assertEquals(Money.of("5.00"), result.fee());
        }

        @Test
        @DisplayName("Should maintain accounting identity: costBasis + profit + fee = proceeds")
        @SpecificationRef(value = "FEE-02", level = TestLevel.DOMAIN)
        void shouldMaintainAccountingIdentity() {
            buyAt(AAPL, 20, 80.00, NOW.minusDays(5));

            Money fee = Money.of("12.00");
            SellResult result = portfolio.sellWithSettlement(
                    AAPL, ShareQuantity.of(15), Price.of(100.00), fee, NOW);

            // Accounting identity: costBasis + profit + fee = proceeds
            Money identity = result.costBasis().add(result.profit()).add(result.fee());
            assertEquals(result.proceeds(), identity,
                    "Accounting identity violated: costBasis + profit + fee must equal proceeds");
        }

        @Test
        @DisplayName("Should reject sell when fee causes negative cash")
        @SpecificationRef(value = "FEE-03", level = TestLevel.DOMAIN)
        void shouldRejectSellWhenFeeExceedsAvailableCash() {
            // Portfolio has exactly $0 after buying
            Portfolio poorPortfolio = new Portfolio(
                    PortfolioId.generate(), "Bob", Money.of("1000.00"), NOW.minusDays(30));
            Holding h = Holding.create(AAPL);
            poorPortfolio.addHolding(h);
            Lot lot = new Lot(LotId.generate(),
                    ShareQuantity.of(10), ShareQuantity.of(10),
                    Price.of(100.00), NOW.minusDays(5),
                    NOW.minusDays(3), false);
            h.addLotFromPersistence(lot);
            // Withdraw to leave balance at 0
            poorPortfolio.withdraw(Money.of("1000.00"));

            // Now sell 10 @ 1.00 each = $10 gross proceeds, with fee = $20
            // Net proceeds = 10 - 20 = -10 → balance would go to -10 → REJECT
            Money excessiveFee = Money.of("20.00");
            assertThrows(InsufficientFundsException.class, () ->
                    poorPortfolio.sellWithSettlement(AAPL, ShareQuantity.of(10),
                            Price.of("1.00"), excessiveFee, NOW));
        }

        @Test
        @DisplayName("Should correctly update cash balance with net proceeds (after fee)")
        @SpecificationRef(value = "FEE-04", level = TestLevel.DOMAIN)
        void shouldUpdateCashBalanceWithNetProceeds() {
            buyAt(AAPL, 10, 100.00, NOW.minusDays(5));
            Money balanceBefore = portfolio.getBalance();

            Money fee = Money.of("3.00");
            portfolio.sellWithSettlement(
                    AAPL, ShareQuantity.of(5), Price.of(150.00), fee, NOW);

            // Net proceeds added: (5 × 150) - 3 = 747
            Money expectedBalance = balanceBefore.add(Money.of("747.00"));
            assertEquals(expectedBalance, portfolio.getBalance());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  4. FIFO Order
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("FIFO Order with Settlement")
    class FifoTests {

        @Test
        @DisplayName("Should consume settled lots in FIFO order")
        @SpecificationRef(value = "FIFO-01", level = TestLevel.DOMAIN)
        void shouldConsumeSettledLotsInFifoOrder() {
            // Three lots, oldest to newest, all settled
            buyAt(AAPL, 5, 80.00, NOW.minusDays(10));   // lot1
            buyAt(AAPL, 5, 100.00, NOW.minusDays(8));   // lot2
            buyAt(AAPL, 5, 120.00, NOW.minusDays(6));   // lot3

            // Sell 8: should consume lot1 (5) + lot2 (3 partial)
            SellResult result = portfolio.sellWithSettlement(
                    AAPL, ShareQuantity.of(8), Price.of(150.00), Money.ZERO, NOW);

            // Cost basis: (5 × 80) + (3 × 100) = 400 + 300 = 700
            assertEquals(Money.of("700.00"), result.costBasis());
            // Proceeds: 8 × 150 = 1200
            assertEquals(Money.of("1200.00"), result.proceeds());

            // Remaining: lot2 has 2 shares, lot3 has 5 shares = 7 total
            Holding h = portfolio.getHolding(AAPL);
            assertEquals(ShareQuantity.of(7), h.getTotalShares());
        }

        @Test
        @DisplayName("Should skip unsettled lot in FIFO order and continue with next settled lot")
        @SpecificationRef(value = "FIFO-02", level = TestLevel.DOMAIN)
        void shouldSkipUnsettledLotInFifoOrder() {
            buyAt(AAPL, 5, 80.00, NOW.minusDays(10));  // lot1 — settled
            buyAt(AAPL, 5, 100.00, NOW);                // lot2 — NOT settled (bought today)
            buyAt(AAPL, 5, 120.00, NOW.minusDays(6));   // lot3 — settled

            // Sell 8: should consume lot1 (5), skip lot2 (unsettled), take lot3 (3)
            SellResult result = portfolio.sellWithSettlement(
                    AAPL, ShareQuantity.of(8), Price.of(150.00), Money.ZERO, NOW);

            // Cost basis: (5 × 80) + (3 × 120) = 400 + 360 = 760
            assertEquals(Money.of("760.00"), result.costBasis());

            // Remaining: lot2 (5 unsettled) + lot3 (2 settled) = 7 total
            assertEquals(ShareQuantity.of(7),
                    portfolio.getHolding(AAPL).getTotalShares());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  5. Partial Lot Selling
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Partial Lot Selling")
    class PartialLotTests {

        @Test
        @DisplayName("Should partially consume a lot and leave remainder intact")
        @SpecificationRef(value = "PARTIAL-01", level = TestLevel.DOMAIN)
        void shouldPartiallyConsumeLot() {
            buyAt(AAPL, 10, 100.00, NOW.minusDays(5));

            portfolio.sellWithSettlement(
                    AAPL, ShareQuantity.of(3), Price.of(130.00), Money.ZERO, NOW);

            Holding h = portfolio.getHolding(AAPL);
            assertEquals(1, h.getLots().size());
            assertEquals(ShareQuantity.of(7), h.getLots().get(0).getRemainingShares());
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  6. Cannot Sell More Than Eligible
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Eligibility Checks")
    class EligibilityTests {

        @Test
        @DisplayName("Should distinguish total shares from eligible shares")
        @SpecificationRef(value = "ELIGIBLE-01", level = TestLevel.DOMAIN)
        void shouldDistinguishTotalFromEligible() {
            buyAt(AAPL, 10, 100.00, NOW.minusDays(10)); // settled
            buyAt(AAPL, 5, 110.00, NOW);                 // unsettled

            Holding h = portfolio.getHolding(AAPL);
            assertEquals(ShareQuantity.of(15), h.getTotalShares());
            assertEquals(ShareQuantity.of(10), h.getEligibleShares(NOW));

            // Reserve the settled lot
            portfolio.reserveLot(AAPL, h.getLots().get(0).getId());
            assertEquals(ShareQuantity.of(0), h.getEligibleShares(NOW));
        }

        @Test
        @DisplayName("Should reject sell with zero quantity")
        @SpecificationRef(value = "ELIGIBLE-02", level = TestLevel.DOMAIN)
        void shouldRejectZeroQuantitySell() {
            buyAt(AAPL, 10, 100.00, NOW.minusDays(5));
            assertThrows(InvalidQuantityException.class, () ->
                    portfolio.sellWithSettlement(AAPL, ShareQuantity.ZERO, Price.of(120.00),
                            Money.ZERO, NOW));
        }

        @Test
        @DisplayName("Should reject sell for non-existent holding")
        @SpecificationRef(value = "ELIGIBLE-03", level = TestLevel.DOMAIN)
        void shouldRejectSellForNonExistentHolding() {
            assertThrows(HoldingNotFoundException.class, () ->
                    portfolio.sellWithSettlement(MSFT, ShareQuantity.of(5), Price.of(100.00),
                            Money.ZERO, NOW));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  7. Lot Settlement Model
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Lot Settlement Behavior")
    class LotSettlementTests {

        @Test
        @DisplayName("Lot should report correctly whether settled based on T+2 rule")
        @SpecificationRef(value = "LOT-SETTLE-01", level = TestLevel.DOMAIN)
        void lotShouldReportSettlementStatus() {
            LocalDateTime purchaseDate = LocalDateTime.of(2026, 3, 10, 9, 0);
            Lot lot = new Lot(LotId.generate(),
                    ShareQuantity.of(10), ShareQuantity.of(10),
                    Price.of(100.00), purchaseDate,
                    purchaseDate.plusDays(Lot.SETTLEMENT_DAYS), false);

            // Before T+2
            assertFalse(lot.isSettled(purchaseDate.plusDays(1)));
            // Exactly at T+2
            assertTrue(lot.isSettled(purchaseDate.plusDays(2)));
            // After T+2
            assertTrue(lot.isSettled(purchaseDate.plusDays(5)));
        }

        @Test
        @DisplayName("Reserved lot should not be available for sale even if settled")
        @SpecificationRef(value = "LOT-SETTLE-02", level = TestLevel.DOMAIN)
        void reservedLotShouldNotBeAvailable() {
            LocalDateTime purchaseDate = NOW.minusDays(10);
            Lot lot = new Lot(LotId.generate(),
                    ShareQuantity.of(10), ShareQuantity.of(10),
                    Price.of(100.00), purchaseDate,
                    purchaseDate.plusDays(Lot.SETTLEMENT_DAYS), false);

            assertTrue(lot.isAvailableForSale(NOW));
            lot.reserve();
            assertFalse(lot.isAvailableForSale(NOW));
            assertEquals(ShareQuantity.ZERO, lot.availableShares(NOW));
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  8. Atomic Consistency (Combined Scenario)
    // ──────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Atomic Consistency")
    class AtomicConsistencyTests {

        @Test
        @DisplayName("Full scenario: mixed settled/unsettled/reserved lots with fee — atomic correctness")
        @SpecificationRef(value = "ATOMIC-01", level = TestLevel.DOMAIN)
        void fullScenarioAtomicCorrectness() {
            // Setup:
            // Lot A: 10 @ $80, bought 10 days ago → settled
            // Lot B: 5 @ $90, bought 8 days ago → settled, will be RESERVED
            // Lot C: 3 @ $100, bought 4 days ago → settled
            // Lot D: 7 @ $110, bought today → NOT settled
            buyAt(AAPL, 10, 80.00, NOW.minusDays(10));  // A
            buyAt(AAPL, 5, 90.00, NOW.minusDays(8));    // B → reserve
            buyAt(AAPL, 3, 100.00, NOW.minusDays(4));   // C
            buyAt(AAPL, 7, 110.00, NOW);                  // D → unsettled

            // Reserve lot B
            Holding h = portfolio.getHolding(AAPL);
            Lot lotB = h.getLots().get(1);
            portfolio.reserveLot(AAPL, lotB.getId());

            // Eligible: A (10) + C (3) = 13 shares
            assertEquals(ShareQuantity.of(13), h.getEligibleShares(NOW));

            Money balanceBefore = portfolio.getBalance();
            Money fee = Money.of("10.00");

            // Sell 12 shares @ $130
            SellResult result = portfolio.sellWithSettlement(
                    AAPL, ShareQuantity.of(12), Price.of(130.00), fee, NOW);

            // FIFO: consume A (10) + skip B (reserved) + C (2 partial)
            // Cost basis: (10 × 80) + (2 × 100) = 800 + 200 = 1000
            assertEquals(Money.of("1000.00"), result.costBasis());
            // Gross proceeds: 12 × 130 = 1560
            assertEquals(Money.of("1560.00"), result.proceeds());
            // NetProceeds: 1560 - 10 = 1550
            assertEquals(Money.of("1550.00"), result.netProceeds());
            // Profit: 1550 - 1000 = 550
            assertEquals(Money.of("550.00"), result.profit());
            // Fee
            assertEquals(Money.of("10.00"), result.fee());

            // Accounting identity
            assertEquals(result.proceeds(),
                    result.costBasis().add(result.profit()).add(result.fee()));

            // Cash balance: before + netProceeds (1550)
            assertEquals(balanceBefore.add(Money.of("1550.00")), portfolio.getBalance());

            // Remaining lots:
            //   B: 5 shares (reserved, untouched)
            //   C: 1 share (partially consumed)
            //   D: 7 shares (unsettled)
            h = portfolio.getHolding(AAPL);
            assertEquals(ShareQuantity.of(13), h.getTotalShares());
            assertEquals(3, h.getLots().size());
        }
    }
}
