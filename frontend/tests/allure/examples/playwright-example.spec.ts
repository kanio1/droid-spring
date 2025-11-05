/**
 * Allure Playwright Example Tests
 *
 * Demonstrates how to use Allure reporting with Playwright
 *
 * Run with:
 * npx playwright test tests/allure/examples/playwright-example.spec.ts --reporter=line,allure-playwright
 */

import { test, expect } from '@playwright/test'
import { AllureTestWrapper } from '@/tests/allure/allure.config'

// Configure test with Allure
test.describe('Customer Management @customers', () => {
  test.beforeEach(async ({ page }) => {
    AllureTestWrapper.epic('Customer Management')
    AllureTestWrapper.feature('Customer CRUD Operations')

    // Add environment info
    console.log('🔧 Running test on:', process.platform)
  })

  test('should create new customer @critical', async ({ page }) => {
    AllureTestWrapper.severity('critical')
    AllureTestWrapper.story('Create Customer')
    AllureTestWrapper.owner('Tech Lead')
    AllureTestWrapper.tag('create')

    // Step 1: Navigate to customers page
    await AllureTestWrapper.step('Navigate to customers page', async () => {
      await page.goto('/customers')
      await expect(page).toHaveTitle(/Customers/)
    })

    // Step 2: Click create button
    await AllureTestWrapper.step('Click create customer button', async () => {
      await page.click('[data-testid="create-customer-button"]')
    })

    // Step 3: Fill form
    await AllureTestWrapper.step('Fill customer form', async () => {
      await page.fill('[data-testid="firstName-input"]', 'John')
      await page.fill('[data-testid="lastName-input"]', 'Doe')
      await page.fill('[data-testid="email-input"]', 'john.doe@example.com')
      await page.fill('[data-testid="phone-input"]', '+1234567890')
    })

    // Step 4: Attach screenshot of filled form
    await AllureTestWrapper.attach('Filled Form', async () => {
      const path = 'test-results/screenshots/filled-form.png'
      await page.screenshot({ path, fullPage: true })
      return path
    })

    // Step 5: Submit form
    await AllureTestWrapper.step('Submit form', async () => {
      await page.click('[data-testid="submit-button"]')
    })

    // Step 6: Verify success
    await AllureTestWrapper.step('Verify customer created', async () => {
      await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
    })

    // Add issue link
    AllureTestWrapper.issue('BSS-123', 'https://jira.company.com/browse/BSS-123')

    // Final assertion
    expect(await page.locator('[data-testid="customer-list"]')).toBeVisible()
  })

  test('should validate required fields @high', async ({ page }) => {
    AllureTestWrapper.severity('high')
    AllureTestWrapper.tag('validation')
    AllureTestWrapper.description('Test should validate all required fields and show appropriate errors')

    await page.goto('/customers/create')

    // Try to submit empty form
    await page.click('[data-testid="submit-button"]')

    // Attach screenshot of validation errors
    await AllureTestWrapper.attach('Validation Errors', async () => {
      const path = 'test-results/screenshots/validation-errors.png'
      await page.screenshot({ path })
      return path
    })

    // Verify error messages
    await AllureTestWrapper.step('Verify error messages are displayed', async () => {
      await expect(page.locator('[data-testid="firstName-error"]')).toContainText('required')
      await expect(page.locator('[data-testid="lastName-error"]')).toContainText('required')
      await expect(page.locator('[data-testid="email-error"]')).toContainText('required')
    })
  })

  test('should search for customer @medium', async ({ page }) => {
    AllureTestWrapper.severity('medium')
    AllureTestWrapper.story('Search Customer')

    await page.goto('/customers')

    // Search
    await AllureTestWrapper.step('Enter search term', async () => {
      await page.fill('[data-testid="search-input"]', 'John Doe')
    })

    await AllureTestWrapper.step('Click search button', async () => {
      await page.click('[data-testid="search-button"]')
    })

    // Wait for results
    await AllureTestWrapper.step('Verify search results', async () => {
      await expect(page.locator('[data-testid="customer-list"]')).toBeVisible()
    })

    // Attach results
    await AllureTestWrapper.attach('Search Results', async () => {
      const path = 'test-results/screenshots/search-results.png'
      await page.screenshot({ path, fullPage: true })
      return path
    })
  })
})

test.describe('Payment Processing @payments', () => {
  test('should process payment successfully', async ({ page }) => {
    AllureTestWrapper.epic('Payment Processing')
    AllureTestWrapper.feature('Payment Gateway')
    AllureTestWrapper.tag('payment', 'critical')

    await page.goto('/payments/create')

    await AllureTestWrapper.step('Enter payment amount', async () => {
      await page.fill('[data-testid="amount-input"]', '99.99')
    })

    await AllureTestWrapper.step('Select payment method', async () => {
      await page.selectOption('[data-testid="method-select"]', 'credit_card')
    })

    await AllureTestWrapper.step('Enter card details', async () => {
      await page.fill('[data-testid="card-number"]', '4242424242424242')
      await page.fill('[data-testid="card-expiry"]', '12/34')
      await page.fill('[data-testid="card-cvc"]', '123')
    })

    // Attach screenshot of payment form
    await AllureTestWrapper.attach('Payment Form', async () => {
      const path = 'test-results/screenshots/payment-form.png'
      await page.screenshot({ path })
      return path
    })

    await AllureTestWrapper.step('Submit payment', async () => {
      await page.click('[data-testid="pay-button"]')
    })

    // Wait for processing
    await AllureTestWrapper.step('Wait for payment processing', async () => {
      await page.waitForSelector('[data-testid="success-message"]', { timeout: 30000 })
    })

    // Attach success screenshot
    await AllureTestWrapper.attach('Payment Success', async () => {
      const path = 'test-results/screenshots/payment-success.png'
      await page.screenshot({ path })
      return path
    })
  })
})
