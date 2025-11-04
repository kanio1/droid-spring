# Lista Plików do Modyfikacji - Testing Implementation

## 📝 Pliki do Edycji (Existing Files)

### 1. Konfiguracja Testów (Priority: HIGH)

#### `/home/labadmin/projects/droid-spring/frontend/vitest.setup.ts`
**Status**: ⚠️ Must be replaced  
**Zmiany**: 
- Usunięcie starej zawartości (tylko `@testing-library/jest-dom`)
- Dodanie kompletnych mocków dla `useApi`, `useAuth`, `useToast`
- Dodanie mocków dla `#app`, `vue-router`, `vue`
- Dodanie `beforeEach` i `afterEach` hooks

#### `/home/labadmin/projects/droid-spring/frontend/vitest.config.ts`
**Status**: ⚠️ Must be updated  
**Zmiany**:
- Dodanie sekcji `coverage` z konfiguracją V8
- Ustawienie threshold na 80%
- Dodanie reporterów (text, json, html, lcov)
- Konfiguracja exclude patterns

#### `/home/labadmin/projects/droid-spring/frontend/package.json`
**Status**: ⚠️ Must be updated  
**Zmiany**:
- Dodanie skryptów: `test:unit:coverage`, `test:unit:watch`
- Aktualizacja skryptów testowych jeśli potrzeba

---

### 2. Testy Jednostkowe (Priority: HIGH)

#### `/home/labadmin/projects/droid-spring/frontend/tests/unit/customer.store.spec.ts`
**Status**: ⚠️ Must be fixed  
**Problemy do naprawy**:
- ❌ Mocki `useApi` nie działają
- ❌ Użycie `store.customers.value` (powinno być bez `.value`)
- ❌ Użycie `store.currentCustomer.value` (powinno być bez `.value`)
**Zmiany**:
- Usunięcie lokalnego mocka `useApi` (przeniesiony do vitest.setup.ts)
- Zmiana wszystkich `*.value` na bezpośredni dostęp
- Użycie `vi.mocked()` dla mocked methods

#### `/home/labadmin/projects/droid-spring/frontend/tests/unit/order.store.spec.ts`
**Status**: ⚠️ Must be fixed  
**Problemy do naprawy**:
- ❌ Błąd "getOrdersByPriority is not defined"
- ❌ Mocki `useApi` nie działają
- ❌ Użycie `store.orders.value`
**Zmiany**:
- Usunięcie lokalnego mocka `useApi`
- Naprawa refs (bez `.value`)
- Metody zostaną dodane w store/order.ts

#### `/home/labadmin/projects/droid-spring/frontend/tests/unit/invoice.store.spec.ts`
**Status**: ⚠️ Must be fixed  
**Problemy**: 
- Mocki `useApi` nie działają
- Użycie `.value` refs
**Zmiany**:
- Usunięcie lokalnego mocka `useApi`
- Naprawa refs

#### `/home/labadmin/projects/droid-spring/frontend/tests/unit/payment.store.spec.ts`
**Status**: ⚠️ Must be fixed  
**Problemy**:
- Mocki `useApi` nie działają
- Użycie `.value` refs
**Zmiany**:
- Usunięcie lokalnego mocka `useApi`
- Naprawa refs

#### `/home/labadmin/projects/droid-spring/frontend/tests/unit/product.store.spec.ts`
**Status**: ⚠️ Must be fixed  
**Problemy**:
- Mocki `useApi` nie działają
- Użycie `.value` refs
**Zmiany**:
- Usunięcie lokalnego mocka `useApi`
- Naprawa refs

#### `/home/labadmin/projects/droid-spring/frontend/tests/unit/subscription.store.spec.ts`
**Status**: ⚠️ Must be fixed  
**Problemy**:
- Mocki `useApi` nie działają
- Użycie `.value` refs
**Zmiany**:
- Usunięcie lokalnego mocka `useApi`
- Naprawa refs

#### `/home/labadmin/projects/droid-spring/frontend/tests/unit/hello.spec.ts`
**Status**: ✅ OK (sprawdzić czy działa)

---

### 3. Store Files (Priority: MEDIUM)

#### `/home/labadmin/projects/droid-spring/frontend/app/stores/order.ts`
**Status**: ⚠️ Must be updated  
**Brakujące elementy**:
- Metoda `getOrdersByPriority(priority: OrderPriority)`
- Metoda `getOrdersByStatus(status: OrderStatus)`
- Metoda `getOrdersByCustomer(customerId: string)`
**Zmiany**:
- Dodanie powyższych metod
- Eksport w return statement

#### `/home/labadmin/projects/droid-spring/frontend/app/stores/invoice.ts`
**Status**: ⚠️ Check if getters missing  
**Akcja**: Sprawdzić czy istnieją wszystkie metody używane w testach

#### `/home/labadmin/projects/droid-spring/frontend/app/stores/payment.ts`
**Status**: ⚠️ Check if getters missing  
**Akcja**: Sprawdzić czy istnieją wszystkie metody używane w testach

#### `/home/labadmin/projects/droid-spring/frontend/app/stores/product.ts`
**Status**: ⚠️ Check if getters missing  
**Akcja**: Sprawdzić czy istnieją wszystkie metody używane w testach

#### `/home/labadmin/projects/droid-spring/frontend/app/stores/subscription.ts`
**Status**: ⚠️ Check if getters missing  
**Akcja**: Sprawdzić czy istnieją wszystkie metody używane w testach

---

### 4. Testy E2E (Priority: LOW - już napisane, tylko weryfikacja)

#### `/home/labadmin/projects/droid-spring/frontend/tests/e2e/login-flow.spec.ts`
**Status**: ⚠️ Must be implemented  
**Obecny stan**: Zawiera tylko `test.todo()`  
**Wymagane**: Pełna implementacja 5 testów logowania

#### `/home/labadmin/projects/droid-spring/frontend/tests/e2e/customer-flow.spec.ts`
**Status**: ✅ Already complete  
**Akcja**: Sprawdzić czy wszystkie selektory są prawidłowe

#### `/home/labadmin/projects/droid-spring/frontend/tests/e2e/product-flow.spec.ts`
**Status**: ✅ Already complete  
**Akcja**: Sprawdzić czy wszystkie selektory są prawidłowe

#### `/home/labadmin/projects/droid-spring/frontend/playwright.config.ts`
**Status**: ✅ Already configured  
**Akcja**: Sprawdzić czy URL jest prawidłowy

---

## 🆕 Pliki do Utworzenia (New Files)

### 1. Mock Helpers (Priority: HIGH)

#### `/home/labadmin/projects/droid-spring/frontend/tests/unit/composables-mock.ts`
**Cel**: Centralizacja mocków composables  
**Zawartość**:
- `mockUseApi()` - factory function
- `mockUseAuth()` - factory function
- `mockUseToast()` - factory function

---

### 2. CI/CD Configuration (Priority: HIGH)

#### `/home/labadmin/projects/droid-spring/frontend/.github/workflows/frontend-tests.yml`
**Cel**: Automatyczne testy w GitHub Actions  
**Zawartość**:
- Job: lint-and-typecheck
- Job: unit-tests (z coverage)
- Job: e2e-tests (z Playwright)
- Job: test-summary

#### `/home/labadmin/projects/droid-spring/frontend/.pre-commit-config.yaml`
**Cel**: Pre-commit hooks  
**Zawartość**:
- Hook: trailing-whitespace
- Hook: check-yaml
- Hook: typecheck
- Hook: eslint
- Hook: unit-tests
- Hook: e2e-tests (opcjonalnie)

---

### 3. Documentation (Priority: MEDIUM)

#### `/home/labadmin/projects/droid-spring/frontend/tests/README.md`
**Cel**: Dokumentacja testów  
**Zawartość**:
- Przewodnik testowania jednostkowego
- Przewodnik E2E
- Instrukcje mockowania
- Przykłady
- Best practices
- Troubleshooting

---

### 4. Summary Files (Already Created)

✅ `/home/labadmin/projects/droid-spring/frontend/TESTING_IMPLEMENTATION_PLAN.md` - Complete implementation plan  
✅ `/home/labadmin/projects/droid-spring/frontend/TESTING_CHECKLIST.md` - Step-by-step checklist  
✅ `/home/labadmin/projects/droid-spring/frontend/IMPLEMENTATION_EXAMPLES.md` - Code examples  
✅ `/home/labadmin/projects/droid-spring/frontend/FILES_TO_MODIFY.md` - This file  

---

## 📊 Podsumowanie Zmian

### Liczba Plików
- **Pliki do edycji**: 15+ files
- **Nowe pliki**: 4 files
- **Całkowita liczba plików**: ~20 files

### Szacowany Czas Implementacji
- **Faza 1 (Mocki i Setup)**: 4-6 hours
- **Faza 2 (Store Tests)**: 8-12 hours
- **Faza 3 (Coverage)**: 2-3 hours
- **Faza 4 (CI/CD)**: 4-6 hours
- **Faza 5 (E2E)**: 6-8 hours
- **Faza 6 (Dokumentacja)**: 2-3 hours
- **Total**: 26-38 hours (3-5 dni roboczych)

### Priorytet Implementacji
1. 🔥 **HIGH**: vitest.setup.ts, customer.store.spec.ts, order.store.spec.ts
2. ⚡ **HIGH**: vitest.config.ts (coverage), package.json scripts
3. 📋 **MEDIUM**: Pozostałe store tests, store files updates
4. 🔧 **LOW**: E2E tests (już napisane)
5. 📚 **LOW**: Documentation

---

## ✅ Checklist Przed Implementacją

### Obowiązkowe Przygotowania
- [ ] Backup wszystkich zmienianych plików
- [ ] Node.js 21+ zainstalowany
- [ ] pnpm 9+ zainstalowany
- [ ] Dostęp do repozytorium Git

### Tools do Instalacji
- [ ] vitest (już zainstalowany)
- [ ] @testing-library/jest-dom (już zainstalowany)
- [ ] @playwright/test (już zainstalowany)
- [ ] playwright (potrzebny install: `npx playwright install`)

### Git Branches
- [ ] Stworzenie branch: `feature/testing-fix`
- [ ] Regular commits podczas implementacji
- [ ] PR z pełną implementacją

---

## 🚀 Quick Start (Po Zatwierdzeniu Planu)

### 1. Start z Mockami
```bash
cd /home/labadmin/projects/droid-spring/frontend
cp vitest.setup.ts vitest.setup.ts.backup
# Edytuj vitest.setup.ts z nowymi mockami
pnpm run test:unit -- customer
```

### 2. Test Individual Store
```bash
pnpm run test:unit -- customer.store
# Sprawdź czy mocki działają
```

### 3. Dodaj Coverage
```bash
# Edytuj vitest.config.ts
pnpm run test:unit:coverage
open coverage/index.html
```

### 4. Setup CI/CD
```bash
mkdir -p .github/workflows
# Dodaj frontend-tests.yml
git add .github/workflows/frontend-tests.yml
git commit -m "feat: add GitHub Actions for tests"
```

### 5. Pre-commit Hooks
```bash
pip install pre-commit
# Dodaj .pre-commit-config.yaml
pre-commit install
```

---

## 🎯 Success Metrics

### Po Implementacji
- ✅ Wszystkie 57 testów przechodzi
- ✅ Coverage ≥ 80%
- ✅ CI/CD workflow działa
- ✅ Pre-commit hooks działają
- ✅ E2E tests przechodzą
- ✅ Dokumentacja kompletna

### Validation Commands
```bash
# Test all unit tests
pnpm run test:unit

# Generate and check coverage
pnpm run test:unit:coverage

# Check threshold
cat coverage/coverage-summary.json

# Run E2E
pnpm run test:e2e

# Build check
pnpm run build

# Typecheck
pnpm run typecheck

# Linter
pnpm run lint
```

