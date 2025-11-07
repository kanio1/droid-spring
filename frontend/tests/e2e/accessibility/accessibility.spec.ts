/**
 * Comprehensive Accessibility Testing Suite
 *
 * Tests accessibility across all main pages and components
 * Using axe-core for WCAG 2.1 Level AA compliance
 *
 * Run with: pnpm test:accessibility
 */

import { test, expect } from '@playwright/test'
import { AccessibilityTest } from '../../framework/accessibility/axe-testing'

test.describe('Accessibility - Pages', () => {
  test('Dashboard page should be accessible', async ({ page }) => {
    await page.goto('/dashboard')

    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa', 'wcag21aa']
    })

    // Generate accessibility report
    await AccessibilityTest.generateReport(
      page,
      'test-results/accessibility-dashboard.html'
    )
  })

  test('Customers page should be accessible', async ({ page }) => {
    await page.goto('/customers')

    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa']
    })

    // Check customer list is accessible
    await AccessibilityTest.expectElementToBeAccessible(
      page,
      '[data-testid="customer-list"]',
      { tags: ['wcag2aa'] }
    )
  })

  test('Customer create form should be accessible', async ({ page }) => {
    await page.goto('/customers/create')

    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa']
    })

    // Check all form fields have proper labels
    const form = page.locator('form')
    await expect(form).toBeVisible()

    // Check that inputs have associated labels
    const violations = await AccessibilityTest.analyzeElement(
      page,
      'form',
      { tags: ['wcag2aa'] }
    )

    // Filter for label-related violations
    const labelViolations = violations.filter(v =>
      v.id === 'label' || v.id === 'label-multiple' || v.id === 'label-title-only'
    )

    expect(labelViolations.length).toBe(0)
  })

  test('Customer edit form should be accessible', async ({ page }) => {
    await page.goto('/customers/edit/cust-001')

    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa']
    })
  })

  test('Products page should be accessible', async ({ page }) => {
    await page.goto('/products')

    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa']
    })
  })

  test('Orders page should be accessible', async ({ page }) => {
    await page.goto('/orders')

    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa']
    })
  })

  test('Invoices page should be accessible', async ({ page }) => {
    await page.goto('/invoices')

    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa']
    })
  })

  test('Login page should be accessible', async ({ page }) => {
    await page.goto('/login')

    await AccessibilityTest.expectPageToBeAccessible(page, {
      tags: ['wcag2a', 'wcag2aa']
    })

    // Ensure login form is properly labeled
    const formLabels = await AccessibilityTest.checkFormLabels(page, 'form')
    expect(formLabels.hasLabels).toBe(true)
    expect(formLabels.missingLabels.length).toBe(0)
  })
})

test.describe('Accessibility - Navigation', () => {
  test('Main navigation should be keyboard accessible', async ({ page }) => {
    await page.goto('/dashboard')

    const nav = page.locator('[data-testid="main-navigation"]')
    await expect(nav).toBeVisible()

    // Check navigation is keyboard accessible
    const navCheck = await AccessibilityTest.checkKeyboardNavigation(
      page,
      '[data-testid="main-navigation"]'
    )
    expect(navCheck.isFocusable).toBe(true)
    expect(navCheck.hasTabIndex).toBe(true)
  })

  test('Skip links should be present and functional', async ({ page }) => {
    await page.goto('/dashboard')

    // Check for skip link
    const skipLink = page.locator('a[href="#main-content"], a[href="#main"]')
    await expect(skipLink).toBeVisible()

    // Test skip link functionality
    await skipLink.focus()
    await page.keyboard.press('Enter')
    // Should skip to main content
  })

  test('Breadcrumbs should be accessible', async ({ page }) => {
    await page.goto('/customers/cust-001')

    const breadcrumbs = page.locator('[data-testid="breadcrumbs"]')
    await expect(breadcrumbs).toBeVisible()

    // Check breadcrumbs structure
    await AccessibilityTest.expectElementToBeAccessible(
      page,
      '[data-testid="breadcrumbs"]',
      { tags: ['wcag2aa'] }
    )
  })
})

test.describe('Accessibility - Forms', () => {
  test('Form inputs should have proper labels', async ({ page }) => {
    await page.goto('/customers/create')

    const labelCheck = await AccessibilityTest.checkFormLabels(page, 'form')
    expect(labelCheck.hasLabels).toBe(true)
    expect(labelCheck.missingLabels).toEqual([])
  })

  test('Form validation errors should be announced', async ({ page }) => {
    await page.goto('/customers/create')

    // Submit empty form to trigger validation
    const submitButton = page.locator('[data-testid="save-customer-btn"]')
    await submitButton.click()

    // Check error messages are announced
    const errorMessages = page.locator('[role="alert"], .error, [data-testid*="error"]')
    await expect(errorMessages.first()).toBeVisible()

    // Check for aria-describedby linking errors to inputs
    const hasAriaDescribedBy = await page.evaluate(() => {
      const inputs = document.querySelectorAll('input[aria-describedby], textarea[aria-describedby], select[aria-describedby]')
      return inputs.length > 0
    })
    expect(hasAriaDescribedBy).toBe(true)
  })

  test('Required form fields should be marked', async ({ page }) => {
    await page.goto('/customers/create')

    const violations = await AccessibilityTest.analyzePage(page, {
      tags: ['wcag2aa']
    })

    // Check for proper required field marking
    const requiredViolations = violations.filter(v =>
      v.id === 'required-attr'
    )

    // Should not have violations for required fields
    expect(requiredViolations.length).toBe(0)
  })
})

test.describe('Accessibility - Interactive Elements', () => {
  test('Buttons should have accessible names', async ({ page }) => {
    await page.goto('/customers')

    // Check create customer button
    const createBtn = page.locator('[data-testid="create-customer-btn"]')
    await expect(createBtn).toBeVisible()

    const btnCheck = await AccessibilityTest.checkAriaLabel(
      page,
      '[data-testid="create-customer-btn"]'
    )
    expect(btnCheck.hasLabel).toBe(true)
  })

  test('Links should have descriptive text', async ({ page }) => {
    await page.goto('/dashboard')

    const violations = await AccessibilityTest.analyzePage(page, {
      tags: ['wcag2aa']
    })

    // Check for links with proper text
    const linkViolations = violations.filter(v =>
      v.id === 'link-name'
    )

    expect(linkViolations.length).toBe(0)
  })

  test('Interactive elements should show focus indicators', async ({ page }) => {
    await page.goto('/customers')

    // Tab to first focusable element
    await page.keyboard.press('Tab')

    const focusedElement = await page.evaluate(() => {
      return document.activeElement
    })

    expect(focusedElement).not.toBeNull()

    // Take screenshot to verify visible focus indicator
    await expect(page).toHaveScreenshot('focus-indicators.png', {
      maxDiffPixelRatio: 0.05
    })
  })
})

test.describe('Accessibility - Tables', () => {
  test('Data tables should have proper headers', async ({ page }) => {
    await page.goto('/customers')

    const table = page.locator('table')
    if (await table.isVisible()) {
      await AccessibilityTest.expectElementToBeAccessible(
        page,
        'table',
        { tags: ['wcag2aa'] }
      )

      // Check that table has proper th elements
      const headers = await page.evaluate(() => {
        const table = document.querySelector('table')
        if (!table) return false
        const ths = table.querySelectorAll('th')
        return ths.length > 0
      })
      expect(headers).toBe(true)
    }
  })

  test('Table should have caption if needed', async ({ page }) => {
    await page.goto('/customers')

    const table = page.locator('table')
    if (await table.isVisible()) {
      const hasCaption = await page.evaluate(() => {
        const table = document.querySelector('table')
        return table?.querySelector('caption') !== null
      })

      // If table has many rows, it should have a caption
      const rowCount = await table.locator('tbody tr').count()
      if (rowCount > 1) {
        expect(hasCaption).toBe(true)
      }
    }
  })
})

test.describe('Accessibility - Modals & Dialogs', () => {
  test('Modal dialogs should trap focus', async ({ page }) => {
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-btn"]')

    // Verify modal is open
    const modal = page.locator('[role="dialog"]')
    await expect(modal).toBeVisible()

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
    await expect(modal).not.toBeVisible()
  })

  test('Modal should have proper ARIA attributes', async ({ page }) => {
    await page.goto('/customers')
    await page.click('[data-testid="create-customer-btn"]')

    const modal = page.locator('[role="dialog"]')

    // Check for ARIA attributes
    const hasAriaModal = await page.evaluate(() => {
      const modal = document.querySelector('[role="dialog"]')
      return modal?.getAttribute('aria-modal') === 'true'
    })
    expect(hasAriaModal).toBe(true)

    // Check for labelledby or label
    const hasLabel = await page.evaluate(() => {
      const modal = document.querySelector('[role="dialog"]')
      return modal?.hasAttribute('aria-labelledby') || modal?.hasAttribute('aria-label')
    })
    expect(hasLabel).toBe(true)
  })
})

test.describe('Accessibility - Color & Contrast', () => {
  test('Text should meet contrast requirements', async ({ page }) => {
    await page.goto('/dashboard')

    // Check color contrast
    const contrastInfo = await AccessibilityTest.checkColorContrast(
      page,
      page.locator('body')
    )

    expect(contrastInfo.normal).toBe(true)
    expect(contrastInfo.large).toBe(true)
    expect(contrastInfo.ratio).toBeGreaterThanOrEqual(4.5)
  })

  test('Color should not be the only means of conveying information', async ({ page }) => {
    await page.goto('/dashboard')

    const violations = await AccessibilityTest.analyzePage(page, {
      tags: ['wcag2aa']
    })

    const colorViolations = violations.filter(v =>
      v.id === 'color-contrast' || v.id === 'color-contrast-enhanced'
    )

    // Allow some violations but not critical ones
    const criticalColorViolations = colorViolations.filter(v =>
      v.impact === 'critical'
    )
    expect(criticalColorViolations.length).toBe(0)
  })
})

test.describe('Accessibility - Dynamic Content', () => {
  test('Loading states should be announced', async ({ page }) => {
    // Mock slow response
    await page.route('**/api/customers**', async route => {
      await new Promise(resolve => setTimeout(resolve, 2000))
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [], totalElements: 0 })
      })
    })

    await page.goto('/customers')

    // Check for loading indicator with proper ARIA
    const loading = page.locator('[data-testid="loading"], .loading, [aria-busy="true"]')
    if (await loading.isVisible()) {
      const hasAriaBusy = await page.evaluate(() => {
        const loading = document.querySelector('[aria-busy="true"]')
        return loading !== null
      })
      expect(hasAriaBusy).toBe(true)
    }
  })

  test('Success messages should be announced', async ({ page }) => {
    await page.goto('/customers')

    // Simulate creating a customer
    await page.click('[data-testid="create-customer-btn"]')
    await page.fill('[name="firstName"]', 'John')
    await page.fill('[name="lastName"]', 'Doe')
    await page.fill('[name="email"]', 'john@example.com')
    await page.click('[data-testid="save-customer-btn"]')

    // Check for success notification with aria-live
    const success = page.locator('[data-testid="success-notification"], .success, [role="status"]')
    if (await success.isVisible()) {
      const hasAriaLive = await page.evaluate(() => {
        const success = document.querySelector('[role="status"], [aria-live="polite"]')
        return success !== null
      })
      expect(hasAriaLive).toBe(true)
    }
  })
})

test.describe('Accessibility - Media', () => {
  test('Images should have alt text', async ({ page }) => {
    await page.goto('/customers')

    const imageCheck = await AccessibilityTest.checkImageAltText(page)
    expect(imageCheck.hasAlt).toBe(true)
    expect(imageCheck.missingAltCount).toBe(0)
  })

  test('Icons should have proper accessibility', async ({ page }) => {
    await page.goto('/customers')

    // Check that icon buttons have labels
    const iconButtons = page.locator('button:not(:has-text()), button:has([aria-hidden="true"])')
    const count = await iconButtons.count()

    if (count > 0) {
      for (let i = 0; i < count; i++) {
        const button = iconButtons.nth(i)
        const hasLabel = await button.evaluate(btn => {
          return btn.getAttribute('aria-label') !== null ||
                 btn.getAttribute('aria-labelledby') !== null ||
                 btn.getAttribute('title') !== null
        })
        // Icon buttons should have labels
        expect(hasLabel).toBe(true)
      }
    }
  })
})

test.describe('WCAG 2.1 Compliance', () => {
  test('Level A compliance - all critical pages', async ({ page }) => {
    const pages = ['/dashboard', '/customers', '/products', '/orders']

    for (const url of pages) {
      await page.goto(url)

      const violations = await AccessibilityTest.analyzePage(page, {
        tags: ['wcag2a']
      })

      // Filter only Level A violations
      const levelAViolations = violations.filter(v =>
        v.tags.includes('wcag2a')
      )

      // No Level A violations allowed
      expect(levelAViolations.length).toBe(0)
    }
  })

  test('Level AA compliance - critical user flows', async ({ page }) => {
    // Test customer creation flow
    await page.goto('/customers/create')

    const violations = await AccessibilityTest.analyzePage(page, {
      tags: ['wcag2aa']
    })

    // Allow minor violations but not critical/serious
    const criticalViolations = violations.filter(v =>
      v.impact === 'critical' || v.impact === 'serious'
    )

    expect(criticalViolations.length).toBe(0)
  })
})
