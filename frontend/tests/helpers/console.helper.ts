/**
 * Console Logging Helper Utilities
 *
 * Provides utilities for intercepting and analyzing console messages
 * - Log capture
 * - Error detection
 * - Performance logging
 */

import { type Page } from '@playwright/test'

export interface ConsoleMessage {
  type: 'log' | 'info' | 'warning' | 'error' | 'debug'
  text: string
  timestamp: Date
}

export interface ConsoleOptions {
  filterTypes?: string[]
  includeStackTrace?: boolean
}

export class ConsoleHelper {
  private static capturedMessages: ConsoleMessage[] = []

  /**
   * Start capturing console messages
   */
  static startCapture(
    page: Page,
    options: ConsoleOptions = {}
  ): () => void {
    this.capturedMessages = []

    const handler = (msg: any) => {
      const type = msg.type() as ConsoleMessage['type']
      const text = msg.text()

      // Filter by type if specified
      if (options.filterTypes && !options.filterTypes.includes(type)) {
        return
      }

      const message: ConsoleMessage = {
        type,
        text,
        timestamp: new Date()
      }

      this.capturedMessages.push(message)

      // Log to test output
      if (type === 'error') {
        console.error(`[Console Error] ${text}`)
      } else if (type === 'warning') {
        console.warn(`[Console Warning] ${text}`)
      }
    }

    page.on('console', handler)

    // Return cleanup function
    return () => {
      page.off('console', handler)
    }
  }

  /**
   * Get captured messages
   */
  static getMessages(): ConsoleMessage[] {
    return [...this.capturedMessages]
  }

  /**
   * Clear captured messages
   */
  static clearMessages(): void {
    this.capturedMessages = []
  }

  /**
   * Get errors only
   */
  static getErrors(): ConsoleMessage[] {
    return this.capturedMessages.filter(msg => msg.type === 'error')
  }

  /**
   * Get warnings only
   */
  static getWarnings(): ConsoleMessage[] {
    return this.capturedMessages.filter(msg => msg.type === 'warning')
  }

  /**
   * Check if specific text appears in console
   */
  static containsText(text: string): boolean {
    return this.capturedMessages.some(msg => msg.text.includes(text))
  }

  /**
   * Check for JavaScript errors
   */
  static hasJavaScriptErrors(): boolean {
    return this.getErrors().length > 0
  }

  /**
   * Get error details
   */
  static getErrorDetails(): Array<{ message: string; stack?: string }> {
    return this.getErrors().map(msg => {
      // Try to extract stack trace
      const stackMatch = msg.text.match(/at\s+.+/)
      return {
        message: msg.text,
        stack: stackMatch ? stackMatch[0] : undefined
      }
    })
  }

  /**
   * Monitor network errors
   */
  static startNetworkErrorCapture(page: Page): () => void {
    const handler = (response: any) => {
      if (!response.ok()) {
        const status = response.status()
        const url = response.url()
        console.error(`[Network Error] ${status} - ${url}`)
      }
    }

    page.on('response', handler)

    return () => {
      page.off('response', handler)
    }
  }

  /**
   * Monitor page errors
   */
  static startPageErrorCapture(page: Page): () => void {
    const handler = (error: any) => {
      console.error(`[Page Error] ${error.message}`, error.stack)
    }

    page.on('pageerror', handler)

    return () => {
      page.off('pageerror', handler)
    }
  }

  /**
   * Start comprehensive monitoring
   */
  static startMonitoring(
    page: Page,
    options: ConsoleOptions = {}
  ): {
    console: () => void
    network: () => void
    page: () => void
  } {
    const consoleCleanup = this.startCapture(page, options)
    const networkCleanup = this.startNetworkErrorCapture(page)
    const pageCleanup = this.startPageErrorCapture(page)

    return {
      console: consoleCleanup,
      network: networkCleanup,
      page: pageCleanup
    }
  }

  /**
   * Wait for console message
   */
  static async waitForMessage(
    page: Page,
    text: string,
    timeout: number = 5000
  ): Promise<ConsoleMessage | null> {
    return new Promise((resolve) => {
      const startTime = Date.now()
      const checkInterval = 100

      const check = () => {
        const message = this.capturedMessages.find(msg => msg.text.includes(text))
        if (message) {
          resolve(message)
        } else if (Date.now() - startTime > timeout) {
          resolve(null)
        } else {
          setTimeout(check, checkInterval)
        }
      }

      check()
    })
  }

  /**
   * Assert no errors in console
   */
  static assertNoErrors(test: any): void {
    const errors = this.getErrors()
    if (errors.length > 0) {
      const errorMessages = errors.map(e => e.text).join('\n')
      test.fail(`Console errors detected:\n${errorMessages}`)
    }
  }

  /**
   * Print summary
   */
  static printSummary(): void {
    const errors = this.getErrors()
    const warnings = this.getWarnings()

    console.log('\n=== Console Summary ===')
    console.log(`Total messages: ${this.capturedMessages.length}`)
    console.log(`Errors: ${errors.length}`)
    console.log(`Warnings: ${warnings.length}`)

    if (errors.length > 0) {
      console.log('\nErrors:')
      errors.forEach(msg => console.log(`  - ${msg.text}`))
    }

    if (warnings.length > 0) {
      console.log('\nWarnings:')
      warnings.forEach(msg => console.log(`  - ${msg.text}`))
    }

    console.log('====================\n')
  }
}
