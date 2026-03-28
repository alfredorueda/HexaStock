package cat.gencat.agaur.hexastock.model.exception;

/**
 * InvalidAmountException indicates that a financial operation was attempted with an invalid monetary amount.
 * 
 * <p>In hexagonal architecture terms, this exception is thrown by application services when
 * a use case is invoked with an invalid monetary amount (e.g., zero, negative, or otherwise invalid).</p>
 * 
 * <p>This exception is typically thrown when:</p>
 * <ul>
 *   <li>A deposit or withdrawal is attempted with a non-positive amount</li>
 *   <li>A stock purchase or sale is attempted with a non-positive price</li>
 * </ul>
 * 
 * <p>It enforces the business rule that all financial transactions must involve valid, positive amounts.</p>
 */
public class InvalidAmountException extends RuntimeException {

    /**
     * Constructs a new InvalidAmountException with the specified detail message.
     * 
     * @param message The detail message explaining why the amount is invalid
     */
    public InvalidAmountException(String message) {
        super(message);
    }
}