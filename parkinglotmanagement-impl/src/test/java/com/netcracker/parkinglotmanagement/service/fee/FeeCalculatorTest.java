package com.netcracker.parkinglotmanagement.service.fee;

import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.FeeBreakdownDTO;
import com.netcracker.parkinglotmanagement.api.exception.InvalidRequestException;
import com.netcracker.parkinglotmanagement.config.RateCardProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The tariff under test is the shipped default:
 *
 * <pre>
 *   grace period      15 minutes
 *   overstay penalty  50.00 per started hour
 *
 *                first hour   additional hour   daily cap
 *   MOTORCYCLE        20.00             10.00      100.00
 *   CAR               50.00             30.00      400.00
 *   TRUCK            100.00             60.00      800.00
 * </pre>
 */
class FeeCalculatorTest {

    private static final LocalDateTime ENTRY = LocalDateTime.of(2026, 3, 1, 8, 0);

    private FeeCalculator feeCalculator;

    @BeforeEach
    void setUp() {
        feeCalculator = new FeeCalculator(new RateCardProperties());
    }

    private FeeBreakdownDTO priceCarStayOf(long minutes) {
        return feeCalculator.calculate(VehicleType.CAR, ENTRY, ENTRY.plusMinutes(minutes));
    }

    @Nested
    @DisplayName("grace period")
    class GracePeriod {

        @Test
        @DisplayName("a zero-length stay is free")
        void zeroLengthStayIsFree() {
            FeeBreakdownDTO fee = priceCarStayOf(0);

            assertThat(fee.getWithinGracePeriod()).isTrue();
            assertThat(fee.getTotalCost()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("exactly at the grace boundary the stay is still free")
        void exactlyAtBoundaryIsFree() {
            FeeBreakdownDTO fee = priceCarStayOf(15);

            assertThat(fee.getWithinGracePeriod()).isTrue();
            assertThat(fee.getBasicCost()).isEqualByComparingTo("0.00");
            assertThat(fee.getChargedDays()).isZero();
            assertThat(fee.getChargedHours()).isZero();
        }

        @Test
        @DisplayName("one minute past the boundary the first hour becomes payable in full")
        void oneMinutePastBoundaryCostsAWholeHour() {
            FeeBreakdownDTO fee = priceCarStayOf(16);

            assertThat(fee.getWithinGracePeriod()).isFalse();
            assertThat(fee.getChargedHours()).isEqualTo(1L);
            assertThat(fee.getTotalCost()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("the grace period is configurable")
        void gracePeriodIsConfigurable() {
            RateCardProperties generous = new RateCardProperties();
            generous.setGracePeriodMinutes(60);

            FeeBreakdownDTO fee = new FeeCalculator(generous)
                    .calculate(VehicleType.CAR, ENTRY, ENTRY.plusMinutes(45));

            assertThat(fee.getWithinGracePeriod()).isTrue();
            assertThat(fee.getTotalCost()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("hourly charging")
    class HourlyCharging {

        @ParameterizedTest(name = "a {0}-minute stay costs {1}")
        @CsvSource({
                // minutes, expected total for a CAR
                "16,     50.00",   // any started hour past grace is a full first hour
                "60,     50.00",   // exactly one hour
                "61,     80.00",   // rolls into the second hour: 50 + 30
                "120,    80.00",   // exactly two hours
                "121,   110.00",   // 50 + 30 + 30
                "180,   110.00",   // exactly three hours
                "600,   320.00",   // ten hours: 50 + 9 x 30
                "780,   400.00",   // thirteen hours would be 410, capped at the daily 400
                "1439,  400.00"    // one minute short of a day, still capped
        })
        void chargesPerStartedHourUpToTheDailyCap(long minutes, String expectedTotal) {
            assertThat(priceCarStayOf(minutes).getTotalCost()).isEqualByComparingTo(expectedTotal);
        }

        @Test
        @DisplayName("a part hour is always rounded up")
        void partHoursRoundUp() {
            assertThat(priceCarStayOf(90).getChargedHours()).isEqualTo(2L);
            assertThat(priceCarStayOf(91).getChargedHours()).isEqualTo(2L);
            assertThat(priceCarStayOf(121).getChargedHours()).isEqualTo(3L);
        }

        @Test
        @DisplayName("a long part-day never costs more than the whole day it sits in")
        void partDayIsCappedAtTheDailyRate() {
            FeeBreakdownDTO fee = priceCarStayOf(23 * 60);

            // 50 + 22 x 30 = 710 uncapped
            assertThat(fee.getHourCharges()).isEqualByComparingTo("400.00");
            assertThat(fee.getTotalCost()).isEqualByComparingTo("400.00");
        }
    }

    @Nested
    @DisplayName("multi-day charging")
    class MultiDayCharging {

        @Test
        @DisplayName("exactly 24 hours is one capped day and no remainder")
        void exactlyOneDay() {
            FeeBreakdownDTO fee = priceCarStayOf(24 * 60);

            assertThat(fee.getChargedDays()).isEqualTo(1L);
            assertThat(fee.getChargedHours()).isZero();
            assertThat(fee.getDayCharges()).isEqualByComparingTo("400.00");
            assertThat(fee.getHourCharges()).isEqualByComparingTo("0.00");
            assertThat(fee.getTotalCost()).isEqualByComparingTo("400.00");
        }

        @Test
        @DisplayName("one day plus one hour adds a single first-hour charge")
        void oneDayPlusOneHour() {
            FeeBreakdownDTO fee = priceCarStayOf(25 * 60);

            assertThat(fee.getChargedDays()).isEqualTo(1L);
            assertThat(fee.getChargedHours()).isEqualTo(1L);
            assertThat(fee.getTotalCost()).isEqualByComparingTo("450.00");
        }

        @Test
        @DisplayName("the worked example from the README adds up")
        void readmeWorkedExample() {
            // In 2026-03-01 08:00, out 2026-03-02 11:30 - 27 h 30 m.
            LocalDateTime exit = LocalDateTime.of(2026, 3, 2, 11, 30);
            FeeBreakdownDTO fee = feeCalculator.calculate(VehicleType.CAR, ENTRY, exit);

            assertThat(fee.getDurationMinutes()).isEqualTo(1650L);
            assertThat(fee.getChargedDays()).isEqualTo(1L);      // 24 h at the cap
            assertThat(fee.getChargedHours()).isEqualTo(4L);     // 3 h 30 m rounded up
            assertThat(fee.getDayCharges()).isEqualByComparingTo("400.00");
            assertThat(fee.getHourCharges()).isEqualByComparingTo("140.00"); // 50 + 3 x 30
            assertThat(fee.getBasicCost()).isEqualByComparingTo("540.00");
            assertThat(fee.getPenalty()).isEqualByComparingTo("0.00");
            assertThat(fee.getTotalCost()).isEqualByComparingTo("540.00");
        }

        @Test
        @DisplayName("three whole days are three capped days")
        void threeWholeDays() {
            FeeBreakdownDTO fee = priceCarStayOf(3 * 24 * 60);

            assertThat(fee.getChargedDays()).isEqualTo(3L);
            assertThat(fee.getTotalCost()).isEqualByComparingTo("1200.00");
        }
    }

    @Nested
    @DisplayName("per vehicle type")
    class PerVehicleType {

        @ParameterizedTest(name = "{0} pays {1} for two hours")
        @CsvSource({
                "MOTORCYCLE,  30.00",   // 20 + 10
                "CAR,         80.00",   // 50 + 30
                "TRUCK,      160.00"    // 100 + 60
        })
        void twoHourStay(VehicleType vehicleType, String expected) {
            FeeBreakdownDTO fee = feeCalculator.calculate(vehicleType, ENTRY, ENTRY.plusHours(2));
            assertThat(fee.getTotalCost()).isEqualByComparingTo(expected);
        }

        @ParameterizedTest(name = "{0} pays its own daily cap for a full day")
        @CsvSource({
                "MOTORCYCLE, 100.00",
                "CAR,        400.00",
                "TRUCK,      800.00"
        })
        void fullDayUsesTheTypeCap(VehicleType vehicleType, String expected) {
            FeeBreakdownDTO fee = feeCalculator.calculate(vehicleType, ENTRY, ENTRY.plusHours(24));
            assertThat(fee.getTotalCost()).isEqualByComparingTo(expected);
        }

        @ParameterizedTest
        @EnumSource(VehicleType.class)
        @DisplayName("every vehicle type has a tariff and returns money at 2 decimals")
        void everyTypeIsPriced(VehicleType vehicleType) {
            FeeBreakdownDTO fee = feeCalculator.calculate(vehicleType, ENTRY, ENTRY.plusHours(5));

            assertThat(fee.getTotalCost()).isNotNull();
            assertThat(fee.getTotalCost().scale()).isEqualTo(2);
            assertThat(fee.getVehicleType()).isEqualTo(vehicleType);
            assertThat(fee.getCurrency()).isEqualTo("INR");
        }
    }

    @Nested
    @DisplayName("BigDecimal handling")
    class MoneyHandling {

        @Test
        @DisplayName("every money field is scaled to exactly two decimals")
        void everyMoneyFieldHasTwoDecimals() {
            FeeBreakdownDTO fee = priceCarStayOf(1650);

            assertThat(fee.getDayCharges().scale()).isEqualTo(2);
            assertThat(fee.getHourCharges().scale()).isEqualTo(2);
            assertThat(fee.getBasicCost().scale()).isEqualTo(2);
            assertThat(fee.getPenalty().scale()).isEqualTo(2);
            assertThat(fee.getTotalCost().scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("a tariff with sub-paise precision is rounded HALF_UP to two decimals")
        void roundsHalfUp() {
            RateCardProperties awkward = new RateCardProperties();
            awkward.getRates().put(VehicleType.CAR, new RateCardProperties.Rate(
                    new BigDecimal("10.005"), new BigDecimal("0.001"), new BigDecimal("999.00")));

            FeeBreakdownDTO fee = new FeeCalculator(awkward)
                    .calculate(VehicleType.CAR, ENTRY, ENTRY.plusHours(1));

            // 10.005 rounds up to 10.01
            assertThat(fee.getBasicCost()).isEqualByComparingTo("10.01");
        }

        @Test
        @DisplayName("totalCost is exactly basicCost + penalty")
        void totalIsBasicPlusPenalty() {
            LocalDateTime reservedUntil = ENTRY.plusHours(2);
            FeeBreakdownDTO fee = feeCalculator.calculate(
                    VehicleType.CAR, ENTRY, ENTRY.plusHours(4), reservedUntil);

            assertThat(fee.getTotalCost())
                    .isEqualByComparingTo(fee.getBasicCost().add(fee.getPenalty()));
        }
    }

    @Nested
    @DisplayName("reservation overstay penalty")
    class OverstayPenalty {

        @Test
        @DisplayName("a walk-in never attracts a penalty")
        void walkInHasNoPenalty() {
            assertThat(priceCarStayOf(600).getPenalty()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("leaving inside the reserved window attracts no penalty")
        void insideWindowHasNoPenalty() {
            FeeBreakdownDTO fee = feeCalculator.calculate(
                    VehicleType.CAR, ENTRY, ENTRY.plusHours(2), ENTRY.plusHours(3));

            assertThat(fee.getPenalty()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("leaving exactly at the end of the window attracts no penalty")
        void exactlyAtWindowEndHasNoPenalty() {
            LocalDateTime end = ENTRY.plusHours(3);
            FeeBreakdownDTO fee = feeCalculator.calculate(VehicleType.CAR, ENTRY, end, end);

            assertThat(fee.getPenalty()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("each started hour of overstay costs the configured penalty")
        void overstayIsChargedPerStartedHour() {
            LocalDateTime reservedUntil = ENTRY.plusHours(2);

            // 2 h 1 m past the window = 3 started hours x 50.00
            FeeBreakdownDTO fee = feeCalculator.calculate(
                    VehicleType.CAR, ENTRY, ENTRY.plusHours(4).plusMinutes(1), reservedUntil);

            assertThat(fee.getPenalty()).isEqualByComparingTo("150.00");
        }

        @Test
        @DisplayName("a stay inside the grace period still pays an overstay penalty")
        void gracePeriodDoesNotWaiveThePenalty() {
            // Arrived late and left almost immediately, but after the window closed.
            LocalDateTime reservedUntil = ENTRY.minusHours(1);
            FeeBreakdownDTO fee = feeCalculator.calculate(
                    VehicleType.CAR, ENTRY, ENTRY.plusMinutes(10), reservedUntil);

            assertThat(fee.getWithinGracePeriod()).isTrue();
            assertThat(fee.getBasicCost()).isEqualByComparingTo("0.00");
            assertThat(fee.getPenalty()).isEqualByComparingTo("100.00"); // 1 h 10 m -> 2 hours
            assertThat(fee.getTotalCost()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("invalid input")
    class InvalidInput {

        @Test
        void exitBeforeEntryIsRejected() {
            assertThatThrownBy(() -> feeCalculator.calculate(
                    VehicleType.CAR, ENTRY, ENTRY.minusMinutes(1)))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessageContaining("precedes");
        }

        @Test
        void nullTimestampsAreRejected() {
            assertThatThrownBy(() -> feeCalculator.calculate(VehicleType.CAR, null, ENTRY))
                    .isInstanceOf(InvalidRequestException.class);
            assertThatThrownBy(() -> feeCalculator.calculate(VehicleType.CAR, ENTRY, null))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        void nullVehicleTypeIsRejected() {
            assertThatThrownBy(() -> feeCalculator.calculate(null, ENTRY, ENTRY.plusHours(1)))
                    .isInstanceOf(InvalidRequestException.class);
        }

        @Test
        @DisplayName("a vehicle type with no configured tariff fails loudly, not silently at zero")
        void missingTariffIsAnError() {
            RateCardProperties incomplete = new RateCardProperties();
            incomplete.getRates().remove(VehicleType.TRUCK);

            assertThatThrownBy(() -> new FeeCalculator(incomplete)
                    .calculate(VehicleType.TRUCK, ENTRY, ENTRY.plusHours(1)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("TRUCK");
        }
    }
}
