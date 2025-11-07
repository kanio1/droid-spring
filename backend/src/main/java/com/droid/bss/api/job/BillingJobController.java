package com.droid.bss.api.job;

import com.droid.bss.application.service.BillingJobService;
import com.droid.bss.domain.job.BackgroundJob;
import com.droid.bss.domain.job.BillingJobStatistics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Billing Job Management API
 * Provides endpoints for managing billing-related background jobs
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/billing-jobs")
@RequiredArgsConstructor
@Tag(name = "Billing Jobs", description = "Billing cycle and invoice generation job management")
public class BillingJobController {

    private final BillingJobService billingJobService;

    @GetMapping("/statistics")
    @Operation(summary = "Get billing job statistics", description = "Returns statistics for all billing-related background jobs")
    public ResponseEntity<BillingJobStatistics> getBillingJobStatistics() {
        log.debug("Fetching billing job statistics");
        BillingJobStatistics stats = billingJobService.getBillingJobStatistics();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/setup")
    @Operation(
        summary = "Set up standard billing jobs",
        description = "Creates and schedules all standard billing jobs including daily usage aggregation, monthly invoice generation, payment processing, and subscription renewals"
    )
    public ResponseEntity<List<BackgroundJob>> setupBillingJobs() {
        log.info("Setting up standard billing jobs...");

        try {
            List<BackgroundJob> createdJobs = billingJobService.setupBillingJobs();
            return ResponseEntity.status(HttpStatus.CREATED).body(createdJobs);
        } catch (Exception e) {
            log.error("Failed to set up billing jobs", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/custom")
    @Operation(summary = "Schedule custom billing job", description = "Creates a custom billing-related background job with the specified parameters")
    public ResponseEntity<BackgroundJob> scheduleCustomBillingJob(
            @RequestBody CustomBillingJobRequest request) {
        log.info("Scheduling custom billing job: {}", request.getName());

        if (request.getName() == null || request.getCronExpression() == null || request.getSqlCommand() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            BackgroundJob job = billingJobService.scheduleCustomBillingJob(
                    request.getName(),
                    request.getDescription(),
                    request.getCronExpression(),
                    request.getSqlCommand(),
                    request.getPriority()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(job);
        } catch (Exception e) {
            log.error("Failed to schedule custom billing job: {}", request.getName(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/all")
    @Operation(
        summary = "Remove all billing jobs",
        description = "Cancels and removes all billing-related background jobs from the system"
    )
    public ResponseEntity<Map<String, String>> removeAllBillingJobs() {
        log.warn("Removing all billing jobs via API");

        try {
            billingJobService.removeAllBillingJobs();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "All billing jobs removed successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to remove all billing jobs", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Failed to remove billing jobs: " + e.getMessage()
            ));
        }
    }

    /**
     * Request DTO for custom billing job scheduling
     */
    public static class CustomBillingJobRequest {
        private String name;
        private String description;
        private String cronExpression;
        private String sqlCommand;
        private String priority; // HIGH, MEDIUM, LOW

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }

        public String getSqlCommand() {
            return sqlCommand;
        }

        public void setSqlCommand(String sqlCommand) {
            this.sqlCommand = sqlCommand;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }
    }
}
