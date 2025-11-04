package com.droid.bss.application.query.subscription;

import com.droid.bss.application.dto.common.PageResponse;
import com.droid.bss.application.dto.subscription.SubscriptionResponse;
import com.droid.bss.domain.subscription.SubscriptionEntity;
import com.droid.bss.domain.subscription.repository.SubscriptionRepository;
import com.droid.bss.domain.subscription.SubscriptionStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
public class SubscriptionQueryService {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionQueryService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public Optional<SubscriptionResponse> findById(String subscriptionId) {
        return subscriptionRepository.findById(UUID.fromString(subscriptionId))
                .map(SubscriptionResponse::from);
    }

    public Optional<SubscriptionResponse> findBySubscriptionNumber(String subscriptionNumber) {
        return subscriptionRepository.findBySubscriptionNumber(subscriptionNumber)
                .map(SubscriptionResponse::from);
    }

    public PageResponse<SubscriptionResponse> findAll(int page, int size, String sort) {
        Sort sortObj = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, sortObj);

        org.springframework.data.domain.Page<SubscriptionEntity> subscriptions =
                subscriptionRepository.findAll(pageRequest);

        List<SubscriptionResponse> responses = subscriptions.getContent().stream()
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(responses, subscriptions.getNumber(), subscriptions.getSize(),
                subscriptions.getTotalElements());
    }

    public PageResponse<SubscriptionResponse> findByCustomerId(String customerId, int page, int size, String sort) {
        Sort sortObj = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, sortObj);

        org.springframework.data.domain.Page<SubscriptionEntity> subscriptions =
                subscriptionRepository.findByCustomerId(UUID.fromString(customerId), pageRequest);

        List<SubscriptionResponse> responses = subscriptions.getContent().stream()
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(responses, subscriptions.getNumber(), subscriptions.getSize(),
                subscriptions.getTotalElements());
    }

    public PageResponse<SubscriptionResponse> findByStatus(String status, int page, int size, String sort) {
        SubscriptionStatus subscriptionStatus;
        try {
            subscriptionStatus = SubscriptionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        Sort sortObj = parseSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, sortObj);

        // Get all subscriptions and filter by status (simplified implementation)
        // In production, this should be a custom query method
        org.springframework.data.domain.Page<SubscriptionEntity> allSubscriptions =
                subscriptionRepository.findAll(pageRequest);

        List<SubscriptionEntity> filteredSubscriptions = allSubscriptions.getContent().stream()
                .filter(s -> s.getStatus() == subscriptionStatus)
                .collect(Collectors.toList());

        List<SubscriptionResponse> responses = filteredSubscriptions.stream()
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(responses, allSubscriptions.getNumber(), allSubscriptions.getSize(),
                allSubscriptions.getTotalElements());
    }

    public List<SubscriptionResponse> findActiveSubscriptions(String customerId) {
        UUID customerUUID = UUID.fromString(customerId);
        org.springframework.data.domain.Page<SubscriptionEntity> subscriptions =
                subscriptionRepository.findByCustomerId(customerUUID, PageRequest.of(0, 100));

        return subscriptions.getContent().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());
    }

    public List<SubscriptionResponse> findExpiringSubscriptions(int days) {
        java.time.LocalDate expirationDate = java.time.LocalDate.now().plusDays(days);

        // This should be a custom query method in production
        List<SubscriptionEntity> allSubscriptions = subscriptionRepository.findAll();

        return allSubscriptions.stream()
                .filter(s -> s.getEndDate() != null && s.getEndDate().isBefore(expirationDate))
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());
    }

    public List<SubscriptionResponse> search(String searchTerm) {
        // This should be a custom query method in production
        List<SubscriptionEntity> allSubscriptions = subscriptionRepository.findAll();

        return allSubscriptions.stream()
                .filter(s -> s.getSubscriptionNumber().toLowerCase().contains(searchTerm.toLowerCase()))
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());
    }

    public long countByStatus(SubscriptionStatus status) {
        // This should be a custom count method in production
        List<SubscriptionEntity> allSubscriptions = subscriptionRepository.findAll();
        return allSubscriptions.stream()
                .filter(s -> s.getStatus() == status)
                .count();
    }

    public List<SubscriptionResponse> findSubscriptionsForRenewal() {
        java.time.LocalDate today = java.time.LocalDate.now();

        // Get subscriptions that need renewal
        List<SubscriptionEntity> allSubscriptions = subscriptionRepository.findAll();

        return allSubscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(s -> s.getAutoRenew())
                .filter(s -> s.getNextBillingDate() != null && !s.getNextBillingDate().isAfter(today))
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        if (parts.length == 2) {
            return Sort.by(Sort.Direction.fromString(parts[1].trim()), parts[0].trim());
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
