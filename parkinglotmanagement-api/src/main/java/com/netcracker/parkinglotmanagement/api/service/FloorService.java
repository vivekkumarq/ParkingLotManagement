package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.dto.FloorDTO;

import java.util.List;
import java.util.UUID;

public interface FloorService {
    FloorDTO createFloor(FloorDTO floorDTO);
    FloorDTO getFloorById(UUID id);
    List<FloorDTO> getAllFloors();
    FloorDTO updateFloor(FloorDTO floorDTO);
    void deleteFloor(UUID id);
}
