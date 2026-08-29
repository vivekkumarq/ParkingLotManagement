package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;
import com.netcracker.parkinglotmanagement.api.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/blocks")
@Tag(name = "Blocks", description = "Wings of a parking lot")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    /** Now returns the created block, so the caller learns the assigned id. */
    @PostMapping("/create-blocks")
    @Operation(summary = "Create a block")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<BlockDTO> createBlock(@Valid @RequestBody BlockDTO blockDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blockService.createBlock(blockDTO));
    }

    @GetMapping
    @Operation(summary = "List every block")
    public ResponseEntity<List<BlockDTO>> getAllBlocks() {
        return ResponseEntity.ok(blockService.getAllBlocks());
    }

    @GetMapping("/{blockId}/{parkingLotId}")
    @Operation(summary = "Fetch one block within a lot")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "No such block in that lot")
    })
    public ResponseEntity<BlockDTO> getBlockById(@PathVariable UUID blockId, @PathVariable UUID parkingLotId) {
        return ResponseEntity.ok(blockService.getBlockById(blockId, parkingLotId));
    }

    @PutMapping("/{blockId}")
    @Operation(summary = "Update a block")
    public ResponseEntity<BlockDTO> updateBlock(@PathVariable UUID blockId,
                                                @Valid @RequestBody BlockDTO blockDTO) {
        blockDTO.setId(blockId);
        return ResponseEntity.ok(blockService.updateBlock(blockDTO));
    }

    @DeleteMapping("/{blockId}/{parkingLotId}")
    @Operation(summary = "Delete a block")
    @ApiResponse(responseCode = "204", description = "Deleted")
    public ResponseEntity<Void> deleteBlock(@PathVariable UUID blockId, @PathVariable UUID parkingLotId) {
        blockService.deleteBlock(blockId, parkingLotId);
        return ResponseEntity.noContent().build();
    }
}
