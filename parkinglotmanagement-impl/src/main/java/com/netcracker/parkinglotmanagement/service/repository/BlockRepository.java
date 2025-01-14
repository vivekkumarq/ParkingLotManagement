package com.netcracker.parkinglotmanagement.service.repository;
//import static com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants;
import static com.netcracker.parkinglotmanagement.data.Tables.BLOCK;
import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.SelectSelectStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

//import static com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants.TableNames.BLOCK;
//import static com.netcracker.parkinglotmanagement.data.Tables.BLOCK;

@Service
public class BlockRepository {

    @Autowired
    @Qualifier("dslContext")
    private DSLContext dsl;

    public BlockDTO save(BlockDTO dto) {
        UUID generatedId = (dto.getId() != null) ? dto.getId() : UUID.randomUUID();

        dsl.insertInto(BLOCK, BLOCK.ID, BLOCK.PARKING_LOT_ID, BLOCK.BLOCK_CODE, BLOCK.NUMBER_OF_FLOORS)
                .values(dto.getId(),
                        dto.getParkingLotId(),
                        dto.getBlockCode(),
                        dto.getNumberOfFloors())
                .execute();
        dto.setId(generatedId);
        return dto;
    }

    public List<BlockDTO> findAll() {
        return selectAsterisk().from(BLOCK)
                .fetchInto(BlockDTO.class);
    }

    public BlockDTO findById(UUID blockId, UUID parkingLotId) {
        return selectAsterisk().from(BLOCK)
                .where(BLOCK.ID.eq(blockId))
                .and(BLOCK.PARKING_LOT_ID.eq(parkingLotId))
                .fetchInto(BlockDTO.class).stream().findFirst().orElse(null);
    }

    public void delete(BlockDTO dto) {
        dsl.deleteFrom(BLOCK)
                .where(BLOCK.ID.eq(dto.getId()))
                .and(BLOCK.PARKING_LOT_ID.eq(dto.getParkingLotId()))
                .execute();
    }

    private SelectSelectStep<Record4<UUID, UUID, String, Integer>> selectAsterisk() {
        return dsl.select(BLOCK.ID, BLOCK.PARKING_LOT_ID, BLOCK.BLOCK_CODE, BLOCK.NUMBER_OF_FLOORS);
    }
}

