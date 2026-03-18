package cat.gencat.agaur.hexastock.model.exception;

/**
 * Thrown when a sell operation is attempted but there are not enough
 * eligible (settled and unreserved) shares to fulfill the request.
 *
 * <p>This differs from {@link ConflictQuantityException} which checks total shares.
 * This exception specifically accounts for settlement and reservation constraints.</p>
 */
public class InsufficientEligibleSharesException extends DomainException {
    public InsufficientEligibleSharesException(String message) {
        super(message);
    }
}
