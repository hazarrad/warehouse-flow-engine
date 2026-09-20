package com.warehouse.flow.engine.event;

import java.util.List;

public record ToteReroutedEvent(
        String toteId,
        String currentNode,
        String destination,
        List<String> route
) implements WarehouseEvent  {}