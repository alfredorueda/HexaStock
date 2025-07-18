package cat.gencat.agaur.hexastock.model.service;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.HoldingDTO;
import cat.gencat.agaur.hexastock.application.port.in.ReportingUseCase;
import cat.gencat.agaur.hexastock.application.port.out.PortfolioPort;
import cat.gencat.agaur.hexastock.application.port.out.StockPriceProviderPort;
import cat.gencat.agaur.hexastock.application.port.out.TransactionPort;
import cat.gencat.agaur.hexastock.model.*;
import cat.gencat.agaur.hexastock.model.exception.PortfolioNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
Servicio para el cálculo de rendimento
 */

@Service // TODO: Añadimos provisionalmente el @Service
public class HoldingPerformanceCalculator {

    public List<HoldingDTO> getHoldingsPerfomance(Portfolio portfolio, List<Transaction> lTransactions,
                                                  Map<Ticker, StockPrice> mTickerPrices) {

        Map<Ticker, List<Transaction>> mapTickerTrans = lTransactions.stream()
                .filter(t -> t.getTicker() != null)
                .collect(Collectors.groupingBy(Transaction::getTicker));

        return mapTickerTrans.entrySet().stream().map(tickerTransactions ->
                new HoldingDTO(tickerTransactions.getKey().value(),
                        getQuantity(tickerTransactions.getValue()),
                        getRemaining(portfolio.getHolding(tickerTransactions.getKey())),
                        getAvaragePurchasePrice(tickerTransactions.getValue()),
                        getCurrentPrice(tickerTransactions.getKey(), mTickerPrices),
                        getUnRealizedGain(portfolio.getHolding(tickerTransactions.getKey()), mTickerPrices),
                        getRealizedGain(tickerTransactions.getValue()))
        ).toList();
    }

    private BigDecimal getQuantity(List<Transaction> lTransactions) {
        return lTransactions.parallelStream()
                .filter(t -> t.getType() == TransactionType.PURCHASE)
                .map(Transaction::getQuantity)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getRemaining(Holding holding) {
        return BigDecimal.valueOf(holding.getTotalShares());
    }

    private BigDecimal getAvaragePurchasePrice(List<Transaction> lTransactions) {

        return lTransactions.parallelStream()
                .filter(t -> t.getType() == TransactionType.PURCHASE)
                .map(t -> t.getUnitPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(getQuantity(lTransactions), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal getCurrentPrice(Ticker ticker, Map<Ticker, StockPrice> mTickerPrices) {

        return BigDecimal.valueOf(mTickerPrices.get(ticker).getPrice());
    }

    private BigDecimal getUnRealizedGain(Holding holding, Map<Ticker, StockPrice> mTickerPrices) {

        return holding.getUnrealizedGain(getCurrentPrice(holding.getTicker(), mTickerPrices));
    }

    private BigDecimal getRealizedGain(List<Transaction> lTransactions) {

         return lTransactions.parallelStream()
                .filter(t -> t.getType() == TransactionType.SALE)
                .map(Transaction::getProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}