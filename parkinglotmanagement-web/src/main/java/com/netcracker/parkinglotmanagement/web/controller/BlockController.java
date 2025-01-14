package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;
import com.netcracker.parkinglotmanagement.api.service.BlockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/blocks")
public class BlockController {

    @Autowired
    private BlockService blockService;

    @PostMapping("/create-blocks")
    public ResponseEntity<Void> createBlock(@RequestBody BlockDTO blockDTO) {
        blockService.createBlock(blockDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<BlockDTO>> getAllBlocks() {
        List<BlockDTO> blocks = blockService.getAllBlocks();
        return ResponseEntity.ok(blocks);
    }

    @GetMapping("/{blockId}/{parkingLotId}")
    public ResponseEntity<BlockDTO> getBlockById(@PathVariable UUID blockId, @PathVariable UUID parkingLotId) {
        BlockDTO blockDTO = blockService.getBlockById(blockId, parkingLotId);
        return blockDTO != null ? ResponseEntity.ok(blockDTO) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{blockId}/{parkingLotId}")
    public ResponseEntity<Void> deleteBlock(@PathVariable UUID blockId, @PathVariable UUID parkingLotId) {
        blockService.deleteBlock(blockId, parkingLotId);
        return ResponseEntity.noContent().build();
    }
}
