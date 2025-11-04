# Testing Framework - Dokumentacja

## Przegląd

BSS Frontend wykorzystuje wielowarstwową strategię testowania z wykorzystaniem:
- **Unit Tests**: Vitest + Vue Test Utils
- **E2E Tests**: Playwright
- **Coverage**: @vitest/coverage-v8

## Struktura Testów

```
frontend/
├── tests/
│   ├── unit/           # Testy jednostkowe (57 testów)
│   │   ├── *.store.spec.ts
│   │   └── hello.spec.ts
│   └── e2e/           # Testy end-to-end (3 testy)
│       ├── login-flow.spec.ts
│       ├── customer-flow.spec.ts
│       └── product-flow.spec.ts
├── vitest.setup.ts    # Globalna konfiguracja mocków
└── vitest.config.ts   # Konfiguracja Vitest + Coverage
```

## Testy Jednostkowe (Unit Tests)

### Technologie
- **Framework**: Vitest 2.1.4
- **Environment**: jsdom
- **Mockowanie**: vi.mock(), vi.fn()
- **Testowanie Pinia**: setActivePinia(), createPinia()

### Pokrycie Store'ów (Coverage)

| Store | Lines | Branches | Functions |
|-------|-------|----------|-----------|
| customer | 79.9% | 50% | 53.84% |
| invoice | 71.49% | 79.54% | 43.75% |
| order | 80.31% | 81.08% | 42.85% |
| payment | 82.47% | 82.05% | 53.84% |
| product | 74.68% | 69.76% | 37.5% |
| subscription | 80.95% | 65.85% | 53.33% |

**Średnia**: ~78% lines, ~72% branches

### Uruchamianie Testów

```bash
# Wszystkie testy
pnpm run test:unit

# Z coverage
pnpm run test:unit:coverage

# Z coverage (alias)
pnpm run test:coverage

# Watch mode
pnpm run test:unit -- --watch
```

### Mockowanie API

Każdy plik testowy ma skonfigurowany mock `useApi`:

```typescript
// Mock useApi composable
const mockUseApi = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: mockUseApi
}))

beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  mockUseApi.mockReturnValue({
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    del: vi.fn(),
    // ... wszystkie metody API
  })
})
```

### Konwencje Testów

1. **Struktura testu**:
   ```typescript
   it('should [action]', async () => {
     // Arrange
     const store = useStore()
     const mockData = {...}
     const { get } = mockUseApi()
     vi.mocked(get).mockResolvedValueOnce({ data: mockData })

     // Act
     await store.someAction()

     // Assert
     expect(store.state).toEqual(expected)
   })
   ```

2. **Pinia refs**: Używaj bezpośrednio `store.customers` zamiast `store.customers.value`

3. **Dynamiczne importy**: Store'y używają `await import()` dla lepszego mockowania

## Coverage

### Konfiguracja
- **Provider**: v8
- **Reporters**: text, json, html, lcov
- **Thresholds**:
  - Global: 70%
  - Per File: 60%

### Raporty
- HTML: `coverage/index.html`
- LCOV: `coverage/lcov.info`
- JSON: `coverage/coverage-final.json`

### Wykluczenia
- Testy (`tests/**`)
- Konfiguracja (`**/*.config.*`)
- UI Components (`app/components/**`)
- Layouts/Plugins/Middleware

## CI/CD

### GitHub Actions
Workflow: `.github/workflows/tests.yml`

**Pipeline**:
1. Checkout code
2. Setup pnpm
3. Install dependencies
4. Type check (`pnpm run typecheck`)
5. Lint (`pnpm run lint`)
6. Run unit tests
7. Run tests with coverage
8. Upload to Codecov
9. Build

### Pre-commit Hooks

**Husky + lint-staged** (opcjonalne):
```bash
# Inicjalizacja (wymaga repozytorium git)
pnpm exec husky init

# Konfiguracja w .lintstagedrc.json
{
  "*.{js,jsx,ts,tsx,vue}": [
    "eslint --fix",
    "vitest run --run"
  ]
}
```

## Testy End-to-End (E2E)

### Technologie
- **Framework**: Playwright 1.56.1
- **Testy**: `tests/e2e/*.spec.ts`

### Aktualne Testy
- ✅ `customer-flow.spec.ts` - Pełna implementacja
- ✅ `product-flow.spec.ts` - Pełna implementacja
- ⏳ `login-flow.spec.ts` - Szkielety (test.todo())

### Uruchamianie
```bash
pnpm run test:e2e
```

### Konwencje
- Używaj `data-testid` dla selektorów
- Testy autentykacji wymagają Keycloak
- BSS wymaga backend do pełnej funkcjonalności

## Debugowanie

### Unit Tests
```bash
# Debug w watch mode
pnpm run test:unit -- --watch

# Uruchom konkretny test
pnpm run test:unit -- customer.store.spec.ts --run

# Debug z inspect
node --inspect-brk node_modules/.bin/vitest run
```

### Coverage
```bash
# Generuj HTML report
pnpm run test:unit:coverage

# Otwórz w przeglądarce
open coverage/index.html
```

### E2E Tests
```bash
# Tryb headed
npx playwright test --headed

# Tryb debug
npx playwright test --debug

# UI Mode
npx playwright test --ui
```

## Best Practices

### Unit Tests
1. ✅ Testuj logikę biznesową w store'ach
2. ✅ Mockuj wszystkie zależności zewnętrzne
3. ✅ Używaj meaningful test names
4. ✅ Testuj stany sukcesu i błędów
5. ❌ Nie testuj bibliotek zewnętrznych (Pinia, Vue)

### E2E Tests
1. ✅ Testuj krytyczne user journeys
2. ✅ Używaj stable selectors
3. ✅ Testuj tylko w kontrolowanych środowiskach
4. ❌ Nie testuj third-party services (Keycloak w pełni)

## Metrics

- **Test Files**: 6 unit + 3 e2e
- **Unit Tests**: 57 passed
- **Coverage**: ~78% lines, ~72% branches
- **Store Coverage**: 74-82% lines
- **GitHub Actions**: ✅ Skonfigurowany
- **Pre-commit**: ⚠️ Wymaga inicjalizacji git

## Wsparcie

W przypadku problemów:
1. Sprawdź logi CI/CD
2. Uruchom `pnpm run test:unit:coverage`
3. Otwórz `coverage/index.html`
4. Sprawdź `.github/workflows/tests.yml`
