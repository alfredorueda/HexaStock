package cat.gencat.agaur.hexastock.application.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an application-service method that must be retried automatically when a
 * concurrent write conflict is detected during persistence.
 *
 * <p>In hexagonal architecture this annotation belongs to the <em>application</em> layer
 * because retrying a conflicting write is a business-level decision: the full unit of work
 * (read → domain logic → write) must be re-executed with fresh data, not just the
 * persistence call.</p>
 *
 * <p>The annotation carries no framework dependency.  The actual retry mechanism
 * is provided by an infrastructure-level AOP aspect (e.g.
 * {@code RetryOnWriteConflictAspect} in {@code bootstrap}).  Replacing the
 * underlying retry library only requires updating that aspect — the use cases
 * are untouched.</p>
 *
 * <p><strong>Important:</strong> the annotated method must NOT own its own
 * {@code @Transactional} proxy at the same AOP level.  The retry advisor must
 * wrap the transaction advisor so that each attempt runs inside a fresh transaction.
 * Concretely, {@code RetryOnWriteConflictAspect} must be ordered before (outer to)
 * the transaction interceptor.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RetryOnWriteConflict {

    /**
     * Maximum number of attempts (initial attempt + retries).
     * Defaults to 3.
     */
    int maxAttempts() default 3;
}
