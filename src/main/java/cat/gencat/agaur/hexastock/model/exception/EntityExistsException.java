package cat.gencat.agaur.hexastock.model.exception;

/**
 * InsufficientFundsException indicates that a financial operation cannot be completed
 * due to insufficient cash balance in the portfolio.
 * 
 * This domain exception is thrown when:
 * - A withdrawal amount exceeds the available balance
 * - A stock purchase requires more cash than is available
 * 
 * It represents a business rule that ensures financial operations maintain
 * the portfolio's cash balance integrity, preventing overdrafts.
 */
public class EntityExistsException extends DomainException {
    public EntityExistsException(String message) {
        super(message);
    }
}
