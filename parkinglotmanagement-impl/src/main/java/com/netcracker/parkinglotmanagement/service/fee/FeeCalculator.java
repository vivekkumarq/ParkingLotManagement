package com.netcracker.parkinglotmanagement.service.fee;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.FeeBreakdownDTO;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.config.RateCardProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Prices a stay from the configured rate card.
 *
 * <p>The rules, in order:
 *
 * <ol>
 *   <li>A stay no longer than the grace period is free.</li>
 *   <li>Beyond that, every complete 24-hour period is billed at the daily cap.</li>
 *   <li>The part-day remainder is billed as {@code firstHour + (h - 1) * perAdditionalHour},
 *       where {@code h} is the remainder rounded <em>up</em> to whole hours, and the result
 *       is itself capped at the daily cap - a 23-hour remainder can never cost more than a
 *       full day.</li>
 *   <li>A stay that ran past a reserved window is charged
 *       {@code overstayPenaltyPerHour} for each started hour of overstay.</li>
 * </ol>
 *
 * <p>Worked example - a car, default tariff (grace 15 min, first hour 50.00,
 * additional hour 30.00, daily cap 400.00), in at 08:00 and out at 11:30 the next
 * day: 27 h 30 m is one complete day (400.00) plus a 3 h 30 m remainder, rounded up
 * to 4 h, billed as 50.00 + 3 x 30.00 = 140.00. Total 540.00.
 *
 * <p>All arithmetic is {@link BigDecimal}; results are scaled to 2 decimals with
 * {@link RoundingMode#HALF_UP}.
 */
@Component
public class FeeCalculator {

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR;

    private final RateCardProperties rateCard;

    public FeeCalculator(RateCardProperties rateCard) {
        this.rateCard = rateCard;
    }

    /**
     * Prices a walk-in stay, with no reservation to overstay.
     *
     * @see #calculate(VehicleType, LocalDateTime, LocalDateTime, LocalDateTime)
     */
    public FeeBreakdownDTO calculate(VehicleType vehicleType, LocalDateTime entryTime, LocalDateTime exitTime) {
        return calculate(vehicleType, entryTime, exitTime, null);
    }

    /**
     * @param reservedUntil end of the reserved window, or null for a walk-in. Any time
     *                      spent past it attracts the overstay penalty.
     * @throws InvalidRequestException if either timestamp is null or the exit precedes the entry
     */
    public FeeBreakdownDTO calculate(VehicleType vehicleType,
                                     LocalDateTime entryTime,
                                     LocalDateTime exitTime,
                                     LocalDateTime reservedUntil) {
        if (vehicleType == null) {
            throw new InvalidRequestException("vehicleType is required to price a stay");
        }
        if (entryTime == null || exitTime == null) {
            throw new InvalidRequestException("both entryTime and exitTime are required to price a stay");
        }
        if (exitTime.isBefore(entryTime)) {
            throw new InvalidRequestException(
                    "exitTime " + exitTime + " precedes entryTime " + entryTime);
        }

        RateCardProperties.Rate rate = rateCard.rateFor(vehicleType);
        long totalMinutes = Duration.between(entryTime, exitTime).toMinutes();
        BigDecimal penalty = overstayPenalty(exitTime, reservedUntil);

        if (totalMinutes <= rateCard.getGracePeriodMinutes()) {
            return FeeBreakdownDTO.builder()
                    .vehicleType(vehicleType)
                    .durationMinutes(totalMinutes)
                    .withinGracePeriod(true)
                    .chargedDays(0L)
                    .chargedHours(0L)
                    .dayCharges(money(BigDecimal.ZERO))
                    .hourCharges(money(BigDecimal.ZERO))
                    .basicCost(money(BigDecimal.ZERO))
                    .penalty(penalty)
                    .totalCost(penalty)
                    .currency(rateCard.getCurrency())
                    .build();
        }

        long chargedDays = totalMinutes / MINUTES_PER_DAY;
        long remainderMinutes = totalMinutes % MINUTES_PER_DAY;
        long chargedHours = ceilToHours(remainderMinutes);

        BigDecimal dayCharges = rate.getDailyCap().multiply(BigDecimal.valueOf(chargedDays));
        BigDecimal hourCharges = BigDecimal.ZERO;
        if (chargedHours > 0) {
            hourCharges = rate.getFirstHour()
                    .add(rate.getPerAdditionalHour().multiply(BigDecimal.valueOf(chargedHours - 1)));
            // A part-day can never cost more than the whole day it sits inside.
            hourCharges = hourCharges.min(rate.getDailyCap());
        }

        BigDecimal basicCost = dayCharges.add(hourCharges);

        return FeeBreakdownDTO.builder()
                .vehicleType(vehicleType)
                .durationMinutes(totalMinutes)
                .withinGracePeriod(false)
                .chargedDays(chargedDays)
                .chargedHours(chargedHours)
                .dayCharges(money(dayCharges))
                .hourCharges(money(hourCharges))
                .basicCost(money(basicCost))
                .penalty(penalty)
                .totalCost(money(basicCost.add(penalty)))
                .currency(rateCard.getCurrency())
                .build();
    }

    private BigDecimal overstayPenalty(LocalDateTime exitTime, LocalDateTime reservedUntil) {
        if (reservedUntil == null || !exitTime.isAfter(reservedUntil)) {
            return money(BigDecimal.ZERO);
        }
        long overstayHours = ceilToHours(Duration.between(reservedUntil, exitTime).toMinutes());
        return money(rateCard.getOverstayPenaltyPerHour().multiply(BigDecimal.valueOf(overstayHours)));
    }

    /** Rounds a minute count up to whole hours; any started hour counts as a full one. */
    private static long ceilToHours(long minutes) {
        if (minutes <= 0) {
            return 0L;
        }
        return (minutes + MINUTES_PER_HOUR - 1) / MINUTES_PER_HOUR;
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }
}
