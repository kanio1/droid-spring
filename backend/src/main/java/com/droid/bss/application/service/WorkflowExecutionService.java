package com.droid.bss.application.service;

import com.droid.bss.domain.workflow.Workflow;
import com.droid.bss.domain.workflow.WorkflowExecution;
import com.droid.bss.domain.workflow.WorkflowStepExecution;
import com.droid.bss.infrastructure.workflow.WorkflowExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Workflow Execution Service
 * Manages workflow execution lifecycle
 */
@Service
public class WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionService.class);

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowActionExecutor actionExecutor;
    private final Executor asyncExecutor;
    private final ObjectMapper objectMapper;

    public WorkflowExecutionService(
            WorkflowExecutionRepository executionRepository,
            WorkflowActionExecutor actionExecutor,
            Executor asyncExecutor,
            ObjectMapper objectMapper) {
        this.executionRepository = executionRepository;
        this.actionExecutor = actionExecutor;
        this.asyncExecutor = asyncExecutor;
        this.objectMapper = objectMapper;
    }

    /**
     * Create new workflow execution
     */
    @Transactional
    public UUID createExecution(Workflow workflow, String entityType, UUID entityId, Map<String, Object> context) {
        log.info("Creating workflow execution for workflow: {} entity: {}/{}", workflow.getName(), entityType, entityId);

        // Create execution record
        WorkflowExecution execution = new WorkflowExecution();
        execution.setWorkflowId(workflow.getId());
        execution.setEntityType(entityType);
        execution.setEntityId(entityId);
        execution.setStatus(WorkflowExecution.Status.RUNNING);
        execution.setCurrentStep(0);
        execution.setTotalSteps(workflow.getTotalSteps());
        execution.setContext(context);
        execution.setStartedAt(LocalDateTime.now());

        execution = executionRepository.save(execution);

        return execution.getId();
    }

    /**
     * Execute workflow steps
     */
    @Transactional
    public void executeWorkflow(UUID executionId) {
        log.info("Starting workflow execution: {}", executionId);

        try {
            WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Workflow execution not found: " + executionId));

            if (execution.isCompleted() || execution.isFailed()) {
                log.warn("Workflow execution already completed/failed: {}", executionId);
                return;
            }

            List<WorkflowStepExecution> steps = getStepsInOrder(executionId);

            for (WorkflowStepExecution step : steps) {
                if (step.getStatus() == WorkflowStepExecution.Status.PENDING) {
                    log.info("Executing step: {} of workflow: {}", step.getStepName(), executionId);

                    try {
                        executeStep(execution, step);
                    } catch (Exception e) {
                        log.error("Step failed: {} of workflow: {}", step.getStepName(), executionId, e);
                        handleStepFailure(execution, step, e);
                        break;
                    }
                }
            }

            // Check if all steps completed
            checkAndUpdateExecutionStatus(executionId);

        } catch (Exception e) {
            log.error("Workflow execution failed: {}", executionId, e);
            markExecutionFailed(executionId, e.getMessage());
        }
    }

    /**
     * Execute single step
     */
    private void executeStep(WorkflowExecution execution, WorkflowStepExecution step) {
        step.setStatus(WorkflowStepExecution.Status.RUNNING);
        step.setStartedAt(LocalDateTime.now());
        executionRepository.saveStepExecution(step);

        // Check if this is a delay step
        if ("delay".equals(step.getStepType())) {
            int delaySeconds = (Integer) step.getInputData().getOrDefault("delay_seconds", 0);
            long delayMs = (long) delaySeconds * 1000;

            log.info("Delaying step {} for {} seconds", step.getStepName(), delaySeconds);

            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(delayMs);
                    markStepCompleted(step.getId(), Map.of("delayed", true));
                } catch (InterruptedException e) {
                    log.error("Delay interrupted for step: {}", step.getStepName(), e);
                    Thread.currentThread().interrupt();
                }
            }, asyncExecutor);

            return;
        }

        // Handle action or condition steps
        String stepType = step.getStepType();

        if ("action".equals(stepType)) {
            String action = (String) step.getInputData().get("action");
            Map<String, Object> config = (Map<String, Object>) step.getInputData().get("config");

            CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> result = actionExecutor.executeAction(action, config, execution.getContext());
                    markStepCompleted(step.getId(), result);
                } catch (Exception e) {
                    log.error("Action failed: {} for workflow: {}", action, execution.getId(), e);
                    markStepFailed(step.getId(), e.getMessage());
                }
            }, asyncExecutor);

        } else if ("condition".equals(stepType)) {
            String condition = (String) step.getInputData().get("condition");
            boolean conditionResult = evaluateCondition(condition, execution.getContext());

            if (conditionResult) {
                markStepCompleted(step.getId(), Map.of("condition_result", true));
            } else {
                markStepSkipped(step.getId(), Map.of("condition_result", false, "reason", "condition_not_met"));
            }
        }
    }

    /**
     * Mark step as completed
     */
    @Transactional
    public void markStepCompleted(UUID stepId, Map<String, Object> outputData) {
        WorkflowStepExecution step = executionRepository.findStepExecutionById(stepId)
            .orElseThrow(() -> new RuntimeException("Step execution not found: " + stepId));

        step.setStatus(WorkflowStepExecution.Status.COMPLETED);
        step.setOutputData(outputData);
        step.setCompletedAt(LocalDateTime.now());

        executionRepository.saveStepExecution(step);

        log.info("Step completed: {} with result: {}", step.getStepName(), outputData);

        // Update execution current step
        updateExecutionCurrentStep(step.getWorkflowExecutionId());
    }

    /**
     * Mark step as failed
     */
    @Transactional
    public void markStepFailed(UUID stepId, String errorMessage) {
        WorkflowStepExecution step = executionRepository.findStepExecutionById(stepId)
            .orElseThrow(() -> new RuntimeException("Step execution not found: " + stepId));

        step.setStatus(WorkflowStepExecution.Status.FAILED);
        step.setErrorMessage(errorMessage);
        step.setCompletedAt(LocalDateTime.now());

        executionRepository.saveStepExecution(step);

        log.error("Step failed: {} with error: {}", step.getStepName(), errorMessage);

        // Mark execution as failed
        markExecutionFailed(step.getWorkflowExecutionId(), errorMessage);
    }

    /**
     * Mark step as skipped
     */
    @Transactional
    public void markStepSkipped(UUID stepId, Map<String, Object> outputData) {
        WorkflowStepExecution step = executionRepository.findStepExecutionById(stepId)
            .orElseThrow(() -> new RuntimeException("Step execution not found: " + stepId));

        step.setStatus(WorkflowStepExecution.Status.SKIPPED);
        step.setOutputData(outputData);
        step.setCompletedAt(LocalDateTime.now());

        executionRepository.saveStepExecution(step);

        log.info("Step skipped: {} with result: {}", step.getStepName(), outputData);

        // Update execution current step
        updateExecutionCurrentStep(step.getWorkflowExecutionId());
    }

    /**
     * Handle step failure
     */
    private void handleStepFailure(WorkflowExecution execution, WorkflowStepExecution step, Exception e) {
        markStepFailed(step.getId(), e.getMessage());
    }

    /**
     * Get workflow execution
     */
    public WorkflowExecution getExecution(UUID executionId) {
        return executionRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Workflow execution not found: " + executionId));
    }

    /**
     * Get executions for entity
     */
    public List<WorkflowExecution> getExecutionsForEntity(String entityType, UUID entityId) {
        return executionRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Cancel execution
     */
    @Transactional
    public void cancelExecution(UUID executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Workflow execution not found: " + executionId));

        execution.setStatus(WorkflowExecution.Status.CANCELLED);
        execution.setCompletedAt(LocalDateTime.now());

        executionRepository.save(execution);
    }

    /**
     * Retry failed execution
     */
    @Transactional
    public void retryExecution(UUID executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Workflow execution not found: " + executionId));

        if (execution.getStatus() != WorkflowExecution.Status.FAILED) {
            throw new IllegalStateException("Only failed executions can be retried");
        }

        // Reset execution
        execution.setStatus(WorkflowExecution.Status.RUNNING);
        execution.setCurrentStep(0);
        execution.setErrorMessage(null);
        execution.setCompletedAt(null);

        executionRepository.save(execution);

        // Reset all steps to pending
        executionRepository.resetStepsToPending(executionId);

        // Re-execute
        executeWorkflow(executionId);
    }

    // Helper methods

    private List<WorkflowStepExecution> getStepsInOrder(UUID executionId) {
        return executionRepository.findStepExecutionsByWorkflowExecutionIdOrderByStepNumber(executionId);
    }

    private void updateExecutionCurrentStep(UUID executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Workflow execution not found: " + executionId));

        List<WorkflowStepExecution> steps = getStepsInOrder(executionId);
        int completedSteps = (int) steps.stream()
            .filter(s -> s.getStatus() == WorkflowStepExecution.Status.COMPLETED)
            .count();

        execution.setCurrentStep(completedSteps);
        executionRepository.save(execution);
    }

    private void checkAndUpdateExecutionStatus(UUID executionId) {
        List<WorkflowStepExecution> steps = getStepsInOrder(executionId);
        boolean allCompleted = steps.stream()
            .allMatch(s -> s.getStatus() == WorkflowStepExecution.Status.COMPLETED ||
                          s.getStatus() == WorkflowStepExecution.Status.SKIPPED);

        if (allCompleted) {
            markExecutionCompleted(executionId);
        }
    }

    private void markExecutionCompleted(UUID executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Workflow execution not found: " + executionId));

        execution.setStatus(WorkflowExecution.Status.COMPLETED);
        execution.setCompletedAt(LocalDateTime.now());
        execution.setCurrentStep(execution.getTotalSteps());

        executionRepository.save(execution);

        log.info("Workflow execution completed: {}", executionId);
    }

    private void markExecutionFailed(UUID executionId, String errorMessage) {
        WorkflowExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Workflow execution not found: " + executionId));

        execution.setStatus(WorkflowExecution.Status.FAILED);
        execution.setCompletedAt(LocalDateTime.now());
        execution.setErrorMessage(errorMessage);

        executionRepository.save(execution);

        log.error("Workflow execution failed: {} with error: {}", executionId, errorMessage);
    }

    private boolean evaluateCondition(String condition, Map<String, Object> context) {
        // Simple condition evaluation (in real implementation, use a proper expression language)
        // For now, just check if condition contains entity data
        try {
            // Replace placeholders like {{customer.id}} with actual values
            String evaluatedCondition = replacePlaceholders(condition, context);

            // Check if evaluated condition contains expected patterns
            return evaluatedCondition.contains("==") || evaluatedCondition.contains("!=") ||
                   evaluatedCondition.contains(">") || evaluatedCondition.contains("<");
        } catch (Exception e) {
            log.error("Failed to evaluate condition: {}", condition, e);
            return false;
        }
    }

    private String replacePlaceholders(String text, Map<String, Object> context) {
        String result = text;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue().toString());
        }
        return result;
    }
}
