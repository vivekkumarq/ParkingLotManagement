package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import lombok.Data;

import java.util.UUID;

@Data
public class FloorDTO {
    private UUID id;
    private UUID blockId;
    private Integer floorNo;
    private Integer numberOfSlots;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    private final String type = "Floor";

    public FloorDTO(UUID id, UUID blockId, Integer floorNo, Integer numberOfSlots) {
        this.id = id;
        this.blockId = blockId;
        this.floorNo = floorNo;
        this.numberOfSlots = numberOfSlots;
    }
}
