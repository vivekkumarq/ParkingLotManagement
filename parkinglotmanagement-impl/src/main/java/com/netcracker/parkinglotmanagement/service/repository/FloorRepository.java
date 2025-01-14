package com.netcracker.parkinglotmanagement.service.repository;

import com.netcracker.parkinglotmanagement.api.dto.FloorDTO;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.SelectSelectStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import static com.netcracker.parkinglotmanagement.data.Tables.FLOOR;

import java.util.List;
import java.util.UUID;

@Service
public class FloorRepository {

    @Autowired
    @Qualifier("dslContext")
    private DSLContext dsl;

    public void save(FloorDTO dto) {
        dsl.insertInto(FLOOR, FLOOR.ID, FLOOR.BLOCK_ID, FLOOR.FLOOR_NO, FLOOR.NUMBER_OF_SLOTS)
                .values(dto.getId(),
                        dto.getBlockId(),
                        dto.getFloorNo(),
                        dto.getNumberOfSlots())
                .execute();
    }

    public List<FloorDTO> findAll() {
        return selectAsterisk().from(FLOOR)
                .fetchInto(FloorDTO.class);
    }

    public FloorDTO findById(UUID floorId) {
        return selectAsterisk().from(FLOOR)
                .where(FLOOR.ID.eq(floorId))
                .fetchInto(FloorDTO.class).stream().findFirst().orElse(null);
    }

    public void delete(FloorDTO dto) {
        dsl.deleteFrom(FLOOR)
                .where(FLOOR.ID.eq(dto.getId()))
                .execute();
    }

    private SelectSelectStep<Record4<UUID, UUID, Integer, Integer>> selectAsterisk() {
        return dsl.select(FLOOR.ID, FLOOR.BLOCK_ID, FLOOR.FLOOR_NO, FLOOR.NUMBER_OF_SLOTS);
    }
}
