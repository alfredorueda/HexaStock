package cat.gencat.agaur.hexastock.config;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository.MongoPortfolioRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "mongodb", "mockfinhub"})
@Import(RetryAttemptCountingTestConfig.class)
@DisplayName("RetryOnWriteConflict integration with MongoDB")
class RetryOnWriteConflictMongoIntegrationTest {

    @Container
    static final MongoDBContainer MONGODB = new MongoDBContainer("mongo:7.0.12");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> MONGODB.getReplicaSetUrl("testdb"));
    }

    @Autowired
    private PortfolioLifecycleUseCase portfolioLifecycleUseCase;

    @Autowired
    private CashManagementUseCase cashManagementUseCase;

    @Autowired
    private RetryAttemptCountingTestConfig.RetryAttemptCounter retryAttemptCounter;

    @MockitoSpyBean
    private MongoPortfolioRepository mongoPortfolioRepository;

    @BeforeEach
    void resetCounter() {
        retryAttemptCounter.reset();
    }

    @Test
    @DisplayName("concurrent stale writes trigger retry of full use case")
    void concurrentConflict_triggersRetry() throws Exception {
        Portfolio created = portfolioLifecycleUseCase.createPortfolio("retry-mongodb");
        PortfolioId portfolioId = created.getId();

        forceStaleConcurrentReadsOnFirstAttempt(portfolioId);
        runConcurrentDeposits(portfolioId, Money.of(100), Money.of(100));

        assertThat(retryAttemptCounter.get()).isGreaterThan(2);
        assertThat(portfolioLifecycleUseCase.getPortfolio(portfolioId).getBalance())
                .isEqualTo(Money.of(200));
    }

    private void forceStaleConcurrentReadsOnFirstAttempt(PortfolioId targetPortfolioId) {
        AtomicInteger readsOnTarget = new AtomicInteger();
        CyclicBarrier firstReadsBarrier = new CyclicBarrier(2);

        doAnswer(invocation -> {
            PortfolioId requestedId = invocation.getArgument(0);

            @SuppressWarnings("unchecked")
            Optional<Portfolio> loaded = (Optional<Portfolio>) invocation.callRealMethod();

            if (targetPortfolioId.equals(requestedId) && loaded.isPresent() && readsOnTarget.incrementAndGet() <= 2) {
                awaitBarrier(firstReadsBarrier);
            }
            return loaded;
        }).when(mongoPortfolioRepository).getPortfolioById(any(PortfolioId.class));
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

    private static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to align concurrent readers", exception);
        }
    }
}
