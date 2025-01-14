package com.netcracker.parkinglotmanagement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import lombok.Data;

import java.util.UUID;

@Data
public class CustomerDTO {
    private UUID id;
    private String vehicleNumber;
    private Integer contactNumber;
    private String name;
    private String email;

    @JsonProperty(ParkingLotConstants.TableColumnNames.TYPE)
    private final String type = "Customer";

    public CustomerDTO(UUID id, String vehicleNumber, Integer contactNumber, String name, String email) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.contactNumber = contactNumber;
        this.name = name;
        this.email = email;
    }
}

