# Testing Checklist - BSS Frontend

## ✅ Pre-Implementation Checklist

### Environment Setup
- [ ] Node.js 21+ installed
- [ ] pnpm 9+ installed
- [ ] Project dependencies installed (`pnpm install`)
- [ ] TypeScript compilation works (`pnpm run typecheck`)
- [ ] Build works (`pnpm run build`)
- [ ] Linter passes (`pnpm run lint`)

### Testing Tools Installation
- [ ] Vitest configured
- [ ] Playwright configured
- [ ] Testing Library installed
- [ ] JSDOM environment configured

---

## ✅ Implementation Checklist

### Phase 1: Mock Setup (Day 1)
- [ ] **vitest.setup.ts** - Complete rewrite with mocks
  - [ ] Mock `useApi` composable
  - [ ] Mock `useAuth` composable
  - [ ] Mock `useToast` composable
  - [ ] Mock `#app` Nuxt composable
  - [ ] Mock `vue` reactive utilities
- [ ] **tests/unit/composables-mock.ts** - Create helper
- [ ] Verify mock imports work in all tests

### Phase 2: Store Tests (Days 1-2)

#### customer.store.spec.ts
- [ ] Fix mock imports
- [ ] Replace `.value` references with direct access
- [ ] Test all CRUD operations
- [ ] Test filtering and search
- [ ] Test pagination
- [ ] Test error handling
- [ ] Test reset functionality

#### order.store.spec.ts
- [ ] Add missing `getOrdersByPriority` getter
- [ ] Add missing `getOrdersByStatus` getter
- [ ] Add missing `getOrdersByCustomer` getter
- [ ] Fix mock imports
- [ ] Replace `.value` references
- [ ] Test all computed getters
- [ ] Test order filtering
- [ ] Test status updates

#### invoice.store.spec.ts
- [ ] Add missing getters (if any)
- [ ] Fix mock imports
- [ ] Replace `.value` references
- [ ] Test invoice operations
- [ ] Test filtering and calculations
- [ ] Test pagination

#### payment.store.spec.ts
- [ ] Add missing getters (if any)
- [ ] Fix mock imports
- [ ] Replace `.value` references
- [ ] Test payment operations
- [ ] Test status changes
- [ ] Test calculations

#### product.store.spec.ts
- [ ] Add missing getters (if any)
- [ ] Fix mock imports
- [ ] Replace `.value` references
- [ ] Test product CRUD
- [ ] Test filtering
- [ ] Test status management

#### subscription.store.spec.ts
- [ ] Add missing getters (if any)
- [ ] Fix mock imports
- [ ] Replace `.value` references
- [ ] Test subscription lifecycle
- [ ] Test billing operations

#### hello.spec.ts
- [ ] Verify basic component test works

### Phase 3: Coverage Configuration (Day 2)
- [ ] **vitest.config.ts** - Add coverage section
  - [ ] Configure provider: 'v8'
  - [ ] Add reporters: text, json, html, lcov
  - [ ] Set thresholds to 80%
  - [ ] Configure excludes
- [ ] **package.json** - Add coverage scripts
- [ ] Run coverage test locally
- [ ] Verify coverage report generation
- [ ] Check coverage thresholds
- [ ] Open HTML coverage report

### Phase 4: CI/CD Setup (Day 3)
- [ ] **.github/workflows/frontend-tests.yml** - Create
  - [ ] Configure Node.js 21
  - [ ] Configure pnpm cache
  - [ ] Add install step
  - [ ] Add typecheck step
  - [ ] Add linter step
  - [ ] Add test step
  - [ ] Add coverage upload
- [ ] Test workflow locally (if possible)
- [ ] Verify workflow triggers on PR
- [ ] Verify workflow runs on push
- [ ] **.pre-commit-config.yaml** - Create
  - [ ] Add unit tests hook
  - [ ] Add typecheck hook
  - [ ] Add linter hook
- [ ] Test pre-commit hooks
- [ ] Install pre-commit hooks locally

### Phase 5: E2E Tests (Day 3)
- [ ] **tests/e2e/login-flow.spec.ts** - Implement
  - [ ] Test unauthenticated redirect
  - [ ] Test login process
  - [ ] Test token refresh
  - [ ] Test session recovery
  - [ ] Test logout
- [ ] **tests/e2e/product-flow.spec.ts** - Review
  - [ ] Verify all test cases
  - [ ] Fix any issues
  - [ ] Add missing scenarios
- [ ] **tests/e2e/customer-flow.spec.ts** - Review
  - [ ] Verify all test cases
  - [ ] Fix any issues
  - [ ] Add missing scenarios
- [ ] Run E2E tests locally
- [ ] Fix any failing tests

### Phase 6: Documentation (Day 4)
- [ ] **tests/README.md** - Create
  - [ ] Unit testing guide
  - [ ] E2E testing guide
  - [ ] Mocking guide
  - [ ] Best practices
  - [ ] Troubleshooting
- [ ] **Frontend README.md** - Update testing section
- [ ] Add badges for test status
- [ ] Add coverage badge
- [ ] Document CI/CD pipeline
- [ ] Document pre-commit hooks

---

## ✅ Post-Implementation Verification

### Unit Tests
- [ ] All 57 tests pass
- [ ] No skipped tests (except intentional)
- [ ] Test execution time acceptable (< 30s)
- [ ] Coverage ≥ 80% for all metrics
- [ ] No console warnings/errors

### E2E Tests
- [ ] All E2E tests pass
- [ ] Login flow works
- [ ] Customer flow works
- [ ] Product flow works
- [ ] Test execution time acceptable
- [ ] Screenshots generated on failure

### CI/CD
- [ ] GitHub Actions workflow runs
- [ ] All steps pass
- [ ] Coverage reported to Codecov
- [ ] Workflow status checks work
- [ ] PR checks enforce tests

### Pre-commit
- [ ] Hooks installed locally
- [ ] Tests run before commit
- [ ] Typecheck runs before commit
- [ ] Linter runs before commit
- [ ] Commit fails on test/coverage issues

---

## 🚨 Common Issues & Solutions

### Issue: "useApi is not defined"
**Solution**: 
- Check mock in vitest.setup.ts
- Ensure mock path matches import path
- Use `vi.mocked()` for method calls

### Issue: "Cannot set properties of null"
**Solution**:
- Don't use `.value` on Pinia stores
- Use `store.customers = [...]` not `store.customers.value = [...]`

### Issue: "getOrdersByPriority is not defined"
**Solution**:
- Add missing getter to store
- Export getter in return statement
- Check method signature matches test

### Issue: Coverage too low
**Solution**:
- Add tests for uncovered lines
- Mark non-testable code with `/* istanbul ignore */`
- Check coverage report for specific files

### Issue: E2E tests timeout
**Solution**:
- Increase timeout value
- Use `waitForSelector` before actions
- Add `page.waitForTimeout()` for loading

### Issue: CI/CD fails
**Solution**:
- Check Node.js version compatibility
- Verify pnpm cache configuration
- Check for environment variables
- Review test output logs

---

## 📊 Success Metrics

### Test Coverage
- [ ] Lines: ≥ 80%
- [ ] Functions: ≥ 80%
- [ ] Branches: ≥ 80%
- [ ] Statements: ≥ 80%

### Test Execution
- [ ] Unit tests: < 30s
- [ ] E2E tests: < 120s
- [ ] Total CI time: < 5min

### Code Quality
- [ ] No TypeScript errors
- [ ] No ESLint warnings
- [ ] No console.log in production
- [ ] All tests documented

---

## 🎯 Final Verification

### Before Marking Complete
1. Run all tests: `pnpm run test:unit`
2. Generate coverage: `pnpm run test:unit:coverage`
3. Run E2E tests: `pnpm run test:e2e`
4. Check build: `pnpm run build`
5. Run typecheck: `pnpm run typecheck`
6. Run linter: `pnpm run lint`
7. Verify CI/CD workflow runs
8. Verify pre-commit hooks work
9. Check documentation completeness
10. Code review completed

### Documentation Checklist
- [ ] Testing guide created
- [ ] CI/CD documented
- [ ] Pre-commit hooks documented
- [ ] Mocking strategy documented
- [ ] Troubleshooting guide added
- [ ] Examples provided
- [ ] Badges added to README

### Team Checklist
- [ ] Team notified of testing setup
- [ ] Testing guidelines shared
- [ ] CI/CD workflow documented
- [ ] Review process defined
- [ ] Maintenance plan created

---

**Estimated Timeline**: 4 days
**Priority**: High
**Risk Level**: Medium
**Dependencies**: None (can implement standalone)
