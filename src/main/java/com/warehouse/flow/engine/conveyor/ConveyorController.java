package com.warehouse.flow.engine.conveyor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conveyors")
public class ConveyorController {

    private final ConveyorStateService conveyorStateService;

    public ConveyorController(ConveyorStateService conveyorStateService) {
        this.conveyorStateService = conveyorStateService;
    }

    @PatchMapping("/{from}/{to}/block")
    public ConveyorState block(@PathVariable String from, @PathVariable String to) {
        conveyorStateService.block(from, to);
        return conveyorStateService.getState(from, to);
    }

    @PatchMapping("/{from}/{to}/unblock")
    public ConveyorState unblock(@PathVariable String from, @PathVariable String to) {
        conveyorStateService.unblock(from, to);
        return conveyorStateService.getState(from, to);
    }

    @GetMapping("/{from}/{to}")
    public ConveyorState getState(@PathVariable String from, @PathVariable String to) {
        return conveyorStateService.getState(from, to);
    }

    @PostMapping("/{from}/{to}/enter")
    public ConveyorState enter(@PathVariable String from, @PathVariable String to) {
        conveyorStateService.enter(from, to);
        return conveyorStateService.getState(from, to);
    }
    @PostMapping("/{from}/{to}/leave")
    public ConveyorState leave(@PathVariable String from, @PathVariable String to) {
        conveyorStateService.leave(from, to);
        return conveyorStateService.getState(from, to);
    }
}