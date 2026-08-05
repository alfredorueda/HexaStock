package cat.gencat.agaur.hexastock.application.port.out;

/**
 * Outbound port that lets pure application services publish domain events without
 * depending on Spring (or any other framework) at the application layer.
 *
 * <p>The infrastructure layer provides an adapter (typically backed by Spring's
 * {@code ApplicationEventPublisher}) so events can be consumed by in-process
 * listeners — for instance the Notifications module's
 * {@code @ApplicationModuleListener}.</p>
 */
public interface DomainEventPublisher {

    /**
     * Publishes a domain event for in-process consumption.
     *
     * @param event the event to publish; must be a plain POJO with no infrastructure
     *              dependencies
     */
    void publish(Object event);
}
