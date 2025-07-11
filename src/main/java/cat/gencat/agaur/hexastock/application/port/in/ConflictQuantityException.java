package cat.gencat.agaur.hexastock.application.port.in;

public class ConflictQuantityException extends RuntimeException {

    public ConflictQuantityException(String message) {
        super(message);
    }
}