package com.droid.bss.application.command.customer;

import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DeleteCustomerUseCase {
    
    private final CustomerRepository customerRepository;
    
    public DeleteCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    public boolean handle(@NotBlank String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        try {
            CustomerId id = new CustomerId(UUID.fromString(customerId));
            return customerRepository.deleteById(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid customer ID format", e);
        }
    }
}
