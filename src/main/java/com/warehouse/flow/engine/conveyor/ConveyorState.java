package com.warehouse.flow.engine.conveyor;

public record ConveyorState(
        int capacity,
        int occupancy,
        ConveyorStatus status
) {

    public boolean isAvailable() {
        return status == ConveyorStatus.ACTIVE && occupancy < capacity;
    }

    public double congestionRatio() {
        return (double) occupancy / capacity;
    }
}