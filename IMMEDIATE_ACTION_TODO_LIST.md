# IMMEDIATE ACTION TODO LIST
## BSS Testing Framework - Next 30 Days

---

## 🎯 WEEK 1-2: BACKEND CRITICAL GAPS

### Application Layer Tests (HIGHEST PRIORITY)

#### Day 1-2: Customer Use Cases
```bash
# Create these test files:
backend/src/test/java/com/droid/bss/application/command/customer/
├── CreateCustomerUseCaseTest.java
├── UpdateCustomerUseCaseTest.java
└── DeleteCustomerUseCaseTest.java

backend/src/test/java/com/droid/bss/application/query/customer/
├── GetCustomerByIdUseCaseTest.java
├── GetCustomersUseCaseTest.java
└── SearchCustomersUseCaseTest.java
```

#### Day 3-4: Order Use Cases
```bash
backend/src/test/java/com/droid/bss/application/command/order/
├── CreateOrderUseCaseTest.java
├── UpdateOrderStatusUseCaseTest.java
└── CancelOrderUseCaseTest.java

backend/src/test/java/com/droid/bss/application/query/order/
├── GetOrderByIdUseCaseTest.java
└── GetOrdersByCustomerUseCaseTest.java
```

#### Day 5-6: Invoice & Payment Use Cases
```bash
backend/src/test/java/com/droid/bss/application/command/invoice/
├── GenerateInvoiceUseCaseTest.java
└── SendInvoiceUseCaseTest.java

backend/src/test/java/com/droid/bss/application/command/payment/
├── ProcessPaymentUseCaseTest.java
└── RefundPaymentUseCaseTest.java
```

#### Day 7: Subscription Use Cases
```bash
backend/src/test/java/com/droid/bss/application/command/subscription/
├── SubscribeUseCaseTest.java
├── CancelSubscriptionUseCaseTest.java
└── UpdateSubscriptionUseCaseTest.java
```

**Expected Outcome: 15 new test files, 60 tests**

### Infrastructure Layer Tests

#### Day 8-10: Configuration Tests
```bash
backend/src/test/java/com/droid/bss/infrastructure/config/
├── DatabaseConfigTest.java
├── RedisConfigTest.java
└── KafkaConfigTest.java
```

#### Day 11-12: Cache Tests
```bash
backend/src/test/java/com/droid/bss/infrastructure/cache/
├── RedisCacheServiceTest.java
└── CacheKeyGenerationTest.java
```

#### Day 13-14: Messaging Tests
```bash
backend/src/test/java/com/droid/bss/infrastructure/messaging/
├── KafkaProducerTest.java
└── KafkaConsumerTest.java
```

**Expected Outcome: 8 new test files, 32 tests**

---

## 🎯 WEEK 3-4: FRONTEND CRITICAL GAPS

### Component Testing Setup

#### Day 15-16: Install & Configure Vue Test Utils
```bash
cd frontend

# Install dependencies
npm install -D @vue/test-utils @testing-library/vue jsdom

# Update vitest.config.ts
# Add test.environment configuration
```

#### Day 17-18: Test Common Components
```bash
frontend/tests/unit/components/
├── common/Header.spec.ts
├── common/Footer.spec.ts
├── common/FormInput.spec.ts
└── common/Button.spec.ts
```

#### Day 19-20: Test Customer Components
```bash
frontend/tests/unit/components/
├── customer/CustomerCard.spec.ts
├── customer/CustomerForm.spec.ts
└── customer/CustomerList.spec.ts
```

**Expected Outcome: 8 new test files, 40 tests**

### Composables Testing

#### Day 21-22: Test Core Composables
```bash
frontend/tests/unit/composables/
├── useApi.spec.ts
├── useAuth.spec.ts
└── useValidation.spec.ts
```

### More Store Tests

#### Day 23-24: Test Missing Stores
```bash
frontend/tests/unit/
├── address.store.spec.ts  # ALREADY EXISTS
├── coverage-node.store.spec.ts  # ALREADY EXISTS
├── asset.store.spec.ts  # ALREADY EXISTS
└── settings.store.spec.ts  # CREATE NEW
```

**Expected Outcome: 1 new test file, 10 tests**

### Page Tests

#### Day 25-28: Test Critical Pages
```bash
frontend/tests/unit/pages/
├── billing/dashboard.spec.ts
├── billing/cycles/index.spec.ts
├── subscriptions/create.spec.ts
└── addresses/index.spec.ts
```

**Expected Outcome: 4 new test files, 20 tests**

---

## 🎯 WEEK 5-6: E2E & INTEGRATION

### E2E Error Scenarios

#### Day 29-32: Add Error Handling E2E Tests
```bash
frontend/tests/e2e/
├── error-handling.spec.ts  # CREATE
├── unauthorized-access.spec.ts
└── network-errors.spec.ts
```

### Integration Tests for New Modules

#### Day 33-36: Address Module Integration
```bash
backend/src/test/java/com/droid/bss/integration/
├── AddressCrudIntegrationTest.java
└── AddressFlowIntegrationTest.java

backend/src/test/java/com/droid/bss/api/address/
└── AddressControllerWebTest.java  # CREATE
```

#### Day 37-40: Coverage-Node Module Integration
```bash
backend/src/test/java/com/droid/bss/integration/
├── CoverageNodeCrudIntegrationTest.java

backend/src/test/java/com/droid/bss/api/coverage-node/
└── CoverageNodeControllerWebTest.java  # CREATE
```

**Expected Outcome: 4 new test files, 25 tests**

---

## 📊 DAILY ROUTINE

### Morning (9:00-10:00)
- [ ] Run existing tests (`mvn test` for backend, `npm test` for frontend)
- [ ] Check coverage reports
- [ ] Review flaky tests from previous day
- [ ] Plan today's test development

### Development (10:00-12:00, 14:00-17:00)
- [ ] Write new tests
- [ ] Implement test data factories
- [ ] Review PRs with test coverage checks

### Evening (17:00-17:30)
- [ ] Run full test suite
- [ ] Update coverage metrics
- [ ] Report progress in Slack/Teams

---

## 🏗️ TEMPLATE FOR NEW TEST FILES

### Backend Test Template (JUnit)
```java
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CreateCustomerUseCase createCustomerUseCase;

    @Test
    @DisplayName("Should create customer with valid data")
    void shouldCreateCustomerWithValidData() {
        // Arrange
        CreateCustomerCommand command = CreateCustomerCommand.builder()
            .email("test@example.com")
            .firstName("John")
            .lastName("Doe")
            .build();

        Customer expectedCustomer = Customer.builder()
            .id("cust-123")
            .email("test@example.com")
            .firstName("John")
            .lastName("Doe")
            .build();

        when(customerRepository.save(any(Customer.class)))
            .thenReturn(expectedCustomer);

        // Act
        Customer result = createCustomerUseCase.execute(command);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid email")
    void shouldThrowExceptionForInvalidEmail() {
        // Arrange
        CreateCustomerCommand command = CreateCustomerCommand.builder()
            .email("invalid-email")
            .firstName("John")
            .lastName("Doe")
            .build();

        // Act & Assert
        assertThatThrownBy(() -> createCustomerUseCase.execute(command))
            .isInstanceOf(InvalidCustomerException.class)
            .hasMessage("Invalid email format");
    }
}
```

### Frontend Test Template (Vitest)
```typescript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import CustomerForm from '@/app/components/customer/CustomerForm.vue'

describe('CustomerForm', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should render form fields', () => {
    render(CustomerForm)

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /submit/i })).toBeInTheDocument()
  })

  it('should emit submit event with form data', async () => {
    const { emitted } = render(CustomerForm)

    await userEvent.type(screen.getByLabelText(/email/i), 'test@example.com')
    await userEvent.type(screen.getByLabelText(/first name/i), 'John')
    await userEvent.type(screen.getByLabelText(/last name/i), 'Doe')
    await userEvent.click(screen.getByRole('button', { name: /submit/i }))

    expect(emitted('submit')).toHaveLength(1)
    expect(emitted('submit')[0][0]).toEqual({
      email: 'test@example.com',
      firstName: 'John',
      lastName: 'Doe'
    })
  })

  it('should show validation errors for empty fields', async () => {
    render(CustomerForm)

    await userEvent.click(screen.getByRole('button', { name: /submit/i }))

    expect(screen.getByText(/email is required/i)).toBeInTheDocument()
    expect(screen.getByText(/first name is required/i)).toBeInTheDocument()
  })
})
```

---

## 🚀 QUICK START COMMANDS

### Backend
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CustomerControllerWebTest

# Run with coverage
mvn test jacoco:report

# Run integration tests only
mvn test -Dtest=*IntegrationTest

# Run in parallel (4 threads)
mvn test -T 4
```

### Frontend
```bash
# Run all tests
npm test

# Run in watch mode
npm test -- --watch

# Run with coverage
npm test -- --coverage

# Run specific test file
npm test customer.store.spec.ts

# Run E2E tests
npm run test:e2e
```

---

## 📈 COVERAGE TARGETS

### Week 2 Targets
- Backend Unit Tests: 85% (from 75%)
- Frontend Unit Tests: 60% (from 45%)

### Week 4 Targets
- Backend Unit Tests: 90%
- Frontend Unit Tests: 75%

### Week 6 Targets
- Backend Unit Tests: 92%
- Frontend Unit Tests: 80%
- E2E Test Coverage: 85%

---

## 🐛 TROUBLESHOOTING

### Common Issues & Solutions

#### Issue 1: Testcontainers Container Won't Start
```bash
# Check Docker status
docker ps

# Restart Docker
sudo systemctl restart docker

# Pull images manually
docker pull postgres:18-alpine
docker pull confluentinc/cp-kafka:7.4.0
docker pull redis:7-alpine

# Check disk space
df -h
```

#### Issue 2: Vue Test Utils Not Working
```bash
# Install dependencies
npm install -D @vue/test-utils @testing-library/vue

# Update vitest.config.ts
export default defineConfig({
  test: {
    environment: 'jsdom',
    setupFiles: ['./tests/setup.ts']
  }
})

# Create tests/setup.ts
import '@testing-library/jest-dom'
```

#### Issue 3: High Test Execution Time
```bash
# Run tests in parallel
mvn test -T 4  # Backend
npm test -- --parallel  # Frontend

# Run only affected tests
mvn test -Dtest=CustomerControllerWebTest

# Skip slow integration tests
mvn test -Dtest=!CustomerCrudIntegrationTest
```

#### Issue 4: Flaky Tests
```bash
# Increase timeout
test.setTimeout(10000)

# Add proper waits
await page.waitForSelector('[data-testid="element"]')

# Use retry mechanism
import { retry } from 'vitest/retry'
const result = await retry(() => someOperation())
```

---

## 📝 CHECKLIST FOR EACH NEW TEST

Before marking a test as complete, verify:

- [ ] Test follows naming convention (Method_Scenario_Expected)
- [ ] Test has clear Arrange-Act-Assert structure
- [ ] Test uses factories for data creation
- [ ] Test has proper assertions
- [ ] Test cleans up after itself
- [ ] Test runs successfully in CI
- [ ] Test adds coverage (check report)
- [ ] Test is reviewed by peer
- [ ] Test documentation is updated

---

## 🎯 SUCCESS METRICS (Week 6)

### Quantitative
- [ ] 65 new test files created
- [ ] 260+ new tests written
- [ ] Backend coverage: 92%
- [ ] Frontend coverage: 80%
- [ ] 0 flaky tests

### Qualitative
- [ ] All critical paths tested
- [ ] Error scenarios covered
- [ ] Edge cases handled
- [ ] Performance acceptable (<5 min unit tests)
- [ ] Documentation updated

---

## 🤝 TEAM ASSIGNMENTS

### Backend Team (Alice, Bob)
**Focus:** Application layer tests, Infrastructure tests
**Goal:** 30 test files in 4 weeks
**Daily:** 2 test files

### Frontend Team (Charlie, Diana)
**Focus:** Component tests, Composables, Page tests
**Goal:** 35 test files in 4 weeks
**Daily:** 2-3 test files

### DevOps Team (Eve)
**Focus:** CI/CD integration, Coverage reporting
**Goal:** Setup complete automation
**Daily:** Monitor & optimize

---

## 📞 SUPPORT

### Slack Channels
- #testing-framework - General discussion
- #backend-tests - Backend-specific
- #frontend-tests - Frontend-specific
- #devops-testing - DevOps & CI/CD

### Documentation
- `/home/labadmin/projects/droid-spring/TEST_COVERAGE_ANALYSIS_AND_ROADMAP.md` - Full analysis
- `/home/labadmin/projects/droid-spring/ANALIZA-SUSO-0511.md` - Framework guide
- `/home/labadmin/projects/droid-spring/frontend/tests/framework/README.md` - Playwright & Vitest
- `/home/labadmin/projects/droid-spring/backend/TESTCONTAINERS.md` - Testcontainers guide

### Tech Lead Office Hours
- Tuesday & Thursday 14:00-15:00
- Book via Calendar
- Or DM on Slack

---

**START DATE:** 2025-11-06
**END DATE:** 2025-12-18 (6 weeks)
**STATUS:** Ready to begin ✅

**Remember: Test quality > test quantity. Each test should add real value!**
