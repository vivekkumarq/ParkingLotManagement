package com.netcracker.parkinglotmanagement.service.repository;

import com.netcracker.parkinglotmanagement.api.domain.SlotStatus;
import com.netcracker.parkinglotmanagement.api.domain.VehicleType;
import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;
import com.netcracker.parkinglotmanagement.api.dto.ParkingSlotDTO;
import com.netcracker.parkinglotmanagement.support.DatabaseFixture;
import com.netcracker.parkinglotmanagement.support.IntegrationTest;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static com.netcracker.parkinglotmanagement.data.Tables.PARKING_SLOT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real Flyway migrations against H2 and checks that the schema they
 * produce is the one the generated jOOQ classes expect.
 *
 * <p>This is the test that would catch drift between
 * {@code V1__Create_Parking_Lot_Management_Tables.sql} plus
 * {@code V2__Parking_Operations_And_Schema_Fixes.sql} and the JPA entity model that
 * code generation reads - the class of bug that made the original
 * {@code parking_lot} inserts impossible.
 */
@IntegrationTest
class SchemaMigrationTest {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private DatabaseFixture fixture;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @AfterEach
    void tearDown() {
        fixture.clean();
    }

    @Test
    @DisplayName("both migrations were applied")
    void migrationsApplied() {
        // Raw SQL with explicit quotes: Flyway creates its history table with a
        // quoted lower-case name, which the DSLContext's unquoted rendering (right
        // for the generated classes) would fold to upper case and miss.
        List<String> versions = dsl
                .fetch("select \"version\" from \"flyway_schema_history\" where \"success\" = true")
                .getValues("version", String.class);

        assertThat(versions).contains("1", "2");
    }

    @Test
    @DisplayName("every table the code addresses exists")
    void allTablesExist() throws Exception {
        List<String> tables = new ArrayList<>();
        try (Connection connection = dsl.configuration().connectionProvider().acquire()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (resultSet.next()) {
                    tables.add(resultSet.getString("TABLE_NAME").toLowerCase(Locale.ROOT));
                }
            }
        }

        assertThat(tables).contains(
                "parking_lot", "block", "floor", "parking_slot",
                "customer", "reservation", "parking_slip");
    }

    @Test
    @DisplayName("V2 retyped parking_lot so a lot with ten blocks and real coordinates round-trips")
    void parkingLotColumnsWereRetyped() {
        // Under V1 alone this was impossible: number_of_blocks was CHAR(1) and the
        // coordinates were VARCHAR, while the entity and DTO used Integer/Double.
        ParkingLotDTO saved = parkingLotRepository.insert(new ParkingLotDTO(
                UUID.randomUUID(), 10, "42 Residency Road, Bengaluru", 77.6033, 12.9698));

        Optional<ParkingLotDTO> reloaded = parkingLotRepository.findById(saved.getId());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getNumberOfBlocks()).isEqualTo(10);
        assertThat(reloaded.get().getLongitude()).isEqualTo(77.6033);
        assertThat(reloaded.get().getLatitude()).isEqualTo(12.9698);
    }

    @Test
    @DisplayName("V2 retyped contact_number so a ten-digit phone number survives")
    void contactNumberSurvivesTenDigits() {
        // 9876543210 is larger than Integer.MAX_VALUE (2147483647).
        CustomerDTO saved = customerRepository.insert(new CustomerDTO(
                null, "KA01AB1234", "9876543210", "Asha Rao", "asha.rao@example.com"));

        Optional<CustomerDTO> reloaded = customerRepository.findById(saved.getId());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getContactNumber()).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("a new slot defaults to FREE at version 0")
    void slotDefaultsAreApplied() {
        DatabaseFixture.Lot lot = fixture.createLot(1, 1, VehicleType.CAR);
        ParkingSlotDTO slot = lot.firstSlot();

        Optional<ParkingSlotDTO> reloaded = dsl.select(PARKING_SLOT.fields())
                .from(PARKING_SLOT)
                .where(PARKING_SLOT.ID.eq(slot.getId()))
                .fetchOptional(ParkingSlotRepository::toDto);

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getStatus()).isEqualTo(SlotStatus.FREE);
        assertThat(reloaded.get().getVersion()).isZero();
    }
}
