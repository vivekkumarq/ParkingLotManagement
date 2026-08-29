package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.DurationReportDTO;
import com.netcracker.parkinglotmanagement.api.dto.OccupancyPointDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlipDTO;
import com.netcracker.parkinglotmanagement.api.dto.PeakHourDTO;
import com.netcracker.parkinglotmanagement.api.dto.RevenueReportDTO;
import com.netcracker.parkinglotmanagement.api.dto.VehicleEntryRequest;
import com.netcracker.parkinglotmanagement.api.dto.VehicleExitRequest;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlipService;
import com.netcracker.parkinglotmanagement.api.service.ReportingService;
import com.netcracker.parkinglotmanagement.support.DatabaseFixture;
import com.netcracker.parkinglotmanagement.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reporting over stays that were genuinely created through the service layer. */
@IntegrationTest
class ReportingServiceIntegrationTest {

    private static final LocalDateTime DAY_ONE = LocalDateTime.of(2026, 3, 1, 0, 0);
    private static final LocalDateTime RANGE_START = LocalDateTime.of(2026, 3, 1, 0, 0);
    private static final LocalDateTime RANGE_END = LocalDateTime.of(2026, 3, 5, 0, 0);

    @Autowired
    private ReportingService reportingService;

    @Autowired
    private ParkingSlipService parkingSlipService;

    @Autowired
    private DatabaseFixture fixture;

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    /** Parks a vehicle and takes it out again, so a priced, closed slip exists. */
    private ParkingSlipDTO completedStay(UUID lotId, String vehicleNumber, VehicleType type,
                                        LocalDateTime in, LocalDateTime out) {
        ParkingSlipDTO slip = parkingSlipService.checkIn(VehicleEntryRequest.builder()
                .vehicleNumber(vehicleNumber)
                .vehicleType(type)
                .parkingLotId(lotId)
                .entryTime(in)
                .build());
        parkingSlipService.checkOut(slip.getId(), VehicleExitRequest.builder().exitTime(out).build());
        return parkingSlipService.getSlipById(slip.getId());
    }

    @Test
    @DisplayName("revenue adds up across closed stays and splits by vehicle type")
    void revenueIsAggregated() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 4, VehicleType.CAR);
        fixture.createSlot(lot.slots().get(0).getFloorId(), 90, VehicleType.MOTORCYCLE);

        // CAR, 2 h -> 50 + 30 = 80.00
        completedStay(lot.lotId(), "KA01AB1111", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(11));
        // CAR, 3 h -> 50 + 2 x 30 = 110.00
        completedStay(lot.lotId(), "KA01AB2222", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(12));
        // MOTORCYCLE, 2 h -> 20 + 10 = 30.00
        completedStay(lot.lotId(), "KA01MC3333", VehicleType.MOTORCYCLE,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(11));

        RevenueReportDTO report = reportingService.getRevenueReport(lot.lotId(), RANGE_START, RANGE_END);

        assertThat(report.getClosedSlips()).isEqualTo(3L);
        assertThat(report.getTotalRevenue()).isEqualByComparingTo("220.00");
        assertThat(report.getBasicRevenue()).isEqualByComparingTo("220.00");
        assertThat(report.getPenaltyRevenue()).isEqualByComparingTo("0.00");
        assertThat(report.getAverageTicket()).isEqualByComparingTo("73.33"); // 220 / 3
        assertThat(report.getRevenueByVehicleType())
                .containsEntry(VehicleType.CAR, new java.math.BigDecimal("190.00"))
                .containsEntry(VehicleType.MOTORCYCLE, new java.math.BigDecimal("30.00"));
        assertThat(report.getCurrency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("revenue is broken down by day of exit")
    void revenueHasADailyBreakdown() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 4, VehicleType.CAR);

        completedStay(lot.lotId(), "KA01AB1111", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(11));               // exits 1 March
        completedStay(lot.lotId(), "KA01AB2222", VehicleType.CAR,
                DAY_ONE.plusDays(1).plusHours(9), DAY_ONE.plusDays(1).plusHours(12)); // exits 2 March

        RevenueReportDTO report = reportingService.getRevenueReport(lot.lotId(), RANGE_START, RANGE_END);

        assertThat(report.getDaily()).hasSize(2);
        assertThat(report.getDaily().get(0).getDate()).isEqualTo(DAY_ONE.toLocalDate());
        assertThat(report.getDaily().get(0).getTotalRevenue()).isEqualByComparingTo("80.00");
        assertThat(report.getDaily().get(1).getTotalRevenue()).isEqualByComparingTo("110.00");
    }

    @Test
    @DisplayName("stays that ended outside the range are excluded")
    void rangeIsHalfOpen() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 4, VehicleType.CAR);
        completedStay(lot.lotId(), "KA01AB1111", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(11));

        // Range that ends exactly at the exit time: the stay is not counted.
        RevenueReportDTO excluded = reportingService.getRevenueReport(
                lot.lotId(), RANGE_START, DAY_ONE.plusHours(11));
        assertThat(excluded.getClosedSlips()).isZero();
        assertThat(excluded.getTotalRevenue()).isEqualByComparingTo("0.00");

        // Range that starts exactly at the exit time: the stay is counted.
        RevenueReportDTO included = reportingService.getRevenueReport(
                lot.lotId(), DAY_ONE.plusHours(11), RANGE_END);
        assertThat(included.getClosedSlips()).isEqualTo(1L);
    }

    @Test
    @DisplayName("an empty range reports zeros rather than nulls")
    void emptyRangeIsZero() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);

        RevenueReportDTO report = reportingService.getRevenueReport(lot.lotId(), RANGE_START, RANGE_END);

        assertThat(report.getClosedSlips()).isZero();
        assertThat(report.getTotalRevenue()).isEqualByComparingTo("0.00");
        assertThat(report.getAverageTicket()).isEqualByComparingTo("0.00");
        assertThat(report.getDaily()).isEmpty();
    }

    @Test
    @DisplayName("revenue for one lot excludes another lot's takings")
    void revenueIsScopedToTheLot() {
        DatabaseFixture.Lot first = fixture.createLot(1, 2, VehicleType.CAR);
        DatabaseFixture.Lot second = fixture.createLot(1, 2, VehicleType.CAR);

        completedStay(first.lotId(), "KA01AB1111", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(11));   // 80.00
        completedStay(second.lotId(), "KA01AB2222", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(12));   // 110.00

        assertThat(reportingService.getRevenueReport(first.lotId(), RANGE_START, RANGE_END)
                .getTotalRevenue()).isEqualByComparingTo("80.00");
        assertThat(reportingService.getRevenueReport(second.lotId(), RANGE_START, RANGE_END)
                .getTotalRevenue()).isEqualByComparingTo("110.00");
        assertThat(reportingService.getRevenueReport(null, RANGE_START, RANGE_END)
                .getTotalRevenue())
                .as("a null lot id reports across every lot")
                .isEqualByComparingTo("190.00");
    }

    @Test
    @DisplayName("stay length reports mean, shortest and longest")
    void durationStatistics() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 4, VehicleType.CAR);

        completedStay(lot.lotId(), "KA01AB1111", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(10));            // 60 min
        completedStay(lot.lotId(), "KA01AB2222", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(12));            // 180 min
        completedStay(lot.lotId(), "KA01AB3333", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(11));            // 120 min

        DurationReportDTO report = reportingService.getDurationReport(lot.lotId(), RANGE_START, RANGE_END);

        assertThat(report.getClosedSlips()).isEqualTo(3L);
        assertThat(report.getAverageMinutes()).isEqualByComparingTo("120.00");
        assertThat(report.getAverageHours()).isEqualByComparingTo("2.00");
        assertThat(report.getMinMinutes()).isEqualTo(60L);
        assertThat(report.getMaxMinutes()).isEqualTo(180L);
    }

    @Test
    @DisplayName("an empty duration report is zeros, not an exception")
    void emptyDurationReport() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);

        DurationReportDTO report = reportingService.getDurationReport(lot.lotId(), RANGE_START, RANGE_END);

        assertThat(report.getClosedSlips()).isZero();
        assertThat(report.getAverageMinutes()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("occupancy is sampled per bucket and expressed against the lot's slot count")
    void occupancyOverTime() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 4, VehicleType.CAR);

        // Parked 09:00-11:00 and 10:00-12:00, so 10:00-11:00 has two vehicles.
        completedStay(lot.lotId(), "KA01AB1111", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(11));
        completedStay(lot.lotId(), "KA01AB2222", VehicleType.CAR,
                DAY_ONE.plusHours(10), DAY_ONE.plusHours(12));

        List<OccupancyPointDTO> series = reportingService.getOccupancyOverTime(
                lot.lotId(), DAY_ONE.plusHours(8), DAY_ONE.plusHours(13), 1);

        assertThat(series).hasSize(5);                                  // 08,09,10,11,12
        assertThat(series.get(0).getOccupiedSlots()).isZero();          // 08:00-09:00
        assertThat(series.get(1).getOccupiedSlots()).isEqualTo(1L);     // 09:00-10:00
        assertThat(series.get(2).getOccupiedSlots()).isEqualTo(2L);     // 10:00-11:00
        assertThat(series.get(3).getOccupiedSlots()).isEqualTo(1L);     // 11:00-12:00
        assertThat(series.get(4).getOccupiedSlots()).isZero();          // 12:00-13:00

        assertThat(series.get(2).getTotalSlots()).isEqualTo(4);
        assertThat(series.get(2).getOccupancyRate()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("without a lot the occupancy series has counts but no rate")
    void occupancyWithoutALotHasNoRate() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 4, VehicleType.CAR);
        completedStay(lot.lotId(), "KA01AB1111", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(11));

        List<OccupancyPointDTO> series = reportingService.getOccupancyOverTime(
                null, DAY_ONE.plusHours(9), DAY_ONE.plusHours(10), 1);

        assertThat(series).hasSize(1);
        assertThat(series.get(0).getOccupiedSlots()).isEqualTo(1L);
        assertThat(series.get(0).getOccupancyRate()).isNull();
    }

    @Test
    @DisplayName("an out-of-range bucket width is rejected")
    void bucketWidthIsValidated() {
        assertThatThrownBy(() -> reportingService.getOccupancyOverTime(null, RANGE_START, RANGE_END, 0))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("bucketHours");
        assertThatThrownBy(() -> reportingService.getOccupancyOverTime(null, RANGE_START, RANGE_END, 25))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("peak hours rank arrival hours by volume")
    void peakHours() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 6, VehicleType.CAR);

        // Three arrivals in the 09:00 hour, one in the 14:00 hour.
        completedStay(lot.lotId(), "KA01AB1111", VehicleType.CAR,
                DAY_ONE.plusHours(9), DAY_ONE.plusHours(10));
        completedStay(lot.lotId(), "KA01AB2222", VehicleType.CAR,
                DAY_ONE.plusHours(9).plusMinutes(20), DAY_ONE.plusHours(10));
        completedStay(lot.lotId(), "KA01AB3333", VehicleType.CAR,
                DAY_ONE.plusHours(9).plusMinutes(50), DAY_ONE.plusHours(10));
        completedStay(lot.lotId(), "KA01AB4444", VehicleType.CAR,
                DAY_ONE.plusHours(14), DAY_ONE.plusHours(15));

        List<PeakHourDTO> peaks = reportingService.getPeakHours(lot.lotId(), RANGE_START, RANGE_END);

        assertThat(peaks).isNotEmpty();
        assertThat(peaks.get(0).getHourOfDay()).isEqualTo(9);
        assertThat(peaks.get(0).getEntries()).isEqualTo(3L);
        assertThat(peaks).anySatisfy(peak -> {
            assertThat(peak.getHourOfDay()).isEqualTo(14);
            assertThat(peak.getEntries()).isEqualTo(1L);
        });
    }

    @Test
    @DisplayName("an inverted or incomplete range is rejected")
    void rangeIsValidated() {
        assertThatThrownBy(() -> reportingService.getRevenueReport(null, RANGE_END, RANGE_START))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("must be before");
        assertThatThrownBy(() -> reportingService.getRevenueReport(null, null, RANGE_END))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> reportingService.getDurationReport(null, RANGE_START, RANGE_START))
                .isInstanceOf(InvalidRequestException.class);
    }
}
