package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.dto.FloorDTO;
import com.netcracker.parkinglotmanagement.api.service.FloorService;
import com.netcracker.parkinglotmanagement.service.repository.FloorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FloorServiceImpl implements FloorService {

    private final FloorRepository floorRepository;

    @Autowired
    public FloorServiceImpl(FloorRepository floorRepository) {
        this.floorRepository = floorRepository;
    }

    @Override
    public FloorDTO createFloor(FloorDTO floorDTO) {
        floorRepository.save(floorDTO);
        return floorDTO; // Return the created floor
    }

    @Override
    public FloorDTO getFloorById(UUID id) {
        return floorRepository.findById(id);
    }

    @Override
    public List<FloorDTO> getAllFloors() {
        return floorRepository.findAll(); // Uncomment this method in FloorRepository to fetch all
    }

    @Override
    public FloorDTO updateFloor(FloorDTO floorDTO) {
        floorRepository.save(floorDTO);
        return floorDTO; // Return the updated floor
    }

    @Override
    public void deleteFloor(UUID id) {
        FloorDTO floorDTO = floorRepository.findById(id);
        if (floorDTO != null) {
            floorRepository.delete(floorDTO);
        }
    }
}
