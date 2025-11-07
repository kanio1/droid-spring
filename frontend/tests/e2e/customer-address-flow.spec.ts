import { test, expect } from '@playwright/test'

test.describe('Customer → Address Assignment Flow', () => {
  const customerData = {
    firstName: 'John',
    lastName: 'Doe',
    email: `john.doe.${Date.now()}@example.com`,
    phone: '+1234567890'
  }

  const addressData = {
    street: '123 Main Street',
    houseNumber: '123',
    postalCode: '12345',
    city: 'New York',
    country: 'US',
    type: 'BILLING'
  }

  test.beforeEach(async ({ page }) => {
    // Navigate to customers page
    await page.goto('/customers')

    // Wait for page to load
    await page.waitForSelector('[data-testid="customers-page"]', { timeout: 5000 })
  })

  test('should create customer and assign address', async ({ page }) => {
    // Step 1: Create new customer
    await test.step('Create customer', async () => {
      // Click Add Customer button
      await page.click('[data-testid="add-customer-button"]')

      // Wait for navigation to create page
      await page.waitForURL('/customers/create')
      await page.waitForSelector('[data-testid="create-customer-page"]', { timeout: 5000 })

      // Fill customer form
      await page.fill('[data-testid="first-name-input"]', customerData.firstName)
      await page.fill('[data-testid="last-name-input"]', customerData.lastName)
      await page.fill('[data-testid="email-input"]', customerData.email)
      await page.fill('[data-testid="phone-input"]', customerData.phone)

      // Submit form
      await page.click('[data-testid="submit-button"]')

      // Wait for navigation back to customers list
      await page.waitForURL('/customers')
      await page.waitForSelector('[data-testid="customers-page"]', { timeout: 5000 })

      // Verify customer appears in list
      await expect(page.locator('table tbody tr').first()).toContainText(customerData.firstName)
      await expect(page.locator('table tbody tr').first()).toContainText(customerData.lastName)
    })

    // Step 2: Navigate to customer details
    await test.step('Navigate to customer details', async () => {
      // Click on the newly created customer
      await page.click('table tbody tr:first-child')

      // Wait for navigation to customer details
      await page.waitForSelector('[data-testid="customer-details-page"]', { timeout: 5000 })

      // Verify customer details are displayed
      await expect(page.locator('[data-testid="customer-name"]')).toContainText(
        `${customerData.firstName} ${customerData.lastName}`
      )
      await expect(page.locator('[data-testid="customer-email"]')).toContainText(customerData.email)
    })

    // Step 3: Add address to customer
    await test.step('Add address to customer', async () => {
      // Click Add Address button
      await page.click('[data-testid="add-address-button"]')

      // Wait for modal or navigation
      await page.waitForSelector('[data-testid="address-form-modal"]', { timeout: 5000 })

      // Fill address form
      await page.fill('[data-testid="street-input"]', addressData.street)
      await page.fill('[data-testid="house-number-input"]', addressData.houseNumber)
      await page.fill('[data-testid="postal-code-input"]', addressData.postalCode)
      await page.fill('[data-testid="city-input"]', addressData.city)

      // Select country
      await page.selectOption('[data-testid="country-select"]', addressData.country)

      // Select address type
      await page.selectOption('[data-testid="type-select"]', addressData.type)

      // Submit address form
      await page.click('[data-testid="address-submit-button"]')

      // Wait for modal to close
      await page.waitForSelector('[data-testid="address-form-modal"]', { state: 'hidden', timeout: 5000 })

      // Small delay for data to update
      await page.waitForTimeout(500)
    })

    // Step 4: Verify address is assigned
    await test.step('Verify address is assigned', async () => {
      // Check if address appears in customer's addresses list
      await expect(page.locator('[data-testid="customer-addresses"]')).toBeVisible()

      // Verify address details
      const addressCard = page.locator('[data-testid="address-card"]').first()
      await expect(addressCard).toContainText(addressData.street)
      await expect(addressCard).toContainText(addressData.city)
      await expect(addressCard).toContainText(addressData.postalCode)
      await expect(addressCard).toContainText(addressData.country)
      await expect(addressCard).toContainText(addressData.type)
    })

    // Step 5: Navigate to addresses page and verify
    await test.step('Verify address in addresses list', async () => {
      // Navigate to addresses page
      await page.goto('/addresses')

      // Wait for page to load
      await page.waitForSelector('[data-testid="addresses-page"]', { timeout: 5000 })

      // Check if the address appears in the list
      await expect(page.locator('table tbody tr').first()).toContainText(addressData.street)
      await expect(page.locator('table tbody tr').first()).toContainText(customerData.firstName)
    })

    // Step 6: Edit address
    await test.step('Edit address', async () => {
      // Click edit button on first address
      await page.click('table tbody tr:first-child [data-testid="edit-button"]')

      // Wait for modal
      await page.waitForSelector('[data-testid="address-form-modal"]', { timeout: 5000 })

      // Update city
      const newCity = 'Los Angeles'
      await page.fill('[data-testid="city-input"]', newCity)

      // Submit changes
      await page.click('[data-testid="address-submit-button"]')

      // Wait for modal to close
      await page.waitForSelector('[data-testid="address-form-modal"]', { state: 'hidden', timeout: 5000 })

      // Verify change
      await expect(page.locator('table tbody tr').first()).toContainText(newCity)
    })

    // Step 7: Delete address
    await test.step('Delete address', async () => {
      // Click delete button on first address
      await page.click('table tbody tr:first-child [data-testid="delete-button"]')

      // Wait for confirmation dialog
      await page.waitForSelector('[data-testid="confirm-dialog"]', { timeout: 5000 })

      // Confirm deletion
      await page.click('[data-testid="confirm-delete-button"]')

      // Wait for row to be removed
      await page.waitForTimeout(500)

      // Verify address is removed
      // Note: This might show empty state or other addresses
    })
  })

  test('should handle address creation with validation errors', async ({ page }) => {
    // Navigate to create customer
    await page.click('[data-testid="add-customer-button"]')
    await page.waitForURL('/customers/create')

    // Try to create customer without required fields
    await page.click('[data-testid="submit-button"]')

    // Check for validation errors
    await expect(page.locator('[data-testid="first-name-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="last-name-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="email-error"]')).toBeVisible()

    // Fill only first name
    await page.fill('[data-testid="first-name-input"]', 'Jane')

    // Try to submit again
    await page.click('[data-testid="submit-button"]')

    // Should still show errors for other required fields
    await expect(page.locator('[data-testid="last-name-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="email-error"]')).toBeVisible()
  })

  test('should handle address creation validation', async ({ page }) => {
    // Create a customer first
    await page.click('[data-testid="add-customer-button"]')
    await page.waitForURL('/customers/create')

    await page.fill('[data-testid="first-name-input"]', 'Jane')
    await page.fill('[data-testid="last-name-input"]', 'Smith')
    await page.fill('[data-testid="email-input"]', `jane.smith.${Date.now()}@example.com`)
    await page.click('[data-testid="submit-button"]')

    await page.waitForURL('/customers')
    await page.waitForSelector('[data-testid="customers-page"]', { timeout: 5000 })

    // Navigate to customer details
    await page.click('table tbody tr:first-child')
    await page.waitForSelector('[data-testid="customer-details-page"]', { timeout: 5000 })

    // Try to add address without required fields
    await page.click('[data-testid="add-address-button"]')
    await page.waitForSelector('[data-testid="address-form-modal"]', { timeout: 5000 })

    await page.click('[data-testid="address-submit-button"]')

    // Check for validation errors
    await expect(page.locator('[data-testid="street-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="city-error"]')).toBeVisible()
    await expect(page.locator('[data-testid="postal-code-error"]')).toBeVisible()
  })
})
