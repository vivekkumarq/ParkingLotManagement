package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.service.ParkingLotService;
import com.netcracker.parkinglotmanagement.service.repository.ParkingLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;

    public ParkingLotServiceImpl(ParkingLotRepository parkingLotRepository) {
        this.parkingLotRepository = parkingLotRepository;
    }

    @Override
    @Transactional
    public ParkingLotDTO createParkingLot(ParkingLotDTO parkingLotDTO) {
        return parkingLotRepository.insert(parkingLotDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingLotDTO getParkingLotById(UUID id) {
        return parkingLotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking lot", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParkingLotDTO> getAllParkingLots() {
        return parkingLotRepository.findAll();
    }

    /**
     * The original implementation called an insert-only {@code save()}, so every
     * update tried to re-insert the row and failed on the primary key.
     */
    @Override
    @Transactional
    public ParkingLotDTO updateParkingLot(ParkingLotDTO parkingLotDTO) {
        if (parkingLotDTO.getId() == null) {
            throw new ResourceNotFoundException("Parking lot id is required for an update");
        }
        if (parkingLotRepository.update(parkingLotDTO) == 0) {
            throw new ResourceNotFoundException("Parking lot", parkingLotDTO.getId());
        }
        return parkingLotDTO;
    }

    @Override
    @Transactional
    public void deleteParkingLot(UUID id) {
        if (parkingLotRepository.deleteById(id) == 0) {
            throw new ResourceNotFoundException("Parking lot", id);
        }
    }
}
