package com.droid.bss.camunda.service;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CamundaWorkflowService
 */
@ExtendWith(MockitoExtension.class)
class CamundaWorkflowServiceTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    @Mock
    private ProcessInstance processInstance;

    @Mock
    private Task task;

    private CamundaWorkflowService workflowService;

    @BeforeEach
    void setUp() {
        workflowService = new CamundaWorkflowService(runtimeService, taskService);
    }

    @Test
    void testStartProcessInstance() {
        // Given
        Map<String, Object> variables = Map.of("key1", "value1", "key2", 123);
        when(runtimeService.startProcessInstanceByKey("testProcess", "businessKey-123", variables))
                .thenReturn(processInstance);
        when(processInstance.getId()).thenReturn("process-456");

        // When
        String processInstanceId = workflowService.startProcessInstance("testProcess", "businessKey-123", variables);

        // Then
        assertEquals("process-456", processInstanceId);
        verify(runtimeService).startProcessInstanceByKey("testProcess", "businessKey-123", variables);
    }

    @Test
    void testStartProcessInstanceWithTenant() {
        // Given
        Map<String, Object> variables = Map.of("key1", "value1");
        when(runtimeService.createProcessInstanceByKey("testProcess"))
                .thenReturn(mock(org.camunda.bpm.engine.runtime.ProcessInstanceBuilder.class));
        org.camunda.bpm.engine.runtime.ProcessInstanceBuilder builder = runtimeService.createProcessInstanceByKey("testProcess");
        when(builder.businessKey("businessKey-123")).thenReturn(builder);
        when(builder.tenantId("tenant-1")).thenReturn(builder);
        when(builder.setVariables(variables)).thenReturn(builder);
        when(builder.execute()).thenReturn(processInstance);
        when(processInstance.getId()).thenReturn("process-789");

        // When
        String processInstanceId = workflowService.startProcessInstance("testProcess", "businessKey-123", "tenant-1", variables);

        // Then
        assertEquals("process-789", processInstanceId);
        verify(builder).execute();
    }

    @Test
    void testGetProcessInstanceVariables() {
        // Given
        Map<String, Object> expectedVariables = Map.of("var1", "value1", "var2", 456);
        when(runtimeService.getVariables("process-123")).thenReturn(expectedVariables);

        // When
        Map<String, Object> variables = workflowService.getProcessInstanceVariables("process-123");

        // Then
        assertEquals(expectedVariables, variables);
        verify(runtimeService).getVariables("process-123");
    }

    @Test
    void testGetProcessInstanceVariable() {
        // Given
        when(runtimeService.getVariable("process-123", "key1")).thenReturn("value1");

        // When
        Object value = workflowService.getProcessInstanceVariable("process-123", "key1");

        // Then
        assertEquals("value1", value);
        verify(runtimeService).getVariable("process-123", "key1");
    }

    @Test
    void testSetProcessInstanceVariable() {
        // When
        workflowService.setProcessInstanceVariable("process-123", "key1", "value1");

        // Then
        verify(runtimeService).setVariable("process-123", "key1", "value1");
    }

    @Test
    void testSetProcessInstanceVariables() {
        // Given
        Map<String, Object> variables = Map.of("key1", "value1", "key2", 456);

        // When
        workflowService.setProcessInstanceVariables("process-123", variables);

        // Then
        verify(runtimeService).setVariables("process-123", variables);
    }

    @Test
    void testGetProcessInstancesByBusinessKey() {
        // Given
        List<ProcessInstance> expectedInstances = List.of(processInstance);
        when(runtimeService.createProcessInstanceQuery())
                .thenReturn(mock(org.camunda.bpm.engine.runtime.ProcessInstanceQuery.class));
        org.camunda.bpm.engine.runtime.ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        when(query.businessKey("businessKey-123")).thenReturn(query);
        when(query.list()).thenReturn(expectedInstances);

        // When
        List<ProcessInstance> instances = workflowService.getProcessInstancesByBusinessKey("businessKey-123");

        // Then
        assertEquals(expectedInstances, instances);
        verify(query).list();
    }

    @Test
    void testGetProcessInstance() {
        // Given
        when(runtimeService.createProcessInstanceQuery())
                .thenReturn(mock(org.camunda.bpm.engine.runtime.ProcessInstanceQuery.class));
        org.camunda.bpm.engine.runtime.ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        when(query.processInstanceId("process-123")).thenReturn(query);
        when(query.singleResult()).thenReturn(processInstance);

        // When
        ProcessInstance instance = workflowService.getProcessInstance("process-123");

        // Then
        assertEquals(processInstance, instance);
    }

    @Test
    void testGetRunningProcessInstances() {
        // Given
        List<ProcessInstance> expectedInstances = List.of(processInstance);
        when(runtimeService.createProcessInstanceQuery())
                .thenReturn(mock(org.camunda.bpm.engine.runtime.ProcessInstanceQuery.class));
        org.camunda.bpm.engine.runtime.ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        when(query.processDefinitionKey("testProcess")).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.list()).thenReturn(expectedInstances);

        // When
        List<ProcessInstance> instances = workflowService.getRunningProcessInstances("testProcess");

        // Then
        assertEquals(expectedInstances, instances);
        verify(query).active();
    }

    @Test
    void testSignal() {
        // When
        workflowService.signal("execution-123");

        // Then
        verify(runtimeService).signal("execution-123");
    }

    @Test
    void testSignalWithVariables() {
        // Given
        Map<String, Object> variables = Map.of("key1", "value1");

        // When
        workflowService.signal("execution-123", variables);

        // Then
        verify(runtimeService).signal("execution-123", variables);
    }

    @Test
    void testCancelProcessInstance() {
        // When
        workflowService.cancelProcessInstance("process-123");

        // Then
        verify(runtimeService).deleteProcessInstance("process-123", "Cancelled by API");
    }

    @Test
    void testGetTasksForProcessInstance() {
        // Given
        List<Task> expectedTasks = List.of(task);
        when(taskService.createTaskQuery())
                .thenReturn(mock(org.camunda.bpm.engine.task.TaskQuery.class));
        org.camunda.bpm.engine.task.TaskQuery query = taskService.createTaskQuery();
        when(query.processInstanceId("process-123")).thenReturn(query);
        when(query.list()).thenReturn(expectedTasks);

        // When
        List<Task> tasks = workflowService.getTasksForProcessInstance("process-123");

        // Then
        assertEquals(expectedTasks, tasks);
        verify(query).list();
    }

    @Test
    void testGetTasksAssignedTo() {
        // Given
        List<Task> expectedTasks = List.of(task);
        when(taskService.createTaskQuery())
                .thenReturn(mock(org.camunda.bpm.engine.task.TaskQuery.class));
        org.camunda.bpm.engine.task.TaskQuery query = taskService.createTaskQuery();
        when(query.assignee("user123")).thenReturn(query);
        when(query.list()).thenReturn(expectedTasks);

        // When
        List<Task> tasks = workflowService.getTasksAssignedTo("user123");

        // Then
        assertEquals(expectedTasks, tasks);
        verify(query).list();
    }

    @Test
    void testCompleteTask() {
        // When
        workflowService.completeTask("task-123");

        // Then
        verify(taskService).complete("task-123");
    }

    @Test
    void testCompleteTaskWithVariables() {
        // Given
        Map<String, Object> variables = Map.of("key1", "value1");

        // When
        workflowService.completeTask("task-123", variables);

        // Then
        verify(taskService).complete("task-123", variables);
    }

    @Test
    void testGetActiveProcessInstanceCount() {
        // Given
        when(runtimeService.createProcessInstanceQuery())
                .thenReturn(mock(org.camunda.bpm.engine.runtime.ProcessInstanceQuery.class));
        org.camunda.bpm.engine.runtime.ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        when(query.processDefinitionKey("testProcess")).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.count()).thenReturn(5L);

        // When
        long count = workflowService.getActiveProcessInstanceCount("testProcess");

        // Then
        assertEquals(5L, count);
        verify(query).count();
    }

    @Test
    void testIsProcessInstanceActive() {
        // Given
        when(runtimeService.createProcessInstanceQuery())
                .thenReturn(mock(org.camunda.bpm.engine.runtime.ProcessInstanceQuery.class));
        org.camunda.bpm.engine.runtime.ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        when(query.processInstanceId("process-123")).thenReturn(query);
        when(query.singleResult()).thenReturn(processInstance);
        when(processInstance.isActive()).thenReturn(true);

        // When
        boolean isActive = workflowService.isProcessInstanceActive("process-123");

        // Then
        assertTrue(isActive);
    }

    @Test
    void testIsProcessInstanceActiveReturnsFalse() {
        // Given
        when(runtimeService.createProcessInstanceQuery())
                .thenReturn(mock(org.camunda.bpm.engine.runtime.ProcessInstanceQuery.class));
        org.camunda.bpm.engine.runtime.ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        when(query.processInstanceId("process-456")).thenReturn(query);
        when(query.singleResult()).thenReturn(null);

        // When
        boolean isActive = workflowService.isProcessInstanceActive("process-456");

        // Then
        assertFalse(isActive);
    }

    @Test
    void testStartProcessInstanceByMessage() {
        // Given
        Map<String, Object> variables = Map.of("key1", "value1");
        when(runtimeService.startProcessInstanceByMessage("messageName", variables))
                .thenReturn(processInstance);
        when(processInstance.getId()).thenReturn("process-789");

        // When
        String processInstanceId = workflowService.startProcessInstanceByMessage("messageName", variables);

        // Then
        assertEquals("process-789", processInstanceId);
        verify(runtimeService).startProcessInstanceByMessage("messageName", variables);
    }

    @Test
    void testCorrelateMessage() {
        // Given
        Map<String, Object> variables = Map.of("key1", "value1");
        when(runtimeService.createMessageCorrelation("messageName"))
                .thenReturn(mock(org.camunda.bpm.engine.runtime.MessageCorrelationBuilder.class));
        org.camunda.bpm.engine.runtime.MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation("messageName");
        when(builder.processInstanceId("process-123")).thenReturn(builder);
        when(builder.setVariables(variables)).thenReturn(builder);

        // When
        workflowService.correlateMessage("messageName", "process-123", variables);

        // Then
        verify(builder).correlate();
    }
}
