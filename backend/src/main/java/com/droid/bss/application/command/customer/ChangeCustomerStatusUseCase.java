package com.droid.bss.application.command.customer;

import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.application.dto.customer.ChangeCustomerStatusCommand;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ChangeCustomerStatusUseCase {
    
    private final CustomerRepository customerRepository;
    
    public ChangeCustomerStatusUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    public Customer handle(ChangeCustomerStatusCommand command) {
        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));
        
        Optional<Customer> existingCustomer = customerRepository.findById(customerId);
        if (existingCustomer.isEmpty()) {
            throw new IllegalArgumentException("Customer not found with ID: " + customerId);
        }
        
        Customer customer = existingCustomer.get();
        CustomerStatus newStatus = command.toCustomerStatus();
        
        Customer updatedCustomer = switch (newStatus) {
            case ACTIVE -> customer.reactivate();
            case INACTIVE -> customer.deactivate();
            case SUSPENDED -> customer.suspend();
            case TERMINATED -> customer.terminate();
        };
        
        return customerRepository.save(updatedCustomer);
    }
}
