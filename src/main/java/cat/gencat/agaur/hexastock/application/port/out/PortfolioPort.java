package cat.gencat.agaur.hexastock.application.port.out;

import cat.gencat.agaur.hexastock.model.Portfolio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface PortfolioPort {

    Portfolio getPortfolioById(String id);
    void createPortfolio(Portfolio portfolio);
    void savePortfolio(Portfolio portfolio);
}