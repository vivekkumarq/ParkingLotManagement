package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.DailyRevenueDTO;
import com.netcracker.parkinglotmanagement.api.dto.DurationReportDTO;
import com.netcracker.parkinglotmanagement.api.dto.OccupancyPointDTO;
import com.netcracker.parkinglotmanagement.api.dto.PeakHourDTO;
import com.netcracker.parkinglotmanagement.api.dto.RevenueReportDTO;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.api.service.ReportingService;
import com.netcracker.parkinglotmanagement.config.RateCardProperties;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlipRepository;
import com.netcracker.parkinglotmanagement.service.repository.ParkingSlotRepository;
import org.jooq.Record2;
import org.jooq.Record4;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Analytics over closed slips. Every range is half-open: {@code [from, to)}. */
@Service
public class ReportingServiceImpl implements ReportingService {

    private static final int MAX_BUCKET_HOURS = 24;
    private static final BigDecimal MINUTES_PER_HOUR = new BigDecimal("60");

    private final ParkingSlipRepository parkingSlipRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final RateCardProperties rateCard;

    public ReportingServiceImpl(ParkingSlipRepository parkingSlipRepository,
                                ParkingSlotRepository parkingSlotRepository,
                                RateCardProperties rateCard) {
        this.parkingSlipRepository = parkingSlipRepository;
        this.parkingSlotRepository = parkingSlotRepository;
        this.rateCard = rateCard;
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueReportDTO getRevenueReport(UUID parkingLotId, LocalDateTime from, LocalDateTime to) {
        requireRange(from, to);

        Record4<Integer, BigDecimal, BigDecimal, BigDecimal> totals =
                parkingSlipRepository.revenueTotals(parkingLotId, from, to);
        long closedSlips = totals != null ? totals.value1() : 0L;
        BigDecimal basic = money(totals != null ? totals.value2() : BigDecimal.ZERO);
        BigDecimal penalty = money(totals != null ? totals.value3() : BigDecimal.ZERO);
        BigDecimal total = money(totals != null ? totals.value4() : BigDecimal.ZERO);

        Map<VehicleType, BigDecimal> byType = new EnumMap<>(VehicleType.class);
        for (Record2<String, BigDecimal> row : parkingSlipRepository.revenueByVehicleType(parkingLotId, from, to)) {
            byType.put(VehicleType.from(row.value1()), money(row.value2()));
        }

        return RevenueReportDTO.builder()
                .from(from)
                .to(to)
                .closedSlips(closedSlips)
                .basicRevenue(basic)
                .penaltyRevenue(penalty)
                .totalRevenue(total)
                .averageTicket(closedSlips == 0
                        ? money(BigDecimal.ZERO)
                        : total.divide(BigDecimal.valueOf(closedSlips), 2, RoundingMode.HALF_UP))
                .revenueByVehicleType(byType)
                .daily(dailyBreakdown(parkingLotId, from, to))
                .currency(rateCard.getCurrency())
                .build();
    }

    private List<DailyRevenueDTO> dailyBreakdown(UUID parkingLotId, LocalDateTime from, LocalDateTime to) {
        // Grouped in Java from a single projection: DATE_TRUNC and CAST-to-date are
        // spelled differently on PostgreSQL and H2, and one query beats a portable
        // dialect switch here.
        Map<LocalDate, long[]> counts = new TreeMap<>();
        Map<LocalDate, BigDecimal> sums = new TreeMap<>();

        for (Record2<LocalDateTime, BigDecimal> row : parkingSlipRepository.closedSlipTotals(parkingLotId, from, to)) {
            LocalDate day = row.value1().toLocalDate();
            counts.computeIfAbsent(day, key -> new long[1])[0]++;
            sums.merge(day, row.value2() != null ? row.value2() : BigDecimal.ZERO, BigDecimal::add);
        }

        List<DailyRevenueDTO> daily = new ArrayList<>(counts.size());
        counts.forEach((day, count) -> daily.add(DailyRevenueDTO.builder()
                .date(day)
                .closedSlips(count[0])
                .totalRevenue(money(sums.getOrDefault(day, BigDecimal.ZERO)))
                .build()));
        return daily;
    }

    @Override
    @Transactional(readOnly = true)
    public DurationReportDTO getDurationReport(UUID parkingLotId, LocalDateTime from, LocalDateTime to) {
        requireRange(from, to);

        List<Record2<LocalDateTime, LocalDateTime>> stays =
                parkingSlipRepository.closedStayWindows(parkingLotId, from, to);

        if (stays.isEmpty()) {
            return DurationReportDTO.builder()
                    .from(from).to(to)
                    .closedSlips(0L)
                    .averageMinutes(money(BigDecimal.ZERO))
                    .averageHours(money(BigDecimal.ZERO))
                    .minMinutes(0L)
                    .maxMinutes(0L)
                    .build();
        }

        long totalMinutes = 0L;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (Record2<LocalDateTime, LocalDateTime> stay : stays) {
            long minutes = Duration.between(stay.value1(), stay.value2()).toMinutes();
            totalMinutes += minutes;
            min = Math.min(min, minutes);
            max = Math.max(max, minutes);
        }

        BigDecimal averageMinutes = BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(stays.size()), 2, RoundingMode.HALF_UP);

        return DurationReportDTO.builder()
                .from(from).to(to)
                .closedSlips((long) stays.size())
                .averageMinutes(averageMinutes)
                .averageHours(averageMinutes.divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP))
                .minMinutes(min)
                .maxMinutes(max)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OccupancyPointDTO> getOccupancyOverTime(UUID parkingLotId, LocalDateTime from, LocalDateTime to, int bucketHours) {
        requireRange(from, to);
        if (bucketHours < 1 || bucketHours > MAX_BUCKET_HOURS) {
            throw new InvalidRequestException(
                    "bucketHours must be between 1 and " + MAX_BUCKET_HOURS + ", was " + bucketHours);
        }

        // One query for every stay that touches the range; the bucketing is then a
        // pass over that list rather than a query per bucket.
        List<Record2<LocalDateTime, LocalDateTime>> stays = parkingSlipRepository.staysOverlapping(parkingLotId, from, to);

        // The denominator is the lot's present slot count. Without a lot the series
        // still reports absolute occupancy, but a rate would be meaningless.
        Integer totalSlots = parkingLotId == null
                ? null
                : parkingSlotRepository.totalsForLot(parkingLotId).getTotalSlots();

        List<OccupancyPointDTO> series = new ArrayList<>();
        for (LocalDateTime bucketStart = from; bucketStart.isBefore(to);
             bucketStart = bucketStart.plusHours(bucketHours)) {

            LocalDateTime bucketEnd = bucketStart.plusHours(bucketHours);
            long occupied = 0L;
            for (Record2<LocalDateTime, LocalDateTime> stay : stays) {
                LocalDateTime entry = stay.value1();
                LocalDateTime exit = stay.value2();
                if (entry != null && entry.isBefore(bucketEnd) && (exit == null || exit.isAfter(bucketStart))) {
                    occupied++;
                }
            }
            series.add(OccupancyPointDTO.builder()
                    .bucketStart(bucketStart)
                    .occupiedSlots(occupied)
                    .totalSlots(totalSlots)
                    .occupancyRate(totalSlots == null || totalSlots == 0
                            ? null
                            : BigDecimal.valueOf(occupied)
                                    .multiply(new BigDecimal("100"))
                                    .divide(BigDecimal.valueOf(totalSlots), 2, RoundingMode.HALF_UP))
                    .build());
        }
        return series;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PeakHourDTO> getPeakHours(UUID parkingLotId, LocalDateTime from, LocalDateTime to) {
        requireRange(from, to);

        List<PeakHourDTO> hours = new ArrayList<>();
        for (Record2<Integer, Integer> row : parkingSlipRepository.entriesByHourOfDay(parkingLotId, from, to)) {
            hours.add(PeakHourDTO.builder()
                    .hourOfDay(row.value1())
                    .entries(row.value2() != null ? row.value2().longValue() : 0L)
                    .build());
        }
        hours.sort(Comparator.comparingLong(PeakHourDTO::getEntries).reversed()
                .thenComparingInt(PeakHourDTO::getHourOfDay));
        return hours;
    }

    private static void requireRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new InvalidRequestException("both 'from' and 'to' are required");
        }
        if (!from.isBefore(to)) {
            throw new InvalidRequestException("'from' (" + from + ") must be before 'to' (" + to + ")");
        }
    }

    private static BigDecimal money(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
