package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb;

import org.testcontainers.containers.MongoDBContainer;

/**
 * Shared singleton MongoDB container for Mongo adapter tests.
 */
public final class SharedMongoDBContainer {

    public static final MongoDBContainer INSTANCE =
            new MongoDBContainer("mongo:7.0.12");

    static {
        INSTANCE.start();
    }

    private SharedMongoDBContainer() {
    }
}
