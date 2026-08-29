package com.netcracker.parkinglotmanagement.service.repository;

import com.netcracker.parkinglotmanagement.api.dto.FloorDTO;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.netcracker.parkinglotmanagement.data.Tables.FLOOR;

@Repository
public class FloorRepository {

    private final DSLContext dsl;

    public FloorRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public FloorDTO insert(FloorDTO dto) {
        UUID id = dto.getId() != null ? dto.getId() : UUID.randomUUID();
        dsl.insertInto(FLOOR)
                .set(FLOOR.ID, id)
                .set(FLOOR.BLOCK_ID, dto.getBlockId())
                .set(FLOOR.FLOOR_NO, dto.getFloorNo())
                .set(FLOOR.NUMBER_OF_SLOTS, dto.getNumberOfSlots())
                .execute();
        dto.setId(id);
        return dto;
    }

    public int update(FloorDTO dto) {
        return dsl.update(FLOOR)
                .set(FLOOR.BLOCK_ID, dto.getBlockId())
                .set(FLOOR.FLOOR_NO, dto.getFloorNo())
                .set(FLOOR.NUMBER_OF_SLOTS, dto.getNumberOfSlots())
                .where(FLOOR.ID.eq(dto.getId()))
                .execute();
    }

    public List<FloorDTO> findAll() {
        return dsl.select(FLOOR.fields())
                .from(FLOOR)
                .orderBy(FLOOR.FLOOR_NO)
                .fetch(FloorRepository::toDto);
    }

    public Optional<FloorDTO> findById(UUID id) {
        return dsl.select(FLOOR.fields())
                .from(FLOOR)
                .where(FLOOR.ID.eq(id))
                .fetchOptional(FloorRepository::toDto);
    }

    public int deleteById(UUID id) {
        return dsl.deleteFrom(FLOOR).where(FLOOR.ID.eq(id)).execute();
    }

    static FloorDTO toDto(Record record) {
        FloorDTO dto = new FloorDTO();
        dto.setId(record.get(FLOOR.ID));
        dto.setBlockId(record.get(FLOOR.BLOCK_ID));
        dto.setFloorNo(record.get(FLOOR.FLOOR_NO));
        dto.setNumberOfSlots(record.get(FLOOR.NUMBER_OF_SLOTS));
        return dto;
    }
}
