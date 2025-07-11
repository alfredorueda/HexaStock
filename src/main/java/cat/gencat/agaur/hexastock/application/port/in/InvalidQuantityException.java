package cat.gencat.agaur.hexastock.application.port.in;

/**
 * Exception thrown when a requested user cannot be found
 */
public class InvalidQuantityException extends RuntimeException {

    public InvalidQuantityException(String message) {
        super(message);
    }
}