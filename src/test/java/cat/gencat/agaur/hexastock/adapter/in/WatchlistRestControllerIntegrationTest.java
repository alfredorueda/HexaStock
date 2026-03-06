package cat.gencat.agaur.hexastock.adapter.in;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Watchlist REST API.
 *
 * Tests the complete end-to-end flow of watchlist management:
 * - Creating watchlists
 * - Adding and removing entries
 * - Retrieving watchlists
 * - Deleting watchlists
 *
 * Uses Testcontainers to run MySQL in a container and RestAssured for HTTP testing.
 * Tests verify that the entire stack (controllers, services, domain, persistence) works correctly.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "jpa", "mockfinhub"})
@DisplayName("Watchlist REST Controller Integration Tests")
class WatchlistRestControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Nested
    @DisplayName("Watchlist Creation")
    class WatchlistCreation {

        @Test
        @DisplayName("Should create watchlist successfully")
        void shouldCreateWatchlistSuccessfully() {
            // When
            ValidatableResponse response = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "John Doe",
                            "name": "Tech Stocks"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201);

            // Then
            response.body("id", notNullValue())
                    .body("ownerName", equalTo("John Doe"))
                    .body("name", equalTo("Tech Stocks"))
                    .body("entries", empty());
        }

        @Test
        @DisplayName("Should create multiple watchlists for same owner")
        void shouldCreateMultipleWatchlistsForSameOwner() {
            // Given - Use unique owner per test run to avoid cross-test data pollution
            String ownerName = "Jane Smith " + java.util.UUID.randomUUID();

            // When - Create first watchlist
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(String.format("""
                        {
                            "ownerName": "%s",
                            "name": "Tech Stocks"
                        }
                        """, ownerName))
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201);

            // When - Create second watchlist
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(String.format("""
                        {
                            "ownerName": "%s",
                            "name": "Growth Stocks"
                        }
                        """, ownerName))
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201);

            // Then - Verify both exist
            RestAssured.given()
                    .queryParam("owner", ownerName)
                    .get("/api/watchlists")
                    .then()
                    .statusCode(200)
                    .body("size()", equalTo(2))
                    .body("name", hasItems("Tech Stocks", "Growth Stocks"));
        }
    }

    @Nested
    @DisplayName("Entry Management")
    class EntryManagement {

        @Test
        @DisplayName("Should add entry to watchlist")
        void shouldAddEntryToWatchlist() {
            // Given - Create watchlist
            String watchlistId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "Test User",
                            "name": "My Watchlist"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            // When - Add entry
            ValidatableResponse response = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "AAPL",
                            "thresholdPrice": 150.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200);

            // Then
            response.body("entries.size()", equalTo(1))
                    .body("entries[0].ticker", equalTo("AAPL"))
                    .body("entries[0].thresholdPrice", equalTo(150.00f))
                    .body("entries[0].currency", equalTo("USD"))
                    .body("entries[0].id", notNullValue());
        }

        @Test
        @DisplayName("Should add multiple entries to watchlist")
        void shouldAddMultipleEntriesToWatchlist() {
            // Given - Create watchlist
            String watchlistId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "Test User",
                            "name": "Tech Watchlist"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            // When - Add first entry
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "AAPL",
                            "thresholdPrice": 150.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200);

            // When - Add second entry
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "MSFT",
                            "thresholdPrice": 300.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200);

            // Then - Verify both entries exist
            RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(200)
                    .body("entries.size()", equalTo(2))
                    .body("entries.ticker", hasItems("AAPL", "MSFT"));
        }

        @Test
        @DisplayName("Should allow same ticker with different thresholds")
        void shouldAllowSameTickerWithDifferentThresholds() {
            // Given - Create watchlist
            String watchlistId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "Test User",
                            "name": "Multi-threshold Watchlist"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            // When - Add AAPL with threshold 150
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "AAPL",
                            "thresholdPrice": 150.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200);

            // When - Add AAPL with threshold 145
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "AAPL",
                            "thresholdPrice": 145.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200);

            // Then - Verify both AAPL entries exist
            RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(200)
                    .body("entries.size()", equalTo(2))
                    .body("entries.ticker", everyItem(equalTo("AAPL")))
                    .body("entries.thresholdPrice", hasItems(150.00f, 145.00f));
        }

        @Test
        @DisplayName("Should remove entry from watchlist")
        void shouldRemoveEntryFromWatchlist() {
            // Given - Create watchlist with entry
            String watchlistId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "Test User",
                            "name": "My Watchlist"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            String entryId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "AAPL",
                            "thresholdPrice": 150.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200)
                    .extract().path("entries[0].id");

            // When - Remove entry
            RestAssured.given()
                    .delete("/api/watchlists/" + watchlistId + "/entries/" + entryId)
                    .then()
                    .statusCode(200);

            // Then - Verify entry removed
            RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(200)
                    .body("entries", empty());
        }

        @Test
        @DisplayName("Should remove correct entry when multiple exist")
        void shouldRemoveCorrectEntryWhenMultipleExist() {
            // Given - Create watchlist
            String watchlistId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "Test User",
                            "name": "Multi-entry Watchlist"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            // Add AAPL
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "AAPL",
                            "thresholdPrice": 150.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200);

            // Add MSFT and get its ID
            String msftEntryId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "MSFT",
                            "thresholdPrice": 300.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200)
                    .extract().path("entries.find { it.ticker == 'MSFT' }.id");

            // Add GOOGL
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "GOOGL",
                            "thresholdPrice": 140.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200);

            // When - Remove MSFT entry
            RestAssured.given()
                    .delete("/api/watchlists/" + watchlistId + "/entries/" + msftEntryId)
                    .then()
                    .statusCode(200);

            // Then - Verify only AAPL and GOOGL remain
            RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(200)
                    .body("entries.size()", equalTo(2))
                    .body("entries.ticker", hasItems("AAPL", "GOOGL"))
                    .body("entries.ticker", not(hasItem("MSFT")));
        }
    }

    @Nested
    @DisplayName("Watchlist Retrieval")
    class WatchlistRetrieval {

        @Test
        @DisplayName("Should get watchlist by ID")
        void shouldGetWatchlistById() {
            // Given - Create watchlist
            String watchlistId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "John Doe",
                            "name": "Tech Stocks"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            // When - Get watchlist
            ValidatableResponse response = RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(200);

            // Then
            response.body("id", equalTo(watchlistId))
                    .body("ownerName", equalTo("John Doe"))
                    .body("name", equalTo("Tech Stocks"));
        }

        @Test
        @DisplayName("Should get all watchlists for owner")
        void shouldGetAllWatchlistsForOwner() {
            // Given - Use unique owner per test run to avoid cross-test data pollution
            String ownerName = "Jane Smith " + java.util.UUID.randomUUID();

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(String.format("""
                        {
                            "ownerName": "%s",
                            "name": "Tech Stocks"
                        }
                        """, ownerName))
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201);

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(String.format("""
                        {
                            "ownerName": "%s",
                            "name": "Growth Stocks"
                        }
                        """, ownerName))
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201);

            // When - Get watchlists by owner
            ValidatableResponse response = RestAssured.given()
                    .queryParam("owner", ownerName)
                    .get("/api/watchlists")
                    .then()
                    .statusCode(200);

            // Then
            response.body("size()", equalTo(2))
                    .body("ownerName", everyItem(equalTo(ownerName)))
                    .body("name", hasItems("Tech Stocks", "Growth Stocks"));
        }

        @Test
        @DisplayName("Should return empty list when owner has no watchlists")
        void shouldReturnEmptyListWhenOwnerHasNoWatchlists() {
            // When
            ValidatableResponse response = RestAssured.given()
                    .queryParam("owner", "NonExistentUser")
                    .get("/api/watchlists")
                    .then()
                    .statusCode(200);

            // Then
            response.body("size()", equalTo(0));
        }

        @Test
        @DisplayName("Should retrieve watchlist with all its entries")
        void shouldRetrieveWatchlistWithAllItsEntries() {
            // Given - Create watchlist with multiple entries
            String watchlistId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "Test User",
                            "name": "Complete Watchlist"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            // Add entries
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "AAPL",
                            "thresholdPrice": 150.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200);

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "MSFT",
                            "thresholdPrice": 300.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200);

            // When - Get watchlist
            ValidatableResponse response = RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(200);

            // Then
            response.body("entries.size()", equalTo(2))
                    .body("entries.ticker", hasItems("AAPL", "MSFT"))
                    .body("entries.thresholdPrice", hasItems(150.00f, 300.00f));
        }
    }

    @Nested
    @DisplayName("Watchlist Deletion")
    class WatchlistDeletion {

        @Test
        @DisplayName("Should delete watchlist")
        void shouldDeleteWatchlist() {
            // Given - Create watchlist
            String watchlistId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "Test User",
                            "name": "Temporary Watchlist"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            // When - Delete watchlist
            RestAssured.given()
                    .delete("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(204);

            // Then - Verify watchlist no longer exists
            RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(404);
        }

        @Test
        @DisplayName("Should delete watchlist with entries")
        void shouldDeleteWatchlistWithEntries() {
            // Given - Create watchlist with entries
            String watchlistId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "Test User",
                            "name": "Watchlist With Entries"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201)
                    .extract().path("id");

            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "AAPL",
                            "thresholdPrice": 150.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200);

            // When - Delete watchlist
            RestAssured.given()
                    .delete("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(204);

            // Then - Verify watchlist deleted
            RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("End-to-End Happy Path")
    class EndToEndHappyPath {

        @Test
        @DisplayName("Should execute complete watchlist lifecycle")
        void shouldExecuteCompleteWatchlistLifecycle() {
            // 1. Create watchlist
            String watchlistId = RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ownerName": "E2E Test User",
                            "name": "E2E Watchlist"
                        }
                        """)
                    .post("/api/watchlists")
                    .then()
                    .statusCode(201)
                    .body("ownerName", equalTo("E2E Test User"))
                    .body("name", equalTo("E2E Watchlist"))
                    .body("entries", empty())
                    .extract().path("id");

            // 2. Add first entry
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "AAPL",
                            "thresholdPrice": 150.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200)
                    .body("entries.size()", equalTo(1));

            // 3. Add second entry
            RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body("""
                        {
                            "ticker": "MSFT",
                            "thresholdPrice": 300.00,
                            "currency": "USD"
                        }
                        """)
                    .post("/api/watchlists/" + watchlistId + "/entries")
                    .then()
                    .statusCode(200)
                    .body("entries.size()", equalTo(2));

            // 4. Verify watchlist state
            String firstEntryId = RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(200)
                    .body("entries.size()", equalTo(2))
                    .body("entries.ticker", hasItems("AAPL", "MSFT"))
                    .extract().path("entries[0].id");

            // 5. Remove first entry
            RestAssured.given()
                    .delete("/api/watchlists/" + watchlistId + "/entries/" + firstEntryId)
                    .then()
                    .statusCode(200)
                    .body("entries.size()", equalTo(1));

            // 6. Verify final state
            RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(200)
                    .body("entries.size()", equalTo(1));

            // 7. Delete watchlist
            RestAssured.given()
                    .delete("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(204);

            // 8. Verify deletion
            RestAssured.given()
                    .get("/api/watchlists/" + watchlistId)
                    .then()
                    .statusCode(404);
        }
    }
}