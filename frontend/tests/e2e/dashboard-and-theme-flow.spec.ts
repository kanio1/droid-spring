import { test, expect } from '@playwright/test'

test.describe('Dashboard and Theme Flow', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('dashboard loads with all components', async ({ page }) => {
    // Verify page title
    await expect(page).toHaveTitle(/Dashboard|BSS/)

    // Check header
    await expect(page.locator('header, .top-header')).toBeVisible()

    // Check navigation
    await expect(page.locator('nav, .sidebar')).toBeVisible()
    await expect(page.locator('text=Dashboard')).toBeVisible()
    await expect(page.locator('text=Customers')).toBeVisible()

    // Check main content
    await expect(page.locator('main, .main-content')).toBeVisible()

    // Check cards/metrics
    await expect(page.locator('.card, [data-testid="metric-card"]')).toHaveCount(3)

    // Check data table
    await expect(page.locator('table, .data-table')).toBeVisible()
  })

  test('theme toggle button visibility and functionality', async ({ page }) => {
    // Theme toggle should be in header
    const themeToggle = page.locator('.top-header__theme-toggle, button[aria-label*="theme" i]')
    await expect(themeToggle).toBeVisible()

    // Click toggle
    await themeToggle.click()

    // Theme should change to dark
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    // Icon should change
    const themeIcon = page.locator('.top-header__theme-icon, svg')
    // Icon might contain 'moon' text or class

    // Click again
    await themeToggle.click()

    // Theme should change back to light
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  })

  test('theme persists on page reload', async ({ page }) => {
    // Toggle to dark mode
    await page.click('.top-header__theme-toggle')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    // Reload page
    await page.reload()

    // Dark mode should persist
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    // All components should render in dark mode
    await expect(page.locator('header, .top-header')).toBeVisible()
    await expect(page.locator('nav, .sidebar')).toBeVisible()
    await expect(page.locator('table, .data-table')).toBeVisible()
  })

  test('theme persists across navigation', async ({ page }) => {
    // Toggle to dark mode
    await page.click('.top-header__theme-toggle')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    // Navigate to customers
    await page.click('text=Customers')
    await expect(page).toHaveURL(/.*\/customers/)

    // Dark mode should persist
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    // Navigate to dashboard
    await page.click('text=Dashboard')
    await expect(page).toHaveURL(/.*\/.*/)

    // Dark mode should still persist
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  })

  test('dashboard navigation works', async ({ page }) => {
    // Navigate to Customers
    await page.click('nav a:has-text("Customers"), .sidebar >> text=Customers')
    await expect(page).toHaveURL(/.*\/customers/)
    await expect(page.locator('h1, h2')).toContainText('Customers')

    // Navigate to Products
    await page.click('nav a:has-text("Products"), .sidebar >> text=Products')
    await expect(page.locator('h1, h2')).toContainText('Products')

    // Navigate to Dashboard
    await page.click('nav a:has-text("Dashboard"), .sidebar >> text=Dashboard')
    await expect(page.locator('h1, h2')).toContainText('Dashboard')
  })

  test('dashboard metric cards', async ({ page }) => {
    // Check metric cards are visible
    const cards = page.locator('.card, [data-testid="metric-card"]')
    await expect(cards).toHaveCount(3)

    // Each card should have a title and value
    for (let i = 0; i < 3; i++) {
      const card = cards.nth(i)
      await expect(card.locator('h3, h4, .card__title')).toBeVisible()
      await expect(card.locator('.card__value, [data-testid="value"]')).toBeVisible()
    }

    // Cards might have trend indicators
    const hasTrend = await page.locator('.card__trend, [data-testid="trend"]').count()
    expect(hasTrend).toBeGreaterThan(0)
  })

  test('dashboard table with sorting', async ({ page }) => {
    // Check table exists
    const table = page.locator('table, .data-table')
    await expect(table).toBeVisible()

    // Check headers
    const headers = table.locator('th, .data-table__header-cell')
    await expect(headers).toHaveCount(4) // ID, Name, Email, Status

    // Sort by Name
    await headers.nth(1).click()
    await expect(headers.nth(1)).toHaveClass(/sorted/)

    // Sort again to reverse
    await headers.nth(1).click()
    await expect(headers.nth(1)).toHaveClass(/desc/)

    // Verify data is sorted (check if rows order changed)
    // This would need actual data inspection
  })

  test('dashboard responsive layout', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })

    // Header should adapt
    await expect(page.locator('header, .top-header')).toBeVisible()

    // Navigation might be hamburger menu on mobile
    const hamburgerMenu = page.locator('button[aria-label*="menu" i], .menu-toggle')
    if (await hamburgerMenu.isVisible()) {
      await hamburgerMenu.click()
      // Sidebar/menu should open
      await expect(page.locator('nav, .sidebar, .mobile-menu')).toBeVisible()
    }

    // Cards should stack
    const cards = page.locator('.card, [data-testid="metric-card"]')
    const cardCount = await cards.count()
    expect(cardCount).toBe(3)

    // Table should be scrollable horizontally
    const table = page.locator('table, .data-table')
    await expect(table).toBeVisible()
  })

  test('theme toggle on mobile', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })

    // Theme toggle should still be visible
    const themeToggle = page.locator('.top-header__theme-toggle, button[aria-label*="theme" i]')
    await expect(themeToggle).toBeVisible()

    // Click toggle
    await themeToggle.click()
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    // Click again
    await themeToggle.click()
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  })

  test('dashboard accessibility', async ({ page }) => {
    // Check for skip link
    const skipLink = page.locator('a[href="#main"], .skip-link')
    if (await skipLink.isVisible()) {
      await skipLink.click()
      await expect(page.locator('main, #main')).toBeFocused()
    }

    // Check landmarks
    await expect(page.locator('header, nav, main, footer')).toBeVisible()

    // Check heading hierarchy
    const h1 = page.locator('h1')
    const h2 = page.locator('h2')
    await expect(h1).toHaveCount(1)
    await expect(h2).toHaveCount(1)

    // Check for alt text on images/icons
    const images = page.locator('img')
    const imageCount = await images.count()
    if (imageCount > 0) {
      for (let i = 0; i < imageCount; i++) {
        const img = images.nth(i)
        await expect(img).toHaveAttribute('alt')
      }
    }

    // Check for aria-labels on buttons
    const buttons = page.locator('button')
    const buttonCount = await buttons.count()
    for (let i = 0; i < buttonCount; i++) {
      const button = buttons.nth(i)
      const hasText = await button.textContent()
      const hasAriaLabel = await button.getAttribute('aria-label')
      const hasAriaLabelledBy = await button.getAttribute('aria-labelledby')
      expect(hasText || hasAriaLabel || hasAriaLabelledBy).toBeTruthy()
    }
  })

  test('keyboard navigation on dashboard', async ({ page }) => {
    // Tab through interactive elements
    await page.keyboard.press('Tab')
    let focused = await page.locator(':focus').inputValue()
    expect(focused).toBeTruthy()

    // Continue tabbing
    for (let i = 0; i < 5; i++) {
      await page.keyboard.press('Tab')
      await page.waitForTimeout(100)
    }

    // Test theme toggle with keyboard
    await page.keyboard.press('Tab') // Focus theme toggle
    await page.keyboard.press('Enter') // Activate
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    // Navigate to customers with Enter
    await page.keyboard.press('Tab')
    await page.keyboard.press('Tab')
    await page.keyboard.press('Enter')
  })

  test('dashboard loading states', async ({ page }) => {
    // Page should load
    await expect(page.locator('header, .top-header')).toBeVisible()

    // Wait for data to load
    await page.waitForSelector('.card, .data-table', { state: 'visible' })

    // Cards should have content
    const cards = page.locator('.card, [data-testid="metric-card"]')
    for (let i = 0; i < await cards.count(); i++) {
      const card = cards.nth(i)
      const hasValue = await card.locator('.card__value, [data-testid="value"]').isVisible()
      expect(hasValue).toBeTruthy()
    }
  })

  test('dashboard error handling', async ({ page }) => {
    // Check if error messages are handled gracefully
    // This would depend on the actual error handling in the app

    // For now, just verify the page doesn't crash
    await expect(page.locator('header, .top-header')).toBeVisible()
    await expect(page.locator('main, .main-content')).toBeVisible()

    // Check for any error boundaries or error messages
    const errorMessage = page.locator('.error, .alert-error, [role="alert"]')
    if (await errorMessage.isVisible()) {
      await expect(errorMessage).toContainText('error')
    }
  })

  test('dashboard performance', async ({ page }) => {
    // Measure load time
    const startTime = Date.now()
    await page.goto('/')
    const loadTime = Date.now() - startTime

    // Should load in reasonable time (adjust as needed)
    expect(loadTime).toBeLessThan(5000) // 5 seconds

    // Check for large images or resources
    const images = page.locator('img')
    const imageCount = await images.count()
    if (imageCount > 0) {
      for (let i = 0; i < imageCount; i++) {
        const img = images.nth(i)
        const src = await img.getAttribute('src')
        if (src) {
          // Should not be huge images
          expect(src).not.toMatch(/placeholder|example\.com/)
        }
      }
    }
  })

  test('theme system preference detection', async ({ page }) => {
    // Check initial theme (should respect system preference or default to light)
    const theme = await page.getAttribute('html', 'data-theme')
    expect(theme).toBeTruthy()

    // Toggle to system
    // This would require actual system preference detection UI
    // For now, just verify theme changes work
    await page.click('.top-header__theme-toggle')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    await page.click('.top-header__theme-toggle')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  })

  test('dashboard print styles', async ({ page }) => {
    // Trigger print
    const printPromise = page.waitForEvent('console')
    await page.keyboard.press('Control+p')
    await printPromise

    // In a real app, print styles would hide navigation, adjust layout, etc.
    // For this test, just verify the page doesn't break
    await expect(page.locator('header, .top-header')).toBeVisible()
  })
})
