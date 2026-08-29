package com.netcracker.parkinglotmanagement.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.parkinglotmanagement.api.domain.SlipStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.FeeBreakdownDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.VehicleEntryRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitResponse;
import com.netcracker.parkinglotmanagement.api.exception.DuplicateEntryException;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.NoSlotAvailableException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlipService;
import com.netcracker.parkinglotmanagement.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingController.class)
@Import(GlobalExceptionHandler.class)
class ParkingControllerTest {

    private static final LocalDateTime ENTRY = LocalDateTime.of(2026, 3, 1, 8, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParkingSlipService parkingSlipService;

    private static ParkingSlipDTO activeSlip() {
        return ParkingSlipDTO.builder()
                .id(UUID.randomUUID())
                .parkingSlotId(UUID.randomUUID())
                .vehicleNumber("KA01AB1234")
                .vehicleType(VehicleType.CAR)
                .actualEntryTime(ENTRY)
                .basicCost(new BigDecimal("0.00"))
                .penalty(new BigDecimal("0.00"))
                .totalCost(new BigDecimal("0.00"))
                .status(SlipStatus.ACTIVE)
                .build();
    }

    private static VehicleEntryRequest validEntry() {
        return VehicleEntryRequest.builder()
                .vehicleNumber("KA01AB1234")
                .vehicleType(VehicleType.CAR)
                .parkingLotId(UUID.randomUUID())
                .entryTime(ENTRY)
                .build();
    }

    @Test
    @DisplayName("POST /parking/entry returns 201 with the slip")
    void checkInReturnsCreated() throws Exception {
        ParkingSlipDTO slip = activeSlip();
        when(parkingSlipService.checkIn(any())).thenReturn(slip);

        mockMvc.perform(post("/parking-lot-management/parking/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEntry())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(slip.getId().toString()))
                .andExpect(jsonPath("$.vehicleNumber").value("KA01AB1234"))
                .andExpect(jsonPath("$.vehicleType").value("CAR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.@type").value("ParkingSlip"));
    }

    @Test
    @DisplayName("a body missing required fields is a 400 listing the offending fields")
    void invalidBodyIsRejected() throws Exception {
        VehicleEntryRequest incomplete = VehicleEntryRequest.builder().build();

        mockMvc.perform(post("/parking-lot-management/parking/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incomplete)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations[*].field")
                        .value(org.hamcrest.Matchers.hasItems("vehicleNumber", "vehicleType", "parkingLotId")));
    }

    @Test
    @DisplayName("an unknown vehicle type is a 400, not a 500")
    void unknownEnumIsRejected() throws Exception {
        String body = "{\"vehicleNumber\":\"KA01AB1234\",\"vehicleType\":\"SPACESHIP\","
                + "\"parkingLotId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/parking-lot-management/parking/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("a full lot is a 409 with a stable code")
    void fullLotIsConflict() throws Exception {
        when(parkingSlipService.checkIn(any())).thenThrow(new NoSlotAvailableException(VehicleType.TRUCK));

        mockMvc.perform(post("/parking-lot-management/parking/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEntry())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_SLOT_AVAILABLE"))
                .andExpect(jsonPath("$.path").value("/parking-lot-management/parking/entry"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("a vehicle already inside is a 409")
    void duplicateEntryIsConflict() throws Exception {
        when(parkingSlipService.checkIn(any()))
                .thenThrow(new DuplicateEntryException("Vehicle KA01AB1234 already has an open slip"));

        mockMvc.perform(post("/parking-lot-management/parking/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEntry())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ENTRY"));
    }

    @Test
    @DisplayName("POST /parking/exit/{slipId} returns the closed slip and the fee breakdown")
    void checkOutReturnsFee() throws Exception {
        UUID slipId = UUID.randomUUID();
        ParkingSlipDTO closed = activeSlip();
        closed.setStatus(SlipStatus.CLOSED);
        closed.setActualExitTime(LocalDateTime.of(2026, 3, 2, 11, 30));
        closed.setTotalCost(new BigDecimal("540.00"));

        FeeBreakdownDTO fee = FeeBreakdownDTO.builder()
                .vehicleType(VehicleType.CAR)
                .durationMinutes(1650L)
                .withinGracePeriod(false)
                .chargedDays(1L)
                .chargedHours(4L)
                .dayCharges(new BigDecimal("400.00"))
                .hourCharges(new BigDecimal("140.00"))
                .basicCost(new BigDecimal("540.00"))
                .penalty(new BigDecimal("0.00"))
                .totalCost(new BigDecimal("540.00"))
                .currency("INR")
                .build();

        when(parkingSlipService.checkOut(eq(slipId), any()))
                .thenReturn(VehicleExitResponse.builder().slip(closed).fee(fee).build());

        mockMvc.perform(post("/parking-lot-management/parking/exit/" + slipId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                VehicleExitRequest.builder()
                                        .exitTime(LocalDateTime.of(2026, 3, 2, 11, 30))
                                        .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slip.status").value("CLOSED"))
                .andExpect(jsonPath("$.slip.totalCost").value(540.00))
                .andExpect(jsonPath("$.fee.chargedDays").value(1))
                .andExpect(jsonPath("$.fee.chargedHours").value(4))
                .andExpect(jsonPath("$.fee.dayCharges").value(400.00))
                .andExpect(jsonPath("$.fee.hourCharges").value(140.00))
                .andExpect(jsonPath("$.fee.currency").value("INR"));
    }

    @Test
    @DisplayName("exit works without a body, defaulting to the server clock")
    void checkOutWithoutABody() throws Exception {
        UUID slipId = UUID.randomUUID();
        when(parkingSlipService.checkOut(eq(slipId), any()))
                .thenReturn(VehicleExitResponse.builder().slip(activeSlip()).build());

        mockMvc.perform(post("/parking-lot-management/parking/exit/" + slipId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("exiting an unknown slip is a 404")
    void unknownSlipIsNotFound() throws Exception {
        UUID slipId = UUID.randomUUID();
        when(parkingSlipService.checkOut(eq(slipId), any()))
                .thenThrow(new ResourceNotFoundException("Parking slip", slipId));

        mockMvc.perform(post("/parking-lot-management/parking/exit/" + slipId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("exiting a slip that is already closed is a 400")
    void alreadyClosedIsBadRequest() throws Exception {
        UUID slipId = UUID.randomUUID();
        when(parkingSlipService.checkOut(eq(slipId), any()))
                .thenThrow(new InvalidRequestException("Parking slip " + slipId + " is already closed"));

        mockMvc.perform(post("/parking-lot-management/parking/exit/" + slipId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("a malformed UUID in the path is a 400, not a 500")
    void malformedUuidIsBadRequest() throws Exception {
        mockMvc.perform(post("/parking-lot-management/parking/exit/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("slipId")));
    }

    @Test
    @DisplayName("exit by registration is supported")
    void checkOutByRegistration() throws Exception {
        when(parkingSlipService.checkOutByVehicleNumber(eq("KA01AB1234"), any()))
                .thenReturn(VehicleExitResponse.builder().slip(activeSlip()).build());

        mockMvc.perform(post("/parking-lot-management/parking/exit/by-vehicle/KA01AB1234"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /parking/slips/{id} returns the slip")
    void getSlip() throws Exception {
        ParkingSlipDTO slip = activeSlip();
        when(parkingSlipService.getSlipById(slip.getId())).thenReturn(slip);

        mockMvc.perform(get("/parking-lot-management/parking/slips/" + slip.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(slip.getId().toString()));
    }

    @Test
    @DisplayName("GET /parking/slips/active/{vehicleNumber} returns the open slip")
    void getActiveSlip() throws Exception {
        ParkingSlipDTO slip = activeSlip();
        when(parkingSlipService.getActiveSlipByVehicleNumber("KA01AB1234")).thenReturn(slip);

        mockMvc.perform(get("/parking-lot-management/parking/slips/active/KA01AB1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /parking/slips passes the RSQL filter through")
    void listSlipsPassesTheFilter() throws Exception {
        when(parkingSlipService.getAllSlips("status==ACTIVE"))
                .thenReturn(Collections.singletonList(activeSlip()));

        mockMvc.perform(get("/parking-lot-management/parking/slips").param("search", "status==ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    @DisplayName("GET /parking/slips without a filter lists everything")
    void listSlipsWithoutAFilter() throws Exception {
        when(parkingSlipService.getAllSlips(null))
                .thenReturn(Arrays.asList(activeSlip(), activeSlip()));

        mockMvc.perform(get("/parking-lot-management/parking/slips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    @DisplayName("an invalid RSQL filter is a 400")
    void invalidFilterIsBadRequest() throws Exception {
        when(parkingSlipService.getAllSlips("bogus==1"))
                .thenThrow(new InvalidRequestException("Unknown filter field 'bogus'"));

        mockMvc.perform(get("/parking-lot-management/parking/slips").param("search", "bogus==1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("an unexpected failure is a 500 that leaks nothing")
    void unexpectedFailureLeaksNothing() throws Exception {
        when(parkingSlipService.getAllSlips(null)).thenThrow(new IllegalStateException("jdbc:postgres://secret"));

        mockMvc.perform(get("/parking-lot-management/parking/slips"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }
}
