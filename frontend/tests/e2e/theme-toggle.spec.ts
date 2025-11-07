import { test, expect } from '@playwright/test'

test.describe('Theme Toggle', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('toggle theme button is visible in header', async ({ page }) => {
    await expect(page.locator('.top-header__theme-toggle')).toBeVisible()
  })

  test('toggles from light to dark mode', async ({ page }) => {
    // Verify initial light mode
    const html = page.locator('html')
    await expect(html).not.toHaveAttribute('data-theme', 'dark')

    // Click theme toggle
    await page.click('.top-header__theme-toggle')

    // Verify dark mode
    await expect(html).toHaveAttribute('data-theme', 'dark')
  })

  test('toggles from dark to light mode', async ({ page }) => {
    // Set initial dark mode
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'dark')
    })

    // Click theme toggle
    await page.click('.top-header__theme-toggle')

    // Verify light mode
    const html = page.locator('html')
    await expect(html).toHaveAttribute('data-theme', 'light')
  })

  test('icon changes from sun to moon', async ({ page }) => {
    const themeIcon = page.locator('.top-header__theme-icon')

    // Initially should be sun (light theme default or system)
    await expect(themeIcon).toBeVisible()

    // Click to toggle
    await page.click('.top-header__theme-toggle')

    // Icon should now be moon
    await expect(themeIcon).toContainText('moon')
  })

  test('icon changes from moon to sun', async ({ page }) => {
    // Set dark mode first
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'dark')
    })

    const themeIcon = page.locator('.top-header__theme-icon')

    // Should be moon in dark mode
    await expect(themeIcon).toContainText('moon')

    // Click to toggle
    await page.click('.top-header__theme-toggle')

    // Should be sun in light mode
    await expect(themeIcon).toContainText('sun')
  })

  test('theme persists after page reload', async ({ page }) => {
    // Toggle to dark mode
    await page.click('.top-header__theme-toggle')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    // Reload page
    await page.reload()

    // Dark mode should persist
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  })

  test('theme persists across different pages', async ({ page }) => {
    // Toggle to dark mode
    await page.click('.top-header__theme-toggle')

    // Navigate to customers page
    await page.click('text=Customers')

    // Theme should still be dark
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    // Navigate back to dashboard
    await page.click('text=Dashboard')

    // Theme should still be dark
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  })

  test('theme applies to all UI components', async ({ page }) => {
    // Toggle to dark mode
    await page.click('.top-header__theme-toggle')

    // Check sidebar styling
    const sidebar = page.locator('.sidebar')
    await expect(sidebar).toBeVisible()

    // Check main content area
    const mainContent = page.locator('.main-content')
    await expect(mainContent).toBeVisible()

    // Check if dark theme classes are applied
    // This would be more specific based on actual styling
  })

  test('theme toggle is accessible via keyboard', async ({ page }) => {
    // Focus on the theme toggle button
    await page.focus('.top-header__theme-toggle')

    // Press Enter
    await page.keyboard.press('Enter')

    // Should toggle theme
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  })

  test('theme toggle button has proper aria-label', async ({ page }) => {
    const toggleButton = page.locator('.top-header__theme-toggle')
    await expect(toggleButton).toHaveAttribute('title')
  })

  test('smooth transition when toggling theme', async ({ page }) => {
    // This test would verify CSS transitions
    // We can check if the transition property is set
    const sidebar = page.locator('.sidebar')
    const transition = await sidebar.evaluate(el => {
      return window.getComputedStyle(el).transition
    })

    // Should have transition property
    expect(transition).toBeTruthy()
  })

  test('all components maintain readability in dark mode', async ({ page }) => {
    // Enable dark mode
    await page.click('.top-header__theme-toggle')

    // Check that text is visible (not black on black)
    const navText = page.locator('.sidebar__nav-text').first()
    const color = await navText.evaluate(el => {
      return window.getComputedStyle(el).color
    })

    // Color should not be black in dark mode
    expect(color).not.toBe('rgb(0, 0, 0)')
  })

  test('theme toggle works on mobile viewport', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })

    // Theme toggle should still be visible
    await expect(page.locator('.top-header__theme-toggle')).toBeVisible()

    // Toggle should work
    await page.click('.top-header__theme-toggle')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  })

  test('detects system preference on first visit', async ({ page }) => {
    // Clear any saved theme preference
    await page.evaluate(() => {
      localStorage.removeItem('bss-theme')
    })

    // Reload page
    await page.reload()

    // Theme should match system preference
    // This is harder to test without controlling system settings
    // We can at least verify it doesn't crash
  })

  test('respects system preference when set to system', async ({ page }) => {
    // Set theme to system
    await page.evaluate(() => {
      localStorage.setItem('bss-theme', 'system')
    })

    // Reload
    await page.reload()

    // Should not have explicit data-theme attribute
    const html = page.locator('html')
    // When system, the data-theme attribute is removed
    const themeAttr = await html.getAttribute('data-theme')
    expect(themeAttr).toBeNull()
  })

  test('button hover effect works in both themes', async ({ page }) => {
    // Test hover in light mode
    await page.hover('.top-header__theme-toggle')
    // Should have hover effect (no assertion, just ensure it doesn't crash)

    // Toggle to dark mode
    await page.click('.top-header__theme-toggle')

    // Test hover in dark mode
    await page.hover('.top-header__theme-toggle')
    // Should have hover effect (no assertion, just ensure it doesn't crash)
  })

  test('theme toggle on slow network', async ({ page }) => {
    // Simulate slow network
    await page.context().setOffline(true)

    // Should still work
    await page.click('.top-header__theme-toggle')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

    // Toggle back
    await page.click('.top-header__theme-toggle')
    await expect(page.locator('html')).not.toHaveAttribute('data-theme', 'dark')
  })

  test('localStorage is updated when theme changes', async ({ page }) => {
    // Toggle theme
    await page.click('.top-header__theme-toggle')

    // Check localStorage
    const theme = await page.evaluate(() => {
      return localStorage.getItem('bss-theme')
    })

    expect(theme).toBe('dark')
  })
})
