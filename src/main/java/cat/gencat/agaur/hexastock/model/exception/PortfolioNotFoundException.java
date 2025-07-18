package cat.gencat.agaur.hexastock.model.exception;

/**
 * PortfolioNotFoundException indicates that a requested portfolio could not be found.
 * 
 * <p>In hexagonal architecture terms, this exception is thrown by application services when
 * a use case is invoked with a portfolio identifier that does not exist in the system.</p>
 * 
 * <p>This exception is typically thrown when:</p>
 * <ul>
 *   <li>A portfolio operation is attempted using an invalid or non-existent portfolio ID</li>
 *   <li>A portfolio lookup fails because the requested portfolio has been deleted or never existed</li>
 * </ul>
 * 
 * <p>This exception helps maintain the integrity of operations by ensuring they are only
 * performed on existing portfolios.</p>
 */
public class PortfolioNotFoundException extends DomainException {

    /**
     * Constructs a new PortfolioNotFoundException with a message indicating which portfolio ID was not found.
     * 
     * @param portfolioId The ID of the portfolio that could not be found
     */
    public PortfolioNotFoundException(String portfolioId) {
        super("El portfolio no existe hulio: " + portfolioId);
    }
}