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
export class InvoicePage extends BasePage implements CrudOperations<any> {
  private invoiceListSelector = '[data-testid="invoices-table"]'
  private invoiceRowSelector = 'table tbody tr'
  private createButtonSelector = '[data-testid="create-invoice-button"]'
  private filterSelector = '[data-testid="status-filter"]'
  private searchInputSelector = '[data-testid="search-input"]'
  private dateFilterSelector = '[data-testid="date-filter"]'
  private exportButtonSelector = '[data-testid="export-invoices-button"]'

  constructor(page: Page) {
    super(page, '/invoices')
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
    await this.page.goto(`${this.baseUrl}/${id}`)
    await this.waitForPageLoad()
  }

  async create(invoice: any): Promise<string> {
    await this.navigateToCreate()

    if (invoice.invoiceNumber) {
      await this.page.fill('[data-testid="invoice-number-input"]', invoice.invoiceNumber)
    }

    await this.page.selectOption('[data-testid="customer-select"]', invoice.customerId)

    if (invoice.items && invoice.items.length > 0) {
      for (const item of invoice.items) {
        await this.page.selectOption('[data-testid="product-select"]', item.productId)
        await this.page.fill('[data-testid="quantity-input"]', item.quantity.toString())
        await this.page.fill('[data-testid="price-input"]', item.price.toString())
        await this.page.click('[data-testid="add-item-button"]')
        await this.page.waitForTimeout(500)
      }
    }

    await this.page.click('[data-testid="submit-button"]')
    await this.page.waitForResponse(/.*\/api\/invoices/)
    await this.page.waitForURL(/.*\/invoices\/[a-zA-Z0-9]+/)

    const url = this.page.url()
    const idMatch = url.match(/\/invoices\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }

  async read(id: string): Promise<any | null> {
    await this.navigateToDetail(id)

    const invoiceData = await this.page.evaluate(() => {
      const element = document.querySelector('[data-testid="invoice-detail"]')
      if (!element) return null

      return {
        invoiceNumber: element.querySelector('[data-testid="invoice-number"]')?.textContent,
        customer: element.querySelector('[data-testid="customer-info"]')?.textContent,
        total: element.querySelector('[data-testid="total-amount"]')?.textContent,
        status: element.querySelector('[data-testid="status-badge"]')?.textContent,
        invoiceDate: element.querySelector('[data-testid="invoice-date"]')?.textContent,
        dueDate: element.querySelector('[data-testid="due-date"]')?.textContent
      }
    })

    return invoiceData
  }

  async update(id: string, updates: Partial<any>): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="edit-invoice-button"]')

    if (updates.status === 'SENT') {
      await this.sendInvoice(id)
    } else if (updates.status === 'PAID') {
      await this.markAsPaid(id)
    }

    await this.page.waitForResponse(/.*\/api\/invoices.*/)
  }

  async delete(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="delete-invoice-button"]')
    await this.page.click('[data-testid="confirm-delete"]')
    await this.page.waitForResponse(/.*\/api\/invoices.*/)
  }

  async list(filters?: { status?: string; search?: string; dateFrom?: string; dateTo?: string }): Promise<any[]> {
    await this.navigateTo()

    if (filters?.status) {
      await this.page.selectOption(this.filterSelector, filters.status)
      await this.page.waitForTimeout(500)
    }

    if (filters?.search) {
      await this.page.fill(this.searchInputSelector, filters.search)
      await this.page.waitForTimeout(500)
    }

    if (filters?.dateFrom) {
      await this.page.fill('[data-testid="date-from"]', filters.dateFrom)
    }

    if (filters?.dateTo) {
      await this.page.fill('[data-testid="date-to"]', filters.dateTo)
    }

    await this.page.click('[data-testid="apply-date-filter"]')
    await this.page.waitForTimeout(500)

    const invoices = await this.page.evaluate((selector: string) => {
      const rows = Array.from(document.querySelectorAll(selector))
      return rows.map(row => ({
        id: row.getAttribute('data-id'),
        invoiceNumber: row.querySelector('[data-testid="invoice-number"]')?.textContent,
        customer: row.querySelector('[data-testid="customer-name"]')?.textContent,
        total: row.querySelector('[data-testid="total-amount"]')?.textContent,
        status: row.querySelector('[data-testid="status-badge"]')?.textContent,
        dueDate: row.querySelector('[data-testid="due-date"]')?.textContent
      }))
    }, this.invoiceRowSelector)

    return invoices
  }

  async getInvoiceById(id: string): Promise<any | null> {
    const invoices = await this.list()
    return invoices.find(i => i.id === id) || null
  }

  async filterByStatus(status: string): Promise<void> {
    await this.page.selectOption(this.filterSelector, status)
    await this.page.waitForTimeout(500)
  }

  async searchInvoice(query: string): Promise<void> {
    await this.page.fill(this.searchInputSelector, query)
    await this.page.waitForTimeout(500)
  }

  async createFromOrder(orderId: string): Promise<string> {
    await this.navigateTo(`/create?orderId=${orderId}`)
    await this.page.click('[data-testid="generate-from-order-button"]')
    await this.page.waitForResponse(/.*\/api\/invoices/)
    await this.page.waitForURL(/.*\/invoices\/[a-zA-Z0-9]+/)

    const url = this.page.url()
    const idMatch = url.match(/\/invoices\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }

  async sendInvoice(id: string, emailTo?: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="send-invoice-button"]')

    if (emailTo) {
      await this.page.fill('[data-testid="email-to"]', emailTo)
    }

    await this.page.click('[data-testid="confirm-send"]')
    await this.page.waitForResponse(/.*\/api\/invoices.*/)
  }

  async markAsPaid(id: string, amount?: string, method?: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="mark-paid-button"]')

    if (amount) {
      await this.page.fill('[data-testid="paid-amount"]', amount)
    }

    if (method) {
      await this.page.selectOption('[data-testid="payment-method"]', method)
    }

    await this.page.click('[data-testid="confirm-paid"]')
    await this.page.waitForResponse(/.*\/api\/invoices.*/)
  }

  async cancelInvoice(id: string, reason: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="cancel-invoice-button"]')
    await this.page.fill('[data-testid="cancellation-reason"]', reason)
    await this.page.click('[data-testid="confirm-cancel"]')
    await this.page.waitForResponse(/.*\/api\/invoices.*/)
  }

  async recordPartialPayment(id: string, amount: string, method: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="record-payment-button"]')
    await this.page.fill('[data-testid="payment-amount"]', amount)
    await this.page.selectOption('[data-testid="payment-method"]', method)
    await this.page.click('[data-testid="record-payment"]')
    await this.page.waitForResponse(/.*\/api\/invoices.*/)
  }

  async applyCredit(id: string, creditId: string, amount: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="apply-credit-button"]')
    await this.page.selectOption('[data-testid="credit-select"]', creditId)
    await this.page.fill('[data-testid="credit-amount"]', amount)
    await this.page.click('[data-testid="apply-credit-confirm"]')
    await this.page.waitForResponse(/.*\/api\/invoices.*/)
  }

  async makeRecurring(id: string, frequency: string, count: number): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="make-recurring-button"]')
    await this.page.selectOption('[data-testid="recurring-frequency"]', frequency)
    await this.page.fill('[data-testid="recurring-count"]', count.toString())
    await this.page.click('[data-testid="save-recurring"]')
    await this.page.waitForResponse(/.*\/api\/invoices.*/)
  }

  async downloadPdf(id: string): Promise<void> {
    const [download] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(`[data-testid="download-pdf-button"]`)
    ])
    await download.saveAs(`test-results/invoices/invoice-${id}.pdf`)
  }

  async emailInvoice(id: string, email: string, subject?: string, message?: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="email-invoice-button"]')
    await this.page.fill('[data-testid="email-to"]', email)

    if (subject) {
      await this.page.fill('[data-testid="email-subject"]', subject)
    }

    if (message) {
      await this.page.fill('[data-testid="email-message"]', message)
    }

    await this.page.click('[data-testid="send-email-button"]')
    await this.page.waitForTimeout(1000)
  }

  async viewHistory(id: string): Promise<any[]> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="view-history-button"]')

    const history = await this.page.evaluate(() => {
      const events = Array.from(document.querySelectorAll('[data-testid="history-event"]'))
      return events.map(event => ({
        timestamp: event.querySelector('[data-testid="event-timestamp"]')?.textContent,
        action: event.querySelector('[data-testid="event-action"]')?.textContent,
        user: event.querySelector('[data-testid="event-user"]')?.textContent,
        details: event.querySelector('[data-testid="event-details"]')?.textContent
      }))
    })

    return history
  }

  async exportInvoices(): Promise<void> {
    await this.navigateTo()
    const [download] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(this.exportButtonSelector)
    ])
    await download.saveAs(`test-results/exports/invoices-${Date.now()}.csv`)
  }

  async getTotalCount(): Promise<number> {
    const countText = await this.page.textContent('[data-testid="total-count"]')
    return countText ? parseInt(countText) : 0
  }

  async getTotalAmount(): Promise<number> {
    const totalText = await this.page.textContent('[data-testid="total-amount"]')
    return totalText ? parseFloat(totalText.replace(/[^0-9.-]+/g, '')) : 0
  }
}

/**
 * Subscription Page Object
 */
export class SubscriptionPage extends BasePage implements CrudOperations<any> {
  private subscriptionListSelector = '[data-testid="subscriptions-table"]'
  private subscriptionRowSelector = 'table tbody tr'
  private createButtonSelector = '[data-testid="create-subscription-button"]'
  private filterSelector = '[data-testid="status-filter"]'
  private planFilterSelector = '[data-testid="plan-filter"]'
  private searchInputSelector = '[data-testid="search-input"]'
  private exportButtonSelector = '[data-testid="export-subscriptions-button"]'

  constructor(page: Page) {
    super(page, '/subscriptions')
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
    await this.page.goto(`${this.baseUrl}/${id}`)
    await this.waitForPageLoad()
  }

  async create(subscription: any): Promise<string> {
    await this.navigateToCreate()

    if (subscription.subscriptionNumber) {
      await this.page.fill('[data-testid="subscription-number-input"]', subscription.subscriptionNumber)
    }

    await this.page.selectOption('[data-testid="customer-select"]', subscription.customerId)

    if (subscription.plan) {
      await this.page.selectOption('[data-testid="plan-select"]', subscription.plan)
    }

    if (subscription.billingCycle) {
      await this.page.selectOption('[data-testid="billing-cycle"]', subscription.billingCycle)
    }

    if (subscription.amount) {
      await this.page.fill('[data-testid="amount-input"]', subscription.amount.toString())
    }

    await this.page.click('[data-testid="submit-subscription"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions/)
    await this.page.waitForURL(/.*\/subscriptions\/[a-zA-Z0-9]+/)

    const url = this.page.url()
    const idMatch = url.match(/\/subscriptions\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }

  async read(id: string): Promise<any | null> {
    await this.navigateToDetail(id)

    const subscriptionData = await this.page.evaluate(() => {
      const element = document.querySelector('[data-testid="subscription-detail"]')
      if (!element) return null

      return {
        subscriptionNumber: element.querySelector('[data-testid="subscription-number"]')?.textContent,
        customer: element.querySelector('[data-testid="customer-info"]')?.textContent,
        plan: element.querySelector('[data-testid="plan-info"]')?.textContent,
        status: element.querySelector('[data-testid="status-badge"]')?.textContent,
        billingCycle: element.querySelector('[data-testid="billing-cycle"]')?.textContent,
        nextBillingDate: element.querySelector('[data-testid="next-billing-date"]')?.textContent
      }
    })

    return subscriptionData
  }

  async update(id: string, updates: Partial<any>): Promise<void> {
    await this.navigateToDetail(id)

    if (updates.status === 'ACTIVE') {
      await this.activateSubscription(id, updates.paymentMethod)
    } else if (updates.status === 'SUSPENDED') {
      await this.suspendSubscription(id, updates.reason)
    } else if (updates.status === 'CANCELLED') {
      await this.cancelSubscription(id, updates.reason)
    }

    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async delete(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="delete-subscription-button"]')
    await this.page.click('[data-testid="confirm-delete"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async list(filters?: { status?: string; plan?: string; search?: string }): Promise<any[]> {
    await this.navigateTo()

    if (filters?.status) {
      await this.page.selectOption(this.filterSelector, filters.status)
      await this.page.waitForTimeout(500)
    }

    if (filters?.plan) {
      await this.page.selectOption(this.planFilterSelector, filters.plan)
      await this.page.waitForTimeout(500)
    }

    if (filters?.search) {
      await this.page.fill(this.searchInputSelector, filters.search)
      await this.page.waitForTimeout(500)
    }

    const subscriptions = await this.page.evaluate((selector: string) => {
      const rows = Array.from(document.querySelectorAll(selector))
      return rows.map(row => ({
        id: row.getAttribute('data-id'),
        subscriptionNumber: row.querySelector('[data-testid="subscription-number"]')?.textContent,
        customer: row.querySelector('[data-testid="customer-name"]')?.textContent,
        plan: row.querySelector('[data-testid="plan"]')?.textContent,
        status: row.querySelector('[data-testid="status-badge"]')?.textContent,
        nextBilling: row.querySelector('[data-testid="next-billing"]')?.textContent
      }))
    }, this.subscriptionRowSelector)

    return subscriptions
  }

  async getSubscriptionById(id: string): Promise<any | null> {
    const subscriptions = await this.list()
    return subscriptions.find(s => s.id === id) || null
  }

  async filterByStatus(status: string): Promise<void> {
    await this.page.selectOption(this.filterSelector, status)
    await this.page.waitForTimeout(500)
  }

  async filterByPlan(plan: string): Promise<void> {
    await this.page.selectOption(this.planFilterSelector, plan)
    await this.page.waitForTimeout(500)
  }

  async searchSubscription(query: string): Promise<void> {
    await this.page.fill(this.searchInputSelector, query)
    await this.page.waitForTimeout(500)
  }

  async subscribeToPlan(planId: string, customerId: string): Promise<void> {
    await this.page.goto(`/subscribe?planId=${planId}&customerId=${customerId}`)
    await this.page.click('[data-testid="confirm-subscription-button"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions/)
  }

  async activateSubscription(id: string, paymentMethod?: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="activate-subscription-button"]')

    if (paymentMethod) {
      await this.page.selectOption('[data-testid="payment-method"]', paymentMethod)
    }

    await this.page.click('[data-testid="confirm-activate"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async suspendSubscription(id: string, reason: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="suspend-subscription-button"]')
    await this.page.fill('[data-testid="suspension-reason"]', reason)
    await this.page.click('[data-testid="confirm-suspend"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async cancelSubscription(id: string, reason: string, notes?: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="cancel-subscription-button"]')
    await this.page.selectOption('[data-testid="cancellation-reason"]', reason)

    if (notes) {
      await this.page.fill('[data-testid="cancellation-notes"]', notes)
    }

    await this.page.click('[data-testid="confirm-cancel"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async pauseSubscription(id: string, duration?: number): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="pause-subscription-button"]')

    if (duration) {
      await this.page.fill('[data-testid="pause-duration"]', duration.toString())
    }

    await this.page.click('[data-testid="confirm-pause"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async resumeSubscription(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="resume-subscription-button"]')
    await this.page.click('[data-testid="confirm-resume"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async changePlan(id: string, newPlan: string, upgradeType: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="change-plan-button"]')
    await this.page.selectOption('[data-testid="new-plan"]', newPlan)
    await this.page.selectOption('[data-testid="upgrade-type"]', upgradeType)
    await this.page.click('[data-testid="confirm-plan-change"]')
    await this.page.waitForTimeout(1000)
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async configureAutoRenewal(id: string, enable: boolean): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="edit-subscription-button"]')

    const checkbox = this.page.locator('[data-testid="auto-renew-checkbox"]')
    const isChecked = await checkbox.isChecked()

    if ((enable && !isChecked) || (!enable && isChecked)) {
      await checkbox.click()
    }

    await this.page.click('[data-testid="save-changes"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async updateBillingCycle(id: string, billingCycle: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="edit-subscription-button"]')
    await this.page.selectOption('[data-testid="billing-cycle"]', billingCycle)
    await this.page.click('[data-testid="save-changes"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async viewUsage(id: string): Promise<any> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="usage-tab"]')

    const usage = await this.page.evaluate(() => {
      return {
        data: document.querySelector('[data-testid="data-usage"]')?.textContent,
        minutes: document.querySelector('[data-testid="minutes-usage"]')?.textContent,
        sms: document.querySelector('[data-testid="sms-usage"]')?.textContent
      }
    })

    return usage
  }

  async configureNotifications(id: string, enable: boolean, method?: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="edit-subscription-button"]')

    const checkbox = this.page.locator('[data-testid="notifications-checkbox"]')
    const isChecked = await checkbox.isChecked()

    if ((enable && !isChecked) || (!enable && isChecked)) {
      await checkbox.click()
    }

    if (method) {
      await this.page.selectOption('[data-testid="notification-method"]', method)
    }

    await this.page.click('[data-testid="save-changes"]')
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async viewFeatures(id: string): Promise<any[]> {
    await this.navigateToDetail(id)

    const features = await this.page.evaluate(() => {
      const items = Array.from(document.querySelectorAll('[data-testid="feature-item"]'))
      return items.map(item => item.textContent?.trim() || '')
    })

    return features
  }

  async manualRenewal(id: string, period: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="renew-subscription-button"]')
    await this.page.selectOption('[data-testid="renewal-period"]', period)
    await this.page.click('[data-testid="confirm-renew"]')
    await this.page.waitForTimeout(1000)
    await this.page.waitForResponse(/.*\/api\/subscriptions.*/)
  }

  async exportSubscriptions(): Promise<void> {
    await this.navigateTo()
    const [download] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(this.exportButtonSelector)
    ])
    await download.saveAs(`test-results/exports/subscriptions-${Date.now()}.csv`)
  }

  async getTotalCount(): Promise<number> {
    const countText = await this.page.textContent('[data-testid="total-count"]')
    return countText ? parseInt(countText) : 0
  }

  async getSummaryCounts(): Promise<{ total: number; active: number; suspended: number; expired: number }> {
    const summary = {
      total: await this.page.textContent('[data-testid="total-subscriptions"]'),
      active: await this.page.textContent('[data-testid="active-count"]'),
      suspended: await this.page.textContent('[data-testid="suspended-count"]'),
      expired: await this.page.textContent('[data-testid="expired-count"]')
    }

    return {
      total: summary.total ? parseInt(summary.total) : 0,
      active: summary.active ? parseInt(summary.active) : 0,
      suspended: summary.suspended ? parseInt(summary.suspended) : 0,
      expired: summary.expired ? parseInt(summary.expired) : 0
    }
  }
}

/**
 * Dashboard Page Object
 */
export class DashboardPage extends BasePage {
  private metricsSelector = '[data-testid="metrics"]'
  private recentActivitySelector = '[data-testid="recent-activity"]'
  private quickActionsSelector = '[data-testid="quick-actions"]'
  private chartsSelector = '[data-testid="charts"]'
  private notificationsSelector = '[data-testid="notifications"]'
  private kpiSelector = '[data-testid^="kpi-"]'

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

  async getKPIs(): Promise<Record<string, { value: string; trend?: string; change?: string }>> {
    const kpis = await this.page.evaluate((selector: string) => {
      const elements = document.querySelectorAll(selector)
      const kpisData: Record<string, { value: string; trend?: string; change?: string }> = {}

      elements.forEach(el => {
        const key = el.getAttribute('data-testid')?.replace('kpi-', '')
        if (key) {
          kpisData[key] = {
            value: el.querySelector('.value')?.textContent || '',
            trend: el.querySelector('.trend')?.textContent || undefined,
            change: el.querySelector('.change')?.textContent || undefined
          }
        }
      })

      return kpisData
    }, this.kpiSelector)

    return kpis
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

  async refreshDashboard(): Promise<void> {
    await this.page.click('[data-testid="refresh-dashboard"]')
    await this.waitForLoadingToComplete()
  }

  async customizeDashboard(): Promise<void> {
    await this.page.click('[data-testid="customize-dashboard"]')
    await this.page.waitForTimeout(500)
  }

  async saveDashboardLayout(): Promise<void> {
    await this.page.click('[data-testid="save-layout"]')
    await this.page.waitForTimeout(500)
  }

  async resetDashboardLayout(): Promise<void> {
    await this.page.click('[data-testid="reset-layout"]')
    await this.page.click('[data-testid="confirm-reset"]')
    await this.page.waitForTimeout(1000)
  }

  async getWidget(widgetId: string): Promise<any> {
    const widget = await this.page.evaluate((id: string) => {
      const element = document.querySelector(`[data-testid="widget-${id}"]`)
      if (!element) return null

      return {
        id,
        title: element.querySelector('.title')?.textContent,
        content: element.querySelector('.content')?.textContent,
        visible: element.offsetParent !== null
      }
    }, widgetId)

    return widget
  }

  async toggleWidget(widgetId: string): Promise<void> {
    await this.page.click(`[data-testid="widget-${widgetId}"] [data-testid="toggle-visibility"]`)
    await this.page.waitForTimeout(300)
  }

  async resizeWidget(widgetId: string, size: 'small' | 'medium' | 'large'): Promise<void> {
    await this.page.click(`[data-testid="widget-${widgetId}"] [data-testid="resize-${size}"]`)
    await this.page.waitForTimeout(500)
  }

  async moveWidget(widgetId: string, position: number): Promise<void> {
    const widget = this.page.locator(`[data-testid="widget-${widgetId}"]`)
    await widget.dragTo(this.page.locator(`[data-testid="widget-position-${position}"]`))
    await this.page.waitForTimeout(500)
  }

  async selectTimeRange(range: string): Promise<void> {
    await this.page.selectOption('[data-testid="time-range-select"]', range)
    await this.page.waitForTimeout(1000)
  }

  async getChartData(chartId: string): Promise<any> {
    const data = await this.page.evaluate((id: string) => {
      const element = document.querySelector(`[data-testid="chart-${id}"]`)
      if (!element) return null

      // This would typically return the actual chart data from the chart library
      // For now, just return some metadata
      return {
        type: element.getAttribute('data-chart-type'),
        title: element.querySelector('.chart-title')?.textContent,
        dataPoints: element.querySelectorAll('.data-point').length
      }
    }, chartId)

    return data
  }

  async exportDashboard(format: 'pdf' | 'png' | 'csv'): Promise<void> {
    const [download] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(`[data-testid="export-${format}"]`)
    ])
    await download.saveAs(`test-results/dashboard-export-${Date.now()}.${format}`)
  }

  async getNotificationCount(): Promise<number> {
    const countElement = await this.page.locator('[data-testid="notification-count"]')
    if (await countElement.isVisible()) {
      const count = await countElement.textContent()
      return count ? parseInt(count) : 0
    }
    return 0
  }

  async viewAllNotifications(): Promise<void> {
    await this.page.click('[data-testid="view-all-notifications"]')
    await this.page.waitForTimeout(500)
  }

  async markNotificationAsRead(notificationId: string): Promise<void> {
    await this.page.click(`[data-testid="notification-${notificationId}"] [data-testid="mark-read"]`)
    await this.page.waitForTimeout(300)
  }

  async dismissNotification(notificationId: string): Promise<void> {
    await this.page.click(`[data-testid="notification-${notificationId}"] [data-testid="dismiss"]`)
    await this.page.waitForTimeout(300)
  }

  async getSystemStatus(): Promise<{ status: string; uptime: string; version: string }> {
    const status = await this.page.evaluate(() => {
      return {
        status: document.querySelector('[data-testid="system-status"]')?.textContent || '',
        uptime: document.querySelector('[data-testid="system-uptime"]')?.textContent || '',
        version: document.querySelector('[data-testid="system-version"]')?.textContent || ''
      }
    })

    return status
  }

  async getPerformanceMetrics(): Promise<Record<string, string>> {
    const metrics = await this.page.evaluate(() => {
      const elements = document.querySelectorAll('[data-testid^="perf-metric-"]')
      const data: Record<string, string> = {}

      elements.forEach(el => {
        const key = el.getAttribute('data-testid')?.replace('perf-metric-', '')
        const value = el.querySelector('.value')?.textContent
        if (key && value) data[key] = value
      })

      return data
    })

    return metrics
  }

  async getAlerts(): Promise<any[]> {
    const alerts = await this.page.evaluate(() => {
      const items = Array.from(document.querySelectorAll('[data-testid^="alert-"]'))
      return items.map(item => ({
        id: item.getAttribute('data-id'),
        type: item.getAttribute('data-type'),
        severity: item.getAttribute('data-severity'),
        message: item.querySelector('.message')?.textContent,
        timestamp: item.getAttribute('data-timestamp'),
        acknowledged: item.getAttribute('data-acknowledged') === 'true'
      }))
    })

    return alerts
  }

  async acknowledgeAlert(alertId: string): Promise<void> {
    await this.page.click(`[data-testid="alert-${alertId}"] [data-testid="acknowledge"]`)
    await this.page.waitForTimeout(300)
  }

  async filterAlerts(severity?: string, type?: string): Promise<void> {
    if (severity) {
      await this.page.selectOption('[data-testid="alert-severity-filter"]', severity)
    }
    if (type) {
      await this.page.selectOption('[data-testid="alert-type-filter"]', type)
    }
    await this.page.waitForTimeout(500)
  }

  async getStatistics(): Promise<Record<string, { current: number; previous: number; change: number }>> {
    const stats = await this.page.evaluate(() => {
      const elements = document.querySelectorAll('[data-testid^="stat-"]')
      const data: Record<string, { current: number; previous: number; change: number }> = {}

      elements.forEach(el => {
        const key = el.getAttribute('data-testid')?.replace('stat-', '')
        if (key) {
          data[key] = {
            current: parseFloat(el.querySelector('.current')?.textContent || '0'),
            previous: parseFloat(el.querySelector('.previous')?.textContent || '0'),
            change: parseFloat(el.querySelector('.change')?.textContent || '0')
          }
        }
      })

      return data
    })

    return stats
  }

  async getTopCustomers(limit: number = 5): Promise<any[]> {
    const customers = await this.page.evaluate((limitArg: number) => {
      const items = Array.from(document.querySelectorAll('[data-testid="top-customer"]')).slice(0, limitArg)
      return items.map(item => ({
        name: item.querySelector('.name')?.textContent,
        revenue: item.querySelector('.revenue')?.textContent,
        orders: item.querySelector('.orders')?.textContent
      }))
    }, limit)

    return customers
  }

  async getRecentOrders(limit: number = 10): Promise<any[]> {
    const orders = await this.page.evaluate((limitArg: number) => {
      const items = Array.from(document.querySelectorAll('[data-testid="recent-order"]')).slice(0, limitArg)
      return items.map(item => ({
        id: item.getAttribute('data-id'),
        number: item.querySelector('.order-number')?.textContent,
        customer: item.querySelector('.customer')?.textContent,
        total: item.querySelector('.total')?.textContent,
        status: item.querySelector('.status')?.textContent,
        date: item.querySelector('.date')?.textContent
      }))
    }, limit)

    return orders
  }

  async getRevenueChart(period: string): Promise<any> {
    await this.selectTimeRange(period)

    const chartData = await this.page.evaluate(() => {
      const chart = document.querySelector('[data-testid="revenue-chart"]')
      if (!chart) return null

      // This would typically extract the actual chart data
      return {
        period: chart.getAttribute('data-period'),
        total: chart.getAttribute('data-total'),
        dataPoints: Array.from(chart.querySelectorAll('.data-point')).length
      }
    })

    return chartData
  }

  private async waitForLoadingToComplete(): Promise<void> {
    await this.page.waitForSelector('[data-testid="loading"]', { state: 'hidden' })
    await this.page.waitForTimeout(1000)
  }
}

/**
 * Order Page Object
 */
export class OrderPage extends BasePage implements CrudOperations<any> {
  private orderListSelector = '[data-testid="orders-table"]'
  private orderRowSelector = 'table tbody tr'
  private createButtonSelector = '[data-testid="create-order-button"]'
  private filterSelector = '[data-testid="status-filter"]'
  private searchInputSelector = '[data-testid="search-input"]'
  private exportButtonSelector = '[data-testid="export-orders-button"]'

  constructor(page: Page) {
    super(page, '/orders')
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
    await this.page.goto(`${this.baseUrl}/${id}`)
    await this.waitForPageLoad()
  }

  async create(order: any): Promise<string> {
    await this.navigateToCreate()

    if (order.orderNumber) {
      await this.page.fill('[data-testid="order-number-input"]', order.orderNumber)
    }

    await this.page.selectOption('[data-testid="customer-select"]', order.customerId)

    if (order.items && order.items.length > 0) {
      for (const item of order.items) {
        await this.page.selectOption('[data-testid="product-select"]', item.productId)
        await this.page.fill('[data-testid="quantity-input"]', item.quantity.toString())
        await this.page.click('[data-testid="add-item-button"]')
        await this.page.waitForTimeout(500)
      }
    }

    await this.page.click('[data-testid="submit-button"]')
    await this.page.waitForResponse(/.*\/api\/orders/)
    await this.page.waitForURL(/.*\/orders\/[a-zA-Z0-9]+/)

    const url = this.page.url()
    const idMatch = url.match(/\/orders\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }

  async read(id: string): Promise<any | null> {
    await this.navigateToDetail(id)

    const orderData = await this.page.evaluate(() => {
      const element = document.querySelector('[data-testid="order-detail"]')
      if (!element) return null

      return {
        orderNumber: element.querySelector('[data-testid="order-number"]')?.textContent,
        customer: element.querySelector('[data-testid="customer-info"]')?.textContent,
        total: element.querySelector('[data-testid="order-total"]')?.textContent,
        status: element.querySelector('[data-testid="status-badge"]')?.textContent,
        orderDate: element.querySelector('[data-testid="order-date"]')?.textContent
      }
    })

    return orderData
  }

  async update(id: string, updates: Partial<any>): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="edit-order-button"]')

    // Apply updates based on what's provided
    if (updates.status) {
      await this.page.click('[data-testid="change-status-button"]')
      await this.page.selectOption('[data-testid="status-select"]', updates.status)
      if (updates.trackingNumber) {
        await this.page.fill('[data-testid="tracking-number"]', updates.trackingNumber)
      }
      await this.page.click('[data-testid="confirm-status-change"]')
    }

    await this.page.waitForResponse(/.*\/api\/orders.*/)
  }

  async delete(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="delete-order-button"]')
    await this.page.click('[data-testid="confirm-delete"]')
    await this.page.waitForResponse(/.*\/api\/orders.*/)
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

    const orders = await this.page.evaluate((selector: string) => {
      const rows = Array.from(document.querySelectorAll(selector))
      return rows.map(row => ({
        id: row.getAttribute('data-id'),
        orderNumber: row.querySelector('[data-testid="order-number"]')?.textContent,
        customer: row.querySelector('[data-testid="customer-name"]')?.textContent,
        total: row.querySelector('[data-testid="order-total"]')?.textContent,
        status: row.querySelector('[data-testid="status-badge"]')?.textContent,
        date: row.querySelector('[data-testid="order-date"]')?.textContent
      }))
    }, this.orderRowSelector)

    return orders
  }

  async getOrderById(id: string): Promise<any | null> {
    const orders = await this.list()
    return orders.find(o => o.id === id) || null
  }

  async filterByStatus(status: string): Promise<void> {
    await this.page.selectOption(this.filterSelector, status)
    await this.page.waitForTimeout(500)
  }

  async searchOrder(query: string): Promise<void> {
    await this.page.fill(this.searchInputSelector, query)
    await this.page.waitForTimeout(500)
  }

  async changeStatus(id: string, newStatus: string, trackingNumber?: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="change-status-button"]')
    await this.page.selectOption('[data-testid="status-select"]', newStatus)

    if (trackingNumber) {
      await this.page.fill('[data-testid="tracking-number"]', trackingNumber)
    }

    await this.page.click('[data-testid="confirm-status-change"]')
    await this.page.waitForResponse(/.*\/api\/orders.*/)
  }

  async fulfillOrder(id: string, partial = false): Promise<void> {
    await this.navigateToDetail(id)

    if (partial) {
      await this.page.click('[data-testid="fulfill-partial-button"]')
      await this.page.check('[data-testid="item-0-checkbox"]')
      await this.page.click('[data-testid="confirm-partial-fulfill"]')
    } else {
      await this.page.click('[data-testid="fulfill-order-button"]')
      await this.page.click('[data-testid="confirm-fulfill"]')
    }

    await this.page.waitForResponse(/.*\/api\/orders.*/)
  }

  async cancelOrder(id: string, reason: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="cancel-order-button"]')
    await this.page.fill('[data-testid="cancellation-reason"]', reason)
    await this.page.click('[data-testid="confirm-cancel"]')
    await this.page.waitForResponse(/.*\/api\/orders.*/)
  }

  async createReturn(id: string, items: number[], reason: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="create-return-button"]')

    for (const itemIndex of items) {
      await this.page.check(`[data-testid="return-item-${itemIndex}"]`)
    }

    await this.page.selectOption('[data-testid="return-reason"]', reason)
    await this.page.click('[data-testid="submit-return"]')
    await this.page.waitForResponse(/.*\/api\/orders.*/)
  }

  async processRefund(id: string, amount: string, method: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="process-refund-button"]')
    await this.page.selectOption('[data-testid="refund-method"]', method)
    await this.page.fill('[data-testid="refund-amount"]', amount)
    await this.page.click('[data-testid="confirm-refund"]')
    await this.page.waitForResponse(/.*\/api\/orders.*/)
  }

  async exportOrders(): Promise<void> {
    await this.navigateTo()
    const [download] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(this.exportButtonSelector)
    ])
    await download.saveAs(`test-results/exports/orders-${Date.now()}.csv`)
  }

  async getTotalCount(): Promise<number> {
    const countText = await this.page.textContent('[data-testid="total-count"]')
    return countText ? parseInt(countText) : 0
  }
}

/**
 * Payment Page Object
 */
export class PaymentPage extends BasePage implements CrudOperations<any> {
  private paymentListSelector = '[data-testid="payments-table"]'
  private paymentRowSelector = 'table tbody tr'
  private createButtonSelector = '[data-testid="create-payment-button"]'
  private filterSelector = '[data-testid="status-filter"]'
  private methodFilterSelector = '[data-testid="method-filter"]'
  private searchInputSelector = '[data-testid="search-input"]'
  private exportButtonSelector = '[data-testid="export-payments-button"]'

  constructor(page: Page) {
    super(page, '/payments')
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
    await this.page.goto(`${this.baseUrl}/${id}`)
    await this.waitForPageLoad()
  }

  async create(payment: any): Promise<string> {
    await this.navigateToCreate()

    if (payment.paymentNumber) {
      await this.page.fill('[data-testid="payment-number-input"]', payment.paymentNumber)
    }

    await this.page.selectOption('[data-testid="customer-select"]', payment.customerId)
    await this.page.fill('[data-testid="amount-input"]', payment.amount.toString())

    if (payment.method) {
      await this.page.selectOption('[data-testid="payment-method"]', payment.method)
    }

    if (payment.currency) {
      await this.page.selectOption('[data-testid="currency"]', payment.currency)
    }

    await this.page.click('[data-testid="submit-payment"]')
    await this.page.waitForResponse(/.*\/api\/payments/)
    await this.page.waitForURL(/.*\/payments\/[a-zA-Z0-9]+/)

    const url = this.page.url()
    const idMatch = url.match(/\/payments\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }

  async read(id: string): Promise<any | null> {
    await this.navigateToDetail(id)

    const paymentData = await this.page.evaluate(() => {
      const element = document.querySelector('[data-testid="payment-detail"]')
      if (!element) return null

      return {
        paymentNumber: element.querySelector('[data-testid="payment-number"]')?.textContent,
        transactionId: element.querySelector('[data-testid="transaction-id"]')?.textContent,
        amount: element.querySelector('[data-testid="amount"]')?.textContent,
        method: element.querySelector('[data-testid="payment-method"]')?.textContent,
        status: element.querySelector('[data-testid="status-badge"]')?.textContent,
        date: element.querySelector('[data-testid="payment-date"]')?.textContent
      }
    })

    return paymentData
  }

  async update(id: string, updates: Partial<any>): Promise<void> {
    await this.navigateToDetail(id)

    if (updates.status === 'PROCESSING') {
      await this.page.click('[data-testid="process-payment-button"]')
      await this.page.click('[data-testid="confirm-process"]')
    } else if (updates.status === 'FAILED') {
      await this.page.click('[data-testid="mark-failed-button"]')
      await this.page.fill('[data-testid="failure-reason"]', updates.reason || 'Payment failed')
      await this.page.click('[data-testid="confirm-fail"]')
    }

    await this.page.waitForResponse(/.*\/api\/payments.*/)
  }

  async delete(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="delete-payment-button"]')
    await this.page.click('[data-testid="confirm-delete"]')
    await this.page.waitForResponse(/.*\/api\/payments.*/)
  }

  async list(filters?: { status?: string; method?: string; search?: string; dateFrom?: string; dateTo?: string }): Promise<any[]> {
    await this.navigateTo()

    if (filters?.status) {
      await this.page.selectOption(this.filterSelector, filters.status)
      await this.page.waitForTimeout(500)
    }

    if (filters?.method) {
      await this.page.selectOption(this.methodFilterSelector, filters.method)
      await this.page.waitForTimeout(500)
    }

    if (filters?.search) {
      await this.page.fill(this.searchInputSelector, filters.search)
      await this.page.waitForTimeout(500)
    }

    if (filters?.dateFrom) {
      await this.page.fill('[data-testid="date-from"]', filters.dateFrom)
    }

    if (filters?.dateTo) {
      await this.page.fill('[data-testid="date-to"]', filters.dateTo)
    }

    await this.page.click('[data-testid="apply-date-filter"]')
    await this.page.waitForTimeout(500)

    const payments = await this.page.evaluate((selector: string) => {
      const rows = Array.from(document.querySelectorAll(selector))
      return rows.map(row => ({
        id: row.getAttribute('data-id'),
        paymentNumber: row.querySelector('[data-testid="payment-number"]')?.textContent,
        customer: row.querySelector('[data-testid="customer-name"]')?.textContent,
        amount: row.querySelector('[data-testid="amount"]')?.textContent,
        method: row.querySelector('[data-testid="method-badge"]')?.textContent,
        status: row.querySelector('[data-testid="status-badge"]')?.textContent,
        date: row.querySelector('[data-testid="payment-date"]')?.textContent
      }))
    }, this.paymentRowSelector)

    return payments
  }

  async getPaymentById(id: string): Promise<any | null> {
    const payments = await this.list()
    return payments.find(p => p.id === id) || null
  }

  async filterByStatus(status: string): Promise<void> {
    await this.page.selectOption(this.filterSelector, status)
    await this.page.waitForTimeout(500)
  }

  async filterByMethod(method: string): Promise<void> {
    await this.page.selectOption(this.methodFilterSelector, method)
    await this.page.waitForTimeout(500)
  }

  async searchPayment(query: string): Promise<void> {
    await this.page.fill(this.searchInputSelector, query)
    await this.page.waitForTimeout(500)
  }

  async processPayment(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="process-payment-button"]')
    await this.page.click('[data-testid="confirm-process"]')
    await this.page.waitForResponse(/.*\/api\/payments.*/)
  }

  async retryPayment(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="retry-payment-button"]')
    await this.page.click('[data-testid="confirm-retry"]')
    await this.page.waitForTimeout(1000)
    await this.page.waitForResponse(/.*\/api\/payments.*/)
  }

  async refundPayment(id: string, type: 'FULL' | 'PARTIAL', amount: string, reason: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="refund-payment-button"]')
    await this.page.selectOption('[data-testid="refund-type"]', type)
    await this.page.fill('[data-testid="refund-amount"]', amount)
    await this.page.selectOption('[data-testid="refund-reason"]', reason)
    await this.page.click('[data-testid="confirm-refund"]')
    await this.page.waitForTimeout(1000)
    await this.page.waitForResponse(/.*\/api\/payments.*/)
  }

  async createDispute(id: string, reason: string, amount: string, description: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="create-dispute-button"]')
    await this.page.selectOption('[data-testid="dispute-reason"]', reason)
    await this.page.fill('[data-testid="dispute-amount"]', amount)
    await this.page.fill('[data-testid="dispute-description"]', description)
    await this.page.click('[data-testid="submit-dispute"]')
    await this.page.waitForTimeout(1000)
    await this.page.waitForResponse(/.*\/api\/payments.*/)
  }

  async viewPaymentHistory(id: string): Promise<any[]> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="view-history-button"]')

    const history = await this.page.evaluate(() => {
      const events = Array.from(document.querySelectorAll('[data-testid="history-event"]'))
      return events.map(event => ({
        timestamp: event.querySelector('[data-testid="event-timestamp"]')?.textContent,
        action: event.querySelector('[data-testid="event-action"]')?.textContent,
        user: event.querySelector('[data-testid="event-user"]')?.textContent,
        details: event.querySelector('[data-testid="event-details"]')?.textContent
      }))
    })

    return history
  }

  async exportPayments(): Promise<void> {
    await this.navigateTo()
    const [download] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(this.exportButtonSelector)
    ])
    await download.saveAs(`test-results/exports/payments-${Date.now()}.csv`)
  }

  async getTotalCount(): Promise<number> {
    const countText = await this.page.textContent('[data-testid="total-count"]')
    return countText ? parseInt(countText) : 0
  }

  async getTotalAmount(): Promise<number> {
    const totalText = await this.page.textContent('[data-testid="total-amount"]')
    return totalText ? parseFloat(totalText.replace(/[^0-9.-]+/g, '')) : 0
  }
}

/**
 * Product Page Object
 */
export class ProductPage extends BasePage implements CrudOperations<any> {
  private productListSelector = '[data-testid="products-table"]'
  private productRowSelector = 'table tbody tr'
  private createButtonSelector = '[data-testid="create-product-button"]'
  private filterSelector = '[data-testid="status-filter"]'
  private categoryFilterSelector = '[data-testid="category-filter"]'
  private searchInputSelector = '[data-testid="search-input"]'
  private exportButtonSelector = '[data-testid="export-products-button"]'

  constructor(page: Page) {
    super(page, '/products')
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
    await this.page.goto(`${this.baseUrl}/${id}`)
    await this.waitForPageLoad()
  }

  async create(product: any): Promise<string> {
    await this.navigateToCreate()

    await this.page.fill('[data-testid="name-input"]', product.name)
    await this.page.fill('[data-testid="description-input"]', product.description)
    await this.page.fill('[data-testid="sku-input"]', product.sku)
    await this.page.fill('[data-testid="price-input"]', product.price.toString())

    if (product.category) {
      await this.page.selectOption('[data-testid="category-select"]', product.category)
    }

    if (product.status) {
      await this.page.selectOption('[data-testid="status-select"]', product.status)
    }

    if (product.inventory !== undefined) {
      await this.page.fill('[data-testid="inventory-input"]', product.inventory.toString())
    }

    await this.page.click('[data-testid="submit-button"]')
    await this.page.waitForResponse(/.*\/api\/products/)
    await this.page.waitForURL(/.*\/products\/[a-zA-Z0-9]+/)

    const url = this.page.url()
    const idMatch = url.match(/\/products\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }

  async read(id: string): Promise<any | null> {
    await this.navigateToDetail(id)

    const productData = await this.page.evaluate(() => {
      const element = document.querySelector('[data-testid="product-detail"]')
      if (!element) return null

      return {
        name: element.querySelector('[data-testid="name"]')?.textContent,
        description: element.querySelector('[data-testid="description"]')?.textContent,
        sku: element.querySelector('[data-testid="sku"]')?.textContent,
        price: element.querySelector('[data-testid="price"]')?.textContent,
        category: element.querySelector('[data-testid="category"]')?.textContent,
        status: element.querySelector('[data-testid="status-badge"]')?.textContent,
        inventory: element.querySelector('[data-testid="inventory"]')?.textContent
      }
    })

    return productData
  }

  async update(id: string, updates: Partial<any>): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="edit-button"]')

    if (updates.name) {
      await this.page.fill('[data-testid="name-input"]', updates.name)
    }
    if (updates.description) {
      await this.page.fill('[data-testid="description-input"]', updates.description)
    }
    if (updates.price) {
      await this.page.fill('[data-testid="price-input"]', updates.price.toString())
    }
    if (updates.status) {
      await this.page.selectOption('[data-testid="status-select"]', updates.status)
    }
    if (updates.inventory !== undefined) {
      await this.page.fill('[data-testid="inventory-input"]', updates.inventory.toString())
    }

    await this.page.click('[data-testid="save-button"]')
    await this.page.waitForResponse(/.*\/api\/products.*/)
  }

  async delete(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="delete-button"]')
    await this.page.click('[data-testid="confirm-delete"]')
    await this.page.waitForResponse(/.*\/api\/products.*/)
  }

  async list(filters?: { status?: string; category?: string; search?: string }): Promise<any[]> {
    await this.navigateTo()

    if (filters?.status) {
      await this.page.selectOption(this.filterSelector, filters.status)
      await this.page.waitForTimeout(500)
    }

    if (filters?.category) {
      await this.page.selectOption(this.categoryFilterSelector, filters.category)
      await this.page.waitForTimeout(500)
    }

    if (filters?.search) {
      await this.page.fill(this.searchInputSelector, filters.search)
      await this.page.waitForTimeout(500)
    }

    const products = await this.page.evaluate((selector: string) => {
      const rows = Array.from(document.querySelectorAll(selector))
      return rows.map(row => ({
        id: row.getAttribute('data-id'),
        name: row.querySelector('[data-testid="name"]')?.textContent,
        sku: row.querySelector('[data-testid="sku"]')?.textContent,
        price: row.querySelector('[data-testid="price"]')?.textContent,
        category: row.querySelector('[data-testid="category"]')?.textContent,
        status: row.querySelector('[data-testid="status-badge"]')?.textContent,
        inventory: row.querySelector('[data-testid="inventory"]')?.textContent
      }))
    }, this.productRowSelector)

    return products
  }

  async getProductById(id: string): Promise<any | null> {
    const products = await this.list()
    return products.find(p => p.id === id) || null
  }

  async filterByStatus(status: string): Promise<void> {
    await this.page.selectOption(this.filterSelector, status)
    await this.page.waitForTimeout(500)
  }

  async filterByCategory(category: string): Promise<void> {
    await this.page.selectOption(this.categoryFilterSelector, category)
    await this.page.waitForTimeout(500)
  }

  async searchProduct(query: string): Promise<void> {
    await this.page.fill(this.searchInputSelector, query)
    await this.page.waitForTimeout(500)
  }

  async updateInventory(id: string, newQuantity: number): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="update-inventory-button"]')
    await this.page.fill('[data-testid="new-quantity-input"]', newQuantity.toString())
    await this.page.selectOption('[data-testid="adjustment-reason"]', 'MANUAL_ADJUSTMENT')
    await this.page.click('[data-testid="confirm-update"]')
    await this.page.waitForResponse(/.*\/api\/products.*/)
  }

  async exportProducts(): Promise<void> {
    await this.navigateTo()
    const [download] = await Promise.all([
      this.page.waitForEvent('download'),
      this.page.click(this.exportButtonSelector)
    ])
    await download.saveAs(`test-results/exports/products-${Date.now()}.csv`)
  }

  async getTotalCount(): Promise<number> {
    const countText = await this.page.textContent('[data-testid="total-count"]')
    return countText ? parseInt(countText) : 0
  }
}

/**
 * Address Page Object
 */
export class AddressPage extends BasePage implements CrudOperations<any> {
  private addressListSelector = '[data-testid="addresses-table"]'
  private createButtonSelector = '[data-testid="add-address-button"]'
  private searchInputSelector = '[data-testid="search-input"]'

  constructor(page: Page) {
    super(page, '/addresses')
  }

  async navigateTo(path?: string): Promise<void> {
    await this.page.goto(`${this.baseUrl}${path || ''}`)
    await this.waitForPageLoad()
  }

  async navigateToCreate(): Promise<void> {
    await this.page.click(this.createButtonSelector)
    await this.waitForPageLoad()
  }

  async create(address: any): Promise<string> {
    await this.navigateToCreate()

    await this.page.selectOption('[data-testid="customer-select"]', address.customerId)
    await this.page.fill('[data-testid="street-input"]', address.street)
    await this.page.fill('[data-testid="city-input"]', address.city)
    await this.page.fill('[data-testid="state-input"]', address.state)
    await this.page.fill('[data-testid="zip-input"]', address.zip)
    await this.page.fill('[data-testid="country-input"]', address.country)

    if (address.type) {
      await this.page.selectOption('[data-testid="type-select"]', address.type)
    }

    if (address.isPrimary) {
      await this.page.check('[data-testid="primary-checkbox"]')
    }

    await this.page.click('[data-testid="submit-button"]')
    await this.page.waitForResponse(/.*\/api\/addresses/)
    await this.page.waitForURL(/.*\/addresses\/[a-zA-Z0-9]+/)

    const url = this.page.url()
    const idMatch = url.match(/\/addresses\/([a-zA-Z0-9]+)/)
    return idMatch ? idMatch[1] : ''
  }

  async read(id: string): Promise<any | null> {
    await this.navigateToDetail(id)

    const addressData = await this.page.evaluate(() => {
      const element = document.querySelector('[data-testid="address-detail"]')
      if (!element) return null

      return {
        street: element.querySelector('[data-testid="street"]')?.textContent,
        city: element.querySelector('[data-testid="city"]')?.textContent,
        state: element.querySelector('[data-testid="state"]')?.textContent,
        zip: element.querySelector('[data-testid="zip"]')?.textContent,
        country: element.querySelector('[data-testid="country"]')?.textContent,
        type: element.querySelector('[data-testid="type"]')?.textContent,
        isPrimary: element.querySelector('[data-testid="primary-badge"]') !== null
      }
    })

    return addressData
  }

  async update(id: string, updates: Partial<any>): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="edit-button"]')

    if (updates.street) {
      await this.page.fill('[data-testid="street-input"]', updates.street)
    }
    if (updates.city) {
      await this.page.fill('[data-testid="city-input"]', updates.city)
    }
    if (updates.state) {
      await this.page.fill('[data-testid="state-input"]', updates.state)
    }
    if (updates.zip) {
      await this.page.fill('[data-testid="zip-input"]', updates.zip)
    }
    if (updates.country) {
      await this.page.fill('[data-testid="country-input"]', updates.country)
    }
    if (updates.type) {
      await this.page.selectOption('[data-testid="type-select"]', updates.type)
    }

    await this.page.click('[data-testid="save-button"]')
    await this.page.waitForResponse(/.*\/api\/addresses.*/)
  }

  async delete(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="delete-button"]')
    await this.page.click('[data-testid="confirm-delete"]')
    await this.page.waitForResponse(/.*\/api\/addresses.*/)
  }

  async list(filters?: { customerId?: string; type?: string; search?: string }): Promise<any[]> {
    await this.navigateTo()

    if (filters?.customerId) {
      await this.page.selectOption('[data-testid="customer-filter"]', filters.customerId)
      await this.page.waitForTimeout(500)
    }

    if (filters?.type) {
      await this.page.selectOption('[data-testid="type-filter"]', filters.type)
      await this.page.waitForTimeout(500)
    }

    if (filters?.search) {
      await this.page.fill(this.searchInputSelector, filters.search)
      await this.page.waitForTimeout(500)
    }

    const addresses = await this.page.evaluate(() => {
      const rows = Array.from(document.querySelectorAll('table tbody tr'))
      return rows.map(row => ({
        id: row.getAttribute('data-id'),
        street: row.querySelector('[data-testid="street"]')?.textContent,
        city: row.querySelector('[data-testid="city"]')?.textContent,
        state: row.querySelector('[data-testid="state"]')?.textContent,
        zip: row.querySelector('[data-testid="zip"]')?.textContent,
        country: row.querySelector('[data-testid="country"]')?.textContent,
        type: row.querySelector('[data-testid="type-badge"]')?.textContent,
        isPrimary: row.querySelector('[data-testid="primary-badge"]') !== null
      }))
    }, this.addressListSelector)

    return addresses
  }

  async getAddressById(id: string): Promise<any | null> {
    const addresses = await this.list()
    return addresses.find(a => a.id === id) || null
  }

  async setPrimaryAddress(id: string): Promise<void> {
    await this.navigateToDetail(id)
    await this.page.click('[data-testid="set-primary-button"]')
    await this.page.click('[data-testid="confirm-set-primary"]')
    await this.page.waitForResponse(/.*\/api\/addresses.*/)
  }

  async searchAddress(query: string): Promise<void> {
    await this.page.fill(this.searchInputSelector, query)
    await this.page.waitForTimeout(500)
  }

  async getTotalCount(): Promise<number> {
    const countText = await this.page.textContent('[data-testid="total-count"]')
    return countText ? parseInt(countText) : 0
  }

  private async navigateToDetail(id: string): Promise<void> {
    await this.page.goto(`${this.baseUrl}/${id}`)
    await this.waitForPageLoad()
  }
}

/**
 * Navigation Page Object
 */
export class NavigationPage extends BasePage {
  private mainMenuSelector = '[data-testid="main-menu"]'
  private userMenuSelector = '[data-testid="user-menu"]'
  private breadcrumbsSelector = '[data-testid="breadcrumbs"]'
  private sidebarSelector = '[data-testid="sidebar"]'

  constructor(page: Page) {
    super(page, '')
  }

  async navigateTo(path?: string): Promise<void> {
    await this.page.goto(path || '/')
    await this.waitForPageLoad()
  }

  async clickMenuItem(menuId: string): Promise<void> {
    await this.page.click(`[data-testid="menu-${menuId}"]`)
    await this.waitForPageLoad()
  }

  async clickSubmenuItem(menuId: string, submenuId: string): Promise<void> {
    await this.page.click(`[data-testid="menu-${menuId}"]`)
    await this.page.click(`[data-testid="submenu-${submenuId}"]`)
    await this.waitForPageLoad()
  }

  async navigateToCustomers(): Promise<void> {
    await this.page.goto('/customers')
    await this.waitForPageLoad()
  }

  async navigateToOrders(): Promise<void> {
    await this.page.goto('/orders')
    await this.waitForPageLoad()
  }

  async navigateToInvoices(): Promise<void> {
    await this.page.goto('/invoices')
    await this.waitForPageLoad()
  }

  async navigateToPayments(): Promise<void> {
    await this.page.goto('/payments')
    await this.waitForPageLoad()
  }

  async navigateToSubscriptions(): Promise<void> {
    await this.page.goto('/subscriptions')
    await this.waitForPageLoad()
  }

  async navigateToProducts(): Promise<void> {
    await this.page.goto('/products')
    await this.waitForPageLoad()
  }

  async navigateToDashboard(): Promise<void> {
    await this.page.goto('/dashboard')
    await this.waitForPageLoad()
  }

  async clickUserMenu(): Promise<void> {
    await this.page.click(this.userMenuSelector)
  }

  async clickProfile(): Promise<void> {
    await this.page.click('[data-testid="menu-profile"]')
    await this.waitForPageLoad()
  }

  async clickSettings(): Promise<void> {
    await this.page.click('[data-testid="menu-settings"]')
    await this.waitForPageLoad()
  }

  async clickLogout(): Promise<void> {
    await this.page.click('[data-testid="menu-logout"]')
    await this.page.waitForLoadState('networkidle')
  }

  async getBreadcrumbs(): Promise<string[]> {
    const breadcrumbs = await this.page.evaluate((selector: string) => {
      const items = Array.from(document.querySelectorAll(`${selector} li`))
      return items.map(item => item.textContent?.trim() || '')
    }, this.breadcrumbsSelector)

    return breadcrumbs.filter(b => b.length > 0)
  }

  async isMenuItemActive(menuId: string): Promise<boolean> {
    const element = await this.page.querySelector(`[data-testid="menu-${menuId}"]`)
    if (!element) return false

    const classes = await element.getAttribute('class') || ''
    return classes.includes('active') || classes.includes('selected')
  }

  async isSidebarVisible(): Promise<boolean> {
    return await this.page.isVisible(this.sidebarSelector)
  }

  async toggleSidebar(): Promise<void> {
    await this.page.click('[data-testid="sidebar-toggle"]')
    await this.page.waitForTimeout(300)
  }

  async searchGlobal(query: string): Promise<void> {
    await this.page.click('[data-testid="global-search-trigger"]')
    await this.page.fill('[data-testid="global-search-input"]', query)
    await this.page.press('[data-testid="global-search-input"]', 'Enter')
    await this.waitForPageLoad()
  }

  async getCurrentPath(): Promise<string> {
    return this.page.url()
  }

  async goBack(): Promise<void> {
    await this.page.goBack()
    await this.waitForPageLoad()
  }

  async goForward(): Promise<void> {
    await this.page.goForward()
    await this.waitForPageLoad()
  }
}

/**
 * Common Page Object
 * Handles dialogs, modals, forms, and other common UI components
 */
export class CommonPage {
  private page: Page

  constructor(page: Page) {
    this.page = page
  }

  // Dialog/Modal operations
  async isDialogVisible(dialogTestId?: string): Promise<boolean> {
    const selector = dialogTestId || '[data-testid^="dialog-"], [role="dialog"]'
    return await this.page.isVisible(selector)
  }

  async closeDialog(dialogTestId?: string): Promise<void> {
    const closeButton = dialogTestId
      ? `${dialogTestId} [data-testid="close-button"]`
      : '[data-testid^="dialog-"] [data-testid="close-button"], [role="dialog"] [data-testid="close-button"]'

    if (await this.page.isVisible(closeButton)) {
      await this.page.click(closeButton)
      await this.page.waitForTimeout(300)
    } else {
      // Try pressing Escape
      await this.page.press('body', 'Escape')
      await this.page.waitForTimeout(300)
    }
  }

  async confirmAction(confirmTestId: string = 'confirm-button'): Promise<void> {
    await this.page.click(`[data-testid="${confirmTestId}"]`)
    await this.page.waitForTimeout(500)
  }

  async cancelAction(cancelTestId: string = 'cancel-button'): Promise<void> {
    await this.page.click(`[data-testid="${cancelTestId}"]`)
    await this.page.waitForTimeout(300)
  }

  // Toast/Notification operations
  async getToastMessage(): Promise<string | null> {
    const toast = await this.page.locator('[data-testid="toast"], .toast, [role="alert"]').first()
    if (await toast.isVisible()) {
      return await toast.textContent()
    }
    return null
  }

  async waitForToast(message?: string, timeout = 5000): Promise<void> {
    const toastSelector = message
      ? `[data-testid="toast"]:has-text("${message}")`
      : '[data-testid="toast"], .toast, [role="alert"]'

    await this.page.waitForSelector(toastSelector, { timeout })
  }

  async dismissToast(): Promise<void> {
    const closeButtons = [
      '[data-testid="toast"] [data-testid="close-button"]',
      '.toast [data-testid="close-button"]',
      '[role="alert"] [data-testid="close-button"]'
    ]

    for (const selector of closeButtons) {
      if (await this.page.isVisible(selector)) {
        await this.page.click(selector)
        break
      }
    }
  }

  // Form operations
  async fillForm(formData: Record<string, any>): Promise<void> {
    for (const [field, value] of Object.entries(formData)) {
      const input = this.page.locator(`[data-testid="${field}"], [name="${field}"]`)

      if (await input.isVisible()) {
        const tagName = await input.evaluate(el => el.tagName.toLowerCase())

        if (tagName === 'select') {
          await input.selectOption(value as string)
        } else if (tagName === 'input' && await input.getAttribute('type') === 'checkbox') {
          if (value) {
            await input.check()
          } else {
            await input.uncheck()
          }
        } else {
          await input.fill(value as string)
        }
      }
    }
  }

  async getFormData(): Promise<Record<string, any>> {
    const formData: Record<string, any> = {}

    const inputs = await this.page.$$eval('input, select, textarea', elements => {
      return elements.map(el => {
        const testId = el.getAttribute('data-testid')
        const name = el.getAttribute('name')
        const key = testId || name
        if (!key) return null

        if (el.tagName.toLowerCase() === 'select') {
          return { key, value: (el as HTMLSelectElement).value }
        } else if (el.getAttribute('type') === 'checkbox') {
          return { key, value: (el as HTMLInputElement).checked }
        } else {
          return { key, value: (el as HTMLInputElement).value }
        }
      }).filter(item => item !== null)
    })

    inputs.forEach(({ key, value }) => {
      formData[key] = value
    })

    return formData
  }

  // Validation error operations
  async getValidationErrors(): Promise<string[]> {
    const errorElements = await this.page.$$eval('[data-testid$="-error"], .error, .validation-error',
      elements => elements.map(el => el.textContent?.trim() || '')
    )
    return errorElements.filter(e => e.length > 0)
  }

  async isFieldInvalid(fieldTestId: string): Promise<boolean> {
    const field = this.page.locator(`[data-testid="${fieldTestId}"]`)
    const error = this.page.locator(`[data-testid="${fieldTestId}-error"]`)

    const fieldClasses = await field.getAttribute('class') || ''
    const hasErrorClass = fieldClasses.includes('error') || fieldClasses.includes('invalid')

    return hasErrorClass || await error.isVisible()
  }

  // Table operations
  async getTableData(tableTestId: string): Promise<any[]> {
    const rows = await this.page.$$eval(`${tableTestId} tbody tr`, rows => {
      return Array.from(rows).map(row => {
        const cells = Array.from(row.querySelectorAll('td'))
        return {
          id: row.getAttribute('data-id'),
          cells: cells.map(cell => cell.textContent?.trim() || '')
        }
      })
    })

    return rows
  }

  async clickTableRow(tableTestId: string, rowIndex: number): Promise<void> {
    const row = this.page.locator(`${tableTestId} tbody tr`).nth(rowIndex)
    await row.click()
  }

  async selectTableRow(tableTestId: string, rowIndex: number): Promise<void> {
    const checkbox = this.page.locator(`${tableTestId} tbody tr`).nth(rowIndex).locator('input[type="checkbox"]')
    await checkbox.check()
  }

  // Pagination operations
  async goToPage(pageNumber: number): Promise<void> {
    await this.page.click(`[data-testid="page-${pageNumber}"]`)
    await this.page.waitForTimeout(500)
  }

  async clickNextPage(): Promise<void> {
    await this.page.click('[data-testid="next-page"]')
    await this.page.waitForTimeout(500)
  }

  async clickPreviousPage(): Promise<void> {
    await this.page.click('[data-testid="prev-page"]')
    await this.page.waitForTimeout(500)
  }

  async getCurrentPage(): Promise<number> {
    const activePage = await this.page.locator('[data-testid*="page-"].active, [data-testid*="page-"].current').textContent()
    return activePage ? parseInt(activePage) : 1
  }

  // Loading states
  async waitForLoadingToComplete(selector: string = '[data-testid="loading"], .loading'): Promise<void> {
    await this.page.waitForSelector(selector, { state: 'hidden' })
  }

  async isLoadingVisible(): Promise<boolean> {
    return await this.page.isVisible('[data-testid="loading"], .loading, .spinner')
  }

  // Dropdown operations
  async selectFromDropdown(dropdownTestId: string, value: string): Promise<void> {
    await this.page.selectOption(`[data-testid="${dropdownTestId}"]`, value)
  }

  async openDropdown(dropdownTestId: string): Promise<void> {
    await this.page.click(`[data-testid="${dropdownTestId}"]`)
    await this.page.waitForTimeout(300)
  }

  async selectDropdownOption(optionText: string): Promise<void> {
    await this.page.click(`[data-testid="option-${optionText.replace(/\s+/g, '-').toLowerCase()}"]`)
  }

  // Date picker operations
  async selectDate(dateTestId: string, date: string): Promise<void> {
    await this.page.fill(`[data-testid="${dateTestId}"]`, date)
    await this.page.press(`[data-testid="${dateTestId}"]`, 'Enter')
  }

  // File upload operations
  async uploadFile(inputTestId: string, filePath: string): Promise<void> {
    await this.page.setInputFiles(`[data-testid="${inputTestId}"]`, filePath)
  }

  // Wait operations
  async waitForNetworkIdle(): Promise<void> {
    await this.page.waitForLoadState('networkidle')
  }

  async waitForResponse(urlPattern: string | RegExp, timeout = 30000): Promise<void> {
    await this.page.waitForResponse(urlPattern, { timeout })
  }
}
