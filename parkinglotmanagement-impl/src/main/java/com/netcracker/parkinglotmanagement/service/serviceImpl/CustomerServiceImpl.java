package com.netcracker.parkinglotmanagement.service.serviceImpl;

import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;
import com.netcracker.parkinglotmanagement.api.service.CustomerService;
import com.netcracker.parkinglotmanagement.service.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        return customerRepository.save(customerDTO);
    }

    @Override
    public CustomerDTO getCustomerById(UUID id) {
        return customerRepository.findById(id);
    }

    @Override
    public List<CustomerDTO> getAllCustomers(Specification<CustomerDTO> spec) {
        return customerRepository.findAll();
    }

    @Override
    public CustomerDTO updateCustomer(CustomerDTO customerDTO) {
        return customerRepository.save(customerDTO);
    }

    @Override
    public void deleteCustomer(UUID id) {
        customerRepository.delete(id);
    }
}