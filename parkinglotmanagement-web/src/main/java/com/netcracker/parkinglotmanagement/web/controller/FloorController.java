package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.FloorDTO;
import com.netcracker.parkinglotmanagement.api.service.FloorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/floors")
@Tag(name = "Floors", description = "Levels within a block")
public class FloorController {

    private final FloorService floorService;

    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    @PostMapping("/create-floors")
    @Operation(summary = "Create a floor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<FloorDTO> createFloor(@Valid @RequestBody FloorDTO floorDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(floorService.createFloor(floorDTO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one floor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "No such floor")
    })
    public ResponseEntity<FloorDTO> getFloorById(@PathVariable UUID id) {
        return ResponseEntity.ok(floorService.getFloorById(id));
    }

    @GetMapping
    @Operation(summary = "List every floor")
    public ResponseEntity<List<FloorDTO>> getAllFloors() {
        return ResponseEntity.ok(floorService.getAllFloors());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a floor")
    public ResponseEntity<FloorDTO> updateFloor(@PathVariable UUID id, @Valid @RequestBody FloorDTO floorDTO) {
        floorDTO.setId(id);
        return ResponseEntity.ok(floorService.updateFloor(floorDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a floor")
    @ApiResponse(responseCode = "204", description = "Deleted")
    public ResponseEntity<Void> deleteFloor(@PathVariable UUID id) {
        floorService.deleteFloor(id);
        return ResponseEntity.noContent().build();
    }
}
