package com.netcracker.parkinglotmanagement.service.repository;

import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;
import com.netcracker.parkinglotmanagement.service.rsql.RsqlFilter;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.netcracker.parkinglotmanagement.data.Tables.CUSTOMER;

@Repository
public class CustomerRepository {

    /** The only fields an RSQL search expression is allowed to name. */
    private static final Map<String, Field<?>> FILTERABLE = filterable();

    private final DSLContext dsl;

    public CustomerRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public static Map<String, Field<?>> filterableFields() {
        return FILTERABLE;
    }

    public CustomerDTO insert(CustomerDTO dto) {
        UUID id = dto.getId() != null ? dto.getId() : UUID.randomUUID();
        dsl.insertInto(CUSTOMER)
                .set(CUSTOMER.ID, id)
                .set(CUSTOMER.VEHICLE_NUMBER, dto.getVehicleNumber())
                .set(CUSTOMER.CONTACT_NUMBER, dto.getContactNumber())
                .set(CUSTOMER.NAME, dto.getName())
                .set(CUSTOMER.EMAIL, dto.getEmail())
                .execute();
        dto.setId(id);
        return dto;
    }

    public int update(CustomerDTO dto) {
        return dsl.update(CUSTOMER)
                .set(CUSTOMER.VEHICLE_NUMBER, dto.getVehicleNumber())
                .set(CUSTOMER.CONTACT_NUMBER, dto.getContactNumber())
                .set(CUSTOMER.NAME, dto.getName())
                .set(CUSTOMER.EMAIL, dto.getEmail())
                .where(CUSTOMER.ID.eq(dto.getId()))
                .execute();
    }

    public Optional<CustomerDTO> findById(UUID id) {
        return dsl.select(CUSTOMER.fields())
                .from(CUSTOMER)
                .where(CUSTOMER.ID.eq(id))
                .fetchOptional(CustomerRepository::toDto);
    }

    /**
     * @param rsqlFilter optional RSQL expression; null or blank returns every row.
     *                   The filter is compiled to a jOOQ condition with bind
     *                   parameters and a whitelisted column set.
     */
    public List<CustomerDTO> findAll(String rsqlFilter) {
        Condition condition = RsqlFilter.toCondition(rsqlFilter, FILTERABLE);
        return dsl.select(CUSTOMER.fields())
                .from(CUSTOMER)
                .where(condition)
                .orderBy(CUSTOMER.NAME)
                .fetch(CustomerRepository::toDto);
    }

    public boolean existsById(UUID id) {
        return dsl.fetchExists(dsl.selectOne().from(CUSTOMER).where(CUSTOMER.ID.eq(id)));
    }

    public int deleteById(UUID id) {
        return dsl.deleteFrom(CUSTOMER).where(CUSTOMER.ID.eq(id)).execute();
    }

    static CustomerDTO toDto(Record record) {
        CustomerDTO dto = new CustomerDTO();
        dto.setId(record.get(CUSTOMER.ID));
        dto.setVehicleNumber(record.get(CUSTOMER.VEHICLE_NUMBER));
        dto.setContactNumber(record.get(CUSTOMER.CONTACT_NUMBER));
        dto.setName(record.get(CUSTOMER.NAME));
        dto.setEmail(record.get(CUSTOMER.EMAIL));
        return dto;
    }

    private static Map<String, Field<?>> filterable() {
        Map<String, Field<?>> fields = new LinkedHashMap<>();
        fields.put("id", CUSTOMER.ID);
        fields.put("vehicleNumber", CUSTOMER.VEHICLE_NUMBER);
        fields.put("contactNumber", CUSTOMER.CONTACT_NUMBER);
        fields.put("name", CUSTOMER.NAME);
        fields.put("email", CUSTOMER.EMAIL);
        return Collections.unmodifiableMap(fields);
    }
}
