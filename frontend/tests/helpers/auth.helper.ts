/**
 * Authentication Helper Utilities
 *
 * Provides utilities for handling authentication in tests
 * - Login helper
 * - Token management
 * - Session handling
 */

import { type Page, type BrowserContext } from '@playwright/test'

export interface AuthConfig {
  username: string
  password: string
  loginUrl?: string
  dashboardUrl?: string
}

export class AuthHelper {
  /**
   * Login user using provided credentials
   */
  static async login(
    page: Page,
    config: AuthConfig
  ): Promise<void> {
    const loginUrl = config.loginUrl || '/login'
    const dashboardUrl = config.dashboardUrl || '/dashboard'

    // Navigate to login page
    await page.goto(loginUrl)
    await page.waitForLoadState('networkidle')

    // Fill credentials
    await page.fill(
      '[data-testid="username-input"], input[name="username"], #username',
      config.username
    )
    await page.fill(
      '[data-testid="password-input"], input[name="password"], #password',
      config.password
    )

    // Submit login
    await page.click('[data-testid="login-button"], button[type="submit"]')

    // Wait for redirect
    await page.waitForURL(dashboardUrl, { timeout: 10000 })
    await page.waitForLoadState('networkidle')
  }

  /**
   * Login with OIDC flow
   */
  static async loginWithOidc(
    page: Page,
    config: AuthConfig
  ): Promise<void> {
    const loginUrl = config.loginUrl || '/login'

    // Navigate to login
    await page.goto(loginUrl)
    await page.waitForLoadState('networkidle')

    // Click OIDC login button
    await page.click(
      '[data-testid="oidc-login"], .oidc-login, button:has-text("Sign in with Keycloak")'
    )

    // Should redirect to Keycloak
    await page.waitForURL(
      /.*keycloak.*\/realms.*\/protocol\/openid-connect\/auth.*/,
      { timeout: 10000 }
    )

    // Login on Keycloak
    await page.fill('#username', config.username)
    await page.fill('#password', config.password)
    await page.click('#kc-login')

    // Should return to app
    await page.waitForURL('/dashboard', { timeout: 10000 })
    await page.waitForLoadState('networkidle')
  }

  /**
   * Logout user
   */
  static async logout(page: Page): Promise<void> {
    // Click logout button
    await page.click(
      '[data-testid="logout-button"], a[href="/logout"], button:has-text("Logout")'
    )

    // Wait for redirect to login
    await page.waitForURL(/.*\/login.*/, { timeout: 10000 })
    await page.waitForLoadState('networkidle')
  }

  /**
   * Check if user is logged in
   */
  static async isLoggedIn(page: Page): Promise<boolean> {
    // Check for user menu or dashboard elements
    const userMenuVisible = await page.locator(
      '[data-testid="user-menu"], .user-menu, .user-avatar'
    ).isVisible()

    // Check for logout button
    const logoutButtonVisible = await page.locator(
      '[data-testid="logout-button"], a[href="/logout"]'
    ).isVisible()

    return userMenuVisible && logoutButtonVisible
  }

  /**
   * Ensure user is logged in, login if not
   */
  static async ensureLoggedIn(
    page: Page,
    config: AuthConfig
  ): Promise<void> {
    const isLoggedIn = await this.isLoggedIn(page)

    if (!isLoggedIn) {
      await this.login(page, config)
    }
  }

  /**
   * Store auth state to reuse in other tests
   */
  static async saveAuthState(
    context: BrowserContext,
    storagePath: string
  ): Promise<void> {
    await context.storageState({ path: storagePath })
  }

  /**
   * Load auth state from file
   */
  static async loadAuthState(
    context: BrowserContext,
    storagePath: string
  ): Promise<void> {
    await context.storageState({ path: storagePath })
  }

  /**
   * Create authenticated context
   */
  static async createAuthenticatedContext(
    browser: import('@playwright/test').Browser,
    storagePath: string
  ): Promise<BrowserContext> {
    return await browser.newContext({
      storageState: storagePath
    })
  }
}
