package cat.gencat.agaur.hexastock.application.port.in;

import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Portfolio;

public interface PortfolioManagmentUseCase {

    Portfolio createPortfolio(String ownerName);
    Portfolio getPortfolio(String id);
    void deposit(String idPortfolio, Money amount);
    void withdraw(String idPortfolio, Money amount);

}
