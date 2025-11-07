package com.droid.bss.camunda.config;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Camunda BPM Configuration
 * Enables process application and configures async execution
 */
@Configuration
@EnableProcessApplication
@EnableAsync
public class CamundaConfiguration {

    /**
     * Configure async task executor for Camunda jobs
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("camunda-");
        executor.initialize();
        return executor;
    }
}
