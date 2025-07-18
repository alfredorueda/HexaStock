package cat.gencat.agaur.hexastock.model.exception;

/**
 * ConflictQuantityException indicates that a stock operation was attempted with a quantity
 * that conflicts with available shares.
 * 
 * <p>In hexagonal architecture terms, this exception is thrown by application services when
 * a use case is invoked with a quantity that cannot be satisfied given the current state
 * of the system.</p>
 * 
 * <p>This exception is typically thrown when:</p>
 * <ul>
 *   <li>A stock sale is attempted for more shares than are owned</li>
 *   <li>A lot reduction is attempted for more shares than remain in the lot</li>
 * </ul>
 * 
 * <p>It enforces the business rule that you cannot sell or reduce more shares than you own,
 * maintaining the integrity of the portfolio's holdings.</p>
 */
public class ConflictQuantityException extends RuntimeException {

    /**
     * Constructs a new ConflictQuantityException with the specified detail message.
     * 
     * @param message The detail message explaining the quantity conflict
     */
    public ConflictQuantityException(String message) {
        super(message);
    }
}