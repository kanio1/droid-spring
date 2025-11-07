/**
 * Allure Reporting Example Tests
 *
 * This file demonstrates various Allure reporting features
 */

import { test as base, expect } from '@playwright/test'
import { allureTest, addTestMetadata, attachScreenshot, addEnvironmentInfo } from '../framework/allure-utils'

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000'

// Test with full Allure metadata
allureTest('Login with valid credentials', async ({ page, allure }) => {
  // Add test metadata
  allure.epic('Authentication')
  allure.feature('User Login')
  allure.story('Valid credentials')
  allure.severity('critical')
  allure.owner('QA Team')
  allure.tag('smoke', 'authentication', 'login')

  await addEnvironmentInfo({
    browser: 'Chromium',
    baseUrl: BASE_URL,
  })

  // Test steps
  await allure.step('Navigate to login page', async () => {
    await page.goto(`${BASE_URL}/login`)
  })

  await allure.step('Enter valid credentials', async () => {
    await page.fill('[data-testid="username-input"]', 'admin')
    await page.fill('[data-testid="password-input"]', 'password')
  })

  await allure.step('Submit login form', async () => {
    await page.click('[data-testid="login-button"]')
  })

  await allure.step('Verify redirect to dashboard', async () => {
    await expect(page).toHaveURL(/.*dashboard.*/)
    await expect(page.locator('h1')).toContainText('Dashboard')
  })

  // Attach screenshot
  await attachScreenshot(page, 'Successful Login')
})

// Test with parameters
allureTest.describe('Parameterized Login Tests', () => {
  const testCases = [
    { username: 'admin', password: 'password', expectedRole: 'Administrator' },
    { username: 'user1', password: 'password123', expectedRole: 'User' },
  ]

  testCases.forEach((testCase) => {
    allureTest(`Login as ${testCase.username}`, async ({ page, allure }) => {
      allure.parameter('username', testCase.username)
      allure.parameter('expectedRole', testCase.expectedRole)

      await page.goto(`${BASE_URL}/login`)
      await page.fill('[data-testid="username-input"]', testCase.username)
      await page.fill('[data-testid="password-input"]', testCase.password)
      await page.click('[data-testid="login-button"]`)

      // Verify based on role
      if (testCase.expectedRole === 'Administrator') {
        await expect(page).toHaveURL(/.*admin.*/)
      } else {
        await expect(page).toHaveURL(/.*dashboard.*/)
      }
    })
  })
})

// Test with failure and attachment
allureTest('Login with invalid credentials shows error', async ({ page, allure }) => {
  allure.description('Test that invalid credentials show proper error message')
  allure.epic('Authentication')
  allure.feature('Error Handling')
  allure.story('Invalid credentials')
  allure.severity('normal')
  allure.tag('negative', 'authentication')

  await page.goto(`${BASE_URL}/login`)

  // Take screenshot before action
  await attachScreenshot(page, 'Before Login Attempt')

  await page.fill('[data-testid="username-input"]', 'invalid')
  await page.fill('[data-testid="password-input"]', 'wrong')

  await page.click('[data-testid="login-button"]')
  await page.waitForTimeout(1000)

  // Attach screenshot after error
  await attachScreenshot(page, 'Error Message Displayed')

  // Verify error message
  const errorMessage = page.locator('[data-testid="error-message"]')
  await expect(errorMessage).toBeVisible()
  await expect(errorMessage).toContainText(/invalid|incorrect|error/i)
})

// Test with custom labels
allureTest('API health check', async ({ request, allure }) => {
  allure.epic('API Tests')
  allure.feature('Health Checks')
  allure.story('API availability')
  allure.severity('critical')
  allure.owner('DevOps Team')
  allure.tag('api', 'health', 'critical')
  allure.label('testType', 'api')
  allure.label('component', 'backend')

  const response = await request.get(`${BASE_URL}/health`)

  await allure.step('Verify health endpoint responds', async () => {
    expect(response.status()).toBeLessThan(500)
  })

  await allure.step('Verify health status is UP', async () => {
    const data = await response.json()
    expect(data.status).toBe('UP')
  })
})

// Test with network monitoring
allureTest('Customer list loads with API calls', async ({ page, allure }) => {
  allure.epic('Customer Management')
  allure.feature('Customer List')
  allure.story('List view')
  allure.severity('normal')
  allure.tag('customer', 'list', 'ui')

  const requests: any[] = []
  const responses: any[] = []

  // Monitor network
  page.on('request', (request) => {
    requests.push({
      url: request.url(),
      method: request.method(),
    })
  })

  page.on('response', (response) => {
    responses.push({
      url: response.url(),
      status: response.status(),
    })
  })

  await page.goto(`${BASE_URL}/customers`)

  await allure.step('Verify page loaded', async () => {
    await expect(page.locator('h1')).toContainText('Customers')
  })

  await allure.step('Verify API calls were made', async () => {
    expect(requests.length).toBeGreaterThan(0)
    expect(responses.length).toBeGreaterThan(0)
  })

  // Attach network summary
  const networkSummary = {
    requests: requests.length,
    responses: responses.length,
    apiCalls: responses.filter(r => r.url.includes('/api/')).length,
  }

  await allure.attachment(
    'Network Summary',
    JSON.stringify(networkSummary, null, 2),
    { contentType: 'application/json' }
  )
})

// Test with multiple steps
allureTest('Complete customer creation workflow', async ({ page, allure }) => {
  allure.epic('Customer Management')
  allure.feature('Customer CRUD')
  allure.story('Create customer')
  allure.severity('critical')
  allure.tag('customer', 'create', 'workflow')

  await addTestMetadata(test.info(), {
    epic: 'Customer Management',
    feature: 'Customer CRUD',
    story: 'As a user, I can create a new customer',
    severity: 'critical',
    owner: 'QA Team',
    tags: ['customer', 'create', 'workflow'],
    description: 'This test verifies the complete customer creation process',
  })

  // Step 1: Navigate to customer list
  await allure.step('Navigate to customers page', async () => {
    await page.goto(`${BASE_URL}/customers`)
    await expect(page.locator('h1')).toContainText('Customers')
  })

  // Step 2: Click add button
  await allure.step('Click add customer button', async () => {
    const addButton = page.locator('[data-testid="add-customer-button"]')
    await addButton.click()
  })

  // Step 3: Fill form
  await allure.step('Fill customer form', async () => {
    await page.fill('[data-testid="firstName"]', 'John')
    await page.fill('[data-testid="lastName"]', 'Doe')
    await page.fill('[data-testid="email"]', 'john.doe@example.com')
  })

  // Step 4: Submit form
  await allure.step('Submit form', async () => {
    await page.click('[data-testid="save-button"]')
  })

  // Step 5: Verify success
  await allure.step('Verify customer was created', async () => {
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  // Step 6: Verify in list
  await allure.step('Verify customer appears in list', async () => {
    await expect(page.locator('text=John Doe')).toBeVisible()
  })
})

// Test with console log monitoring
allureTest('No console errors during navigation', async ({ page, allure }) => {
  allure.epic('Quality Assurance')
  allure.feature('Error Monitoring')
  allure.story('Console errors')
  allure.severity('minor')
  allure.tag('quality', 'console')

  const consoleMessages: string[] = []
  page.on('console', (msg) => {
    consoleMessages.push(`${msg.type()}: ${msg.text()}`)
  })

  // Navigate to different pages
  await page.goto(`${BASE_URL}/`)
  await page.waitForTimeout(500)

  await page.goto(`${BASE_URL}/customers`)
  await page.waitForTimeout(500)

  await page.goto(`${BASE_URL}/orders`)
  await page.waitForTimeout(500)

  // Check for errors
  const errors = consoleMessages.filter(msg => msg.includes('error') || msg.includes('Error'))

  await allure.step('Check for console errors', async () => {
    expect(errors.length).toBe(0)
  })

  // Attach console log summary
  if (consoleMessages.length > 0) {
    await allure.attachment(
      'Console Logs',
      consoleMessages.join('\n'),
      { contentType: 'text/plain' }
    )
  }
})

// Test with video attachment (if available)
allureTest('Login flow recorded', async ({ page, allure }) => {
  allure.epic('Authentication')
  allure.feature('Login')
  allure.story('Login flow video')
  allure.severity('normal')
  allure.tag('video', 'login')

  await page.goto(`${BASE_URL}/login`)

  // Start video recording is automatic in Playwright
  // Just need to ensure video is enabled in config

  await page.fill('[data-testid="username-input"]', 'admin')
  await page.fill('[data-testid="password-input"]', 'password')
  await page.click('[data-testid="login-button"]')
  await page.waitForURL(/.*dashboard.*/)

  // Note: Video attachment would be handled by Playwright's video feature
  // This is a placeholder showing how it would be done
  console.log('[Allure] Video recording available in Playwright report')
})

// Test with performance metrics
allureTest('Page load performance', async ({ page, allure }) => {
  allure.epic('Performance')
  allure.feature('Load Time')
  allure.story('Dashboard load time')
  allure.severity('normal')
  allure.tag('performance', 'load-time')

  const startTime = Date.now()

  await page.goto(`${BASE_URL}/dashboard`)

  await page.waitForLoadState('networkidle')

  const loadTime = Date.now() - startTime

  await allure.step('Measure page load time', async () => {
    expect(loadTime).toBeLessThan(5000) // Should load in < 5 seconds
  })

  // Attach performance metrics
  const metrics = await page.evaluate(() => {
    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming
    return {
      loadTime: loadTime,
      domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      firstPaint: performance.getEntriesByName('first-paint')[0]?.startTime || 0,
      largestContentfulPaint: performance.getEntriesByName('largest-contentful-paint')[0]?.startTime || 0,
    }
  })

  await allure.attachment(
    'Performance Metrics',
    JSON.stringify(metrics, null, 2),
    { contentType: 'application/json' }
  )
})

// Test with links
allureTest('Documentation link works', async ({ page, allure }) => {
  allure.epic('Navigation')
  allure.feature('Links')
  allure.story('External links')
  allure.severity('minor')
  allure.tag('links', 'documentation')

  await page.goto(`${BASE_URL}/login`)

  // Add link metadata
  allure.link('https://docs.example.com', 'User Guide', 'guide')
  allure.link('https://github.com/example/project', 'Source Code', 'source')

  const docLink = page.locator('a[href*="docs"]')
  await expect(docLink).toBeVisible()

  // In a real test, you might verify the link is valid
  console.log('[Allure] External links documented with metadata')
})

// Test with environment information
allureTest('Environment-specific test', async ({ page, allure }) => {
  const env = process.env.NODE_ENV || 'development'
  const baseUrl = process.env.BASE_URL || 'http://localhost:3000'

  allure.epic('Environment Tests')
  allure.feature('Environment Validation')
  allure.story(`Test in ${env} environment`)
  allure.severity('normal')
  allure.tag('environment', env)

  // Add environment-specific info
  addEnvironmentInfo({
    environment: env,
    baseUrl: baseUrl,
    nodeEnv: process.env.NODE_ENV || 'development',
    buildNumber: process.env.BUILD_NUMBER || 'local',
  })

  await page.goto(baseUrl)

  // Test logic specific to environment
  if (env === 'development') {
    await expect(page.locator('text=Development Mode')).toBeVisible()
  }

  console.log(`[Allure] Running in ${env} environment`)
})
