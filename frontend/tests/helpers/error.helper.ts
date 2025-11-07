/**
 * Error Handling Helper Utilities
 *
 * Provides utilities for handling and asserting errors in tests
 * - Error assertions
 * - Error message validation
 * - Retry mechanisms
 */

export interface ErrorAssertion {
  message?: string | RegExp
  statusCode?: number
  field?: string
}

export class ErrorHelper {
  /**
   * Assert element contains error message
   */
  static async assertErrorMessage(
    page: any,
    selector: string,
    expectedMessage: string | RegExp
  ): Promise<void> {
    const element = page.locator(selector)
    await expect(element).toBeVisible()
    await expect(element).toContainText(expectedMessage)
  }

  /**
   * Assert validation errors
   */
  static async assertValidationErrors(
    page: any,
    errors: Record<string, string>
  ): Promise<void> {
    for (const [field, message] of Object.entries(errors)) {
      const selector = `[data-testid="${field}-error"], .error:has-text("${field}")`
      await this.assertErrorMessage(page, selector, message)
    }
  }

  /**
   * Check for error dialog
   */
  static async hasErrorDialog(page: any): Promise<boolean> {
    const dialog = page.locator('[role="dialog"], .modal, .popup')
    const hasDialog = await dialog.isVisible()

    if (hasDialog) {
      const hasErrorText = await dialog.locator('.error, [data-testid="error-message"]').count() > 0
      return hasErrorText
    }

    return false
  }

  /**
   * Handle error dialog
   */
  static async handleErrorDialog(page: any, action: 'accept' | 'dismiss' = 'accept'): Promise<void> {
    if (action === 'accept') {
      await page.dialog?.accept()
    } else {
      await page.dialog?.dismiss()
    }
  }

  /**
   * Retry operation until success or timeout
   */
  static async retry<T>(
    operation: () => Promise<T>,
    maxAttempts: number = 3,
    delay: number = 1000
  ): Promise<T> {
    let lastError: Error

    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return await operation()
      } catch (error) {
        lastError = error as Error
        if (attempt === maxAttempts) {
          throw lastError
        }
        console.log(`Attempt ${attempt} failed, retrying in ${delay}ms...`)
        await new Promise(resolve => setTimeout(resolve, delay))
      }
    }

    throw lastError!
  }

  /**
   * Wait for element to not be visible (timeout expected)
   */
  static async waitForElementToDisappear(
    page: any,
    selector: string,
    timeout: number = 5000
  ): Promise<void> {
    await expect(page.locator(selector)).toBeHidden({ timeout })
  }

  /**
   * Wait for loading state to complete
   */
  static async waitForLoadingToComplete(
    page: any,
    timeout: number = 10000
  ): Promise<void> {
    // Wait for spinner to appear
    await page.locator('[data-testid="loading-spinner"], .spinner, [role="progressbar"]').waitFor({ timeout: 1000 })

    // Wait for spinner to disappear
    await expect(
      page.locator('[data-testid="loading-spinner"], .spinner, [role="progressbar"]')
    ).toBeHidden({ timeout })
  }

  /**
   * Handle 404 errors
   */
  static async handle404Error(
    page: any,
    expected: boolean = true
  ): Promise<void> {
    if (expected) {
      await expect(page.locator('h1, h2')).toContainText(/404|not found/i, { timeout: 5000 })
    } else {
      await expect(page.locator('h1, h2')).not.toContainText(/404|not found/i)
    }
  }

  /**
   * Handle 500 errors
   */
  static async handle500Error(page: any): Promise<void> {
    await expect(page.locator('h1, h2')).toContainText(/500|server error|internal error/i, { timeout: 5000 })
  }

  /**
   * Assert API error response
   */
  static async assertApiError(
    page: any,
    urlPattern: string,
    errorDetails: ErrorAssertion
  ): Promise<void> {
    const [response] = await Promise.all([
      page.waitForResponse(urlPattern),
      page.reload()
    ])

    expect(response.status()).toBe(errorDetails.statusCode || 400)

    const body = await response.json()
    if (errorDetails.message) {
      expect(body.message || body.error).toMatch(errorDetails.message)
    }
  }

  /**
   * Wait for network idle
   */
  static async waitForNetworkIdle(
    page: any,
    timeout: number = 5000
  ): Promise<void> {
    await page.waitForLoadState('networkidle', { timeout })
  }

  /**
   * Check if page is in error state
   */
  static async isPageInErrorState(page: any): Promise<boolean> {
    // Check for error messages
    const hasErrorMessage = await page.locator('.error, [data-testid="error-message"]').count() > 0

    // Check for 404/500 status
    const has404 = page.url().includes('404')
    const has500 = page.url().includes('500')

    // Check for blank page or no content
    const bodyText = await page.locator('body').textContent()
    const isBlank = !bodyText || bodyText.trim().length === 0

    return hasErrorMessage || has404 || has500 || isBlank
  }

  /**
   * Take screenshot on error
   */
  static async takeScreenshotOnError(
    page: any,
    testName: string
  ): Promise<void> {
    const isError = await this.isPageInErrorState(page)

    if (isError) {
      const screenshotPath = `test-results/screenshots/${testName}-error.png`
      await page.screenshot({ path: screenshotPath, fullPage: true })
      console.log(`Error screenshot saved: ${screenshotPath}`)
    }
  }

  /**
   * Get element error text
   */
  static async getErrorText(page: any, selector: string): Promise<string | null> {
    try {
      const text = await page.locator(selector).textContent()
      return text
    } catch {
      return null
    }
  }

  /**
   * Check for accessibility errors
   */
  static async checkA11yErrors(page: any): Promise<string[]> {
    // This would integrate with axe-core or other a11y tools
    const violations: string[] = []

    // Basic checks can be done without external tools
    const missingAlts = await page.locator('img:not([alt])').count()
    if (missingAlts > 0) {
      violations.push(`Found ${missingAlts} images without alt text`)
    }

    const missingLabels = await page.locator('input:not([aria-label]):not([id])').count()
    if (missingLabels > 0) {
      violations.push(`Found ${missingLabels} inputs without labels`)
    }

    return violations
  }
}
