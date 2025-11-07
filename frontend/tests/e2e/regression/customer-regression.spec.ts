/**
 * Customer Regression Tests - Comprehensive Test Suite
 *
 * These tests cover comprehensive customer management scenarios including:
 * - Edge cases
 * - Negative tests
 * - Boundary conditions
 * - Error handling
 * - Data validation
 * - Complex workflows
 */

import { test, expect } from '@playwright/test'
import { TestDataGenerator } from '@tests/framework/data-factories'

test.describe('Customer Regression Tests - Comprehensive', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Create test data for each test
    const testData = TestDataGenerator.fullCustomerJourney()
    await TestDataGenerator.seedTestData(testData)
  })

  test.afterEach(async ({ page }) => {
    // Cleanup test data
    await TestDataGenerator.cleanupTestData()
  })

  // ==================== EDGE CASES ====================

  test('REGRESSION-001: Should handle very long customer names', async ({ page }) => {
    await page.goto('/customers')

    const longName = 'A'.repeat(255) // Maximum length
    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', longName)
    await page.fill('[name="lastName"]', longName)
    await page.fill('[name="email"]', `test-${Date.now()}@example.com`)
    await page.click('[data-testid="submit-button"]')

    // Should either accept or show appropriate validation
    await expect(page.locator('[data-testid="success-message"], [data-testid="error-message"]')).toBeVisible()
  })

  test('REGRESSION-002: Should handle special characters in names', async ({ page }) => {
    await page.goto('/customers')

    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', "O'Brien-Smith")
    await page.fill('[name="lastName"]', 'García-López')
    await page.fill('[name="email"]', `special-${Date.now()}@example.com`)
    await page.fill('[name="phone"]', '+1-555-0123')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  test('REGRESSION-003: Should handle unicode characters', async ({ page }) => {
    await page.goto('/customers')

    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', 'Έλληνας')
    await page.fill('[name="lastName"]', 'Русский')
    await page.fill('[name="email"]', `unicode-${Date.now()}@example.com`)
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  test('REGRESSION-004: Should handle empty string in optional fields', async ({ page }) => {
    await page.goto('/customers')

    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', 'John')
    await page.fill('[name="lastName"]', 'Doe')
    await page.fill('[name="email"]', `optional-${Date.now()}@example.com`)
    // Leave phone empty (should be optional)
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  test('REGRESSION-005: Should handle concurrent customer creation', async ({ page }) => {
    await page.goto('/customers')

    // Create customer in one tab
    const pagePromise = page.context().newPage()
    const newPage = await pagePromise

    await newPage.goto('/customers')
    await newPage.click('[data-testid="create-customer-button"]')
    await newPage.fill('[name="firstName"]', 'Concurrent')
    await newPage.fill('[name="lastName"]', 'User')
    await newPage.fill('[name="email"]', `concurrent-${Date.now()}@example.com`)
    await newPage.click('[data-testid="submit-button"]')

    // Create customer in main tab
    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', 'Another')
    await page.fill('[name="lastName"]', 'User')
    await page.fill('[name="email"]', `another-${Date.now()}@example.com`)
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  // ==================== NEGATIVE TESTS ====================

  test('REGRESSION-006: Should reject invalid email formats', async ({ page }) => {
    await page.goto('/customers')

    const invalidEmails = [
      'plainaddress',
      '@missingdomain.com',
      'missing@',
      'spaces in@email.com',
      'double@@domain.com',
      'user@domain'
    ]

    for (const email of invalidEmails) {
      await page.click('[data-testid="create-customer-button"]')
      await page.fill('[name="firstName"]', 'Test')
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', email)
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="error-message"]')).toContainText(/email/i)
      await page.click('[data-testid="cancel-button"]')
    }
  })

  test('REGRESSION-007: Should reject invalid phone formats', async ({ page }) => {
    await page.goto('/customers')

    const invalidPhones = [
      '123',
      'abcdefghijk',
      '+',
      '++1234567890',
      '123-456-7890-extra'
    ]

    for (const phone of invalidPhones) {
      await page.click('[data-testid="create-customer-button"]')
      await page.fill('[name="firstName"]', 'Test')
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', `phone-${Date.now()}@example.com`)
      await page.fill('[name="phone"]', phone)
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator('[data-testid="error-message"], [data-testid="error-phone"]'))
        .toBeVisible()
      await page.click('[data-testid="cancel-button"]')
    }
  })

  test('REGRESSION-008: Should reject duplicate email on update', async ({ page }) => {
    // Create two customers
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', 'User1')
    await page.fill('[name="lastName"]', 'One')
    await page.fill('[name="email"]', 'user1@example.com')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', 'User2')
    await page.fill('[name="lastName"]', 'Two')
    await page.fill('[name="email"]', 'user2@example.com')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Try to update User2 with User1's email
    await page.click('[data-testid="customer-row-1"] [data-testid="edit-button"]')
    await page.fill('[name="email"]', 'user1@example.com')
    await page.click('[data-testid="submit-button"]')

    await expect(page.locator('[data-testid="error-message"]')).toContainText(/duplicate/i)
  })

  test('REGRESSION-009: Should reject creation with missing required fields', async ({ page }) => {
    await page.goto('/customers')

    const requiredFields = [
      { field: 'firstName', value: 'Valid', skipField: 'firstName' },
      { field: 'lastName', value: 'Valid', skipField: 'lastName' },
      { field: 'email', value: 'valid@example.com', skipField: 'email' }
    ]

    for (const scenario of requiredFields) {
      await page.click('[data-testid="create-customer-button"]')
      await page.fill('[name="firstName"]', 'Test')
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', 'test@example.com')
      await page.fill(`[name="${scenario.field}"]`, scenario.value)
      await page.clear(`[name="${scenario.skipField}"]`)
      await page.click('[data-testid="submit-button"]')

      await expect(page.locator(`[data-testid="error-${scenario.skipField}"]`)).toBeVisible()
      await page.click('[data-testid="cancel-button"]')
    }
  })

  test('REGRESSION-010: Should handle 404 for non-existent customer', async ({ page }) => {
    const nonExistentId = '00000000-0000-0000-0000-000000000000'
    await page.goto(`/customers/${nonExistentId}`)

    await expect(page.locator('h1')).toContainText(/404|not found/i)
  })

  // ==================== BOUNDARY CONDITIONS ====================

  test('REGRESSION-011: Should handle search with single character', async ({ page }) => {
    await page.goto('/customers')

    // Create customers with different starting letters
    for (let i = 0; i < 3; i++) {
      await page.click('[data-testid="create-customer-button"]')
      await page.fill('[name="firstName']', `A${i}`)
      await page.fill('[name="lastName']', 'User')
      await page.fill('[name="email']', `a${i}@example.com`)
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    // Search with single character
    await page.fill('[data-testid="search-input"]', 'A')
    await page.press('[data-testid="search-input"]', 'Enter')

    await expect(page.locator('[data-testid="customer-row"]')).toHaveCount(3)
  })

  test('REGRESSION-012: Should handle search with very long query', async ({ page }) => {
    await page.goto('/customers')

    const longQuery = 'A'.repeat(1000)
    await page.fill('[data-testid="search-input"]', longQuery)
    await page.press('[data-testid="search-input"]', 'Enter')

    // Should show no results or handle gracefully
    await expect(page.locator('[data-testid="no-results"], [data-testid="customer-list"]')).toBeVisible()
  })

  test('REGRESSION-013: Should handle pagination at boundaries', async ({ page }) => {
    await page.goto('/customers')

    // Create exactly enough customers to test pagination
    const pageSize = 20
    for (let i = 0; i < pageSize + 5; i++) {
      await page.click('[data-testid="create-customer-button"]')
      await page.fill('[name="firstName"]', `User${i}`)
      await page.fill('[name="lastName"]', 'Test')
      await page.fill('[name="email"]', `user${i}@example.com`)
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    // Go to second page
    await page.click('[data-testid="next-page"]')
    await expect(page.locator('[data-testid="customer-row"]')).toHaveCount(5)

    // Go back to first page
    await page.click('[data-testid="prev-page"]')
    await expect(page.locator('[data-testid="customer-row"]')).toHaveCount(pageSize)
  })

  test('REGRESSION-014: Should handle negative page numbers', async ({ page }) => {
    await page.goto('/customers?page=-1')

    // Should handle gracefully (either show page 1 or error)
    await expect(page.locator('[data-testid="customer-list"], [data-testid="error-message"]')).toBeVisible()
  })

  // ==================== WORKFLOW TESTS ====================

  test('REGRESSION-015: Should complete full customer lifecycle', async ({ page }) => {
    const customerData = {
      firstName: 'Lifecycle',
      lastName: 'Test',
      email: `lifecycle-${Date.now()}@example.com`,
      phone: '+1-555-0123'
    }

    // 1. Create customer
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', customerData.firstName)
    await page.fill('[name="lastName"]', customerData.lastName)
    await page.fill('[name="email"]', customerData.email)
    await page.fill('[name="phone"]', customerData.phone)
    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()

    // 2. View customer
    await page.click('[data-testid="customer-row-0"] [data-testid="view-button"]')
    await expect(page.locator('[data-testid="customer-details"]')).toContainText(customerData.firstName)

    // 3. Edit customer
    await page.click('[data-testid="edit-button"]')
    await page.fill('[name="phone"]', '+1-555-9999')
    await page.click('[data-testid="submit-button"]')
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/updated/i)

    // 4. Search for updated customer
    await page.goto('/customers')
    await page.fill('[data-testid="search-input"]', customerData.firstName)
    await page.press('[data-testid="search-input"]', 'Enter')
    await expect(page.locator('[data-testid="customer-row"]')).toBeVisible()

    // 5. Delete customer
    await page.click('[data-testid="customer-row-0"] [data-testid="delete-button"]')
    await page.click('[data-testid="confirm-delete"]')
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/deleted/i)
  })

  test('REGRESSION-016: Should handle bulk operations', async ({ page }) => {
    await page.goto('/customers')

    // Create multiple customers
    for (let i = 0; i < 5; i++) {
      await page.click('[data-testid="create-customer-button"]')
      await page.fill('[name="firstName"]', `Bulk${i}`)
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', `bulk${i}@example.com`)
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    // Select multiple customers
    for (let i = 0; i < 3; i++) {
      await page.click(`[data-testid="customer-row-${i}"] [data-testid="select-checkbox"]`)
    }

    // Bulk actions should be available
    await expect(page.locator('[data-testid="bulk-actions"]')).toBeVisible()
  })

  // ==================== DATA CONSISTENCY ====================

  test('REGRESSION-017: Should maintain sort order after operation', async ({ page }) => {
    await page.goto('/customers')

    // Create customers in random order
    const names = ['Zebra', 'Apple', 'Banana', 'Dog']
    for (const name of names) {
      await page.click('[data-testid="create-customer-button"]')
      await page.fill('[name="firstName"]', name)
      await page.fill('[name="lastName"]', 'Test')
      await page.fill('[name="email"]', `${name.toLowerCase()}@example.com`)
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    // Sort by first name
    await page.click('[data-testid="sort-firstName"]')

    // Perform an operation
    await page.click('[data-testid="customer-row-0"] [data-testid="view-button"]')
    await page.goBack()

    // Sort order should be maintained
    await expect(page.locator('[data-testid="customer-row-0"] [data-testid="firstName"]')).toContainText(/Apple/)
  })

  test('REGRESSION-018: Should preserve filters after navigation', async ({ page }) => {
    await page.goto('/customers')

    // Apply filter
    await page.selectOption('[data-testid="status-filter"]', 'active')
    await page.waitForTimeout(500)

    // Navigate to customer details
    if (await page.locator('[data-testid="customer-row-0"] [data-testid="view-button"]').isVisible()) {
      await page.click('[data-testid="customer-row-0"] [data-testid="view-button"]')
      await page.goBack()

      // Filter should still be applied
      await expect(page.locator('[data-testid="status-filter"]')).toHaveValue('active')
    }
  })

  // ==================== PERFORMANCE ====================

  test('REGRESSION-019: Should load customer list with many items', async ({ page }) => {
    const startTime = Date.now()

    await page.goto('/customers')

    // Create 50 customers
    for (let i = 0; i < 50; i++) {
      await page.click('[data-testid="create-customer-button"]')
      await page.fill('[name="firstName"]', `Perf${i}`)
      await page.fill('[name="lastName"]', 'Test')
      await page.fill('[name="email"]', `perf${i}@example.com`)
      await page.click('[data-testid="submit-button"]')
      await page.waitForSelector('[data-testid="success-message"]')
    }

    const endTime = Date.now()
    const loadTime = endTime - startTime

    // Should complete within reasonable time (< 60 seconds for 50 customers)
    expect(loadTime).toBeLessThan(60000)
    await expect(page.locator('[data-testid="customer-row"]')).toHaveCount(50)
  })

  test('REGRESSION-020: Should handle rapid search queries', async ({ page }) => {
    await page.goto('/customers')

    // Create a customer
    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', 'Rapid')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', 'rapid@example.com')
    await page.click('[data-testid="submit-button"]')
    await page.waitForSelector('[data-testid="success-message"]')

    // Rapidly type in search
    const searchInput = page.locator('[data-testid="search-input"]')
    await searchInput.focus()
    await searchInput.type('R', { delay: 10 })
    await searchInput.type('a', { delay: 10 })
    await searchInput.type('p', { delay: 10 })
    await searchInput.type('i', { delay: 10 })
    await searchInput.press('Enter')

    await expect(page.locator('[data-testid="customer-row"]')).toBeVisible()
  })
})
