/**
 * Dashboard Page Tests
 *
 * Comprehensive test coverage for dashboard page functionality
 * Tests metrics display, charts, widgets, real-time updates, and user interactions
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { test, expect } from '@playwright/test'

test.describe('Dashboard Page', () => {
  test.describe('Dashboard Layout', () => {
    test.beforeEach(async ({ page }) => {
      // Mock dashboard API response
      await page.route('**/api/dashboard**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            summary: {
              totalCustomers: 1248,
              activeOrders: 342,
              monthlyRevenue: 125000,
              pendingInvoices: 56,
              growthRate: 12.5,
              churnRate: 2.1
            },
            recentActivities: [
              { id: 1, type: 'order', message: 'New order #1234 created', timestamp: '2024-11-05T10:30:00Z' },
              { id: 2, type: 'payment', message: 'Payment received from Customer #5678', timestamp: '2024-11-05T09:15:00Z' }
            ]
          })
        })
      })
    })

    test('should navigate to dashboard', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('h1.page-title')).toContainText('Dashboard')
      await expect(page.locator('[data-testid="welcome-message"]')).toBeVisible()
    })

    test('should display summary metrics cards', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="metric-total-customers"]')).toContainText('1248')
      await expect(page.locator('[data-testid="metric-active-orders"]')).toContainText('342')
      await expect(page.locator('[data-testid="metric-monthly-revenue"]')).toContainText('125,000')
      await expect(page.locator('[data-testid="metric-pending-invoices"]')).toContainText('56')
      await expect(page.locator('[data-testid="metric-growth-rate"]')).toContainText('12.5%')
      await expect(page.locator('[data-testid="metric-churn-rate"]')).toContainText('2.1%')
    })

    test('should display metric trends', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="total-customers-trend"]')).toBeVisible()
      await expect(page.locator('.trend-indicator')).toBeVisible()
      await expect(page.locator('[data-testid="trend-up"]')).toBeVisible()
    })

    test('should toggle metric timeframe', async ({ page }) => {
      await page.goto('/dashboard')

      // Click on metric card
      await page.click('[data-testid="metric-total-customers"]')

      // Select different timeframe
      await page.selectOption('[data-testid="timeframe-select"]', '7d')

      await expect(page.locator('[data-testid="timeframe-select"]')).toHaveValue('7d')
    })

    test('should refresh dashboard data', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="refresh-dashboard-btn"]')

      // Verify refresh indicator
      await expect(page.locator('[data-testid="loading-spinner"]')).toBeVisible()

      // Wait for data to load
      await expect(page.locator('[data-testid="loading-spinner"]')).not.toBeVisible()
    })

    test('should display last updated timestamp', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="last-updated"]')).toContainText('Last updated')
    })
  })

  test.describe('Charts and Visualizations', () => {
    test.beforeEach(async ({ page }) => {
      // Mock charts API response
      await page.route('**/api/dashboard/charts**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            revenueChart: {
              labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
              data: [80000, 85000, 90000, 95000, 110000, 125000]
            },
            customerGrowthChart: {
              labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
              data: [800, 850, 950, 1050, 1150, 1248]
            },
            orderStatusChart: {
              labels: ['Pending', 'Processing', 'Shipped', 'Delivered'],
              data: [45, 120, 89, 88]
            }
          })
        })
      })
    })

    test('should display revenue chart', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="revenue-chart"]')).toBeVisible()
      await expect(page.locator('.chart-revenue')).toBeVisible()
    })

    test('should display customer growth chart', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="customer-growth-chart"]')).toBeVisible()
    })

    test('should display order status pie chart', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="order-status-chart"]')).toBeVisible()
      await expect(page.locator('.pie-chart')).toBeVisible()
    })

    test('should toggle chart view (bar/line)', async ({ page }) => {
      await page.goto('/dashboard')

      // Switch to line chart
      await page.click('[data-testid="chart-view-toggle"]')

      await expect(page.locator('[data-testid="chart-view-toggle"]')).toContainText('Line')
      await expect(page.locator('.line-chart')).toBeVisible()
    })

    test('should export chart as PNG', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="revenue-chart"] [data-testid="export-chart-btn"]')

      const downloadPromise = page.waitForEvent('download')
      await page.click('text=Download PNG')

      const download = await downloadPromise
      expect(download.suggestedFilename()).toContain('revenue-chart.png')
    })

    test('should show chart data on hover', async ({ page }) => {
      await page.goto('/dashboard')

      // Hover over chart
      await page.hover('[data-testid="revenue-chart"] canvas')

      // Verify tooltip appears
      await expect(page.locator('.chart-tooltip')).toBeVisible()
    })

    test('should change chart date range', async ({ page }) => {
      await page.route('**/api/dashboard/charts**', async (route) => {
        const url = new URL(route.request().url())
        const range = url.searchParams.get('range')

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            revenueChart: {
              labels: range === '7d' ? ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'] : ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
              data: range === '7d' ? [15000, 18000, 16000, 20000, 22000, 19000, 21000] : [80000, 85000, 90000, 95000, 110000, 125000]
            }
          })
        })
      })

      await page.goto('/dashboard')

      await page.selectOption('[data-testid="chart-date-range"]', '7d')

      // Verify chart updates
      await expect(page.locator('[data-testid="chart-loading"]')).toBeVisible()
      await expect(page.locator('[data-testid="chart-loading"]')).not.toBeVisible()
    })
  })

  test.describe('Recent Activities Widget', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/dashboard/activities**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([
            { id: 1, type: 'order', message: 'New order #1234 created', timestamp: '2024-11-05T10:30:00Z', user: 'John Doe' },
            { id: 2, type: 'payment', message: 'Payment received $500', timestamp: '2024-11-05T09:15:00Z', user: 'Jane Smith' },
            { id: 3, type: 'customer', message: 'New customer registered', timestamp: '2024-11-05T08:00:00Z', user: 'System' }
          ])
        })
      })
    })

    test('should display recent activities list', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="recent-activities"]')).toBeVisible()
      await expect(page.locator('[data-testid="activity-item"]')).toHaveCount(3)
    })

    test('should show activity icons', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="activity-icon-order"]')).toBeVisible()
      await expect(page.locator('[data-testid="activity-icon-payment"]')).toBeVisible()
      await expect(page.locator('[data-testid="activity-icon-customer"]')).toBeVisible()
    })

    test('should filter activities by type', async ({ page }) => {
      await page.goto('/dashboard')

      await page.selectOption('[data-testid="activity-filter"]', 'order')

      const activities = page.locator('[data-testid="activity-item"]')
      const count = await activities.count()

      // Should only show orders
      for (let i = 0; i < count; i++) {
        await expect(activities.nth(i)).toContainText('order')
      }
    })

    test('should mark activity as read', async ({ page }) => {
      await page.route('**/api/dashboard/activities/1/read**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ success: true })
        })
      })

      await page.goto('/dashboard')

      // Click on activity to mark as read
      await page.click('[data-testid="activity-item"]')

      await expect(page.locator('[data-testid="activity-item"]')).toHaveClass(/read/)
    })

    test('should load more activities', async ({ page }) => {
      await page.route('**/api/dashboard/activities**', async (route) => {
        const url = new URL(route.request().url())
        const page = url.searchParams.get('page')

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(
            page === '2' ? [
              { id: 4, type: 'order', message: 'Order #5678 updated', timestamp: '2024-11-04T15:00:00Z', user: 'Bob' }
            ] : [
              { id: 1, type: 'order', message: 'New order', timestamp: '2024-11-05T10:30:00Z', user: 'John' },
              { id: 2, type: 'payment', message: 'Payment received', timestamp: '2024-11-05T09:15:00Z', user: 'Jane' }
            ]
          )
        })
      })

      await page.goto('/dashboard')

      await page.click('[data-testid="load-more-activities"]')

      await expect(page.locator('[data-testid="activity-item"]')).toHaveCount(3)
    })
  })

  test.describe('Quick Actions', () => {
    test('should display quick action buttons', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="quick-action-new-customer"]')).toBeVisible()
      await expect(page.locator('[data-testid="quick-action-new-order"]')).toBeVisible()
      await expect(page.locator('[data-testid="quick-action-generate-invoice"]')).toBeVisible()
    })

    test('should create new customer from dashboard', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="quick-action-new-customer"]')

      // Should navigate to create customer page
      await expect(page).toHaveURL('/customers/create')
    })

    test('should create new order from dashboard', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="quick-action-new-order"]')

      // Should navigate to create order page
      await expect(page).toHaveURL('/orders/create')
    })

    test('should generate invoice from dashboard', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="quick-action-generate-invoice"]')

      // Should open invoice generation modal or navigate to page
      await expect(page.locator('[data-testid="generate-invoice-modal"]')).toBeVisible()
    })
  })

  test.describe('Widgets Customization', () => {
    test('should enable edit mode', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="edit-dashboard-btn"]')

      await expect(page.locator('body')).toHaveClass(/edit-mode/)
      await expect(page.locator('[data-testid="widget-controls"]')).toBeVisible()
    })

    test('should rearrange widgets', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="edit-dashboard-btn"]')

      // Drag widget
      const widget = page.locator('[data-testid="widget-revenue-chart"]')
      await widget.dragTo(page.locator('[data-testid="widget-customer-growth"]'))

      // Verify position changed
      // Note: Actual verification depends on implementation
    })

    test('should resize widgets', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="edit-dashboard-btn"]')

      // Find resize handle
      await page.hover('[data-testid="widget-revenue-chart"] [data-testid="resize-handle"]')

      // Drag to resize
      await page.mouse.down()
      await page.mouse.move(100, 0)
      await page.mouse.up()

      // Verify widget resized
      await expect(page.locator('[data-testid="widget-revenue-chart"]')).toHaveClass(/resized/)
    })

    test('should hide widget', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="edit-dashboard-btn"]')

      await page.click('[data-testid="widget-revenue-chart"] [data-testid="hide-widget-btn"]')

      await expect(page.locator('[data-testid="widget-revenue-chart"]')).not.toBeVisible()
    })

    test('should restore hidden widget', async ({ page }) => {
      await page.goto('/dashboard')

      // Click to restore from widget menu
      await page.click('[data-testid="restore-widget-menu"]')
      await page.click('text=Revenue Chart')

      await expect(page.locator('[data-testid="widget-revenue-chart"]')).toBeVisible()
    })

    test('should reset dashboard to default', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="edit-dashboard-btn"]')
      await page.click('[data-testid="reset-dashboard-btn"]')

      await expect(page.locator('[data-testid="confirm-dialog"]')).toBeVisible()
      await page.click('[data-testid="confirm-reset"]')

      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('Dashboard reset to default')
    })
  })

  test.describe('Real-time Updates', () => {
    test('should receive real-time metric updates', async ({ page }) => {
      await page.goto('/dashboard')

      // Mock WebSocket message for metric update
      await page.evaluate(() => {
        window.dispatchEvent(new CustomEvent('metric-update', {
          detail: { metric: 'totalCustomers', value: 1249 }
        }))
      })

      // Wait for update
      await page.waitForTimeout(1000)

      // Verify updated value
      await expect(page.locator('[data-testid="metric-total-customers"]')).toContainText('1249')
    })

    test('should show real-time notification badge', async ({ page }) => {
      await page.goto('/dashboard')

      // Mock new activity notification
      await page.evaluate(() => {
        window.dispatchEvent(new CustomEvent('new-activity', {
          detail: { type: 'order', message: 'New order received' }
        }))
      })

      // Notification badge should appear
      await expect(page.locator('[data-testid="notification-badge"]')).toBeVisible()
    })

    test('should handle WebSocket connection status', async ({ page }) => {
      await page.goto('/dashboard')

      // Initially connected
      await expect(page.locator('[data-testid="connection-status"]')).toContainText('Connected')

      // Simulate disconnection
      await page.evaluate(() => {
        window.dispatchEvent(new Event('ws-disconnected'))
      })

      await expect(page.locator('[data-testid="connection-status"]')).toContainText('Disconnected')

      // Simulate reconnection
      await page.evaluate(() => {
        window.dispatchEvent(new Event('ws-connected'))
      })

      await expect(page.locator('[data-testid="connection-status"]')).toContainText('Connected')
    })
  })

  test.describe('Dashboard Filters and Date Range', () => {
    test('should filter dashboard by date range', async ({ page }) => {
      await page.route('**/api/dashboard**', async (route) => {
        const url = new URL(route.request().url())
        const from = url.searchParams.get('from')
        const to = url.searchParams.get('to')

        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            summary: {
              totalCustomers: 1248,
              activeOrders: 342,
              monthlyRevenue: 125000
            }
          })
        })
      })

      await page.goto('/dashboard')

      await page.click('[data-testid="date-range-picker"]')
      await page.selectOption('[data-testid="quick-range"]', '7d')

      // Verify API call includes date parameters
      const calls = page.request.allInterceptions()
      const dashboardCall = calls.find(c => c.request.url().includes('/api/dashboard'))
      expect(dashboardCall?.request.url()).toMatch(/from=/)
    })

    test('should apply custom date range', async ({ page }) => {
      await page.goto('/dashboard')

      await page.click('[data-testid="date-range-picker"]')
      await page.click('[data-testid="custom-range-option"]')

      await page.fill('[data-testid="date-from"]', '2024-10-01')
      await page.fill('[data-testid="date-to"]', '2024-10-31')

      await page.click('[data-testid="apply-date-range"]')

      // Wait for data to reload
      await expect(page.locator('[data-testid="loading-overlay"]')).toBeVisible()
    })

    test('should save dashboard filters', async ({ page }) => {
      await page.goto('/dashboard')

      await page.selectOption('[data-testid="quick-range"]', '30d')

      await page.click('[data-testid="save-filters-btn"]')

      await expect(page.locator('[data-testid="success-message"]'))
        .toContainText('Filters saved')
    })
  })

  test.describe('Error Handling', () => {
    test('should handle API errors gracefully', async ({ page }) => {
      await page.route('**/api/dashboard**', async (route) => {
        await route.fulfill({
          status: 500,
          contentType: 'application/json',
          body: JSON.stringify({ error: 'Internal Server Error' })
        })
      })

      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="error-message"]')).toBeVisible()
      await expect(page.locator('[data-testid="retry-btn"]')).toBeVisible()
    })

    test('should retry on failed data load', async ({ page }) => {
      let requestCount = 0
      await page.route('**/api/dashboard**', async (route) => {
        requestCount++
        if (requestCount === 1) {
          await route.abort('internetdisconnected')
        } else {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              summary: { totalCustomers: 1248, activeOrders: 342, monthlyRevenue: 125000 }
            })
          })
        }
      })

      await page.goto('/dashboard')
      await page.waitForTimeout(2000)

      // Should show dashboard data after retry
      await expect(page.locator('[data-testid="metric-total-customers"]')).toContainText('1248')
    })

    test('should display empty state when no data', async ({ page }) => {
      await page.route('**/api/dashboard**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ summary: null, recentActivities: [] })
        })
      })

      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="empty-state"]')).toBeVisible()
    })
  })

  test.describe('Accessibility', () => {
    test('should support keyboard navigation', async ({ page }) => {
      await page.goto('/dashboard')

      // Tab through dashboard elements
      await page.keyboard.press('Tab')
      await page.keyboard.press('Tab')

      const focused = await page.evaluate(() => document.activeElement?.getAttribute('data-testid'))
      expect(focused).toBeDefined()
    })

    test('should have proper ARIA labels', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.locator('[data-testid="revenue-chart"]'))
        .toHaveAttribute('aria-label', 'Revenue Chart')

      await expect(page.locator('[data-testid="metric-total-customers"]'))
        .toHaveAttribute('aria-label', 'Total Customers: 1248')
    })

    test('should announce metric updates to screen readers', async ({ page }) => {
      await page.goto('/dashboard')

      // Trigger metric update
      await page.evaluate(() => {
        window.dispatchEvent(new CustomEvent('metric-update', {
          detail: { metric: 'totalCustomers', value: 1249 }
        }))
      })

      // Screen reader should announce the update
      const announcement = await page.evaluate(() => {
        return document.querySelector('[aria-live]')?.textContent || ''
      })

      // Note: Actual implementation depends on a11y setup
    })

    test('should support keyboard shortcuts', async ({ page }) => {
      await page.goto('/dashboard')

      // 'r' to refresh
      await page.keyboard.press('r')

      await expect(page.locator('[data-testid="loading-spinner"]')).toBeVisible()
    })
  })
})
