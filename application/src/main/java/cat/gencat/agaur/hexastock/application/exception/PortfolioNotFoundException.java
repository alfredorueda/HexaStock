package cat.gencat.agaur.hexastock.application.exception;

/**
 * PortfolioNotFoundException indicates that a requested portfolio could not be found.
 *
 * <p>This is an <strong>application-layer</strong> exception: it is thrown by application
 * services when a use case is invoked with a portfolio identifier that does not exist
 * in the system.  The check occurs <em>before</em> the domain model is invoked, so it
 * guards an application-level precondition rather than a domain invariant.</p>
 *
 * <p>This exception is typically thrown when:</p>
 * <ul>
 *   <li>A portfolio operation is attempted using an invalid or non-existent portfolio ID</li>
 *   <li>A portfolio lookup fails because the requested portfolio has been deleted or never existed</li>
 * </ul>
 */
public class PortfolioNotFoundException extends RuntimeException {

    /**
     * Constructs a new PortfolioNotFoundException with a message indicating which portfolio ID was not found.
     *
     * @param portfolioId The ID of the portfolio that could not be found
     */
    public PortfolioNotFoundException(String portfolioId) {
        super("Portfolio not found with id: " + portfolioId);
    }
}
