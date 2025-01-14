package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;

import java.util.List;
import java.util.UUID;

public interface ParkingLotService {
    ParkingLotDTO createParkingLot(ParkingLotDTO parkingLotDTO);
    ParkingLotDTO getParkingLotById(UUID id);
    List<ParkingLotDTO> getAllParkingLots();
    ParkingLotDTO updateParkingLot(ParkingLotDTO parkingLotDTO);
    void deleteParkingLot(UUID id);
}