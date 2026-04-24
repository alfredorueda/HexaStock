package cat.gencat.agaur.hexastock.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
