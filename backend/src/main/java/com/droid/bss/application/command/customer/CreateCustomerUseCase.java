package com.droid.bss.application.command.customer;

import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.customer.CustomerStatus;
import com.droid.bss.domain.customer.event.CustomerEventPublisher;
import com.droid.bss.application.dto.customer.CreateCustomerCommand;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerEntityRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class CreateCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerEntityRepository customerEntityRepository;
    private final CustomerEventPublisher eventPublisher;

    public CreateCustomerUseCase(
            CustomerRepository customerRepository,
            CustomerEntityRepository customerEntityRepository,
            CustomerEventPublisher eventPublisher) {
        this.customerRepository = customerRepository;
        this.customerEntityRepository = customerEntityRepository;
        this.eventPublisher = eventPublisher;
    }
    
    public CustomerId handle(CreateCustomerCommand command) {
        // Business validation
        if (command.pesel() != null && customerRepository.existsByPesel(command.pesel())) {
            throw new IllegalArgumentException("Customer with PESEL %s already exists".formatted(command.pesel()));
        }

        if (command.nip() != null && customerRepository.existsByNip(command.nip())) {
            throw new IllegalArgumentException("Customer with NIP %s already exists".formatted(command.nip()));
        }

        // Create customer
        Customer customer = Customer.create(command.toCustomerInfo(), command.toContactInfo());
        Customer savedCustomer = customerRepository.save(customer);

        // Publish event
        CustomerEntity customerEntity = customerEntityRepository.findById(savedCustomer.getId().value())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found after creation"));
        eventPublisher.publishCustomerCreated(customerEntity);

        return savedCustomer.getId();
    }
}
