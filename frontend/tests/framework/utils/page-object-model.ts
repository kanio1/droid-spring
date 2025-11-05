/**
 * Page Object Model (POM) Implementation
 *
 * Provides reusable page objects for BSS UI components
 * Supports TypeScript generics for type-safe interactions
 *
 * Usage:
 * ```typescript
 * const customerPage = new CustomerPage(page)
 * await customerPage.navigateToList()
 * await customerPage.filterByStatus('active')
 * const customer = await customerPage.getCustomerById('123')
 * ```
 */

import { type Page, type Locator } from '@playwright/test'

// Generic CRUD operations interface
export interface CrudOperations<T> {
  create(item: T): Promise<string>
  read(id: string): Promise<T | null>
  update(id: string, item: Partial<T>): Promise<void>
  delete(id: string): Promise<void>
  list(filters?: Record<string, any>): Promise<T[]>
}

/**
 * Base Page Object with common operations
 */
export abstract class BasePage {
  protected page: Page
  protected baseUrl: string

  constructor(page: Page, baseUrl: string = '') {
    this.page = page
    this.baseUrl = baseUrl
  }

  abstract navigateTo(path?: string): Promise<void>

  async waitForPageLoad() {
    await this.page.waitForLoadState('networkidle')
  }

  async takeScreenshot(name: string) {
    await this.page.screenshot({ path: `test-results/screenshots/${name}.png` })
  }

  async reload() {
    await this.page.reload()
    await this.waitForPageLoad()
  }
}

/**
 * Customer Page Object
 */
export class CustomerPage extends BasePage implements CrudOperations<any> {
  private customerListSelector = '[data-testid="customer-list"]'
  private customerRowSelector = '[data-testid^="customer-row-"]'
  private createButtonSelector = '[data-testid="create-customer-button"]'
  private filterSelector = '[data-testid="status-filter"]'
  private searchInputSelector = '[data-testid="customer-search"]'

  constructor(page: Page) {
    super(page, '/customers')
  }

  async navigateTo(path?: string): Promise<void> {
    await this.page.goto(`${this.baseUrl}${path || ''}`)
    await this.waitForPageLoad()
  }

  async navigateToCreate(): Promise<void> {
    await this.page.click(this.createButtonSelector)
    await this.waitForPageLoad()
  }

  async navigateToDetail(id: string): Promise<void> {
    await this.page.click(`${this.customerRowSelector}[data-id="${id}"]`)
    await this.waitForPageLoad()
  }

  async create(customer: any): Promise<string> {
    await this.navigateToCreate()

    await this.page.fill('[data-testid="firstName-input"]', customer.firstName)
    await this.page.fill('[data-testid="lastName-input"]', customer.lastName)
    await this.page.fill('[data-testid="email-input"]', customer.email)
    await this.page.fill('[data-testid="phone-input"]', customer.phone)

    if (customer.status) {
      await this.page.selectOption('[data-testid="status-select"]', customer.status)
    }

    await this.page.click('[data-testid="submit-button"]')
    await this.page.waitForResponse(/.*\/api\/customers/)

    // Extract ID from response or URL
    const url = this.page.url()
    const idMatch = url.match(/\/customers\/(\w+)/)
    return idMatch ? idMatch[1] : ''
  }

  async read(id: string): Promise<any | null> {
    await this.navigateToDetail(id)

    const customerData = await this.page.evaluate(() => {
      const element = document.querySelector('[data-testid="customer-detail"]')
      if (!element) return null

      return {
        firstName: element.querySelector('[data-testid="firstName"]')?.textContent,
        lastName: element.querySelector('[data-testid="lastName"]')?.textContent,
        email: element.querySelector('[data-testid="email"]')?.textContent,
        phone: element.querySelector('[data-testid="phone"]')?.textContent,
        status: element.querySelector('[data-testid="status"]')?.textContent
      }
    })

    return customerData
  }

  async update(id: string, updates: Partial<any>): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="edit-button"]')

    if (updates.firstName) {
      await this.page.fill('[data-testid="firstName-input"]', updates.firstName)
    }
    if (updates.lastName) {
      await this.page.fill('[data-testid="lastName-input"]', updates.lastName)
    }
    if (updates.email) {
      await this.page.fill('[data-testid="email-input"]', updates.email)
    }
    if (updates.phone) {
      await this.page.fill('[data-testid="phone-input"]', updates.phone)
    }
    if (updates.status) {
      await this.page.selectOption('[data-testid="status-select"]', updates.status)
    }

    await this.page.click('[data-testid="save-button"]')
    await this.page.waitForResponse(/.*\/api\/customers.*/)
  }

  async delete(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="delete-button"]')
    await this.page.click('[data-testid="confirm-delete"]')
    await this.page.waitForResponse(/.*\/api\/customers.*/)
  }

  async list(filters?: { status?: string; search?: string }): Promise<any[]> {
    await this.navigateTo()

    if (filters?.status) {
      await this.page.selectOption(this.filterSelector, filters.status)
      await this.page.waitForTimeout(500)
    }

    if (filters?.search) {
      await this.page.fill(this.searchInputSelector, filters.search)
      await this.page.waitForTimeout(500)
    }

    const customers = await this.page.evaluate((selector: string) => {
      const rows = Array.from(document.querySelectorAll(selector))
      return rows.map(row => ({
        id: row.getAttribute('data-id'),
        firstName: row.querySelector('[data-testid="firstName"]')?.textContent,
        lastName: row.querySelector('[data-testid="lastName"]')?.textContent,
        email: row.querySelector('[data-testid="email"]')?.textContent,
        status: row.querySelector('[data-testid="status"]')?.textContent
      }))
    }, this.customerRowSelector)

    return customers
  }

  async getCustomerById(id: string): Promise<any | null> {
    const customers = await this.list()
    return customers.find(c => c.id === id) || null
  }

  async filterByStatus(status: string): Promise<void> {
    await this.page.selectOption(this.filterSelector, status)
    await this.page.waitForTimeout(500)
  }

  async searchCustomer(query: string): Promise<void> {
    await this.page.fill(this.searchInputSelector, query)
    await this.page.waitForTimeout(500)
  }

  async getTotalCount(): Promise<number> {
    const countText = await this.page.textContent('[data-testid="total-count"]')
    return countText ? parseInt(countText) : 0
  }
}

/**
 * Invoice Page Object
 */
export class InvoicePage extends BasePage {
  private invoiceListSelector = '[data-testid="invoice-list"]'
  private invoiceRowSelector = '[data-testid^="invoice-row-"]'
  private createButtonSelector = '[data-testid="create-invoice-button"]'

  constructor(page: Page) {
    super(page, '/invoices')
  }

  async navigateTo(path?: string): Promise<void> {
    await this.page.goto(`${this.baseUrl}${path || ''}`)
    await this.waitForPageLoad()
  }

  async createFromOrder(orderId: string): Promise<string> {
    await this.navigateTo(`/create?orderId=${orderId}`)
    await this.page.click('[data-testid="generate-invoice-button"]')
    await this.page.waitForResponse(/.*\/api\/invoices/)

    const url = this.page.url()
    const idMatch = url.match(/\/invoices\/(\w+)/)
    return idMatch ? idMatch[1] : ''
  }

  async markAsPaid(id: string): Promise<void> {
    await this.page.goto(`${this.baseUrl}/${id}`)
    await this.page.click('[data-testid="mark-paid-button"]')
    await this.page.click('[data-testid="confirm-paid"]')
    await this.page.waitForResponse(/.*\/api\/invoices.*/)
  }

  async downloadPdf(id: string): Promise<void> {
    const [download] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(`[data-testid="invoice-row-${id}"] [data-testid="download-pdf"]`)
    ])
    await download.saveAs(`test-results/invoices/invoice-${id}.pdf`)
  }

  async list(filters?: { status?: string; dateFrom?: string; dateTo?: string }): Promise<any[]> {
    await this.navigateTo()

    if (filters?.status) {
      await this.page.selectOption('[data-testid="status-filter"]', filters.status)
    }
    if (filters?.dateFrom) {
      await this.page.fill('[data-testid="date-from"]', filters.dateFrom)
    }
    if (filters?.dateTo) {
      await this.page.fill('[data-testid="date-to"]', filters.dateTo)
    }

    await this.page.waitForTimeout(500)

    const invoices = await this.page.evaluate((selector: string) => {
      const rows = Array.from(document.querySelectorAll(selector))
      return rows.map(row => ({
        id: row.getAttribute('data-id'),
        invoiceNumber: row.querySelector('[data-testid="invoiceNumber"]')?.textContent,
        customer: row.querySelector('[data-testid="customer"]')?.textContent,
        total: row.querySelector('[data-testid="total"]')?.textContent,
        status: row.querySelector('[data-testid="status"]')?.textContent,
        dueDate: row.querySelector('[data-testid="dueDate"]')?.textContent
      }))
    }, this.invoiceRowSelector)

    return invoices
  }
}

/**
 * Subscription Page Object
 */
export class SubscriptionPage extends BasePage {
  constructor(page: Page) {
    super(page, '/subscriptions')
  }

  async navigateTo(path?: string): Promise<void> {
    await this.page.goto(`${this.baseUrl}${path || ''}`)
    await this.waitForPageLoad()
  }

  async subscribeToPlan(planId: string, customerId: string): Promise<void> {
    await this.page.goto(`/subscribe?planId=${planId}&customerId=${customerId}`)
    await this.page.click('[data-testid="confirm-subscription-button"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions/)
  }

  async cancelSubscription(id: string): Promise<void> {
    await this.navigateTo(`/${id}`)
    await this.page.click('[data-testid="cancel-subscription-button"]')
    await this.page.click('[data-testid="confirm-cancel"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async pauseSubscription(id: string): Promise<void> {
    await this.navigateTo(`/${id}`)
    await this.page.click('[data-testid="pause-subscription-button"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async resumeSubscription(id: string): Promise<void> {
    await this.navigateTo(`/${id}`)
    await this.page.click('[data-testid="resume-subscription-button"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }
}

/**
 * Dashboard Page Object
 */
export class DashboardPage extends BasePage {
  private metricsSelector = '[data-testid="metrics"]'
  private recentActivitySelector = '[data-testid="recent-activity"]'
  private quickActionsSelector = '[data-testid="quick-actions"]'

  constructor(page: Page) {
    super(page, '/dashboard')
  }

  async navigateTo(path?: string): Promise<void> {
    await this.page.goto(`${this.baseUrl}${path || ''}`)
    await this.waitForPageLoad()
  }

  async getMetrics(): Promise<Record<string, string>> {
    const metrics = await this.page.evaluate((selector: string) => {
      const container = document.querySelector(selector)
      if (!container) return {}

      const metricElements = container.querySelectorAll('[data-testid^="metric-"]')
      const metrics: Record<string, string> = {}

      metricElements.forEach(el => {
        const key = el.getAttribute('data-testid')?.replace('metric-', '')
        const value = el.querySelector('.value')?.textContent
        if (key && value) metrics[key] = value
      })

      return metrics
    }, this.metricsSelector)

    return metrics
  }

  async getRecentActivity(): Promise<any[]> {
    const activities = await this.page.evaluate((selector: string) => {
      const items = Array.from(document.querySelectorAll(selector))
      return items.map(item => ({
        id: item.getAttribute('data-id'),
        type: item.getAttribute('data-type'),
        description: item.textContent,
        timestamp: item.getAttribute('data-timestamp')
      }))
    }, this.recentActivitySelector)

    return activities
  }

  async executeQuickAction(actionId: string): Promise<void> {
    await this.page.click(`[data-testid="quick-action-${actionId}"]`)
    await this.waitForPageLoad()
  }
}
