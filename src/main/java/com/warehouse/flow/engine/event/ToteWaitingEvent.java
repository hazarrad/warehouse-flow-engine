package com.warehouse.flow.engine.event;

public record ToteWaitingEvent(
        String toteId,
        String currentNode,
        String destination
) implements WarehouseEvent {}