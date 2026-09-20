package com.warehouse.flow.engine.event;

public enum WarehouseEventType {

    TOTE_STARTED,
    TOTE_MOVING,
    TOTE_WAITING,
    TOTE_REROUTED,
    TOTE_DELIVERED,

    CONVEYOR_BLOCKED,
    CONVEYOR_UNBLOCKED
}