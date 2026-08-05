package cat.gencat.agaur.hexastock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application class for the HexaStock financial portfolio management system.
 *
 * <p>This is the composition root of the hexagonal architecture. It bootstraps all modules
 * by scanning the base package, which encompasses domain, application, and adapter layers
 * across all Maven submodules.</p>
 *
 * <p>The {@link Modulithic} annotation declares this application to Spring Modulith. The
 * key Modulith application modules introduced by the proof-of-concept refactoring are
 * {@code cat.gencat.agaur.hexastock.watchlists} (publisher of
 * {@code WatchlistAlertTriggeredEvent}) and {@code cat.gencat.agaur.hexastock.notifications}
 * (in-process consumer that dispatches to channel-specific senders).</p>
 *
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 */
@SpringBootApplication(scanBasePackages = "cat.gencat.agaur.hexastock")
@Modulithic(systemName = "HexaStock")
@EnableScheduling
public class HexaStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(HexaStockApplication.class, args);
    }
}
