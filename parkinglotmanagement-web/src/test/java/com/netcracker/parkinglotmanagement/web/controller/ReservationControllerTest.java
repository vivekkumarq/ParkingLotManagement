package com.netcracker.parkinglotmanagement.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.parkinglotmanagement.api.domain.ReservationStatus;
import com.netcracker.parkinglotmanagement.api.domain.SlipStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationDTO;
import com.netcracker.parkinglotmanagement.api.dto.ReservationRequest;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.exception.ReservationConflictException;
import com.netcracker.parkinglotmanagement.api.service.ReservationService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 1, 9, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    private static ReservationDTO reservation() {
        return ReservationDTO.builder()
                .id(UUID.randomUUID())
                .parkingSlotId(UUID.randomUUID())
                .vehicleNumber("KA01AB1234")
                .startTimestamp(START)
                .endTimestamp(START.plusHours(3))
                .durationInHours(3)
                .bookingDate(LocalDate.of(2026, 8, 29))
                .status(ReservationStatus.BOOKED)
                .createdAt(LocalDateTime.of(2026, 8, 29, 10, 0))
                .build();
    }

    private static ReservationRequest validRequest() {
        return ReservationRequest.builder()
                .vehicleNumber("KA01AB1234")
                .vehicleType(VehicleType.CAR)
                .parkingLotId(UUID.randomUUID())
                .startTimestamp(START)
                .durationInHours(3)
                .build();
    }

    @Test
    @DisplayName("POST /reservations returns 201 with the booked window")
    void createReturnsCreated() throws Exception {
        when(reservationService.createReservation(any())).thenReturn(reservation());

        mockMvc.perform(post("/parking-lot-management/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andExpect(jsonPath("$.durationInHours").value(3))
                .andExpect(jsonPath("$.startTimestamp").value("2026-09-01T09:00:00"))
                .andExpect(jsonPath("$.endTimestamp").value("2026-09-01T12:00:00"));
    }

    @Test
    @DisplayName("an overlapping booking is a 409 with the RESERVATION_CONFLICT code")
    void overlapIsConflict() throws Exception {
        when(reservationService.createReservation(any()))
                .thenThrow(new ReservationConflictException("Slot is already booked from 09:00 to 12:00"));

        mockMvc.perform(post("/parking-lot-management/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_CONFLICT"));
    }

    @Test
    @DisplayName("a booking in the past is a 400")
    void pastBookingIsBadRequest() throws Exception {
        when(reservationService.createReservation(any()))
                .thenThrow(new InvalidRequestException("A reservation cannot start in the past"));

        mockMvc.perform(post("/parking-lot-management/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a duration outside 1-720 hours is rejected before it reaches the service")
    void durationIsValidated() throws Exception {
        ReservationRequest tooLong = validRequest();
        tooLong.setDurationInHours(1000);

        mockMvc.perform(post("/parking-lot-management/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("durationInHours"));

        ReservationRequest tooShort = validRequest();
        tooShort.setDurationInHours(0);

        mockMvc.perform(post("/parking-lot-management/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooShort)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /reservations/{id} returns the booking")
    void getReservation() throws Exception {
        ReservationDTO reservation = reservation();
        when(reservationService.getReservationById(reservation.getId())).thenReturn(reservation);

        mockMvc.perform(get("/parking-lot-management/reservations/" + reservation.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleNumber").value("KA01AB1234"));
    }

    @Test
    @DisplayName("GET /reservations passes the RSQL filter through")
    void listPassesTheFilter() throws Exception {
        when(reservationService.getAllReservations("status==BOOKED"))
                .thenReturn(Collections.singletonList(reservation()));

        mockMvc.perform(get("/parking-lot-management/reservations").param("search", "status==BOOKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("POST /reservations/{id}/claim returns 201 with the new slip")
    void claimReturnsASlip() throws Exception {
        UUID reservationId = UUID.randomUUID();
        when(reservationService.claimReservation(eq(reservationId), any()))
                .thenReturn(ParkingSlipDTO.builder()
                        .id(UUID.randomUUID())
                        .parkingSlotReservationId(reservationId)
                        .vehicleNumber("KA01AB1234")
                        .vehicleType(VehicleType.CAR)
                        .actualEntryTime(START)
                        .basicCost(new BigDecimal("0.00"))
                        .penalty(new BigDecimal("0.00"))
                        .totalCost(new BigDecimal("0.00"))
                        .status(SlipStatus.ACTIVE)
                        .build());

        mockMvc.perform(post("/parking-lot-management/reservations/" + reservationId + "/claim")
                        .param("arrivalTime", "2026-09-01T09:15:00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.parkingSlotReservationId").value(reservationId.toString()));
    }

    @Test
    @DisplayName("claiming without an arrival time defaults to the server clock")
    void claimWithoutArrivalTime() throws Exception {
        UUID reservationId = UUID.randomUUID();
        when(reservationService.claimReservation(eq(reservationId), eq(null)))
                .thenReturn(ParkingSlipDTO.builder().id(UUID.randomUUID()).status(SlipStatus.ACTIVE).build());

        mockMvc.perform(post("/parking-lot-management/reservations/" + reservationId + "/claim"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("claiming a booking that is not claimable is a 400")
    void claimingANonClaimableBookingIsBadRequest() throws Exception {
        UUID reservationId = UUID.randomUUID();
        when(reservationService.claimReservation(eq(reservationId), any()))
                .thenThrow(new InvalidRequestException("Reservation is CANCELLED and cannot be claimed"));

        mockMvc.perform(post("/parking-lot-management/reservations/" + reservationId + "/claim"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("DELETE /reservations/{id} cancels the booking")
    void cancelReservation() throws Exception {
        ReservationDTO cancelled = reservation();
        cancelled.setStatus(ReservationStatus.CANCELLED);
        when(reservationService.cancelReservation(cancelled.getId())).thenReturn(cancelled);

        mockMvc.perform(delete("/parking-lot-management/reservations/" + cancelled.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /reservations/expire reports how many were expired")
    void expireSweep() throws Exception {
        when(reservationService.expireStaleReservations(any())).thenReturn(4);

        mockMvc.perform(post("/parking-lot-management/reservations/expire")
                        .param("asOf", "2026-09-01T13:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(4));

        verify(reservationService).expireStaleReservations(LocalDateTime.of(2026, 9, 1, 13, 0));
    }

    @Test
    @DisplayName("a malformed timestamp parameter is a 400")
    void malformedTimestampIsBadRequest() throws Exception {
        mockMvc.perform(post("/parking-lot-management/reservations/expire").param("asOf", "yesterday"))
                .andExpect(status().isBadRequest());
    }
}
