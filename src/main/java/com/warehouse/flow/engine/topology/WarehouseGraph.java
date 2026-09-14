package com.warehouse.flow.engine.topology;

import com.warehouse.flow.engine.configuration.WarehouseGraphConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class WarehouseGraph {

    private final Map<String, Node> nodes;

    private final List<Edge> edges;

    private final WarehouseGraphConfig warehouseGraphConfig;

    public WarehouseGraph(WarehouseGraphConfig warehouseGraphConfig) {
        this.warehouseGraphConfig = warehouseGraphConfig;
        this.nodes = warehouseGraphConfig.getNodes().stream().collect(Collectors.toMap(Node::id, Function.identity()));

        this.edges = warehouseGraphConfig.getEdges();
    }

    public Node getNode(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }

        return node;
    }

    public List<Edge> getOutgoingEdges(String nodeId) {

        return edges.stream().filter(edge -> edge.from().equals(nodeId)).toList();
    }
}
