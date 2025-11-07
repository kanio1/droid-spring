package com.droid.bss.infrastructure.servicediscovery;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.discovery.ReregistrationHealth;
import org.springframework.cloud.consul.serviceregistry.AutoServiceRegistrationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Consul Service Discovery Properties
 * Configuration for Consul client, service registration, and health checks
 */
@Data
@ConfigurationProperties(prefix = "bss.servicediscovery")
@Validated
public class ConsulServiceProperties {

    /**
     * Enable service discovery
     */
    private boolean enabled = false;

    /**
     * Consul server host
     */
    @NotBlank(message = "Consul host cannot be blank")
    private String host = "localhost";

    /**
     * Consul server port
     */
    @NotNull(message = "Consul port cannot be null")
    private Integer port = 8500;

    /**
     * Service name
     */
    @NotBlank(message = "Service name cannot be blank")
    private String serviceName = "bss-backend";

    /**
     * Service ID
     */
    private String serviceId;

    /**
     * Service port
     */
    private Integer servicePort = 8080;

    /**
     * Service tags
     */
    private String[] tags = {"bss", "backend", "api"};

    /**
     * Enable health checks
     */
    private boolean enableHealthCheck = true;

    /**
     * Health check interval
     */
    private Duration healthCheckInterval = Duration.ofSeconds(10);

    /**
     * Health check timeout
     */
    private Duration healthCheckTimeout = Duration.ofSeconds(5);

    /**
     * Health check URL path
     */
    private String healthCheckPath = "/actuator/health";

    /**
     * Enable heartbeat for health checks
     */
    private boolean enableHeartbeat = true;

    /**
     * Heartbeat interval
     */
    private Duration heartbeatInterval = Duration.ofSeconds(5);

    /**
     * Deregister critical service after
     */
    private Duration deregisterCriticalServiceAfter = Duration.ofMinutes(1);

    /**
     * Registration configuration
     */
    private AutoServiceRegistrationProperties registration = new AutoServiceRegistrationProperties();

    /**
     * Deregistration configuration
     */
    private AutoServiceRegistrationProperties deregistration = new AutoServiceRegistrationProperties();

    /**
     * Discovery properties
     */
    private ConsulDiscoveryProperties discovery = new ConsulDiscoveryProperties();

    /**
     * Heartbeat properties
     */
    private HeartbeatProperties heartbeat = new HeartbeatProperties();

    /**
     * Reregistration health check
     */
    private ReregistrationHealth reregistrationHealth = new ReregistrationHealth();

    /**
     * Namespace (for Consul Enterprise)
     */
    private String namespace;

    /**
     * Datacenter
     */
    private String datacenter;
}
