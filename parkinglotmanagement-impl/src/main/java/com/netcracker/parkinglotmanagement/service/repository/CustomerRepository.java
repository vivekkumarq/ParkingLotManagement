package com.netcracker.parkinglotmanagement.service.repository;

//import static com.netcracker.parkinglotmanagement.api.consts.ParkingLotConstants.TableNames.CUSTOMER;

import static com.netcracker.parkinglotmanagement.data.Tables.CUSTOMER;


//import com.netcracker.parkinglotmanagement.api.entity.Customer;

import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;
import org.jooq.DSLContext;
import org.jooq.Record5;
import org.jooq.SelectSelectStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Repository
public class CustomerRepository {

    @Autowired
    @Qualifier("dslContext")
    private DSLContext dsl;

    public CustomerDTO save(CustomerDTO dto) {
        if (dto.getId() == null) {
            UUID newId = UUID.randomUUID();
            dto.setId(newId);
            dsl.insertInto(CUSTOMER, CUSTOMER.ID, CUSTOMER.VEHICLE_NUMBER, CUSTOMER.CONTACT_NUMBER, CUSTOMER.NAME, CUSTOMER.EMAIL)
                    .values(newId, dto.getVehicleNumber(), dto.getContactNumber(), dto.getName(), dto.getEmail())
                    .execute();
            return dto;
        } else {
            // Update existing customer
            dsl.update(CUSTOMER)
                    .set(CUSTOMER.VEHICLE_NUMBER, dto.getVehicleNumber())
                    .set(CUSTOMER.CONTACT_NUMBER, dto.getContactNumber())
                    .set(CUSTOMER.NAME, dto.getName())
                    .set(CUSTOMER.EMAIL, dto.getEmail())
                    .where(CUSTOMER.ID.eq(dto.getId()))
                    .execute();
            return dto;
        }
    }

    public CustomerDTO findById(UUID id) {
        return selectAsterisk().from(CUSTOMER)
                .where(CUSTOMER.ID.eq(id))
                .fetchOneInto(CustomerDTO.class);
    }

    public List<CustomerDTO> findAll() {
        return selectAsterisk().from(CUSTOMER)
                .fetchInto(CustomerDTO.class);
    }

    public void delete(UUID id) {
        dsl.deleteFrom(CUSTOMER)
                .where(CUSTOMER.ID.eq(id))
                .execute();
    }

    private SelectSelectStep<Record5<UUID, String, Integer, String, String>> selectAsterisk() {
        return dsl.select(CUSTOMER.ID, CUSTOMER.VEHICLE_NUMBER, CUSTOMER.CONTACT_NUMBER, CUSTOMER.NAME, CUSTOMER.EMAIL);
    }
}