import { test, expect } from '@playwright/test'

test.describe('Customer Management Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('complete customer creation flow', async ({ page }) => {
    // Navigate to customers page
    await page.click('text=Customers')

    // Click "Add Customer" button
    await page.click('button:has-text("Add Customer")')

    // Verify modal opens
    await expect(page.locator('.modal')).toBeVisible()
    await expect(page.locator('h2:has-text("Create Customer")')).toBeVisible()

    // Fill form
    await page.fill('input[placeholder*="name"], input[type="text"]', 'John Doe')
    await page.fill('input[type="email"]', 'john.doe@example.com')
    await page.fill('input[type="tel"]', '+1234567890')

    // Select country
    await page.click('.select__trigger')
    await page.click('.select__option:has-text("United States")')

    // Submit form
    await page.click('button:has-text("Create Customer")')

    // Wait for loading
    await page.waitForSelector('.btn--loading', { state: 'detached' })

    // Verify success
    await expect(page.locator('.toast, .notification')).toContainText('success')

    // Modal should close
    await expect(page.locator('.modal')).not.toBeVisible()

    // New customer should appear in table
    await expect(page.locator('table')).toContainText('John Doe')
    await expect(page.locator('table')).toContainText('john.doe@example.com')
  })

  test('customer form validation', async ({ page }) => {
    await page.goto('/customers')

    // Open create modal
    await page.click('button:has-text("Add Customer")')

    // Try to submit empty form
    await page.click('button:has-text("Create Customer")')

    // Error messages should appear
    await expect(page.locator('.input__error')).toBeVisible()
    await expect(page.locator('.input__error')).toContainText('required')

    // Fill required fields
    await page.fill('input[type="text"]', 'John Doe')
    await page.fill('input[type="email"]', 'invalid-email')

    // Should show email error
    await expect(page.locator('.input__error')).toContainText('email')

    // Fix email
    await page.fill('input[type="email"]', 'john@example.com')

    // Select country
    await page.click('.select__trigger')
    await page.click('.select__option:has-text("Canada")')

    // Form should be valid now
    await expect(page.locator('button[type="submit"]:not([disabled])')).toBeVisible()

    // Can submit
    await page.click('button:has-text("Create Customer")')
    await expect(page.locator('.modal')).not.toBeVisible()
  })

  test('edit customer flow', async ({ page }) => {
    // Assume we have customers in the table
    await page.goto('/customers')

    // Click edit action for first customer
    await page.click('.data-table__action-button:has-text("Edit")', { first: true })

    // Edit modal should open
    await expect(page.locator('.modal')).toBeVisible()
    await expect(page.locator('h2:has-text("Edit Customer")')).toBeVisible()

    // Form should be pre-filled
    await expect(page.locator('input[type="text"]')).toHaveValue('John Doe')

    // Update data
    await page.fill('input[type="text"]', 'Jane Doe')
    await page.fill('input[type="email"]', 'jane.doe@example.com')

    // Save changes
    await page.click('button:has-text("Save")')

    // Success message
    await expect(page.locator('.toast')).toContainText('updated')

    // Changes should appear in table
    await expect(page.locator('table')).toContainText('Jane Doe')
    await expect(page.locator('table')).toContainText('jane.doe@example.com')
  })

  test('delete customer confirmation', async ({ page }) => {
    await page.goto('/customers')

    // Click delete action
    await page.click('.data-table__action-button:has-text("Delete")', { first: true })

    // Confirmation modal
    await expect(page.locator('.modal')).toBeVisible()
    await expect(page.locator('h2:has-text("Confirm Deletion")')).toBeVisible()
    await expect(page.locator('.modal')).toContainText('Are you sure')

    // Cancel
    await page.click('button:has-text("Cancel")')
    await expect(page.locator('.modal')).not.toBeVisible()

    // Try again and confirm
    await page.click('.data-table__action-button:has-text("Delete")', { first: true })
    await page.click('button:has-text("Delete")')

    // Success
    await expect(page.locator('.toast')).toContainText('deleted')

    // Customer should be removed from table
    await expect(page.locator('table')).not.toContainText('John Doe')
  })

  test('customer search and filter', async ({ page }) => {
    await page.goto('/customers')

    // Search for customer
    await page.fill('input[placeholder*="Search"]', 'John')

    // Should filter results
    await page.waitForSelector('table')
    const rows = page.locator('.data-table__row')
    await expect(rows).toHaveCount(1)
    await expect(page.locator('table')).toContainText('John')

    // Clear search
    await page.fill('input[placeholder*="Search"]', '')

    // All customers visible
    await expect(rows).toHaveCount(5)

    // Filter by status
    await page.click('.select__trigger')
    await page.click('.select__option:has-text("Active")')

    // Should filter by active status
    await expect(rows).toHaveCount(3) // Assuming 3 active customers

    // Clear filter
    await page.click('.select__clear, button:has-text("Clear")')
    await expect(rows).toHaveCount(5)
  })

  test('bulk operations on customers', async ({ page }) => {
    await page.goto('/customers')

    // Enable selection
    await page.click('input[type="checkbox"].data-table__checkbox--header')

    // Select all customers
    await page.check('input[type="checkbox"].data-table__checkbox--header')

    // Bulk actions should appear
    await expect(page.locator('.bulk-actions')).toBeVisible()
    await expect(page.locator('.bulk-actions')).toContainText('5 selected')

    // Click bulk delete
    await page.click('button:has-text("Bulk Delete")')

    // Confirmation modal
    await expect(page.locator('.modal')).toBeVisible()
    await expect(page.locator('.modal')).toContainText('5 customers')

    // Confirm
    await page.click('button:has-text("Delete")')

    // Success
    await expect(page.locator('.toast')).toContainText('deleted')

    // Table should be empty or show empty state
    await expect(page.locator('.data-table__empty, table:empty')).toBeVisible()
  })

  test('customer data table sorting', async ({ page }) => {
    await page.goto('/customers')

    // Sort by Name column
    await page.click('.data-table__header-cell:has-text("Name")')

    // Should show sort indicator
    await expect(page.locator('.data-table__header-cell--sorted')).toBeVisible()
    await expect(page.locator('.data-table__header-cell--asc')).toBeVisible()

    // Click again to reverse sort
    await page.click('.data-table__header-cell:has-text("Name")')

    // Should be descending
    await expect(page.locator('.data-table__header-cell--desc')).toBeVisible()

    // Sort by different column
    await page.click('.data-table__header-cell:has-text("ID")')

    // ID column should be sorted
    await expect(page.locator('.data-table__header-cell >> nth=0')).toHaveClass(/sorted/)
  })

  test('customer row click action', async ({ page }) => {
    await page.goto('/customers')

    // Click on a row (but not on actions)
    await page.click('.data-table__row >> nth=0')

    // Details modal should open
    await expect(page.locator('.modal')).toBeVisible()
    await expect(page.locator('h2:has-text("Customer Details")')).toBeVisible()

    // Should show customer information
    await expect(page.locator('.modal')).toContainText('ID:')
    await expect(page.locator('.modal')).toContainText('Name:')
    await expect(page.locator('.modal')).toContainText('Email:')

    // Close modal
    await page.click('button:has-text("Close")')
    await expect(page.locator('.modal')).not.toBeVisible()
  })

  test('customer pagination', async ({ page }) => {
    await page.goto('/customers')

    // Assuming 50 customers with 10 per page
    // Check if pagination controls exist
    const pagination = page.locator('.pagination, .data-table__pagination')
    if (await pagination.isVisible()) {
      // Go to next page
      await page.click('button:has-text("Next")')

      // Should be on page 2
      await expect(page.locator('.pagination >> .active')).toContainText('2')

      // Go to previous page
      await page.click('button:has-text("Previous")')
      await expect(page.locator('.pagination >> .active')).toContainText('1')
    }
  })

  test('customer form with keyboard navigation', async ({ page }) => {
    await page.goto('/customers')
    await page.click('button:has-text("Add Customer")')

    // Tab through form fields
    await page.keyboard.press('Tab')
    await expect(page.locator('input[type="text"]')).toBeFocused()

    await page.keyboard.press('Tab')
    await expect(page.locator('input[type="email"]')).toBeFocused()

    await page.keyboard.press('Tab')
    await expect(page.locator('.select__trigger')).toBeFocused()

    // Fill fields using keyboard
    await page.keyboard.type('John Doe')
    await page.keyboard.press('Tab')
    await page.keyboard.type('john@example.com')
    await page.keyboard.press('Enter') // Open select
    await page.keyboard.press('ArrowDown')
    await page.keyboard.press('Enter')

    // Submit with keyboard
    await page.keyboard.press('Tab')
    await page.keyboard.press('Tab')
    await page.keyboard.press('Enter')

    // Form should submit
    await expect(page.locator('.modal')).not.toBeVisible()
  })

  test('customer form clear and reset', async ({ page }) => {
    await page.goto('/customers')
    await page.click('button:has-text("Add Customer")')

    // Fill form
    await page.fill('input[type="text"]', 'John Doe')
    await page.fill('input[type="email"]', 'john@example.com')
    await page.click('.select__trigger')
    await page.click('.select__option:has-text("United States")')

    // Clear button should exist
    const clearButton = page.locator('input[type="text"] >> following-sibling::button.input__clear')
    if (await clearButton.isVisible()) {
      await clearButton.click()
      await expect(page.locator('input[type="text"]')).toHaveValue('')
    }

    // Or click Cancel to reset
    await page.click('button:has-text("Cancel")')
    await expect(page.locator('.modal')).not.toBeVisible()

    // Open again, form should be empty
    await page.click('button:has-text("Add Customer")')
    await expect(page.locator('input[type="text"]')).toHaveValue('')
    await expect(page.locator('input[type="email"]')).toHaveValue('')
  })
})
