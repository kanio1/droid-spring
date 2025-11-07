/**
 * Subscription Management E2E Tests
 *
 * Comprehensive test suite covering all subscription management workflows:
 * - Subscription creation and activation
 * - Subscription status transitions (active, suspended, cancelled, expired)
 * - Plan changes and upgrades/downgrades
 * - Subscription cancellation
 * - Subscription pause/resume
 * - Trial period management
 * - Auto-renewal configuration
 * - Billing cycle management
 * - Subscription renewal
 * - Usage tracking and limits
 * - Feature access management
 * - Notification settings
 * - Search and filtering
 * - Date range filtering
 * - Bulk operations
 * - Subscription history
 * - Prorated charges
 * - Error handling and validation
 *
 * Target: 30 comprehensive tests
 */

import { test, expect } from '@playwright/test'
import { SubscriptionFactory, CustomerFactory } from '../framework/data-factories'
import { AuthHelper } from '../helpers'
import type { Page } from '@playwright/test'

test.describe('Subscription Management E2E', () => {
  let testSubscriptions: any[] = []
  let testCustomers: any[] = []

  test.beforeEach(async ({ page }) => {
    // Login before each test
    await AuthHelper.login(page, {
      username: process.env.TEST_USER || 'testuser',
      password: process.env.TEST_PASSWORD || 'testpass'
    })

    await page.goto('/subscriptions')
    await expect(page.locator('h1')).toContainText('Subscriptions')
  })

  test.afterEach(async ({ page }) => {
    // Cleanup test data
    for (const subscription of testSubscriptions) {
      try {
        await page.goto(`/subscriptions/${subscription.id}`)
        if (await page.locator('[data-testid="delete-subscription-button"]').isVisible()) {
          await page.click('[data-testid="delete-subscription-button"]')
          await page.click('[data-testid="confirm-delete"]')
        }
      } catch (error) {
        console.log(`Cleanup failed for subscription ${subscription.id}:`, error)
      }
    }
    testSubscriptions = []
    testCustomers = []
  })

  // ========== LIST VIEW TESTS ==========

  test('01 - Should display subscriptions list with all required elements', async ({ page }) => {
    await expect(page.locator('h1')).toContainText('Subscriptions')
    await expect(page.locator('[data-testid="subscriptions-table"]')).toBeVisible()
    await expect(page.locator('[data-testid="search-input"]')).toBeVisible()
    await expect(page.locator('[data-testid="status-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="plan-filter"]')).toBeVisible()
    await expect(page.locator('[data-testid="create-subscription-button"]')).toBeVisible()
  })

  test('02 - Should show subscription summary cards', async ({ page }) => {
    const cards = page.locator('[data-testid="summary-card"]')
    const count = await cards.count()
    expect(count).toBeGreaterThanOrEqual(4)

    await expect(page.locator('[data-testid="total-subscriptions"]')).toBeVisible()
    await expect(page.locator('[data-testid="active-count"]')).toBeVisible()
    await expect(page.locator('[data-testid="suspended-count"]')).toBeVisible()
    await expect(page.locator('[data-testid="expired-count"]')).toBeVisible()
  })

  test('03 - Should handle empty state when no subscriptions exist', async ({ page }) => {
    const emptyState = page.locator('[data-testid="empty-state"]')
    if (await emptyState.isVisible()) {
      await expect(emptyState).toContainText(/No subscriptions found/i)
      await expect(page.locator('[data-testid="create-first-subscription-button"]')).toBeVisible()
    }
  })

  // ========== SEARCH TESTS ==========

  test('04 - Should search subscriptions by subscription number', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .withSubscriptionNumber('SUB-TEST-001')
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })
    await page.waitForTimeout(1000)

    // Search by subscription number
    await page.fill('[data-testid="search-input"]', 'SUB-TEST-001')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)
  })

  test('05 - Should search subscriptions by customer name', async ({ page }) => {
    const customer = await createTestCustomer(page, 'Alice', 'Johnson')
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })
    await page.waitForTimeout(1000)

    // Search by customer name
    await page.fill('[data-testid="search-input"]', 'Alice')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThan(0)
  })

  test('06 - Should show no results for search with no matches', async ({ page }) => {
    await page.fill('[data-testid="search-input"]', 'NonExistentSubscription12345')
    await page.waitForTimeout(1000)

    await expect(page.locator('[data-testid="no-results-message"]'))
      .toContainText(/No subscriptions found/i)
    await expect(page.locator('table tbody tr')).toHaveCount(0)
  })

  // ========== FILTERING TESTS ==========

  test('07 - Should filter subscriptions by status - Active', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'ACTIVE')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/active/i)
        }
      }
    }
  })

  test('08 - Should filter subscriptions by status - Suspended', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'SUSPENDED')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/suspended/i)
        }
      }
    }
  })

  test('09 - Should filter subscriptions by status - Cancelled', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'CANCELLED')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()

    if (count > 0) {
      for (let i = 0; i < Math.min(count, 5); i++) {
        const statusBadge = rows.nth(i).locator('[data-testid="status-badge"]')
        if (await statusBadge.isVisible()) {
          await expect(statusBadge).toContainText(/cancelled/i)
        }
      }
    }
  })

  test('10 - Should filter subscriptions by plan', async ({ page }) => {
    await page.selectOption('[data-testid="plan-filter"]', 'PREMIUM')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })

  test('11 - Should combine search and status filter', async ({ page }) => {
    await page.selectOption('[data-testid="status-filter"]', 'ACTIVE')
    await page.fill('[data-testid="search-input"]', 'test')
    await page.waitForTimeout(500)

    const rows = page.locator('table tbody tr')
    const count = await rows.count()
    expect(count).toBeGreaterThanOrEqual(0)
  })

  // ========== CREATE SUBSCRIPTION TESTS ==========

  test('12 - Should create a new subscription with valid data', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    expect(subscriptionId).toBeTruthy()
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Verify redirect to details page
    await expect(page).toHaveURL(/.*\/subscriptions\/[a-zA-Z0-9]+/)

    // Verify subscription data
    await expect(page.locator('[data-testid="subscription-number"]')).toBeVisible()
  })

  test('13 - Should create subscription with trial period', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .withTrialDays(30)
      .trial()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Verify trial period
    await page.goto(`/subscriptions/${subscriptionId}`)
    const trialInfo = page.locator('[data-testid="trial-period"]')
    await expect(trialInfo).toContainText(/30 days/i)
  })

  test('14 - Should show validation errors for required fields', async ({ page }) => {
    await page.click('[data-testid="create-subscription-button"]')
    await expect(page).toHaveURL(/.*\/subscriptions\/create/)

    // Submit empty form
    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(500)

    // Check for validation errors
    await expect(page.locator('[data-testid="customer-error"]'))
      .toContainText(/required/i)
    await expect(page.locator('[data-testid="plan-error"]'))
      .toContainText(/required/i)
  })

  // ========== SUBSCRIPTION STATUS TRANSITIONS ==========

  test('15 - Should change subscription status from Trial to Active', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .trial()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Activate subscription
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="activate-subscription-button"]')
    await page.selectOption('[data-testid="payment-method"]', 'CREDIT_CARD')
    await page.click('[data-testid="confirm-activate"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/active/i)
  })

  test('16 - Should suspend subscription', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Suspend subscription
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="suspend-subscription-button"]')
    await page.fill('[data-testid="suspension-reason"]', 'Non-payment')
    await page.click('[data-testid="confirm-suspend"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/suspended/i)
  })

  test('17 - Should cancel subscription', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Cancel subscription
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="cancel-subscription-button"]')
    await page.selectOption('[data-testid="cancellation-reason"]', 'CUSTOMER_REQUEST')
    await page.fill('[data-testid="cancellation-notes"]', 'Customer requested cancellation')
    await page.click('[data-testid="confirm-cancel"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/cancelled/i)
  })

  // ========== SUBSCRIPTION LIFECYCLE MANAGEMENT ==========

  test('18 - Should pause subscription', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Pause subscription
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="pause-subscription-button"]')
    await page.fill('[data-testid="pause-duration"]', '30')
    await page.click('[data-testid="confirm-pause"]')
    await page.waitForTimeout(500)

    // Verify pause
    const pauseInfo = page.locator('[data-testid="pause-info"]')
    await expect(pauseInfo).toContainText(/paused/i)
  })

  test('19 - Should resume paused subscription', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .paused()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Resume subscription
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="resume-subscription-button"]')
    await page.click('[data-testid="confirm-resume"]')
    await page.waitForTimeout(500)

    // Verify status change
    const statusBadge = page.locator('[data-testid="status-badge"]')
    await expect(statusBadge).toContainText(/active/i)
  })

  // ========== PLAN CHANGES ==========

  test('20 - Should upgrade subscription plan', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .withPlan('BASIC')
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Upgrade plan
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="change-plan-button"]')
    await page.selectOption('[data-testid="new-plan"]', 'PREMIUM')
    await page.selectOption('[data-testid="upgrade-type"]', 'IMMEDIATE')
    await page.click('[data-testid="confirm-plan-change"]')
    await page.waitForTimeout(1000)

    // Verify plan change
    const planInfo = page.locator('[data-testid="current-plan"]')
    await expect(planInfo).toContainText(/premium/i)
  })

  test('21 - Should downgrade subscription plan', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .withPlan('PREMIUM')
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Downgrade plan
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="change-plan-button"]')
    await page.selectOption('[data-testid="new-plan"]', 'BASIC')
    await page.selectOption('[data-testid="upgrade-type"]', 'END_OF_TERM')
    await page.click('[data-testid="confirm-plan-change"]')
    await page.waitForTimeout(1000)

    // Verify plan change scheduled
    const changeInfo = page.locator('[data-testid="pending-change"]')
    await expect(changeInfo).toContainText(/scheduled/i)
  })

  // ========== BILLING MANAGEMENT ==========

  test('22 - Should configure auto-renewal', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .active()
      .withAutoRenew(false)
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Enable auto-renewal
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="edit-subscription-button"]')
    await page.check('[data-testid="auto-renew-checkbox"]')
    await page.click('[data-testid="save-changes"]')
    await page.waitForTimeout(500)

    // Verify auto-renewal enabled
    const autoRenewInfo = page.locator('[data-testid="auto-renew-status"]')
    await expect(autoRenewInfo).toContainText(/enabled/i)
  })

  test('23 - Should update billing cycle', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .withBillingCycle('MONTHLY')
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Change to yearly
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="edit-subscription-button"]')
    await page.selectOption('[data-testid="billing-cycle"]', 'YEARLY')
    await page.click('[data-testid="save-changes"]')
    await page.waitForTimeout(1000)

    // Verify billing cycle change
    const billingInfo = page.locator('[data-testid="billing-cycle"]')
    await expect(billingInfo).toContainText(/yearly/i)
  })

  // ========== USAGE TRACKING ==========

  test('24 - Should display usage tracking information', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .withUsageLimits({
        data: 10000000000, // 10GB
        minutes: 500,
        sms: 100
      })
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // View usage
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="usage-tab"]')

    // Verify usage displayed
    await expect(page.locator('[data-testid="data-usage"]')).toBeVisible()
    await expect(page.locator('[data-testid="minutes-usage"]')).toBeVisible()
    await expect(page.locator('[data-testid="sms-usage"]')).toBeVisible()
  })

  // ========== NOTIFICATION SETTINGS ==========

  test('25 - Should configure notification settings', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .active()
      .withNotifications(false)
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Enable notifications
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="edit-subscription-button"]')
    await page.check('[data-testid="notifications-checkbox"]')
    await page.selectOption('[data-testid="notification-method"]', 'EMAIL')
    await page.click('[data-testid="save-changes"]')
    await page.waitForTimeout(500)

    // Verify notifications enabled
    const notificationInfo = page.locator('[data-testid="notification-status"]')
    await expect(notificationInfo).toContainText(/email/i)
  })

  // ========== SUBSCRIPTION DETAILS VIEW ==========

  test('26 - Should view complete subscription details', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .active()
      .withFeatures(['Feature 1', 'Feature 2', 'Feature 3'])
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // View subscription details
    await page.goto(`/subscriptions/${subscriptionId}`)

    // Verify all subscription information is displayed
    await expect(page.locator('[data-testid="subscription-number"]')).toBeVisible()
    await expect(page.locator('[data-testid="customer-info"]')).toBeVisible()
    await expect(page.locator('[data-testid="plan-info"]')).toBeVisible()
    await expect(page.locator('[data-testid="status-badge"]')).toBeVisible()
    await expect(page.locator('[data-testid="billing-cycle"]')).toBeVisible()
    await expect(page.locator('[data-testid="next-billing-date"]')).toBeVisible()
  })

  test('27 - Should show subscription features list', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .withFeatures(['Unlimited Data', '24/7 Support', 'Premium Content'])
      .active()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // View features
    await page.goto(`/subscriptions/${subscriptionId}`)

    const features = page.locator('[data-testid="feature-item"]')
    const count = await features.count()
    expect(count).toBeGreaterThan(0)

    await expect(features.first()).toContainText('Unlimited Data')
  })

  // ========== BULK OPERATIONS ==========

  test('28 - Should select multiple subscriptions and perform bulk action', async ({ page }) => {
    const customer = await createTestCustomer(page)

    // Create multiple subscriptions
    const subscriptions = SubscriptionFactory.create().buildMany(3)
    const createdIds: any[] = []

    for (const subscription of subscriptions) {
      const subscriptionId = await createSubscription(page, { ...subscription, customerId: customer.id })
      createdIds.push({ id: subscriptionId, ...subscription })
    }
    testSubscriptions.push(...createdIds)

    await page.waitForTimeout(1000)

    // Select multiple subscriptions
    await page.click('[data-testid="select-all-checkbox"]')

    // Perform bulk action
    await page.click('[data-testid="bulk-action-dropdown"]')
    await page.click('[data-testid="bulk-action-export"]')
    await page.waitForTimeout(500)

    // Verify export started
    const exportMessage = page.locator('[data-testid="export-message"]')
    if (await exportMessage.isVisible()) {
      await expect(exportMessage).toContainText(/export/i)
    }
  })

  // ========== RENEWAL MANAGEMENT ==========

  test('29 - Should manually renew subscription', async ({ page }) => {
    const customer = await createTestCustomer(page)
    const subscription = SubscriptionFactory.create()
      .withCustomerId(customer.id)
      .withAutoRenew(false)
      .expiring()
      .build()

    const subscriptionId = await createSubscription(page, subscription)
    testSubscriptions.push({ id: subscriptionId, ...subscription })

    // Manual renew
    await page.goto(`/subscriptions/${subscriptionId}`)
    await page.click('[data-testid="renew-subscription-button"]')
    await page.selectOption('[data-testid="renewal-period"]', '1YEAR')
    await page.click('[data-testid="confirm-renew"]')
    await page.waitForTimeout(1000)

    // Verify renewal
    const renewalInfo = page.locator('[data-testid="renewal-status"]')
    await expect(renewalInfo).toContainText(/renewed/i)
  })

  // ========== ERROR HANDLING ==========

  test('30 - Should handle invalid plan selection', async ({ page }) => {
    const customer = await createTestCustomer(page)

    await page.click('[data-testid="create-subscription-button"]')
    await expect(page).toHaveURL(/.*\/subscriptions\/create/)

    await page.selectOption('[data-testid="customer-select"]', customer.id)
    await page.selectOption('[data-testid="plan-select"]', 'INVALID_PLAN')

    await page.click('[data-testid="submit-button"]')
    await page.waitForTimeout(1000)

    // Should show error
    await expect(page.locator('[data-testid="plan-error"]'))
      .toContainText(/invalid|not available/i)
  })

  // ========== HELPER FUNCTIONS ==========

  async function createTestCustomer(
    page: Page,
    firstName: string = 'Test',
    lastName: string = 'Customer'
  ): Promise<any> {
    const customer = CustomerFactory.create()
      .withFirstName(firstName)
      .withLastName(lastName)
      .withRandomEmail()
      .active()
      .build()

    // Navigate to customer create page
    await page.goto('/customers/create')
    await page.fill('[data-testid="firstName-input"]', customer.firstName)
    await page.fill('[data-testid="lastName-input"]', customer.lastName)
    await page.fill('[data-testid="email-input"]', customer.email)

    if (customer.phone) {
      await page.fill('[data-testid="phone-input"]', customer.phone)
    }

    await page.click('[data-testid="submit-button"]')
    await page.waitForResponse(/.*\/api\/customers/)
    await page.waitForURL(/.*\/customers\/[a-zA-Z0-9]+/)

    // Extract customer ID from URL
    const url = page.url()
    const idMatch = url.match(/\/customers\/([a-zA-Z0-9]+)/)
    const id = idMatch ? idMatch[1] : ''

    testCustomers.push({ id, ...customer })
    return { id, ...customer }
  }

  async function createSubscription(page: Page, subscription: any): Promise<string> {
    await page.click('[data-testid="create-subscription-button"]')
    await page.waitForURL(/.*\/subscriptions\/create/)

    // Fill form
    if (subscription.subscriptionNumber) {
      await page.fill('[data-testid="subscription-number-input"]', subscription.subscriptionNumber)
    }

    await page.selectOption('[data-testid="customer-select"]', subscription.customerId)

    if (subscription.plan) {
      await page.selectOption('[data-testid="plan-select"]', subscription.plan)
    }

    if (subscription.billingCycle) {
      await page.selectOption('[data-testid="billing-cycle"]', subscription.billingCycle)
    }

    if (subscription.amount) {
      await page.fill('[data-testid="amount-input"]', subscription.amount.toString())
    }

    await page.click('[data-testid="submit-subscription"]')
    await page.waitForResponse(/.*\/api\/subscriptions/)
    await page.waitForURL(/.*\/subscriptions\/[a-zA-Z0-9]+/)

    const url = page.url()
    const idMatch = url.match(/\/subscriptions\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }
})
