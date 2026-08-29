package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;

import java.util.List;
import java.util.UUID;

public interface BlockService {

    /** @return the stored block, including the identifier the server assigned */
    BlockDTO createBlock(BlockDTO blockDTO);

    List<BlockDTO> getAllBlocks();

    /**
     * @throws com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException
     *         when the block does not exist in that lot
     */
    BlockDTO getBlockById(UUID blockId, UUID parkingLotId);

    BlockDTO updateBlock(BlockDTO blockDTO);

    void deleteBlock(UUID blockId, UUID parkingLotId);
}
