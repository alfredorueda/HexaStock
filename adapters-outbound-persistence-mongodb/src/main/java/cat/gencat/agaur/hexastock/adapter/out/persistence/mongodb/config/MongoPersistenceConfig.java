package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

/**
 * MongoDB transaction configuration for the "mongodb" profile.
 */
@Configuration
@Profile("mongodb")
public class MongoPersistenceConfig {

    @Bean(name = "transactionManager")
    @Primary
    public MongoTransactionManager transactionManager(MongoDatabaseFactory mongoDbFactory) {
        return new MongoTransactionManager(mongoDbFactory);
    }
}
