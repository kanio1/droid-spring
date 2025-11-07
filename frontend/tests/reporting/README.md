# Allure Reporting Suite

This directory contains the **Allure Reporting** configuration and utilities for generating rich, interactive test reports.

## Overview

Allure is a flexible, lightweight multi-language test reporting tool that provides clear, concise test reports with:
- **Rich visualizations** - Charts, graphs, and trends
- **Detailed test information** - Steps, attachments, and metadata
- **Historical data** - Test history and trend analysis
- **Category-based grouping** - Defects, failures, and flaky tests
- **Interactive reports** - Filter, search, and drill-down capabilities

## Features

### 1. Rich Test Reports
- Test execution timeline
- Pass/fail/skip statistics
- Test duration analysis
- Retry information
- Parameterized test details

### 2. Visual Analytics
- Trend charts (pass rate over time)
- Category pie charts
- Duration histograms
- Retry statistics
- Environment information

### 3. Detailed Attachments
- Screenshots on failure
- Page source code
- Console logs
- Network requests/responses
- Video recordings
- Custom files

### 4. Test Organization
- Epic/Feature/Story hierarchy
- Severity levels
- Owner assignment
- Custom labels
- Tags and categories

### 5. Historical Tracking
- Test history
- Trend analysis
- Flaky test detection
- Performance degradation alerts
- Build comparison

## Installation

### Option 1: Automatic Installation

```bash
# Install Allure CLI
pnpm run test:report:install

# Or use the script directly
./generate-allure-report.sh install
```

### Option 2: Manual Installation

**Linux:**
```bash
sudo apt-get update
sudo apt-get install -y default-jre
curl -o allure-2.24.1.tgz -L https://github.com/allure-framework/allure2/releases/download/2.24.1/allure-2.24.1.tgz
sudo tar -xzf allure-2.24.1.tgz -C /opt
sudo ln -s /opt/allure-2.24.1/bin/allure /usr/local/bin/allure
```

**macOS:**
```bash
brew install allure
```

**Windows:**
```powershell
# Install using Scoop
scoop install allure

# Or using Chocolatey
choco install allure
```

### Verify Installation

```bash
allure --version
```

## Configuration

### Playwright Configuration

Allure integrates with Playwright through custom reporter:

```typescript
// playwright.config.ts
import { defineConfig } from '@playwright/test'

export default defineConfig({
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['allure-file', { outputDir: './allure-results' }],
  ],
})
```

### Allure Utility Functions

```typescript
// tests/framework/allure-utils.ts
import { test } from './allure-utils'

test('Test with Allure', async ({ page, allure }) => {
  // Add test metadata
  allure.epic('E2E Tests')
  allure.feature('User Authentication')
  allure.story('Login')
  allure.severity('critical')
  allure.owner('QA Team')
  allure.tag('smoke', 'authentication')

  // Add test steps
  await allure.step('Navigate to login page', async () => {
    await page.goto('/login')
  })

  // Attach screenshot
  await allure.attachment('Login Page', await page.screenshot())

  // Test assertions
  await expect(page.locator('h1')).toContainText('Login')
})
```

## Running Tests with Allure

### Run Specific Test Type

```bash
# E2E tests
pnpm exec playwright test --reporter=line,allure-file

# Smoke tests
pnpm exec playwright test --project=smoke --reporter=line,allure-file

# Security tests
pnpm exec playwright test --project=security --reporter=line,allure-file

# Resilience tests
pnpm exec playwright test --project=resilience --reporter=line,allure-file
```

### Run All Tests and Generate Report

```bash
# Run all tests with Allure
./generate-allure-report.sh run all

# Run and clean old results
./generate-allure-report.sh run all clean
```

### Generate Report from Existing Results

```bash
# Generate report
pnpm run test:report

# Or use script
./generate-allure-report.sh generate
```

### Serve Report

```bash
# Start report server on port 5050
pnpm run test:report:serve

# Or use script
./generate-allure-report.sh serve

# Open in browser
open http://localhost:5050
```

## Using Allure in Tests

### Basic Test with Allure

```typescript
import { test } from '../framework/allure-utils'

test('User can login', async ({ page, allure }) => {
  // Add metadata
  allure.epic('Authentication')
  allure.feature('Login')
  allure.story('Valid credentials')
  allure.severity('critical')
  allure.owner('QA Team')
  allure.tag('smoke')

  // Add test step
  await allure.step('Navigate to login page', async () => {
    await page.goto('/login')
  })

  await allure.step('Enter credentials', async () => {
    await page.fill('[data-testid="username"]', 'admin')
    await page.fill('[data-testid="password"]', 'password')
  })

  await allure.step('Submit form', async () => {
    await page.click('[data-testid="login-button"]')
  })

  await allure.step('Verify redirect to dashboard', async () => {
    await expect(page).toHaveURL('/dashboard')
    await expect(page.locator('h1')).toContainText('Dashboard')
  })
})
```

### Test with Attachments

```typescript
test('Screenshot on failure', async ({ page, allure }) => {
  allure.description('Test demonstrates screenshot attachment on failure')

  await page.goto('/login')

  // Attach initial screenshot
  await allure.attachment('Initial Login Page', await page.screenshot())

  try {
    await page.fill('[data-testid="username"]', 'invalid')
    await page.fill('[data-testid="password"]', 'invalid')
    await page.click('[data-testid="login-button"]')

    // This will fail
    await expect(page.locator('[data-testid="error"]')).toContainText('Invalid')
  } catch (error) {
    // Attach screenshot on failure
    await allure.attachment('Error Screenshot', await page.screenshot())
    throw error
  }
})
```

### Test with Steps and Parameters

```typescript
test.describe('Parameterized Test', () => {
  const users = [
    { username: 'admin', role: 'Administrator' },
    { username: 'user1', role: 'User' },
  ]

  users.forEach((user) => {
    test(`User ${user.username} can access dashboard`, async ({ page, allure }) => {
      allure.parameter('username', user.username)
      allure.parameter('role', user.role)

      await page.goto('/login')
      await page.fill('[data-testid="username"]', user.username)
      await page.fill('[data-testid="password"]', 'password')
      await page.click('[data-testid="login-button"]')

      await expect(page).toHaveURL('/dashboard')
    })
  })
})
```

### Test with Custom Labels

```typescript
test('API test with custom labels', async ({ request, allure }) => {
  allure.epic('API Tests')
  allure.feature('Customer API')
  allure.story('GET /api/customers')
  allure.severity('normal')
  allure.tag('api', 'customers', 'get')

  const response = await request.get('/api/customers')
  expect(response.status()).toBe(200)

  const data = await response.json()
  expect(data).toBeInstanceOf(Array)
})
```

## Test Organization

### Epic/Feature/Story Structure

```typescript
// Epic: High-level business goal
allure.epic('Customer Management')

// Feature: Specific functionality
allure.feature('Customer CRUD Operations')

// Story: User story
allure.story('As a user, I can create a customer')
```

### Severity Levels

```typescript
allure.severity('blocker')   // Critical, blocks release
allure.severity('critical')  // High priority
allure.severity('normal')    // Default severity
allure.severity('minor')     // Low priority
allure.severity('trivial')   // Very low priority
```

### Owner and Tags

```typescript
allure.owner('QA Team')      // Assign ownership
allure.tag('smoke')          // Tag for filtering
allure.tag('authentication')
allure.tag('regression')
```

### Custom Labels

```typescript
allure.label('testType', 'e2e')
allure.label('component', 'auth')
allure.label('priority', 'high')
```

## Test Categories

### Automatic Categories

Allure automatically categorizes tests:
- **Product defects** - Tests that revealed product bugs
- **Test defects** - Flaky or broken tests
- **Passed** - Successful tests
- **Skipped** - Skipped tests

### Custom Categories

Create `categories.json` in `allure-results` directory:

```json
[
  {
    "name": "Ignored tests",
    "messageRegex": ".*_skipped.*",
    "matchedStatuses": ["skipped"]
  },
  {
    "name": "Infrastructure issues",
    "messageRegex": ".*connection.*",
    "matchedStatuses": ["broken", "failed"]
  },
  {
    "name": "Test performance",
    "durationRegex": ".*5000.*",
    "matchedStatuses": ["passed"]
  }
]
```

## Environment Information

Add environment information to reports:

```typescript
// Add environment info
import { addEnvironmentInfo } from '../framework/allure-utils'

addEnvironmentInfo({
  Browser: 'Chromium',
  BrowserVersion: '120.0.0.0',
  Platform: 'Linux',
  BaseURL: 'http://localhost:3000',
  NodeVersion: 'v20.10.0',
})
```

Or create `environment.properties` file:

```properties
Browser=Chromium
BrowserVersion=120.0.0.0
Platform=Linux
BaseURL=http://localhost:3000
NodeVersion=v20.10.0
BuildVersion=1.0.0
```

## Report Structure

### Main Dashboard

**Overview:**
- Total tests count
- Passed/Failed/Skipped counts
- Pass rate percentage
- Test duration

**Graphs:**
- Status pie chart
- Duration distribution
- Trend over time
- Retry statistics

### Test Results

**List View:**
- Test names with status
- Duration
- Timestamp
- Assignee
- Labels

**Detailed View:**
- Test description
- Steps with status
- Attachments
- Parameters
- Links
- Test history

### Categories

**Defects:**
- Product defects
- Test defects
- Broken tests

**Flaky Tests:**
- Unstable tests
- Intermittent failures

### Timeline

**Execution Timeline:**
- Test execution order
- Parallel execution visualization
- Duration per test
- Gaps and overlaps

## CI/CD Integration

### GitHub Actions

```yaml
name: Test Report

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test-and-report:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '20'

      - name: Install dependencies
        run: npm install

      - name: Install Allure
        run: ./generate-allure-report.sh install

      - name: Run tests
        run: ./generate-allure-report.sh run all clean

      - name: Generate report
        run: ./generate-allure-report.sh generate

      - name: Upload Allure report
        uses: actions/upload-artifact@v3
        with:
          name: allure-report
          path: allure-report/

      - name: Deploy to GitHub Pages
        if: github.ref == 'refs/heads/main'
        uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./allure-report
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any

    stages {
        stage('Test') {
            steps {
                sh 'npm install'
                sh './generate-allure-report.sh run all clean'
            }
        }

        stage('Report') {
            steps {
                sh './generate-allure-report.sh generate'

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

## Advanced Features

### Custom Reporter

```typescript
// custom-allure-reporter.ts
import { FullResult, TestResult } from '@playwright/test/reporter'

class CustomAllureReporter {
  onBegin(config, suite) {
    console.log(`Starting test run with ${suite.allTests().length} tests`)
  }

  onTestEnd(test, result) {
    console.log(`Finished test ${test.title} with status ${result.status}`)
  }

  onEnd(result) {
    console.log(`Test run finished with status ${result.status}`)
  }
}

export default CustomAllureReporter
```

### Environment Setup

```bash
# Add to your test setup
beforeEach(async () => {
  addEnvironmentInfo({
    CI: process.env.CI || 'false',
    BuildNumber: process.env.BUILD_NUMBER || 'local',
    Commit: process.env.GITHUB_SHA || 'unknown',
  })
})
```

### Filtering Tests

```bash
# Run tests with specific tags
allure serve allure-results -D tags=smoke

# Run tests with specific severity
allure serve allure-results -D severity=critical

# Run tests by owner
allure serve allure-results -D owner=QA Team
```

## Troubleshooting

### No Report Generated

**Problem:** No report after running tests

**Solution:**
1. Check if `allure-results` directory exists
2. Verify tests generated results
3. Run with verbose output: `allure serve allure-results -v`

### Allure Not Found

**Problem:** `allure: command not found`

**Solution:**
```bash
# Install Allure
./generate-allure-report.sh install

# Or add to PATH
export PATH=$PATH:/opt/allure-2.24.1/bin
```

### Empty Report

**Problem:** Report generated but no tests shown

**Solution:**
1. Check if results directory is correct
2. Verify test execution completed
3. Check for errors in test output

### Port Already in Use

**Problem:** Port 5050 is already in use

**Solution:**
```bash
# Use different port
allure serve allure-results -p 5051
```

## Best Practices

### 1. Test Organization
- Use Epic/Feature/Story hierarchy consistently
- Add meaningful test descriptions
- Use appropriate severity levels
- Tag tests for easy filtering

### 2. Attachments
- Attach screenshots on failure
- Add console logs for debugging
- Include network traces
- Document steps with attachments

### 3. Metadata
- Set owner for each test
- Add environment information
- Use custom labels for categorization
- Include links to test cases

### 4. Performance
- Don't attach large files unnecessarily
- Compress images before attaching
- Use appropriate file types
- Clean old reports regularly

### 5. CI/CD Integration
- Archive reports as artifacts
- Deploy to static hosting
- Track trends over time
- Monitor flaky tests

## Environment Variables

```bash
# Allure directories
export ALLURE_RESULTS_DIR=./allure-results
export ALLURE_REPORT_DIR=./allure-report
export HISTORY_DIR=./allure-history

# Allure version
export ALLURE_VERSION=2.24.1
```

## Files and Directories

```
tests/
├── framework/
│   └── allure-utils.ts           # Allure utility functions
├── reporting/
│   └── README.md                 # This file
├── allure.config.ts              # Allure configuration

allure-results/                   # Test results (auto-generated)
allure-report/                    # Generated reports (auto-generated)
allure-history/                   # History data (auto-generated)

generate-allure-report.sh         # Report generator script
```

## Quick Reference

### Common Commands

```bash
# Install Allure
./generate-allure-report.sh install

# Run tests and generate report
./generate-allure-report.sh run all

# Generate report from existing results
./generate-allure-report.sh generate

# Serve report
./generate-allure-report.sh serve

# Check Allure installation
./generate-allure-report.sh check
```

### Allure Functions

```typescript
// Metadata
allure.epic('Epic Name')
allure.feature('Feature Name')
allure.story('Story Name')
allure.severity('critical')
allure.owner('Owner Name')
allure.tag('tag1', 'tag2')
allure.label('name', 'value')

// Steps
await allure.step('Step name', async () => {
  // Test code
})

// Attachments
await allure.attachment('Name', screenshotBuffer)
await allure.attachment('Name', fileContent, {
  contentType: 'application/json'
})

// Parameters
allure.parameter('name', 'value')

// Links
allure.link('https://example.com', 'Documentation')
```

## Resources

- [Allure Documentation](https://docs.qameta.io/allure/)
- [Allure Report](https://github.com/allure-framework/allure2)
- [Playwright Allure Integration](https://playwright.dev/docs/test-reporters#allure)
- [Allure Example](https://github.com/allure-examples)

## Support

For issues:
1. Check Allure documentation
2. Verify installation
3. Review test output
4. Check CI/CD configuration

---

**Last Updated:** 2025-11-06
**Test Reports:** HTML with interactive dashboards
**Attachments:** Screenshots, logs, videos
**History:** Trend analysis and tracking
**CI/CD:** GitHub Actions, Jenkins integration
