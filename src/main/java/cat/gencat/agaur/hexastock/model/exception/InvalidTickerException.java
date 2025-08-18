package cat.gencat.agaur.hexastock.model.exception;

/**
 * Thrown when a ticker symbol is invalid or not recognized by the system.
 */
public class InvalidTickerException extends DomainException {
    public InvalidTickerException(String message) {
        super(message);
    }
}
