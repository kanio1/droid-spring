package com.droid.bss.camunda.service;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Camunda Workflow Service
 *
 * Provides a clean API for managing Camunda workflows.
 * This service wraps Camunda's RuntimeService and TaskService to provide
 * a simplified interface for workflow operations.
 */
@Service
public class CamundaWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(CamundaWorkflowService.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public CamundaWorkflowService(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    /**
     * Start a process instance by key
     */
    public String startProcessInstance(String processDefinitionKey, String businessKey, Map<String, Object> variables) {
        logger.info("Starting process instance: {} for business key: {}", processDefinitionKey, businessKey);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                processDefinitionKey,
                businessKey,
                variables
        );

        logger.info("Started process instance: {} (ID: {})", processDefinitionKey, processInstance.getId());
        return processInstance.getId();
    }

    /**
     * Start a process instance by key with tenant ID
     */
    public String startProcessInstance(String processDefinitionKey, String businessKey, String tenantId, Map<String, Object> variables) {
        logger.info("Starting process instance: {} for business key: {} in tenant: {}", processDefinitionKey, businessKey, tenantId);

        ProcessInstance processInstance = runtimeService.createProcessInstanceByKey(processDefinitionKey)
                .businessKey(businessKey)
                .tenantId(tenantId)
                .setVariables(variables)
                .execute();

        logger.info("Started process instance: {} (ID: {}) in tenant: {}", processDefinitionKey, processInstance.getId(), tenantId);
        return processInstance.getId();
    }

    /**
     * Get process instance variables
     */
    public Map<String, Object> getProcessInstanceVariables(String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    /**
     * Get a specific process instance variable
     */
    public Object getProcessInstanceVariable(String processInstanceId, String variableName) {
        return runtimeService.getVariable(processInstanceId, variableName);
    }

    /**
     * Set a process instance variable
     */
    public void setProcessInstanceVariable(String processInstanceId, String variableName, Object value) {
        runtimeService.setVariable(processInstanceId, variableName, value);
    }

    /**
     * Set multiple process instance variables
     */
    public void setProcessInstanceVariables(String processInstanceId, Map<String, Object> variables) {
        runtimeService.setVariables(processInstanceId, variables);
    }

    /**
     * Get all process instances for a business key
     */
    public List<ProcessInstance> getProcessInstancesByBusinessKey(String businessKey) {
        return runtimeService.createProcessInstanceQuery()
                .businessKey(businessKey)
                .list();
    }

    /**
     * Get process instance by ID
     */
    public ProcessInstance getProcessInstance(String processInstanceId) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
    }

    /**
     * Get all running process instances for a definition
     */
    public List<ProcessInstance> getRunningProcessInstances(String processDefinitionKey) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .active()
                .list();
    }

    /**
     * Signal an execution (for signal events)
     */
    public void signal(String executionId) {
        logger.info("Sending signal to execution: {}", executionId);
        runtimeService.signal(executionId);
    }

    /**
     * Signal an execution with variables
     */
    public void signal(String executionId, Map<String, Object> variables) {
        logger.info("Sending signal to execution: {} with variables", executionId);
        runtimeService.signal(executionId, variables);
    }

    /**
     * Cancel a process instance
     */
    public void cancelProcessInstance(String processInstanceId) {
        logger.info("Cancelling process instance: {}", processInstanceId);
        runtimeService.deleteProcessInstance(processInstanceId, "Cancelled by API");
    }

    /**
     * Get tasks for a process instance
     */
    public List<Task> getTasksForProcessInstance(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
    }

    /**
     * Get tasks assigned to a user
     */
    public List<Task> getTasksAssignedTo(String assignee) {
        return taskService.createTaskQuery()
                .assignee(assignee)
                .list();
    }

    /**
     * Complete a task
     */
    public void completeTask(String taskId) {
        logger.info("Completing task: {}", taskId);
        taskService.complete(taskId);
    }

    /**
     * Complete a task with variables
     */
    public void completeTask(String taskId, Map<String, Object> variables) {
        logger.info("Completing task: {} with variables", taskId);
        taskService.complete(taskId, variables);
    }

    /**
     * Get the count of active process instances for a definition
     */
    public long getActiveProcessInstanceCount(String processDefinitionKey) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .active()
                .count();
    }

    /**
     * Check if a process instance is active
     */
    public boolean isProcessInstanceActive(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        return processInstance != null && processInstance.isActive();
    }

    /**
     * Correlate a message to start a process instance
     */
    public String startProcessInstanceByMessage(String messageName, Map<String, Object> variables) {
        logger.info("Starting process instance by message: {}", messageName);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByMessage(messageName, variables);
        logger.info("Started process instance by message: {} (ID: {})", messageName, processInstance.getId());
        return processInstance.getId();
    }

    /**
     * Correlate a message to a process instance
     */
    public void correlateMessage(String messageName, String processInstanceId, Map<String, Object> variables) {
        logger.info("Correlating message: {} to process instance: {}", messageName, processInstanceId);
        runtimeService.createMessageCorrelation(messageName)
                .processInstanceId(processInstanceId)
                .setVariables(variables)
                .correlate();
    }
}
