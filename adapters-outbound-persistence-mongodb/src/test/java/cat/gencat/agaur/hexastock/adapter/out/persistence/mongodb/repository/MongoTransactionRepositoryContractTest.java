package cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.repository;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.SharedMongoDBContainer;
import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.springdatarepository.MongoTransactionSpringDataRepository;
import cat.gencat.agaur.hexastock.portfolios.application.port.out.AbstractTransactionPortContractTest;
import cat.gencat.agaur.hexastock.portfolios.application.port.out.TransactionPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DataMongoTest
@Import(MongoTransactionRepository.class)
@ActiveProfiles("mongodb")
@DisplayName("MongoTransactionRepository - TransactionPort contract (Testcontainers MongoDB)")
class MongoTransactionRepositoryContractTest extends AbstractTransactionPortContractTest {

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> SharedMongoDBContainer.INSTANCE.getReplicaSetUrl("testdb"));
    }

    @Autowired
    private MongoTransactionRepository repository;

    @Autowired
    private MongoTransactionSpringDataRepository springDataRepository;

    @BeforeEach
    void cleanCollections() {
        springDataRepository.deleteAll();
    }

    @Override
    protected TransactionPort port() {
        return repository;
    }

    @Override @Test protected void depositRoundTrip()                  { super.depositRoundTrip(); }
    @Override @Test protected void purchaseRoundTrip()                 { super.purchaseRoundTrip(); }
    @Override @Test protected void saleRoundTrip()                     { super.saleRoundTrip(); }
    @Override @Test protected void unknownPortfolio_returnsEmptyList() { super.unknownPortfolio_returnsEmptyList(); }
}
