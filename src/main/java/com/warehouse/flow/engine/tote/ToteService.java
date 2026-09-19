package com.warehouse.flow.engine.tote;

import com.warehouse.flow.engine.conveyor.ConveyorStateService;
import com.warehouse.flow.engine.recovery.RecoveryService;
import com.warehouse.flow.engine.routes.RoutingService;
import com.warehouse.flow.engine.topology.Edge;
import com.warehouse.flow.engine.topology.WarehouseGraph;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ToteService {

    private final RoutingService routingService;
    private final ConveyorStateService conveyorStateService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final WarehouseGraph warehouseGraph;
    private final ToteMovementSimulator movementSimulator;
    private final RecoveryService recoveryService;

    private final Map<String, Tote> totes = new ConcurrentHashMap<>();

    public ToteService(RoutingService routingService, ConveyorStateService conveyorStateService, WarehouseGraph warehouseGraph, ToteMovementSimulator movementSimulator, RecoveryService recoveryService) {
        this.routingService = routingService;
        this.conveyorStateService = conveyorStateService;
        this.warehouseGraph = warehouseGraph;
        this.movementSimulator = movementSimulator;
        this.recoveryService = recoveryService;
    }

    public RouteResponse startTote(Tote tote) {

        Tote existingTote = totes.get(tote.id());
        if (existingTote != null && existingTote.status() == ToteStatus.MOVING) {
            throw new IllegalStateException("Tote is already moving: " + tote.id());
        }

        RouteResponse route = routingService.calculateRoute(tote);
        Tote movingTote = new Tote(tote.id(), tote.currentNode(), tote.destination(), tote.priority(), ToteStatus.MOVING);
        totes.put(tote.id(), movingTote);

        executorService.submit(() -> executeRoute(tote.id(), route.route()));

        return route;
    }

    private void executeRoute(String toteId, List<String> route) {

        for (int i = 0; i < route.size() - 1; i++) {
            String from = route.get(i);
            String to = route.get(i + 1);
            Edge edge = findEdge(from, to);

            try {
                moveTote(toteId, edge);
            } catch (IllegalStateException e) {
                handleMovementFailure(toteId);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                markAsWaiting(toteId);
                return;
            }
        }
        markAsDelivered(toteId);
    }


    private void moveTote(String toteId, Edge edge) throws InterruptedException {

        Tote tote = getTote(toteId);
        String from = edge.from();
        String to = edge.to();

        if (!tote.currentNode().equals(from)) {
            throw new IllegalStateException("Tote " + toteId + " is not at " + from);
        }

        // Fails if conveyor is BLOCKED or FULL
        conveyorStateService.enter(from, to);

        updateTote(tote, from, ToteStatus.MOVING);

        // Simulate physical movement
        // Thread.sleep(edge.travelTimeSeconds() * 1000L);
        movementSimulator.move(edge.travelTimeSeconds());

        // Leave conveyor
        conveyorStateService.leave(from, to);

        // Arrive at next node
        updateTote(getTote(toteId), to, ToteStatus.MOVING);
    }

    public void resumeTote(String toteId, List<String> route) {

        Tote tote = getTote(toteId);
        if (tote.status() != ToteStatus.WAITING) {
            throw new IllegalStateException("Tote is not waiting: " + toteId);
        }

        updateTote(tote, tote.currentNode(), ToteStatus.MOVING);
        executorService.submit(() -> executeRoute(toteId, route));
    }

    private Edge findEdge(String from, String to) {

        return warehouseGraph.getOutgoingEdges(from).stream().filter(edge -> edge.to().equals(to)).findFirst().orElseThrow(() -> new IllegalStateException("Unknown edge: " + from + " -> " + to));
    }

    private void handleMovementFailure(String toteId) {

        Tote tote = getTote(toteId);
        markAsWaiting(toteId);

        try {
            RouteResponse recoveryRoute = recoveryService.calculateRecoveryRoute(getTote(toteId));
            resumeTote(toteId, recoveryRoute.route());
        } catch (Exception e) {
            // No alternative route available
            // Tote remains WAITING
        }
    }

    private void markAsDelivered(String toteId) {

        Tote tote = getTote(toteId);
        updateTote(tote, tote.currentNode(), ToteStatus.DELIVERED);
    }

    private void markAsWaiting(String toteId) {

        Tote tote = getTote(toteId);
        updateTote(tote, tote.currentNode(), ToteStatus.WAITING);
    }

    private void updateTote(Tote tote, String currentNode, ToteStatus status) {

        totes.put(tote.id(), new Tote(tote.id(), currentNode, tote.destination(), tote.priority(), status));
    }

    public Tote getTote(String toteId) {

        Tote tote = totes.get(toteId);
        if (tote == null) {
            throw new IllegalArgumentException("Unknown tote: " + toteId);
        }
        return tote;
    }
}
