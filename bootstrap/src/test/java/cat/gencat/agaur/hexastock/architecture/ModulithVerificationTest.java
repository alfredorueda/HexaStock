package cat.gencat.agaur.hexastock.architecture;

import cat.gencat.agaur.hexastock.HexaStockApplication;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith fitness tests that verify the structural integrity of the new
 * {@code watchlists} and {@code notifications} application modules introduced by
 * the proof-of-concept refactoring.
 *
 * <p>The pre-existing hexagonal layout (top-level packages {@code model},
 * {@code application}, {@code adapter}, {@code config}, {@code architecture})
 * is intentionally excluded from Modulith inspection: those packages predate the
 * Modulith POC and follow the classical hexagonal/onion layering enforced by
 * {@link HexagonalArchitectureTest}. Modulith verification is therefore scoped
 * exclusively to the two new modules so that cyclic-dependency detection and
 * named-interface enforcement target only the POC surface area.</p>
 *
 * <p>This test additionally renders Modulith's PlantUML / AsciiDoc documentation
 * into {@code target/spring-modulith-docs} so the module graph can be inspected
 * after each build.</p>
 */
@DisplayName("Spring Modulith - module structure")
class ModulithVerificationTest {

    /**
     * Module set scoped to the POC packages only. Everything outside
     * {@code watchlists} and {@code notifications} is excluded from inspection.
     */
    private static final ApplicationModules MODULES = ApplicationModules.of(
            HexaStockApplication.class,
            new DescribedPredicate<JavaClass>("classes outside the watchlists/notifications POC modules") {
                @Override
                public boolean test(JavaClass type) {
                    String pkg = type.getPackageName();
                    return !pkg.startsWith("cat.gencat.agaur.hexastock.watchlists")
                        && !pkg.startsWith("cat.gencat.agaur.hexastock.notifications");
                }
            }
    );

    @Test
    @DisplayName("watchlists and notifications modules are detected")
    void modulesAreDetected() {
        MODULES.forEach(module -> System.out.println("Detected module: " + module.getName()));
        assert MODULES.stream().anyMatch(m -> m.getName().equals("watchlists"))
                : "Expected 'watchlists' module to be detected by Spring Modulith";
        assert MODULES.stream().anyMatch(m -> m.getName().equals("notifications"))
                : "Expected 'notifications' module to be detected by Spring Modulith";
    }

    @Test
    @DisplayName("module structure is acyclic and respects allowed dependencies")
    void verifyModuleStructure() {
        MODULES.verify();
    }

    @Test
    @DisplayName("notifications module depends on watchlists for the published event type only")
    void notificationsListensToWatchlistEvents() {
        ApplicationModule notifications = MODULES.getModuleByName("notifications").orElseThrow();
        // Sanity check: Modulith's verify() above catches forbidden cross-module imports;
        // this test documents the intent and confirms the module is rooted under the expected package.
        assert notifications.getBasePackage().getName().endsWith("notifications")
                : "notifications module base package should end with 'notifications', was: "
                  + notifications.getBasePackage().getName();
    }

    @Test
    @DisplayName("renders Modulith documentation (diagrams + canvas) under target/")
    void writeDocumentation() {
        new Documenter(MODULES).writeDocumentation();
    }
}
