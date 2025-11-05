/**
 * Custom Playwright Matchers
 *
 * Provides domain-specific matchers for BSS testing
 * Extends expect() with business-specific assertions
 *
 * Usage:
 * ```typescript
 * await expect(page).toHaveActiveSubscription()
 * await expect(invoiceRow).toBePaid()
 * await expect(customerCard).toHaveStatus('active')
 * ```
 */

import { type Page, type Locator, expect } from '@playwright/test'
import type { Customer, Invoice, Order, Payment, Subscription } from '../../../app/schemas'

/**
 * Matcher: Check if customer has specific status
 */
export async function toHaveCustomerStatus(
  this: any,
  locator: Locator,
  expectedStatus: string
) {
  const actualStatus = await locator.getAttribute('data-status')
  const pass = actualStatus === expectedStatus

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected customer status to be "${expectedStatus}", but got "${actualStatus}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if subscription is active
 */
export async function toHaveActiveSubscription(this: any, page: Page) {
  const subscriptionElement = page.locator('[data-testid="subscription-status"]')
  const status = await subscriptionElement.textContent()

  const pass = status?.toLowerCase().includes('active')

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected subscription to be active, but status is "${status}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if invoice is paid
 */
export async function toBePaidInvoice(this: any, locator: Locator) {
  const statusElement = locator.locator('[data-testid="invoice-status"]')
  const status = await statusElement.textContent()

  const pass = status?.toLowerCase() === 'paid' || status?.toLowerCase().includes('completed')

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected invoice to be paid, but status is "${status}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if payment is successful
 */
export async function toHaveSuccessfulPayment(this: any, locator: Locator) {
  const statusElement = locator.locator('[data-testid="payment-status"]')
  const status = await statusElement.textContent()

  const pass = status?.toLowerCase().includes('completed') ||
               status?.toLowerCase().includes('succeeded')

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected payment to be successful, but status is "${status}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if order is delivered
 */
export async function toBeDelivered(this: any, locator: Locator) {
  const statusElement = locator.locator('[data-testid="order-status"]')
  const status = await statusElement.textContent()

  const pass = status?.toLowerCase().includes('delivered')

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected order to be delivered, but status is "${status}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if element has loading state
 */
export async function toBeLoading(this: any, locator: Locator) {
  const isVisible = await locator.isVisible()
  const hasSpinner = await locator.locator('spinner, .spinner, [role="progressbar"]').count() > 0
  const isDisabled = await locator.isDisabled()

  const pass = isVisible && (hasSpinner || isDisabled)

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected element to be in loading state, but it's ${isVisible ? 'visible' : 'hidden'} and ${isDisabled ? 'disabled' : 'enabled'}`,
      pass
    )
  }
}

/**
 * Matcher: Check if customer data matches expected object
 */
export async function toMatchCustomerData(
  this: any,
  page: Page,
  expectedCustomer: Customer
) {
  const actualName = await page.locator('[data-testid="customer-name"]').textContent()
  const actualEmail = await page.locator('[data-testid="customer-email"]').textContent()
  const actualPhone = await page.locator('[data-testid="customer-phone"]').textContent()

  const pass =
    actualName === `${expectedCustomer.firstName} ${expectedCustomer.lastName}` &&
    actualEmail === expectedCustomer.email &&
    actualPhone === expectedCustomer.phone

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected customer data to match, but got:
        Name: ${actualName} (expected: ${expectedCustomer.firstName} ${expectedCustomer.lastName})
        Email: ${actualEmail} (expected: ${expectedCustomer.email})
        Phone: ${actualPhone} (expected: ${expectedCustomer.phone})`,
      pass
    )
  }
}

/**
 * Matcher: Check if page has no validation errors
 */
export async function toHaveNoValidationErrors(this: any, page: Page) {
  const errorElements = page.locator('.error, .validation-error, [data-testid="error"]')
  const count = await errorElements.count()

  const pass = count === 0

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected no validation errors, but found ${count} error(s)`,
      pass
    )
  }
}

/**
 * Matcher: Check if element contains formatted currency
 */
export async function toHaveCurrencyFormat(
  this: any,
  locator: Locator,
  expectedAmount: number,
  currency: string = 'USD'
) {
  const text = await locator.textContent()
  // Simple regex to match currency format
  const currencyPattern = new RegExp(`\\${currency}\\s?${expectedAmount.toFixed(2)}`)
  const pass = currencyPattern.test(text || '')

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected currency format "${currency} ${expectedAmount.toFixed(2)}", but got "${text}"`,
      pass
    )
  }
}

/**
 * Register all custom matchers
 */
export function registerCustomMatchers() {
  expect.extend({
    toHaveCustomerStatus,
    toHaveActiveSubscription,
    toBePaidInvoice,
    toHaveSuccessfulPayment,
    toBeDelivered,
    toBeLoading,
    toMatchCustomerData,
    toHaveNoValidationErrors,
    toHaveCurrencyFormat
  })
}

declare global {
  namespace PlaywrightTest {
    interface Matchers<R> {
      toHaveCustomerStatus(expectedStatus: string): Promise<R>
      toBePaidInvoice(): Promise<R>
      toHaveSuccessfulPayment(): Promise<R>
      toBeDelivered(): Promise<R>
      toBeLoading(): Promise<R>
      toHaveNoValidationErrors(): Promise<R>
      toHaveCurrencyFormat(expectedAmount: number, currency?: string): Promise<R>
    }
  }
}
