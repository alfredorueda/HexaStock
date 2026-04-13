package cat.gencat.agaur.hexastock.adapter.out.persistence.jpa;

import org.testcontainers.containers.MySQLContainer;

/**
 * Shared singleton MySQL container for all JPA adapter tests.
 *
 * <p>Started once (on first class-load) and reused across every test class
 * in the module. This avoids paying the container startup cost per test
 * class while still testing against real MySQL 8 with InnoDB row-level
 * locking, production-identical SQL dialect, and real transaction semantics.</p>
 */
public final class SharedMySQLContainer {

    public static final MySQLContainer<?> INSTANCE =
            new MySQLContainer<>("mysql:8.0.32").withDatabaseName("testdb");

    static {
        INSTANCE.start();
    }

    private SharedMySQLContainer() { }
}
