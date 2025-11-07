package com.droid.bss.application.query.customer;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.customer.CustomerResponse;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.customer.CustomerStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Transactional(readOnly = true)
@CacheConfig(cacheNames = "customers")
public class CustomerQueryService {
    
    private final CustomerRepository customerRepository;
    
    public CustomerQueryService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    @CircuitBreaker(name = "customerQueryService", fallbackMethod = "findByIdFallback")
    @Retry(name = "customerQueryService")
    @TimeLimiter(name = "customerQueryService")
    @Cacheable(key = "#customerId", unless = "#result == null")
    public Optional<CustomerResponse> findById(String customerId) {
        CustomerId id = new CustomerId(UUID.fromString(customerId));
        return customerRepository.findById(id)
                .map(CustomerResponse::from);
    }

    public Optional<CustomerResponse> findByIdFallback(String customerId, Exception ex) {
        // Circuit breaker is open - return empty result
        return Optional.empty();
    }
    
    @CircuitBreaker(name = "customerQueryService", fallbackMethod = "findAllFallback")
    @Retry(name = "customerQueryService")
    @TimeLimiter(name = "customerQueryService")
    public PageResponse<CustomerResponse> findAll(int page, int size, String sort) {
        String[] sortFields = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortFields));

        // For now, we'll use simple implementation
        List<Customer> customers = customerRepository.findAll(pageRequest.getPageNumber(), pageRequest.getPageSize());
        long total = customerRepository.count();

        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .toList();

        return PageResponse.of(responses, page, size, total);
    }

    public PageResponse<CustomerResponse> findAllFallback(int page, int size, String sort, Exception ex) {
        // Circuit breaker is open - return empty page
        return PageResponse.of(List.of(), page, size, 0);
    }
    
    @CircuitBreaker(name = "customerQueryService", fallbackMethod = "findByStatusFallback")
    @Retry(name = "customerQueryService")
    @TimeLimiter(name = "customerQueryService")
    @Cacheable(key = "{#status, #page, #size, #sort}")
    public PageResponse<CustomerResponse> findByStatus(String status, int page, int size, String sort) {
        CustomerStatus customerStatus;
        try {
            customerStatus = CustomerStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        List<Customer> customers = customerRepository.findByStatus(customerStatus, page, size);
        long total = customerRepository.countByStatus(customerStatus);

        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .toList();

        return PageResponse.of(responses, page, size, total);
    }

    public PageResponse<CustomerResponse> findByStatusFallback(String status, int page, int size, String sort, Exception ex) {
        // Circuit breaker is open - return empty page
        return PageResponse.of(List.of(), page, size, 0);
    }
    
    @CircuitBreaker(name = "customerQueryService", fallbackMethod = "searchFallback")
    @Retry(name = "customerQueryService")
    @TimeLimiter(name = "customerQueryService")
    @Cacheable(key = "{#searchTerm, #page, #size, #sort}")
    public PageResponse<CustomerResponse> search(String searchTerm, int page, int size, String sort) {
        List<Customer> customers = customerRepository.search(searchTerm, page, size);
        long total = customerRepository.count();

        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .toList();

        return PageResponse.of(responses, page, size, total);
    }

    public PageResponse<CustomerResponse> searchFallback(String searchTerm, int page, int size, String sort, Exception ex) {
        // Circuit breaker is open - return empty page
        return PageResponse.of(List.of(), page, size, 0);
    }
    
    private String[] parseSort(String sort) {
        if (sort == null || sort.isEmpty()) {
            return new String[]{"createdAt"};
        }
        
        return sort.split(",");
    }
}
