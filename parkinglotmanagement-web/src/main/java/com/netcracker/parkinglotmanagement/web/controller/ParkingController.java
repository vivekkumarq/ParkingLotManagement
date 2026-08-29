package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.VehicleEntryRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitResponse;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/parking")
@Tag(name = "Parking", description = "Vehicle entry, exit and the slips they produce")
public class ParkingController {

    private final ParkingSlipService parkingSlipService;

    public ParkingController(ParkingSlipService parkingSlipService) {
        this.parkingSlipService = parkingSlipService;
    }

    @PostMapping("/entry")
    @Operation(summary = "Check a vehicle in",
            description = "Allocates the lowest free slot matching the vehicle type and issues a slip.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Slip issued"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Lot is full, or the vehicle is already inside")
    })
    public ResponseEntity<ParkingSlipDTO> checkIn(@Valid @RequestBody VehicleEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(parkingSlipService.checkIn(request));
    }

    @PostMapping("/exit/{slipId}")
    @Operation(summary = "Check a vehicle out by slip id",
            description = "Closes the slip, prices the stay from the rate card and frees the slot.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Slip closed and priced"),
            @ApiResponse(responseCode = "404", description = "No such slip"),
            @ApiResponse(responseCode = "400", description = "Slip already closed, or exit precedes entry")
    })
    public ResponseEntity<VehicleExitResponse> checkOut(
            @PathVariable UUID slipId,
            @RequestBody(required = false) VehicleExitRequest request) {
        return ResponseEntity.ok(parkingSlipService.checkOut(slipId, request));
    }

    @PostMapping("/exit/by-vehicle/{vehicleNumber}")
    @Operation(summary = "Check a vehicle out by registration",
            description = "For gates that read a number plate rather than a ticket.")
    public ResponseEntity<VehicleExitResponse> checkOutByVehicle(
            @PathVariable String vehicleNumber,
            @RequestBody(required = false) VehicleExitRequest request) {
        return ResponseEntity.ok(parkingSlipService.checkOutByVehicleNumber(vehicleNumber, request));
    }

    @GetMapping("/slips/{id}")
    @Operation(summary = "Fetch one slip")
    public ResponseEntity<ParkingSlipDTO> getSlip(@PathVariable UUID id) {
        return ResponseEntity.ok(parkingSlipService.getSlipById(id));
    }

    @GetMapping("/slips/active/{vehicleNumber}")
    @Operation(summary = "The open slip for a vehicle, if it is inside")
    public ResponseEntity<ParkingSlipDTO> getActiveSlip(@PathVariable String vehicleNumber) {
        return ResponseEntity.ok(parkingSlipService.getActiveSlipByVehicleNumber(vehicleNumber));
    }

    @GetMapping("/slips")
    @Operation(summary = "List slips",
            description = "Optional RSQL filter over id, parkingSlotId, customerId, vehicleNumber, "
                    + "vehicleType, status, basicCost, totalCost, actualEntryTime and actualExitTime. "
                    + "Example: status==CLOSED;totalCost=gt=500")
    public ResponseEntity<List<ParkingSlipDTO>> listSlips(
            @Parameter(description = "RSQL filter", example = "vehicleType==CAR;status==ACTIVE")
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(parkingSlipService.getAllSlips(search));
    }
}
