package com.droid.bss.camunda.integration;

import com.droid.bss.camunda.service.CamundaWorkflowService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Camunda workflow execution
 *
 * These tests require Camunda to be running and the BPMN models to be deployed.
 * Tests verify end-to-end workflow execution.
 */
@SpringBootTest
@ActiveProfiles("test")
class CamundaWorkflowIntegrationTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private CamundaWorkflowService workflowService;

    @Test
    void testStartAndMonitorCustomerOnboardingWorkflow() {
        // Given
        String customerId = "test-customer-" + System.currentTimeMillis();
        Map<String, Object> variables = Map.of(
                "customerId", customerId,
                "customerName", "Test Customer",
                "customerEmail", "test@example.com",
                "customer_status", "NEW",
                "services", "basic,portal,support"
        );

        // When - Start the workflow
        String processInstanceId = workflowService.startProcessInstance(
                "customerOnboarding",
                customerId,
                variables
        );

        // Then - Verify process instance was created
        assertNotNull(processInstanceId);
        assertTrue(workflowService.isProcessInstanceActive(processInstanceId));

        // Verify variables were set
        Map<String, Object> processVariables = workflowService.getProcessInstanceVariables(processInstanceId);
        assertEquals(customerId, processVariables.get("customerId"));
        assertEquals("Test Customer", processVariables.get("customerName"));
        assertEquals("test@example.com", processVariables.get("customerEmail"));

        // Note: Actual workflow completion would require:
        // 1. Mocking external services (email, provisioning)
        // 2. Or using Camunda Test utilities to simulate task completion
        // 3. The workflow will pause at the 30-day timer event

        // Clean up
        workflowService.cancelProcessInstance(processInstanceId);
    }

    @Test
    void testStartAndMonitorPaymentFailedRecoveryWorkflow() {
        // Given
        String paymentId = "test-payment-" + System.currentTimeMillis();
        Map<String, Object> variables = Map.of(
                "paymentId", paymentId,
                "customerId", "test-customer-456",
                "amount", 99.99,
                "currency", "USD",
                "reason", "Card expired"
        );

        // When - Start the workflow
        String processInstanceId = workflowService.startProcessInstance(
                "paymentFailedRecovery",
                paymentId,
                variables
        );

        // Then - Verify process instance was created
        assertNotNull(processInstanceId);
        assertTrue(workflowService.isProcessInstanceActive(processInstanceId));

        // Verify variables were set
        Map<String, Object> processVariables = workflowService.getProcessInstanceVariables(processInstanceId);
        assertEquals(paymentId, processVariables.get("paymentId"));
        assertEquals("test-customer-456", processVariables.get("customerId"));
        assertEquals(99.99, processVariables.get("amount"));
        assertEquals("USD", processVariables.get("currency"));
        assertEquals("Card expired", processVariables.get("reason"));

        // Note: The workflow will pause at the 3-day timer event
        // To test complete flow, you would need to:
        // 1. Wait for timer (or advance time in tests)
        // 2. Complete the retry payment task
        // 3. Verify the gateway path taken

        // Clean up
        workflowService.cancelProcessInstance(processInstanceId);
    }

    @Test
    void testWorkflowVariableManagement() {
        // Given
        String processId = "test-process-" + System.currentTimeMillis();
        Map<String, Object> initialVariables = Map.of("key1", "value1");

        // When - Start process
        String processInstanceId = workflowService.startProcessInstance(
                "customerOnboarding",
                processId,
                initialVariables
        );

        // Then - Test variable operations
        assertTrue(workflowService.isProcessInstanceActive(processInstanceId));

        // Get single variable
        Object value1 = workflowService.getProcessInstanceVariable(processInstanceId, "key1");
        assertEquals("value1", value1);

        // Set new variable
        workflowService.setProcessInstanceVariable(processInstanceId, "key2", "value2");
        Object value2 = workflowService.getProcessInstanceVariable(processInstanceId, "key2");
        assertEquals("value2", value2);

        // Set multiple variables
        Map<String, Object> newVariables = Map.of("key3", 123, "key4", true);
        workflowService.setProcessInstanceVariables(processInstanceId, newVariables);

        // Get all variables
        Map<String, Object> allVariables = workflowService.getProcessInstanceVariables(processInstanceId);
        assertEquals(4, allVariables.size());
        assertEquals("value1", allVariables.get("key1"));
        assertEquals("value2", allVariables.get("key2"));
        assertEquals(123, allVariables.get("key3"));
        assertEquals(true, allVariables.get("key4"));

        // Clean up
        workflowService.cancelProcessInstance(processInstanceId);
    }

    @Test
    void testWorkflowQueryOperations() {
        // Given - Start multiple processes
        String processKey1 = "query-test-1";
        String processKey2 = "query-test-2";

        String processInstanceId1 = workflowService.startProcessInstance(
                "customerOnboarding",
                processKey1,
                Map.of("customerId", processKey1)
        );

        String processInstanceId2 = workflowService.startProcessInstance(
                "customerOnboarding",
                processKey2,
                Map.of("customerId", processKey2)
        );

        try {
            // When - Query by business key
            List<ProcessInstance> instances = workflowService.getProcessInstancesByBusinessKey(processKey1);

            // Then - Verify query results
            assertFalse(instances.isEmpty());
            assertEquals(1, instances.size());
            assertEquals(processInstanceId1, instances.get(0).getId());

            // Query running instances by process definition
            List<ProcessInstance> runningInstances = workflowService.getRunningProcessInstances("customerOnboarding");
            assertTrue(runningInstances.size() >= 2);

            // Get active process count
            long activeCount = workflowService.getActiveProcessInstanceCount("customerOnboarding");
            assertTrue(activeCount >= 2);

        } finally {
            // Clean up
            workflowService.cancelProcessInstance(processInstanceId1);
            workflowService.cancelProcessInstance(processInstanceId2);
        }
    }

    @Test
    void testProcessInstanceCancellation() {
        // Given
        String processId = "cancel-test-" + System.currentTimeMillis();
        String processInstanceId = workflowService.startProcessInstance(
                "customerOnboarding",
                processId,
                Map.of("customerId", processId)
        );

        assertTrue(workflowService.isProcessInstanceActive(processInstanceId));

        // When - Cancel the process
        workflowService.cancelProcessInstance(processInstanceId);

        // Then - Verify it's no longer active
        assertFalse(workflowService.isProcessInstanceActive(processInstanceId));
    }
}
