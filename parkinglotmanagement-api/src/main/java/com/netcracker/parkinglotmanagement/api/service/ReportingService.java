package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.dto.DurationReportDTO;
import com.netcracker.parkinglotmanagement.api.dto.OccupancyPointDTO;
import com.netcracker.parkinglotmanagement.api.dto.PeakHourDTO;
import com.netcracker.parkinglotmanagement.api.dto.RevenueReportDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Read-only analytics over closed slips. Every range is half-open:
 * {@code [from, to)}.
 *
 * <p>Every method takes a {@code parkingLotId}; pass null to report across all
 * lots.
 */
public interface ReportingService {

    /** Takings from slips whose exit time falls in the range. */
    RevenueReportDTO getRevenueReport(UUID parkingLotId, LocalDateTime from, LocalDateTime to);

    /** Mean, min and max stay length over the range. */
    DurationReportDTO getDurationReport(UUID parkingLotId, LocalDateTime from, LocalDateTime to);

    /**
     * Occupancy sampled at a fixed interval.
     *
     * <p>The rate is expressed against the lot's current slot count, so it is only
     * populated when a {@code parkingLotId} is given.
     *
     * @param bucketHours width of each sample bucket in hours, 1-24
     */
    List<OccupancyPointDTO> getOccupancyOverTime(UUID parkingLotId, LocalDateTime from, LocalDateTime to, int bucketHours);

    /** Arrivals per hour of day over the range, busiest first. */
    List<PeakHourDTO> getPeakHours(UUID parkingLotId, LocalDateTime from, LocalDateTime to);
}
