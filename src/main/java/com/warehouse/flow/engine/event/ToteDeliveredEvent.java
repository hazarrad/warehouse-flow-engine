package com.warehouse.flow.engine.event;

public record ToteDeliveredEvent(
        String toteId,
        String destination
) implements WarehouseEvent {

    @Override
    public WarehouseEventType eventType() {
        return WarehouseEventType.TOTE_DELIVERED;
    }
}