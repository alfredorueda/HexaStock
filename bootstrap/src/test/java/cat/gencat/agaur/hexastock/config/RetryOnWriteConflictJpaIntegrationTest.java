package cat.gencat.agaur.hexastock.config;

import cat.gencat.agaur.hexastock.portfolios.application.port.in.CashManagementUseCase;
import cat.gencat.agaur.hexastock.portfolios.application.port.in.PortfolioLifecycleUseCase;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "jpa", "mockfinhub"})
@Import(RetryAttemptCountingTestConfig.class)
@DisplayName("RetryOnWriteConflict integration with JPA")
class RetryOnWriteConflictJpaIntegrationTest {

    @Autowired
    private PortfolioLifecycleUseCase portfolioLifecycleUseCase;

    @Autowired
    private CashManagementUseCase cashManagementUseCase;

    @Autowired
    private RetryAttemptCountingTestConfig.RetryAttemptCounter retryAttemptCounter;

    @BeforeEach
    void resetCounter() {
        retryAttemptCounter.reset();
    }

    @Test
    @DisplayName("concurrent deposits are serialized by pessimistic lock without retry attempts")
    void concurrentDeposits_doNotTriggerRetry() throws Exception {
        Portfolio created = portfolioLifecycleUseCase.createPortfolio("retry-jpa");
        PortfolioId portfolioId = created.getId();

        runConcurrentDeposits(portfolioId, Money.of(100), Money.of(100));

        assertThat(retryAttemptCounter.get()).isEqualTo(2);
        assertThat(portfolioLifecycleUseCase.getPortfolio(portfolioId).getBalance())
                .isEqualTo(Money.of(200));
    }

    private void runConcurrentDeposits(PortfolioId portfolioId, Money firstAmount, Money secondAmount) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Future<?> first = executor.submit(() -> executeDepositWhenReleased(startLatch, portfolioId, firstAmount));
            Future<?> second = executor.submit(() -> executeDepositWhenReleased(startLatch, portfolioId, secondAmount));

            startLatch.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private void executeDepositWhenReleased(CountDownLatch startLatch, PortfolioId portfolioId, Money amount) {
        awaitLatch(startLatch);
        cashManagementUseCase.deposit(portfolioId, amount);
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent start latch timeout");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent start", exception);
        }
    }
}
