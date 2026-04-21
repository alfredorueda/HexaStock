package cat.gencat.agaur.hexastock.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only infrastructure to count how many times @RetryOnWriteConflict methods are executed.
 */
@TestConfiguration
class RetryAttemptCountingTestConfig {

    @Bean
    RetryAttemptCounter retryAttemptCounter() {
        return new RetryAttemptCounter();
    }

    @Bean
    RetryAttemptCountingAspect retryAttemptCountingAspect(RetryAttemptCounter counter) {
        return new RetryAttemptCountingAspect(counter);
    }

    static final class RetryAttemptCounter {

        private final AtomicInteger attempts = new AtomicInteger();

        void reset() {
            attempts.set(0);
        }

        void increment() {
            attempts.incrementAndGet();
        }

        int get() {
            return attempts.get();
        }
    }

    @Aspect
    @Order(Ordered.HIGHEST_PRECEDENCE + 10)
    static final class RetryAttemptCountingAspect {

        private final RetryAttemptCounter counter;

        RetryAttemptCountingAspect(RetryAttemptCounter counter) {
            this.counter = counter;
        }

        @Around("@annotation(cat.gencat.agaur.hexastock.application.annotation.RetryOnWriteConflict)")
        Object countAttempts(ProceedingJoinPoint joinPoint) throws Throwable {
            counter.increment();
            return joinPoint.proceed();
        }
    }
}
