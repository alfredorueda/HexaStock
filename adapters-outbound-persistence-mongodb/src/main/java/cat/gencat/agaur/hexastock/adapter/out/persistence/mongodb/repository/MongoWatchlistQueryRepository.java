package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.application.port.out.TriggeredAlertView;
import cat.gencat.agaur.hexastock.application.port.out.WatchlistQueryPort;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.types.Decimal128;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Component
@Profile("mongodb")
public class MongoWatchlistQueryRepository implements WatchlistQueryPort {

    private final MongoTemplate mongoTemplate;

    public MongoWatchlistQueryRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Set<Ticker> findDistinctTickersInActiveWatchlists() {
        UnwindOperation unwindAlerts = unwind("alerts");
        Aggregation agg = newAggregation(
                match(Criteria.where("active").is(true)),
                unwindAlerts,
                group("alerts.ticker")
        );
        AggregationResults<DistinctTickerRow> res =
                mongoTemplate.aggregate(agg, "watchlists", DistinctTickerRow.class);

        return res.getMappedResults().stream()
                .map(r -> Ticker.of(r.id))
                .collect(Collectors.toSet());
    }

    @Override
    public List<TriggeredAlertView> findTriggeredAlerts(Ticker ticker, Money currentPrice) {
        BigDecimal price = currentPrice.amount();
        Decimal128 price128 = new Decimal128(price);
        Aggregation agg = newAggregation(
                match(Criteria.where("active").is(true)),
                unwind("alerts"),
                match(Criteria.where("alerts.ticker").is(ticker.value())
                        .and("alerts.thresholdPrice").gte(price128)),
                project()
                        .and("ownerName").as("ownerName")
                        .and("listName").as("listName")
                        .and("telegramChatId").as("telegramChatId")
                        .and("alerts.ticker").as("ticker")
                        .and("alerts.thresholdPrice").as("thresholdPrice")
        );

        AggregationResults<TriggeredAlertRow> res =
                mongoTemplate.aggregate(agg, "watchlists", TriggeredAlertRow.class);

        return res.getMappedResults().stream()
                .map(r -> new TriggeredAlertView(
                        r.ownerName,
                        r.listName,
                        r.telegramChatId,
                        Ticker.of(r.ticker),
                        Money.of(r.thresholdPrice)
                ))
                .toList();
    }

    static class DistinctTickerRow {
        public String id;
    }

    static class TriggeredAlertRow {
        public String ownerName;
        public String listName;
        public String telegramChatId;
        public String ticker;
        public BigDecimal thresholdPrice;
    }
}

