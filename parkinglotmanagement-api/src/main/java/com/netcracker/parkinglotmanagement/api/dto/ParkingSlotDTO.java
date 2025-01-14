package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import lombok.Data;

import java.util.UUID;

@Data
public class ParkingSlotDTO {
    private UUID id;
    private UUID floorId;
    private Integer slotNumber;
    private Integer charges;
    private String vehicleType;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    private final String type = "ParkingSlot";

    public ParkingSlotDTO(UUID id, UUID floorId, Integer slotNumber, Integer charges, String vehicleType) {
        this.id = id;
        this.floorId = floorId;
        this.slotNumber = slotNumber;
        this.charges = charges;
        this.vehicleType = vehicleType;
    }
}

