package com.warehouse.flow.engine.event;

import java.time.Instant;
import java.util.UUID;

public interface WarehouseEvent {

    WarehouseEventType eventType();

    public static WarehouseEventEnvelope envelope(String type, WarehouseEvent payload) {
        return new WarehouseEventEnvelope(UUID.randomUUID().toString(), type, Instant.now(), payload);
    }
}
