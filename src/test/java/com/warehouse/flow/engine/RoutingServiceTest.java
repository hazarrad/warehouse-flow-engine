package com.warehouse.flow.engine;

import com.warehouse.flow.engine.conveyor.ConveyorState;
import com.warehouse.flow.engine.conveyor.ConveyorStateService;
import com.warehouse.flow.engine.routes.RoutingService;
import com.warehouse.flow.engine.tote.RouteResponse;
import com.warehouse.flow.engine.tote.Tote;
import com.warehouse.flow.engine.tote.TotePriority;
import com.warehouse.flow.engine.tote.ToteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RoutingServiceTest {

    @Autowired
    private RoutingService routingService;

    @Autowired
    private ConveyorStateService conveyorStateService;

    @BeforeEach
    void resetConveyorState() {
        conveyorStateService.reset();
    }

    @Test
    void shouldFindShortestRoute() {

        Tote tote = new Tote("TOTE-001", "C03", "PACKING-01", TotePriority.NORMAL, ToteStatus.CREATED);
        RouteResponse response = routingService.calculateRoute(tote);

        assertEquals(List.of("C03", "C09", "C10", "C11", "PACKING-01"), response.route());
        assertEquals(8, response.estimatedTravelTimeSeconds());
    }

    @Test
    void shouldCalculateRouteFromEntry() {

        Tote tote = new Tote("TOTE-002", "ENTRY", "PACKING-01", TotePriority.NORMAL, ToteStatus.CREATED);
        RouteResponse response = routingService.calculateRoute(tote);

        assertEquals(List.of("ENTRY", "C01", "C02", "C03", "C09", "C10", "C11", "PACKING-01"), response.route());
        assertEquals(14, response.estimatedTravelTimeSeconds());
    }

    @Test
    void shouldRejectUnknownStartNode() {

        Tote tote = new Tote("TOTE-003", "UNKNOWN", "PACKING-01", TotePriority.NORMAL, ToteStatus.CREATED);

        assertThrows(IllegalArgumentException.class, () -> routingService.calculateRoute(tote));
    }

    @Test
    void shouldAvoidBlockedConveyor() {

        conveyorStateService.block("C03", "C09");

        Tote tote = new Tote("TOTE-006", "C03", "PACKING-01", TotePriority.NORMAL, ToteStatus.CREATED);
        RouteResponse response = routingService.calculateRoute(tote);

        assertEquals(List.of("C03", "C04", "C05", "C06", "C11", "PACKING-01"), response.route());
        assertEquals(13, response.estimatedTravelTimeSeconds());
    }

    @Test
    void shouldUseConveyorAfterUnblocking() {

        conveyorStateService.block("C03", "C09");
        conveyorStateService.unblock("C03", "C09");

        Tote tote = new Tote("TOTE-007", "C03", "PACKING-01", TotePriority.NORMAL, ToteStatus.CREATED);
        RouteResponse response = routingService.calculateRoute(tote);

        assertEquals(List.of("C03", "C09", "C10", "C11", "PACKING-01"), response.route());
        assertEquals(8, response.estimatedTravelTimeSeconds());
    }

    @Test
    void shouldIncreaseOccupancyWhenToteEnters() {

        conveyorStateService.enter("C03", "C09");
        ConveyorState state = conveyorStateService.getState("C03", "C09");

        assertEquals(1, state.occupancy());
    }

    @Test
    void shouldDecreaseOccupancyWhenToteLeaves() {

        conveyorStateService.enter("C03", "C09");
        conveyorStateService.leave("C03", "C09");
        ConveyorState state = conveyorStateService.getState("C03", "C09");

        assertEquals(0, state.occupancy());
    }


    @Test
    void shouldRejectToteWhenConveyorIsFull() {

        conveyorStateService.enter("C03", "C09");
        conveyorStateService.enter("C03", "C09");
        conveyorStateService.enter("C03", "C09");

        assertThrows(IllegalStateException.class, () -> conveyorStateService.enter("C03", "C09"));
    }

    @Test
    void shouldRejectLeavingEmptyConveyor() {

        assertThrows(IllegalStateException.class, () -> conveyorStateService.leave("C03", "C09"));
    }

    @Test
    void shouldAvoidFullConveyor() {

        conveyorStateService.enter("C03", "C09");
        conveyorStateService.enter("C03", "C09");
        conveyorStateService.enter("C03", "C09");

        Tote tote = new Tote("TOTE-001", "C03", "PACKING-01", TotePriority.NORMAL, ToteStatus.CREATED);
        RouteResponse response = routingService.calculateRoute(tote);

        assertEquals(List.of("C03", "C04", "C05", "C06", "C11", "PACKING-01"), response.route());
        assertEquals(13, response.estimatedTravelTimeSeconds());
    }

}