package com.droid.bss.domain.customer.event;

import com.droid.bss.domain.customer.CustomerEntity;
import com.droid.bss.domain.customer.CustomerStatus;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for CustomerRepository tests
 * Provides mock CustomerEventPublisher to avoid Kafka dependencies
 */
@TestConfiguration
public class CustomerRepositoryTestConfig {

    @Bean
    @Primary
    public CustomerEventPublisher customerEventPublisher() {
        return new MockCustomerEventPublisher();
    }

    /**
     * Mock implementation that just logs events instead of publishing to Kafka
     */
    private static class MockCustomerEventPublisher extends CustomerEventPublisher {

        public MockCustomerEventPublisher() {
            // Call no-args constructor
        }

        @Override
        public void publishCustomerCreated(CustomerEntity customer) {
            // Log instead of publishing
            System.out.println("MOCK: CustomerCreatedEvent for customer: " + customer.getId());
        }

        @Override
        public void publishCustomerUpdated(CustomerEntity customer) {
            System.out.println("MOCK: CustomerUpdatedEvent for customer: " + customer.getId());
        }

        @Override
        public void publishCustomerStatusChanged(CustomerEntity customer, CustomerStatus previousStatus) {
            System.out.println("MOCK: CustomerStatusChangedEvent for customer: " + customer.getId());
        }

        @Override
        public void publishCustomerTerminated(CustomerEntity customer, String terminationReason) {
            System.out.println("MOCK: CustomerTerminatedEvent for customer: " + customer.getId());
        }
    }
}
