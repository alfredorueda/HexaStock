package cat.gencat.agaur.hexastock.model.exception;

/**
 * InvalidQuantityException indicates that a stock operation was attempted with an invalid quantity of shares.
 * 
 * <p>In hexagonal architecture terms, this exception is thrown by application services when
 * a use case is invoked with an invalid share quantity (e.g., zero, negative, or fractional).</p>
 * 
 * <p>This exception is typically thrown when:</p>
 * <ul>
 *   <li>A stock purchase is attempted with a non-positive quantity</li>
 *   <li>A stock sale is attempted with a non-positive quantity</li>
 * </ul>
 * 
 * <p>It enforces the business rule that all stock transactions must involve valid, positive,
 * whole-number quantities of shares.</p>
 */
public class InvalidQuantityException extends RuntimeException {

    /**
     * Constructs a new InvalidQuantityException with the specified detail message.
     * 
     * @param message The detail message explaining why the quantity is invalid
     */
    public InvalidQuantityException(String message) {
        super(message);
    }
}