# Camunda BPM Migration Plan
**Refactoring from Custom Workflow to Camunda**
**Data:** 2025-11-07

---

## 📊 Current Implementation Analysis

### What We Have (Custom Workflow)
**Strengths:**
- ✅ Event-driven architecture (Kafka listeners)
- ✅ 2 pre-defined workflows (Customer Onboarding, Payment Failed)
- ✅ Async execution
- ✅ Basic action execution (email, ticket, payment retry)
- ✅ Database persistence (V1041 migration)

**Weaknesses:**
- ❌ **No durability** - workflows may lose state on restart
- ❌ **No monitoring** - no Cockpit, Tasklist, or admin UI
- ❌ **Simple conditions** - basic string replacement
- ❌ **No human tasks** - no task assignment/claiming
- ❌ **Poor timer handling** - Thread.sleep, not durable
- ❌ **No retries** - basic error handling
- ❌ **Custom code** - hard to maintain, no tooling
- ❌ **No BPMN** - no visual modeling
- ❌ **No versioning** - hard to update workflows

---

## 🎯 Why Camunda?

### Benefits Over Custom Implementation
| Feature | Custom Workflow | Camunda | Impact |
|---------|----------------|---------|--------|
| **Durability** | Basic DB | Full persistence + history | ⭐⭐⭐⭐⭐ |
| **Monitoring** | None | Cockpit, Tasklist, Admin UI | ⭐⭐⭐⭐⭐ |
| **BPMN 2.0** | No | Yes (visual) | ⭐⭐⭐⭐⭐ |
| **Human Tasks** | No | Yes (with assignment) | ⭐⭐⭐⭐ |
| **Timer Jobs** | Thread.sleep | Durable timers | ⭐⭐⭐⭐⭐ |
| **Retries** | Basic | Built-in retry strategies | ⭐⭐⭐⭐ |
| **Conditions** | String replace | JUEL/Spin expressions | ⭐⭐⭐⭐ |
| **Scalability** | Single instance | Cluster-ready | ⭐⭐⭐⭐⭐ |
| **Community** | None | Large + Enterprise | ⭐⭐⭐⭐ |
| **Documentation** | Custom | Extensive | ⭐⭐⭐⭐ |

### Business Benefits
- 🚀 **Faster Development** - Visual modeling, not code
- 📊 **Better Monitoring** - Full visibility into workflows
- 👥 **Human Tasks** - Users can interact with workflows
- ⏱️ **Reliable Timers** - No missed delays after restart
- 🔄 **Easy Updates** - Deploy new BPMN without code changes
- 🛡️ **Production Proven** - Used by Fortune 500 companies

---

## 🗺️ Migration Strategy

### Phase 1: Foundation (1-2 days)
1. **Add Camunda Dependencies**
   - camunda-bpm-spring-boot-starter
   - camunda-bpm-spring-boot-starter-webapp
   - camunda-bpm-spring-boot-starter-rest

2. **Configure Camunda**
   - Database tables (auto-created)
   - Engine configuration
   - Admin users

3. **Create Camunda Configuration Class**
   - Process application annotation
   - Custom configurations

### Phase 2: BPMN Models (1 day)
1. **Customer Onboarding Process**
   - Start event → Send Email → Provision Service → Timer → Create Ticket → End
   - Use service tasks, timer events, message events

2. **Payment Failed Process**
   - Start event → Send Alert → Timer (3 days) → Retry Payment → Gateway (success/fail)
   - Path 1: Success → End
   - Path 2: Failure → Timer (7 days) → Suspend Services → Timer (30 days) → Escalate → End

### Phase 3: Java Delegates (1 day)
1. **SendEmailDelegate**
   - Replace WorkflowActionExecutor.send_email
   - Use spring EmailService

2. **ProvisionServiceDelegate**
   - Replace WorkflowActionExecutor.provision_service
   - Mock service provisioning

3. **CreateTicketDelegate**
   - Replace WorkflowActionExecutor.create_ticket
   - Mock ticket creation

4. **RetryPaymentDelegate**
   - Replace WorkflowActionExecutor.retry_payment
   - Simulate payment retry

5. **SuspendServicesDelegate**
   - Replace WorkflowActionExecutor.suspend_services
   - Mock service suspension

### Phase 4: Event Integration (0.5 day)
1. **Refactor Event Listeners**
   - Update WorkflowEngineService
   - Use Camunda RuntimeService
   - Start processes via process key

2. **Variable Mapping**
   - Pass event data as process variables
   - Use Spring Expression Language

### Phase 5: Testing (1 day)
1. **Unit Tests** - Test Java Delegates
2. **Integration Tests** - Test process execution
3. **E2E Tests** - Test full event-to-workflow flow

### Phase 6: Cleanup (0.5 day)
1. **Remove Custom Workflow Code**
   - Delete WorkflowEngineService
   - Delete WorkflowExecutionService
   - Delete domain models
   - Keep WorkflowActionExecutor (legacy support)

2. **Remove Database Tables**
   - Drop V1041 migration (later)
   - Keep as reference during migration

---

## 🏗️ Implementation Steps

### Step 1: Dependencies
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
```

### Step 2: Configuration
```java
@SpringBootApplication
@EnableProcessApplication
public class BssApplication {
    public static void main(String[] args) {
        SpringApplication.run(BssApplication.class, args);
    }
}
```

### Step 3: BPMN Model (Example)
```xml
<definitions>
  <process id="customerOnboarding" name="Customer Onboarding">
    <startEvent id="start"/>
    <serviceTask id="sendWelcomeEmail" name="Send Welcome Email"/>
    <serviceTask id="provisionDefaultServices" name="Provision Default Services"/>
    <timerEventDefinition>
      <timeDuration>P30D</timeDuration>
    </timerEventDefinition>
    <serviceTask id="createCheckInTicket" name="Create Check-in Ticket"/>
    <endEvent id="end"/>
  </process>
</definitions>
```

### Step 4: Java Delegate
```java
@Component
public class SendEmailDelegate implements JavaDelegate {
    @Autowired
    private EmailService emailService;

    @Override
    public void execute(DelegateExecution execution) {
        String customerEmail = execution.getVariable("customer_email");
        emailService.sendWelcomeEmail(customerEmail);
    }
}
```

### Step 5: Trigger from Event
```java
@Service
public class CamundaEventListener {
    @Autowired
    private RuntimeService runtimeService;

    @KafkaListener(topics = "customer.events")
    public void handleCustomerEvent(CustomerEvent event) {
        Map<String, Object> variables = Map.of(
            "customerId", event.getCustomerId().toString(),
            "customerEmail", event.getEmail(),
            "customerName", event.getName()
        );
        runtimeService.startProcessInstanceByKey("customerOnboarding", variables);
    }
}
```

---

## 📂 File Changes

### New Files (Camunda Integration)
```
backend/
├── src/main/java/com/droid/bss/
│   ├── camunda/
│   │   ├── config/
│   │   │   └── CamundaConfiguration.java
│   │   ├── delegate/
│   │   │   ├── SendEmailDelegate.java
│   │   │   ├── ProvisionServiceDelegate.java
│   │   │   ├── CreateTicketDelegate.java
│   │   │   ├── RetryPaymentDelegate.java
│   │   │   └── SuspendServicesDelegate.java
│   │   ├── listener/
│   │   │   └── CamundaEventListener.java
│   └── integration/
│       └── CamundaWorkflowService.java
│
├── src/main/resources/processes/
│   ├── customer-onboarding.bpmn
│   └── payment-failed-recovery.bpmn
│
└── pom.xml (updated)
```

### Files to Delete (Custom Workflow)
```
DELETE:
├── WorkflowEngineService.java
├── WorkflowExecutionService.java
├── WorkflowActionExecutor.java (or keep as legacy)
├── Workflow.java
├── WorkflowExecution.java
├── WorkflowRepository.java
├── V1041__create_workflow_engine_tables.sql (later, keep for reference)
```

---

## 🧪 Testing Strategy

### Unit Tests
```java
@SpringBootTest
class SendEmailDelegateTest {
    @Mock
    private EmailService emailService;

    @InjectMocks
    private SendEmailDelegate delegate;

    @Test
    void shouldSendWelcomeEmail() {
        DelegateExecution execution = mock(DelegateExecution.class);
        when(execution.getVariable("customer_email")).thenReturn("test@example.com");

        delegate.execute(execution);

        verify(emailService).sendWelcomeEmail("test@example.com");
    }
}
```

### Integration Tests
```java
@SpringBootTest
@TestMethodOrder(OrderAnnotation.class)
class CustomerOnboardingProcessTest {

    @Autowired
    private RuntimeService runtimeService;

    @Test
    void shouldCompleteCustomerOnboarding() {
        Map<String, Object> variables = Map.of(
            "customerId", "123",
            "customerEmail", "test@example.com"
        );

        ProcessInstance instance = runtimeService.startProcessInstanceByKey("customerOnboarding", variables);

        assertThat(instance).isNotNull();
        assertThat(instance.getState()).isEqualTo(State.COMPLETED);
    }
}
```

### E2E Tests
```java
@SpringBootTest
@Testcontainers
class WorkflowE2ETest {

    @Test
    void shouldTriggerWorkflowFromEvent() {
        // Publish customer.created event
        kafkaTemplate.send("customer.events", new CustomerCreatedEvent(...));

        // Wait for process to complete
        await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> {
                List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
                    .processDefinitionKey("customerOnboarding")
                    .list();

                assertThat(instances).hasSize(1);
                assertThat(instances.get(0).isEnded()).isTrue();
            });
    }
}
```

---

## 📊 Performance Comparison

| Metric | Custom Workflow | Camunda | Delta |
|--------|----------------|---------|-------|
| **Development Time** | 2 days | 5 days | -3 days (but more features) |
| **Time to Market** | Fast | Faster (visual) | ✅ |
| **Monitoring** | Manual logs | Full Cockpit | ⭐⭐⭐⭐⭐ |
| **Maintenance** | Hard (custom) | Easy (BPMN) | ⭐⭐⭐⭐⭐ |
| **Reliability** | Basic | Enterprise | ⭐⭐⭐⭐⭐ |
| **Scalability** | Manual | Auto | ⭐⭐⭐⭐⭐ |

---

## 🚨 Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| **Learning Curve** | Medium | Medium | - Start with simple processes<br>- Use Camunda's excellent docs |
| **Migration Effort** | High | Low | - Phased approach<br>- Keep old code as fallback |
| **Performance** | Low | Low | - Camunda is optimized<br>- Use production config |
| **Database Size** | Low | Medium | - Archive old data<br>- Configure history retention |
| **Team Training** | Medium | High | - Internal workshops<br>- Use visual tools first |

---

## 🎯 Success Criteria

### Technical
- ✅ All 2 workflows migrated to Camunda
- ✅ Event listeners trigger Camunda processes
- ✅ Java Delegates execute all actions
- ✅ All tests passing
- ✅ Camunda Cockpit accessible

### Business
- ✅ No manual workflow changes needed
- ✅ Full monitoring available
- ✅ Human tasks can be assigned
- ✅ Workflows survive restart
- ✅ Better error handling

---

## 🏃‍♂️ Next Steps

1. **Start with Phase 1** - Add dependencies, configure Camunda
2. **Create simple test** - Start process manually
3. **Build BPMN models** - Use Camunda Modeler
4. **Implement Java Delegates** - One by one
5. **Integrate events** - Update listeners
6. **Test thoroughly** - Unit, integration, E2E
7. **Deploy and monitor** - Use Cockpit

---

**Estimated Timeline: 5-6 days**
**Expected Outcome: Production-ready, enterprise-grade workflow engine**
