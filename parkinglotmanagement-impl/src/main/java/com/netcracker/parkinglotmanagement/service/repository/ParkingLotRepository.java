package com.netcracker.parkinglotmanagement.service.repository;

import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.netcracker.parkinglotmanagement.data.Tables.PARKING_LOT;

@Repository
public class ParkingLotRepository {

    private final DSLContext dsl;

    public ParkingLotRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public ParkingLotDTO insert(ParkingLotDTO dto) {
        UUID id = dto.getId() != null ? dto.getId() : UUID.randomUUID();
        dsl.insertInto(PARKING_LOT)
                .set(PARKING_LOT.ID, id)
                .set(PARKING_LOT.NUMBER_OF_BLOCKS, dto.getNumberOfBlocks())
                .set(PARKING_LOT.ADDRESS, dto.getAddress())
                .set(PARKING_LOT.LONGITUDE, dto.getLongitude())
                .set(PARKING_LOT.LATITUDE, dto.getLatitude())
                .execute();
        dto.setId(id);
        return dto;
    }

    /** @return rows changed, so the caller can distinguish a missing row from an updated one */
    public int update(ParkingLotDTO dto) {
        return dsl.update(PARKING_LOT)
                .set(PARKING_LOT.NUMBER_OF_BLOCKS, dto.getNumberOfBlocks())
                .set(PARKING_LOT.ADDRESS, dto.getAddress())
                .set(PARKING_LOT.LONGITUDE, dto.getLongitude())
                .set(PARKING_LOT.LATITUDE, dto.getLatitude())
                .where(PARKING_LOT.ID.eq(dto.getId()))
                .execute();
    }

    public List<ParkingLotDTO> findAll() {
        return dsl.select(PARKING_LOT.fields())
                .from(PARKING_LOT)
                .orderBy(PARKING_LOT.ADDRESS)
                .fetch(ParkingLotRepository::toDto);
    }

    public Optional<ParkingLotDTO> findById(UUID id) {
        return dsl.select(PARKING_LOT.fields())
                .from(PARKING_LOT)
                .where(PARKING_LOT.ID.eq(id))
                .fetchOptional(ParkingLotRepository::toDto);
    }

    public boolean existsById(UUID id) {
        return dsl.fetchExists(dsl.selectOne().from(PARKING_LOT).where(PARKING_LOT.ID.eq(id)));
    }

    public int deleteById(UUID id) {
        return dsl.deleteFrom(PARKING_LOT).where(PARKING_LOT.ID.eq(id)).execute();
    }

    static ParkingLotDTO toDto(Record record) {
        ParkingLotDTO dto = new ParkingLotDTO();
        dto.setId(record.get(PARKING_LOT.ID));
        dto.setNumberOfBlocks(record.get(PARKING_LOT.NUMBER_OF_BLOCKS));
        dto.setAddress(record.get(PARKING_LOT.ADDRESS));
        dto.setLongitude(record.get(PARKING_LOT.LONGITUDE));
        dto.setLatitude(record.get(PARKING_LOT.LATITUDE));
        return dto;
    }
}
