package com.warehouse.flow.engine.tote;

import java.util.List;

public record RouteResponse(
        String toteId,
        List<String> route,
        int estimatedTravelTimeSeconds,
        int routingCost
) {
}