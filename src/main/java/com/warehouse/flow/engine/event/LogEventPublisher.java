package com.warehouse.flow.engine.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LogEventPublisher.class);

    @Override
    public void publish(WarehouseEvent event) {
        log.info("Warehouse event: {}", event);
    }
}
