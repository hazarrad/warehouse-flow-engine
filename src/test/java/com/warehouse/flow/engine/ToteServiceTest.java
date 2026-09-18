package com.warehouse.flow.engine;

import com.warehouse.flow.engine.conveyor.ConveyorStateService;
import com.warehouse.flow.engine.routes.RoutingService;
import com.warehouse.flow.engine.topology.Edge;
import com.warehouse.flow.engine.topology.WarehouseGraph;
import com.warehouse.flow.engine.tote.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToteServiceTest {

    @Mock
    private RoutingService routingService;

    @Mock
    private ConveyorStateService conveyorStateService;

    @Mock
    private WarehouseGraph warehouseGraph;

    private ToteService toteService;

    @Mock
    private ToteMovementSimulator movementSimulator;

    @BeforeEach
    void setUp() {
        toteService = new ToteService(routingService, conveyorStateService, warehouseGraph, movementSimulator);
    }

    @Test
    void shouldStartTote() {

        Tote tote = createTote("TOTE-001", "C03", "PACKING-01");
        RouteResponse route = createRoute("TOTE-001", "C03", "C09", "C10", "C11", "PACKING-01");

        when(routingService.calculateRoute(tote)).thenReturn(route);

        toteService.startTote(tote);

        verify(routingService).calculateRoute(tote);
        assertEventually("TOTE-001", result -> assertEquals(ToteStatus.MOVING, result.status()));
    }

    @Test
    void shouldStartToteAndMoveItToDestination() {

        Tote tote = new Tote("TOTE-001", "C03", "C09", TotePriority.NORMAL, ToteStatus.CREATED);
        Edge edge = new Edge("C03", "C09", 0, 3);
        RouteResponse route = new RouteResponse("TOTE-001", List.of("C03", "C09"), 0, 0);

        when(routingService.calculateRoute(tote)).thenReturn(route);
        when(warehouseGraph.getOutgoingEdges("C03")).thenReturn(List.of(edge));

        toteService.startTote(tote);

        verify(movementSimulator, timeout(1000)).move(0);
        verify(conveyorStateService, timeout(1000)).enter("C03", "C09");

        Tote result = toteService.getTote("TOTE-001");

        assertEquals("C09", result.currentNode());
        assertEquals(ToteStatus.DELIVERED, result.status());
    }

    @Test
    void shouldMoveToteThroughRoute() {

        Tote tote = createTote("TOTE-001", "C03", "C10");
        Edge edge1 = new Edge("C03", "C09", 2, 3);
        Edge edge2 = new Edge("C09", "C10", 2, 3);
        RouteResponse route = new RouteResponse("TOTE-001", List.of("C03", "C09", "C10"), 4, 4);

        when(routingService.calculateRoute(tote)).thenReturn(route);
        when(warehouseGraph.getOutgoingEdges("C03")).thenReturn(List.of(edge1));
        when(warehouseGraph.getOutgoingEdges("C09")).thenReturn(List.of(edge2));

        toteService.startTote(tote);

        verify(movementSimulator, timeout(1000).times(2)).move(2);
        verify(conveyorStateService, timeout(1000)).enter("C03", "C09");
        verify(conveyorStateService, timeout(1000)).leave("C03", "C09");
        verify(conveyorStateService, timeout(1000)).enter("C09", "C10");
        verify(conveyorStateService, timeout(1000)).leave("C09", "C10");
    }

    @Test
    void shouldMarkToteAsDelivered() {

        Tote tote = createTote("TOTE-001", "C03", "C09");
        Edge edge = new Edge("C03", "C09", 2, 3);
        RouteResponse route = new RouteResponse("TOTE-001", List.of("C03", "C09"), 2, 2);

        when(routingService.calculateRoute(tote)).thenReturn(route);
        when(warehouseGraph.getOutgoingEdges("C03")).thenReturn(List.of(edge));

        toteService.startTote(tote);

        assertEventually("TOTE-001", result -> {
            assertEquals("C09", result.currentNode());
            assertEquals(ToteStatus.DELIVERED, result.status());
        });
    }

    @Test
    void shouldPutToteInWaitingWhenConveyorUnavailable() {

        Tote tote = createTote("TOTE-001", "C03", "C09");
        Edge edge = new Edge("C03", "C09", 2, 3);
        RouteResponse route = new RouteResponse("TOTE-001", List.of("C03", "C09"), 2, 2);

        when(routingService.calculateRoute(tote)).thenReturn(route);
        when(warehouseGraph.getOutgoingEdges("C03")).thenReturn(List.of(edge));

        doThrow(new IllegalStateException("Conveyor unavailable")).when(conveyorStateService).enter("C03", "C09");

        toteService.startTote(tote);

        assertEventually("TOTE-001", result -> {
            assertEquals("C03", result.currentNode());
            assertEquals(ToteStatus.WAITING, result.status());
        });

        verify(movementSimulator, never()).move(anyInt());
    }

    @Test
    void shouldRejectToteAlreadyMoving() {

        Tote tote = createTote("TOTE-001", "C03", "C09");
        RouteResponse route = new RouteResponse("TOTE-001", List.of("C03", "C09"), 2, 2);

        when(routingService.calculateRoute(tote)).thenReturn(route);

        toteService.startTote(tote);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> toteService.startTote(tote));

        assertEquals("Tote is already moving: TOTE-001", exception.getMessage());
    }

    @Test
    void shouldAllowRestartingDeliveredTote() {

        Tote tote = createTote("TOTE-001", "C03", "C09");
        Edge edge = new Edge("C03", "C09", 2, 3);
        RouteResponse route = new RouteResponse("TOTE-001", List.of("C03", "C09"), 2, 2);

        when(routingService.calculateRoute(tote)).thenReturn(route);
        when(warehouseGraph.getOutgoingEdges("C03")).thenReturn(List.of(edge));

        // First journey
        toteService.startTote(tote);

        assertEventually("TOTE-001", result -> assertEquals(ToteStatus.DELIVERED, result.status()));

        // Second journey
        toteService.startTote(tote);

        assertEventually("TOTE-001", result -> assertEquals(ToteStatus.MOVING, result.status()));

        verify(routingService, times(2)).calculateRoute(tote);
    }

    @Test
    void shouldEnterAndLeaveEachConveyor() {

        Tote tote = createTote("TOTE-001", "C03", "C10");
        Edge edge1 = new Edge("C03", "C09", 2, 3);
        Edge edge2 = new Edge("C09", "C10", 2, 3);

        RouteResponse route = new RouteResponse("TOTE-001", List.of("C03", "C09", "C10"), 4, 4);

        when(routingService.calculateRoute(tote)).thenReturn(route);
        when(warehouseGraph.getOutgoingEdges("C03")).thenReturn(List.of(edge1));
        when(warehouseGraph.getOutgoingEdges("C09")).thenReturn(List.of(edge2));

        toteService.startTote(tote);

        verify(conveyorStateService, timeout(1000)).enter("C03", "C09");
        verify(conveyorStateService, timeout(1000)).leave("C03", "C09");
        verify(conveyorStateService, timeout(1000)).enter("C09", "C10");
        verify(conveyorStateService, timeout(1000)).leave("C09", "C10");
    }


    private Tote createTote(String id, String currentNode, String destination) {
        return new Tote(id, currentNode, destination, TotePriority.NORMAL, ToteStatus.CREATED);
    }

    private RouteResponse createRoute(String toteId, String... nodes) {
        return new RouteResponse(toteId, List.of(nodes), 10, 10);
    }

    private void assertEventually(String toteId, Consumer<Tote> assertion) {
        AssertionError lastError = null;

        for (int i = 0; i < 100; i++) {
            try {
                assertion.accept(toteService.getTote(toteId));
                return;
            } catch (AssertionError e) {
                lastError = e;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        throw lastError;
    }

}
