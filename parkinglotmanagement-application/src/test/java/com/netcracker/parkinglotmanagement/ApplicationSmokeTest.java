package com.netcracker.parkinglotmanagement;

import com.netcracker.parkinglotmanagement.api.service.BlockService;
import com.netcracker.parkinglotmanagement.api.service.CustomerService;
import com.netcracker.parkinglotmanagement.api.service.FloorService;
import com.netcracker.parkinglotmanagement.api.service.ParkingLotService;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlipService;
import com.netcracker.parkinglotmanagement.api.service.ParkingSlotService;
import com.netcracker.parkinglotmanagement.api.service.ReportingService;
import com.netcracker.parkinglotmanagement.api.service.ReservationService;
import com.netcracker.parkinglotmanagement.api.service.SlotAllocationService;
import com.netcracker.parkinglotmanagement.config.RateCardProperties;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the real application against the {@code test} profile - in-memory H2, real
 * Flyway migrations - and checks that everything is wired.
 *
 * <p>This is the test that would fail if the module split broke: if a service in
 * {@code -impl} were no longer discovered by the component scan, if the DSLContext
 * were not auto-configured from the datasource, or if a migration stopped applying.
 *
 * <p>The servlet layer is exercised through MockMvc rather than a real port. The
 * whole context - filters, converters, the exception advice, actuator and springdoc
 * - is still built and dispatched through; only the socket is left out, which keeps
 * the test independent of whatever the host will let a build agent bind.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationSmokeTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private RateCardProperties rateCard;

    @Test
    @DisplayName("the context starts and every service interface has an implementation")
    void everyServiceIsWired() {
        assertThat(context.getBean(ParkingLotService.class)).isNotNull();
        assertThat(context.getBean(BlockService.class)).isNotNull();
        assertThat(context.getBean(FloorService.class)).isNotNull();
        assertThat(context.getBean(CustomerService.class)).isNotNull();
        assertThat(context.getBean(ParkingSlotService.class)).isNotNull();
        assertThat(context.getBean(ParkingSlipService.class)).isNotNull();
        assertThat(context.getBean(ReservationService.class)).isNotNull();
        assertThat(context.getBean(ReportingService.class)).isNotNull();
        assertThat(context.getBean(SlotAllocationService.class)).isNotNull();
    }

    @Test
    @DisplayName("jOOQ is auto-configured against the pooled datasource")
    void jooqIsWired() {
        assertThat(dsl).isNotNull();
        assertThat(dsl.selectOne().fetchOne(0, Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Hibernate is not started - nothing can alter the schema behind Flyway")
    void jpaIsNotOnTheContext() {
        assertThat(context.getBeanNamesForType(javax.persistence.EntityManagerFactory.class))
                .as("HibernateJpaAutoConfiguration is excluded on purpose")
                .isEmpty();
    }

    @Test
    @DisplayName("the Flyway migrations created the whole schema")
    void migrationsRan() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            for (String table : new String[]{"parking_lot", "block", "floor", "parking_slot",
                    "customer", "reservation", "parking_slip"}) {
                try (ResultSet resultSet = statement.executeQuery("select count(*) from " + table)) {
                    assertThat(resultSet.next()).as("table %s is queryable", table).isTrue();
                }
            }
        }
    }

    @Test
    @DisplayName("the rate card is bound from configuration")
    void rateCardIsBound() {
        assertThat(rateCard.getCurrency()).isEqualTo("INR");
        assertThat(rateCard.getGracePeriodMinutes()).isEqualTo(15);
        assertThat(rateCard.rateFor(com.netcracker.parkinglotmanagement.api.domain.VehicleType.CAR)
                .getDailyCap()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("the actuator health endpoint answers, so the container healthcheck has something to hit")
    void healthEndpointIsUp() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/actuator/health"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("the OpenAPI document is served and describes the parking endpoints")
    void openApiDocumentIsServed() throws Exception {
        String document = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v3/api-docs"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(document)
                .contains("Parking Lot Management API")
                .contains("/parking-lot-management/parking/entry")
                .contains("/parking-lot-management/reservations")
                .contains("/parking-lot-management/reports/revenue")
                .contains("/parking-lot-management/slots/occupancy/{parkingLotId}");
    }

    @Test
    @DisplayName("an unknown resource comes back as the structured error payload, not an HTML page")
    void errorsAreStructured() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/parking-lot-management/parkingLot/00000000-0000-0000-0000-000000000000"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isNotFound())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.status").value(404));
    }
}
