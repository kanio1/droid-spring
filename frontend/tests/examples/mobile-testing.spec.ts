/**
 * Mobile Device Testing Examples
 *
 * Demonstrates how to test responsive design and mobile interactions
 * Includes touch gestures, responsive layouts, and mobile-specific UI
 *
 * Run with:
 * - Mobile Chrome: npx playwright test mobile-testing.spec.ts --project="mobile-chrome"
 * - Mobile Safari: npx playwright test mobile-testing.spec.ts --project="mobile-safari"
 */

import { test, expect, devices } from '@playwright/test'

// Configure mobile projects in playwright.config.ts
test.describe('Mobile Device Testing', () => {
  test.use({ ...devices['iPhone 12'] })

  test('customer list on mobile', async ({ page }) => {
    await page.goto('/customers')

    // Verify mobile layout
    await expect(page.locator('.mobile-menu-toggle')).toBeVisible()

    // Open mobile menu
    await page.tap('.mobile-menu-toggle')
    await expect(page.locator('.mobile-menu')).toBeVisible()

    // Verify menu items
    await expect(page.locator('.mobile-menu [data-testid="nav-customers"]')).toBeVisible()
    await expect(page.locator('.mobile-menu [data-testid="nav-orders"]')).toBeVisible()
  })

  test('touch gestures - swipe to delete', async ({ page }) => {
    await page.goto('/customers')

    // Swipe left on customer card
    await page.swipe(
      '[data-testid="customer-card"]',
      { x: 300, y: 0 }, // swipe left
      10 // steps
    )

    // Verify delete action appears
    await expect(page.locator('.swipe-delete')).toBeVisible()

    // Tap delete
    await page.tap('.swipe-delete')

    // Verify confirmation
    await expect(page.locator('[data-testid="confirm-dialog"]')).toBeVisible()
  })

  test('pull to refresh', async ({ page }) => {
    await page.goto('/customers')

    // Pull down to refresh
    await page.swipe(
      '[data-testid="customer-list"]',
      { x: 0, y: 500 }, // pull down
      50
    )

    // Verify refresh indicator
    await expect(page.locator('.refresh-indicator')).toBeVisible()
    await expect(page.locator('.spinner')).toBeVisible()

    // Wait for refresh to complete
    await page.waitForSelector('.refresh-indicator', { state: 'hidden' })
  })

  test('mobile form interaction', async ({ page }) => {
    await page.goto('/customers/create')

    // Verify mobile-optimized form
    await expect(page.locator('.mobile-form')).toBeVisible()

    // Test virtual keyboard doesn't obscure inputs
    const nameInput = page.locator('[name="firstName"]')
    await nameInput.tap()

    // Input should scroll into view
    await page.waitForFunction(() => {
      const input = document.querySelector('[name="firstName"]')
      const rect = input?.getBoundingClientRect()
      return rect && rect.top > 0
    })

    // Type text
    await nameInput.fill('John')
    await expect(nameInput).toHaveValue('John')
  })

  test('bottom sheet interactions', async ({ page }) => {
    await page.goto('/orders')

    // Open bottom sheet
    await page.tap('[data-testid="filter-button"]')
    await expect(page.locator('.bottom-sheet')).toBeVisible()

    // Scroll in bottom sheet
    await page.swipe(
      '.bottom-sheet-content',
      { x: 0, y: 200 },
      10
    )

    // Select filter option
    await page.tap('[data-testid="filter-pending"]')
    await expect(page.locator('.bottom-sheet')).not.toBeVisible()

    // Verify filter applied
    await expect(page.locator('[data-testid="active-filter"]'))
      .toContainText('Pending')
  })

  test('mobile navigation drawer', async ({ page }) => {
    await page.goto('/dashboard')

    // Open drawer from edge swipe
    await page.swipe(
      { x: 0, y: 300 },
      { x: 200, y: 300 }, // swipe right from edge
      5
    )

    // Verify drawer opened
    await expect(page.locator('.navigation-drawer')).toBeVisible()

    // Navigate to customers
    await page.tap('[data-testid="nav-customers"]')

    // Drawer should close
    await expect(page.locator('.navigation-drawer')).not.toBeVisible()
  })

  test('mobile table view', async ({ page }) => {
    await page.goto('/customers')

    // Switch to list view (mobile default)
    await expect(page.locator('.mobile-list-view')).toBeVisible()

    // Verify cards layout
    await expect(page.locator('.customer-card')).toHaveCount(10)

    // Verify swipe actions on cards
    await page.swipe(
      '.customer-card',
      { x: 200, y: 0 },
      5
    )
    await expect(page.locator('.swipe-actions')).toBeVisible()
  })

  test('tab bar navigation', async ({ page }) => {
    await page.goto('/')

    // Verify tab bar is present on mobile
    await expect(page.locator('.tab-bar')).toBeVisible()

    // Switch tabs
    await page.tap('[data-testid="tab-orders"]')
    await expect(page).toHaveURL('/orders')

    await page.tap('[data-testid="tab-invoices"]')
    await expect(page).toHaveURL('/invoices')

    // Verify active tab
    await expect(page.locator('[data-testid="tab-invoices"].active')).toBeVisible()
  })

  test('mobile search with keyboard', async ({ page }) => {
    await page.goto('/customers')

    // Tap search
    await page.tap('[data-testid="search-icon"]')

    // Verify search opens in full screen
    await expect(page.locator('.search-overlay')).toBeVisible()

    // Type search query
    const searchInput = page.locator('[data-testid="search-input"]')
    await searchInput.fill('john')

    // Verify search results
    await expect(page.locator('.search-results')).toBeVisible()

    // Clear search
    await page.tap('[data-testid="clear-search"]')
    await expect(searchInput).toHaveValue('')

    // Close search
    await page.tap('[data-testid="close-search"]')
    await expect(page.locator('.search-overlay')).not.toBeVisible()
  })

  test('mobile modal behavior', async ({ page }) => {
    await page.goto('/customers')

    // Open modal
    await page.tap('[data-testid="view-customer-btn"]')

    // Verify full-screen modal on mobile
    await expect(page.locator('.modal-fullscreen')).toBeVisible()

    // Close modal
    await page.tap('[data-testid="close-modal"]')
    await expect(page.locator('.modal-fullscreen')).not.toBeVisible()
  })

  test('orientation change', async ({ page }) => {
    await page.goto('/dashboard')

    // Portrait mode
    await expect(page.locator('.portrait-layout')).toBeVisible()

    // Change to landscape
    await page.setOrientation('landscape')

    // Verify landscape layout
    await expect(page.locator('.landscape-layout')).toBeVisible()

    // Restore portrait
    await page.setOrientation('portrait')
    await expect(page.locator('.portrait-layout')).toBeVisible()
  })
})

// Tablet-specific tests
test.describe('Tablet Testing', () => {
  test.use({ ...devices['iPad Pro'] })

  test('tablet layout', async ({ page }) => {
    await page.goto('/customers')

    // Tablet should show more columns
    await expect(page.locator('.customer-table')).toBeVisible()
    await expect(page.locator('.customer-table th')).toHaveCount(6)

    // Sidebar should be visible
    await expect(page.locator('.sidebar')).toBeVisible()
  })

  test('tablet split view', async ({ page }) => {
    await page.goto('/customers')

    // Open detail view
    await page.tap('[data-testid="customer-item"]')

    // Verify split view on tablet
    await expect(page.locator('.split-view')).toBeVisible()
    await expect(page.locator('.split-view .list-pane')).toBeVisible()
    await expect(page.locator('.split-view .detail-pane')).toBeVisible()
  })
})

// Android-specific tests
test.describe('Android Testing', () => {
  test.use({ ...devices['Pixel 5'] })

  test('material design components', async ({ page }) => {
    await page.goto('/customers')

    // Verify material design elements
    await expect(page.locator('.material-card')).toBeVisible()
    await expect(page.locator('.material-fab')).toBeVisible()

    // Test FAB (Floating Action Button)
    await page.tap('.material-fab')
    await expect(page.locator('.fab-menu')).toBeVisible()
  })

  test('android back button', async ({ page }) => {
    await page.goto('/customers')
    await page.goto('/customers/123')

    // Use browser back
    await page.goBack()
    await expect(page).toHaveURL('/customers')
  })

  test('hardware back button', async ({ page }) => {
    await page.goto('/customers/create')

    // Press hardware back
    page.evaluate(() => {
      window.history.back()
    })

    // Verify navigation
    await expect(page).toHaveURL('/customers')
  })
})
