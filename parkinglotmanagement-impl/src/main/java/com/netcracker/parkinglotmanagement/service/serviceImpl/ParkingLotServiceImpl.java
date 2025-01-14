package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;
import com.netcracker.parkinglotmanagement.api.service.ParkingLotService;
import com.netcracker.parkinglotmanagement.service.repository.ParkingLotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;

    @Autowired
    public ParkingLotServiceImpl(ParkingLotRepository parkingLotRepository) {
        this.parkingLotRepository = parkingLotRepository;
    }

    @Override
    public ParkingLotDTO createParkingLot(ParkingLotDTO parkingLotDTO) {
        parkingLotRepository.save(parkingLotDTO);
        return parkingLotDTO; // Return the saved DTO
    }

    @Override
    public ParkingLotDTO getParkingLotById(UUID id) {
        return parkingLotRepository.findById(id);
    }

    @Override
    public List<ParkingLotDTO> getAllParkingLots() {
        return parkingLotRepository.findAll();
    }

    @Override
    public ParkingLotDTO updateParkingLot(ParkingLotDTO parkingLotDTO) {
        parkingLotRepository.save(parkingLotDTO);
        return parkingLotDTO; // Return the updated DTO
    }

    @Override
    public void deleteParkingLot(UUID id) {
        ParkingLotDTO parkingLotDTO = getParkingLotById(id);
        if (parkingLotDTO != null) {
            parkingLotRepository.delete(parkingLotDTO);
        }
    }
}