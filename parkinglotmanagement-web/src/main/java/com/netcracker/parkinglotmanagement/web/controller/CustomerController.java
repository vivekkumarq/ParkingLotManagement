package com.netcracker.parkinglotmanagement.web.controller;

import com.netcracker.parkinglotmanagement.api.dto.CustomerDTO;
import com.netcracker.parkinglotmanagement.api.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/parking-lot-management/customer")
@Tag(name = "Customers", description = "Registered customers and their vehicles")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/create-customer")
    @Operation(summary = "Register a customer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<CustomerDTO> createCustomer(@Valid @RequestBody CustomerDTO customerDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(customerDTO));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one customer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "No such customer")
    })
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    /**
     * The {@code search} expression used to be parsed into a JPA Specification and
     * then dropped on the floor by the service, so the filter never had any effect.
     * It is now compiled to a jOOQ condition and executed.
     */
    @GetMapping
    @Operation(summary = "List customers",
            description = "Optional RSQL filter over id, vehicleNumber, contactNumber, name and email. "
                    + "Supports == != =gt= =ge= =lt= =le= =in= =out=, ';' for AND, ',' for OR and "
                    + "'*' as a wildcard on text fields. Example: name==A*,email==*@example.com")
    public ResponseEntity<List<CustomerDTO>> getAllCustomers(
            @Parameter(description = "RSQL filter", example = "name==Asha*;email==*@example.com")
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(customerService.getAllCustomers(search));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a customer")
    public ResponseEntity<CustomerDTO> updateCustomer(@PathVariable UUID id,
                                                      @Valid @RequestBody CustomerDTO customerDTO) {
        customerDTO.setId(id);
        return ResponseEntity.ok(customerService.updateCustomer(customerDTO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a customer")
    @ApiResponse(responseCode = "204", description = "Deleted")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
