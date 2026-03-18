package cat.gencat.agaur.hexastock.model.exception;

/**
 * Thrown when a sell is attempted but there are insufficient settled, unreserved shares.
 */
public class InsufficientEligibleSharesException extends DomainException {
    public InsufficientEligibleSharesException(String message) {
        super(message);
    }
}
