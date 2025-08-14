package cat.gencat.agaur.hexastock.adapter.in;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "jpa", "finhub"})
class PortfolioRestControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    void happyPath_endToEnd() {
        // 1. Create portfolio
        String ownerName = "TestUser";
        ValidatableResponse createResp = RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ownerName\": \"" + ownerName + "\"}")
            .post("/api/portfolios")
            .then()
            .statusCode(201)
            .body("id", notNullValue());
        String portfolioId = createResp.extract().path("id");

        // 2. Deposit
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 10000}")
            .post("/api/portfolios/" + portfolioId + "/deposits")
            .then()
            .statusCode(200);

        // 3. Buy shares
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 10}")
            .post("/api/portfolios/" + portfolioId + "/purchases")
            .then()
            .statusCode(200);

        // 4. Sell shares
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"ticker\": \"AAPL\", \"quantity\": 5}")
            .post("/api/portfolios/" + portfolioId + "/sales")
            .then()
            .statusCode(200)
            .body("proceeds", greaterThan(0f));

        // 5. Withdraw
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body("{\"amount\": 1000}")
            .post("/api/portfolios/" + portfolioId + "/withdrawals")
            .then()
            .statusCode(200);
    }
}
