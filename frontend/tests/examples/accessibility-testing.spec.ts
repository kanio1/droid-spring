/**
 * Accessibility Testing Examples
 *
 * Demonstrates how to test accessibility with Playwright and axe-core
 *
 * Run with: npx playwright test accessibility-testing.spec.ts
 */

import { test, expect } from '@playwright/test'
import { AccessibilityTest } from '../framework/accessibility/axe-testing'

test.describe('Accessibility Testing', () => {
  test('homepage should be accessible', async ({ page }) => {
    await page.goto('/')

    // Run comprehensive accessibility check
    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa', 'wcag21aa']
    })
  })

  test('customer list should be accessible', async ({ page }) => {
    await page.goto('/customers')

    // Check specific element accessibility
    await AccessibilityTest.expectElementToBeAccessible(
      page,
      '[data-testid="customer-list"]',
      { tags: ['wcag2aa'] }
    )

    // Check all images have alt text
    const imageCheck = await AccessibilityTest.checkImageAltText(page)
    expect(imageCheck.hasAlt).toBe(true)
  })

  test('form inputs should have proper labels', async ({ page }) => {
    await page.goto('/customers/create')

    // Check form has proper labels
    const labelCheck = await AccessibilityTest.checkFormLabels(page, 'form')
    expect(labelCheck.hasLabels).toBe(true)

    // List any missing labels
    if (labelCheck.missingLabels.length > 0) {
      console.log('Missing labels:', labelCheck.missingLabels)
    }
  })

  test('keyboard navigation should work', async ({ page }) => {
    await page.goto('/customers')

    // Check navigation menu is keyboard accessible
    const navCheck = await AccessibilityTest.checkKeyboardNavigation(
      page,
      '[data-testid="main-navigation"]'
    )
    expect(navCheck.isFocusable).toBe(true)
  })

  test('buttons should have ARIA labels', async ({ page }) => {
    await page.goto('/customers')

    // Check action buttons have proper labels
    const createBtn = await AccessibilityTest.checkAriaLabel(
      page,
      '[data-testid="create-customer-btn"]'
    )
    expect(createBtn.hasLabel).toBe(true)
  })

  test('color contrast should meet WCAG standards', async ({ page }) => {
    await page.goto('/dashboard')

    const contrastInfo = await AccessibilityTest.checkColorContrast(
      page,
      page.locator('.dashboard-card')
    )

    expect(contrastInfo.normal).toBe(true)
    expect(contrastInfo.large).toBe(true)
    expect(contrastInfo.ratio).toBeGreaterThanOrEqual(4.5)
  })

  test('page should have no critical violations', async ({ page }) => {
    await page.goto('/invoices')

    await expect(page).toHaveNoCriticalViolations()
  })

  test('generate accessibility report', async ({ page }) => {
    await page.goto('/customers')

    // Generate detailed HTML report
    await AccessibilityTest.generateReport(
      page,
      'test-results/accessibility-customer-list.html'
    )

    // Verify report was generated
    const fs = require('fs')
    expect(fs.existsSync('test-results/accessibility-customer-list.html')).toBe(true)
  })
})

test.describe('WCAG 2.1 Compliance', () => {
  test('Level A compliance', async ({ page }) => {
    await page.goto('/products')

    const violations = await AccessibilityTest.analyzePage(page, {
      tags: ['wcag2a']
    })

    // Filter only Level A violations
    const levelAViolations = violations.filter(v =>
      v.tags.includes('wcag2a')
    )

    expect(levelAViolations.length).toBe(0)
  })

  test('Level AA compliance', async ({ page }) => {
    await page.goto('/orders')

    const violations = await AccessibilityTest.analyzePage(page, {
      tags: ['wcag2aa']
    })

    expect(violations.length).toBe(0)
  })
})

test.describe('Specific Accessibility Features', () => {
  test('skip links should be present', async ({ page }) => {
    await page.goto('/')

    // Check for skip link
    const skipLink = page.locator('a[href="#main-content"]')
    await expect(skipLink).toBeVisible()
  })

  test('focus indicators should be visible', async ({ page }) => {
    await page.goto('/')

    // Tab to first focusable element
    await page.keyboard.press('Tab')

    const focusedElement = await page.evaluate(() => {
      return document.activeElement
    })

    expect(focusedElement).not.toBeNull()

    // Take screenshot to verify visible focus indicator
    await expect(page).toHaveScreenshot('focus-indicator.png')
  })

  test('ARIA live regions should announce updates', async ({ page }) => {
    await page.goto('/invoices')

    // Check for aria-live region
    const liveRegion = page.locator('[aria-live]')
    await expect(liveRegion).toBeVisible()
  })

  test('modal dialogs should trap focus', async ({ page }) => {
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-btn"]')

    // Verify modal is open
    await expect(page.locator('[role="dialog"]')).toBeVisible()

    // Check modal traps focus
    await page.keyboard.press('Tab')
    let focusedInModal = await page.evaluate(() => {
      const modal = document.querySelector('[role="dialog"]')
      const active = document.activeElement
      return modal?.contains(active as Node)
    })
    expect(focusedInModal).toBe(true)

    // Verify Escape key closes modal
    await page.keyboard.press('Escape')
    await expect(page.locator('[role="dialog"]')).not.toBeVisible()
  })
})
