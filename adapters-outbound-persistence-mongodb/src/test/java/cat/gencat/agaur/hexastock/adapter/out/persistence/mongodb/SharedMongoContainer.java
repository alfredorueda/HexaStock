package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared singleton MongoDB container for all Mongo adapter tests.
 *
 * <p>Started once on first class-load and reused across every test class in the
 * module. Mirrors the pattern used by {@code SharedMySQLContainer} in the JPA
 * adapter module.</p>
 */
public final class SharedMongoContainer {

    public static final MongoDBContainer INSTANCE =
            new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    static {
        INSTANCE.start();
    }

    private SharedMongoContainer() { }
}
