/**
 * Enterprise Admin Dashboard - E2E Tests
 *
 * Comprehensive tests for admin dashboard using Playwright 1.55+ features:
 * - test.serial() for multi-step workflows
 * - test.step() for step-by-step validation
 * - expect.soft() for non-blocking assertions
 *
 * Coverage: Admin dashboard, user management, system overview
 */

import { test, expect, devices } from '@playwright/test'

// Custom fixtures using test.extend()
test.describe('Admin Dashboard', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.describe.configure({
    mode: 'serial',
    retries: 2
  })

  test('Admin dashboard loads with all widgets', async ({ page }) => {
    await test.step('Navigate to admin dashboard', async () => {
      await page.goto('/admin/dashboard')
      await expect(page.locator('[data-admin="dashboard"]')).toBeVisible()
    })

    await test.step('Verify system statistics', async () => {
      await expect.soft(page.locator('[data-stat="total-users"]')).toBeVisible()
      await expect.soft(page.locator('[data-stat="active-sessions"]')).toBeVisible()
      await expect.soft(page.locator('[data-stat="system-health"]')).toBeVisible()
    })

    await test.step('Verify recent activity feed', async () => {
      await expect.soft(page.locator('[data-feed="recent-activity"]')).toBeVisible()
      await expect(page.locator('[data-activity]')).toHaveCount({ min: 1, max: 10 })
    })

    await test.step('Verify quick actions', async () => {
      await expect.soft(page.locator('[data-action="create-user"]')).toBeVisible()
      await expect.soft(page.locator('[data-action="view-logs"]')).toBeVisible()
      await expect.soft(page.locator('[data-action="system-config"]')).toBeVisible()
    })
  })

  test.serial('Navigate through admin sections', async ({ page }) => {
    await test.step('Go to Users section', async () => {
      await page.goto('/admin/dashboard')
      await page.click('[data-nav="users"]')
      await expect(page.locator('[data-section="users"]')).toBeVisible()
    })

    await test.step('Go to Roles section', async () => {
      await page.click('[data-nav="roles"]')
      await expect(page.locator('[data-section="roles"]')).toBeVisible()
    })

    await test.step('Go to Permissions section', async () => {
      await page.click('[data-nav="permissions"]')
      await expect(page.locator('[data-section="permissions"]')).toBeVisible()
    })

    await test.step('Go to System Logs section', async () => {
      await page.click('[data-nav="logs"]')
      await expect(page.locator('[data-section="logs"]')).toBeVisible()
    })
  })

  test('Admin dashboard on mobile', async ({ page }) => {
    // Using device emulation
    await test.step('Switch to mobile viewport', async () => {
      await page.setViewportSize({ width: 375, height: 667 })
    })

    await test.step('Navigate to admin dashboard', async () => {
      await page.goto('/admin/dashboard')
      await expect(page.locator('[data-admin="mobile-menu"]')).toBeVisible()
    })

    await test.step('Open mobile menu', async () => {
      await page.click('[data-admin="mobile-menu"]')
      await expect(page.locator('[data-mobile-nav="open"]')).toBeVisible()
    })

    await test.step('Navigate using mobile menu', async () => {
      await page.click('[data-mobile-nav="users"]')
      await expect(page.locator('[data-section="users"]')).toBeVisible()
    })
  })

  test('Admin dashboard performance', async ({ page }) => {
    const start = Date.now()

    await test.step('Load dashboard', async () => {
      await page.goto('/admin/dashboard')
      await expect(page.locator('[data-admin="dashboard"]')).toBeVisible()
    })

    await test.step('Verify load time', async () => {
      const loadTime = Date.now() - start
      expect(loadTime).toBeLessThan(3000) // 3 second SLA
    })

    await test.step('Verify all widgets load', async () => {
      const widgets = await page.locator('[data-widget]').count()
      expect(widgets).toBeGreaterThan(5)
    })
  })
})

test.describe('Admin Navigation', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Full admin navigation flow', async ({ page }) => {
    await test.step('Start at dashboard', async () => {
      await page.goto('/admin/dashboard')
    })

    await test.step('Navigate to user management', async () => {
      await page.click('[data-nav="users"]')
      await expect(page).toHaveURL(/.*\/admin\/users.*/)
    })

    await test.step('Navigate to role management', async () => {
      await page.click('[data-nav="roles"]')
      await expect(page).toHaveURL(/.*\/admin\/roles.*/)
    })

    await test.step('Navigate to permissions', async () => {
      await page.click('[data-nav="permissions"]')
      await expect(page).toHaveURL(/.*\/admin\/permissions.*/)
    })

    await test.step('Navigate back to dashboard', async () => {
      await page.click('[data-nav="dashboard"]')
      await expect(page).toHaveURL(/.*\/admin\/dashboard.*/)
      await expect(page.locator('[data-admin="dashboard"]')).toBeVisible()
    })
  })

  test('Admin breadcrumbs navigation', async ({ page }) => {
    await test.step('Navigate deep into users section', async () => {
      await page.goto('/admin/users')
      await page.click('[data-action="create-user"]')
      await expect(page.locator('[data-breadcrumb]')).toBeVisible()
    })

    await test.step('Verify breadcrumb trail', async () => {
      const breadcrumbs = page.locator('[data-breadcrumb] a')
      await expect(breadcrumbs).toHaveCount(3) // Home > Admin > Users
    })

    await test.step('Navigate using breadcrumbs', async () => {
      await page.click('[data-breadcrumb] a:nth-child(1)') // Home
      await expect(page).toHaveURL('/')
    })
  })
})

test.describe('Admin Dashboard Widgets', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('System health widget', async ({ page }) => {
    await page.goto('/admin/dashboard')

    await test.step('Check CPU usage', async () => {
      await expect.soft(page.locator('[data-widget="cpu"]')).toBeVisible()
      const cpuValue = await page.locator('[data-widget="cpu"] [data-value]').textContent()
      expect(cpuValue).toMatch(/\d+%/)
    })

    await test.step('Check memory usage', async () => {
      await expect.soft(page.locator('[data-widget="memory"]')).toBeVisible()
      const memValue = await page.locator('[data-widget="memory"] [data-value]').textContent()
      expect(memValue).toMatch(/\d+%/)
    })

    await test.step('Check disk usage', async () => {
      await expect.soft(page.locator('[data-widget="disk"]')).toBeVisible()
      const diskValue = await page.locator('[data-widget="disk"] [data-value]').textContent()
      expect(diskValue).toMatch(/\d+%/)
    })

    await test.step('Verify overall health status', async () => {
      await expect(page.locator('[data-status="system-health"]')).toBeVisible()
    })
  })

  test('User statistics widget', async ({ page }) => {
    await page.goto('/admin/dashboard')

    await test.step('Check total users count', async () => {
      await expect(page.locator('[data-widget="total-users"]')).toBeVisible()
      const count = await page.locator('[data-widget="total-users"] [data-count]').textContent()
      expect(parseInt(count || '0')).toBeGreaterThanOrEqual(0)
    })

    await test.step('Check active users', async () => {
      await expect(page.locator('[data-widget="active-users"]')).toBeVisible()
      const count = await page.locator('[data-widget="active-users"] [data-count]').textContent()
      expect(parseInt(count || '0')).toBeGreaterThanOrEqual(0)
    })

    await test.step('Check pending users', async () => {
      await expect(page.locator('[data-widget="pending-users"]')).toBeVisible()
      const count = await page.locator('[data-widget="pending-users"] [data-count]').textContent()
      expect(parseInt(count || '0')).toBeGreaterThanOrEqual(0)
    })
  })

  test('Recent activity widget', async ({ page }) => {
    await page.goto('/admin/dashboard')

    await test.step('Verify activity feed is visible', async () => {
      await expect(page.locator('[data-widget="activity"]')).toBeVisible()
    })

    await test.step('Check activity entries', async () => {
      const entries = page.locator('[data-widget="activity"] [data-activity]')
      await expect(entries).toHaveCount({ min: 1, max: 20 })
    })

    await test.step('Verify activity timestamps', async () => {
      const timestamps = page.locator('[data-widget="activity"] [data-timestamp]')
      const count = await timestamps.count()

      for (let i = 0; i < count; i++) {
        await expect.soft(timestamps.nth(i)).toHaveText(/\d+\s+(minutes?|hours?|days?)\s+ago/)
      }
    })
  })
})

test.describe('Admin Dashboard Actions', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Quick actions execution', async ({ page }) => {
    await page.goto('/admin/dashboard')

    await test.step('Execute create user action', async () => {
      await page.click('[data-action="quick-create-user"]')
      await expect(page).toHaveURL(/.*\/admin\/users\/create.*/)
    })

    await test.step('Go back to dashboard', async () => {
      await page.goto('/admin/dashboard')
    })

    await test.step('Execute view logs action', async () => {
      await page.click('[data-action="quick-view-logs"]')
      await expect(page).toHaveURL(/.*\/admin\/logs.*/)
    })
  })

  test('Dashboard refresh', async ({ page }) => {
    await page.goto('/admin/dashboard')

    await test.step('Store initial timestamp', async () => {
      await page.locator('[data-widget="activity"] [data-activity]:first-child').getAttribute('data-timestamp')
    })

    await test.step('Refresh dashboard', async () => {
      await page.click('[data-action="refresh-dashboard"]')
      await expect(page.locator('[data-loading="refreshing"]')).toBeVisible()
    })

    await test.step('Verify refresh completed', async () => {
      await expect(page.locator('[data-loading="refreshing"]')).toBeHidden({ timeout: 5000 })
    })
  })
})

// Additional test using test.repeat() for stress testing
test.describe('Admin Dashboard Stress Tests', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.repeat(10, 'Dashboard loads consistently', async ({ page }) => {
    await page.goto('/admin/dashboard')
    await expect(page.locator('[data-admin="dashboard"]')).toBeVisible()

    // Verify all critical widgets are present
    await expect(page.locator('[data-widget="system-health"]')).toBeVisible()
    await expect(page.locator('[data-widget="users"]')).toBeVisible()
    await expect(page.locator('[data-widget="activity"]')).toBeVisible()
  })

  test.repeat(5, 'Navigation remains responsive', async ({ page }) => {
    // Rapid navigation test
    await page.goto('/admin/users')
    await page.goto('/admin/roles')
    await page.goto('/admin/permissions')
    await page.goto('/admin/dashboard')

    // Should complete all navigations without errors
    await expect(page.locator('[data-admin="dashboard"]')).toBeVisible()
  })
})
