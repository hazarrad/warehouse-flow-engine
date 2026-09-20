package com.warehouse.flow.engine.event;

public record ConveyorBlockedEvent(
        String from,
        String to
) implements WarehouseEvent {}