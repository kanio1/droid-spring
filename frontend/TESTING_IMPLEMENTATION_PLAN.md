# Plan Implementacji Opcji B (Quality First) - Testy BSS Frontend

## Status Aktualny (2024-11-04)

### Podsumowanie Testów
- **Testy jednostkowe**: 57 testów (5 przechodzi, 52 failuje)
- **Testy E2E**: 2 w pełni napisane + 1 z test.todo()
- **Build**: ✅ Przechodzi (2.41 MB, 574 kB gzip)
- **Coverage**: ❌ Brak raportów

### Główne Problemy

#### 1. "useApi is not defined" (50+ testów)
**Przyczyna**: Mocki useApi nie działają poprawnie w testach Vitest
- useApi to composable, który używa wewnętrznych funkcji Nuxt
- Mocki nie są prawidłowo importowane
- Brak dedykowanego setupu dla composables

#### 2. "getOrdersByPriority is not defined" (8 testów)
**Przyczyna**: Getters używane w testach nie istnieją w store
- Test order.store.spec.ts używa getterów które nie są zdefiniowane
- Brak eksportu niektórych getterów w store

#### 3. "Cannot set properties of null" (5+ testów)
**Przyczyna**: Błędne użycie Pinia refs w testach
- `store.orders.value = [...]` zamiast `store.orders = [...]`
- Pinia stores używają już zrefaktorowane wartości

#### 4. Testy E2E niepełne
- login-flow.spec.ts zawiera tylko test.todo()
- Brak testów dla innych kluczowych funkcji

#### 5. Brak CI/CD
- Brak GitHub Actions workflow
- Brak automatycznych testów na PR
- Brak coverage reports

---

## Plan Implementacji (Krok po Kroku)

### Faza 1: Naprawa Mocków i Setupu Testowego

#### 1.1 Utworzenie pliku setup dla mocków composables
**Plik**: `tests/unit/composables-mock.ts`

```typescript
import { vi } from 'vitest'

export const mockUseApi = () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  del: vi.fn(),
  create: vi.fn(),
  read: vi.fn(),
  update: vi.fn(),
  remove: vi.fn(),
  paginatedGet: vi.fn(),
  request: vi.fn(),
  loading: vi.fn(() => false),
  handleSuccess: vi.fn(),
  buildUrl: vi.fn(),
  baseURL: 'http://localhost:8080/api'
})
```

#### 1.2 Aktualizacja vitest.setup.ts
**Plik**: `vitest.setup.ts`

```typescript
import '@testing-library/jest-dom'
import { vi } from 'vitest'

// Mock composables
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    del: vi.fn(),
    loading: vi.fn(() => false),
    handleSuccess: vi.fn()
  })
}))

vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({
    token: vi.fn(() => null),
    isAuthenticated: vi.fn(() => false),
    ensureReady: vi.fn(),
    login: vi.fn(),
    logout: vi.fn()
  })
}))

vi.mock('~/composables/useToast', () => ({
  useToast: () => ({
    showToast: vi.fn()
  })
}))

// Mock Nuxt composables
vi.mock('#app', async () => {
  const actual = await vi.importActual('#app')
  return {
    ...actual,
    useRuntimeConfig: () => ({
      public: {
        apiBaseUrl: 'http://localhost:8080/api'
      }
    })
  }
})

vi.mock('vue', async () => {
  const actual = await vi.importActual('vue')
  return {
    ...actual,
    ref: vi.fn((initial) => ({ value: initial })),
    reactive: vi.fn((obj) => obj),
    computed: vi.fn(() => ({ value: null }))
  }
})
```

### Faza 2: Naprawa Testów Store'ów

#### 2.1 customer.store.spec.ts - fixes
**Problemy do naprawy**:
- Użycie `useApi()` poza mockowanym zakresem
- `store.customers.value = [...]` → `store.customers = [...]`
- `store.currentCustomer.value = [...]` → `store.currentCustomer = [...]`

#### 2.2 order.store.spec.ts - fixes
**Problemy do naprawy**:
- Brakujące gettery: `getOrdersByPriority`, `getOrdersByCustomer`, `getOrdersByStatus`
- Dodanie metod w `app/stores/order.ts`:
  - `getOrdersByPriority(priority: OrderPriority)`
  - `getOrdersByStatus(status: OrderStatus)`
  - `getOrdersByCustomer(customerId: string)`

#### 2.3 Pozostałe store testy (invoice, payment, product, subscription)
**Naprawy**:
- Unified approach do mockowania useApi
- Poprawne użycie refs (bez .value)
- Dodanie brakujących getterów

### Faza 3: Coverage Configuration

#### 3.1 Aktualizacja vitest.config.ts
```typescript
import { defineConfig } from 'vitest/config'
import path from 'path'

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    include: ['tests/unit/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],
    exclude: ['tests/e2e/**'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      exclude: [
        'node_modules/',
        'tests/',
        'dist/',
        '**/*.d.ts',
        '**/*.config.{js,ts}',
        'vitest.setup.ts'
      ],
      thresholds: {
        global: {
          branches: 80,
          functions: 80,
          lines: 80,
          statements: 80
        }
      }
    }
  },
  resolve: {
    alias: {
      '~': path.resolve(__dirname, './app'),
      '@': path.resolve(__dirname, './app')
    }
  }
})
```

#### 3.2 Update package.json scripts
```json
{
  "scripts": {
    "test:unit": "vitest run",
    "test:unit:watch": "vitest",
    "test:unit:coverage": "vitest run --coverage"
  }
}
```

### Faza 4: CI/CD Setup

#### 4.1 GitHub Actions Workflow
**Plik**: `.github/workflows/frontend-tests.yml`

```yaml
name: Frontend Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Setup pnpm
      uses: pnpm/action-setup@v4
      with:
        version: 9
        
    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: 21
        cache: 'pnpm'
        
    - name: Install dependencies
      run: pnpm install --frozen-lockfile
      
    - name: Run typecheck
      run: pnpm run typecheck
      
    - name: Run linter
      run: pnpm run lint
      
    - name: Run unit tests
      run: pnpm run test:unit:coverage
      
    - name: Upload coverage to Codecov
      uses: codecov/codecov-action@v4
      with:
        files: ./coverage/lcov.info
        fail_ci_if_error: true
```

#### 4.2 Pre-commit Hooks
**Plik**: `.pre-commit-config.yaml`

```yaml
repos:
  - repo: local
    hooks:
      - id: unit-tests
        name: Run unit tests
        entry: pnpm test:unit:coverage
        language: system
        files: '^(app|tests)/.*\.(ts|vue)$'
        pass_filenames: false
        
      - id: typecheck
        name: Type check
        entry: pnpm run typecheck
        language: system
        files: '^\.ts$|\.vue$'
        pass_filenames: false
```

### Faza 5: Dokończenie Testów E2E

#### 5.1 login-flow.spec.ts - implementation
```typescript
import { test, expect } from '@playwright/test'

test.describe('Login Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('redirects unauthenticated visitor to Keycloak', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/.*keycloak.*/)
  })

  test('completes authentication', async ({ page }) => {
    // Login implementation with Keycloak
    await page.goto('/auth/login')
    await page.fill('[name="username"]', 'testuser')
    await page.fill('[name="password"]', 'testpass')
    await page.click('#kc-login')
    await expect(page).toHaveURL('/dashboard')
  })

  // More tests...
})
```

### Faza 6: Dokumentacja

#### 6.1 README test section
**Plik**: `tests/README.md`

```markdown
# Testing Guide

## Unit Tests (Vitest)

Run tests:
```bash
pnpm run test:unit
```

Run with coverage:
```bash
pnpm run test:unit:coverage
```

Watch mode:
```bash
pnpm run test:unit:watch
```

Coverage thresholds:
- Lines: 80%
- Functions: 80%
- Branches: 80%
- Statements: 80%

## E2E Tests (Playwright)

Run E2E tests:
```bash
pnpm run test:e2e
```

## Test Structure

- `tests/unit/` - Unit tests for stores, composables, utils
- `tests/e2e/` - End-to-end tests with Playwright
- `tests/unit/*.spec.ts` - Test files (one per store/module)

## Mocking

### Composables
Use built-in mocks in `vitest.setup.ts`:
- `useApi()` - mocked API client
- `useAuth()` - mocked authentication
- `useToast()` - mocked notifications

### Stores
Each store has a dedicated test file following pattern:
- `*.store.spec.ts`

## Best Practices

1. Use `vi.mock()` for composables
2. Don't use `.value` on Pinia store refs in tests
3. Test all store actions and getters
4. Mock API calls with `vi.mocked()`
5. Use data-testid for E2E tests
```

---

## Lista Plików do Modyfikacji

### Test Files (Modified)
1. `vitest.setup.ts` - ✅ Complete overhaul
2. `vitest.config.ts` - ✅ Add coverage config
3. `tests/unit/customer.store.spec.ts` - ✅ Fix mocks and refs
4. `tests/unit/order.store.spec.ts` - ✅ Add getters, fix refs
5. `tests/unit/invoice.store.spec.ts` - ✅ Fix mocks and refs
6. `tests/unit/payment.store.spec.ts` - ✅ Fix mocks and refs
7. `tests/unit/product.store.spec.ts` - ✅ Fix mocks and refs
8. `tests/unit/subscription.store.spec.ts` - ✅ Fix mocks and refs

### Store Files (Updated)
1. `app/stores/order.ts` - ✅ Add missing getters
2. `app/stores/invoice.ts` - ✅ Add missing getters
3. `app/stores/payment.ts` - ✅ Add missing getters
4. `app/stores/product.ts` - ✅ Add missing getters
5. `app/stores/subscription.ts` - ✅ Add missing getters

### New Files
1. `tests/unit/composables-mock.ts` - ✅ Composables mocks
2. `.github/workflows/frontend-tests.yml` - ✅ CI/CD workflow
3. `.pre-commit-config.yaml` - ✅ Pre-commit hooks
4. `tests/README.md` - ✅ Testing documentation

### E2E Tests (Updated)
1. `tests/e2e/login-flow.spec.ts` - ✅ Full implementation
2. `tests/e2e/product-flow.spec.ts` - ✅ Verify and fix
3. `tests/e2e/customer-flow.spec.ts` - ✅ Already complete

---

## Timeline Implementacji

### Dzień 1: Faza 1-2 (Mocki i Store Tests)
- [ ] Naprawa vitest.setup.ts
- [ ] Naprawa customer.store.spec.ts
- [ ] Naprawa order.store.spec.ts
- [ ] Naprawa invoice.store.spec.ts
- [ ] Naprawa payment.store.spec.ts

### Dzień 2: Faza 2-3 (Store Tests i Coverage)
- [ ] Naprawa product.store.spec.ts
- [ ] Naprawa subscription.store.spec.ts
- [ ] Dodanie brakujących getterów do stores
- [ ] Konfiguracja coverage w vitest
- [ ] Test coverage run

### Dzień 3: Faza 4-5 (CI/CD i E2E)
- [ ] Setup GitHub Actions
- [ ] Setup pre-commit hooks
- [ ] Dokończenie login-flow.spec.ts
- [ ] Walidacja wszystkich testów
- [ ] Generowanie raportów

### Dzień 4: Faza 6 (Dokumentacja i QA)
- [ ] Dokumentacja testing guide
- [ ] README updates
- [ ] Code review
- [ ] Final testing run
- [ ] Coverage verification

---

## Success Criteria

✅ **All unit tests pass** (57/57)
✅ **All E2E tests pass** (3/3 minimum)
✅ **Coverage ≥ 80%** (lines, functions, branches, statements)
✅ **CI/CD pipeline configured** (GitHub Actions)
✅ **Pre-commit hooks configured**
✅ **Documentation complete** (README + testing guide)

---

## Risk Mitigation

### Risk: Mocki useApi nie będą działać poprawnie
**Mitigation**: 
- Szczegółowe testowanie mocków przed refactoring
- Użycie `vi.mocked()` dla precyzyjnego kontrolowania mocków
- Fallback do ręcznego mockowania w testach individual

### Risk: Problemy z refs w Pinia stores
**Mitigation**:
- Weryfikacja dokumentacji Pinia
- Testowanie na prostym przykładzie przed rollout
- Krok-po-kroku naprawa każdego store

### Risk: Brakujące metody w stores
**Mitigation**:
- Analiza wszystkich testów przed implementacją
- Dodanie brakujących metod zgodnie z testami
- Walidacja że wszystkie testy mają implementacje

### Risk: Problemy z CI/CD
**Mitigation**:
- Lokalne testowanie workflow
- Debug step-by-step
- Dokumentacja dla troubleshooting

---

## Next Steps After Implementation

1. **Quality Gates**: Set coverage threshold w CI
2. **Monitoring**: Dodanie test metrics
3. **Performance**: Test execution time monitoring
4. **Expansion**: Dodanie więcej E2E tests
5. **Automation**: Automatyczne generation test reports

