package com.netcracker.parkinglotmanagement.service.repository;

import com.netcracker.parkinglotmanagement.api.dto.BlockDTO;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.netcracker.parkinglotmanagement.data.Tables.BLOCK;

@Repository
public class BlockRepository {

    private final DSLContext dsl;

    public BlockRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * The original implementation generated an id, inserted {@code dto.getId()} -
     * still null - and then handed back a DTO carrying an id that had never been
     * persisted. The generated id is now what actually goes into the row.
     */
    public BlockDTO insert(BlockDTO dto) {
        UUID id = dto.getId() != null ? dto.getId() : UUID.randomUUID();
        dsl.insertInto(BLOCK)
                .set(BLOCK.ID, id)
                .set(BLOCK.PARKING_LOT_ID, dto.getParkingLotId())
                .set(BLOCK.BLOCK_CODE, dto.getBlockCode())
                .set(BLOCK.NUMBER_OF_FLOORS, dto.getNumberOfFloors())
                .execute();
        dto.setId(id);
        return dto;
    }

    public int update(BlockDTO dto) {
        return dsl.update(BLOCK)
                .set(BLOCK.PARKING_LOT_ID, dto.getParkingLotId())
                .set(BLOCK.BLOCK_CODE, dto.getBlockCode())
                .set(BLOCK.NUMBER_OF_FLOORS, dto.getNumberOfFloors())
                .where(BLOCK.ID.eq(dto.getId()))
                .execute();
    }

    public List<BlockDTO> findAll() {
        return dsl.select(BLOCK.fields())
                .from(BLOCK)
                .orderBy(BLOCK.BLOCK_CODE)
                .fetch(BlockRepository::toDto);
    }

    public Optional<BlockDTO> findById(UUID blockId, UUID parkingLotId) {
        return dsl.select(BLOCK.fields())
                .from(BLOCK)
                .where(BLOCK.ID.eq(blockId))
                .and(BLOCK.PARKING_LOT_ID.eq(parkingLotId))
                .fetchOptional(BlockRepository::toDto);
    }

    public int delete(UUID blockId, UUID parkingLotId) {
        return dsl.deleteFrom(BLOCK)
                .where(BLOCK.ID.eq(blockId))
                .and(BLOCK.PARKING_LOT_ID.eq(parkingLotId))
                .execute();
    }

    static BlockDTO toDto(Record record) {
        BlockDTO dto = new BlockDTO();
        dto.setId(record.get(BLOCK.ID));
        dto.setParkingLotId(record.get(BLOCK.PARKING_LOT_ID));
        dto.setBlockCode(record.get(BLOCK.BLOCK_CODE));
        dto.setNumberOfFloors(record.get(BLOCK.NUMBER_OF_FLOORS));
        return dto;
    }
}
