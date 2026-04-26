package cat.gencat.agaur.hexastock.config.events;

import cat.gencat.agaur.hexastock.application.port.out.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(SpringDomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        log.info("DOMAIN_EVENT_PUBLISHED type={} payload={}",
                event.getClass().getName(), event);
        applicationEventPublisher.publishEvent(event);
    }
}
