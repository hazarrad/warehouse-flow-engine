package com.warehouse.flow.engine.event;

import java.time.Instant;

public record WarehouseEventEnvelope(
        String eventId,
        String eventType,
        Instant timestamp,
        WarehouseEvent payload
) {
}
