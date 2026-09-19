package com.warehouse.flow.engine.recovery;

import com.warehouse.flow.engine.routes.RoutingService;
import com.warehouse.flow.engine.tote.RouteResponse;
import com.warehouse.flow.engine.tote.Tote;
import com.warehouse.flow.engine.tote.ToteStatus;
import org.springframework.stereotype.Service;

@Service
public class RecoveryService {

    private final RoutingService routingService;

    public RecoveryService(RoutingService routingService) {
        this.routingService = routingService;
    }

    public RouteResponse calculateRecoveryRoute(Tote tote) {
        if (tote.status() != ToteStatus.WAITING) {
            throw new IllegalStateException("Tote is not waiting: " + tote.id());
        }
        return routingService.calculateRoute(tote);
    }
}