package cat.gencat.agaur.hexastock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the HexaStock financial portfolio management system.
 *
 * <p>This is the composition root of the hexagonal architecture. It bootstraps all modules
 * by scanning the base package, which encompasses domain, application, and adapter layers
 * across all Maven submodules.</p>
 *
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 */
@SpringBootApplication(scanBasePackages = "cat.gencat.agaur.hexastock")
public class HexaStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(HexaStockApplication.class, args);
    }
}
