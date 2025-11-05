/**
 * Addresses E2E Flow Tests
 *
 * Complete test coverage for address management functionality
 * Tests CRUD operations, filtering, search, and primary address handling
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐
 */

import { test, expect } from '@playwright/test'

test.describe('Addresses Flow', () => {
  test.describe('Addresses List', () => {
    test.beforeEach(async ({ page }) => {
      // Mock API responses
      await page.route('**/api/addresses**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [],
            totalElements: 0,
            totalPages: 0,
            number: 0,
            size: 20
          })
        })
      })
    })

    test('should navigate to addresses page', async ({ page }) => {
      await page.goto('/addresses')

      await expect(page.locator('h1.page-title')).toContainText('Addresses')
      await expect(page.locator('text=Manage customer addresses')).toBeVisible()
    })

    test('should display addresses summary cards', async ({ page }) => {
      await page.goto('/addresses')

      await expect(page.locator('.summary-card')).toHaveCount(4)
      await expect(page.locator('text=Total Addresses')).toBeVisible()
      await expect(page.locator('text=Active')).toBeVisible()
      await expect(page.locator('text=Inactive')).toBeVisible()
      await expect(page.locator('text=Primary')).toBeVisible()
    })

    test('should filter by address type', async ({ page }) => {
      await page.goto('/addresses')

      await page.selectOption('[data-testid="address-type-filter"]', 'BILLING')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('addressType'))
      expect(filterCall).toBeDefined()
    })

    test('should search addresses', async ({ page }) => {
      await page.goto('/addresses')

      const searchInput = page.locator('input[placeholder*="Search addresses"]')
      await searchInput.fill('123 Main St')
      await page.keyboard.press('Enter')
      await page.waitForTimeout(500)

      const calls = page.request.allInterceptions()
      const searchCall = calls.find(c => c.request.url().includes('searchTerm'))
      expect(searchCall).toBeDefined()
    })

    test('should display empty state when no addresses', async ({ page }) => {
      await page.goto('/addresses')

      await expect(page.locator('.empty-state')).toBeVisible()
      await expect(page.locator('text=No addresses found')).toBeVisible()
      await expect(page.locator('[data-testid="create-address-btn"]')).toBeVisible()
    })
  })

  test.describe('Create Address', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/addresses**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 201,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'addr-123',
              street: '123 Main St',
              city: 'New York',
              state: 'NY',
              zipCode: '10001',
              country: 'US',
              type: 'BILLING',
              status: 'ACTIVE',
              isPrimary: false
            })
          })
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ content: [], totalElements: 0 })
          })
        }
      })
    })

    test('should open create address modal', async ({ page }) => {
      await page.goto('/addresses')
      await page.click('[data-testid="create-address-btn"]')

      await expect(page.locator('[data-testid="address-modal"]')).toBeVisible()
      await expect(page.locator('h2.modal-title')).toContainText('Create Address')
    })

    test('should create new address with valid data', async ({ page }) => {
      await page.goto('/addresses')
      await page.click('[data-testid="create-address-btn"]')

      // Fill form
      await page.fill('[name="street"]', '123 Main St')
      await page.fill('[name="city"]', 'New York')
      await page.fill('[name="state"]', 'NY')
      await page.fill('[name="zipCode"]', '10001')
      await page.selectOption('[name="country"]', 'US')
      await page.selectOption('[name="type"]', 'BILLING')

      // Submit
      await page.click('[data-testid="save-address-btn"]')

      // Verify success
      await expect(page.locator('[data-testid="toast-success"]')).toContainText('Address created successfully')
      await expect(page.locator('[data-testid="address-modal"]')).not.toBeVisible()
    })

    test('should validate required fields', async ({ page }) => {
      await page.goto('/addresses')
      await page.click('[data-testid="create-address-btn"]')

      // Try to submit without filling required fields
      await page.click('[data-testid="save-address-btn"]')

      // Verify validation errors
      await expect(page.locator('[name="street"]')).toHaveAttribute('aria-invalid', 'true')
      await expect(page.locator('[name="city"]')).toHaveAttribute('aria-invalid', 'true')
      await expect(page.locator('.error-message')).toContainText('This field is required')
    })

    test('should set as primary address', async ({ page }) => {
      await page.goto('/addresses')
      await page.click('[data-testid="create-address-btn"]')

      await page.fill('[name="street"]', '456 Oak Ave')
      await page.fill('[name="city"]', 'Boston')
      await page.fill('[name="state"]', 'MA')
      await page.fill('[name="zipCode"]', '02101')
      await page.selectOption('[name="country"]', 'US')
      await page.check('[name="isPrimary"]')

      await page.click('[data-testid="save-address-btn"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Address created and set as primary')
    })
  })

  test.describe('Edit Address', () => {
    test.beforeEach(async ({ page }) => {
      // Mock get address
      await page.route('**/api/addresses/addr-123', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'addr-123',
            street: '123 Main St',
            city: 'New York',
            state: 'NY',
            zipCode: '10001',
            country: 'US',
            type: 'BILLING',
            status: 'ACTIVE',
            isPrimary: false
          })
        })
      })

      // Mock update
      await page.route('**/api/addresses/addr-123', async (route) => {
        if (route.request().method() === 'PUT') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'addr-123',
              street: '123 Main St Updated',
              city: 'New York',
              state: 'NY',
              zipCode: '10001',
              country: 'US',
              type: 'BILLING',
              status: 'ACTIVE',
              isPrimary: false
            })
          })
        }
      })

      // Mock list
      await page.route('**/api/addresses**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [
              {
                id: 'addr-123',
                street: '123 Main St',
                city: 'New York',
                state: 'NY',
                zipCode: '10001',
                country: 'US',
                type: 'BILLING',
                status: 'ACTIVE',
                isPrimary: false
              }
            ],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 20
          })
        })
      })
    })

    test('should open edit modal from list', async ({ page }) => {
      await page.goto('/addresses')

      await page.click('[data-testid="edit-address-addr-123"]')

      await expect(page.locator('[data-testid="address-modal"]')).toBeVisible()
      await expect(page.locator('h2.modal-title')).toContainText('Edit Address')
      await expect(page.locator('[name="street"]')).toHaveValue('123 Main St')
    })

    test('should update address successfully', async ({ page }) => {
      await page.goto('/addresses')
      await page.click('[data-testid="edit-address-addr-123"]')

      // Update street
      await page.fill('[name="street"]', '123 Main St Updated')
      await page.click('[data-testid="save-address-btn"]')

      // Verify success
      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Address updated successfully')
      await expect(page.locator('[data-testid="address-modal"]')).not.toBeVisible()
    })
  })

  test.describe('Delete Address', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/addresses**', async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              content: [
                {
                  id: 'addr-123',
                  street: '123 Main St',
                  city: 'New York',
                  state: 'NY',
                  zipCode: '10001',
                  country: 'US',
                  type: 'BILLING',
                  status: 'ACTIVE',
                  isPrimary: false
                }
              ],
              totalElements: 1,
              totalPages: 1,
              number: 0,
              size: 20
            })
          })
        } else if (route.request().method() === 'DELETE') {
          await route.fulfill({
            status: 204
          })
        }
      })
    })

    test('should delete address after confirmation', async ({ page }) => {
      await page.goto('/addresses')

      await page.click('[data-testid="delete-address-addr-123"]')

      // Confirm deletion
      await expect(page.locator('[data-testid="confirm-dialog"]')).toBeVisible()
      await page.click('[data-testid="confirm-delete-btn"]')

      // Verify success
      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Address deleted successfully')
    })

    test('should not delete when cancel is clicked', async ({ page }) => {
      await page.goto('/addresses')

      await page.click('[data-testid="delete-address-addr-123"]')
      await page.click('[data-testid="cancel-delete-btn"]')

      // Verify dialog is closed
      await expect(page.locator('[data-testid="confirm-dialog"]')).not.toBeVisible()
    })
  })

  test.describe('Primary Address', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/addresses**', async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              content: [
                {
                  id: 'addr-123',
                  street: '123 Main St',
                  city: 'New York',
                  state: 'NY',
                  zipCode: '10001',
                  country: 'US',
                  type: 'BILLING',
                  status: 'ACTIVE',
                  isPrimary: false
                },
                {
                  id: 'addr-456',
                  street: '456 Oak Ave',
                  city: 'Boston',
                  state: 'MA',
                  zipCode: '02101',
                  country: 'US',
                  type: 'SHIPPING',
                  status: 'ACTIVE',
                  isPrimary: true
                }
              ],
              totalElements: 2,
              totalPages: 1,
              number: 0,
              size: 20
            })
          })
        } else if (route.request().method() === 'PATCH') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'addr-123',
              street: '123 Main St',
              city: 'New York',
              state: 'NY',
              zipCode: '10001',
              country: 'US',
              type: 'BILLING',
              status: 'ACTIVE',
              isPrimary: true
            })
          })
        }
      })
    })

    test('should set address as primary', async ({ page }) => {
      await page.goto('/addresses')

      await page.click('[data-testid="set-primary-addr-123"]')

      // Verify success
      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Primary address updated')

      // Verify primary badge is visible
      await expect(page.locator('[data-testid="primary-badge-addr-123"]')).toBeVisible()
    })

    test('should indicate primary address with badge', async ({ page }) => {
      await page.goto('/addresses')

      await expect(page.locator('[data-testid="primary-badge-addr-456"]')).toBeVisible()
      await expect(page.locator('[data-testid="primary-badge-addr-123"]')).not.toBeVisible()
    })
  })

  test.describe('Address Details', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/addresses/addr-123', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'addr-123',
            street: '123 Main St',
            city: 'New York',
            state: 'NY',
            zipCode: '10001',
            country: 'US',
            type: 'BILLING',
            status: 'ACTIVE',
            isPrimary: false,
            createdAt: '2024-01-15T10:00:00Z',
            updatedAt: '2024-01-16T10:00:00Z'
          })
        })
      })
    })

    test('should navigate to address details page', async ({ page }) => {
      await page.goto('/addresses/addr-123')

      await expect(page.locator('h1.page-title')).toContainText('Address Details')
      await expect(page.locator('[data-testid="address-street"]')).toContainText('123 Main St')
      await expect(page.locator('[data-testid="address-city"]')).toContainText('New York, NY 10001')
      await expect(page.locator('[data-testid="address-type"]')).toContainText('BILLING')
    })

    test('should display address metadata', async ({ page }) => {
      await page.goto('/addresses/addr-123')

      await expect(page.locator('[data-testid="created-at"]')).toContainText('2024-01-15')
      await expect(page.locator('[data-testid="updated-at"]')).toContainText('2024-01-16')
    })
  })

  test.describe('Filter and Sort', () => {
    test('should filter addresses by status', async ({ page }) => {
      await page.route('**/api/addresses**', async (route) => {
        const url = new URL(route.request().url())
        const status = url.searchParams.get('status')

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: status === 'ACTIVE' ? [
              { id: 'addr-123', status: 'ACTIVE' }
            ] : [],
            totalElements: status === 'ACTIVE' ? 1 : 0
          })
        })
      })

      await page.goto('/addresses')
      await page.selectOption('[data-testid="status-filter"]', 'ACTIVE')

      const calls = page.request.allInterceptions()
      const filterCall = calls.find(c => c.request.url().includes('status=ACTIVE'))
      expect(filterCall).toBeDefined()
    })

    test('should sort addresses by street', async ({ page }) => {
      await page.route('**/api/addresses**', async (route) => {
        const url = new URL(route.request().url())
        const sortBy = url.searchParams.get('sortBy')

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            content: [],
            totalElements: 0
          })
        })
      })

      await page.goto('/addresses')
      await page.click('[data-testid="sort-street"]')

      const calls = page.request.allInterceptions()
      const sortCall = calls.find(c => c.request.url().includes('sortBy=street'))
      expect(sortCall).toBeDefined()
    })
  })

  test.describe('Accessibility', () => {
    test('should support keyboard navigation', async ({ page }) => {
      await page.goto('/addresses')

      // Tab through interactive elements
      await page.keyboard.press('Tab')
      await page.keyboard.press('Tab')
      await page.keyboard.press('Tab')

      // Verify focus is visible
      const focusedElement = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'))
      expect(focusedElement).toBeDefined()
    })

    test('should have proper ARIA labels', async ({ page }) => {
      await page.goto('/addresses')

      await expect(page.locator('[data-testid="create-address-btn"]'))
        .toHaveAttribute('aria-label', 'Create new address')
    })
  })
})
