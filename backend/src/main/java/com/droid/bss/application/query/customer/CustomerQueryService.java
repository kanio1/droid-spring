package com.droid.bss.application.query.customer;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.customer.CustomerResponse;
import com.droid.bss.domain.customer.Customer;
import com.droid.bss.domain.customer.CustomerId;
import com.droid.bss.domain.customer.CustomerRepository;
import com.droid.bss.domain.customer.CustomerStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CustomerQueryService {
    
    private final CustomerRepository customerRepository;
    
    public CustomerQueryService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
    
    public Optional<CustomerResponse> findById(String customerId) {
        CustomerId id = new CustomerId(UUID.fromString(customerId));
        return customerRepository.findById(id)
                .map(CustomerResponse::from);
    }
    
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
    
    public PageResponse<CustomerResponse> search(String searchTerm, int page, int size, String sort) {
        List<Customer> customers = customerRepository.search(searchTerm, page, size);
        
        // For now, total count approximation
        long total = Math.min(customers.size() + page * size, 1000L);
        
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::from)
                .toList();
        
        return PageResponse.of(responses, page, size, total);
    }
    
    private String[] parseSort(String sort) {
        if (sort == null || sort.isEmpty()) {
            return new String[]{"createdAt"};
        }
        
        return sort.split(",");
    }
}
