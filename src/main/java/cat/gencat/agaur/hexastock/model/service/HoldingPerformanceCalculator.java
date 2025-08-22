package cat.gencat.agaur.hexastock.model.service;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.HoldingDTO;
import cat.gencat.agaur.hexastock.model.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HoldingPerformanceCalculator provides methods to calculate the performance of holdings in a portfolio.
 *
 * <p>In DDD terms, this is a <strong>Domain Service</strong> that encapsulates
 * business logic related to calculating the performance of financial holdings.</p>
 *
 * <p>This service:</p>
 * <ul>
 *   <li>Calculates the total quantity of shares held for each ticker</li>
 *   <li>Computes the average purchase price of shares</li>
 *   <li>Determines the current market price of shares</li>
 *   <li>Calculates unrealized and realized gains for each holding</li>
 * </ul>
 *
 * <p>The service operates on domain models such as {@link Portfolio}, {@link Transaction}, and {@link StockPrice},
 * and returns results in the form of {@link HoldingDTO} objects.</p>
 */
public class HoldingPerformanceCalculator {

    public List<HoldingDTO> getHoldingsPerformance(Portfolio portfolio,
                                                   List<Transaction> transactions,
                                                   Map<Ticker, StockPrice> mTickerPrices) {

        Map<Ticker, List<Transaction>> mapTickerTrans = transactions.stream()
                .filter(t -> t.getTicker() != null)
                .collect(Collectors.groupingBy(Transaction::getTicker));

        return mapTickerTrans.entrySet()
                .stream()
                .map(tickerTransactions ->

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
        return BigDecimal.valueOf(mTickerPrices.get(ticker).price());
    }

    private BigDecimal getUnRealizedGain(Holding holding, Map<Ticker, StockPrice> mTickerPrices) {

        return holding.getUnrealizedGain(getCurrentPrice(holding.getTicker(), mTickerPrices));
    }

    private BigDecimal getRealizedGain(List<Transaction> transactions) {

         return transactions.parallelStream()
                .filter(t -> t.getType() == TransactionType.SALE)
                .map(Transaction::getProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}