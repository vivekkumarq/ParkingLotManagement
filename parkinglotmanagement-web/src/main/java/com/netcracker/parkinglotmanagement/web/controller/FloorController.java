package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.FloorDTO;
import com.netcracker.parkinglotmanagement.api.service.FloorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/floors")
public class FloorController {

    private final FloorService floorService;

    @Autowired
    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    @PostMapping("/create-floors")
    public ResponseEntity<FloorDTO> createFloor(@RequestBody FloorDTO floorDTO) {
        FloorDTO createdFloor = floorService.createFloor(floorDTO);
        return new ResponseEntity<>(createdFloor, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FloorDTO> getFloorById(@PathVariable UUID id) {
        FloorDTO floorDTO = floorService.getFloorById(id);
        return floorDTO != null ? ResponseEntity.ok(floorDTO) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<FloorDTO>> getAllFloors() {
        List<FloorDTO> floors = floorService.getAllFloors();
        return ResponseEntity.ok(floors);
    }

    @PutMapping
    public ResponseEntity<FloorDTO> updateFloor(@RequestBody FloorDTO floorDTO) {
        FloorDTO updatedFloor = floorService.updateFloor(floorDTO);
        return ResponseEntity.ok(updatedFloor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFloor(@PathVariable UUID id) {
        floorService.deleteFloor(id);
        return ResponseEntity.noContent().build();
    }
}