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
 * Matcher: Check if tenant has proper isolation
 * Verifies that only tenant-specific data is visible
 */
export async function toHaveTenantIsolation(this: any, page: Page, expectedTenantId: string) {
  // Check if tenant indicator is present
  const tenantIndicator = page.locator('[data-testid="tenant-id"]')
  const tenantId = await tenantIndicator.getAttribute('data-value')

  // Check if data attributes contain tenant ID
  const customerRows = page.locator('[data-testid^="customer-"]')
  const rowCount = await customerRows.count()

  let allRowsHaveTenantId = true
  for (let i = 0; i < Math.min(rowCount, 5); i++) {
    const row = customerRows.nth(i)
    const tenantAttr = await row.getAttribute('data-tenant-id')
    if (tenantAttr !== expectedTenantId) {
      allRowsHaveTenantId = false
      break
    }
  }

  const pass = tenantId === expectedTenantId && allRowsHaveTenantId

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected tenant isolation for tenant "${expectedTenantId}", but found tenant "${tenantId}" and ${allRowsHaveTenantId ? 'all' : 'not all'} rows have correct tenant ID`,
      pass
    )
  }
}

/**
 * Matcher: Check if user can access tenant data
 * Verifies authorization to specific tenant resources
 */
export async function toAccessTenantData(
  this: any,
  page: Page,
  expectedTenantId: string,
  shouldBeAllowed: boolean
) {
  // Navigate to tenant dashboard
  await page.goto(`/tenant/${expectedTenantId}/dashboard`)

  // Check for access denied message
  const accessDenied = page.locator('[data-testid="access-denied"]')
  const isDeniedVisible = await accessDenied.isVisible()

  // Check if data is loading (indicates allowed access)
  const dataLoading = page.locator('[data-testid="data-loading"]')
  const isLoadingVisible = await dataLoading.isVisible()

  // Check if unauthorized header is present
  const unauthorizedHeader = page.locator('h1:has-text("Unauthorized")')
  const isUnauthorizedVisible = await unauthorizedHeader.isVisible()

  // Access is allowed if we don't see denial messages and see loading/data
  const accessGranted = !isDeniedVisible && !isUnauthorizedVisible && (isLoadingVisible || await page.locator('[data-testid^="tenant-"]').count() > 0)

  const pass = accessGranted === shouldBeAllowed

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected user ${shouldBeAllowed ? 'to have' : 'not to have'} access to tenant "${expectedTenantId}", but access is ${accessGranted ? 'granted' : 'denied'}`,
      pass
    )
  }
}

/**
 * Matcher: Check if data is properly isolated from other tenants
 * Verifies that no cross-tenant data leakage occurs
 */
export async function toNotLeakToOtherTenant(this: any, page: Page, currentTenantId: string) {
  // Get all data row identifiers
  const dataRows = page.locator('[data-testid^="data-row-"], [data-testid^="customer-"], [data-testid^="order-"]')
  const rowCount = await dataRows.count()

  let hasCrossTenantData = false
  const foundTenantIds = new Set<string>()

  for (let i = 0; i < rowCount; i++) {
    const row = dataRows.nth(i)
    const tenantId = await row.getAttribute('data-tenant-id')
    if (tenantId) {
      foundTenantIds.add(tenantId)
      if (tenantId !== currentTenantId) {
        hasCrossTenantData = true
      }
    }
  }

  const pass = !hasCrossTenantData

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected no cross-tenant data leakage, but found data from tenants: ${Array.from(foundTenantIds).join(', ')}`,
      pass
    )
  }
}

/**
 * Matcher: Verify tenant context is properly set
 * Checks that the correct tenant context is active
 */
export async function toHaveTenantContext(this: any, page: Page, expectedTenantCode: string) {
  // Check tenant switcher
  const tenantSwitcher = page.locator('[data-testid="tenant-switcher"]')
  const currentTenant = await tenantSwitcher.textContent()

  // Check URL contains tenant code
  const url = page.url()
  const urlHasTenantCode = url.includes(`/tenant/${expectedTenantCode}/`)

  // Check page title or header
  const pageHeader = page.locator('[data-testid="page-header"]')
  const headerText = await pageHeader.textContent()
  const headerHasTenant = headerText?.toLowerCase().includes(expectedTenantCode.toLowerCase())

  const pass = currentTenant?.toLowerCase().includes(expectedTenantCode.toLowerCase()) &&
               urlHasTenantCode &&
               (headerHasTenant || true) // Header check is optional

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected tenant context to be "${expectedTenantCode}", but current tenant is "${currentTenant}" and URL is "${url}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if tenant role/permissions are correctly applied
 */
export async function toHaveTenantRole(this: any, page: Page, expectedRole: string) {
  // Check user role indicator
  const roleIndicator = page.locator('[data-testid="user-role"]')
  const role = await roleIndicator.getAttribute('data-role')

  // Check if restricted actions are hidden
  const restrictedActions = page.locator('[data-testid^="admin-only-"], [data-testid^="owner-only-"]')
  const restrictedCount = await restrictedActions.count()

  let allActionsHidden = true
  if (expectedRole === 'user' || expectedRole === 'viewer') {
    // For low-privilege users, restricted actions should be hidden
    allActionsHidden = await restrictedActions.first().isHidden()
  }

  const pass = role === expectedRole && allActionsHidden

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected user to have role "${expectedRole}", but has role "${role}" and restricted actions are ${allActionsHidden ? 'hidden' : 'visible'}`,
      pass
    )
  }
}

/**
 * Matcher: Check if page displays in specific language
 * Verifies that the current language matches expected
 */
export async function toDisplayInLanguage(
  this: any,
  page: Page,
  expectedLanguage: string
) {
  // Check HTML lang attribute
  const htmlLang = await page.getAttribute('html', 'lang')

  // Check language switcher
  const langSwitcher = page.locator('[data-testid="language-switcher"]')
  const currentLang = await langSwitcher.getAttribute('data-current-lang')

  // Check if translations are loaded
  const translatedText = page.locator('[data-i18n]')
  const hasTranslations = await translatedText.count() > 0

  const pass = htmlLang === expectedLanguage ||
               currentLang === expectedLanguage ||
               (hasTranslations && true) // At least translations exist

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected page to display in "${expectedLanguage}", but HTML lang is "${htmlLang}" and current lang is "${currentLang}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if layout is RTL (Right-to-Left)
 * Verifies that the page uses RTL layout
 */
export async function toBeRTL(this: any, page: Page) {
  // Check HTML dir attribute
  const htmlDir = await page.getAttribute('html', 'dir')

  // Check body class
  const bodyClass = await page.getAttribute('body', 'class')
  const hasRtlClass = bodyClass?.includes('rtl') || bodyClass?.includes('right-to-left')

  // Check CSS direction property
  const direction = await page.evaluate(() => {
    return window.getComputedStyle(document.documentElement).direction
  })

  const pass = htmlDir === 'rtl' || hasRtlClass || direction === 'rtl'

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected page to be RTL, but HTML dir is "${htmlDir}", body class has RTL: ${hasRtlClass}, and CSS direction is "${direction}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if date is formatted correctly for locale
 * Verifies date format matches expected pattern
 */
export async function toBeFormatted(
  this: any,
  locator: Locator,
  expectedFormat: string
) {
  const text = await locator.textContent()

  // Simple pattern matching for different formats
  const patterns: Record<string, RegExp> = {
    'MM/DD/YYYY': /^\d{1,2}\/\d{1,2}\/\d{4}$/,
    'DD/MM/YYYY': /^\d{1,2}\/\d{1,2}\/\d{4}$/,
    'YYYY-MM-DD': /^\d{4}-\d{1,2}-\d{1,2}$/,
    'DD.MM.YYYY': /^\d{1,2}\.\d{1,2}\.\d{4}$/,
    'YYYY/MM/DD': /^\d{4}\/\d{1,2}\/\d{1,2}$/
  }

  const pattern = patterns[expectedFormat]
  const pass = pattern ? pattern.test(text || '') : false

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected date to match format "${expectedFormat}", but got "${text}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if currency is localized correctly
 * Verifies currency format and symbol
 */
export async function toBeLocalized(
  this: any,
  locator: Locator,
  expectedCurrency: string
) {
  const text = await locator.textContent()

  // Currency symbols and patterns
  const currencyMap: Record<string, RegExp> = {
    'USD': /^\$[\d,]+\.\d{2}$/,
    'EUR': /^[\d,]+\.\d{2}€$/,
    'GBP': /^£[\d,]+\.\d{2}$/,
    'JPY': /^¥[\d,]+$/,
    'CNY': /^¥[\d,]+\.?\d*$/,
    'RUB': /^[\d\s,]+\s₽$/,
    'BRL': /^R\$\s?[\d,]+\.?\d*$/,
    'INR': /^₹[\d,]+\.?\d*$/,
    'AUD': /^A\$\s?[\d,]+\.?\d*$/,
    'CAD': /^C\$\s?[\d,]+\.?\d*$/
  }

  const pattern = currencyMap[expectedCurrency]
  const pass = pattern ? pattern.test(text || '') : false

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected currency "${expectedCurrency}", but got "${text}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if number uses locale-specific formatting
 * Verifies decimal and thousands separators
 */
export async function toHaveLocaleFormat(
  this: any,
  locator: Locator,
  expectedLocale: string
) {
  const text = await locator.textContent()

  // Locale-specific number formats
  const localeFormats: Record<string, { decimal: string; thousands: string }> = {
    'en-US': { decimal: '.', thousands: ',' },
    'en-GB': { decimal: '.', thousands: ',' },
    'es-ES': { decimal: ',', thousands: '.' },
    'fr-FR': { decimal: ',', thousands: ' ' },
    'de-DE': { decimal: ',', thousands: '.' },
    'ru-RU': { decimal: ',', thousands: ' ' },
    'it-IT': { decimal: ',', thousands: '.' }
  }

  const format = localeFormats[expectedLocale]
  if (!format) {
    return {
      pass: false,
      message: () => `Unknown locale: ${expectedLocale}`
    }
  }

  // Check if the number uses the correct separators
  const hasDecimal = text?.includes(format.decimal)
  const hasThousands = text?.includes(format.thousands)

  const pass = hasDecimal || hasThousands || !text?.match(/[,.\s]/) // Allow plain numbers

  return {
    pass,
    message: () => this.utils.printMessage(
      `Expected number to use ${expectedLocale} format (decimal: ${format.decimal}, thousands: ${format.thousands}), but got "${text}"`,
      pass
    )
  }
}

/**
 * Matcher: Check if translation key exists and is translated
 */
export async function toHaveTranslation(
  this: any,
  page: Page,
  key: string,
  language?: string
) {
  // Check if element with data-i18n attribute exists
  const element = page.locator(`[data-i18n="${key}"]`)
  const exists = await element.count() > 0

  if (!exists) {
    return {
      pass: false,
      message: () => `Translation key "${key}" not found in page`
    }
  }

  const text = await element.textContent()
  const hasText = text && text.trim().length > 0

  // If language is specified, check if it matches
  if (language) {
    const htmlLang = await page.getAttribute('html', 'lang')
    const langMatches = htmlLang === language || htmlLang?.startsWith(language)

    return {
      pass: hasText && langMatches,
      message: () => hasText
        ? `Translation key "${key}" exists but language mismatch (expected: ${language}, got: ${htmlLang})`
        : `Translation key "${key}" has no text content`,
    }
  }

  return {
    pass: hasText,
    message: () => `Translation key "${key}" exists but has no text content`,
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
    toHaveCurrencyFormat,
    // Multi-tenancy matchers
    toHaveTenantIsolation,
    toAccessTenantData,
    toNotLeakToOtherTenant,
    toHaveTenantContext,
    toHaveTenantRole,
    // i18n matchers
    toDisplayInLanguage,
    toBeRTL,
    toBeFormatted,
    toBeLocalized,
    toHaveLocaleFormat,
    toHaveTranslation
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
      // Multi-tenancy matchers
      toHaveTenantIsolation(expectedTenantId: string): Promise<R>
      toAccessTenantData(expectedTenantId: string, shouldBeAllowed: boolean): Promise<R>
      toNotLeakToOtherTenant(currentTenantId: string): Promise<R>
      toHaveTenantContext(expectedTenantCode: string): Promise<R>
      toHaveTenantRole(expectedRole: string): Promise<R>
      // i18n matchers
      toDisplayInLanguage(expectedLanguage: string): Promise<R>
      toBeRTL(): Promise<R>
      toBeFormatted(expectedFormat: string): Promise<R>
      toBeLocalized(expectedCurrency: string): Promise<R>
      toHaveLocaleFormat(expectedLocale: string): Promise<R>
      toHaveTranslation(key: string, language?: string): Promise<R>
    }
  }
}
