package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.dto.FloorDTO;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.service.FloorService;
import com.netcracker.parkinglotmanagement.service.repository.FloorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FloorServiceImpl implements FloorService {

    private final FloorRepository floorRepository;

    public FloorServiceImpl(FloorRepository floorRepository) {
        this.floorRepository = floorRepository;
    }

    @Override
    @Transactional
    public FloorDTO createFloor(FloorDTO floorDTO) {
        return floorRepository.insert(floorDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public FloorDTO getFloorById(UUID id) {
        return floorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Floor", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FloorDTO> getAllFloors() {
        return floorRepository.findAll();
    }

    @Override
    @Transactional
    public FloorDTO updateFloor(FloorDTO floorDTO) {
        if (floorDTO.getId() == null) {
            throw new ResourceNotFoundException("Floor id is required for an update");
        }
        if (floorRepository.update(floorDTO) == 0) {
            throw new ResourceNotFoundException("Floor", floorDTO.getId());
        }
        return floorDTO;
    }

    @Override
    @Transactional
    public void deleteFloor(UUID id) {
        if (floorRepository.deleteById(id) == 0) {
            throw new ResourceNotFoundException("Floor", id);
        }
    }
}
