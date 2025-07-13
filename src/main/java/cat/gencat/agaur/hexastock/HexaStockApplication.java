package cat.gencat.agaur.hexastock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the HexaStock financial portfolio management system.
 * 
 * <p>This application implements a stock portfolio management system using hexagonal architecture
 * and Domain-Driven Design principles. It serves as the entry point for the application
 * and configures Spring Boot components.</p>
 * 
 * <p>The application is structured according to hexagonal architecture:</p>
 * <ul>
 *   <li>Domain Model - Core business entities and logic</li>
 *   <li>Application Layer - Use cases and ports</li>
 *   <li>Infrastructure Layer - Adapters for external systems and frameworks</li>
 * </ul>
 * 
 * @see org.springframework.boot.SpringApplication
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 */
@SpringBootApplication
public class HexaStockApplication {
    /**
     * The main method that serves as the entry point for the HexaStock application.
     * 
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(HexaStockApplication.class, args);
    }
}
