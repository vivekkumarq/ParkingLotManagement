package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;
import com.netcracker.parkinglotmanagement.api.service.BlockService;
import com.netcracker.parkinglotmanagement.service.repository.BlockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BlockServiceImpl implements BlockService {


    private final BlockRepository blockRepository;

    @Autowired
    public BlockServiceImpl(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

        @Override
        public void createBlock (BlockDTO blockDTO){
            blockRepository.save(blockDTO);
        }

        @Override
        public List<BlockDTO> getAllBlocks () {
            return blockRepository.findAll();
        }

        @Override
        public BlockDTO getBlockById (UUID blockId, UUID parkingLotId){
            return blockRepository.findById(blockId, parkingLotId);
        }

        @Override
        public void deleteBlock (UUID blockId, UUID parkingLotId){
            BlockDTO blockDTO = blockRepository.findById(blockId, parkingLotId);
            if (blockDTO != null) {
                blockRepository.delete(blockDTO);
            }
        }
    }