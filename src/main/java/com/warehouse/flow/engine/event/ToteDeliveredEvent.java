package com.warehouse.flow.engine.event;

public record ToteDeliveredEvent(
        String toteId,
        String destination
) implements WarehouseEvent {}