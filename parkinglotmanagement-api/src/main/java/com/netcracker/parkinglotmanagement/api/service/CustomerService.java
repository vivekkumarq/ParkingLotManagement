package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;

import java.util.List;
import java.util.UUID;

public interface CustomerService {

    CustomerDTO createCustomer(CustomerDTO customerDTO);

    CustomerDTO getCustomerById(UUID id);

    /**
     * @param rsqlFilter optional RSQL expression, e.g. {@code name==Asha;email==*@example.com}.
     *                   Null or blank returns every customer.
     */
    List<CustomerDTO> getAllCustomers(String rsqlFilter);

    CustomerDTO updateCustomer(CustomerDTO customerDTO);

    void deleteCustomer(UUID id);
}
