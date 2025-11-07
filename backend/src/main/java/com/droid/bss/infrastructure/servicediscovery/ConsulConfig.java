package com.droid.bss.infrastructure.servicediscovery;

import com.ecwid.consul.v1.ConsulClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.serviceregistry.ConsulAutoServiceRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PreDestroy;

/**
 * Consul Service Discovery Configuration
 * Configures Consul client, service registration, and discovery
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ConsulServiceProperties.class)
@ConditionalOnConsulEnabled
@ConditionalOnProperty(name = "bss.servicediscovery.enabled", havingValue = "true")
public class ConsulConfig {

    private ConsulClient consulClient;
    private ConsulAutoServiceRegistration autoRegistration;

    @Bean
    @Primary
    public ConsulClient consulClient(ConsulServiceProperties properties) {
        log.info("Configuring Consul client at {}:{}", properties.getHost(), properties.getPort());

        consulClient = new ConsulClient(properties.getHost(), properties.getPort());
        return consulClient;
    }

    @Bean
    public ConsulAutoServiceRegistration consulAutoRegistration(
            ConsulClient client,
            ConsulServiceProperties properties) {

        autoRegistration = new ConsulAutoServiceRegistration(
            client,
            new ConsulServiceRegistry(client),
            client,
            properties.getRegistration(),
            properties.getDeregistration()
        );

        log.info("Configuring Consul auto-registration for service: {}", properties.getServiceName());
        return autoRegistration;
    }

    @PreDestroy
    public void destroy() {
        if (autoRegistration != null) {
            log.info("Deregistering service from Consul");
            autoRegistration.stop();
        }
    }
}
