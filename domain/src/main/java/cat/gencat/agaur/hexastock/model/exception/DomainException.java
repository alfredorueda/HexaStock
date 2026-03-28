package cat.gencat.agaur.hexastock.model.exception;

/**
 * DomainException is the base exception class for all domain-specific exceptions.
 * 
 * In DDD terms, this represents a Domain Exception that explicitly indicates when
 * domain rules or invariants are violated. By extending from this class, we create
 * a clear separation between domain errors and technical/infrastructure errors.
 * 
 * Having a separate hierarchy for domain exceptions allows for:
 * - Clear identification of business rule violations
 * - Consistent error handling for domain issues
 * - Proper encapsulation of the domain logic and its constraints
 * 
 * All domain-specific exceptions should extend this class rather than using
 * generic exceptions to clearly communicate the domain concept that was violated.
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
