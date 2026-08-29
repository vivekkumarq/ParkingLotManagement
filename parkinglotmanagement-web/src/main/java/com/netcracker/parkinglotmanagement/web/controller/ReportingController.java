package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.DurationReportDTO;
import com.netcracker.parkinglotmanagement.api.dto.OccupancyPointDTO;
import com.netcracker.parkinglotmanagement.api.dto.PeakHourDTO;
import com.netcracker.parkinglotmanagement.api.dto.RevenueReportDTO;
import com.netcracker.parkinglotmanagement.api.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/reports")
@Tag(name = "Reporting", description = "Revenue, occupancy, stay length and peak hours")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/revenue")
    @Operation(summary = "Revenue over a date range",
            description = "Counts slips whose exit time falls in [from, to). Omit parkingLotId to "
                    + "report across every lot.")
    public ResponseEntity<RevenueReportDTO> revenue(
            @Parameter(description = "Restrict to one lot; omit for all")
            @RequestParam(required = false) UUID parkingLotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reportingService.getRevenueReport(parkingLotId, from, to));
    }

    @GetMapping("/duration")
    @Operation(summary = "Average, shortest and longest stay over a date range")
    public ResponseEntity<DurationReportDTO> duration(
            @RequestParam(required = false) UUID parkingLotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reportingService.getDurationReport(parkingLotId, from, to));
    }

    @GetMapping("/occupancy")
    @Operation(summary = "Occupancy sampled over time",
            description = "The occupancy rate is only computed when a parkingLotId is supplied, since "
                    + "it needs that lot's slot count as the denominator.")
    public ResponseEntity<List<OccupancyPointDTO>> occupancy(
            @RequestParam(required = false) UUID parkingLotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Bucket width in hours, 1-24")
            @RequestParam(defaultValue = "1") int bucketHours) {
        return ResponseEntity.ok(reportingService.getOccupancyOverTime(parkingLotId, from, to, bucketHours));
    }

    @GetMapping("/peak-hours")
    @Operation(summary = "Arrivals per hour of day, busiest first")
    public ResponseEntity<List<PeakHourDTO>> peakHours(
            @RequestParam(required = false) UUID parkingLotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reportingService.getPeakHours(parkingLotId, from, to));
    }
}
