package com.netcracker.parkinglotmanagement.config;

import com.netcracker.parkinglotmanagement.api.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Releases slots held by bookings that were never claimed.
 *
 * <p>Without this a no-show would hold its bay forever. The sweep is idempotent -
 * it only moves BOOKED rows whose window has already closed - so running it twice,
 * or alongside a manual call to {@code POST /reservations/expire}, is harmless.
 *
 * <p>Disable with {@code parking.reservation.expiry.enabled=false}; change the
 * cadence with {@code parking.reservation.expiry.interval-ms}.
 */
@Component
@ConditionalOnProperty(name = "parking.reservation.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationExpiryJob {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final ReservationService reservationService;
    private final Clock clock;

    public ReservationExpiryJob(ReservationService reservationService, Clock clock) {
        this.reservationService = reservationService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${parking.reservation.expiry.interval-ms:300000}",
            initialDelayString = "${parking.reservation.expiry.initial-delay-ms:60000}")
    public void expireStaleReservations() {
        try {
            int expired = reservationService.expireStaleReservations(LocalDateTime.now(clock));
            if (expired > 0) {
                LOG.info("Reservation expiry sweep released {} slot(s)", expired);
            }
        } catch (RuntimeException e) {
            // Never let a failed sweep kill the scheduler thread.
            LOG.error("Reservation expiry sweep failed; it will run again on the next tick", e);
        }
    }
}
