# Visual Regression Testing

Ten folder zawiera testy regresji wizualnej używające **Percy** i **Playwright**.

## Przegląd

Testy wizualne automatycznie porównują zrzuty ekranu komponentów i stron, wykrywając niepożądane zmiany wizualne.

## Narzędzia

- **Percy Cloud**: Hostowane testy wizualne z przechowywaniem snapshotów
- **Playwright**: Framework E2E do automatyzacji przeglądarki
- **@percy/playwright**: Integracja Percy z Playwright

## Pliki testowe

### 1. `visual-regression.spec.ts` (515 linii)
Podstawowy zestaw testów wizualnych z Playwright's `toHaveScreenshot()`.

**Kategorie testów:**
- **Component Visual Tests** - komponenty UI (Button, Input, Card, Modal, Table, Navigation)
- **Page Visual Tests** - pełne strony (Dashboard, Products, Customers, Orders)
- **Viewport Visual Tests** - responsywność (mobile 390x844, tablet 768x1024, desktop 1920x1080)
- **Theme Visual Tests** - motywy jasny/ciemny
- **Interactive States** - stany interaktywne (hover, focus, active)
- **Form Visual Tests** - formularze (login, create customer, create order)
- **Data Display** - stany danych (puste, ładowanie, błędy, tabela)
- **Navigation Visual** - nawigacja (sidebar, breadcrumbs, pagination)
- **Modal Visual** - modale (potwierdzenie, szczegóły klienta)
- **Notification Visual** - powiadomienia (success, error)

### 2. `customer-form.visual.spec.ts` (90 linii)
Testy wizualne formularza klienta z Percy.

**Scenariusze:**
- Tryb tworzenia (Create Mode)
- Tryb edycji (Edit Mode)
- Tryb podglądu (View Mode)
- Błędy walidacji
- Stan ładowania podczas wysyłania

**Viewports:** 375px, 768px, 1280px

### 3. `customer-list.visual.spec.ts` (111 linii)
Testy wizualne listy klientów z Percy.

**Scenariusze:**
- Widok siatka (Grid View)
- Widok lista (List View)
- Widok tabela (Table View)
- Stan pusty (Empty State)
- Stan ładowania (Loading State)
- Stan błędu (Error State)
- Wyszukiwanie i filtry

**Viewports:** 375px, 768px, 1280px

## Konfiguracja

### 1. Zależności

Dodane w `package.json`:
```json
"devDependencies": {
  "@percy/cli": "^1.30.0",
  "@percy/playwright": "^1.0.5"
}
```

### 2. Playwright Config

Projekt `visual` w `playwright.config.ts`:
```typescript
{
  name: 'visual',
  testDir: './tests/visual',
  use: { ...devices['Desktop Chrome'] },
  timeout: 60000,
  retries: 0,
}
```

### 3. Percy Config

Plik `.percy.yml`:
```yaml
version: 2
snapshot:
  widths: [375, 768, 1280]
  min-height: 1024
  percy-css: |
    [data-testid="loading"], .loading, .spinner {
      visibility: hidden !important;
    }
    * {
      animation-duration: 0s !important;
      transition-duration: 0s !important;
    }
```

### 4. Zmienne środowiskowe

Skopiuj `.env.percy.example` do `.env.percy` i ustaw:
```bash
PERCY_TOKEN=your_percy_token_here
```

**Uzyskaj token:** https://percy.io/app/project-settings

## Uruchamianie testów

### Opcja 1: Skrypt pomocniczy (zalecane)
```bash
./scripts/run-visual-tests.sh
```

Skrypt automatycznie:
- Sprawdzi token Percy
- Uruchomi dev server jeśli nie jest dostępny
- Wykona testy wizualne
- Pokaże raport

### Opcja 2: Bezpośrednie komendy
```bash
# Z tokenem Percy
export PERCY_TOKEN=your_token_here
pnpm test:visual

# Bez tokena (dry-run - tylko testy Playwright)
pnpm playwright test visual
```

### Opcja 3: Z plikiem .env.percy
```bash
# 1. Skopiuj szablon
cp .env.percy.example .env.percy

# 2. Edytuj i dodaj swój token
vim .env.percy

# 3. Uruchom testy
pnpm test:visual
```

## Interpretacja wyników

### ✅ Testy zakończone pomyślnie
- Wszystkie snapshoty zgodne z baseline
- Nowe snapshoty zapisane do baseline

### ❌ Testy nieudane
- **Visual diffs** - wykryto zmiany wizualne
  - Sprawdź Percy Dashboard: https://percy.io
  - Zatwierdź zmiany jako nowy baseline
  - Albo popraw kod i uruchom ponownie

### 🚀 Nowe snapshoty
- Przy pierwszym uruchomieniu testów
- Percy zapisuje snapshoty jako baseline
- Kolejne uruchomienia porównują do baseline

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Visual Tests
on: [push, pull_request]

jobs:
  visual-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: pnpm/action-setup@v2
      - uses: actions/setup-node@v3
        with:
          node-version: 18
      - run: pnpm install
      - run: pnpm test:visual
        env:
          PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}
```

### Jenkins Pipeline Example
```groovy
pipeline {
    agent any
    environment {
        PERCY_TOKEN = credentials('percy-token')
    }
    stages {
        stage('Install') {
            steps {
                sh 'pnpm install'
            }
        }
        stage('Visual Tests') {
            steps {
                sh 'pnpm test:visual'
            }
        }
    }
}
```

## Best Practices

### 1. Stabilizacja Snapshotów
```typescript
// Ukryj elementy dynamiczne
await page.addStyleTag({
  content: `
    [data-testid="loading"], .spinner {
      visibility: hidden !important;
    }
  `
})

// Wyłącz animacje
await page.addStyleTag({
  content: `
    * {
      animation-duration: 0s !important;
      transition-duration: 0s !important;
    }
  `
})
```

### 2. Czekanie na elementy
```typescript
// Zawsze czekaj na elementy przed screenshotem
await page.waitForSelector('[data-testid="customer-list"]')
await expect(page.locator('[data-testid="customer-list"]')).toBeVisible()
```

### 3. Stany interaktywne
```typescript
// Testuj stany hover, focus, active
await button.hover()
await page.screenshot()

await button.focus()
await page.screenshot()
```

### 4. Responsywność
```typescript
// Testuj na różnych viewportach
for (const width of [375, 768, 1280]) {
  await page.setViewportSize({ width, height: 1024 })
  await page.screenshot()
}
```

## Troubleshooting

### Problem: Testy nie uruchamiają się
**Rozwiązanie:**
```bash
# Sprawdź instalację Percy
pnpm list @percy/cli @percy/playwright

# Reinstaluj jeśli brak
pnpm add -D @percy/cli @percy/playwright
```

### Problem: "PERCY_TOKEN is required"
**Rozwiązanie:**
```bash
# Ustaw token
export PERCY_TOKEN=your_token_here

# Lub dodaj do .env.percy
echo "PERCY_TOKEN=your_token_here" > .env.percy
```

### Problem: Visual diffs w CI, ale nie lokalnie
**Rozwiązanie:**
- Spójny viewport: CI często ma inny rozmiar
- Wyłącz animacje w CSS
- Stabilizuj elementy dynamiczne (loading, spinners)
- Czekaj na network idle

### Problem: Nietea sprawdzania w snapshotach
**Rozwiązanie:**
- Zwiększ `maxDiffPixelRatio` w playwright.config.ts
- Dla Percy: dostosuj threshold w UI
- Sprawdź CSS per-snapshot

## Raportowanie

### Percy Dashboard
- URL: https://percy.io
- Historia wszystkich snapshotów
- Porównania między buildami
- Akceptacja/odrzucenie zmian

### Playwright Report
- HTML: `./playwright-report/index.html`
- JSON: `./test-results/results.xml`
- Zrzuty ekranu błędów w `./test-results/`

## Zasoby

- **Percy Docs:** https://docs.percy.io/
- **Playwright Screenshots:** https://playwright.dev/docs/test-snapshots
- **Percy CLI:** https://github.com/percy/cli
- **Visual Testing Best Practices:** https://docs.percy.io/docs/visual-testing

## Następne kroki

1. ✅ Uruchom testy lokalnie
2. ✅ Zintegruj z CI/CD
3. ✅ Skonfiguruj powiadomienia
4. ✅ Dodaj więcej komponentów do testowania
5. ✅ Testuj na wielu przeglądarkach
6. ✅ Skonfiguruj threshold dla diff'ów
