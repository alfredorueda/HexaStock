package cat.gencat.agaur.hexastock.portfolios.application.service;

import cat.gencat.agaur.hexastock.portfolios.application.port.in.PortfolioLifecycleUseCase;
import cat.gencat.agaur.hexastock.portfolios.application.exception.PortfolioNotFoundException;
import cat.gencat.agaur.hexastock.portfolios.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.Portfolio;
import cat.gencat.agaur.hexastock.portfolios.model.portfolio.PortfolioId;
import jakarta.transaction.Transactional;
import java.util.List;

/**
 * Application service for portfolio lifecycle operations: creation, retrieval, and listing.
 *
 * <p>In hexagonal architecture terms, this is an <strong>application service</strong> that:
 * <ul>
 *   <li>Implements a primary port ({@link PortfolioLifecycleUseCase}) to be used by driving adapters</li>
 *   <li>Uses a secondary port ({@link PortfolioPort}) to communicate with driven adapters</li>
 * </ul>
 * </p>
 *
 * <p>This service was extracted from the former {@code PortfolioManagementService} together with
 * {@link CashManagementService} to improve Interface Segregation.</p>
 *
 * @see CashManagementService
 */
@Transactional
public class PortfolioLifecycleService implements PortfolioLifecycleUseCase {

    private final PortfolioPort portfolioPort;

    public PortfolioLifecycleService(PortfolioPort portfolioPort) {
        this.portfolioPort = portfolioPort;
    }

    @Override
    public Portfolio createPortfolio(String ownerName) {
        Portfolio portfolio = Portfolio.create(ownerName);
        portfolioPort.createPortfolio(portfolio);
        return portfolio;
    }

    @Override
    public Portfolio getPortfolio(PortfolioId portfolioId) {
        return portfolioPort.getPortfolioById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId.value()));
    }

    @Override
    public List<Portfolio> getAllPortfolios() {
        return portfolioPort.getAllPortfolios();
    }
}
