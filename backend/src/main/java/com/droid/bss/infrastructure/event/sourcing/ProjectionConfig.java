package com.droid.bss.infrastructure.event.sourcing;

import com.droid.bss.infrastructure.event.sourcing.projections.CustomerReadModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for projections
 */
@Configuration
public class ProjectionConfig {

    @Bean
    public ProjectionManager projectionManager(EventStore eventStore) {
        ProjectionManager manager = new ProjectionManager(eventStore);

        // Register built-in projections
        manager.registerProjection(new CustomerReadModel());

        return manager;
    }

    @Bean
    public Projection customerReadModel() {
        return new CustomerReadModel();
    }
}
