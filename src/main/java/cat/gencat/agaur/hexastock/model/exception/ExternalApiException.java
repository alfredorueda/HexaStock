package cat.gencat.agaur.hexastock.model.exception;

/**
 * Exception thrown when there is an error communicating with an external API (e.g., Finnhub).
 */
public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
    public ExternalApiException(String message) {
        super(message);
    }
}

