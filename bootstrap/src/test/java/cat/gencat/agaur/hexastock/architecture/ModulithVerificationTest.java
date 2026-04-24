package cat.gencat.agaur.hexastock.architecture;

import cat.gencat.agaur.hexastock.HexaStockApplication;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Modulith fitness tests that verify the structural integrity of the
 * application modules introduced by the proof-of-concept refactoring.
 *
 * <h2>Staged verification strategy</h2>
 * <p>The HexaStock codebase still mixes two architectural taxonomies during the
 * incremental Modulith refactoring described in
 * {@code doc/architecture/SPRING-MODULITH-GLOBAL-REFACTORING-PLAN.md}:</p>
 * <ul>
 *   <li><b>Legacy hexagonal layout</b> — top-level packages {@code model},
 *       {@code application}, {@code adapter}, {@code config}, {@code architecture}.
 *       These predate the Modulith POC and follow the hexagonal/onion layering
 *       enforced by {@link HexagonalArchitectureTest}. They were never designed
 *       as Modulith modules with named interfaces, so a global
 *       {@code ApplicationModules.verify()} would produce noisy false positives
 *       (non-exposed type errors, ambiguous module roots).</li>
 *   <li><b>New business-capability modules</b> — top-level packages
 *       {@code watchlists} and {@code notifications}. These were designed
 *       from day one as Modulith modules with explicit, asymmetric event-driven
 *       boundaries.</li>
 * </ul>
 *
 * <p>Modulith verification is therefore <b>scoped</b> to the new modules. As
 * each future Bounded Context migrates into its own top-level package
 * (Phase 3+ of the global plan), it will be added to {@link #MODULE_PACKAGES}
 * and the same scoped verification will guard it. Phase 6 of the plan replaces
 * this scoped strategy with a single global verification once every BC has
 * been extracted.</p>
 *
 * <p>This test also renders Modulith's PlantUML / AsciiDoc documentation into
 * {@code target/spring-modulith-docs} so the module graph can be inspected
 * after each build.</p>
 */
@DisplayName("Spring Modulith - module structure")
class ModulithVerificationTest {

    /**
     * Top-level package names that have been intentionally promoted to
     * Spring Modulith application modules. Add a new entry here whenever a
     * future migration phase extracts a Bounded Context.
     */
    private static final String[] MODULE_PACKAGES = {
            "cat.gencat.agaur.hexastock.watchlists",
            "cat.gencat.agaur.hexastock.notifications"
    };

    /**
     * Module set scoped to the promoted modules only. Classes outside any of
     * {@link #MODULE_PACKAGES} are excluded from Modulith inspection.
     */
    private static final ApplicationModules MODULES = ApplicationModules.of(
            HexaStockApplication.class,
            new DescribedPredicate<JavaClass>(
                    "classes outside the promoted Spring Modulith application modules") {
                @Override
                public boolean test(JavaClass type) {
                    String pkg = type.getPackageName();
                    for (String modulePkg : MODULE_PACKAGES) {
                        if (pkg.startsWith(modulePkg)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
    );

    @Test
    @DisplayName("all promoted top-level packages are detected as Modulith modules")
    void allPromotedModulesAreDetected() {
        MODULES.forEach(module -> System.out.println("Detected module: " + module.getName()));
        assertThat(MODULES.stream().map(ApplicationModule::getName))
                .as("Modulith should detect exactly the promoted modules")
                .containsExactlyInAnyOrder("watchlists", "notifications");
    }

    @Test
    @DisplayName("module structure is acyclic and respects allowed dependencies")
    void verifyModuleStructure() {
        MODULES.verify();
    }

    @Test
    @DisplayName("notifications module is rooted under the expected package")
    void notificationsModuleIsRootedCorrectly() {
        ApplicationModule notifications = MODULES.getModuleByName("notifications").orElseThrow();
        assertThat(notifications.getBasePackage().getName())
                .as("notifications module base package")
                .endsWith("notifications");
    }

    @Test
    @DisplayName("notifications module's only cross-module dependency is on 'watchlists'")
    void notificationsOnlyDependsOnWatchlists() {
        ApplicationModule notifications = MODULES.getModuleByName("notifications").orElseThrow();
        assertThat(notifications.getDependencies(MODULES).stream()
                .map(dep -> dep.getTargetModule().getName())
                .filter(name -> !name.equals("notifications")))
                .as("cross-module dependencies of notifications")
                .containsOnly("watchlists");
    }

    @Test
    @DisplayName("watchlists module is a pure publisher (no outgoing cross-module dependencies)")
    void watchlistsHasNoOutgoingModuleDependencies() {
        ApplicationModule watchlists = MODULES.getModuleByName("watchlists").orElseThrow();
        assertThat(watchlists.getDependencies(MODULES).stream()
                .map(dep -> dep.getTargetModule().getName())
                .filter(name -> !name.equals("watchlists")))
                .as("watchlists must not depend on any other promoted module")
                .isEmpty();
    }

    @Test
    @DisplayName("renders Modulith documentation (diagrams + canvas) under target/")
    void writeDocumentation() {
        new Documenter(MODULES).writeDocumentation();
    }
}
