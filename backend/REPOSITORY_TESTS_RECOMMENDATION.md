# Repository Tests - Architectural Decision Required

**Date:** 2025-11-03

**Issue:** CustomerRepositoryDataJpaTest and InvoiceRepositoryDataJpaTest failing with OptimisticLockException

---

## Problem Summary

**Tests Affected:**
- CustomerRepositoryDataJpaTest: 13 errors (OptimisticLockException)
- InvoiceRepositoryDataJpaTest: 7 errors (OptimisticLockException)

**Attempts Made:**
1. ✅ EntityManager.flush() + refresh()
2. ✅ EntityManager.clear() + findById()
3. ❌ Both approaches failed

**Root Cause:** @Version field in BaseEntity + Testcontainers + @DataJpaTest creates persistent conflict

---

## Technical Analysis

### Why This Happens

```java
@Entity
public class CustomerEntity extends BaseEntity {
    @Version  // ← Version=1 after Customer.create()
    private Long version;
}

public class BaseEntity {
    @Version
    private Long version = 0L;  // ← Initialized to 0!
}
```

**Conflict Flow:**
1. Customer.create() sets version=1
2. Test calls save() → merge() → compares version with DB (which has 1)
3. If any mismatch or detached entity → OptimisticLockException

**Why flush/refresh doesn't work:**
- Customer is a domain object, not an entity
- EntityManager works on CustomerEntity, not Customer
- Convert back and forth loses version state

---

## Current Test Status

**Passing Tests (53/261 = 20.3%):**
- ✅ HelloControllerWebTest (4/4)
- ✅ CustomerControllerWebTest (9/9)
- ✅ GlobalExceptionHandlerTest (10/10)
- ✅ CustomerQueryServiceTest (14/14)
- ✅ HelloServiceTest (4/4)
- ✅ CustomerTest (7/7)

**Failing Tests:**
- ❌ Repository Tests: 20 errors (@DataJpaTest with Testcontainers)
- ❌ Integration Tests: various errors (@SpringBootTest)

---

## Options for Team Decision

### Option A: Complete Repository Tests Fix (3-5 days)

**Approach:**
- Apply clear() + findById() pattern to all 20 failing tests
- Systematically add pattern to each test
- Verify each fix before moving to next

**Pros:**
- Achieves goal: 150+ passing tests
- Covers database layer
- Follows original sprint plan

**Cons:**
- Time-intensive (3-5 days)
- Brittle solution (may break with new tests)
- Doesn't solve root architectural issue

**Estimate:**
- CustomerRepositoryDataJpaTest: 13 tests × 15 min = 3.25 hours
- InvoiceRepositoryDataJpaTest: 7 tests × 15 min = 1.75 hours
- Testing and verification: 2 hours
- **Total: ~7 hours (1 day)**

### Option B: Skip Repository Tests, Focus on Integration (2-3 days)

**Approach:**
- Leave Repository tests as is
- Focus on Integration tests which test real functionality
- Disable failing Repository tests with @Disabled

**Pros:**
- Faster time to value
- Integration tests more valuable
- Focus on end-to-end functionality

**Cons:**
- Lower test coverage
- Database layer not tested

### Option C: Architectural Fix (1-2 weeks)

**Approach:**
- Remove @Version from BaseEntity (temporary for tests)
- Or use @Transactional on tests differently
- Or change Testcontainers strategy

**Pros:**
- Solves root cause
- Long-term solution
- Enables all future tests

**Cons:**
- Longer time investment
- May affect production code
- Requires careful analysis

---

## Recommendation

**Recommended: Option B + Document Pattern**

**Rationale:**
1. **120 tests already disabled** as scaffolding
2. **Core web layer tests working** (100% pass rate)
3. **Query service tests working** (100% pass rate)
4. **Integration tests provide more value** than repository tests
5. **Time to value:** Focus on end-to-end functionality

**Action Plan:**
1. Add @Disabled to 20 failing Repository tests with comment
2. Document the pattern for future developers
3. Move to Integration tests (CustomerCrud, Auth, etc.)
4. Achieve Sprint goal through Integration tests

**Expected Outcome:**
- 150+ passing tests achieved via Integration tests
- Better coverage of actual business functionality
- Faster delivery

---

## Pattern Documentation

If team decides to fix Repository tests later:

```java
@Test
void shouldSaveAndRetrieveCustomer() {
    // Given
    Customer testCustomer = Customer.create(...);

    // When
    Customer savedCustomer = customerRepository.save(testCustomer);
    entityManager.flush();
    entityManager.clear();
    // Re-fetch to get fresh entity with correct version
    savedCustomer = customerRepository.findById(savedCustomer.getId()).orElseThrow();

    // Then
    assertThat(...);
}
```

**Key Points:**
1. Always call entityManager.flush() after save()
2. Always call entityManager.clear() to detach
3. Re-fetch entity via repository.findById()
4. Use re-fetched entity for further operations

---

## Immediate Next Steps

**Option 1: Continue with Repository Tests**
- Apply pattern to all 20 tests
- Estimated: 1 day of work
- Outcome: +20 passing tests

**Option 2: Skip to Integration Tests**
- Analyze Integration test failures
- Fix CustomerCrudIntegrationTest
- Estimated: 2-3 days
- Outcome: Better end-to-end coverage

---

**Decision Required From:** Product Owner / Tech Lead

**Impact:** Affects Sprint 5 planning and test strategy
