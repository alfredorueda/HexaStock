package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.jpa.springdatarepository.JpaAlertEntrySpringDataRepository;
import cat.gencat.agaur.hexastock.application.port.out.TriggeredAlertView;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import cat.gencat.agaur.hexastock.model.Money;
import cat.gencat.agaur.hexastock.model.Ticker;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@Profile("jpa")
public class JpaWatchlistQueryRepository implements WatchlistQueryPort {

    private final JpaAlertEntrySpringDataRepository jpaAlertEntrySpringDataRepository;

    public JpaWatchlistQueryRepository(JpaAlertEntrySpringDataRepository jpaAlertEntrySpringDataRepository) {
        this.jpaAlertEntrySpringDataRepository = jpaAlertEntrySpringDataRepository;
    }

    @Override
    public Set<Ticker> findDistinctTickersInActiveWatchlists() {
        return jpaAlertEntrySpringDataRepository.findDistinctTickersInActiveWatchlists().stream()
                .map(Ticker::of)
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public List<TriggeredAlertView> findTriggeredAlerts(Ticker ticker, Money currentPrice) {
        return jpaAlertEntrySpringDataRepository.findTriggeredAlerts(ticker.value(), currentPrice.amount()).stream()
                .map(projection -> new TriggeredAlertView(
                        projection.getOwnerName(),
                        projection.getListName(),
                        Ticker.of(projection.getTicker()),
                        Money.of(projection.getThresholdPrice())
                ))
                .toList();
    }
}
