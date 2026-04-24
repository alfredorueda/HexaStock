package cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.SharedMongoDBContainer;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.config.MongoPersistenceConfig;
import cat.gencat.agaur.hexastock.portfolios.adapter.out.persistence.mongodb.springdatarepository.MongoPortfolioSpringDataRepository;
import com.mongodb.MongoException;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import({MongoPortfolioRepository.class, MongoPersistenceConfig.class})
@ActiveProfiles("mongodb")
@DisplayName("Mongo-specific - optimistic locking (stale write detection)")
class MongoOptimisticLockingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 6, 15, 10, 0);

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> SharedMongoDBContainer.INSTANCE.getReplicaSetUrl("testdb"));
    }

    @Autowired
    private MongoPortfolioRepository repository;

    @Autowired
    private MongoPortfolioSpringDataRepository springDataRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanCollections() {
        springDataRepository.deleteAll();
    }

    @Test
    @DisplayName("concurrent stale saves on same portfolio trigger optimistic lock conflict")
    void concurrentStaleSave_detectsConflict() throws Exception {
        PortfolioId portfolioId = PortfolioId.of("p-lock");
        repository.createPortfolio(new Portfolio(portfolioId, "LockTest", Money.of(1000), NOW));

        CyclicBarrier readBarrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var firstFuture = executor.submit(worker(portfolioId, Money.of(500), readBarrier));
            var secondFuture = executor.submit(worker(portfolioId, Money.of(200), readBarrier));

            var firstResult = firstFuture.get(20, TimeUnit.SECONDS);
            var secondResult = secondFuture.get(20, TimeUnit.SECONDS);

            long successCount = Stream.of(firstResult, secondResult).filter(Objects::isNull).count();
            assertThat(successCount).isEqualTo(1);

            Throwable failure = firstResult != null ? firstResult : secondResult;
            assertThat(failure).isInstanceOfAny(OptimisticLockingFailureException.class, MongoException.class);

            Portfolio saved = repository.getPortfolioById(portfolioId).orElseThrow();
            assertThat(saved.getBalance()).isIn(Money.of(1500), Money.of(1200));
            assertThat(springDataRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Throwable> worker(PortfolioId portfolioId, Money amount, CyclicBarrier readBarrier) {
        return () -> {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            try {
                template.executeWithoutResult(status -> {
                    Portfolio portfolio = repository.getPortfolioById(portfolioId).orElseThrow();
                    portfolio.deposit(amount);
                    awaitBarrier(readBarrier);
                    repository.savePortfolio(portfolio);
                });
                return null;
            } catch (Throwable throwable) {
                return rootCause(throwable);
            }
        };
    }

    private static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to align concurrent readers", exception);
        }
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
