package cat.gencat.agaur.hexastock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Links a test to a specific functional scenario in the behavioural specification.
 *
 * <h3>Purpose</h3>
 * In professional software engineering, it is valuable to maintain an explicit
 * connection between requirements, behavioural scenarios, and automated tests.
 * This annotation creates that connection — it records <em>which specification
 * scenario</em> a given test is verifying.
 *
 * <h3>Traceability chain</h3>
 * <pre>
 *   Requirement (US-07)  →  Scenario (.feature)  →  Test (JUnit)  →  Code
 * </pre>
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li><strong>Lightweight:</strong> a single annotation with no runtime dependencies —
 *       no Cucumber, no BDD framework, no external tooling required.</li>
 *   <li><strong>Framework-agnostic:</strong> works with JUnit 5, TestNG, or any test runner.
 *       Retained at runtime so that tools or reflective scanners can build
 *       traceability reports if needed.</li>
 *   <li><strong>{@code @Repeatable}:</strong> a test that verifies multiple acceptance
 *       criteria can carry more than one {@code @SpecificationRef}.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>
 * &#64;Test
 * &#64;SpecificationRef(value = "US-07.FIFO-2", level = TestLevel.DOMAIN,
 *                   feature = "sell-stocks.feature")
 * void shouldSellSharesAcrossMultipleLots() { ... }
 * </pre>
 *
 * <h3>Why this matters for AI-assisted development</h3>
 * When specifications, tests, and code are explicitly connected, AI tools can
 * reason about the system more effectively — generating code aligned with
 * specifications, detecting inconsistencies, and suggesting tests that cover
 * missing scenarios.
 *
 * @see TestLevel
 * @see SpecificationRefs
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(SpecificationRefs.class)
public @interface SpecificationRef {
    /** Scenario identifier, e.g. {@code "US-07.AC-1"} or {@code "US-07.FIFO-2"}. */
    String value();

    /** Whether this test operates at the domain or integration level. */
    TestLevel level();

    /** Optional reference to the {@code .feature} file containing the Gherkin scenario. */
    String feature() default "";
}
