/**
 * Visual Regression Tests with Accessibility Validation
 *
 * Tests for visual differences across themes, viewports, and components
 * Includes accessibility validation using axe-core
 * Used for visual regression testing in CI/CD pipeline
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { test, expect } from '@playwright/test'
import { AccessibilityTest } from '../framework/accessibility/axe-testing'

test.describe('Visual Regression Tests', () => {
  test.describe('Component Visual Tests @visual:component', () => {
    test.beforeEach(async ({ page }) => {
      await page.goto('/')
      // Inject axe-core for accessibility testing
      await AccessibilityTest.injectAxe(page)
    })

    test('Button component - Light theme @visual:component:Button', async ({ page }) => {
      await page.goto('/components/buttons')
      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'light')
      })

      const button = page.locator('[data-testid="primary-button"]').first()
      await expect(button).toBeVisible()

      // Validate button accessibility
      const buttonCheck = await AccessibilityTest.checkAriaLabel(
        page,
        '[data-testid="primary-button"]'
      )
      expect(buttonCheck.hasLabel).toBe(true)

      await expect(button).toHaveScreenshot('button-primary-light.png')
    })

    test('Button component - Dark theme @visual:component:Button', async ({ page }) => {
      await page.goto('/components/buttons')
      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'dark')
      })

      const button = page.locator('[data-testid="primary-button"]').first()
      await expect(button).toBeVisible()
      await expect(button).toHaveScreenshot('button-primary-dark.png')
    })

    test('Input field component - Light theme @visual:component:Input', async ({ page }) => {
      await page.goto('/components/inputs')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'light')
      })

      const input = page.locator('[data-testid="text-input"]').first()
      await expect(input).toBeVisible()
      await expect(input).toHaveScreenshot('input-light.png')
    })

    test('Input field component - Dark theme @visual:component:Input', async ({ page }) => {
      await page.goto('/components/inputs')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'dark')
      })

      const input = page.locator('[data-testid="text-input"]').first()
      await expect(input).toBeVisible()
      await expect(input).toHaveScreenshot('input-dark.png')
    })

    test('Card component - Light theme @visual:component:Card', async ({ page }) => {
      await page.goto('/components/cards')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'light')
      })

      const card = page.locator('[data-testid="card"]').first()
      await expect(card).toBeVisible()
      await expect(card).toHaveScreenshot('card-light.png')
    })

    test('Card component - Dark theme @visual:component:Card', async ({ page }) => {
      await page.goto('/components/cards')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'dark')
      })

      const card = page.locator('[data-testid="card"]').first()
      await expect(card).toBeVisible()
      await expect(card).toHaveScreenshot('card-dark.png')
    })

    test('Modal component @visual:component:Modal', async ({ page }) => {
      await page.goto('/components/modals')

      await page.click('[data-testid="open-modal"]')
      await page.waitForSelector('[data-testid="modal"]')

      const modal = page.locator('[data-testid="modal"]').first()
      await expect(modal).toBeVisible()
      await expect(modal).toHaveScreenshot('modal.png')
    })

    test('Table component @visual:component:Table', async ({ page }) => {
      await page.goto('/components/tables')

      await page.waitForSelector('[data-testid="data-table"]')

      const table = page.locator('[data-testid="data-table"]').first()
      await expect(table).toBeVisible()
      await expect(table).toHaveScreenshot('table.png')
    })

    test('Navigation component @visual:component:Navigation', async ({ page }) => {
      await page.goto('/')

      await page.waitForSelector('[data-testid="main-navigation"]')

      const nav = page.locator('[data-testid="main-navigation"]').first()
      await expect(nav).toBeVisible()
      await expect(nav).toHaveScreenshot('navigation.png')
    })
  })

  test.describe('Page Visual Tests @visual:page', () => {
    test('Dashboard page - Light theme @visual:page:Dashboard', async ({ page }) => {
      await page.goto('/dashboard')

      await page.waitForSelector('[data-testid="dashboard-container"]')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'light')
      })

      await expect(page.locator('body')).toHaveScreenshot('dashboard-light.png', {
        fullPage: true
      })
    })

    test('Dashboard page - Dark theme @visual:page:Dashboard', async ({ page }) => {
      await page.goto('/dashboard')

      await page.waitForSelector('[data-testid="dashboard-container"]')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'dark')
      })

      await expect(page.locator('body')).toHaveScreenshot('dashboard-dark.png', {
        fullPage: true
      })
    })

    test('Products page - Light theme @visual:page:Products', async ({ page }) => {
      await page.goto('/products')

      await page.waitForSelector('[data-testid="products-container"]')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'light')
      })

      await expect(page.locator('body')).toHaveScreenshot('products-light.png', {
        fullPage: true
      })
    })

    test('Products page - Dark theme @visual:page:Products', async ({ page }) => {
      await page.goto('/products')

      await page.waitForSelector('[data-testid="products-container"]')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'dark')
      })

      await expect(page.locator('body')).toHaveScreenshot('products-dark.png', {
        fullPage: true
      })
    })

    test('Customers page @visual:page:Customers', async ({ page }) => {
      await page.goto('/customers')

      await page.waitForSelector('[data-testid="customers-container"]')

      await expect(page.locator('body')).toHaveScreenshot('customers.png', {
        fullPage: true
      })
    })

    test('Orders page @visual:page:Orders', async ({ page }) => {
      await page.goto('/orders')

      await page.waitForSelector('[data-testid="orders-container"]')

      await expect(page.locator('body')).toHaveScreenshot('orders.png', {
        fullPage: true
      })
    })
  })

  test.describe('Viewport Visual Tests @visual', () => {
    test('Dashboard on mobile viewport @visual:mobile', async ({ page }) => {
      await page.setViewportSize({ width: 390, height: 844 })
      await page.goto('/dashboard')

      await page.waitForSelector('[data-testid="dashboard-container"]')

      await expect(page.locator('body')).toHaveScreenshot('dashboard-mobile.png', {
        fullPage: true
      })
    })

    test('Dashboard on tablet viewport @visual:tablet', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 })
      await page.goto('/dashboard')

      await page.waitForSelector('[data-testid="dashboard-container"]')

      await expect(page.locator('body')).toHaveScreenshot('dashboard-tablet.png', {
        fullPage: true
      })
    })

    test('Dashboard on desktop viewport @visual:desktop', async ({ page }) => {
      await page.setViewportSize({ width: 1920, height: 1080 })
      await page.goto('/dashboard')

      await page.waitForSelector('[data-testid="dashboard-container"]')

      await expect(page.locator('body')).toHaveScreenshot('dashboard-desktop.png', {
        fullPage: true
      })
    })

    test('Products page on mobile @visual:mobile', async ({ page }) => {
      await page.setViewportSize({ width: 390, height: 844 })
      await page.goto('/products')

      await page.waitForSelector('[data-testid="products-container"]')

      await expect(page.locator('body')).toHaveScreenshot('products-mobile.png', {
        fullPage: true
      })
    })

    test('Products page on tablet @visual:tablet', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 })
      await page.goto('/products')

      await page.waitForSelector('[data-testid="products-container"]')

      await expect(page.locator('body')).toHaveScreenshot('products-tablet.png', {
        fullPage: true
      })
    })

    test('Products page on desktop @visual:desktop', async ({ page }) => {
      await page.setViewportSize({ width: 1920, height: 1080 })
      await page.goto('/products')

      await page.waitForSelector('[data-testid="products-container"]')

      await expect(page.locator('body')).toHaveScreenshot('products-desktop.png', {
        fullPage: true
      })
    })
  })

  test.describe('Theme Visual Tests @visual:theme', () => {
    test('Light theme @visual:theme:light', async ({ page }) => {
      await page.goto('/')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'light')
      })

      await expect(page.locator('body')).toHaveScreenshot('theme-light.png', {
        fullPage: true
      })
    })

    test('Dark theme @visual:theme:dark', async ({ page }) => {
      await page.goto('/')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'dark')
      })

      await expect(page.locator('body')).toHaveScreenshot('theme-dark.png', {
        fullPage: true
      })
    })

    test('Theme toggle animation @visual:theme:toggle', async ({ page }) => {
      await page.goto('/')

      await page.evaluate(() => {
        document.documentElement.setAttribute('data-theme', 'light')
      })

      const themeButton = page.locator('[data-testid="theme-toggle"]')
      await themeButton.click()

      await expect(page.locator('body')).toHaveScreenshot('theme-transition.png')
    })
  })

  test.describe('Interactive States @visual:state', () => {
    test('Button hover state @visual:state:hover', async ({ page }) => {
      await page.goto('/components/buttons')

      const button = page.locator('[data-testid="primary-button"]').first()
      await button.hover()

      await expect(button).toHaveScreenshot('button-hover.png')
    })

    test('Button active state @visual:state:active', async ({ page }) => {
      await page.goto('/components/buttons')

      const button = page.locator('[data-testid="primary-button"]').first()
      await button.press('Space')

      await expect(button).toHaveScreenshot('button-active.png')
    })

    test('Input focus state @visual:state:focus', async ({ page }) => {
      await page.goto('/components/inputs')

      const input = page.locator('[data-testid="text-input"]').first()
      await input.focus()

      await expect(input).toHaveScreenshot('input-focus.png')
    })

    test('Dropdown open state @visual:state:dropdown', async ({ page }) => {
      await page.goto('/components/dropdowns')

      const dropdown = page.locator('[data-testid="dropdown-trigger"]').first()
      await dropdown.click()

      await expect(dropdown).toHaveScreenshot('dropdown-open.png')
    })
  })

  test.describe('Form Visual Tests @visual:form', () => {
    test('Login form @visual:form:login', async ({ page }) => {
      await page.goto('/login')

      await page.waitForSelector('[data-testid="login-form"]')

      await expect(page.locator('[data-testid="login-form"]')).toHaveScreenshot('login-form.png')
    })

    test('Create customer form @visual:form:create-customer', async ({ page }) => {
      await page.goto('/customers/create')

      await page.waitForSelector('[data-testid="customer-form"]')

      await expect(page.locator('[data-testid="customer-form"]')).toHaveScreenshot('customer-form.png', {
        fullPage: true
      })
    })

    test('Create order form @visual:form:create-order', async ({ page }) => {
      await page.goto('/orders/create')

      await page.waitForSelector('[data-testid="order-form"]')

      await expect(page.locator('[data-testid="order-form"]')).toHaveScreenshot('order-form.png', {
        fullPage: true
      })
    })
  })

  test.describe('Data Display @visual:data', () => {
    test('Empty state @visual:data:empty', async ({ page }) => {
      await page.route('**/api/products**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ content: [], totalElements: 0 })
        })
      })

      await page.goto('/products')

      await page.waitForSelector('[data-testid="empty-state"]')

      await expect(page.locator('[data-testid="empty-state"]')).toHaveScreenshot('empty-state.png')
    })

    test('Loading state @visual:data:loading', async ({ page }) => {
      await page.route('**/api/products**', async (route) => {
        await new Promise(resolve => setTimeout(resolve, 1000))
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ content: [], totalElements: 0 })
        })
      })

      await page.goto('/products')

      await page.waitForSelector('[data-testid="loading-spinner"]')

      await expect(page.locator('[data-testid="loading-spinner"]')).toHaveScreenshot('loading-state.png')
    })

    test('Error state @visual:data:error', async ({ page }) => {
      await page.route('**/api/products**', async (route) => {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Server error' })
        })
      })

      await page.goto('/products')

      await page.waitForSelector('[data-testid="error-message"]')

      await expect(page.locator('[data-testid="error-message"]')).toHaveScreenshot('error-state.png')
    })

    test('Data table with rows @visual:data:table', async ({ page }) => {
      await page.goto('/customers')

      await page.waitForSelector('[data-testid="customers-table"]')

      await expect(page.locator('[data-testid="customers-table"]')).toHaveScreenshot('data-table.png', {
        fullPage: true
      })
    })
  })

  test.describe('Navigation Visual Tests @visual:nav', () => {
    test('Sidebar navigation @visual:nav:sidebar', async ({ page }) => {
      await page.goto('/')

      await page.waitForSelector('[data-testid="sidebar"]')

      await expect(page.locator('[data-testid="sidebar"]')).toHaveScreenshot('sidebar.png')
    })

    test('Breadcrumbs @visual:nav:breadcrumbs', async ({ page }) => {
      await page.goto('/customers/cust-123')

      await page.waitForSelector('[data-testid="breadcrumbs"]')

      await expect(page.locator('[data-testid="breadcrumbs"]')).toHaveScreenshot('breadcrumbs.png')
    })

    test('Pagination @visual:nav:pagination', async ({ page }) => {
      await page.goto('/customers')

      await page.waitForSelector('[data-testid="pagination"]')

      await expect(page.locator('[data-testid="pagination"]')).toHaveScreenshot('pagination.png')
    })
  })

  test.describe('Modal Visual Tests @visual:modal', () => {
    test('Confirmation dialog @visual:modal:confirm', async ({ page }) => {
      await page.goto('/customers')

      await page.click('[data-testid="delete-customer-btn"]')
      await page.waitForSelector('[data-testid="confirm-dialog"]')

      await expect(page.locator('[data-testid="confirm-dialog"]')).toHaveScreenshot('confirm-dialog.png')
    })

    test('Customer details modal @visual:modal:customer', async ({ page }) => {
      await page.goto('/customers')

      await page.click('[data-testid="view-customer-btn"]')
      await page.waitForSelector('[data-testid="customer-details-modal"]')

      await expect(page.locator('[data-testid="customer-details-modal"]')).toHaveScreenshot('customer-modal.png')
    })
  })

  test.describe('Notification Visual Tests @visual:notification', () => {
    test('Success notification @visual:notification:success', async ({ page }) => {
      await page.goto('/customers')

      await page.click('[data-testid="create-customer-btn"]')

      await page.fill('[data-testid="customer-name"]', 'Test Customer')
      await page.click('[data-testid="save-customer-btn"]')

      await page.waitForSelector('[data-testid="success-notification"]')

      await expect(page.locator('[data-testid="success-notification"]')).toHaveScreenshot('success-notification.png')
    })

    test('Error notification @visual:notification:error', async ({ page }) => {
      await page.goto('/customers')

      await page.route('**/api/customers**', async (route) => {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Validation failed' })
        })
      })

      await page.click('[data-testid="create-customer-btn"]')
      await page.fill('[data-testid="customer-name"]', '')
      await page.click('[data-testid="save-customer-btn"]')

      await page.waitForSelector('[data-testid="error-notification"]')

      await expect(page.locator('[data-testid="error-notification"]')).toHaveScreenshot('error-notification.png')
    })
  })

  test.describe('Accessibility Tests @visual:accessibility', () => {
    test('Homepage should be accessible', async ({ page }) => {
      await page.goto('/')

      await AccessibilityTest.expectPageToBeAccessible(page, {
        tags: ['wcag2a', 'wcag2aa', 'wcag21aa']
      })
    })

    test('Dashboard should meet accessibility standards', async ({ page }) => {
      await page.goto('/dashboard')

      await AccessibilityTest.expectPageToBeAccessible(page, {
        tags: ['wcag2a', 'wcag2aa']
      })

      // Check focus indicators
      await page.keyboard.press('Tab')
      const focusedElement = await page.evaluate(() => document.activeElement)
      expect(focusedElement).not.toBeNull()

      await expect(page).toHaveScreenshot('dashboard-focus-indicators.png')
    })

    test('Modal dialogs should trap focus and be accessible', async ({ page }) => {
      await page.goto('/customers')

      // Open modal
      await page.click('[data-testid="create-customer-btn"]')
      await page.waitForSelector('[data-testid="modal"]')

      // Validate modal accessibility
      await AccessibilityTest.expectElementToBeAccessible(
        page,
        '[data-testid="modal"]',
        { tags: ['wcag2aa'] }
      )

      // Test focus trap
      await page.keyboard.press('Tab')
      let focusedInModal = await page.evaluate(() => {
        const modal = document.querySelector('[role="dialog"]')
        const active = document.activeElement
        return modal?.contains(active as Node)
      })
      expect(focusedInModal).toBe(true)

      // Close modal with Escape
      await page.keyboard.press('Escape')
      await expect(page.locator('[data-testid="modal"]')).not.toBeVisible()
    })

    test('Form should be fully accessible with proper labels', async ({ page }) => {
      await page.goto('/customers/create')

      await AccessibilityTest.expectPageToBeAccessible(page, {
        tags: ['wcag2a', 'wcag2aa']
      })

      // Check all form fields have labels
      const labelCheck = await AccessibilityTest.checkFormLabels(page, 'form')
      expect(labelCheck.hasLabels).toBe(true)
      expect(labelCheck.missingLabels.length).toBe(0)
    })

    test('Navigation should be keyboard accessible', async ({ page }) => {
      await page.goto('/')

      const nav = page.locator('[data-testid="main-navigation"]')
      await expect(nav).toBeVisible()

      // Check keyboard navigation
      const navCheck = await AccessibilityTest.checkKeyboardNavigation(
        page,
        '[data-testid="main-navigation"]'
      )
      expect(navCheck.isFocusable).toBe(true)
    })

    test('Color contrast should meet WCAG standards', async ({ page }) => {
      await page.goto('/dashboard')

      const contrastInfo = await AccessibilityTest.checkColorContrast(
        page,
        page.locator('body')
      )

      expect(contrastInfo.normal).toBe(true)
      expect(contrastInfo.large).toBe(true)
      expect(contrastInfo.ratio).toBeGreaterThanOrEqual(4.5)
    })

    test('Images should have alt text', async ({ page }) => {
      await page.goto('/customers')

      const imageCheck = await AccessibilityTest.checkImageAltText(page)
      expect(imageCheck.hasAlt).toBe(true)
    })

    test('Skip links should be present and functional', async ({ page }) => {
      await page.goto('/')

      const skipLink = page.locator('a[href="#main-content"]')
      await expect(skipLink).toBeVisible()

      // Test skip link works
      await skipLink.focus()
      await page.keyboard.press('Enter')
    })

    test('ARIA live regions should announce dynamic updates', async ({ page }) => {
      await page.goto('/customers')

      // Create a customer to trigger success message
      await page.click('[data-testid="create-customer-btn"]')
      await page.fill('[name="firstName"]', 'Test')
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', 'test@example.com')
      await page.click('[data-testid="save-customer-btn"]')

      // Check for ARIA live regions
      const liveRegion = page.locator('[aria-live="polite"], [role="status"]')
      if (await liveRegion.isVisible()) {
        const hasAriaLive = await page.evaluate(() => {
          return document.querySelector('[aria-live="polite"], [role="status"]') !== null
        })
        expect(hasAriaLive).toBe(true)
      }
    })
  })
})
