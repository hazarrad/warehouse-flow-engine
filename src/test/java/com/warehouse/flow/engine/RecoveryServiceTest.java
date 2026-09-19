package com.warehouse.flow.engine;

import com.warehouse.flow.engine.recovery.RecoveryService;
import com.warehouse.flow.engine.routes.RoutingService;
import com.warehouse.flow.engine.tote.RouteResponse;
import com.warehouse.flow.engine.tote.Tote;
import com.warehouse.flow.engine.tote.TotePriority;
import com.warehouse.flow.engine.tote.ToteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecoveryServiceTest {

    @Mock
    private RoutingService routingService;

    private RecoveryService recoveryService;

    @BeforeEach
    void setUp() {
        recoveryService = new RecoveryService(routingService);
    }

    @Test
    void shouldCalculateRecoveryRouteForWaitingTote() {

        Tote tote = new Tote("TOTE-001", "C03", "PACKING-01", TotePriority.NORMAL, ToteStatus.WAITING);
        RouteResponse expectedRoute = new RouteResponse("TOTE-001", List.of("C03", "C04", "C05", "C06", "C11", "PACKING-01"), 13, 13);

        when(routingService.calculateRoute(tote)).thenReturn(expectedRoute);

        RouteResponse result = recoveryService.calculateRecoveryRoute(tote);

        assertEquals(expectedRoute, result);

        verify(routingService).calculateRoute(tote);
    }

    @Test
    void shouldRejectRecoveryWhenToteIsNotWaiting() {

        Tote tote = new Tote("TOTE-001", "C03", "PACKING-01", TotePriority.NORMAL, ToteStatus.MOVING);

        assertThrows(IllegalStateException.class, () -> recoveryService.calculateRecoveryRoute(tote));

        verifyNoInteractions(routingService);
    }
}