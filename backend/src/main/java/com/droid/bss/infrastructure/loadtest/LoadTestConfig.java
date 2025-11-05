package com.droid.bss.infrastructure.loadtest;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for load testing
 */
@Configuration
@Profile("loadtest")
public class LoadTestConfig {

    @Bean
    @ConfigurationProperties(prefix = "loadtest")
    public LoadTestProperties loadTestProperties() {
        return new LoadTestProperties();
    }

    public static class LoadTestProperties {
        private int virtualUsers = 100;
        private int durationMinutes = 5;
        private int rampUpSeconds = 60;
        private String baseUrl = "http://localhost:8080";
        private int threads = 20;
        private int iterations = 1000;

        // Getters and setters
        public int getVirtualUsers() { return virtualUsers; }
        public void setVirtualUsers(int virtualUsers) { this.virtualUsers = virtualUsers; }

        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

        public int getRampUpSeconds() { return rampUpSeconds; }
        public void setRampUpSeconds(int rampUpSeconds) { this.rampUpSeconds = rampUpSeconds; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public int getThreads() { return threads; }
        public void setThreads(int threads) { this.threads = threads; }

        public int getIterations() { return iterations; }
        public void setIterations(int iterations) { this.iterations = iterations; }
    }
}
