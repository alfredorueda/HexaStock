package cat.gencat.agaur.hexastock.application.port.in;

/**
 * Exception thrown when a requested user cannot be found
 */
public class PortfolioNotFoundException extends RuntimeException {

    public PortfolioNotFoundException(String portfolioId) {
        super("El portfolio no existe hulio: " + portfolioId);
    }
}