package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.service.BlockService;
import com.netcracker.parkinglotmanagement.service.repository.BlockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BlockServiceImpl implements BlockService {

    private final BlockRepository blockRepository;

    public BlockServiceImpl(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    @Override
    @Transactional
    public BlockDTO createBlock(BlockDTO blockDTO) {
        return blockRepository.insert(blockDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockDTO> getAllBlocks() {
        return blockRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public BlockDTO getBlockById(UUID blockId, UUID parkingLotId) {
        return blockRepository.findById(blockId, parkingLotId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Block " + blockId + " was not found in parking lot " + parkingLotId));
    }

    @Override
    @Transactional
    public BlockDTO updateBlock(BlockDTO blockDTO) {
        if (blockDTO.getId() == null) {
            throw new ResourceNotFoundException("Block id is required for an update");
        }
        if (blockRepository.update(blockDTO) == 0) {
            throw new ResourceNotFoundException("Block", blockDTO.getId());
        }
        return blockDTO;
    }

    @Override
    @Transactional
    public void deleteBlock(UUID blockId, UUID parkingLotId) {
        if (blockRepository.delete(blockId, parkingLotId) == 0) {
            throw new ResourceNotFoundException(
                    "Block " + blockId + " was not found in parking lot " + parkingLotId);
        }
    }
}
