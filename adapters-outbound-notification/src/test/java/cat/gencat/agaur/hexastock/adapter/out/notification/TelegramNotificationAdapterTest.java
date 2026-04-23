package cat.gencat.agaur.hexastock.adapter.out.notification;

import cat.gencat.agaur.hexastock.application.port.out.BuySignal;
import cat.gencat.agaur.hexastock.application.port.out.TriggeredAlertView;
import cat.gencat.agaur.hexastock.model.ExternalApiException;
import cat.gencat.agaur.hexastock.model.market.StockPrice;
import cat.gencat.agaur.hexastock.model.market.Ticker;
import cat.gencat.agaur.hexastock.model.money.Money;
import cat.gencat.agaur.hexastock.model.money.Price;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
@DisplayName("TelegramNotificationAdapter – WireMock integration tests")
class TelegramNotificationAdapterTest {

    private TelegramNotificationAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        adapter = new TelegramNotificationAdapter();
        ReflectionTestUtils.setField(adapter, "botToken", "test-token");
        ReflectionTestUtils.setField(adapter, "telegramApiBaseUrl", wmInfo.getHttpBaseUrl());
    }

    @Test
    void sendsMessageToTelegram() {
        stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .withRequestBody(matchingJsonPath("$.chat_id", equalTo("123456")))
                .willReturn(okJson("""
                        {"ok":true,"result":{"message_id":1}}
                        """)));

        StockPrice price = new StockPrice(Ticker.of("AAPL"), Price.of("140.00"), Instant.now());
        TriggeredAlertView view = new TriggeredAlertView("alice", "Tech", "123456", Ticker.of("AAPL"), Money.of("150.00"));
        BuySignal signal = BuySignal.from(view, price);

        adapter.notifyBuySignal(signal);

        verify(postRequestedFor(urlPathEqualTo("/bottest-token/sendMessage")));
    }

    @Test
    void throwsExternalApiExceptionOnServerError() {
        stubFor(post(urlPathEqualTo("/bottest-token/sendMessage"))
                .willReturn(serverError()));

        StockPrice price = new StockPrice(Ticker.of("AAPL"), Price.of("140.00"), Instant.now());
        TriggeredAlertView view = new TriggeredAlertView("alice", "Tech", "123456", Ticker.of("AAPL"), Money.of("150.00"));
        BuySignal signal = BuySignal.from(view, price);

        assertThatThrownBy(() -> adapter.notifyBuySignal(signal))
                .isInstanceOf(ExternalApiException.class);
    }
}

