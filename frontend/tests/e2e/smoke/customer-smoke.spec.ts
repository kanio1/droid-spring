/**
 * Customer Smoke Tests - Critical Path Tests for Customer Management
 *
 * These tests cover the most critical customer management scenarios
 * that must work for the system to be considered functional
 */

import { test, expect } from '@playwright/test'

test.describe('Customer Smoke Tests - Critical Paths', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to application
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    // Authenticate (if needed)
    // await login(page, testUser)
  })

  test('SMOKE-001: Should display customer list page', async ({ page }) => {
    await page.goto('/customers')
    await expect(page.locator('h1')).toContainText(/customers/i)
    await expect(page.locator('[data-testid="customer-list"]')).toBeVisible()
  })

  test('SMOKE-002: Should create a new customer', async ({ page }) => {
    await page.goto('/customers')

    // Click create button
    await page.click('[data-testid="create-customer-button"]')

    // Fill form
    await page.fill('[name="firstName"]', 'John')
    await page.fill('[name="lastName"]', 'Doe')
    await page.fill('[name="email"]', 'john.doe@example.com')
    await page.fill('[name="phone"]', '+1-555-0123')

    // Submit
    await page.click('[data-testid="submit-button"]')

    // Verify success
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
  })

  test('SMOKE-003: Should view customer details', async ({ page }) => {
    // First create a customer
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', 'Jane')
    await page.fill('[name="lastName"]', 'Smith')
    await page.fill('[name="email"]', 'jane.smith@example.com')
    await page.click('[data-testid="submit-button"]')

    // View details
    await page.click('[data-testid="customer-row-0"] [data-testid="view-button"]')
    await expect(page.locator('h1')).toContainText(/jane smith/i)
    await expect(page.locator('[data-testid="customer-details"]')).toBeVisible()
  })

  test('SMOKE-004: Should search customers', async ({ page }) => {
    await page.goto('/customers')

    // Search for customer
    await page.fill('[data-testid="search-input"]', 'John')
    await page.press('[data-testid="search-input"]', 'Enter')

    // Verify results
    await expect(page.locator('[data-testid="customer-list"]')).toBeVisible()
    await expect(page.locator('text=John')).toBeVisible()
  })

  test('SMOKE-005: Should filter customers by status', async ({ page }) => {
    await page.goto('/customers')

    // Apply status filter
    await page.selectOption('[data-testid="status-filter"]', 'active')
    await page.waitForTimeout(500)

    // Verify filtered results
    const rows = page.locator('[data-testid="customer-row"]')
    await expect(rows.first()).toBeVisible()
  })

  test('SMOKE-006: Should edit customer information', async ({ page }) => {
    await page.goto('/customers')

    // Click edit on first customer
    await page.click('[data-testid="customer-row-0"] [data-testid="edit-button"]')

    // Update information
    await page.fill('[name="phone"]', '+1-555-9999')
    await page.click('[data-testid="submit-button"]')

    // Verify update
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
    await expect(page.locator('text=+1-555-9999')).toBeVisible()
  })

  test('SMOKE-007: Should delete a customer', async ({ page }) => {
    // First create a customer to delete
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', 'Delete')
    await page.fill('[name="lastName"]', 'Me')
    await page.fill('[name="email"]', 'delete@example.com')
    await page.click('[data-testid="submit-button"]')

    // Delete customer
    await page.click('[data-testid="customer-row-0"] [data-testid="delete-button"]')
    await page.click('[data-testid="confirm-delete"]')

    // Verify deletion
    await expect(page.locator('[data-testid="success-message"]')).toContainText(/deleted/i)
  })

  test('SMOKE-008: Should handle duplicate email error', async ({ page }) => {
    await page.goto('/customers')

    // Try to create customer with duplicate email
    await page.click('[data-testid="create-customer-button"]')
    await page.fill('[name="firstName"]', 'Duplicate')
    await page.fill('[name="lastName"]', 'Email')
    await page.fill('[name="email"]', 'existing@example.com') // Using existing email
    await page.click('[data-testid="submit-button"]')

    // Verify error message
    await expect(page.locator('[data-testid="error-message"]')).toContainText(/email/i)
  })

  test('SMOKE-009: Should validate required fields', async ({ page }) => {
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-button"]')

    // Submit empty form
    await page.click('[data-testid="submit-button"]')

    // Verify validation errors
    await expect(page.locator('[data-testid="error-firstName"]')).toBeVisible()
    await expect(page.locator('[data-testid="error-email"]')).toBeVisible()
  })

  test('SMOKE-010: Should display customer count in pagination', async ({ page }) => {
    await page.goto('/customers')

    // Check if pagination shows correct count
    const pagination = page.locator('[data-testid="pagination-info"]')
    if (await pagination.isVisible()) {
      await expect(pagination).toContainText(/\d+ customers/i)
    }
  })
})
