package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import lombok.Data;

import java.util.UUID;

@Data
public class BlockDTO {
    private UUID id;
    private UUID parkingLotId;
    private String blockCode;
    private Integer numberOfFloors;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    private final String type = "Block";

    public BlockDTO() {
        this.id = UUID.randomUUID();
    }

    public BlockDTO(UUID id, UUID parkingLotId, String blockCode, Integer numberOfFloors) {
        this.id = id;
        this.parkingLotId = parkingLotId;
        this.blockCode= blockCode;
        this.numberOfFloors = numberOfFloors;

    }
}
