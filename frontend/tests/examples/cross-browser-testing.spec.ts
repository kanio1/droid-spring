/**
 * Cross-Browser Testing Examples
 *
 * Demonstrates testing across different browsers (Chrome, Firefox, Safari, Edge)
 * Validates consistent behavior and appearance across browsers
 *
 * Configure in playwright.config.ts:
 * projects: [
 *   { name: 'chromium', use: { ...devices['Desktop Chrome'] }},
 *   { name: 'firefox', use: { ...devices['Desktop Firefox'] }},
 *   { name: 'webkit', use: { ...devices['Desktop Safari'] }},
 *   { name: 'edge', use: { ...devices['Desktop Edge'] }},
 * ]
 */

import { test, expect } from '@playwright/test'

test.describe('Cross-Browser Compatibility', () => {
  test('page should load correctly', async ({ page, browserName }) => {
    await page.goto('/')

    await expect(page.locator('h1')).toContainText('BSS Dashboard')

    // Browser-specific checks
    if (browserName === 'webkit') {
      // Safari-specific validations
      await expect(page.locator('.safari-fix')).toBeVisible()
    } else if (browserName === 'firefox') {
      // Firefox-specific validations
      await expect(page.locator('.firefox-fix')).toBeVisible()
    }
  })

  test('JavaScript functionality should work', async ({ page }) => {
    await page.goto('/customers')

    // Test dynamic content loading
    await page.click('[data-testid="filter-active"]')

    const cards = page.locator('.customer-card')
    await expect(cards).toHaveCount(5)

    // Verify all cards have 'active' class
    await Promise.all(
      (await cards.elementHandles()).map(async (card) => {
        const className = await card.evaluate(el => el.className)
        expect(className).toContain('active')
      })
    )
  })

  test('CSS styling should render consistently', async ({ page }) => {
    await page.goto('/dashboard')

    // Take screenshot for visual comparison
    await expect(page).toHaveScreenshot('dashboard.png', {
      fullPage: true,
      animations: 'disabled'
    })

    // Check specific styles
    const header = page.locator('header')
    await expect(header).toHaveCSS('background-color', 'rgb(0, 123, 255)')
  })

  test('form submission should work', async ({ page }) => {
    await page.goto('/customers/create')

    // Fill form
    await page.fill('[name="firstName"]', 'John')
    await page.fill('[name="lastName"]', 'Doe')
    await page.fill('[name="email"]', 'john@example.com')
    await page.selectOption('[name="status"]', 'active')

    // Submit form
    await page.click('[data-testid="submit-btn"]')

    // Verify success
    await expect(page.locator('[data-testid="success-message"]')).toBeVisible()
    await expect(page).toHaveURL('/customers')
  })

  test('dropdown should work', async ({ page }) => {
    await page.goto('/customers')

    // Open dropdown
    await page.click('[data-testid="status-filter"]')

    // Select option
    await page.selectOption('[name="status"]', 'active')

    // Verify selection
    await expect(page.locator('[data-testid="status-filter"]')).toHaveValue('active')
  })

  test('modal dialog should work', async ({ page }) => {
    await page.goto('/customers')

    // Open modal
    await page.click('[data-testid="create-customer-btn"]')
    await expect(page.locator('[role="dialog"]')).toBeVisible()

    // Verify focus management
    const closeButton = page.locator('[data-testid="close-modal"]')
    await expect(closeButton).toBeFocused()

    // Close modal
    await page.click('[data-testid="close-modal"]')
    await expect(page.locator('[role="dialog"]')).not.toBeVisible()
  })

  test('table sorting should work', async ({ page }) => {
    await page.goto('/customers')

    // Click sort header
    await page.click('[data-testid="sort-name"]')

    // Verify sort indicator
    await expect(page.locator('[data-testid="sort-name"].sorted-asc')).toBeVisible()

    // Click again to reverse sort
    await page.click('[data-testid="sort-name"]')
    await expect(page.locator('[data-testid="sort-name"].sorted-desc')).toBeVisible()
  })

  test('pagination should work', async ({ page }) => {
    await page.goto('/customers')

    // Go to next page
    await page.click('[data-testid="next-page"]')

    // Verify page changed
    await expect(page.locator('[data-testid="current-page"]')).toHaveText('2')

    // Go to previous page
    await page.click('[data-testid="prev-page"]')
    await expect(page.locator('[data-testid="current-page"]')).toHaveText('1')
  })

  test('search should work', async ({ page }) => {
    await page.goto('/customers')

    // Type in search
    await page.fill('[data-testid="search-input"]', 'john')
    await page.press('[data-testid="search-input"]', 'Enter')

    // Verify filtered results
    const cards = page.locator('.customer-card')
    const count = await cards.count()

    for (let i = 0; i < count; i++) {
      const card = cards.nth(i)
      await expect(card).toContainText('john')
    }
  })

  test('keyboard navigation should work', async ({ page }) => {
    await page.goto('/customers')

    // Tab through elements
    await page.keyboard.press('Tab')
    let focused = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'))
    expect(focused).toBe('search-input')

    await page.keyboard.press('Tab')
    focused = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'))
    expect(focused).toBe('filter-status')

    // Enter key
    await page.keyboard.press('Enter')
    // Should activate focused element
  })

  test('local storage should work', async ({ page }) => {
    await page.goto('/settings')

    // Set value
    await page.evaluate(() => {
      localStorage.setItem('theme', 'dark')
    })

    // Verify value
    const theme = await page.evaluate(() => {
      return localStorage.getItem('theme')
    })
    expect(theme).toBe('dark')
  })

  test('cookies should work', async ({ page }) => {
    await page.goto('/')

    // Set cookie
    await page.context().addCookies([
      {
        name: 'session',
        value: 'abc123',
        domain: 'localhost',
        path: '/'
      }
    ])

    // Refresh and verify
    await page.reload()
    const cookies = await page.context().cookies()
    expect(cookies.find(c => c.name === 'session')).toBeTruthy()
  })

  test('Fetch API should work', async ({ page }) => {
    await page.route('**/api/customers', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ id: 1, name: 'John' }])
      })
    })

    const customers = await page.evaluate(async () => {
      const response = await fetch('/api/customers')
      return response.json()
    })

    expect(customers).toHaveLength(1)
    expect(customers[0].name).toBe('John')
  })

  test('WebSocket connection should work', async ({ page }) => {
    // Start WebSocket server (mock)
    await page.evaluate(() => {
      window.ws = new WebSocket('ws://localhost:8080')
    })

    // Mock WebSocket events
    await page.evaluate(() => {
      window.ws.addEventListener('open', () => {
        console.log('WebSocket connected')
      })
    })

    // Verify connection
    const connected = await page.evaluate(() => {
      return window.ws.readyState === WebSocket.OPEN
    })
    expect(connected).toBe(true)
  })

  test('Canvas rendering should work', async ({ page }) => {
    await page.goto('/dashboard')

    // Verify canvas element
    const canvas = page.locator('canvas')
    await expect(canvas).toBeVisible()

    // Verify canvas has content
    const hasContent = await canvas.evaluate((el) => {
      const context = el.getContext('2d')
      const imageData = context.getImageData(0, 0, 1, 1)
      return imageData.data[3] > 0 // Check alpha channel
    })
    expect(hasContent).toBe(true)
  })

  test('PDF generation should work', async ({ page }) => {
    await page.goto('/invoices')

    // Generate PDF
    await page.click('[data-testid="generate-pdf"]')

    // Wait for download
    const downloadPromise = page.waitForEvent('download')
    await page.click('[data-testid="download-pdf"]')
    const download = await downloadPromise

    expect(download.suggestedFilename()).toContain('.pdf')
  })

  test('file upload should work', async ({ page }) => {
    await page.goto('/customers')

    // Create test file
    const filePath = 'test-data/customer-import.csv'
    await page.evaluate((path) => {
      const fs = require('fs')
      fs.writeFileSync(path, 'name,email\njohn@example.com\njane@example.com')
    }, filePath)

    // Upload file
    const fileInput = page.locator('input[type="file"]')
    await fileInput.setInputFiles(filePath)

    // Verify upload
    await expect(page.locator('[data-testid="upload-status"]'))
      .toContainText('2 records imported')
  })

  test('drag and drop should work', async ({ page }) => {
    await page.goto('/assets/network-elements')

    // Drag element
    const source = page.locator('[data-testid="network-element"]')
    const target = page.locator('[data-testid="drop-zone"]')

    await source.dragTo(target)

    // Verify drop
    await expect(target.locator('.network-element')).toBeVisible()
    await expect(page.locator('[data-testid="success-message"]'))
      .toContainText('Element moved successfully')
  })

  test('clipboard API should work', async ({ page }) => {
    await page.goto('/customers')

    // Copy text
    await page.evaluate(() => {
      navigator.clipboard.writeText('Copied text')
    })

    // Verify clipboard content
    const clipboardText = await page.evaluate(() => {
      return navigator.clipboard.readText()
    })
    expect(clipboardText).toBe('Copied text')
  })

  test('geolocation API should work', async ({ page }) => {
    await page.goto('/coverage-nodes')

    // Mock geolocation
    await page.context().setGeolocation({ longitude: -73.9857, latitude: 40.7484 })

    // Access location-dependent feature
    await page.click('[data-testid="detect-location"]')

    // Verify location detected
    await expect(page.locator('[data-testid="location-display"]'))
      .toContainText('New York')
  })
})
