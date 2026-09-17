package com.warehouse.flow.engine.conveyor;

import com.warehouse.flow.engine.configuration.CongestionConfig;
import com.warehouse.flow.engine.topology.Edge;
import com.warehouse.flow.engine.topology.WarehouseGraph;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConveyorStateService {

    private final Map<String, ConveyorState> states = new ConcurrentHashMap<>();

    private CongestionConfig congestionConfig;
    public ConveyorStateService(WarehouseGraph warehouseGraph, CongestionConfig congestionConfig) {
        this.congestionConfig = congestionConfig;
        for (Edge edge : warehouseGraph.getEdges()) {
            states.put(key(edge), new ConveyorState(edge.capacity(), 0, ConveyorStatus.ACTIVE));
        }
    }

    public ConveyorState getState(String from, String to) {

        ConveyorState state = states.get(key(from, to));

        if (state == null) {
            throw new IllegalArgumentException("Unknown conveyor: " + from + " -> " + to);
        }

        return state;
    }

    public boolean isAvailable(String from, String to) {
        return getState(from, to).isAvailable();
    }

    public void block(String from, String to) {

        ConveyorState current = getState(from, to);

        states.put(key(from, to), new ConveyorState(current.capacity(), current.occupancy(), ConveyorStatus.BLOCKED));
    }

    public void unblock(String from, String to) {

        ConveyorState current = getState(from, to);

        states.put(key(from, to), new ConveyorState(current.capacity(), current.occupancy(), ConveyorStatus.ACTIVE));
    }

    public void reset() {
        states.replaceAll((key, state) -> new ConveyorState(state.capacity(), 0, ConveyorStatus.ACTIVE));
    }

    public void enter(String from, String to) {

        ConveyorState current = getState(from, to);

        if (!current.isAvailable()) {
            throw new IllegalStateException("Conveyor is not available: " + from + " -> " + to);
        }

        states.put(key(from, to), new ConveyorState(current.capacity(), current.occupancy() + 1, current.status()));
    }

    public void leave(String from, String to) {

        ConveyorState current = getState(from, to);

        if (current.occupancy() <= 0) {
            throw new IllegalStateException("Conveyor occupancy cannot be negative");
        }

        states.put(key(from, to), new ConveyorState(current.capacity(), current.occupancy() - 1, current.status()));
    }


    private String key(Edge edge) {
        return key(edge.from(), edge.to());
    }

    private String key(String from, String to) {
        return from + "->" + to;
    }

    public CongestionLevel getCongestionLevel(String from, String to) {

        ConveyorState state = getState(from, to);

        double ratio = state.congestionRatio();

        if (ratio >= congestionConfig.getHighThreshold()) {
            return CongestionLevel.HIGH;
        }

        if (ratio >= congestionConfig.getMediumThreshold()) {
            return CongestionLevel.MEDIUM;
        }

        return CongestionLevel.LOW;
    }
}
