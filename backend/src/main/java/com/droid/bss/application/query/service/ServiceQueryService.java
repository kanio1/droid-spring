package com.droid.bss.application.query.service;

import com.droid.bss.domain.service.ServiceEntity;
import com.droid.bss.domain.service.ServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query service for service entities
 */
@Service
@Transactional(readOnly = true)
public class ServiceQueryService {

    private final ServiceRepository serviceRepository;

    public ServiceQueryService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<ServiceEntity> findAllServices() {
        return serviceRepository.findAll();
    }

    public List<ServiceEntity> findActiveServices() {
        return serviceRepository.findAllActive();
    }
}
