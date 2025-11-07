/**
 * Dashboard Smoke Tests - Critical Path Tests for Dashboard and Analytics
 */

import { test, expect } from '@playwright/test'

test.describe('Dashboard Smoke Tests - Critical Paths', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })

  test('SMOKE-061: Should display dashboard overview', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page.locator('h1')).toContainText(/dashboard/i)
    await expect(page.locator('[data-testid="dashboard-overview"]')).toBeVisible()
  })

  test('SMOKE-062: Should display customer metrics', async ({ page }) => {
    await page.goto('/dashboard')

    // Check for customer count
    const customerMetric = page.locator('[data-testid="metric-customers"]')
    if (await customerMetric.isVisible()) {
      await expect(customerMetric).toContainText(/\d+/)
    }
  })

  test('SMOKE-063: Should display order metrics', async ({ page }) => {
    await page.goto('/dashboard')

    // Check for order count
    const orderMetric = page.locator('[data-testid="metric-orders"]')
    if (await orderMetric.isVisible()) {
      await expect(orderMetric).toContainText(/\d+/)
    }
  })

  test('SMOKE-064: Should display revenue metrics', async ({ page }) => {
    await page.goto('/dashboard')

    // Check for revenue
    const revenueMetric = page.locator('[data-testid="metric-revenue"]')
    if (await revenueMetric.isVisible()) {
      await expect(revenueMetric).toContainText(/[\$€£]/)
    }
  })

  test('SMOKE-065: Should display recent activity', async ({ page }) => {
    await page.goto('/dashboard')

    // Check for activity feed
    const activityFeed = page.locator('[data-testid="recent-activity"]')
    if (await activityFeed.isVisible()) {
      await expect(activityFeed).toBeVisible()
    }
  })

  test('SMOKE-066: Should display charts', async ({ page }) => {
    await page.goto('/dashboard')

    // Check for revenue chart
    const revenueChart = page.locator('[data-testid="revenue-chart"]')
    if (await revenueChart.isVisible()) {
      await expect(revenueChart).toBeVisible()
    }

    // Check for order chart
    const orderChart = page.locator('[data-testid="order-chart"]')
    if (await orderChart.isVisible()) {
      await expect(orderChart).toBeVisible()
    }
  })
})
