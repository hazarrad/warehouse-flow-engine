package com.warehouse.flow.engine.event;

public interface EventPublisher {

    void publish(WarehouseEventEnvelope event);
}
