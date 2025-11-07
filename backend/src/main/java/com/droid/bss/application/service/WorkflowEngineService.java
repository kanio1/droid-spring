package com.droid.bss.application.service;

import com.droid.bss.domain.customer.event.CustomerEvent;
import com.droid.bss.domain.invoice.event.InvoiceEvent;
import com.droid.bss.domain.payment.event.PaymentEvent;
import com.droid.bss.domain.workflow.Workflow;
import com.droid.bss.domain.workflow.WorkflowExecution;
import com.droid.bss.infrastructure.workflow.WorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Legacy Workflow Engine Service
 *
 * @deprecated This service is being replaced by Camunda BPM.
 * Use {@link com.droid.bss.camunda.listener.CamundaEventListener} and
 * {@link com.droid.bss.camunda.service.CamundaWorkflowService} instead.
 *
 * This service orchestrates workflow execution based on domain events using
 * a custom workflow engine. It is being migrated to Camunda BPM for better
 * durability, monitoring, and visual workflow modeling.
 *
 * Migration plan:
 * 1. Use CamundaEventListener to trigger workflows from domain events
 * 2. Use CamundaWorkflowService for workflow management
 * 3. Use Camunda Cockpit for monitoring and management
 */
@Service
@Deprecated
public class WorkflowEngineService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngineService.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionService executionService;
    private final WorkflowActionExecutor actionExecutor;
    private final ObjectMapper objectMapper;

    public WorkflowEngineService(
            WorkflowRepository workflowRepository,
            WorkflowExecutionService executionService,
            WorkflowActionExecutor actionExecutor,
            ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.executionService = executionService;
        this.actionExecutor = actionExecutor;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle customer events and trigger workflows
     */
    @KafkaListener(topics = "customer.events", groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-workflow}")
    @Transactional
    public void handleCustomerEvent(CustomerEvent event) {
        log.info("Processing customer event for workflow: {}", event.getType());

        try {
            switch (event.getType()) {
                case CREATED:
                    triggerWorkflow("customer_onboarding", "customer", event.getCustomerId().toString(), event);
                    break;
                case STATUS_CHANGED:
                    // Could trigger status change workflows
                    break;
                default:
                    log.debug("No workflow for customer event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Failed to process customer event in workflow engine", e);
        }
    }

    /**
     * Handle payment events and trigger workflows
     */
    @KafkaListener(topics = "payment.events", groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-workflow}")
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Processing payment event for workflow: {}", event.getType());

        try {
            switch (event.getType()) {
                case FAILED:
                    triggerWorkflow("payment_failed_recovery", "payment", event.getPaymentId().toString(), event);
                    break;
                case COMPLETED:
                    // Could trigger payment success workflows
                    break;
                default:
                    log.debug("No workflow for payment event type: {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Failed to process payment event in workflow engine", e);
        }
    }

    /**
     * Handle invoice events
     */
    @KafkaListener(topics = "invoice.events", groupId = "${KAFKA_CONSUMER_GROUP_ID:bss-workflow}")
    @Transactional
    public void handleInvoiceEvent(InvoiceEvent event) {
        log.info("Processing invoice event for workflow: {}", event.getType());

        try {
            // Future: Add invoice-related workflows
            // e.g., "invoice_overdue", "invoice_paid", etc.
        } catch (Exception e) {
            log.error("Failed to process invoice event in workflow engine", e);
        }
    }

    /**
     * Trigger workflow by name
     */
    @Transactional
    public void triggerWorkflow(String workflowName, String entityType, String entityId, Object eventData) {
        log.info("Triggering workflow: {} for entity: {}/{}", workflowName, entityType, entityId);

        try {
            // Get workflow by name
            Workflow workflow = workflowRepository.findByName(workflowName)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowName));

            if (!workflow.isActive()) {
                log.warn("Workflow is not active: {}", workflowName);
                return;
            }

            // Create execution context
            Map<String, Object> context = createContextFromEvent(entityType, entityId, eventData);

            // Create and start execution
            UUID executionId = executionService.createExecution(workflow, entityType, UUID.fromString(entityId), context);
            log.info("Created workflow execution: {} for workflow: {}", executionId, workflowName);

            // Execute workflow asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    executionService.executeWorkflow(executionId);
                } catch (Exception e) {
                    log.error("Failed to execute workflow: {}", executionId, e);
                }
            });

        } catch (Exception e) {
            log.error("Failed to trigger workflow: {}", workflowName, e);
            throw new RuntimeException("Failed to trigger workflow: " + workflowName, e);
        }
    }

    /**
     * Get workflow execution status
     */
    public WorkflowExecution getExecutionStatus(UUID executionId) {
        return executionService.getExecution(executionId);
    }

    /**
     * Get all executions for an entity
     */
    public java.util.List<WorkflowExecution> getExecutionsForEntity(String entityType, UUID entityId) {
        return executionService.getExecutionsForEntity(entityType, entityId);
    }

    /**
     * Cancel workflow execution
     */
    public void cancelExecution(UUID executionId) {
        executionService.cancelExecution(executionId);
    }

    /**
     * Retry failed execution
     */
    public void retryExecution(UUID executionId) {
        executionService.retryExecution(executionId);
    }

    /**
     * Create context from event data
     */
    private Map<String, Object> createContextFromEvent(String entityType, String entityId, Object eventData) {
        try {
            // Convert event to map
            @SuppressWarnings("unchecked")
            Map<String, Object> eventMap = objectMapper.convertValue(eventData, Map.class);

            // Add standard context
            eventMap.put("entity_type", entityType);
            eventMap.put("entity_id", entityId);
            eventMap.put("triggered_at", java.time.Instant.now().toString());

            return eventMap;
        } catch (Exception e) {
            log.error("Failed to create context from event", e);
            return Map.of(
                "entity_type", entityType,
                "entity_id", entityId,
                "triggered_at", java.time.Instant.now().toString()
            );
        }
    }
}
