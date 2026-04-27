package cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.SharedMongoDBContainer;
import cat.gencat.agaur.hexastock.marketdata.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.watchlists.adapter.out.persistence.mongodb.springdatarepository.MongoWatchlistSpringDataRepository;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.Watchlist;
import cat.gencat.agaur.hexastock.watchlists.model.watchlist.WatchlistId;
import com.mongodb.MongoException;
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

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import(MongoWatchlistRepository.class)
@ActiveProfiles("mongodb")
@DisplayName("Mongo-specific - watchlist optimistic locking (stale write detection)")
class MongoWatchlistOptimisticLockingTest {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> SharedMongoDBContainer.INSTANCE.getReplicaSetUrl("testdb"));
    }

    @Autowired
    private MongoWatchlistRepository repository;

    @Autowired
    private MongoWatchlistSpringDataRepository springDataRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanCollections() {
        springDataRepository.deleteAll();
    }

    @Test
    @DisplayName("concurrent stale saves on same watchlist trigger optimistic lock conflict")
    void concurrentStaleSave_detectsConflict() throws Exception {
        WatchlistId watchlistId = WatchlistId.of("wl-lock");
        repository.createWatchlist(Watchlist.create(watchlistId, "alice", "Tech"));

        CyclicBarrier readBarrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            var firstFuture = executor.submit(worker(watchlistId, Ticker.of("AAPL"), Money.of("150.00"), readBarrier));
            var secondFuture = executor.submit(worker(watchlistId, Ticker.of("GOOG"), Money.of("200.00"), readBarrier));

            var firstResult = firstFuture.get(20, TimeUnit.SECONDS);
            var secondResult = secondFuture.get(20, TimeUnit.SECONDS);

            long successCount = Stream.of(firstResult, secondResult).filter(Objects::isNull).count();
            assertThat(successCount).isEqualTo(1);

            Throwable failure = firstResult != null ? firstResult : secondResult;
            assertThat(failure).isInstanceOfAny(OptimisticLockingFailureException.class, MongoException.class);

            assertThat(springDataRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Throwable> worker(WatchlistId watchlistId,
                                       Ticker ticker,
                                       Money threshold,
                                       CyclicBarrier readBarrier) {
        return () -> {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            try {
                template.executeWithoutResult(status -> {
                    Watchlist watchlist = repository.getWatchlistById(watchlistId).orElseThrow();
                    watchlist.addAlert(ticker, threshold);
                    awaitBarrier(readBarrier);
                    repository.saveWatchlist(watchlist);
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