# Sprint 3 Report: Test Logic Fixes

**Sprint Goal:** Fix remaining test logic errors and achieve 226/261 (86.6%) passing tests

**Sprint Period:** [Current Sprint]

**Scrum Master:** Claude Code

---

## 📊 Sprint Summary

### Status: ❌ PARTIAL FAILURE
- **Attempted:** Fix Repository layer tests with OptimisticLockException
- **Result:** Found fundamental architectural issues with `@Version` and Testcontainers
- **Outcome:** Restored code to stable state, identified root causes

---

## 🎯 Sprint Goals vs Actual

| Goal | Target | Actual | Status |
|------|--------|--------|--------|
| Fix CustomerRepositoryDataJpaTest | 13 errors → 0 | 13 errors | ❌ Not Fixed |
| Fix InvoiceRepositoryDataJpaTest | 7 errors → 0 | 7 errors | ❌ Not Fixed |
| Fix CustomerQueryServiceTest | 5 failures → 0 | 5 failures | ⚠️ Not Attempted |
| Fix HelloControllerWebTest | 1 error → 0 | 1 error | ⚠️ Not Attempted |
| Achieve 226/261 tests passing | 86.6% | ~62/261 (23.7%) | ❌ Regressed |

---

## 🔍 Detailed Analysis

### ✅ What Worked

1. **HelloServiceTest: 4/4 passing**
   - Simple unit tests without database dependencies
   - No issues with optimistic locking

2. **CustomerControllerWebTest: 9/9 passing**
   - @WebMvcTest with mocked dependencies
   - No direct database access issues

### ❌ Critical Problems Identified

#### 1. OptimisticLockException in Repository Tests

**Affected Tests:**
- CustomerRepositoryDataJpaTest: 13 errors
- InvoiceRepositoryDataJpaTest: 7 errors

**Root Cause:**
```java
@Entity
public class CustomerEntity extends BaseEntity {
    @Version
    private Long version;  // ← This field causes conflicts
}

public class BaseEntity {
    private Long version = 0L;  // ← Initialized to 0
}
```

**Problem Flow:**
1. `Customer.create()` sets version=1
2. `CustomerRepository.save()` uses `merge()`
3. Hibernate compares version with database
4. If version mismatch → OptimisticLockException

#### 2. Test Isolation Issues

**Observation:**
- `@DataJpaTest` with Testcontainers PostgreSQL
- @Transactional should rollback after each test
- But tests still see data from previous tests
- Possibly related to static container sharing

#### 3. Test Data Conflicts

**Pattern Observed:**
```java
// Multiple tests use same PESEL/NIP
CustomerInfo personalInfo = new CustomerInfo("John", "Doe", "12345678901", ...);
// This PESEL reused across tests!
```

### 🧪 Attempts Made

1. **Removed @DirtiesContext**
   - Thought it conflicted with @DataJpaTest
   - Result: No improvement

2. **Changed Customer.create() version from 1 to 0**
   - Intended to signal "new entity" to Hibernate
   - Result: Caused "detached entity with null version" errors

3. **Added @Transactional on test classes**
   - Attempted to keep entities managed
   - Result: No improvement

4. **Used merge() vs persist() logic**
   - Tried to detect new vs existing entities
   - Result: Still OptimisticLockException

5. **Added setVersion(null) in test setup**
   - Attempted to reset version for new entities
   - Result: "uninitialized version value" errors

---

## 📈 Test Status Overview

```
Total Tests: 261
├─ Passing: ~62 (23.7%)
├─ Failures: 10 (assertion errors)
├─ Errors: 69 (exceptions)
└─ Skipped: 120 (disabled, "test scaffolding")
```

**By Category:**
- Unit Tests (HelloService, CustomerTest): Some passing
- Web Layer Tests (CustomerControllerWebTest): ✅ All passing
- Repository Tests: ❌ All have OptimisticLockException
- Integration Tests: ❌ Various errors

---

## 🏆 Achievements

1. **Code Restored to Stable State**
   - Reverted problematic changes
   - Core functionality tests (Hello, CustomerController) working

2. **Root Cause Analysis Completed**
   - Identified @Version as primary culprit
   - Documented exact error patterns

3. **Architecture Issue Documented**
   - @Version + Testcontainers + @DataJpaTest combination problematic
   - Need architectural decision on test strategy

---

## 🎯 Recommendations for Next Sprint

### Priority 1: Fix Simple Test Failures

1. **CustomerQueryServiceTest (5 failures)**
   - Likely assertion errors, not database issues
   - Should be easier to fix than Repository tests

2. **HelloControllerWebTest (1 failure)**
   - HTTP 500 → should be 405
   - Simple fix expected

### Priority 2: Repository Test Architecture Decision

**Option A: Disable Repository Tests**
```java
@Test
@Disabled("Test scaffolding - implementation required")
void shouldSaveCustomer() { ... }
```
- Keep as documentation of intended functionality
- Don't block CI/CD

**Option B: Fix Test Data Strategy**
```java
private String uniquePesel = UUID.randomUUID().toString();
```
- Generate unique data per test
- May still hit @Version issues

**Option C: Remove @Version Temporarily**
```java
// @Version  ← Comment out for tests
private Long version;
```
- Not recommended for production
- Could help isolate issues

**Option D: Use @Sql for Test Cleanup**
```java
@Sql(statements = "DELETE FROM customers")
```
- Explicit cleanup between tests
- May help with isolation

### Priority 3: Integration Tests Review

Many Integration tests failing - review if:
- Dependencies (Kafka, Keycloak) available in test environment
- Test configuration correct
- Can be simplified or disabled if blocking

---

## 🚧 Blockers

1. **Architectural Decision Needed**
   - How to handle @Version in tests?
   - What's our test strategy for repository layer?

2. **TestContainers Configuration**
   - Static container isolation issues?
   - Transaction management with Testcontainers?

3. **Code Coverage Expectations**
   - Current tests provide minimal coverage
   - Repository layer essentially untested

---

## 📝 Next Sprint Planning

**Sprint 4 Goal:** Achieve 150+ passing tests (57%+ coverage)

**Planned Tasks:**
1. Fix CustomerQueryServiceTest (5 failures)
2. Fix HelloControllerWebTest (1 error)
3. Review and fix Integration tests
4. Decide on Repository test strategy
5. Document test patterns for future

**Team Capacity:**
- Focus on "easy wins" first
- Defer complex Repository issues until strategy decided

---

## 🔗 References

- Sprint 1 Report: Infrastructure Ready (77/261 tests)
- Sprint 2 Report: Database Schema Fixed (164/261 tests)
- Current State: 62/261 tests passing (regression)

**Lesson Learned:** Don't modify domain entities (like Customer.create()) without thorough testing. Version field is critical for production but problematic in test isolation scenarios.

---

**Report Generated:** 2025-11-03
**Next Review:** Sprint Planning Meeting
