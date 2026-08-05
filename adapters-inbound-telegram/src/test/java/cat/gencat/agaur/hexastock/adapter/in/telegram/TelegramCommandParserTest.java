package cat.gencat.agaur.hexastock.adapter.in.telegram;

import cat.gencat.agaur.hexastock.SpecificationRef;
import cat.gencat.agaur.hexastock.TestLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TelegramCommandParser")
class TelegramCommandParserTest {

    @Test
    @SpecificationRef(value = "US-WL-01.AC-1", level = TestLevel.DOMAIN, feature = "watchlists-create.feature")
    void parsesCreateWatchlist() {
        var cmd = TelegramCommandParser.parse("/watchlist_create Tech Stocks").orElseThrow();
        assertThat(cmd).isInstanceOf(TelegramCommand.CreateWatchlist.class);
        assertThat(((TelegramCommand.CreateWatchlist) cmd).listName()).isEqualTo("Tech Stocks");
    }

    @Test
    @SpecificationRef(value = "US-WL-02.AC-1", level = TestLevel.DOMAIN, feature = "watchlists-alerts.feature")
    void parsesAddAlert() {
        var cmd = TelegramCommandParser.parse("/alert_add wl-1 AAPL 150.00").orElseThrow();
        assertThat(cmd).isInstanceOf(TelegramCommand.AddAlert.class);
    }
}

