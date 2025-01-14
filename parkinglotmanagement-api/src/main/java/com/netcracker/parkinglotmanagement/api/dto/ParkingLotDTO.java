package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import lombok.Data;

import java.util.UUID;

@Data
public class ParkingLotDTO {
    private UUID id;
    private int numberOfBlocks;
    private String address;
    private Double longitude;
    private Double latitude;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    private final String type = "ParkingLot";

    public ParkingLotDTO(UUID id, int numberOfBlocks, String address, Double longitude, Double latitude) {
        this.id = id;
        this.numberOfBlocks = numberOfBlocks;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
    }
}