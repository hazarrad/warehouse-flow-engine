package com.warehouse.flow.engine.topology;

public record Edge(
        String from,
        String to,
        int travelTimeSeconds,
        int capacity
) {
}
