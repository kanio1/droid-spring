# Accessibility Testing Guide

Ten folder zawiera testy accessibility używające **axe-core** i **Playwright** dla walidacji WCAG 2.1 Level AA.

## Przegląd

Testy accessibility automatycznie sprawdzają zgodność aplikacji ze standardami dostępności internetowej WCAG 2.1.

## Narzędzia

- **axe-core** - najpopularniejszy engine do testowania accessibility w web
- **Playwright** - framework E2E do automatyzacji przeglądarki
- **WCAG 2.1** - Web Content Accessibility Guidelines 2.1
  - Level A (podstawowy)
  - Level AA (standard przemysłowy)
  - Level AAA (zaawansowany)

## Pliki testowe

### `accessibility.spec.ts` (350+ linii)

Kompleksowa suite testów accessibility podzielona na kategorie:

#### 1. **Pages** - Strony główne
- Dashboard page
- Customers page
- Customer create form
- Customer edit form
- Products page
- Orders page
- Invoices page
- Login page

#### 2. **Navigation** - Nawigacja
- Main navigation keyboard accessibility
- Skip links presence and functionality
- Breadcrumbs accessibility

#### 3. **Forms** - Formularze
- Form inputs with proper labels
- Form validation errors announcement
- Required field marking

#### 4. **Interactive Elements** - Elementy interaktywne
- Buttons with accessible names
- Links with descriptive text
- Focus indicators visibility

#### 5. **Tables** - Tabele
- Data tables with proper headers
- Table captions

#### 6. **Modals & Dialogs** - Modale i dialogi
- Focus trapping in modals
- ARIA attributes on dialogs

#### 7. **Color & Contrast** - Kolor i kontrast
- Text contrast requirements (4.5:1 ratio)
- Color not sole means of information

#### 8. **Dynamic Content** - Zawartość dynamiczna
- Loading states announcement
- Success messages with aria-live

#### 9. **Media** - Media
- Images with alt text
- Icons with proper accessibility

#### 10. **WCAG 2.1 Compliance** - Zgodność z WCAG
- Level A compliance for all critical pages
- Level AA compliance for critical user flows

## Framework

### `tests/framework/accessibility/axe-testing.ts`

Framework accessibility dostarczający metod pomocniczych:

#### Główne metody:
- `injectAxe(page)` - Injeuje axe-core do strony
- `analyzePage(page, options)` - Analizuje całą stronę
- `analyzeElement(page, selector, options)` - Analizuje konkretny element
- `expectPageToBeAccessible(page, options)` - Sprawdza dostępność strony
- `expectElementToBeAccessible(page, selector, options)` - Sprawdza element
- `checkFormLabels(page, form)` - Sprawdza etykiety formularzy
- `checkKeyboardNavigation(page, selector)` - Sprawdza nawigację klawiaturą
- `checkAriaLabel(page, selector)` - Sprawdza ARIA labels
- `checkColorContrast(page, locator)` - Sprawdza kontrast kolorów
- `checkImageAltText(page)` - Sprawdza alt text obrazów
- `generateReport(page, path)` - Generuje raport HTML

## Uruchamianie testów

### Pojedynczy plik
```bash
pnpm test:accessibility
```

### Z tagami
```bash
# Tylko testy WCAG Level A
pnpm playwright test --grep "Level A"

# Tylko testy formularzy
pnpm playwright test --grep "Forms"
```

### W trybie debug
```bash
pnpm playwright test accessibility.spec.ts --debug
```

### W trybie headed
```bash
pnpm playwright test accessibility.spec.ts --headed
```

### Z raportem HTML
```bash
pnpm playwright test accessibility.spec.ts --reporter=html
```

## Konfiguracja

### Playwright Config
Projekt `accessibility` w `playwright.config.ts`:
```typescript
{
  name: 'accessibility',
  testDir: './tests/e2e/accessibility',
  use: { ...devices['Desktop Chrome'] },
  timeout: 60000,
  retries: 0,
}
```

### Package.json Scripts
```json
{
  "test:accessibility": "playwright test accessibility"
}
```

## Przykłady testów

### Sprawdzenie dostępności strony
```typescript
test('Dashboard should be accessible', async ({ page }) => {
  await page.goto('/dashboard')

  await AccessibilityTest.expectPageToBeAccessible(page, {
    tags: ['wcag2a', 'wcag2aa', 'wcag21aa']
  })
})
```

### Sprawdzenie elementu
```typescript
test('Customer list should be accessible', async ({ page }) => {
  await page.goto('/customers')

  await AccessibilityTest.expectElementToBeAccessible(
    page,
    '[data-testid="customer-list"]',
    { tags: ['wcag2aa'] }
  )
})
```

### Sprawdzenie etykiet formularza
```typescript
test('Form inputs should have proper labels', async ({ page }) => {
  await page.goto('/customers/create')

  const labelCheck = await AccessibilityTest.checkFormLabels(page, 'form')
  expect(labelCheck.hasLabels).toBe(true)
  expect(labelCheck.missingLabels).toEqual([])
})
```

### Sprawdzenie nawigacji klawiaturą
```typescript
test('Navigation should be keyboard accessible', async ({ page }) => {
  await page.goto('/dashboard')

  const navCheck = await AccessibilityTest.checkKeyboardNavigation(
    page,
    '[data-testid="main-navigation"]'
  )
  expect(navCheck.isFocusable).toBe(true)
})
```

### Sprawdzenie kontrastu kolorów
```typescript
test('Text should meet contrast requirements', async ({ page }) => {
  await page.goto('/dashboard')

  const contrastInfo = await AccessibilityTest.checkColorContrast(
    page,
    page.locator('body')
  )

  expect(contrastInfo.normal).toBe(true)
  expect(contrastInfo.large).toBe(true)
  expect(contrastInfo.ratio).toBeGreaterThanOrEqual(4.5)
})
```

## Interpretacja wyników

### ✅ Testy zakończone pomyślnie
- Brak naruszeń accessibility
- Strona spełnia wymagania WCAG

### ❌ Testy nieudane
- **Violations** - wykryte problemy accessibility
  - **Critical** - krytyczne, muszą być naprawione
  - **Serious** - poważne, powinny być naprawione
  - **Moderate** - umiarkowane, warto naprawić
  - **Minor** - drobne, opcjonalne do naprawienia

### Przykład błędu
```
Accessibility violations found:
  - color-contrast (serious)
    Description: Elements must have sufficient color contrast
    Help: https://dequeuniversity.com/rules/axe/4.4/color-contrast
    Elements: .button, .text-muted
```

## WCAG 2.1 Level AA - Kluczowe wymagania

### 1. Perceivable (Postrzegalne)
- **1.1 Text Alternatives** - Tekst alternatywny dla obrazów
- **1.2 Time-based Media** - Napisy dla multimediów
- **1.3 Adaptable** - Treść adaptowalna
- **1.4 Distinguishable** - Rozróżnialna treść (kontrast 4.5:1)

### 2. Operable (Obsługiwalne)
- **2.1 Keyboard Accessible** - Dostępne klawiaturą
- **2.2 Enough Time** - Wystarczający czas
- **2.3 Seizures** - Bez napadów
- **2.4 Navigable** - Nawigowalna treść

### 3. Understandable (Zrozumiałe)
- **3.1 Readable** - Czytelna treść
- **3.2 Predictable** - Przewidywalne działanie
- **3.3 Input Assistance** - Pomoc przy wprowadzaniu

### 4. Robust (Solidne)
- **4.1 Compatible** - Kompatybilne z technologiami wspomagającymi

## Naprawa typowych problemów

### 1. Brak etykiet dla inputów
```html
<!-- ❌ Złe -->
<input type="text" id="name">

<!-- ✅ Dobre -->
<label for="name">Name</label>
<input type="text" id="name">

<!-- Lub -->
<input type="text" aria-label="Name">
```

### 2. Niewystarczający kontrast
```css
/* ❌ Złe - kontrast 2.5:1 */
.button { color: #999; background: #fff; }

/* ✅ Dobre - kontrast 4.5:1 */
.button { color: #333; background: #fff; }
```

### 3. Brak focus indicators
```css
/* ✅ Dobre */
button:focus {
  outline: 2px solid #0066cc;
  outline-offset: 2px;
}
```

### 4. Obrazy bez alt text
```html
<!-- ❌ Złe -->
<img src="logo.png">

<!-- ✅ Dobre -->
<img src="logo.png" alt="Company Logo">
```

### 5. Modale bez focus trap
```typescript
// ✅ Implementuj focus trap
const focusableElements = modal.querySelectorAll(
  'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
)
const firstElement = focusableElements[0]
const lastElement = focusableElements[focusableElements.length - 1]
```

## Integracja z CI/CD

### GitHub Actions
```yaml
name: Accessibility Tests
on: [push, pull_request]

jobs:
  accessibility:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: pnpm/action-setup@v2
      - uses: actions/setup-node@v3
        with:
          node-version: 18
      - run: pnpm install
      - run: pnpm test:accessibility
```

### Jenkins Pipeline
```groovy
pipeline {
    agent any
    stages {
        stage('Accessibility Tests') {
            steps {
                sh 'pnpm install'
                sh 'pnpm test:accessibility'
            }
        }
    }
}
```

## Raportowanie

### HTML Report
```bash
# Generuje raport w playwright-report/
pnpm playwright test accessibility.spec.ts --reporter=html
```

### Accessibility Report
```typescript
await AccessibilityTest.generateReport(
  page,
  'test-results/accessibility-dashboard.html'
)
```

## Najlepsze praktyki

### 1. Używaj semantycznego HTML
```html
<button>Click me</button> <!-- ✅ Dobre -->
<div onclick="doSomething()">Click me</div> <!-- ❌ Złe -->
```

### 2. Dodawaj ARIA tylko gdy potrzebne
```html
<!-- ✅ Dobre - natywne semantyczne -->
<button>Save</button>

<!-- ❌ Złe - niepotrzebne ARIA -->
<div role="button" aria-label="Save">Save</div>
```

### 3. Testuj klawiaturą
- Tab - nawigacja do przodu
- Shift+Tab - nawigacja wstecz
- Enter/Space - aktywacja
- Escape - zamknięcie modal/dropdown

### 4. Zawsze testuj z czytnikiem ekranu
- NVDA (Windows)
- JAWS (Windows)
- VoiceOver (macOS)

## Narzędzia deweloperskie

### Browser DevTools
- Chrome: Lighthouse > Accessibility
- Firefox: Accessibility Inspector
- Safari: Accessibility Inspector

### Rozszerzenia przeglądarki
- axe DevTools
- WAVE
- Lighthouse

## Zasoby

- **WCAG 2.1:** https://www.w3.org/WAI/WCAG21/quickref/
- **axe-core:** https://github.com/dequelabs/axe-core
- **WebAIM:** https://webaim.org/
- **Accessibility Guidelines:** https://a11yproject.com/
- **Playwright:** https://playwright.dev/docs/accessibility-testing

## Troubleshooting

### Problem: axe-core nie jest załadowany
**Rozwiązanie:**
```typescript
await AccessibilityTest.injectAxe(page)
```

### Problem: Testy przekraczają timeout
**Rozwiązanie:**
```typescript
// Zwiększ timeout w test
test('should be accessible', async ({ page }) => {
  test.setTimeout(60000)
  // test code
})
```

### Problem: Za dużo violations
**Rozwiązanie:**
- Naprawaj w kolejności: Critical → Serious → Moderate → Minor
- Używaj `exclude` dla znanych problemów:
```typescript
await AccessibilityTest.analyzePage(page, {
  exclude: ['.known-issue']
})
```

## CI/CD Integration

### Threshold dla regression
```typescript
const violations = await AccessibilityTest.analyzePage(page)
const criticalCount = violations.filter(v => v.impact === 'critical').length

// Fail jeśli jest więcej niż 0 critical violations
expect(criticalCount).toBe(0)
```

## Następne kroki

1. ✅ Uruchom testy accessibility
2. ✅ Napraw wykryte problemy
3. ✅ Dodaj testy do CI/CD
4. ✅ Skonfiguruj alerting na violations
5. ✅ Testuj z rzeczywistymi czytnikami ekranu
6. ✅ Edukuj zespół o accessibility
7. ✅ Regularnie audytuj aplikację
