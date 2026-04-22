package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springdatarepository.JpaWatchlistQuerySpringDataRepository;
import cat.gencat.agaur.hexastock.application.port.out.TriggeredAlertView;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Component
@Profile("jpa")
public class JpaWatchlistQueryRepository implements WatchlistQueryPort {

    private final JpaWatchlistQuerySpringDataRepository springDataRepository;

    public JpaWatchlistQueryRepository(JpaWatchlistQuerySpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Set<Ticker> findDistinctTickersInActiveWatchlists() {
        return springDataRepository.findDistinctTickersInActiveWatchlists().stream()
                .map(Ticker::of)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public List<TriggeredAlertView> findTriggeredAlerts(Ticker ticker, Money currentPrice) {
        BigDecimal price = currentPrice.amount();
        return springDataRepository.findTriggeredAlerts(ticker.value(), price).stream()
                .map(this::mapRow)
                .toList();
    }

    private TriggeredAlertView mapRow(JpaWatchlistQuerySpringDataRepository.TriggeredAlertRow row) {
        return new TriggeredAlertView(
                row.getOwnerName(),
                row.getListName(),
                row.getTelegramChatId(),
                Ticker.of(row.getTicker()),
                Money.of(row.getThresholdPrice())
        );
    }
}

