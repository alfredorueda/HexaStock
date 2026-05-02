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
 * <p><strong>Combining with {@code @Transactional}:</strong> this annotation is
 * designed to be used together with {@code @Transactional}, but the interaction
 * between the two must be set up carefully. The intent is simple: when a write
 * conflict is detected, the entire use case — including reading the aggregate,
 * executing the domain logic and writing the result — must be re-executed from
 * scratch, and <em>each retry attempt must run inside a brand new transaction</em>.
 * Reusing the original transaction would be pointless, because once a transaction
 * has observed a conflict it is already doomed to roll back, and any subsequent
 * read inside it would still see stale data.</p>
 *
 * <p>To guarantee this behaviour, the retry interceptor must sit <em>outside</em>
 * the transactional interceptor in the AOP proxy chain. In other words, the call
 * order at runtime must be:
 * {@code retry → begin transaction → use case → commit/rollback → (on conflict) retry → begin new transaction → ...}.
 * In Spring terms, {@code RetryOnWriteConflictAspect} must have a higher precedence
 * (lower {@code @Order} value) than the transaction interceptor, so that retries
 * happen around — never inside — the transactional boundary.</p>
 *
 * <p>The opposite ordering (transaction outside, retry inside) is incorrect:
 * every retry attempt would execute within the same, already-failed transaction,
 * the rollback would never be triggered between attempts, and the conflict would
 * simply be raised again on each iteration. The retry would therefore have no
 * chance of succeeding and would only delay the eventual failure.</p>
 *
 * <p>Example of correct usage in an application service. Note that the use case
 * itself stays free of framework-specific imports beyond the transactional
 * annotation, which can be either {@code jakarta.transaction.Transactional} or
 * {@code org.springframework.transaction.annotation.Transactional} — the retry
 * aspect is agnostic to the chosen transaction manager:</p>
 * <pre>{@code
 * public class WithdrawCashUseCase {
 *
 *     @RetryOnWriteConflict(maxAttempts = 5)
 *     @Transactional
 *     public void withdraw(PortfolioId id, Money amount) {
 *         Portfolio portfolio = portfolios.getById(id);   // read
 *         portfolio.withdraw(amount);                     // domain logic
 *         portfolios.save(portfolio);                     // write — may raise a conflict
 *     }
 * }
 * }</pre>
 *
 * <p>If a write conflict is raised, the surrounding transaction is rolled back
 * and the whole method is invoked again inside a new transaction, up to
 * {@link #maxAttempts()} times.</p>
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
