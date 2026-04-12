package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.model.portfolio.PortfolioId;
import cat.gencat.agaur.hexastock.application.exception.PortfolioNotFoundException;
import java.util.List;

/**
 * Primary port for portfolio lifecycle operations: creation, retrieval, and listing.
 *
 * <p>In hexagonal architecture terms, this is a <strong>driving port</strong> (input port)
 * that defines how the outside world can interact with the application core for
 * portfolio lifecycle management.</p>
 *
 * <p>This interface was extracted from the former {@code PortfolioManagementUseCase}
 * to satisfy the <strong>Interface Segregation Principle</strong>: clients that only
 * need portfolio CRUD operations are not forced to depend on cash management methods.</p>
 *
 * @see CashManagementUseCase
 */
public interface PortfolioLifecycleUseCase {

    /**
     * Creates a new portfolio for the specified owner.
     *
     * @param ownerName The name of the portfolio owner
     * @return The newly created Portfolio domain object
     */
    Portfolio createPortfolio(String ownerName);

    /**
     * Retrieves a portfolio by its unique identifier.
     *
     * @param id The unique identifier of the portfolio to retrieve
     * @return The requested Portfolio domain object
     * @throws PortfolioNotFoundException if the portfolio is not found
     */
    Portfolio getPortfolio(PortfolioId id);

    /**
     * Retrieves all portfolios in the system.
     *
     * @return List of all Portfolio domain objects
     */
    List<Portfolio> getAllPortfolios();
}
