package cat.gencat.agaur.hexastock.adapter.in;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency Integration Tests for Portfolio REST API.
 *
 * <h2>Purpose</h2>
 * These tests demonstrate why pessimistic database locking is essential for financial operations.
 * They show the difference between correct serialized behavior (with locking) and incorrect
 * concurrent behavior (without locking).
 *
 * <h2>How to Run</h2>
 * <pre>
 * # Normal test run (excludes concurrency tests):
 * ./mvnw test
 *
 * # Run ONLY concurrency tests:
 * ./mvnw test -Pconcurrency
 * </pre>
 *
 * <h2>Technical Background</h2>
 *
 * <h3>Database Row Locks (SELECT ... FOR UPDATE)</h3>
 * When a transaction executes SELECT ... FOR UPDATE, the database engine places an exclusive lock
 * on the selected rows. Other transactions attempting to read the same rows with FOR UPDATE (or
 * modify them) will block until the lock is released. The lock is released when the transaction
 * commits or rolls back.
 *
 * <h3>Why JDBC Appears "Blocked"</h3>
 * When a second transaction tries to acquire a lock held by another transaction, the database
 * cannot return results until the lock is available. The JDBC driver's executeQuery() call blocks
 * waiting for the database response. This is not a Java-level lock; the blocking happens at the
 * database level, and the JDBC connection simply waits for the response.
 *
 * <h3>@Transactional and Lock Duration</h3>
 * Spring's @Transactional annotation defines the transaction boundary. The pessimistic lock
 * acquired via @Lock(PESSIMISTIC_WRITE) is held for the entire duration of the transaction,
 * from the SELECT ... FOR UPDATE until the transaction commits or rolls back. This ensures
 * that no other transaction can read or modify the locked row during that time.
 *
 * <h3>Java 21 Virtual Threads and Blocking I/O</h3>
 * These tests use virtual threads (Executors.newVirtualThreadPerTaskExecutor()) for concurrency.
 * Virtual threads are lightweight and managed by the JVM. When a virtual thread blocks on I/O
 * (such as waiting for a JDBC response), the JVM "parks" (unmounts) it from its carrier thread,
 * freeing the carrier to run other virtual threads. This makes blocking I/O scalable: thousands
 * of virtual threads can wait for database locks without exhausting OS threads.
 *
 * <h3>Teaching Instrumentation: Conditional Sleep</h3>
 * The "test-concurrency" profile activates a 200ms sleep in the withdrawal use case, right after
 * reading the portfolio and before modifying it. This widens the race window so that concurrent
 * requests reliably read the same stale state (balance=1000) before any transaction commits.
 * Without this delay, the race window is too narrow to demonstrate reliably.
 *
 * <strong>WARNING:</strong> This sleep is teaching-only instrumentation. It must never be merged
 * into main or deployed to production. Proper concurrency control handles race conditions without
 * timing manipulation.
 *
 * <h2>Expected Behavior</h2>
 * <ul>
 *   <li>WITH pessimistic locking: The second withdrawal blocks at the database until the first
 *       commits. One succeeds (200), one fails with 409 Insufficient Funds. Final balance = 300.</li>
 *   <li>WITHOUT pessimistic locking (if removed): Both transactions read balance=1000 during the
 *       sleep window, both proceed, and the final balance becomes incorrect (-400 or corrupted).</li>
 * </ul>
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "jpa", "mockfinhub", "test-concurrency"})
@Tag("concurrency")
class PortfolioConcurrencyIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Creates a new portfolio via REST API.
     *
     * @param ownerName the name of the portfolio owner
     * @return the portfolio ID
     */
    private String createPortfolio(String ownerName) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"ownerName\": \"" + ownerName + "\"}")
                .post("/api/portfolios")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
    }

    /**
     * Deposits funds into a portfolio via REST API.
     *
     * @param portfolioId the portfolio ID
     * @param amount      the amount to deposit
     */
    private void deposit(String portfolioId, int amount) {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"amount\": " + amount + "}")
                .post("/api/portfolios/" + portfolioId + "/deposits")
                .then()
                .statusCode(200);
    }

    /**
     * Attempts a withdrawal from a portfolio via REST API.
     *
     * @param portfolioId the portfolio ID
     * @param amount      the amount to withdraw
     * @return the HTTP response (includes status code and body)
     */
    private Response withdraw(String portfolioId, int amount) {
        return RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"amount\": " + amount + "}")
                .post("/api/portfolios/" + portfolioId + "/withdrawals");
    }

    /**
     * Gets the current balance of a portfolio via REST API.
     *
     * @param portfolioId the portfolio ID
     * @return the balance as a double
     */
    private double getBalance(String portfolioId) {
        Float balance = RestAssured.given()
                .get("/api/portfolios/" + portfolioId)
                .then()
                .statusCode(200)
                .extract()
                .path("balance");
        return balance.doubleValue();
    }

    // ========================================================================
    // TEST 1: Demonstrates correct behavior WITH pessimistic locking
    // ========================================================================

    /**
     * Concurrent withdrawals with pessimistic locking should serialize correctly.
     *
     * <p>Scenario:
     * <ul>
     *   <li>Initial balance: 1000</li>
     *   <li>Two concurrent withdrawals of 700 each</li>
     *   <li>Expected: One succeeds (200), one fails (409 Insufficient Funds)</li>
     *   <li>Final balance: 300</li>
     * </ul>
     *
     * <p>How pessimistic locking works here:
     * <ol>
     *   <li>Request #1 acquires the row lock via SELECT ... FOR UPDATE</li>
     *   <li>Request #2 attempts to acquire the same lock but blocks at the database level</li>
     *   <li>Request #1 completes its withdrawal and commits, releasing the lock</li>
     *   <li>Request #2 now acquires the lock, reads the updated balance (300), and fails
     *       because 700 > 300</li>
     * </ol>
     *
     * <p>This test also captures completion order to demonstrate that serialization occurred.
     */
    @Test
    void concurrentWithdrawals_withPessimisticLock_shouldSerializeAndBeCorrect() throws Exception {
        // Arrange
        String portfolioId = createPortfolio("ConcurrencyTestUser");
        deposit(portfolioId, 1000);

        // Verify initial balance
        assertEquals(1000.0, getBalance(portfolioId), 0.01);

        // We'll track completion order to prove serialization
        List<String> completionOrder = new CopyOnWriteArrayList<>();
        CountDownLatch bothStarted = new CountDownLatch(2);
        CountDownLatch canProceed = new CountDownLatch(1);

        // Act: Fire 2 concurrent withdrawals using virtual threads
        // Virtual threads park during JDBC I/O waits, keeping carrier threads free
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Response> future1 = executor.submit(() -> {
                bothStarted.countDown();
                canProceed.await();
                Response response = withdraw(portfolioId, 700);
                completionOrder.add("request1-status:" + response.getStatusCode());
                return response;
            });

            Future<Response> future2 = executor.submit(() -> {
                bothStarted.countDown();
                canProceed.await();
                Response response = withdraw(portfolioId, 700);
                completionOrder.add("request2-status:" + response.getStatusCode());
                return response;
            });

            // Wait for both threads to be ready, then release them simultaneously
            bothStarted.await(5, TimeUnit.SECONDS);
            canProceed.countDown();

            Response response1 = future1.get(10, TimeUnit.SECONDS);
            Response response2 = future2.get(10, TimeUnit.SECONDS);

            // Assert: Exactly one should succeed (200), one should fail (409)
            int status1 = response1.getStatusCode();
            int status2 = response2.getStatusCode();

            boolean oneSucceeded = (status1 == 200 && status2 == 409) || (status1 == 409 && status2 == 200);
            assertTrue(oneSucceeded,
                    "Expected one 200 and one 409, but got status1=" + status1 + ", status2=" + status2);

            // Assert: Final balance should be exactly 300
            double finalBalance = getBalance(portfolioId);
            assertEquals(300.0, finalBalance, 0.01,
                    "Final balance should be 300 (1000 - 700), but was " + finalBalance);

            // Log completion order for visibility (demonstrates serialization)
            System.out.println("Completion order: " + completionOrder);
        }
    }

    // ========================================================================
    // TEST 2: Shows what WOULD happen without pessimistic locking
    // ========================================================================

    /**
     * This test verifies that the system behaves correctly with pessimistic locking.
     *
     * <p>TO SEE THIS FAIL (demonstrating the need for pessimistic locking):
     * <ol>
     *   <li>In JpaPortfolioRepository.getPortfolioById(), change the call from
     *       findByIdForUpdate() to findById() (bypasses @Lock(PESSIMISTIC_WRITE))</li>
     *   <li>Run this test: ./mvnw test -Pconcurrency</li>
     *   <li>The test will FAIL because both withdrawals will read balance=1000 during
     *       the 200ms sleep window and both will succeed, resulting in incorrect state</li>
     * </ol>
     *
     * <p>With pessimistic locking enabled (the default), this test PASSES because:
     * <ul>
     *   <li>The second request blocks at SELECT ... FOR UPDATE until the first commits</li>
     *   <li>After the first commits, the second reads the correct balance (300)</li>
     *   <li>The second request correctly fails with Insufficient Funds</li>
     * </ul>
     */
    @Test
    void concurrentWithdrawals_withoutPessimisticLock_shouldExposeDoubleSpending() throws Exception {
        // Arrange
        String portfolioId = createPortfolio("DoubleSpendingTestUser");
        deposit(portfolioId, 1000);

        assertEquals(1000.0, getBalance(portfolioId), 0.01);

        // Act: Fire 2 concurrent withdrawals
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch readyGate = new CountDownLatch(2);

            Future<Response> future1 = executor.submit(() -> {
                readyGate.countDown();
                startGate.await();
                return withdraw(portfolioId, 700);
            });

            Future<Response> future2 = executor.submit(() -> {
                readyGate.countDown();
                startGate.await();
                return withdraw(portfolioId, 700);
            });

            // Ensure both threads are ready before starting
            readyGate.await(5, TimeUnit.SECONDS);
            startGate.countDown();

            Response response1 = future1.get(10, TimeUnit.SECONDS);
            Response response2 = future2.get(10, TimeUnit.SECONDS);

            int status1 = response1.getStatusCode();
            int status2 = response2.getStatusCode();

            // Assert: With correct locking, exactly one succeeds and one fails
            // If locking is removed/bypassed, this assertion will fail
            boolean correctBehavior = (status1 == 200 && status2 == 409) || (status1 == 409 && status2 == 200);

            // Also check the final balance as a secondary verification
            double finalBalance = getBalance(portfolioId);

            // The test passes if we have correct serialization
            // If you remove findByIdForUpdate, both may succeed and finalBalance becomes -400
            assertTrue(correctBehavior,
                    "CONCURRENCY BUG DETECTED: Expected one 200 and one 409, but got " +
                    "status1=" + status1 + ", status2=" + status2 + ". " +
                    "Final balance=" + finalBalance + ". " +
                    "This indicates the pessimistic lock is not working correctly.");

            assertEquals(300.0, finalBalance, 0.01,
                    "CONCURRENCY BUG DETECTED: Final balance should be 300, but was " + finalBalance + ". " +
                    "This indicates double-spending occurred.");
        }
    }

    // ========================================================================
    // TEST 3: Many concurrent operations should remain consistent
    // ========================================================================

    /**
     * Many concurrent deposits and withdrawals should maintain consistency.
     *
     * <p>Scenario:
     * <ul>
     *   <li>Initial deposit: 5000</li>
     *   <li>10 concurrent deposits of 100 each (+1000 total)</li>
     *   <li>5 concurrent withdrawals of 200 each (-1000 total)</li>
     *   <li>Expected final balance: 5000 + 1000 - 1000 = 5000</li>
     * </ul>
     *
     * <p>This test ensures that pessimistic locking maintains consistency even under
     * high concurrency. Each operation correctly serializes at the database level.
     */
    @Test
    void manyConcurrentOperations_withLock_shouldRemainConsistent() throws Exception {
        // Arrange
        String portfolioId = createPortfolio("HighConcurrencyUser");
        deposit(portfolioId, 5000);

        assertEquals(5000.0, getBalance(portfolioId), 0.01);

        // Define deterministic operations
        int depositCount = 10;
        int depositAmount = 100;
        int withdrawalCount = 5;
        int withdrawalAmount = 200;

        // Expected: 5000 + (10 * 100) - (5 * 200) = 5000 + 1000 - 1000 = 5000
        double expectedFinalBalance = 5000.0 + (depositCount * depositAmount) - (withdrawalCount * withdrawalAmount);

        List<Future<Integer>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch readyGate = new CountDownLatch(depositCount + withdrawalCount);

            // Submit deposit tasks
            for (int i = 0; i < depositCount; i++) {
                futures.add(executor.submit(() -> {
                    readyGate.countDown();
                    startGate.await();
                    return RestAssured.given()
                            .contentType(ContentType.JSON)
                            .body("{\"amount\": " + depositAmount + "}")
                            .post("/api/portfolios/" + portfolioId + "/deposits")
                            .getStatusCode();
                }));
            }

            // Submit withdrawal tasks
            for (int i = 0; i < withdrawalCount; i++) {
                futures.add(executor.submit(() -> {
                    readyGate.countDown();
                    startGate.await();
                    return withdraw(portfolioId, withdrawalAmount).getStatusCode();
                }));
            }

            // Wait for all threads to be ready, then release them
            readyGate.await(10, TimeUnit.SECONDS);
            startGate.countDown();

            // Collect results
            int successfulDeposits = 0;
            int successfulWithdrawals = 0;

            for (int i = 0; i < futures.size(); i++) {
                int status = futures.get(i).get(15, TimeUnit.SECONDS);
                if (status == 200) {
                    if (i < depositCount) {
                        successfulDeposits++;
                    } else {
                        successfulWithdrawals++;
                    }
                }
            }

            // Assert: All deposits should succeed
            assertEquals(depositCount, successfulDeposits,
                    "All deposits should succeed");

            // Assert: All withdrawals should succeed (we have enough balance)
            assertEquals(withdrawalCount, successfulWithdrawals,
                    "All withdrawals should succeed (sufficient funds)");

            // Assert: Final balance should be correct
            double finalBalance = getBalance(portfolioId);
            assertEquals(expectedFinalBalance, finalBalance, 0.01,
                    "Final balance should be " + expectedFinalBalance + " but was " + finalBalance);

            // Assert: Balance should never be negative (invariant)
            assertTrue(finalBalance >= 0, "Balance should never be negative");

            System.out.println("Concurrent operations completed: " +
                    successfulDeposits + " deposits, " +
                    successfulWithdrawals + " withdrawals, " +
                    "final balance = " + finalBalance);
        }
    }

    // ========================================================================
    // TEST 4: Serialization proof with timing capture
    // ========================================================================

    /**
     * Proves that requests are serialized by capturing completion timestamps.
     *
     * <p>With pessimistic locking, the second request cannot complete until the first
     * transaction commits. This test captures completion times to demonstrate that
     * there is a measurable gap between completions (due to the 200ms instrumented sleep
     * plus transaction overhead).
     */
    @Test
    void concurrentWithdrawals_shouldShowSerializationTiming() throws Exception {
        // Arrange
        String portfolioId = createPortfolio("TimingTestUser");
        deposit(portfolioId, 1000);

        List<Long> completionTimes = new CopyOnWriteArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch startGate = new CountDownLatch(1);
            CountDownLatch readyGate = new CountDownLatch(2);

            Future<Response> future1 = executor.submit(() -> {
                readyGate.countDown();
                startGate.await();
                Response response = withdraw(portfolioId, 700);
                completionTimes.add(System.currentTimeMillis());
                return response;
            });

            Future<Response> future2 = executor.submit(() -> {
                readyGate.countDown();
                startGate.await();
                Response response = withdraw(portfolioId, 700);
                completionTimes.add(System.currentTimeMillis());
                return response;
            });

            readyGate.await(5, TimeUnit.SECONDS);
            long startTime = System.currentTimeMillis();
            startGate.countDown();

            future1.get(10, TimeUnit.SECONDS);
            future2.get(10, TimeUnit.SECONDS);

            // With serialization, the second completion should be noticeably after the first
            // because it had to wait for the lock
            assertEquals(2, completionTimes.size());

            long firstCompletion = Math.min(completionTimes.get(0), completionTimes.get(1));
            long secondCompletion = Math.max(completionTimes.get(0), completionTimes.get(1));
            long gap = secondCompletion - firstCompletion;

            System.out.println("First completion: " + (firstCompletion - startTime) + "ms after start");
            System.out.println("Second completion: " + (secondCompletion - startTime) + "ms after start");
            System.out.println("Gap between completions: " + gap + "ms");

            // The gap should be at least ~200ms due to the instrumented sleep
            // (the second request waits for the first to commit)
            // We use a lower bound to account for timing variations
            assertTrue(gap >= 100,
                    "Expected serialization gap of at least 100ms, but gap was " + gap + "ms. " +
                    "This suggests requests may not be properly serialized.");

            // Final balance check
            assertEquals(300.0, getBalance(portfolioId), 0.01);
        }
    }
}