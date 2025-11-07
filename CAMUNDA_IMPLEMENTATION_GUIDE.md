# Camunda BPM Implementation Guide

## Overview

This document describes the Camunda BPM integration in the BSS application. Camunda is a powerful workflow and decision automation platform that allows you to design, execute, and monitor business processes.

## Architecture

### Components

1. **Camunda Engine** - The core workflow engine integrated with Spring Boot
2. **BPMN 2.0 Models** - Visual workflow definitions stored in `src/main/resources/processes/`
3. **Java Delegates** - Service task implementations in `src/main/java/com/droid/bss/camunda/delegate/`
4. **Event Listeners** - Bridge between Kafka events and Camunda processes
5. **Workflow Service** - Clean API for workflow management

### Integration Points

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Domain    │  Kafka  │  Camunda     │  BPMN   │   Business  │
│   Events    │ ──────> │  Event       │ ──────> │  Processes  │
│             │         │  Listener    │         │             │
└─────────────┘         └──────────────┘         └─────────────┘
                                │                        │
                                │                        ▼
                         ┌──────────────┐         ┌─────────────┐
                         │  Camunda     │         │   Java      │
                         │  Workflow    │ <─────> │  Delegates  │
                         │  Service     │         │             │
                         └──────────────┘         └─────────────┘
```

## Workflow Processes

### 1. Customer Onboarding Process

**Process ID:** `customerOnboarding`
**Business Key:** Customer ID
**Description:** Handles new customer onboarding with service provisioning and follow-up

**Steps:**
1. **Send Welcome Email** - Sends personalized welcome email to new customer
2. **Provision Default Services** - Sets up basic services (portal, support, API access)
3. **Wait 30 Days** - Timer event to wait before next action
4. **Create Check-in Ticket** - Creates support ticket for customer satisfaction check

**BPMN File:** `src/main/resources/processes/customer-onboarding.bpmn`

### 2. Payment Failed Recovery Process

**Process ID:** `paymentFailedRecovery`
**Business Key:** Payment ID
**Description:** Handles payment failures with retry logic and service suspension

**Steps:**
1. **Send Payment Alert** - Notifies customer of failed payment
2. **Wait 3 Days** - Grace period for customer to update payment info
3. **Retry Payment** - Attempts to process payment again
4. **Exclusive Gateway** - Checks if payment was successful
   - **Success Path:** Process ends successfully
   - **Failure Path:**
     5. **Suspend Services** - Temporarily suspends customer services
     6. **Wait 7 Days** - Final grace period
     7. **Escalate to Human** - Creates high-priority support ticket
     8. **End Failure** - Process ends with failure status

**BPMN File:** `src/main/resources/processes/payment-failed-recovery.bpmn`

## Configuration

### Dependencies

The following dependencies are required in `pom.xml`:

```xml
<dependency>
    <groupId>org.camunda.bpm.springboot</groupId>
    <artifactId>camunda-bpm-spring-boot-starter</artifactId>
    <version>7.21.0</version>
</dependency>
<dependency>
    <groupId>org.camunda.bpm.springboot</groupId>
    <artifactId>camunda-bpm-spring-boot-starter-webapp</artifactId>
    <version>7.21.0</version>
</dependency>
<dependency>
    <groupId>org.camunda.bpm.springboot</groupId>
    <artifactId>camunda-bpm-spring-boot-starter-rest</artifactId>
    <version>7.21.0</version>
</dependency>
```

### Spring Configuration

Add `@EnableProcessApplication` to your main application class:

```java
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableProcessApplication
public class BssApplication {
    public static void main(String[] args) {
        SpringApplication.run(BssApplication.class, args);
    }
}
```

### Camunda Configuration

The Camunda configuration is in `src/main/java/com/droid/bss/camunda/config/CamundaConfiguration.java`:

- Async task executor configured
- Process application enabled
- Job execution enabled for timer events

## Event Listeners

### CamundaEventListener

Listens to domain events and triggers Camunda processes:

```java
@Component
public class CamundaEventListener {
    // Listens to: customer.created
    public void handleCustomerCreatedEvent(CustomerCreatedEvent event)

    // Listens to: payment.failed
    public void handlePaymentFailedEvent(PaymentFailedEvent event)
}
```

### CamundaCloudEventListener

Listens to CloudEvent format messages and triggers workflows:

```java
@Component
public class CamundaCloudEventListener {
    // Listens to CloudEvent topics
    public void handleCustomerCloudEvent(CloudEvent cloudEvent)
    public void handlePaymentCloudEvent(CloudEvent cloudEvent)
}
```

## Java Delegates

Java Delegates are service task implementations:

### 1. SendEmailDelegate
- Sends emails using Spring Mail
- Templates: welcome_email, payment_failed
- Variables: `to`, `subject`, `template`, `customer_name`, `customerId`, `amount`

### 2. ProvisionServiceDelegate
- Simulates service provisioning
- Variables: `customerId`, `services`
- Sets: `services_provisioned`, `provisioning_results`

### 3. CreateTicketDelegate
- Creates support tickets
- Variables: `customerId`, `queue`, `priority`, `subject`, `description`
- Sets: `ticket_created`, `ticket_id`

### 4. RetryPaymentDelegate
- Simulates payment retry (90% success rate)
- Variables: `paymentId`, `customerId`, `amount`
- Sets: `payment_status`, `payment_response_code`, `payment_retry_attempted`

### 5. SuspendServicesDelegate
- Simulates service suspension
- Variables: `customerId`, `reason`
- Sets: `services_suspended`, `suspension_results`

## Workflow Service API

Use `CamundaWorkflowService` to manage workflows:

```java
@Service
public class YourService {
    @Autowired
    private CamundaWorkflowService workflowService;

    // Start a process
    String processInstanceId = workflowService.startProcessInstance(
        "customerOnboarding",
        customerId,
        variables
    );

    // Get process variables
    Map<String, Object> variables = workflowService.getProcessInstanceVariables(processInstanceId);

    // Complete a task
    workflowService.completeTask(taskId, variables);

    // Signal an execution
    workflowService.signal(executionId);

    // Cancel a process instance
    workflowService.cancelProcessInstance(processInstanceId);
}
```

## Monitoring and Management

### Camunda Cockpit

Access the Camunda web interface at:
- **Dev:** http://localhost:8080/camunda
- Production URL depends on your deployment

Features:
- Process instance monitoring
- Task management
- Job execution status
- BPMN model visualization
- Variable inspection

### Database Tables

Camunda uses the following tables:
- `ACT_RU_PROCESSINSTANCE` - Running process instances
- `ACT_RU_TASK` - User tasks
- `ACT_RU_EXECUTION` - Process executions
- `ACT_RU_VARIABLE` - Process variables
- `ACT_RU_JOB` - Async jobs and timers

### Actuator Endpoints

Monitor Camunda via Spring Boot Actuator:
- `GET /actuator/health` - Overall health
- `GET /actuator/metrics` - Runtime metrics

## Testing

### Unit Tests

Test Java Delegates:

```java
@SpringBootTest
class SendEmailDelegateTest {
    @Autowired
    private SendEmailDelegate delegate;

    @Test
    void testExecute() {
        // Mock DelegateExecution
        // Call delegate.execute(execution)
        // Verify variables are set
    }
}
```

### Integration Tests

Test workflow execution:

```java
@SpringBootTest
class CamundaWorkflowIntegrationTest {
    @Autowired
    private CamundaWorkflowService workflowService;

    @Test
    void testCustomerOnboarding() {
        // Start process
        String processId = workflowService.startProcessInstance(
            "customerOnboarding",
            "customer-123",
            variables
        );

        // Wait for completion
        // Verify results
    }
}
```

## Migration from Legacy Workflow

The old `WorkflowEngineService` is deprecated. Migration steps:

1. **Update Event Listeners**
   - Old: Use `WorkflowEngineService.handleCustomerEvent()`
   - New: Use `CamundaEventListener` or `CamundaCloudEventListener`

2. **Update Workflow Management**
   - Old: Use `WorkflowExecutionService`
   - New: Use `CamundaWorkflowService`

3. **Migrate Workflow Definitions**
   - Old: JSON-based workflow definitions
   - New: BPMN 2.0 visual models

4. **Update Monitoring**
   - Old: Custom monitoring
   - New: Camunda Cockpit

## Best Practices

### 1. Process Design
- Keep processes simple and focused
- Use parallel gateways for independent tasks
- Use timer events for delays
- Use message events for communication

### 2. Error Handling
- Always use try-catch in Java Delegates
- Set failure variables on error
- Use boundary events for error handling

### 3. Performance
- Use async task execution
- Configure job executor for timers
- Monitor job backlog
- Use process variables sparingly

### 4. Testing
- Test each Java Delegate independently
- Test complete workflows with integration tests
- Mock external dependencies
- Test error scenarios

### 5. Maintenance
- Version control BPMN models
- Document complex process logic
- Monitor process instance counts
- Clean up old process instances

## Troubleshooting

### Common Issues

**Process doesn't start**
- Check BPMN process ID matches
- Verify process is deployed
- Check required variables are provided

**Timer not firing**
- Check job executor is enabled
- Verify timer configuration
- Check database job table

**Service task fails**
- Check Java Delegate implementation
- Verify dependency injection
- Review logs for exceptions

**Variables not set**
- Check input/output mapping in BPMN
- Verify variable names match
- Check process instance exists

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    org.camunda.bpm: DEBUG
    com.droid.bss.camunda: DEBUG
```

## Resources

- [Camunda Documentation](https://docs.camunda.org/)
- [BPMN 2.0 Specification](https://www.omg.org/spec/BPMN/2.0/)
- [Camunda Spring Boot Integration](https://docs.camunda.org/docs/guides/spring-boot-integration/)
- [Java Delegate Documentation](https://docs.camunda.org/docs/guides/best-practices/)
