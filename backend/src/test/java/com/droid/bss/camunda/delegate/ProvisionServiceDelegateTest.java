package com.droid.bss.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProvisionServiceDelegate
 */
@ExtendWith(MockitoExtension.class)
class ProvisionServiceDelegateTest {

    @Mock
    private DelegateExecution execution;

    private ProvisionServiceDelegate delegate;

    @BeforeEach
    void setUp() {
        delegate = new ProvisionServiceDelegate();
    }

    @Test
    void testExecuteWithListOfServices() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn("process-123");
        when(execution.getVariable("customerId")).thenReturn("customer-123");
        when(execution.getVariable("services")).thenReturn(List.of("portal", "api", "support"));

        // When
        delegate.execute(execution);

        // Then
        verify(execution).setVariable("services_provisioned", true);
        verify(execution).setVariable(eq("provisioning_results"), any(Map.class));
        verify(execution).setVariable(eq("services_provisioned_at"), anyString());

        // Verify provisioning results
        ArgumentCaptor<Map<String, Object>> resultsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(execution).setVariable(eq("provisioning_results"), resultsCaptor.capture());
        Map<String, Object> results = resultsCaptor.getValue();

        assertEquals("customer-123", results.get("customer_id"));
        assertEquals("provisioned", results.get("status"));
        assertTrue(results.containsKey("provisioning_id"));
        assertTrue(results.containsKey("services_detail"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> servicesDetail = (List<Map<String, Object>>) results.get("services_detail");
        assertEquals(3, servicesDetail.size());

        // Verify each service
        assertEquals("portal", servicesDetail.get(0).get("service_name"));
        assertEquals("active", servicesDetail.get(0).get("status"));

        assertEquals("api", servicesDetail.get(1).get("service_name"));
        assertEquals("active", servicesDetail.get(1).get("status"));

        assertEquals("support", servicesDetail.get(2).get("service_name"));
        assertEquals("active", servicesDetail.get(2).get("status"));
    }

    @Test
    void testExecuteWithCommaSeparatedString() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn("process-456");
        when(execution.getVariable("customerId")).thenReturn("customer-456");
        when(execution.getVariable("services")).thenReturn("basic, premium, enterprise");

        // When
        delegate.execute(execution);

        // Then
        verify(execution).setVariable("services_provisioned", true);

        ArgumentCaptor<Map<String, Object>> resultsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(execution).setVariable(eq("provisioning_results"), resultsCaptor.capture());
        Map<String, Object> results = resultsCaptor.getValue();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> servicesDetail = (List<Map<String, Object>>) results.get("services_detail");
        assertEquals(3, servicesDetail.size());
    }

    @Test
    void testExecuteWithEmptyServices() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn("process-789");
        when(execution.getVariable("customerId")).thenReturn("customer-789");
        when(execution.getVariable("services")).thenReturn("");

        // When
        delegate.execute(execution);

        // Then
        verify(execution).setVariable("services_provisioned", true);
    }

    @Test
    void testExecuteHandlesException() throws Exception {
        // Given
        when(execution.getProcessInstanceId()).thenReturn("process-error");
        when(execution.getVariable("customerId")).thenReturn("customer-error");
        when(execution.getVariable("services")).thenReturn("basic");
        doThrow(new RuntimeException("Provisioning failed")).when(execution).setVariable(anyString(), any());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> delegate.execute(execution));
        assertEquals("Provisioning failed", exception.getMessage());

        verify(execution).setVariable("services_provisioned", false);
        verify(execution).setVariable(eq("provisioning_error"), anyString());
    }

    @Test
    void testSimulateProvisioning() {
        // When
        Map<String, Object> result = delegate.simulateProvisioning("customer-100",
                List.of("basic", "premium"));

        // Then
        assertEquals("customer-100", result.get("customer_id"));
        assertEquals("provisioned", result.get("status"));
        assertTrue(result.get("provisioning_id").toString().startsWith("prov-"));
        assertTrue(result.containsKey("services_detail"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> services = (List<Map<String, Object>>) result.get("services_detail");
        assertEquals(2, services.size());

        assertEquals("basic", services.get(0).get("service_name"));
        assertEquals("active", services.get(0).get("status"));
        assertTrue(services.get(0).containsKey("provisioned_at"));

        assertEquals("premium", services.get(1).get("service_name"));
        assertEquals("active", services.get(1).get("status"));
    }
}
