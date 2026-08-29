package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.OccupancySummaryDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.dto.SlotAvailabilityDTO;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/slots")
@Tag(name = "Parking slots", description = "The physical bays and their live availability")
public class ParkingSlotController {

    private final ParkingSlotService parkingSlotService;

    public ParkingSlotController(ParkingSlotService parkingSlotService) {
        this.parkingSlotService = parkingSlotService;
    }

    @PostMapping
    @Operation(summary = "Create a slot")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Slot created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<ParkingSlotDTO> createSlot(@Valid @RequestBody ParkingSlotDTO slot) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSlotService.createSlot(slot));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one slot")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "No such slot")
    })
    public ResponseEntity<ParkingSlotDTO> getSlot(@PathVariable UUID id) {
        return ResponseEntity.ok(parkingSlotService.getSlotById(id));
    }

    @GetMapping
    @Operation(summary = "List slots",
            description = "Optional RSQL filter over id, floorId, slotNumber, charges, vehicleType "
                    + "and status. Example: vehicleType==CAR;status==FREE")
    public ResponseEntity<List<ParkingSlotDTO>> listSlots(
            @Parameter(description = "RSQL filter", example = "status==FREE;vehicleType==CAR")
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(parkingSlotService.getAllSlots(search));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a slot's description",
            description = "Status and version are owned by the allocator and are not writable here.")
    public ResponseEntity<ParkingSlotDTO> updateSlot(@PathVariable UUID id,
                                                     @Valid @RequestBody ParkingSlotDTO slot) {
        slot.setId(id);
        return ResponseEntity.ok(parkingSlotService.updateSlot(slot));
    }

    @PatchMapping("/{id}/out-of-service")
    @Operation(summary = "Withdraw a slot from service, or put it back",
            description = "Only a FREE slot can be withdrawn; a bay with a vehicle in it stays allocatable "
                    + "to the exit gate.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "Slot is not in a state that allows the change"),
            @ApiResponse(responseCode = "404", description = "No such slot")
    })
    public ResponseEntity<ParkingSlotDTO> setOutOfService(
            @PathVariable UUID id,
            @RequestParam(value = "value", defaultValue = "true") boolean outOfService) {
        return ResponseEntity.ok(parkingSlotService.setOutOfService(id, outOfService));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a slot")
    @ApiResponse(responseCode = "204", description = "Deleted")
    public ResponseEntity<Void> deleteSlot(@PathVariable UUID id) {
        parkingSlotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------
    // Availability
    // ---------------------------------------------------------------

    @GetMapping("/availability/{parkingLotId}/by-floor")
    @Operation(summary = "Free / reserved / occupied counts per floor")
    public ResponseEntity<List<SlotAvailabilityDTO>> availabilityByFloor(@PathVariable UUID parkingLotId) {
        return ResponseEntity.ok(parkingSlotService.getAvailabilityByFloor(parkingLotId));
    }

    @GetMapping("/availability/{parkingLotId}/by-block")
    @Operation(summary = "Free / reserved / occupied counts per block")
    public ResponseEntity<List<SlotAvailabilityDTO>> availabilityByBlock(@PathVariable UUID parkingLotId) {
        return ResponseEntity.ok(parkingSlotService.getAvailabilityByBlock(parkingLotId));
    }

    @GetMapping("/availability/{parkingLotId}/by-vehicle-type")
    @Operation(summary = "Free / reserved / occupied counts per vehicle type")
    public ResponseEntity<List<SlotAvailabilityDTO>> availabilityByVehicleType(@PathVariable UUID parkingLotId) {
        return ResponseEntity.ok(parkingSlotService.getAvailabilityByVehicleType(parkingLotId));
    }

    @GetMapping("/availability/{parkingLotId}/free")
    @Operation(summary = "Every slot that could be allocated right now for a vehicle type")
    public ResponseEntity<List<ParkingSlotDTO>> freeSlots(@PathVariable UUID parkingLotId,
                                                          @RequestParam VehicleType vehicleType) {
        return ResponseEntity.ok(parkingSlotService.getFreeSlots(parkingLotId, vehicleType));
    }

    @GetMapping("/occupancy/{parkingLotId}")
    @Operation(summary = "Live occupancy summary for a lot")
    public ResponseEntity<OccupancySummaryDTO> occupancy(@PathVariable UUID parkingLotId) {
        return ResponseEntity.ok(parkingSlotService.getOccupancy(parkingLotId));
    }
}
