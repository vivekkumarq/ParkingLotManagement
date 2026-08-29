package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;
import com.netcracker.parkinglotmanagement.api.exception.ResourceNotFoundException;
import com.netcracker.parkinglotmanagement.api.service.CustomerService;
import com.netcracker.parkinglotmanagement.service.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        return customerRepository.insert(customerDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    /**
     * The original implementation accepted a JPA {@code Specification} and then
     * called {@code findAll()} without it, so every {@code ?search=} query silently
     * returned the whole table. The filter is now compiled to a jOOQ condition and
     * pushed into the query.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers(String rsqlFilter) {
        return customerRepository.findAll(rsqlFilter);
    }

    @Override
    @Transactional
    public CustomerDTO updateCustomer(CustomerDTO customerDTO) {
        if (customerDTO.getId() == null) {
            throw new ResourceNotFoundException("Customer id is required for an update");
        }
        if (customerRepository.update(customerDTO) == 0) {
            throw new ResourceNotFoundException("Customer", customerDTO.getId());
        }
        return customerDTO;
    }

    @Override
    @Transactional
    public void deleteCustomer(UUID id) {
        if (customerRepository.deleteById(id) == 0) {
            throw new ResourceNotFoundException("Customer", id);
        }
    }
}
