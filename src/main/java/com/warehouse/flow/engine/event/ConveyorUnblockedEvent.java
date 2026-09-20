package com.warehouse.flow.engine.event;

public record ConveyorUnblockedEvent(
        String from,
        String to
) implements WarehouseEvent {}