package com.warehouse.flow.engine.tote;

public record Tote(
        String id,
        String currentNode,
        String destination,
        TotePriority priority,
        ToteStatus status
) {
}