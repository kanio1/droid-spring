/**
 * Visual Regression Tests for CustomerForm Component
 * Using Percy for visual testing with accessibility validation
 */

import { test, expect } from '@playwright/test'
import { AccessibilityTest } from '../framework/accessibility/axe-testing'

// Note: This test requires PERCY_TOKEN environment variable
// To run: PERCY_TOKEN=your_token pnpm test:visual

test.describe('CustomerForm Visual Tests', () => {
  test('CustomerForm - Create Mode', async ({ page }) => {
    await page.goto('/customers/create')

    await page.waitForSelector('[data-testid="customer-form"]')

    await percySnapshot(page, 'CustomerForm - Create Mode', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerForm - Edit Mode', async ({ page }) => {
    await page.goto('/customers/edit/cust-001')

    await page.waitForSelector('[data-testid="customer-form"]')

    await percySnapshot(page, 'CustomerForm - Edit Mode', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerForm - View Mode', async ({ page }) => {
    await page.goto('/customers/view/cust-001')

    await page.waitForSelector('[data-testid="customer-form"]')

    await percySnapshot(page, 'CustomerForm - View Mode', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerForm - With Validation Errors', async ({ page }) => {
    await page.goto('/customers/create')

    await page.waitForSelector('[data-testid="customer-form"]')

    await page.click('[data-testid="save-button"]')

    await page.waitForTimeout(500)

    await percySnapshot(page, 'CustomerForm - Validation Errors', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerForm - Loading State', async ({ page }) => {
    await page.goto('/customers/create')

    await page.waitForSelector('[data-testid="customer-form"]')

    await page.fill('[name="firstName"]', 'John')
    await page.fill('[name="lastName"]', 'Doe')
    await page.fill('[name="email"]', 'john.doe@example.com')

    await page.click('[data-testid="save-button"]')

    await page.waitForSelector('[data-testid="loading"]', { state: 'visible' })

    await percySnapshot(page, 'CustomerForm - Loading State', {
      widths: [375, 768, 1280]
    })

    expect(await page.screenshot()).toBeTruthy()
  })

  test('CustomerForm - Accessibility Validation', async ({ page }) => {
    await page.goto('/customers/create')

    await page.waitForSelector('[data-testid="customer-form"]')

    // Run accessibility checks
    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa']
    })

    // Check all form fields have labels
    const labelCheck = await AccessibilityTest.checkFormLabels(page, 'form')
    expect(labelCheck.hasLabels).toBe(true)
    expect(labelCheck.missingLabels).toEqual([])

    // Check buttons have proper labels
    const saveBtn = await AccessibilityTest.checkAriaLabel(
      page,
      '[data-testid="save-button"]'
    )
    expect(saveBtn.hasLabel).toBe(true)

    // Take visual snapshot
    await percySnapshot(page, 'CustomerForm - Accessibility', {
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
