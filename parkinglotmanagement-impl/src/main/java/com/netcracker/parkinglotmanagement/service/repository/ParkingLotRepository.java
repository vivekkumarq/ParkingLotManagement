package com.netcracker.parkinglotmanagement.service.repository;
import static com.netcracker.parkinglotmanagement.data.Tables.PARKING_LOT;

import com.netcracker.parkinglotmanagement.api.dto.ParkingLotDTO;
import org.jooq.DSLContext;
import org.jooq.Record5;
import org.jooq.SelectSelectStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import static com.netcracker.parkinglotmanagement.data.Tables.PARKING_LOT;

import java.util.List;
import java.util.UUID;

//import static com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants.TableNames.PARKING_LOT;


@Service
public class ParkingLotRepository {

    @Autowired
    @Qualifier("dslContext")
    private DSLContext dsl;

    public void save(ParkingLotDTO dto) {
        dsl.insertInto(PARKING_LOT, PARKING_LOT.ID, PARKING_LOT.NUMBER_OF_BLOCKS, PARKING_LOT.ADDRESS, PARKING_LOT.LONGITUDE, PARKING_LOT.LATITUDE)
                .values(dto.getId(),
                        dto.getNumberOfBlocks(),
                        dto.getAddress(),
                        dto.getLongitude(),
                        dto.getLatitude())
                .execute();
    }

    public List<ParkingLotDTO> findAll() {
        return selectAsterisk().from(PARKING_LOT)
                .fetchInto(ParkingLotDTO.class);
    }

    public ParkingLotDTO findById(UUID id) {
        return selectAsterisk().from(PARKING_LOT)
                .where(PARKING_LOT.ID.eq(id))
                .fetchInto(ParkingLotDTO.class).stream().findFirst().orElse(null);
    }

    public void delete(ParkingLotDTO dto) {
        dsl.deleteFrom(PARKING_LOT).where(PARKING_LOT.ID.eq(dto.getId())).execute();
    }

    private SelectSelectStep<Record5<UUID, Integer, String, Double, Double>> selectAsterisk() {
        return dsl.select(PARKING_LOT.ID, PARKING_LOT.NUMBER_OF_BLOCKS, PARKING_LOT.ADDRESS, PARKING_LOT.LONGITUDE, PARKING_LOT.LATITUDE);
    }
}
