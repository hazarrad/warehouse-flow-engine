package com.warehouse.flow.engine.tote;

import org.springframework.stereotype.Component;

@Component
public class RealToteMovementSimulator implements ToteMovementSimulator {

    @Override
    public void move(int travelTimeSeconds) {
        try {
            Thread.sleep(travelTimeSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tote movement was interrupted", e);
        }
    }
}