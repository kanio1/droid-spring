# Sprint Task Board - Critical Issues Resolution

**Sprint:** 2025-W44 (Week 44)
**Date Created:** 2025-10-29
**Scrum Master:** Claude Code Agent
**Tech Lead Review Date:** 2025-10-29
**Review Grade:** A- (85/100)

---

## 🎯 Sprint Goal

**Fix critical architecture and data model issues to achieve A+ grade (90+/100)**

---

## 📋 Task Board

### **PRIORITY 1 - Architecture Violation**

#### **Task 1.1: Refactor ProductController to Use Use Cases**
**ID:** ARCH-101
**Priority:** P0 (Critical)
**Story Points:** 8
**Assignee:** Backend Implementation Agent
**Estimated Time:** 4-6 hours

**Description:**
ProductController currently bypasses Hexagonal architecture by directly using repositories. Must be refactored to follow the CustomerController pattern with Use Cases.

**Acceptance Criteria:**
- [ ] ProductController removed direct repository injection
- [ ] Create ProductUseCase interface following Customer pattern
- [ ] Create CreateProductUseCase, UpdateProductUseCase, DeleteProductUseCase implementations
- [ ] Update ProductController to use Use Cases
- [ ] Maintain all existing endpoints (9 total)
- [ ] Maintain all validation and OpenAPI documentation
- [ ] All tests pass

**Files to Modify:**
- `/backend/src/main/java/com/droid/bss/api/product/ProductController.java`
- New: `/backend/src/main/java/com/droid/bss/application/command/product/` (use case interfaces)
- New: `/backend/src/main/java/com/droid/bss/application/command/product/impl/` (implementations)

**Technical Notes:**
- Follow exact pattern from `CreateCustomerUseCase.java`
- Keep repository layer in `infrastructure/write/` as adapter
- Use Command pattern for mutations
- Maintain optimistic locking with @Version

---

#### **Task 1.2: Create Product Query Service**
**ID:** ARCH-102
**Priority:** P0 (Critical)
**Story Points:** 5
**Assignee:** Backend Implementation Agent
**Estimated Time:** 2-3 hours

**Description:**
Create query service for Product to complete CQRS separation following Customer pattern.

**Acceptance Criteria:**
- [ ] Create ProductQueryService interface
- [ ] Create ProductQueryServiceImpl
- [ ] Implement all query methods from ProductRepository
- [ ] Update ProductController to use ProductQueryService
- [ ] Separate command and query paths clearly

**Reference:**
- Follow `CustomerQueryService.java` pattern

---

### **PRIORITY 2 - BaseEntity Inconsistency**

#### **Task 2.1: Fix CustomerEntity BaseEntity Inheritance**
**ID:** DATA-201
**Priority:** P0 (Critical)
**Story Points:** 13
**Assignee:** Backend Implementation Agent
**Estimated Time:** 6-8 hours

**Description:**
CustomerEntity currently doesn't extend BaseEntity and has duplicate audit fields. Must be refactored to inherit from BaseEntity and remove duplicate fields.

**Acceptance Criteria:**
- [ ] CustomerEntity extends BaseEntity
- [ ] Remove duplicate `createdAt`, `updatedAt`, `version` fields
- [ ] Remove duplicate `createdBy`, `updatedBy` fields
- [ ] All existing functionality preserved
- [ ] All existing queries still work
- [ ] All tests pass
- [ ] Data migration created for existing records (if any)

**Files to Modify:**
- `/backend/src/main/java/com/droid/bss/domain/customer/CustomerEntity.java`
- `/backend/src/main/java/com/droid/bss/domain/customer/CustomerRepository.java`
- Any service classes using these fields
- New migration: `V999__fix_customer_entity_baseentity.sql`

**Technical Notes:**
- Audit fields will be auto-populated via JPA auditing
- Remove any manual audit field assignments
- Update equals/hashCode to use BaseEntity implementation
- Test with existing Customer data

---

#### **Task 2.2: Verify All Entities Extend BaseEntity**
**ID:** DATA-202
**Priority:** P1 (High)
**Story Points:** 3
**Assignee:** Backend Implementation Agent
**Estimated Time:** 1-2 hours

**Description:**
Audit all entities to ensure consistent BaseEntity inheritance.

**Acceptance Criteria:**
- [ ] Verify all 10 entities extend BaseEntity
- [ ] Document any exceptions with justification
- [ ] Ensure all audit fields are consistent
- [ ] Report any missing @Version annotations

**Entities to Verify:**
- BaseEntity (superclass) ✅
- CustomerEntity (needs fix)
- ProductEntity ✅
- ProductFeatureEntity ✅
- OrderEntity ✅
- OrderItemEntity ✅
- SubscriptionEntity ✅
- InvoiceEntity ✅
- InvoiceItemEntity ✅
- PaymentEntity ✅

---

### **PRIORITY 3 - Soft Delete Gap**

#### **Task 3.1: Add Soft Delete to PaymentEntity**
**ID:** DATA-301
**Priority:** P0 (Critical)
**Story Points:** 5
**Assignee:** Backend Implementation Agent
**Estimated Time:** 2-3 hours

**Description:**
PaymentEntity lacks soft delete support. Add @SQLRestriction and deleted_at field.

**Acceptance Criteria:**
- [ ] Add `@SQLRestriction("deleted_at IS NULL")` to PaymentEntity
- [ ] Add `deletedAt` field with @Column annotation
- [ ] Add soft delete business methods (markAsDeleted, isDeleted)
- [ ] Update repository queries to respect soft delete
- [ ] All tests pass

**Files to Modify:**
- `/backend/src/main/java/com/droid/bss/domain/payment/PaymentEntity.java`
- `/backend/src/main/java/com/droid/bss/domain/payment/repository/PaymentRepository.java`

**Reference:**
- Follow ProductEntity pattern with @SQLRestriction

---

#### **Task 3.2: Verify All Entities Support Soft Delete**
**ID:** DATA-302
**Priority:** P1 (High)
**Story Points:** 3
**Assignee:** Backend Implementation Agent
**Estimated Time:** 1-2 hours

**Description:**
Verify all business entities have soft delete support.

**Acceptance Criteria:**
- [ ] All 9 business entities have @SQLRestriction
- [ ] All have deletedAt field
- [ ] Consistent naming convention
- [ ] Document any entities without soft delete (with justification)

---

### **PRIORITY 4 - Enum Query Issues**

#### **Task 4.1: Fix Repository Enum Queries**
**ID:** REPO-401
**Priority:** P1 (High)
**Story Points:** 5
**Assignee:** Backend Implementation Agent
**Estimated Time:** 2-3 hours

**Description:**
Fix repository queries that use strings instead of enum constants.

**Acceptance Criteria:**
- [ ] Find all @Query methods using string comparisons for enums
- [ ] Replace string literals with enum constants
- [ ] Use proper JPQL enum syntax: `WHERE e.status = :status` (not string)
- [ ] All queries compile and work correctly
- [ ] Type-safe parameter binding

**Files to Review:**
- ProductRepository
- OrderRepository
- SubscriptionRepository
- InvoiceRepository
- PaymentRepository

**Example Fix:**
```java
// WRONG:
@Query("SELECT p FROM ProductEntity p WHERE p.status = 'ACTIVE'")

// CORRECT:
@Query("SELECT p FROM ProductEntity p WHERE p.status = com.droid.bss.domain.product.ProductStatus.ACTIVE")
// OR better with parameter:
@Query("SELECT p FROM ProductEntity p WHERE p.status = :status")
Page<ProductEntity> findByStatus(@Param("status") ProductStatus status)
```

---

#### **Task 4.2: Create Enum Conversion Utility**
**ID:** REPO-402
**Priority:** P2 (Medium)
**Story Points:** 3
**Assignee:** Backend Implementation Agent
**Estimated Time:** 1-2 hours

**Description:**
Create utility to safely convert between enums and their display names.

**Acceptance Criteria:**
- [ ] Create EnumUtils class
- [ ] Add methods for safe enum conversion
- [ ] Handle invalid enum values gracefully
- [ ] Add tests for enum utilities
- [ ] Document usage

---

## 📊 Sprint Summary

### **Total Tasks:** 8
### **Total Story Points:** 50
### **Estimated Effort:** 20-30 hours
### **Duration:** 1 sprint (2 weeks)

### **Story Points by Priority:**
- P0 (Critical): 34 points (7 tasks)
- P1 (High): 11 points (3 tasks)
- P2 (Medium): 3 points (1 task)
- P3 (Low): 2 points (1 task)

### **By Category:**
- Architecture: 13 points (2 tasks)
- Data Model: 21 points (4 tasks)
- Repositories: 8 points (2 tasks)

---

## 🎯 Definition of Done

For each task:
- [ ] Code implemented and reviewed
- [ ] All tests pass
- [ ] Code compiled successfully (`mvn clean compile`)
- [ ] OpenAPI docs updated (if applicable)
- [ ] PR created and merged
- [ ] Updated documentation

**Overall Sprint DoD:**
- [ ] All P0 tasks completed
- [ ] All P1 tasks completed
- [ ] Grade improved to A+ (90+/100)
- [ ] Architecture review passed
- [ ] No regression in existing functionality

---

## 🔗 Dependencies

**Task Dependencies:**
- Task 1.1 must complete before Task 1.2
- Task 2.1 must complete before Task 2.2
- Task 3.1 should complete before Task 3.2
- Task 4.1 can be done in parallel with other tasks

**External Dependencies:**
- None

---

## 📝 Notes

1. **CustomerController is the reference implementation** - follow it exactly
2. **BaseEntity provides audit fields** - use them, don't duplicate
3. **Soft delete is standard** - all business entities should support it
4. **Enums should be type-safe** - no string literals in queries
5. **Test after each task** - ensure no regressions

---

## 🚨 Risk Mitigation

**Risk:** Breaking existing Customer functionality
**Mitigation:** Test Customer module thoroughly after Task 2.1

**Risk:** Product API regression
**Mitigation:** Maintain all existing endpoints and behavior

**Risk:** Data integrity issues
**Mitigation:** Create migration scripts and test with sample data

**Risk:** Test failures
**Mitigation:** Update tests as needed, maintain coverage

---

## ✅ Approval

**Tech Lead Approval:** ________________ Date: __________
**Scrum Master Approval:** ________________ Date: __________
**Development Start:** ________________

---

**End of Task Board**
