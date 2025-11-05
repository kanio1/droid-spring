# Allure Reports Integration

## Overview

Allure Reports provides comprehensive, beautiful test reporting with rich attachments, test steps, and categories. This integration enables advanced test reporting for both Playwright E2E tests and Vitest unit tests.

## Features

- ✅ **Rich Test Steps** - Add meaningful steps to test reports
- ✅ **Attachments** - Screenshots, logs, videos, files
- ✅ **Custom Metadata** - Severity, owner, tags, links
- ✅ **Categories** - Automatic test categorization (Passed, Failed, Broken, etc.)
- ✅ **Historical Data** - Track trends and flakiness
- ✅ **Environment Info** - Browser, OS, Node.js version, etc.
- ✅ **Bugs Integration** - Link to JIRA/TMS issues

## Installation

### 1. Install Allure Command Line

```bash
# Using npm (recommended)
npm install -g allure-commandline

# Or using package manager
brew install allure  # macOS
choco install allure.commandline  # Windows
```

### 2. Install Playwright Allure Reporter

```bash
cd frontend
npm install -D allure-playwright
```

### 3. Install Vitest Allure Reporter

```bash
npm install -D @wdio/allure-reporter
```

## Configuration

### Playwright Configuration

Edit `playwright.config.ts`:

```typescript
import { defineConfig } from '@playwright/test'
import { AllurePlaywrightReporter } from '@/tests/allure/playwright.hooks'

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['list'],
    ['html'],
    ['allure-playwright', {
      outputFolder: 'test-results/allure',
      cleanOutputDir: true,
      includeLegacyMochawesome: true,
    }]
  ],
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ],
  webServer: {
    command: 'npm run dev',
    port: 3000,
    reuseExistingServer: !process.env.CI
  }
})
```

### Vitest Configuration

Edit `vitest.config.ts`:

```typescript
import { defineConfig } from 'vitest/config'
import { allureReporter } from '@/tests/allure/vitest.config'

export default defineConfig({
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './tests/vitest.setup.ts',
    reporters: [
      'default',
      'html',
      allureReporter({
        outputDir: 'test-results/allure-vitest'
      })
    ],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/',
        'tests/',
        'dist/',
        'coverage/'
      ]
    }
  }
})
```

## Usage

### Playwright with Allure

```typescript
import { test } from '@playwright/test'
import { AllureTestWrapper } from '@/tests/allure/allure.config'

test.describe('Customer Management', () => {
  test('should create customer @critical', async ({ page }) => {
    // Add metadata
    AllureTestWrapper.epic('Customer Management')
    AllureTestWrapper.feature('Customer CRUD')
    AllureTestWrapper.severity('critical')
    AllureTestWrapper.owner('Tech Lead')
    AllureTestWrapper.tag('create', 'customer')

    // Add links
    AllureTestWrapper.issue('BSS-123', 'https://jira.company.com/browse/BSS-123')
    AllureTestWrapper.tms('TMS-456', 'https://tms.company.com/case/TMS-456')

    // Add description
    AllureTestWrapper.description('This test creates a new customer and verifies the process')

    // Add test steps
    await AllureTestWrapper.step('Navigate to customers page', async () => {
      await page.goto('/customers')
    })

    await AllureTestWrapper.step('Click create button', async () => {
      await page.click('[data-testid="create-button"]')
    })

    await AllureTestWrapper.step('Fill form', async () => {
      await page.fill('[data-testid="email"]', 'john.doe@example.com')
    })

    // Attach screenshot
    await AllureTestWrapper.attach('Filled Form', async () => {
      await page.screenshot({ path: 'screenshot.png' })
      return 'screenshot.png'
    })

    // Submit and verify
    await AllureTestWrapper.step('Submit form', async () => {
      await page.click('[data-testid="submit"]')
      await expect(page.locator('[data-testid="success"]')).toBeVisible()
    })
  })
})
```

### Vitest with Allure

```typescript
import { describe, it, expect } from 'vitest'
import { CustomerService } from '@/app/services/customer.service'
import { CustomerFactory } from '@/tests/framework/data-factories'

describe('Customer Service @customer-service', () => {
  it('should create customer @critical', () => {
    // Add metadata using Vitest's built-in features
    // Note: Vitest doesn't have built-in Allure integration like Playwright
    // You can use console logs or a custom reporter

    const customerData = CustomerFactory.create().active().build()
    const service = new CustomerService()

    const result = service.createCustomer(customerData)

    expect(result).toBeDefined()
    expect(result.email).toBe(customerData.email)
  })
})
```

## Running Tests with Allure

### Playwright Tests

```bash
# Run tests and generate Allure results
npx playwright test --reporter=line,allure-playwright

# Run specific test file
npx playwright test tests/e2e/customer-flow.spec.ts --reporter=allure-playwright
```

### Vitest Tests

```bash
# Run tests and generate Allure results
npx vitest run --reporter=default,allure-vitest

# Run in watch mode
npx vitest --reporter=default,allure-vitest
```

## Generating Reports

### Option 1: Command Line (Recommended)

```bash
# Generate report from results
allure generate test-results/allure -o allure-report --clean

# Serve report in browser
allure serve test-results/allure

# Open on specific port
allure serve test-results/allure -p 5050
```

### Option 2: GitHub Actions

Add to your workflow:

```yaml
- name: Run Playwright tests
  run: npx playwright test --reporter=line,allure-playwright

- name: Generate Allure Report
  run: |
    npm install -g allure-commandline
    allure generate test-results/allure -o allure-report --clean

- name: Upload Allure Report
  uses: actions/upload-artifact@v3
  if: always()
  with:
    name: allure-report
    path: allure-report

- name: Deploy to GitHub Pages
  if: github.ref == 'refs/heads/main'
  uses: peaceiris/actions-gh-pages@v3
  with:
    github_token: ${{ secrets.GITHUB_TOKEN }}
    publish_dir: ./allure-report
```

## Advanced Features

### Custom Categories

Create `allure-results/categories.json`:

```json
[
  {
    "name": "Product Defects",
    "matchedStatuses": ["failed"],
    "messageRegex": "AssertionError.*"
  },
  {
    "name": "Test Defects",
    "matchedStatuses": ["broken"],
    "messageRegex": "Error.*"
  }
]
```

### Environment Configuration

The framework automatically generates `environment.properties`:

```properties
project=BSS System
version=1.0.0
framework=Playwright/Vitest
node=18.17.0
platform=linux
browser=chromium
environment=development
timestamp=2025-11-05T11:00:00.000Z
```

### Custom Tags and Metadata

Use tags in test titles:

```typescript
test('should process payment @critical @payment')
test('should validate form @high @validation')
test('should handle error @medium @error')
```

## Report Structure

After generation, your Allure report includes:

- **Overview Dashboard** - Summary statistics
- **Categories** - Passed, Failed, Broken, Skipped
- **Suites** - Test suites organized by feature
- **Graphs** - Status distribution, duration trends
- **Timeline** - Test execution timeline
- **Behaviors** - Epic/Feature/Story breakdown
- **Package** - Tests organized by file/package

## Best Practices

### 1. Use Descriptive Test Names

```typescript
// ✅ Good
test('should create customer with valid email and password @critical')

// ❌ Bad
test('test1')
```

### 2. Add Meaningful Steps

```typescript
// ✅ Good
await allure.step('Navigate to registration page', async () => {
  await page.goto('/register')
})

// ❌ Bad
await page.goto('/register')
```

### 3. Attach Screenshots on Failure

```typescript
test.afterEach(async ({ page }, testInfo) => {
  if (testInfo.status !== 'passed') {
    await allure.attach('Failure Screenshot', async () => {
      await page.screenshot()
      return 'screenshot.png'
    })
  }
})
```

### 4. Link to Issues

```typescript
AllureTestWrapper.issue('BUG-123', 'https://jira.com/bug/BUG-123')
AllureTestWrapper.tms('TMS-456', 'https://tms.com/case/TMS-456')
```

### 5. Use Severity Levels

- `@critical` - Must pass, blocking for release
- `@high` - Important functionality
- `@medium` - Normal functionality
- `@low` - Minor issues, nice-to-have

## Troubleshooting

### Allure command not found

```bash
# Install globally
npm install -g allure-commandline

# Add to PATH
export PATH=$PATH:$(npm root -g)/allure-commandline/bin
```

### Report not generating

```bash
# Check if results exist
ls -la test-results/allure/

# Clean and regenerate
rm -rf test-results/allure
npx playwright test --reporter=allure-playwright
allure generate test-results/allure -o allure-report
```

### Empty attachments

Ensure you're using async/await correctly:

```typescript
// ✅ Correct
await AllureTestWrapper.attach('Screenshot', async () => {
  await page.screenshot({ path: 'screenshot.png' })
  return 'screenshot.png'
})

// ❌ Wrong
AllureTestWrapper.attach('Screenshot', async () => {
  await page.screenshot({ path: 'screenshot.png' })
  return 'screenshot.png'
})  // Missing await
```

## Integration with CI/CD

### Jenkins

```groovy
pipeline {
  agent any
  stages {
    stage('Test') {
      steps {
        sh 'npm test -- --reporter=allure-playwright'
      }
    }
    stage('Report') {
      steps {
        sh 'allure generate test-results/allure -o allure-report --clean'
        publishHTML([
          allowMissing: false,
          alwaysLinkToLastBuild: true,
          keepAll: true,
          reportDir: 'allure-report',
          reportFiles: 'index.html',
          reportName: 'Allure Report'
        ])
      }
    }
  }
}
```

### GitLab CI

```yaml
test:
  script:
    - npm test -- --reporter=allure-playwright
  artifacts:
    reports:
      allure:
        path: test-results/allure

pages:
  script:
    - allure generate test-results/allure -o public --clean
  artifacts:
    paths:
      - public
```

## Examples

See example tests:
- `tests/allure/examples/playwright-example.spec.ts` - Playwright with Allure
- `tests/allure/examples/vitest-example.spec.ts` - Vitest with Allure

## Additional Resources

- [Allure Documentation](https://docs.qameta.io/allure/)
- [Playwright Reporter API](https://playwright.dev/docs/test-reporters)
- [Vitest Reporters](https://vitest.dev/guide/reporters.html)

## Support

For issues with Allure integration:
1. Check if Allure CLI is installed: `allure --version`
2. Verify test results exist: `ls test-results/allure/`
3. Check console output for errors
4. See troubleshooting section above

## License

MIT License - feel free to use in your projects
