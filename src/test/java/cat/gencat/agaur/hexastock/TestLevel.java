package cat.gencat.agaur.hexastock;

/**
 * Indicates the architectural level at which a test verifies a specification scenario.
 *
 * <ul>
 *   <li>{@link #DOMAIN} — Pure domain tests that run without infrastructure
 *       (no database, no HTTP, no Spring context). These verify business rules
 *       and aggregate invariants in isolation.</li>
 *   <li>{@link #INTEGRATION} — Full-stack tests that exercise the complete
 *       request path: HTTP → Controller → Service → Domain → Persistence.
 *       These verify that all layers collaborate correctly.</li>
 * </ul>
 *
 * Used together with {@link SpecificationRef} to record both <em>what</em>
 * scenario a test verifies and <em>at which level</em> it does so.
 *
 * @see SpecificationRef
 */
public enum TestLevel {
    /** Pure domain test — no infrastructure, no framework. */
    DOMAIN,
    /** Full-stack integration test — HTTP through to persistence. */
    INTEGRATION
}
