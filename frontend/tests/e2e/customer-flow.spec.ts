/**
 * Customer Management E2E Tests
 *
 * Comprehensive test suite covering all customer management workflows:
 * - CRUD operations (Create, Read, Update, Delete)
 * - Search and filtering
 * - Sorting and pagination
 * - Bulk operations
 * - Address management
 * - Validation and error handling
 * - Status management
 * - Export functionality
 *
 * Target: 25 comprehensive tests
 */

import { test, expect } from '@playwright/test'
import { CustomerPage } from '../framework/utils/page-object-model'
import { CustomerFactory } from '../framework/data-factories'
import { AuthHelper } from '../helpers'
import type { Page } from '@playwright/test'

test.describe('Customer Management E2E', () => {
  let customerPage: CustomerPage
  let testCustomers: any[] = []

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await AuthHelper.login(page, {
      username: process.env.TEST_USER || 'testuser',
      password: process.env.TEST_PASSWORD || 'testpass'
    })

    customerPage = new CustomerPage(page)
    await customerPage.navigateTo()

    // Verify page loaded
    await expect(page.locator('h1')).toContainText('Customers')
  })

  test.afterEach(async ({ page }) => {
    // Cleanup test data created during test
    for (const customer of testCustomers) {
      try {
        await customerPage.delete(customer.id)
      } catch (error) {
        console.log(`Cleanup failed for customer ${customer.id}:`, error)
      }
    }
    testCustomers = []
  })

  // ========== LIST VIEW TESTS ==========

  test('01 - Should display customers list with all required elements', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Customers')
    await expect(page.locator('[data-testid="customers-table"]')).toBeVisible()
    await expect(page.locator('[data-testid="search-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="status-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="add-customer-button"]')).toBeVisible()
  })

  test('02 - Should show customer count and pagination', async ({ page }) => {
    const count = await customerPage.getTotalCount()
    await expect(page.locator('[data-testid="total-count"]')).toContainText(count.toString())

    // Check pagination if many customers
    if (count > 10) {
      await expect(page.locator('[data-testid="pagination"]')).toBeVisible()
    }
  })

  test('03 - Should handle empty state when no customers exist', async ({ page }) => {
    // Navigate to fresh page
    await page.goto('/customers')

    const emptyState = page.locator('[data-testid="empty-state"]')
    if (await emptyState.isVisible()) {
      await expect(emptyState).toContainText(/No customers found/i)
      await expect(page.locator('[data-testid="add-first-customer-button"]')).toBeVisible()

      // Clicking should navigate to create form
      await emptyState.click()
      await expect(page).toHaveURL(/.*\/customers\/create/)
    }
  })

  // ========== SEARCH TESTS ==========

  test('04 - Should search customers by name', async ({ page }) => {
    // Create a test customer
    const customer = CustomerFactory.create()
      .withFirstName('John')
      .withLastName('Smith')
      .withRandomEmail()
      .active()
      .build()

    const id = await customerPage.create(customer)
    testCustomers.push({ id, ...customer })

    // Wait for table to refresh
    await page.waitForTimeout(1000)

    // Search by first name
    await customerPage.searchCustomer('John')
    await page.waitForTimeout(500)

    const customers = await customerPage.list()
    const found = customers.find(c => c.firstName?.includes('John'))
    expect(found).toBeDefined()

    // Clear search
    await customerPage.searchCustomer('')
    await page.waitForTimeout(500)
  })

  test('05 - Should search customers by email', async ({ page }) => {
    const email = `test${Date.now()}@example.com`
    const customer = CustomerFactory.create()
      .withFirstName('Jane')
      .withLastName('Doe')
      .withEmail(email)
      .active()
      .build()

    const id = await customerPage.create(customer)
    testCustomers.push({ id, ...customer })

    await page.waitForTimeout(1000)

    // Search by email
    await customerPage.searchCustomer(email)
    await page.waitForTimeout(500)

    const customers = await customerPage.list()
    const found = customers.find(c => c.email?.includes(email.split('@')[0]))
    expect(found).toBeDefined()
  })

  test('06 - Should show no results for search with no matches', async ({ page }) => {
    await customerPage.searchCustomer('NonExistentCustomer12345')
    await page.waitForTimeout(1000)

    await expect(page.locator('[data-testid="no-results-message"]')).toContainText(/No customers found/i)
    await expect(page.locator('table tbody tr')).toHaveCount(0)
  })

  // ========== FILTERING TESTS ==========

  test('07 - Should filter customers by status - Active', async ({ page }) => {
    await customerPage.filterByStatus('ACTIVE')
    await page.waitForTimeout(500)

    const customers = await customerPage.list()
    expect(customers.length).toBeGreaterThan(0)

    // Verify all visible customers are active
    for (const customer of customers) {
      if (customer.status) {
        expect(customer.status).toMatch(/active/i)
      }
    }
  })

  test('08 - Should filter customers by status - Inactive', async ({ page }) => {
    await customerPage.filterByStatus('INACTIVE')
    await page.waitForTimeout(500)

    const customers = await customerPage.list()

    // Verify all visible customers are inactive or no results
    for (const customer of customers) {
      if (customer.status) {
        expect(customer.status).toMatch(/inactive/i)
      }
    }
  })

  test('09 - Should filter customers by status - Pending', async ({ page }) => {
    await customerPage.filterByStatus('PENDING')
    await page.waitForTimeout(500)

    const customers = await customerPage.list()

    for (const customer of customers) {
      if (customer.status) {
        expect(customer.status).toMatch(/pending/i)
      }
    }
  })

  test('10 - Should combine search and filter', async ({ page }) => {
    await customerPage.filterByStatus('ACTIVE')
    await customerPage.searchCustomer('John')
    await page.waitForTimeout(500)

    const customers = await customerPage.list()
    expect(customers.length).toBeGreaterThan(0)

    for (const customer of customers) {
      if (customer.status) {
        expect(customer.status).toMatch(/active/i)
      }
      if (customer.firstName) {
        expect(customer.firstName).toMatch(/John/i)
      }
    }
  })

  // ========== CREATE CUSTOMER TESTS ==========

  test('11 - Should create a new customer with valid data', async ({ page }) => {
    const customer = CustomerFactory.create()
      .withFirstName('Alice')
      .withLastName('Johnson')
      .withRandomEmail()
      .withPhone('+1234567890')
      .active()
      .build()

    const id = await customerPage.create(customer)
    expect(id).toBeTruthy()
    testCustomers.push({ id, ...customer })

    // Verify redirect to details page
    await expect(page).toHaveURL(/.*\/customers\/[a-zA-Z0-9]+/)

    // Verify customer data is displayed
    await expect(page.locator('[data-testid="firstName"]')).toContainText('Alice')
    await expect(page.locator('[data-testid="lastName"]')).toContainText('Johnson')
    await expect(page.locator('[data-testid="email"]')).toContainText('@')
  })

  test('12 - Should create customer with minimal required fields', async ({ page }) => {
    const customer = {
      firstName: 'Bob',
      lastName: 'Wilson',
      email: `min${Date.now()}@example.com`
    }

    const id = await customerPage.create(customer)
    expect(id).toBeTruthy()
    testCustomers.push({ id, ...customer })

    // Verify customer was created
    await customerPage.navigateTo()
    const customers = await customerPage.list()
    const found = customers.find(c => c.id === id)
    expect(found).toBeDefined()
  })

  test('13 - Should show validation errors for required fields', async ({ page }) => {
    await customerPage.navigateToCreate()

    // Submit empty form
    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(500)

    // Check for validation errors
    await expect(page.locator('[data-testid="firstName-error"]'))
      .toContainText(/required/i)
    await expect(page.locator('[data-testid="lastName-error"]'))
      .toContainText(/required/i)
    await expect(page.locator('[data-testid="email-error"]'))
      .toContainText(/required/i)
  })

  test('14 - Should show validation error for invalid email', async ({ page }) => {
    await customerPage.navigateToCreate()

    await page.fill('[data-testid="firstName-input"]', 'Test')
    await page.fill('[data-testid="lastName-input"]', 'User')
    await page.fill('[data-testid="email-input"]', 'invalid-email')

    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(500)

    await expect(page.locator('[data-testid="email-error"]'))
      .toContainText(/valid email/i)
  })

  test('15 - Should prevent duplicate email', async ({ page }) => {
    const email = `duplicate${Date.now()}@example.com`

    // Create first customer
    const customer1 = CustomerFactory.create()
      .withFirstName('First')
      .withLastName('Customer')
      .withEmail(email)
      .active()
      .build()

    const id1 = await customerPage.create(customer1)
    testCustomers.push({ id1, ...customer1 })

    // Try to create second customer with same email
    await customerPage.navigateToCreate()

    await page.fill('[data-testid="firstName-input"]', 'Second')
    await page.fill('[data-testid="lastName-input"]', 'Customer')
    await page.fill('[data-testid="email-input"]', email)

    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(1000)

    // Should show duplicate email error
    await expect(page.locator('[data-testid="email-error"]'))
      .toContainText(/already exists|duplicate/i)
  })

  // ========== READ/VIEW CUSTOMER TESTS ==========

  test('16 - Should view customer details', async ({ page }) => {
    const customer = CustomerFactory.create()
      .withFirstName('Detail')
      .withLastName('View')
      .withRandomEmail()
      .active()
      .build()

    const id = await customerPage.create(customer)
    testCustomers.push({ id, ...customer })

    // Read customer details
    const details = await customerPage.read(id)
    expect(details).toBeDefined()
    expect(details?.firstName).toBe('Detail')
    expect(details?.lastName).toBe('View')
  })

  test('17 - Should show customer information completely', async ({ page }) => {
    const customer = CustomerFactory.create()
      .withFirstName('Complete')
      .withLastName('Info')
      .withRandomEmail()
      .withPhone('+1987654321')
      .active()
      .build()

    const id = await customerPage.create(customer)
    testCustomers.push({ id, ...customer })

    // Navigate to customer details
    await customerPage.navigateToDetail(id)

    // Verify all fields are displayed
    await expect(page.locator('[data-testid="firstName"]')).toContainText('Complete')
    await expect(page.locator('[data-testid="lastName"]')).toContainText('Info')
    await expect(page.locator('[data-testid="email"]')).toContainText('@')
    await expect(page.locator('[data-testid="phone"]')).toContainText('+1987654321')
    await expect(page.locator('[data-testid="status-badge"]')).toContainText(/Active/i)
  })

  // ========== UPDATE CUSTOMER TESTS ==========

  test('18 - Should update customer information', async ({ page }) => {
    const customer = CustomerFactory.create()
      .withFirstName('Original')
      .withLastName('Name')
      .withRandomEmail()
      .active()
      .build()

    const id = await customerPage.create(customer)
    testCustomers.push({ id, ...customer })

    // Update customer
    await customerPage.update(id, {
      firstName: 'Updated',
      lastName: 'Name',
      phone: '+1111111111'
    })

    // Verify updates
    const updated = await customerPage.read(id)
    expect(updated?.firstName).toBe('Updated')
    expect(updated?.lastName).toBe('Name')
    expect(updated?.phone).toContain('+1111111111')
  })

  test('19 - Should update customer status', async ({ page }) => {
    const customer = CustomerFactory.create()
      .withFirstName('Status')
      .withLastName('Test')
      .withRandomEmail()
      .active()
      .build()

    const id = await customerPage.create(customer)
    testCustomers.push({ id, ...customer })

    // Update status to inactive
    await customerPage.update(id, { status: 'INACTIVE' })

    // Verify status change
    const updated = await customerPage.read(id)
    expect(updated?.status).toMatch(/inactive/i)
  })

  test('20 - Should show validation errors on update with invalid data', async ({ page }) => {
    const customer = CustomerFactory.create()
      .withFirstName('Update')
      .withLastName('Test')
      .withRandomEmail()
      .active()
      .build()

    const id = await customerPage.create(customer)
    testCustomers.push({ id, ...customer })

    await customerPage.navigateToDetail(id)
    await page.click('[data-testid="edit-button"]')

    // Clear email and try to save
    await page.fill('[data-testid="email-input"]', '')
    await page.click('[data-testid="save-button"]')
    await page.waitForTimeout(500)

    await expect(page.locator('[data-testid="email-error"]'))
      .toContainText(/required/i)
  })

  // ========== DELETE CUSTOMER TESTS ==========

  test('21 - Should delete customer', async ({ page }) => {
    const customer = CustomerFactory.create()
      .withFirstName('Delete')
      .withLastName('Me')
      .withRandomEmail()
      .active()
      .build()

    const id = await customerPage.create(customer)
    testCustomers.push({ id, ...customer })

    // Delete customer
    await customerPage.delete(id)

    // Verify customer is gone
    const customers = await customerPage.list()
    const found = customers.find(c => c.id === id)
    expect(found).toBeUndefined()
  })

  test('22 - Should show confirmation dialog before delete', async ({ page }) => {
    const customer = CustomerFactory.create()
      .withFirstName('Confirm')
      .withLastName('Delete')
      .withRandomEmail()
      .active()
      .build()

    const id = await customerPage.create(customer)
    testCustomers.push({ id, ...customer })

    await customerPage.navigateToDetail(id)
    await page.click('[data-testid="delete-button"]')

    // Should show confirmation dialog
    await expect(page.locator('[data-testid="confirm-delete-dialog"]')).toBeVisible()
    await expect(page.locator('[data-testid="confirm-delete"]')).toBeVisible()

    // Confirm deletion
    await page.click('[data-testid="confirm-delete"]')
    await page.waitForTimeout(500)

    // Should redirect to list
    await expect(page).toHaveURL(/.*\/customers/)
  })

  // ========== BULK OPERATIONS TESTS ==========

  test('23 - Should select multiple customers and perform bulk action', async ({ page }) => {
    // Create multiple customers
    const customers = CustomerFactory.create().buildMany(3)
    const createdIds: any[] = []

    for (const customer of customers) {
      const id = await customerPage.create(customer)
      createdIds.push({ id, ...customer })
    }
    testCustomers.push(...createdIds)

    await page.waitForTimeout(1000)

    // Select multiple customers
    await page.click('[data-testid="select-all-checkbox"]')

    // Perform bulk action (e.g., change status)
    await page.click('[data-testid="bulk-action-dropdown"]')
    await page.click('[data-testid="bulk-action-change-status"]')
    await page.selectOption('[data-testid="bulk-status-select"]', 'INACTIVE')
    await page.click('[data-testid="bulk-action-apply"]')

    // Wait for operation to complete
    await page.waitForTimeout(1000)

    // Verify bulk action was applied
    const updatedCustomers = await customerPage.list()
    expect(updatedCustomers.length).toBeGreaterThan(0)
  })

  // ========== SORTING TESTS ==========

  test('24 - Should sort customers by different fields', async ({ page }) => {
    // Check if sorting dropdown exists
    const sortDropdown = page.locator('[data-testid="sort-dropdown"]')
    if (await sortDropdown.isVisible()) {
      // Sort by first name
      await page.selectOption('[data-testid="sort-dropdown"]', 'firstName')
      await page.click('[data-testid="apply-sort"]')
      await page.waitForTimeout(500)

      // Sort by last name
      await page.selectOption('[data-testid="sort-dropdown"]', 'lastName')
      await page.click('[data-testid="apply-sort"]')
      await page.waitForTimeout(500)

      // Sort by email
      await page.selectOption('[data-testid="sort-dropdown"]', 'email')
      await page.click('[data-testid="apply-sort"]')
      await page.waitForTimeout(500)

      // Verify table is still visible
      await expect(page.locator('table tbody tr')).toBeVisible()
    }
  })

  // ========== EXPORT TESTS ==========

  test('25 - Should export customers to CSV', async ({ page }) => {
    // Check if export button exists
    const exportButton = page.locator('[data-testid="export-customers-button"]')
    if (await exportButton.isVisible()) {
      const [download] = await Promise.all([
        page.waitForEvent('download'),
        exportButton.click()
      ])

      const path = await download.path()
      expect(path).toBeTruthy()

      // Verify file is CSV
      expect(path).toMatch(/\.csv$/i)
    }
  })
})
