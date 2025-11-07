package com.droid.bss.infrastructure.servicediscovery;

import com.ecwid.consul.v1.QueryOptions;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.health.model.HealthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service Discovery Client
 * High-level client for discovering and querying services in Consul
 */
@Slf4j
@Component
@EnableFeignClients
public class ServiceDiscoveryClient {

    private final ConsulServiceProperties properties;

    public ServiceDiscoveryClient(ConsulServiceProperties properties) {
        this.properties = properties;
    }

    /**
     * Discover all healthy instances of a service
     */
    public List<String> discoverHealthyInstances(String serviceName) {
        try {
            log.trace("Discovering healthy instances of service: {}", serviceName);

            Response<List<HealthService>> response = properties.getDiscovery()
                .getHealthClient()
                .getHealthyServiceInstances(serviceName, QueryOptions.Builder.defaultOptions());

            return response.getValue().stream()
                .map(this::extractServiceAddress)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to discover instances for service: {}", serviceName, e);
            throw new ServiceDiscoveryException("Failed to discover service instances: " + serviceName, e);
        }
    }

    /**
     * Discover all instances (healthy and unhealthy) of a service
     */
    public List<String> discoverAllInstances(String serviceName) {
        try {
            log.trace("Discovering all instances of service: {}", serviceName);

            Response<List<CatalogService>> response = properties.getDiscovery()
                .getCatalogClient()
                .getServices(QueryOptions.Builder.defaultOptions());

            return response.getValue().stream()
                .filter(service -> serviceName.equals(service.getServiceName()))
                .map(this::extractServiceAddress)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to discover all instances for service: {}", serviceName, e);
            throw new ServiceDiscoveryException("Failed to discover service instances: " + serviceName, e);
        }
    }

    /**
     * Get service instance count
     */
    public int getServiceInstanceCount(String serviceName) {
        List<String> instances = discoverHealthyInstances(serviceName);
        return instances.size();
    }

    /**
     * Check if service is available (has at least one healthy instance)
     */
    public boolean isServiceAvailable(String serviceName) {
        int count = getServiceInstanceCount(serviceName);
        boolean available = count > 0;
        log.trace("Service {} is {} (healthy instances: {})",
            serviceName, available ? "available" : "unavailable", count);
        return available;
    }

    private String extractServiceAddress(HealthService service) {
        HealthService.Instance instance = service.getService();
        String address = instance.getAddress() != null ? instance.getAddress() : instance.getId();
        int port = instance.getPort();

        return address + ":" + port;
    }

    private String extractServiceAddress(CatalogService service) {
        String address = service.getAddress() != null ? service.getAddress() : service.getServiceId();
        Integer port = service.getServicePort();

        return port != null ? address + ":" + port : address;
    }

    /**
     * Exception for service discovery errors
     */
    public static class ServiceDiscoveryException extends RuntimeException {
        public ServiceDiscoveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
