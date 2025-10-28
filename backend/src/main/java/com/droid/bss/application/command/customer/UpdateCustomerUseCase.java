package com.droid.bss.application.command.customer;

import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.application.dto.customer.UpdateCustomerCommand;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UpdateCustomerUseCase {
    
    private final CustomerRepository customerRepository;
    
    public UpdateCustomerUseCase(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    public Customer handle(UpdateCustomerCommand command) {
        CustomerId customerId = new CustomerId(UUID.fromString(command.customerId()));
        
        Optional<Customer> existingCustomer = customerRepository.findById(customerId);
        if (existingCustomer.isEmpty()) {
            throw new IllegalArgumentException("Customer not found with ID: " + customerId);
        }
        
        Customer customer = existingCustomer.get();
        if (!customer.canBeModified()) {
            throw new IllegalArgumentException("Cannot modify terminated customer");
        }
        
        // Update customer
        Customer updatedCustomer = customer.updatePersonalInfo(command.toCustomerInfo())
                .updateContactInfo(command.toContactInfo());
        
        return customerRepository.save(updatedCustomer);
    }
}
