package com.droid.bss.application.command.order;

import com.droid.bss.application.dto.order.CreateOrderCommand;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.order.OrderEntity;
import com.droid.bss.domain.order.OrderPriority;
import com.droid.bss.domain.order.OrderStatus;
import com.droid.bss.domain.order.OrderType;
import com.droid.bss.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Component
@Transactional
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public CreateOrderUseCase(OrderRepository orderRepository, CustomerRepository customerRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Create a new order
     */
    public UUID handle(CreateOrderCommand command) {
        // Business validation
        if (orderRepository.existsByOrderNumber(command.orderNumber())) {
            throw new IllegalArgumentException("Order with number %s already exists".formatted(command.orderNumber()));
        }

        // For now, we'll create the order without validating customer
        // In a full implementation, we'd need to bridge between Customer (aggregate) and CustomerEntity (JPA)
        // or use a JPA repository for CustomerEntity

        // Create a minimal CustomerEntity from the command data
        // This is a simplified approach for demonstration
        CustomerEntity customer = new CustomerEntity();
        customer.setId(UUID.fromString(command.customerId()));
        customer.setFirstName("Unknown");
        customer.setLastName("Customer");
        customer.setEmail("unknown@example.com");
        customer.setStatus(com.droid.bss.domain.customer.CustomerStatus.ACTIVE);

        // Create order
        OrderEntity order = new OrderEntity(
            command.orderNumber(),
            customer,
            command.orderType(),
            command.status(),
            command.priority(),
            command.totalAmount(),
            command.currency() != null ? command.currency() : "PLN",
            command.requestedDate() != null ? command.requestedDate() : LocalDate.now(),
            command.orderChannel(),
            command.salesRepId()
        );

        // Set promised date (e.g., requested date + 5 business days)
        if (command.requestedDate() != null) {
            order.setPromisedDate(command.requestedDate().plusDays(5));
        }

        // Save order
        OrderEntity savedOrder = orderRepository.save(order);

        return savedOrder.getId();
    }
}
