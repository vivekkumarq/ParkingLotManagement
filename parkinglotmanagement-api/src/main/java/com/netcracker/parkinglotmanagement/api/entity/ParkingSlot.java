package com.netcracker.parkinglotmanagement.api.entity;

import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

/**
 * A single bay on a floor.
 *
 * <p>{@code status} and {@code version} back the allocation protocol: a slot is
 * claimed with an UPDATE that matches on both, so exactly one of two concurrent
 * check-ins can win.
 *
 * <p>This class doubles as the input model for jOOQ code generation
 * (see {@code jooqGeneratorConfig.xml}), so its column definitions must stay in
 * step with the Flyway migrations.
 */
@Entity
@Table(name = ParkingLotConstants.TableNames.PARKING_SLOT)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSlot {

    @Id
    @Column(name = ParkingLotConstants.TableColumnNames.ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID id;

    @Column(name = ParkingLotConstants.TableColumnNames.FLOOR_ID, columnDefinition = ParkingLotConstants.TypeNames.UUID_TYPE)
    private UUID floorId;

    @Column(name = ParkingLotConstants.TableColumnNames.SLOT_NUMBER, nullable = false)
    private Integer slotNumber;

    @Column(name = ParkingLotConstants.TableColumnNames.CHARGES, nullable = false)
    private Integer charges;

    @Column(name = ParkingLotConstants.TableColumnNames.VEHICLE_TYPE, nullable = false, length = 20)
    private String vehicleType;

    @Column(name = ParkingLotConstants.TableColumnNames.STATUS, nullable = false, length = 20)
    private String status;

    /** Optimistic-lock counter, bumped by every guarded status transition. */
    @Column(name = ParkingLotConstants.TableColumnNames.VERSION, nullable = false)
    private Long version;
}
