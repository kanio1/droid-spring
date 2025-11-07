package com.droid.bss.config;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.listener.LifecycleListener;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Allure Configuration for Test Reporting
 *
 * Configures Allure lifecycle and provides utilities for:
 * - Adding test metadata
 * - Attaching files to test results
 * - Setting test categories
 * - Recording environment information
 */
@Configuration
public class AllureConfig {

    /**
     * Configure Allure lifecycle listener
     */
    @Bean
    public AllureLifecycle allureLifecycle() {
        AllureLifecycle lifecycle = Allure.getLifecycle();

        // Add custom lifecycle listener
        lifecycle.addListener(new LifecycleListener() {
            @Override
            public void beforeTestStop(TestResult result) {
                // Add custom metadata before test stops
                result.setHistoryId(UUID.randomUUID().toString());
            }
        });

        return lifecycle;
    }

    /**
     * Record environment information
     */
    public static void recordEnvironmentInfo() {
        Allure.parameter("Java Version", System.getProperty("java.version"));
        Allure.parameter("OS", System.getProperty("os.name"));
        Allure.parameter("User", System.getProperty("user.name"));
    }

    /**
     * Add test category
     */
    public static void addTestCategory(String category) {
        Allure.label("category", category);
    }

    /**
     * Add severity
     */
    public static void setSeverity(String severity) {
        Allure.label("severity", severity);
    }

    /**
     * Mark test as flaky
     */
    public static void markAsFlaky() {
        Allure.label("flaky", "true");
    }

    /**
     * Add feature
     */
    public static void addFeature(String feature) {
        Allure.label("feature", feature);
    }

    /**
     * Add story
     */
    public static void addStory(String story) {
        Allure.label("story", story);
    }

    /**
     * Add tag
     */
    public static void addTag(String tag) {
        Allure.label("tag", tag);
    }

    /**
     * Set test status based on exception
     */
    public static Status getStatusFromThrowable(Throwable throwable) {
        if (throwable instanceof AssertionError) {
            return Status.FAILED;
        } else if (throwable instanceof RuntimeException) {
            return Status.BROKEN;
        } else {
            return Status.FAILED;
        }
    }
}
