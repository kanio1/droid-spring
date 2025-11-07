# Recommended Testing Tools for BSS Stack

**Date**: 2025-11-06
**Purpose**: Identify popular testing tools missing from our current stack

---

## 🎯 Top 3 Recommended Tools

### 1. **Apache JMeter** - Load & Performance Testing

**Why We Need It**:
- Most popular open-source load testing tool (GitHub: 20k+ stars)
- Industry standard for enterprise load testing
- Rich GUI for test creation + CLI for CI/CD
- Better integration with enterprise environments than K6

**What It Adds Beyond K6**:
- **GUI Test Designer** - Visual test creation (K6 is code-only)
- **Protocol Support** - HTTP/HTTPS, JDBC, LDAP, JMS, FTP, MongoDB, REST APIs
- **Enterprise Features** - Distributed testing, real-time results, extensive reporting
- **Test Fragments** - Reusable test components
- **Backend Listeners** - Send metrics to Grafana, InfluxDB, Elasticsearch
- **Correlation Support** - Better handling of dynamic data (cookies, tokens)

**Use Cases**:
- Load testing database operations (JDBC protocol)
- Testing LDAP authentication flows
- Performance testing of message queues (JMS)
- FTP/file upload performance
- End-to-end multi-protocol scenarios

**Installation**:
```bash
# macOS
brew install jmeter

# Linux
apt-get install jmeter

# Docker
docker run -i -p 8080:8080 justb4/docker-jmeter -n -t test-plan.jmx -l results.jtl

# Or download from: https://jmeter.apache.org/download_jmeter.cgi
```

**Integration with CI/CD**:
```xml
<!-- Add to backend/pom.xml -->
<plugin>
    <groupId>com.lazerycode.jmeter</groupId>
    <artifactId>jmeter-maven-plugin</artifactId>
    <version>3.0.0</version>
    <executions>
        <execution>
            <id>jmeter-test</id>
            <phase>verify</phase>
            <goals>
                <goal>jmeter</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**JMeter vs K6**:
| Feature | JMeter | K6 |
|---------|--------|-----|
| GUI | ✅ Excellent | ❌ Code only |
| Distributed Testing | ✅ | ✅ |
| Protocol Support | ✅ JDBC, LDAP, JMS | ❌ HTTP only |
| CI/CD Integration | ✅ Maven, Ant, CLI | ✅ CLI |
| Learning Curve | Moderate | Easy |
| Enterprise Adoption | Very High | Growing |

---

### 2. **Pact** - Contract Testing for Microservices

**Why We Need It**:
- Industry-standard for consumer-driven contract testing
- Critical for microservices with REST APIs
- Prevents integration bugs between services
- Popular in Spring Boot ecosystem (Spring Cloud Contract)

**What It Adds**:
- **Consumer Tests** - Frontend defines expectations from backend
- **Provider Verification** - Backend validates it meets contract
- **CI Integration** - Prevents breaking changes
- **Pact Broker** - Central registry of contracts
- **Multiple Languages** - Java, JavaScript, Python, Go, etc.

**Use Cases**:
- Customer service ↔ Order service API integration
- Frontend ↔ Backend API contracts
- Microservice integration testing
- Regression prevention in distributed systems

**Implementation**:

**Step 1: Frontend (Consumer) - Install Pact**
```bash
cd frontend
pnpm add -D @pact-foundation/pact
```

**Step 2: Create Consumer Test**
```typescript
// frontend/tests/contract/customer.api.pact.test.ts
import { pactWith } from '@pact-foundation/pact';
import { getCustomer, createOrder } from '../services/api';

pactWith({ consumer: 'frontend', provider: 'backend' }, (provider) => {
    beforeEach(() => {
        provider.addInteraction({
            state: 'customer exists',
            uponReceiving: 'a request for customer',
            withRequest: {
                method: 'GET',
                path: '/api/v1/customers/123',
            },
            willRespondWith: {
                status: 200,
                body: {
                    id: '123',
                    firstName: 'John',
                    lastName: 'Doe',
                    email: 'john@example.com',
                },
            },
        });
    });

    it('should get customer', async () => {
        const customer = await getCustomer('123');
        expect(customer.id).toBe('123');
    });
});
```

**Step 3: Backend (Provider) - Verify Contract**
```xml
<!-- Add to backend/pom.xml -->
<dependency>
    <groupId>au.com.dius.pact.provider</groupId>
    <artifactId>spring-mvc</artifactId>
    <version>4.3.5</version>
    <scope>test</scope>
</dependency>
```

```java
// Backend test
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("backend")
@Consumer("frontend")
@PactBroker(url = "http://pact-broker:80")
public class ContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationTargetProvider.class)
    @PactVerification(value = "backend")
    public void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("customer exists")
    public void customerExists() {
        // Setup test data
    }
}
```

**Step 4: CI/CD Integration**
```yaml
# .github/workflows/contract-testing.yml
- name: Run contract tests
  run: |
    cd frontend
    pnpm run test:contract

- name: Publish contracts
  run: |
    pact-broker publish frontend/pacts/ \
      --consumer-app-version=${{ github.sha }} \
      --provider-app-version=${{ github.sha }} \
      --broker-url=${{ secrets.PACT_BROKER_URL }}
```

---

### 3. **Percy** - Visual Regression Testing

**Why We Need It**:
- Industry-leading visual testing platform
- Essential for UI/UX validation
- Catches visual regressions humans miss
- Integrates perfectly with Storybook/Nuxt

**What It Adds**:
- **Pixel-Perfect Diff Detection** - Automatic visual regression detection
- **Multi-Device Testing** - Chrome, Firefox, Safari, mobile
- **Storybook Integration** - Automatic screenshot of all stories
- **CI Integration** - Automatic visual QA in PRs
- **Review Workflows** - Approve/reject visual changes
- **Cross-Browser Testing** - Consistent UI across browsers

**Use Cases**:
- Validate UI changes don't break existing pages
- Test responsive design across devices
- Catch CSS regressions
- Visual QA in CI/CD pipeline
- Design system compliance

**Implementation**:

**Step 1: Install Percy SDK**
```bash
cd frontend
pnpm add -D @percy/cli @percy/storybook
```

**Step 2: Configure Percy**
```typescript
// percy.config.ts
import { PercyConfig } from '@percy/cli';

export default {
    version: 2,
    snapshot: {
        widths: [375, 768, 1280],
        minHeight: 1024,
        percyCSS: '',
    },
    discovery: {
        allowedHostnames: ['localhost', 'bss.example.com'],
        networkIdleTimeout: 750,
        disableCache: false,
    },
} satisfies PercyConfig;
```

**Step 3: Snapshot in Tests**
```typescript
// frontend/tests/visual/customer-list.visual.test.ts
import { test, expect } from '@playwright/test';
import percySnapshot from '@percy/playwright';

test('customer list visual test', async ({ page }) => {
    await page.goto('http://localhost:3000/customers');

    await percySnapshot(page, 'Customer List - Default', {
        widths: [375, 768, 1280],
    });

    // Visual assertions
    await expect(page.locator('[data-testid="customer-list"]')).toBeVisible();
});
```

**Step 4: Storybook Integration**
```json
// frontend/.storybook/main.js
module.exports = {
    framework: '@storybook/vue3-vite',
    stories: ['../app/**/*.stories.@(js|ts)'],
    addons: [
        '@storybook/addon-essentials',
        '@percy/storybook',
    ],
};
```

```json
// frontend/.storybook/preview.js
export const parameters = {
    percy: {
        skip: false,
        widths: [375, 768, 1280],
    },
};
```

**Step 5: CI/CD Integration**
```yaml
# .github/workflows/visual-testing.yml
- name: Run visual tests
  env:
    PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}
  run: |
    cd frontend
    pnpm run test:visual

- name: Run Storybook Percy
  env:
    PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}
  run: |
    cd frontend
    pnpm run storybook:percy
```

**Alternative: Chromatic** (for Storybook)
```bash
pnpm add -D chromatic
# Then run: npx chromatic --project-token=$CHROMATIC_TOKEN
```

---

## 🔄 Integration Strategy

### Phase 1: Add JMeter (Week 1)

**Benefits**:
- Immediate load testing capabilities
- Better than K6 for enterprise features
- GUI makes debugging easier

**Actions**:
1. Install JMeter locally
2. Create basic HTTP test plan for API
3. Add JMeter Maven plugin
4. Create distributed test config
5. Integrate with CI/CD

**Example Test Plan**:
```xml
<!-- jmeter/test-plans/api-load-test.jmx -->
<?xml version="1.0"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.comments">BSS API Load Test</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
      <stringProp name="TestPlan.user_define_classpath"></stringProp>
    </TestPlan>

    <hashTree>
      <ThreadGroup>
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
        <stringProp name="ThreadGroup.delay">0</stringProp>
        <boolProp name="ThreadGroup.scheduler">true</boolProp>
      </ThreadGroup>

      <hashTree>
        <HTTPSamplerProxy>
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.path">/api/v1/customers</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

### Phase 2: Add Pact (Week 2-3)

**Benefits**:
- Prevent integration bugs
- Frontend-backend contract validation
- CI/CD integration

**Actions**:
1. Setup Pact Broker (standalone Docker or hosted)
2. Add Pact to frontend
3. Create consumer tests
4. Add Spring Cloud Contract to backend
5. Create provider verification tests
6. Add to CI/CD pipeline

**Pact Broker Setup**:
```yaml
# docker-compose.pact.yml
version: '3.8'
services:
  pact-broker:
    image: pactfoundation/pact-broker:latest
    ports:
      - "9292:9292"
    environment:
      PACT_BROKER_DATABASE_URL: postgres://postgres:password@pact-db:5432/pact_broker
      PACT_BROKER_PORT: 9292

  pact-db:
    image: postgres:13
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
      POSTGRES_DB: pact_broker
```

### Phase 3: Add Percy (Week 3-4)

**Benefits**:
- Catch visual regressions
- Cross-browser testing
- Storybook integration

**Actions**:
1. Sign up for Percy (free tier available)
2. Add PERCY_TOKEN to GitHub Secrets
3. Install Percy SDK
4. Create visual tests
5. Add Storybook Percy integration
6. Add to CI/CD

---

## 🛠️ Additional Tools Worth Considering

### 4. **REST Assured** - API Testing for Java

**Better than Postman for CI/CD**:
```java
@Test
public void testCustomerAPI() {
    given()
        .auth().basic("user", "password")
        .when()
        .get("/api/v1/customers/123")
        .then()
        .statusCode(200)
        .body("id", equalTo("123"));
}
```

### 5. **DbTest** - Database Testing

**For testing database logic**:
```java
@Test
@DatabaseSetup("/datasets/customer.xml")
public void testCustomerCreation() {
    Customer customer = customerService.create("John", "Doe");
    assertThat(customer.getId()).isNotNull();
}
```

### 6. **Mutation Testing (PiTest)** - Test Quality

**Ensure tests actually work**:
```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.15.0</version>
</plugin>
```

---

## 📊 Testing Tool Matrix

| Tool | Purpose | Priority | Effort | Benefit |
|------|---------|----------|--------|---------|
| **JMeter** | Load Testing | High | Low | Excellent |
| **Pact** | Contract Testing | High | Medium | Excellent |
| **Percy** | Visual Testing | Medium | Medium | High |
| REST Assured | API Testing | Medium | Low | Medium |
| DbTest | Database Testing | Low | Medium | Medium |
| PiTest | Mutation Testing | Low | High | Low |

---

## 🎯 Recommendations

### Top Priority (Implement Now)

1. **Apache JMeter** - Replace/enhance K6 for enterprise load testing
2. **Pact** - Critical for microservices integration
3. **Percy** - Essential for visual regression testing

### Future Enhancements

4. **REST Assured** - Better than MockMvc for API testing
5. **DbTest** - Database-centric testing
6. **Mutation Testing** - Only if 80%+ coverage achieved

---

## 💡 Implementation Timeline

```
Week 1: Install and configure JMeter
    ↓
Week 2: Create JMeter test plans for critical APIs
    ↓
Week 3: Setup Pact Broker, add consumer tests
    ↓
Week 4: Add provider verification, integrate with CI/CD
    ↓
Week 5: Install Percy, create visual tests
    ↓
Week 6: Full integration with all pipelines
```

---

## 📚 Resources

**JMeter**:
- https://jmeter.apache.org/
- https://github.com/jmeter-maven-plugin/jmeter-maven-plugin

**Pact**:
- https://docs.pact.io/
- https://github.com/DiUS/pact-jvm

**Percy**:
- https://percy.io/
- https://github.com/percy/percy-storybook

**Contract Testing Best Practices**:
- https://docs.pact.io/pact_nirvana_kata

**Visual Testing Guide**:
- https://percy.io/docs

---

## ✅ Decision

**Recommended Immediate Actions**:

1. **Add JMeter** - Download, create basic test, integrate with Maven
2. **Setup Pact Broker** - Docker compose, publish first contract
3. **Sign up for Percy** - Get free tier, run first snapshot

These three tools will significantly improve our testing coverage and catch issues our current tools miss.
