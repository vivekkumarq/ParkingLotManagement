package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;
import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import com.netcracker.parkinglotmanagement.api.service.CustomerService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/create-customer")
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        CustomerDTO createdCustomer = customerService.createCustomer(customerDTO);
        return ResponseEntity.ok(createdCustomer);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable UUID id) {
        CustomerDTO customerDTO = customerService.getCustomerById(id);
        return customerDTO != null ? ResponseEntity.ok(customerDTO) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<CustomerDTO>> getAllCustomers(
            @RequestParam(value = "search", required = false) String search) {

        Specification<CustomerDTO> spec = null;

        if (search != null) {
            Node rootNode = new RSQLParser().parse(search);
            CustomRsqlVisitor<CustomerDTO> visitor = new CustomRsqlVisitor<>();
            spec = rootNode.accept(visitor);
        }

        List<CustomerDTO> customers = customerService.getAllCustomers(spec);
        return ResponseEntity.ok(customers);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable UUID id, @Valid @RequestBody CustomerDTO customerDTO) {
        customerDTO.setId(id);
        CustomerDTO updatedCustomer = customerService.updateCustomer(customerDTO);
        return ResponseEntity.ok(updatedCustomer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
