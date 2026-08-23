package cat.gencat.agaur.hexastock.config.events;

import cat.gencat.agaur.hexastock.application.port.out.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Default infrastructure adapter for {@link DomainEventPublisher}.
 *
 * <p>Delegates to Spring's {@link ApplicationEventPublisher} so the application service
 * stays Spring-free while events are still routed in-process to
 * {@code @ApplicationModuleListener}-annotated consumers (Notifications module).</p>
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
