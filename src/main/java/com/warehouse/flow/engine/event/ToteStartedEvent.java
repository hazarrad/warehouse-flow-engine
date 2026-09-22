package com.warehouse.flow.engine.event;

public record ToteStartedEvent(
        String toteId,
        String currentNode,
        String destination
) implements WarehouseEvent {

    @Override
    public WarehouseEventType eventType() {
        return WarehouseEventType.TOTE_STARTED;
    }
}