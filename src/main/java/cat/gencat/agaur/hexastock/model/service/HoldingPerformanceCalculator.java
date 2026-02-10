package cat.gencat.agaur.hexastock.model.service;

import cat.gencat.agaur.hexastock.adapter.in.webmodel.HoldingDTO;
import cat.gencat.agaur.hexastock.model.*;

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
                                                   Map<Ticker, StockPrice> tickerPrices) {
        Map<Ticker, List<Transaction>> transactionsByTicker = transactions.stream()
                .filter(t -> t.getTicker() != null)
                .collect(Collectors.groupingBy(Transaction::getTicker));

        return transactionsByTicker.entrySet().stream()
                .map(entry -> {
                    Ticker ticker = entry.getKey();
                    List<Transaction> tickerTransactions = entry.getValue();
                    Holding holding = portfolio.getHolding(ticker);
                    
                    return new HoldingDTO(
                            ticker.value(),
                            getQuantity(tickerTransactions),
                            getRemaining(holding),
                            getAveragePurchasePrice(tickerTransactions),
                            getCurrentPrice(ticker, tickerPrices),
                            getUnrealizedGain(holding, tickerPrices),
                            getRealizedGain(tickerTransactions)
                    );
                })
                .toList();
    }

    private BigDecimal getQuantity(List<Transaction> transactions) {
        return transactions.parallelStream()
                .filter(t -> t.getType() == TransactionType.PURCHASE)
                .map(t -> BigDecimal.valueOf(t.getQuantity().value()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getRemaining(Holding holding) {
        return BigDecimal.valueOf(holding.getTotalShares().value());
    }

    private BigDecimal getAveragePurchasePrice(List<Transaction> transactions) {
        BigDecimal totalCost = transactions.parallelStream()
                .filter(t -> t.getType() == TransactionType.PURCHASE)
                .map(t -> t.getUnitPrice().value().multiply(BigDecimal.valueOf(t.getQuantity().value())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal quantity = getQuantity(transactions);
        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(quantity, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal getCurrentPrice(Ticker ticker, Map<Ticker, StockPrice> tickerPrices) {
        return tickerPrices.get(ticker).price().value();
    }

    private BigDecimal getUnrealizedGain(Holding holding, Map<Ticker, StockPrice> tickerPrices) {
        Price currentPrice = tickerPrices.get(holding.getTicker()).price();
        return holding.getUnrealizedGain(currentPrice).amount();
    }

    private BigDecimal getRealizedGain(List<Transaction> transactions) {
        return transactions.parallelStream()
                .filter(t -> t.getType() == TransactionType.SALE)
                .map(t -> t.getProfit().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}