package cat.gencat.agaur.hexastock.config;

import cat.gencat.agaur.hexastock.application.annotation.RetryOnWriteConflict;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RetryOnWriteConflictAspect")
class RetryOnWriteConflictAspectTest {

    private RetryProbeService probeService;

    @BeforeEach
    void setUp() {
        RetryProbeService target = new RetryProbeService();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new RetryOnWriteConflictAspect());
        probeService = factory.getProxy();
    }

    @Test
    @DisplayName("retries the full annotated method up to success")
    void retriesAnnotatedMethod() {
        String result = probeService.succeedsOnThirdAttempt();

        assertThat(result).isEqualTo("ok");
        assertThat(probeService.attempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("stops after max attempts when conflict persists")
    void stopsAfterMaxAttempts() {
        assertThatThrownBy(() -> probeService.alwaysConflicts())
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(probeService.attempts()).isEqualTo(3);
    }

    static class RetryProbeService {

        private final AtomicInteger attempts = new AtomicInteger();

        int attempts() {
            return attempts.get();
        }

        @RetryOnWriteConflict(maxAttempts = 3)
        String succeedsOnThirdAttempt() {
            if (attempts.incrementAndGet() < 3) {
                throw new OptimisticLockingFailureException("conflict");
            }
            return "ok";
        }

        @RetryOnWriteConflict(maxAttempts = 3)
        String alwaysConflicts() {
            attempts.incrementAndGet();
            throw new OptimisticLockingFailureException("conflict");
        }
    }
}
