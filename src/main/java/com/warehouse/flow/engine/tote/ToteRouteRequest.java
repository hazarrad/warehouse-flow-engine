package com.warehouse.flow.engine.tote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ToteRouteRequest(
        @NotBlank
        String currentNode,

        @NotBlank
        String destination,

        @NotNull
        TotePriority priority
) {
}