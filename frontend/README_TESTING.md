# BSS Frontend - Testing Implementation Guide

## 📚 Dokumentacja

Ten katalog zawiera kompletną dokumentację implementacji systemu testowego dla BSS Frontend (Opcja B - Quality First).

---

## 📖 Spis Dokumentów

### 1. Executive Summary
**Plik**: `EXECUTIVE_SUMMARY.md`  
**Dla**: Managerów, Tech Leadów  
**Zawartość**:
- Podsumowanie statusu projektu
- Koszt vs Benefit
- Timeline implementacji
- Ryzyka i mitigacje
- Approval form

**Quick Start**: Przeczytaj ten dokument na początku dla zrozumienia całego planu.

---

### 2. Główny Plan Implementacji
**Plik**: `TESTING_IMPLEMENTATION_PLAN.md`  
**Dla**: Developerów, QA  
**Zawartość**:
- Szczegółowa analiza problemów
- 6-fazowy plan implementacji
- Konfiguracja mocków, testów, CI/CD
- Success criteria
- Timeline 4 dni

**Quick Start**: To jest główny dokument z instrukcjami krok po krok.

---

### 3. Praktyczna Checklist
**Plik**: `TESTING_CHECKLIST.md`  
**Dla**: Developerów (codzienna praca)  
**Zawartość**:
- Pre-implementation checklist
- Phase-by-phase implementation
- Post-implementation verification
- Common issues & solutions
- Success metrics

**Quick Start**: Użyj tej checklisty podczas implementacji - sprawdzaj kolejne punkty.

---

### 4. Przykłady Kodu
**Plik**: `IMPLEMENTATION_EXAMPLES.md`  
**Dla**: Developerów (reference)  
**Zawartość**:
- Gotowy kod do skopiowania
- vitest.setup.ts (complete)
- Przykłady testów store'ów
- GitHub Actions workflow
- Pre-commit hooks config
- E2E test examples

**Quick Start**: Kopiuj-klej kod z tego pliku zamiast pisania od zera.

---

### 5. Lista Plików
**Plik**: `FILES_TO_MODIFY.md`  
**Dla**: Developerów, Code Reviewerów  
**Zawartość**:
- Szczegółowa lista ~20 plików
- Status każdego pliku (must-fix, check, ok)
- Priorytety zmian
- Quick start commands
- Validation commands

**Quick Start**: Przed implementacją sprawdź ten dokument - wiesz co będziesz edytować.

---

### 6. Ten Dokument
**Plik**: `README_TESTING.md`  
**Dla**: Wszystkich  
**Zawartość**:
- Przegląd dokumentacji
- Szybki start dla każdej roli
- Command reference
- Troubleshooting

---

## 🚀 Quick Start Guide

### Dla Managerów
1. Przeczytaj: `EXECUTIVE_SUMMARY.md`
2. Zatwierdź: Timeline i budget (26-38h)
3. Assign: Developer do implementacji
4. Monitor: Progress według checklisty

### Dla Tech Leadów
1. Przeczytaj: `TESTING_IMPLEMENTATION_PLAN.md`
2. Review: `FILES_TO_MODIFY.md` - sprawdź co będzie zmienione
3. Support: Developer podczas implementacji
4. Code Review: Po każdej fazie

### Dla Developerów
1. Przeczytaj: `TESTING_IMPLEMENTATION_PLAN.md` (sekcje 1-3)
2. Używaj: `TESTING_CHECKLIST.md` - sprawdzaj punkty
3. Copy-paste: Z `IMPLEMENTATION_EXAMPLES.md`
4. Sprawdzaj: Komendy z `FILES_TO_MODIFY.md`

### Dla QA
1. Przeczytaj: `TESTING_IMPLEMENTATION_PLAN.md` (sekcja o testach E2E)
2. Review: `IMPLEMENTATION_EXAMPLES.md` (E2E tests)
3. Test: E2E tests po implementacji
4. Monitor: Coverage reports

---

## 📊 Command Reference

### Testing Commands
```bash
# Basic unit tests
pnpm run test:unit

# Unit tests with coverage
pnpm run test:unit:coverage

# Watch mode for development
pnpm run test:unit:watch

# Run specific test file
pnpm run test:unit -- customer

# Run E2E tests
pnpm run test:e2e

# E2E tests with UI
pnpm exec playwright test --ui
```

### Quality Gates
```bash
# Typecheck
pnpm run typecheck

# Linter
pnpm run lint

# Build
pnpm run build

# Full validation (all in one)
pnpm run typecheck && pnpm run lint && pnpm run test:unit && pnpm run build
```

### Coverage
```bash
# Generate coverage
pnpm run test:unit:coverage

# View HTML report
open coverage/index.html

# Check coverage thresholds
cat coverage/coverage-summary.json
```

### CI/CD
```bash
# Test GitHub Actions locally (if act installed)
act -P ubuntu-latest=nektos/act-environments-ubuntu:18.04 push

# Pre-commit hooks
pre-commit install
pre-commit run --all-files
```

---

## 🎯 Implementation Flow

```
1. BACKUP
   ↓
   cp vitest.setup.ts vitest.setup.ts.backup
   git checkout -b feature/testing-fix
   
2. MOCKS (Phase 1)
   ↓
   Edytuj vitest.setup.ts (użyj IMPLEMENTATION_EXAMPLES.md)
   Test: pnpm run test:unit -- customer
   
3. STORE TESTS (Phase 2)
   ↓
   Napraw customer.store.spec.ts
   Napraw order.store.spec.ts
   ... (reszta według checklisty)
   
4. COVERAGE (Phase 3)
   ↓
   Edytuj vitest.config.ts
   Test: pnpm run test:unit:coverage
   
5. CI/CD (Phase 4)
   ↓
   Dodaj .github/workflows/frontend-tests.yml
   Dodaj .pre-commit-config.yaml
   
6. E2E (Phase 5)
   ↓
   Implementuj login-flow.spec.ts
   Waliduj customer-flow.spec.ts
   Waliduj product-flow.spec.ts
   
7. DOCUMENTATION (Phase 6)
   ↓
   Dodaj tests/README.md
   Code review
   Merge
```

---

## 📋 Phase Checklist

### Phase 1: Mock Setup
- [ ] vitest.setup.ts updated with mocks
- [ ] tests/unit/composables-mock.ts created
- [ ] First test passes (customer store)

### Phase 2: Store Tests
- [ ] customer.store.spec.ts fixed
- [ ] order.store.spec.ts fixed + getters added
- [ ] invoice.store.spec.ts fixed
- [ ] payment.store.spec.ts fixed
- [ ] product.store.spec.ts fixed
- [ ] subscription.store.spec.ts fixed
- [ ] All 57 tests pass

### Phase 3: Coverage
- [ ] vitest.config.ts updated with coverage
- [ ] Threshold set to 80%
- [ ] HTML report generated
- [ ] Coverage ≥ 80%

### Phase 4: CI/CD
- [ ] .github/workflows/frontend-tests.yml created
- [ ] .pre-commit-config.yaml created
- [ ] Hooks tested locally
- [ ] Workflow triggers on PR

### Phase 5: E2E Tests
- [ ] login-flow.spec.ts implemented
- [ ] customer-flow.spec.ts validated
- [ ] product-flow.spec.ts validated
- [ ] All E2E tests pass

### Phase 6: Documentation
- [ ] tests/README.md created
- [ ] Code review completed
- [ ] Merged to develop
- [ ] Team training done

---

## 🚨 Troubleshooting

### Problem: "useApi is not defined"
**Rozwiązanie**: 
1. Sprawdź vitest.setup.ts czy ma mock `vi.mock('~/composables/useApi')`
2. Użyj `const { get } = useApi()` w teście
3. Nie używaj lokalnych mocków w plikach testowych

### Problem: "Cannot set properties of null"
**Rozwiązanie**:
1. Zmień `store.customers.value = [...]` na `store.customers = [...]`
2. Pinia setup stores nie wymagają `.value`

### Problem: "getOrdersByPriority is not defined"
**Rozwiązanie**:
1. Dodaj metodę w app/stores/order.ts
2. Eksportuj w return statement
3. Sprawdź TESTING_CHECKLIST.md - jest lista brakujących metod

### Problem: Coverage too low
**Rozwiązanie**:
1. Uruchom: `pnpm run test:unit:coverage`
2. Otwórz: `coverage/index.html`
3. Dodaj testy dla niepokrytych linii
4. Sprawdź excludes w vitest.config.ts

### Problem: CI/CD fails
**Rozwiązanie**:
1. Sprawdź Node.js version (wymagane 21+)
2. Sprawdź pnpm cache w workflow
3. Test locally: `act push`
4. Check logs w GitHub Actions

### Problem: Pre-commit hooks fail
**Rozwiązanie**:
1. Zainstaluj: `pip install pre-commit`
2. Zainstaluj hooks: `pre-commit install`
3. Test: `pre-commit run --all-files`
4. Skip dla szybkiego commit: `git commit --no-verify`

---

## 📞 Support

### Documentation Order
1. **Start here**: EXECUTIVE_SUMMARY.md
2. **Implementation**: TESTING_IMPLEMENTATION_PLAN.md
3. **Daily work**: TESTING_CHECKLIST.md
4. **Reference**: IMPLEMENTATION_EXAMPLES.md
5. **Files**: FILES_TO_MODIFY.md
6. **This file**: README_TESTING.md

### Resources
- **Vitest**: https://vitest.dev/
- **Playwright**: https://playwright.dev/
- **Vue Test Utils**: https://vue-test-utils.vuejs.org/
- **Pinia Testing**: https://pinia.vuejs.org/cookbook/testing.html
- **GitHub Actions**: https://docs.github.com/en/actions

### Common Patterns
```typescript
// Store test pattern
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCustomerStore } from '~/stores/customer'

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
})

// Mock API in test
const { get } = useApi()
vi.mocked(get).mockResolvedValueOnce({ data: mockData })

// Access Pinia refs directly (no .value)
store.customers = mockCustomers
```

---

## ✅ Final Verification

### Before Merge
```bash
# All tests pass
pnpm run test:unit

# Coverage is OK
pnpm run test:unit:coverage

# E2E tests pass
pnpm run test:e2e

# Typecheck OK
pnpm run typecheck

# Linter OK
pnpm run lint

# Build OK
pnpm run build
```

### After Implementation
- [ ] 57/57 tests pass (100%)
- [ ] Coverage ≥ 80%
- [ ] CI/CD workflow green
- [ ] Pre-commit hooks working
- [ ] E2E tests pass
- [ ] Documentation complete
- [ ] Team trained

---

## 📈 Success Metrics

| Metric | Target | Status |
|--------|--------|--------|
| Unit tests | 100% pass | 57/57 |
| E2E tests | 100% pass | 3/3 |
| Coverage | ≥80% | TBD |
| CI/CD | Configured | TBD |
| Pre-commit | Configured | TBD |
| Documentation | Complete | ✅ |

---

**Dokumentacja utworzona**: 2024-11-04  
**Wersja**: 1.0  
**Status**: Ready for Implementation

