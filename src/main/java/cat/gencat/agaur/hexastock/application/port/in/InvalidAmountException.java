package cat.gencat.agaur.hexastock.application.port.in;

/**
 * Exception thrown when a requested user cannot be found
 */
public class InvalidAmountException extends RuntimeException {

    public InvalidAmountException(String message) {
        super(message);
    }
}