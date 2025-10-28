package com.droid.bss.api.customer;

import com.droid.bss.application.command.customer.ChangeCustomerStatusUseCase;
import com.droid.bss.application.command.customer.CreateCustomerUseCase;
import com.droid.bss.application.command.customer.DeleteCustomerUseCase;
import com.droid.bss.application.command.customer.UpdateCustomerUseCase;
import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.customer.ChangeCustomerStatusCommand;
import com.droid.bss.application.dto.customer.CreateCustomerCommand;
import com.droid.bss.application.dto.customer.CustomerResponse;
import com.droid.bss.application.dto.customer.UpdateCustomerCommand;
import com.droid.bss.application.query.customer.CustomerQueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    
    private final CreateCustomerUseCase createCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final ChangeCustomerStatusUseCase changeCustomerStatusUseCase;
    private final DeleteCustomerUseCase deleteCustomerUseCase;
    private final CustomerQueryService customerQueryService;
    
    public CustomerController(
            CreateCustomerUseCase createCustomerUseCase,
            UpdateCustomerUseCase updateCustomerUseCase,
            ChangeCustomerStatusUseCase changeCustomerStatusUseCase,
            DeleteCustomerUseCase deleteCustomerUseCase,
            CustomerQueryService customerQueryService) {
        this.createCustomerUseCase = createCustomerUseCase;
        this.updateCustomerUseCase = updateCustomerUseCase;
        this.changeCustomerStatusUseCase = changeCustomerStatusUseCase;
        this.deleteCustomerUseCase = deleteCustomerUseCase;
        this.customerQueryService = customerQueryService;
    }
    
    // Create
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerCommand command,
            @AuthenticationPrincipal Jwt principal
    ) {
        var customerId = createCustomerUseCase.handle(command);
        var customer = customerQueryService.findById(customerId.toString())
                .orElseThrow(() -> new RuntimeException("Customer not found after creation"));
        
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(customerId.toString())
                .toUri();
        
        return ResponseEntity.created(location).body(customer);
    }
    
    // Read single
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable String id) {
        return customerQueryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Read all with pagination and sorting
    @GetMapping
    public ResponseEntity<PageResponse<CustomerResponse>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        var pageResponse = customerQueryService.findAll(page, size, sort);
        return ResponseEntity.ok(pageResponse);
    }
    
    // Read by status
    @GetMapping("/by-status/{status}")
    public ResponseEntity<PageResponse<CustomerResponse>> getCustomersByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        var pageResponse = customerQueryService.findByStatus(status, page, size, sort);
        return ResponseEntity.ok(pageResponse);
    }
    
    // Search
    @GetMapping("/search")
    public ResponseEntity<PageResponse<CustomerResponse>> searchCustomers(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        var pageResponse = customerQueryService.search(searchTerm, page, size, sort);
        return ResponseEntity.ok(pageResponse);
    }
    
    // Update
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody UpdateCustomerCommand command
    ) {
        var updatedCommand = new UpdateCustomerCommand(
                id,
                command.firstName(),
                command.lastName(),
                command.pesel(),
                command.nip(),
                command.email(),
                command.phone()
        );
        
        var updatedCustomer = updateCustomerUseCase.handle(updatedCommand);
        var response = CustomerResponse.from(updatedCustomer);
        
        return ResponseEntity.ok(response);
    }
    
    // Change status
    @PutMapping("/{id}/status")
    public ResponseEntity<CustomerResponse> changeCustomerStatus(
            @PathVariable String id,
            @Valid @RequestBody ChangeCustomerStatusCommand command
    ) {
        var statusCommand = new ChangeCustomerStatusCommand(id, command.status());
        var updatedCustomer = changeCustomerStatusUseCase.handle(statusCommand);
        var response = CustomerResponse.from(updatedCustomer);
        
        return ResponseEntity.ok(response);
    }
    
    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable String id) {
        boolean deleted = deleteCustomerUseCase.handle(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
