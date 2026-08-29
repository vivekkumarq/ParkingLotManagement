package com.netcracker.parkinglotmanagement.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.OccupancySummaryDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.api.dto.SlotAvailabilityDTO;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlotService;
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
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingSlotController.class)
@Import(GlobalExceptionHandler.class)
class ParkingSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParkingSlotService parkingSlotService;

    private static ParkingSlotDTO slot() {
        return ParkingSlotDTO.builder()
                .id(UUID.randomUUID())
                .floorId(UUID.randomUUID())
                .slotNumber(17)
                .charges(0)
                .vehicleType(VehicleType.CAR)
                .status(SlotStatus.FREE)
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("POST /slots returns 201")
    void createSlot() throws Exception {
        ParkingSlotDTO slot = slot();
        when(parkingSlotService.createSlot(any())).thenReturn(slot);

        mockMvc.perform(post("/parking-lot-management/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotNumber").value(17))
                .andExpect(jsonPath("$.vehicleType").value("CAR"))
                .andExpect(jsonPath("$.status").value("FREE"));
    }

    @Test
    @DisplayName("a slot without a floor or vehicle type is rejected")
    void invalidSlotIsRejected() throws Exception {
        mockMvc.perform(post("/parking-lot-management/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotNumber\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[*].field")
                        .value(org.hamcrest.Matchers.hasItems("floorId", "vehicleType", "slotNumber")));
    }

    @Test
    @DisplayName("GET /slots/{id} returns 404 when the slot is unknown")
    void unknownSlotIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(parkingSlotService.getSlotById(id)).thenThrow(new ResourceNotFoundException("Parking slot", id));

        mockMvc.perform(get("/parking-lot-management/slots/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /slots passes the RSQL filter through")
    void listPassesTheFilter() throws Exception {
        when(parkingSlotService.getAllSlots("status==FREE"))
                .thenReturn(Collections.singletonList(slot()));

        mockMvc.perform(get("/parking-lot-management/slots").param("search", "status==FREE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("PUT /slots/{id} takes the id from the path")
    void updateUsesThePathId() throws Exception {
        ParkingSlotDTO slot = slot();
        UUID pathId = UUID.randomUUID();
        when(parkingSlotService.updateSlot(any())).thenReturn(slot);

        mockMvc.perform(put("/parking-lot-management/slots/" + pathId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slot)))
                .andExpect(status().isOk());

        verify(parkingSlotService).updateSlot(org.mockito.ArgumentMatchers.argThat(
                updated -> pathId.equals(updated.getId())));
    }

    @Test
    @DisplayName("PATCH /slots/{id}/out-of-service withdraws a slot")
    void setOutOfService() throws Exception {
        ParkingSlotDTO slot = slot();
        slot.setStatus(SlotStatus.OUT_OF_SERVICE);
        when(parkingSlotService.setOutOfService(any(), org.mockito.ArgumentMatchers.eq(true))).thenReturn(slot);

        mockMvc.perform(patch("/parking-lot-management/slots/" + slot.getId() + "/out-of-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
    }

    @Test
    @DisplayName("withdrawing an occupied slot is a 400")
    void withdrawingAnOccupiedSlotIsRejected() throws Exception {
        UUID id = UUID.randomUUID();
        when(parkingSlotService.setOutOfService(any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenThrow(new InvalidRequestException("Slot " + id + " is OCCUPIED"));

        mockMvc.perform(patch("/parking-lot-management/slots/" + id + "/out-of-service"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /slots/{id} returns 204")
    void deleteSlot() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/parking-lot-management/slots/" + id))
                .andExpect(status().isNoContent());

        verify(parkingSlotService).deleteSlot(id);
    }

    @Test
    @DisplayName("deleting an unknown slot is a 404")
    void deletingAnUnknownSlotIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Parking slot", id)).when(parkingSlotService).deleteSlot(id);

        mockMvc.perform(delete("/parking-lot-management/slots/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("availability by floor returns the per-floor counts")
    void availabilityByFloor() throws Exception {
        UUID lotId = UUID.randomUUID();
        when(parkingSlotService.getAvailabilityByFloor(lotId)).thenReturn(Collections.singletonList(
                SlotAvailabilityDTO.builder()
                        .parkingLotId(lotId).floorNo(1).blockCode("A")
                        .freeSlots(12).reservedSlots(3).occupiedSlots(35).outOfServiceSlots(0).totalSlots(50)
                        .build()));

        mockMvc.perform(get("/parking-lot-management/slots/availability/" + lotId + "/by-floor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].floorNo").value(1))
                .andExpect(jsonPath("$[0].freeSlots").value(12))
                .andExpect(jsonPath("$[0].totalSlots").value(50));
    }

    @Test
    @DisplayName("availability by block and by vehicle type are both exposed")
    void availabilityByBlockAndType() throws Exception {
        UUID lotId = UUID.randomUUID();
        when(parkingSlotService.getAvailabilityByBlock(lotId)).thenReturn(Collections.emptyList());
        when(parkingSlotService.getAvailabilityByVehicleType(lotId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/parking-lot-management/slots/availability/" + lotId + "/by-block"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/parking-lot-management/slots/availability/" + lotId + "/by-vehicle-type"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("free slots are filtered by vehicle type")
    void freeSlotsByVehicleType() throws Exception {
        UUID lotId = UUID.randomUUID();
        when(parkingSlotService.getFreeSlots(lotId, VehicleType.CAR))
                .thenReturn(Collections.singletonList(slot()));

        mockMvc.perform(get("/parking-lot-management/slots/availability/" + lotId + "/free")
                        .param("vehicleType", "CAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("an unknown vehicle type in a query parameter is a 400")
    void unknownVehicleTypeParameterIsRejected() throws Exception {
        mockMvc.perform(get("/parking-lot-management/slots/availability/" + UUID.randomUUID() + "/free")
                        .param("vehicleType", "SPACESHIP"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("occupancy returns the headline figures")
    void occupancy() throws Exception {
        UUID lotId = UUID.randomUUID();
        when(parkingSlotService.getOccupancy(lotId)).thenReturn(OccupancySummaryDTO.builder()
                .parkingLotId(lotId)
                .asOf(LocalDateTime.of(2026, 3, 1, 12, 0))
                .totalSlots(150).occupiedSlots(98).reservedSlots(9).freeSlots(43)
                .occupancyRate(new BigDecimal("65.33"))
                .activeSlips(98)
                .byVehicleType(Collections.emptyList())
                .build());

        mockMvc.perform(get("/parking-lot-management/slots/occupancy/" + lotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(150))
                .andExpect(jsonPath("$.occupiedSlots").value(98))
                .andExpect(jsonPath("$.occupancyRate").value(65.33))
                .andExpect(jsonPath("$.activeSlips").value(98));
    }
}
