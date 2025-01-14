package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;

import java.util.List;
import java.util.UUID;

public interface BlockService {
    void createBlock(BlockDTO blockDTO);
    List<BlockDTO> getAllBlocks();
    BlockDTO getBlockById(UUID blockId, UUID parkingLotId);
    void deleteBlock(UUID blockId, UUID parkingLotId);
}
