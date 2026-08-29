package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.DailyRevenueDTO;
import com.netcracker.parkinglotmanagement.api.dto.DurationReportDTO;
import com.netcracker.parkinglotmanagement.api.dto.OccupancyPointDTO;
import com.netcracker.parkinglotmanagement.api.dto.PeakHourDTO;
import com.netcracker.parkinglotmanagement.api.dto.RevenueReportDTO;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.service.ReportingService;
import com.netcracker.parkinglotmanagement.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportingController.class)
@Import(GlobalExceptionHandler.class)
class ReportingControllerTest {

    private static final String FROM = "2026-03-01T00:00:00";
    private static final String TO = "2026-03-08T00:00:00";
    private static final LocalDateTime FROM_TIME = LocalDateTime.of(2026, 3, 1, 0, 0);
    private static final LocalDateTime TO_TIME = LocalDateTime.of(2026, 3, 8, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportingService reportingService;

    @Test
    @DisplayName("GET /reports/revenue returns the totals and the per-type split")
    void revenue() throws Exception {
        Map<VehicleType, BigDecimal> byType = new EnumMap<>(VehicleType.class);
        byType.put(VehicleType.CAR, new BigDecimal("184320.00"));

        when(reportingService.getRevenueReport(eq(null), eq(FROM_TIME), eq(TO_TIME)))
                .thenReturn(RevenueReportDTO.builder()
                        .from(FROM_TIME).to(TO_TIME)
                        .closedSlips(412L)
                        .basicRevenue(new BigDecimal("184320.00"))
                        .penaltyRevenue(new BigDecimal("2400.00"))
                        .totalRevenue(new BigDecimal("186720.00"))
                        .averageTicket(new BigDecimal("453.20"))
                        .revenueByVehicleType(byType)
                        .daily(Collections.singletonList(DailyRevenueDTO.builder()
                                .date(LocalDate.of(2026, 3, 1))
                                .closedSlips(37L)
                                .totalRevenue(new BigDecimal("16780.00"))
                                .build()))
                        .currency("INR")
                        .build());

        mockMvc.perform(get("/parking-lot-management/reports/revenue")
                        .param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedSlips").value(412))
                .andExpect(jsonPath("$.totalRevenue").value(186720.00))
                .andExpect(jsonPath("$.averageTicket").value(453.20))
                .andExpect(jsonPath("$.revenueByVehicleType.CAR").value(184320.00))
                .andExpect(jsonPath("$.daily", hasSize(1)))
                .andExpect(jsonPath("$.daily[0].date").value("2026-03-01"))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    @DisplayName("a parkingLotId scopes the report")
    void revenueCanBeScopedToALot() throws Exception {
        UUID lotId = UUID.randomUUID();
        when(reportingService.getRevenueReport(eq(lotId), any(), any()))
                .thenReturn(RevenueReportDTO.builder().totalRevenue(new BigDecimal("100.00")).build());

        mockMvc.perform(get("/parking-lot-management/reports/revenue")
                        .param("parkingLotId", lotId.toString())
                        .param("from", FROM).param("to", TO))
                .andExpect(status().isOk());

        verify(reportingService).getRevenueReport(lotId, FROM_TIME, TO_TIME);
    }

    @Test
    @DisplayName("a missing range parameter is a 400")
    void missingRangeIsBadRequest() throws Exception {
        mockMvc.perform(get("/parking-lot-management/reports/revenue").param("from", FROM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("to")));
    }

    @Test
    @DisplayName("an inverted range is a 400 from the service")
    void invertedRangeIsBadRequest() throws Exception {
        when(reportingService.getRevenueReport(any(), any(), any()))
                .thenThrow(new InvalidRequestException("'from' must be before 'to'"));

        mockMvc.perform(get("/parking-lot-management/reports/revenue")
                        .param("from", TO).param("to", FROM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("GET /reports/duration returns stay-length statistics")
    void duration() throws Exception {
        when(reportingService.getDurationReport(any(), any(), any()))
                .thenReturn(DurationReportDTO.builder()
                        .from(FROM_TIME).to(TO_TIME)
                        .closedSlips(412L)
                        .averageMinutes(new BigDecimal("168.45"))
                        .averageHours(new BigDecimal("2.81"))
                        .minMinutes(12L).maxMinutes(2874L)
                        .build());

        mockMvc.perform(get("/parking-lot-management/reports/duration")
                        .param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageMinutes").value(168.45))
                .andExpect(jsonPath("$.averageHours").value(2.81))
                .andExpect(jsonPath("$.minMinutes").value(12))
                .andExpect(jsonPath("$.maxMinutes").value(2874));
    }

    @Test
    @DisplayName("GET /reports/occupancy defaults to one-hour buckets")
    void occupancyDefaultsToHourlyBuckets() throws Exception {
        when(reportingService.getOccupancyOverTime(any(), any(), any(), anyInt()))
                .thenReturn(Collections.singletonList(OccupancyPointDTO.builder()
                        .bucketStart(FROM_TIME)
                        .occupiedSlots(84L)
                        .totalSlots(150)
                        .occupancyRate(new BigDecimal("56.00"))
                        .build()));

        mockMvc.perform(get("/parking-lot-management/reports/occupancy")
                        .param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].occupiedSlots").value(84))
                .andExpect(jsonPath("$[0].occupancyRate").value(56.00));

        verify(reportingService).getOccupancyOverTime(null, FROM_TIME, TO_TIME, 1);
    }

    @Test
    @DisplayName("the bucket width can be overridden")
    void occupancyBucketWidthIsHonoured() throws Exception {
        when(reportingService.getOccupancyOverTime(any(), any(), any(), anyInt()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/parking-lot-management/reports/occupancy")
                        .param("from", FROM).param("to", TO).param("bucketHours", "6"))
                .andExpect(status().isOk());

        verify(reportingService).getOccupancyOverTime(null, FROM_TIME, TO_TIME, 6);
    }

    @Test
    @DisplayName("GET /reports/peak-hours returns the busiest hours first")
    void peakHours() throws Exception {
        when(reportingService.getPeakHours(any(), any(), any())).thenReturn(Arrays.asList(
                PeakHourDTO.builder().hourOfDay(18).entries(97L).build(),
                PeakHourDTO.builder().hourOfDay(9).entries(64L).build()));

        mockMvc.perform(get("/parking-lot-management/reports/peak-hours")
                        .param("from", FROM).param("to", TO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].hourOfDay").value(18))
                .andExpect(jsonPath("$[0].entries").value(97));
    }
}
