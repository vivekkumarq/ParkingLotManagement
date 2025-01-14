package com.netcracker.parkinglotmanagement.api.service;

import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerDTO createCustomer(CustomerDTO customerDTO);
    CustomerDTO getCustomerById(UUID id);
    List<CustomerDTO> getAllCustomers(Specification<CustomerDTO> spec);
    CustomerDTO updateCustomer(CustomerDTO customerDTO);
    void deleteCustomer(UUID id);
}
