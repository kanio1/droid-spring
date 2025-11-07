/**
 * Visual Regression Tests for CustomerList Component
 * Using Percy for visual testing with accessibility validation
 */

import { test, expect } from '@playwright/test'
import { AccessibilityTest } from '../framework/accessibility/axe-testing'

test.describe('CustomerList Visual Tests', () => {
  test('CustomerList - Grid View', async ({ page }) => {
    await page.goto('/customers')

    await page.waitForSelector('[data-testid="customer-list"]')

    await percySnapshot(page, 'CustomerList - Grid View', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerList - List View', async ({ page }) => {
    await page.goto('/customers')

    await page.waitForSelector('[data-testid="customer-list"]')

    await page.click('[data-testid="list-view-btn"]')

    await page.waitForTimeout(500)

    await percySnapshot(page, 'CustomerList - List View', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerList - Table View', async ({ page }) => {
    await page.goto('/customers')

    await page.waitForSelector('[data-testid="customer-list"]')

    await page.click('[data-testid="table-view-btn"]')

    await page.waitForTimeout(500)

    await percySnapshot(page, 'CustomerList - Table View', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerList - Empty State', async ({ page }) => {
    await page.goto('/customers?empty=true')

    await page.waitForSelector('[data-testid="customer-list"]')

    await percySnapshot(page, 'CustomerList - Empty State', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerList - Loading State', async ({ page }) => {
    await page.goto('/customers?loading=true')

    await page.waitForSelector('[data-testid="loading"]')

    await percySnapshot(page, 'CustomerList - Loading State', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerList - Error State', async ({ page }) => {
    await page.goto('/customers?error=true')

    await page.waitForSelector('[data-testid="customer-list"]')

    await percySnapshot(page, 'CustomerList - Error State', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerList - With Search and Filters', async ({ page }) => {
    await page.goto('/customers')

    await page.waitForSelector('[data-testid="customer-list"]')

    await page.fill('[data-testid="search-input"]', 'John')

    await page.waitForTimeout(500)

    await percySnapshot(page, 'CustomerList - Search and Filters', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerList - Accessibility Validation', async ({ page }) => {
    await page.goto('/customers')

    await page.waitForSelector('[data-testid="customer-list"]')

    // Run accessibility checks
    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa']
    })

    // Check search input is accessible
    const searchInput = page.locator('[data-testid="search-input"]')
    if (await searchInput.isVisible()) {
      const hasLabel = await searchInput.evaluate(input => {
        const id = input.getAttribute('id')
        const hasAriaLabel = input.hasAttribute('aria-label')
        const hasAriaLabelledby = input.hasAttribute('aria-labelledby')
        const hasLabel = id && document.querySelector(`label[for="${id}"]`)
        return hasAriaLabel || hasAriaLabelledby || hasLabel
      })
      expect(hasLabel).toBe(true)
    }

    // Check navigation is keyboard accessible
    const nav = page.locator('[data-testid="main-navigation"]')
    if (await nav.isVisible()) {
      const navCheck = await AccessibilityTest.checkKeyboardNavigation(
        page,
        '[data-testid="main-navigation"]'
      )
      expect(navCheck.isFocusable).toBe(true)
    }

    // Take visual snapshot
    await percySnapshot(page, 'CustomerList - Accessibility', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })
})

// Percy snapshot helper function
async function percySnapshot(page: any, name: string, options?: any) {
  const { percySnapshot } = await import('@percy/playwright')
  return percySnapshot(page, name, options)
}
