package cat.gencat.agaur.hexastock.architecture;

import cat.gencat.agaur.hexastock.adapter.out.persistence.mongodb.OptimisticVersionContext;
import cat.gencat.agaur.hexastock.application.annotation.RetryOnWriteConflict;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture fitness tests that enforce hexagonal dependency rules
 * across the multi-module build.
 *
 * <p>These tests scan compiled classes from all modules and verify that
 * dependency directions follow the hexagonal architecture constraints:</p>
 * <ul>
 *   <li>Domain has no dependencies on application, adapters, or Spring</li>
 *   <li>Application depends only on domain (not on adapters or Spring)</li>
 *   <li>Adapters depend on application (ports) but not on each other</li>
 * </ul>
 */
@DisplayName("Hexagonal Architecture Rules")
class HexagonalArchitectureTest {

    private static JavaClasses allClasses;

    @BeforeAll
    static void importClasses() {
        allClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("cat.gencat.agaur.hexastock");
    }

    @Nested
    @DisplayName("Domain layer")
    class DomainLayer {

        @Test
        @DisplayName("should not depend on application layer")
        void domainDoesNotDependOnApplication() {
            noClasses()
                    .that().resideInAPackage("..model..")
                    .should().dependOnClassesThat().resideInAPackage("..application..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("should not depend on adapter layer")
        void domainDoesNotDependOnAdapters() {
            noClasses()
                    .that().resideInAPackage("..model..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("should not depend on Spring framework")
        void domainDoesNotDependOnSpring() {
            noClasses()
                    .that().resideInAPackage("..model..")
                    .and().haveSimpleNameNotEndingWith("package-info")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .check(allClasses);
        }
    }

    @Nested
    @DisplayName("Application layer")
    class ApplicationLayer {

        @Test
        @DisplayName("should not depend on adapter layer")
        void applicationDoesNotDependOnAdapters() {
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("should not depend on Spring framework")
        void applicationDoesNotDependOnSpring() {
            noClasses()
                    .that().resideInAPackage("..application..")
                    .and().haveSimpleNameNotEndingWith("package-info")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .check(allClasses);
        }
    }

    @Nested
    @DisplayName("Optimistic locking")
    class OptimisticLocking {

        @Test
        @DisplayName("MongoDB repositories backing @RetryOnWriteConflict use cases must declare OptimisticVersionContext")
        void mongoRepositoriesBehindRetriedUseCasesMustDeclareVersionContext() {
            // Step 1: collect outbound port interfaces used by services with @RetryOnWriteConflict methods
            Set<JavaClass> retriedPorts = allClasses.stream()
                    .filter(c -> c.getMethods().stream()
                            .anyMatch(m -> m.isAnnotatedWith(RetryOnWriteConflict.class)))
                    .flatMap(c -> c.getFields().stream())
                    .map(JavaField::getRawType)
                    .filter(t -> t.getPackageName().contains(".application.port.out"))
                    .collect(Collectors.toSet());

            if (retriedPorts.isEmpty()) {
                return; // no retried use cases yet — rule trivially satisfied
            }

            // Step 2: MongoDB adapters in the repository package that implement any of those ports
            DescribedPredicate<JavaClass> isMongoAdapterOfRetriedPort = DescribedPredicate.describe(
                    "is a MongoDB repository adapter for a port used by @RetryOnWriteConflict",
                    clazz -> retriedPorts.stream()
                            .anyMatch(port -> clazz.getAllRawInterfaces().contains(port)));

            // Step 3: must declare an OptimisticVersionContext field
            ArchCondition<JavaClass> hasVersionContextField =
                    new ArchCondition<>("declare a field of type OptimisticVersionContext") {
                        @Override
                        public void check(JavaClass clazz, ConditionEvents events) {
                            boolean found = clazz.getFields().stream()
                                    .anyMatch(f -> f.getRawType().isEquivalentTo(OptimisticVersionContext.class));
                            if (!found) {
                                events.add(SimpleConditionEvent.violated(clazz,
                                        clazz.getSimpleName() + " is missing an OptimisticVersionContext field" +
                                        " — its port is used by a @RetryOnWriteConflict use case"));
                            }
                        }
                    };

            classes()
                    .that().resideInAPackage("..adapter.out.persistence.mongodb.repository..")
                    .and(isMongoAdapterOfRetriedPort)
                    .should(hasVersionContextField)
                    .check(allClasses);
        }
    }

    @Nested
    @DisplayName("Adapter isolation")
    class AdapterIsolation {

        @Test
        @DisplayName("inbound REST adapter should not depend on outbound persistence adapter")
        void restDoesNotDependOnPersistence() {
            noClasses()
                    .that().resideInAPackage("..adapter.in..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter.out..")
                    .check(allClasses);
        }

        @Test
        @DisplayName("outbound adapters should not depend on inbound REST adapter")
        void outboundDoesNotDependOnInbound() {
            noClasses()
                    .that().resideInAPackage("..adapter.out..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter.in..")
                    .check(allClasses);
        }
    }
}
