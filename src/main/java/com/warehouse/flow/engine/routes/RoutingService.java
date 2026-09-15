package com.warehouse.flow.engine.routes;

import com.warehouse.flow.engine.conveyor.ConveyorStateService;
import com.warehouse.flow.engine.topology.Edge;
import com.warehouse.flow.engine.topology.NodeDistance;
import com.warehouse.flow.engine.topology.WarehouseGraph;
import com.warehouse.flow.engine.tote.RouteResponse;
import com.warehouse.flow.engine.tote.Tote;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RoutingService {

    private final WarehouseGraph warehouseGraph;
    private final ConveyorStateService conveyorStateService;

    public RoutingService(WarehouseGraph warehouseGraph, ConveyorStateService conveyorStateService) {
        this.warehouseGraph = warehouseGraph;
        this.conveyorStateService = conveyorStateService;
    }

    private void validateNodes(String start, String destination) {
        warehouseGraph.getNode(start);
        warehouseGraph.getNode(destination);
    }

    public RouteResponse calculateRoute(Tote tote) {

        String start = tote.currentNode();
        String destination = tote.destination();

        validateNodes(start, destination);

        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previousNode = new HashMap<>();

        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingInt(NodeDistance::distance));
        distances.put(start, 0);
        queue.add(new NodeDistance(start, 0));

        while (!queue.isEmpty()) {

            NodeDistance current = queue.poll();
            String currentNode = current.node();
            int currentDistance = current.distance();

            //Ignore outdated queue entries
            if (currentDistance > distances.getOrDefault(currentNode, Integer.MAX_VALUE)) {
                continue;
            }

            //Destination reached
            if (currentNode.equals(destination)) {
                break;
            }

            //Explore outgoing edges
            for (Edge edge : warehouseGraph.getOutgoingEdges(currentNode)) {

                if (!conveyorStateService.isAvailable(edge.from(), edge.to())) {
                    continue;
                }

                int newDistance = currentDistance + edge.travelTimeSeconds();
                int existingDistance = distances.getOrDefault(edge.to(), Integer.MAX_VALUE);

                if (newDistance < existingDistance) {
                    distances.put(edge.to(), newDistance);
                    previousNode.put(edge.to(), currentNode);
                    queue.add(new NodeDistance(edge.to(), newDistance));
                }
            }
        }

        if (!distances.containsKey(destination)) {
            throw new IllegalStateException("No route found from " + start + " to " + destination);
        }


        List<String> route = buildRoute(start, destination, previousNode);
        return new RouteResponse(tote.id(), route, distances.get(destination));
    }

    private List<String> buildRoute(String start, String destination, Map<String, String> previousNode) {

        LinkedList<String> route = new LinkedList<>();
        String current = destination;

        while (current != null) {
            route.addFirst(current);
            if (current.equals(start)) {
                break;
            }
            current = previousNode.get(current);
        }

        return route;
    }
}
