package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.Portfolio;
import cat.gencat.agaur.hexastock.model.PortfolioId;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * PortfolioPort defines the secondary port for portfolio persistence operations.
 * 
 * <p>In hexagonal architecture terms, this is a <strong>secondary port</strong> (output port)
 * that defines how the application core communicates with the persistence layer for
 * portfolio data. It is implemented by driven adapters that connect to actual database systems.</p>
 * 
 * <p>This interface allows the application to:</p>
 * <ul>
 *   <li>Abstract away the details of how portfolios are stored and retrieved</li>
 *   <li>Support multiple storage technologies without changing the application core</li>
 *   <li>Facilitate testing by allowing mock implementations</li>
 * </ul>
 * 
 * <p>The port handles the core persistence operations needed by the application:</p>
 * <ul>
 *   <li>Retrieving portfolios by their unique identifier</li>
 *   <li>Creating new portfolios in the persistence store</li>
 *   <li>Saving changes to existing portfolios</li>
 * </ul>
 */
public interface PortfolioPort {

    /**
     * Retrieves a portfolio by its unique identifier.
     * 
     * @param id The unique identifier of the portfolio to retrieve
     * @return The requested Portfolio domain object
     * @throws PortfolioNotFoundException if the portfolio is not found
     */
    Optional<Portfolio> getPortfolioById(PortfolioId id);
    
    /**
     * Creates a new portfolio in the persistence store.
     * 
     * @param portfolio The Portfolio domain object to persist
     */
    void createPortfolio(Portfolio portfolio);
    
    /**
     * Saves changes to an existing portfolio.
     * 
     * <p>This method updates the state of an existing portfolio in the persistence store,
     * reflecting any changes made to the domain object such as balance updates, stock
     * purchases or sales, etc.</p>
     * 
     * @param portfolio The Portfolio domain object with updated state
     */
    void savePortfolio(Portfolio portfolio);

    /**
     * Retrieves all portfolios from the persistence store.
     *
     * @return List of Portfolio domain objects
     */
    List<Portfolio> getAllPortfolios();
}