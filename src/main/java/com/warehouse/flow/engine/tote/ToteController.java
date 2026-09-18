package com.warehouse.flow.engine.tote;

import com.warehouse.flow.engine.routes.RoutingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/totes")
public class ToteController {

    private final RoutingService routingService;
    private final ToteService toteService;

    public ToteController(RoutingService routingService, ToteService toteService) {
        this.routingService = routingService;
        this.toteService = toteService;
    }

    @PostMapping("/{toteId}/route")
    public RouteResponse calculateRoute(@PathVariable String toteId, @Valid @RequestBody ToteRouteRequest request) {

        Tote tote = new Tote(toteId, request.currentNode(), request.destination(), request.priority(), ToteStatus.CREATED);

        return routingService.calculateRoute(tote);
    }


    @PostMapping("/{toteId}/start")
    public RouteResponse startTote(@PathVariable String toteId, @Valid @RequestBody ToteRouteRequest request) {

        Tote tote = new Tote(toteId, request.currentNode(), request.destination(), request.priority(), ToteStatus.CREATED);

        return toteService.startTote(tote);
    }

    @GetMapping("/{toteId}")
    public Tote getTote(@PathVariable String toteId) {
        return toteService.getTote(toteId);
    }
}
