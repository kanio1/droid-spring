/**
 * Network Mocking Helper Utilities
 *
 * Provides utilities for mocking network requests
 * - API mocking
 * - Response interception
 * - Network throttling
 * - Request/response logging
 */

import { type Page, type Route, type Request } from '@playwright/test'

export interface MockResponse {
  status: number
  headers?: Record<string, string>
  body?: any
  contentType?: string
}

export interface MockRequest {
  urlPattern: string | RegExp
  method?: string
  response: MockResponse
}

export class NetworkHelper {
  /**
   * Mock API requests
   */
  static async mockApiRequests(
    page: Page,
    mocks: MockRequest[]
  ): Promise<() => void> {
    const routes: Route[] = []

    for (const mock of mocks) {
      const route = await page.route(mock.urlPattern, async (route) => {
        // Check method if specified
        if (mock.method && route.request().method() !== mock.method) {
          await route.continue()
          return
        }

        // Fulfill with mock response
        await route.fulfill({
          status: mock.response.status,
          headers: mock.response.headers,
          contentType: mock.response.contentType || 'application/json',
          body: mock.response.body
            ? JSON.stringify(mock.response.body)
            : undefined
        })
      })
      routes.push(route)
    }

    // Return cleanup function
    return () => {
      routes.forEach(route => route.abort())
    }
  }

  /**
   * Mock single request
   */
  static async mockRequest(
    page: Page,
    urlPattern: string | RegExp,
    response: MockResponse
  ): Promise<() => void> {
    const cleanup = await this.mockApiRequests(page, [{
      urlPattern,
      response
    }])

    return cleanup
  }

  /**
   * Mock 404 for API calls
   */
  static async mockNotFound(page: Page, urlPattern: string | RegExp): Promise<() => void> {
    return await this.mockRequest(page, urlPattern, {
      status: 404,
      body: { error: 'Not found' }
    })
  }

  /**
   * Mock server error
   */
  static async mockServerError(
    page: Page,
    urlPattern: string | RegExp
  ): Promise<() => void> {
    return await this.mockRequest(page, urlPattern, {
      status: 500,
      body: { error: 'Internal server error' }
    })
  }

  /**
   * Mock successful response with data
   */
  static async mockSuccess(
    page: Page,
    urlPattern: string | RegExp,
    data: any
  ): Promise<() => void> {
    return await this.mockRequest(page, urlPattern, {
      status: 200,
      body: data
    })
  }

  /**
   * Add network delay
   */
  static async addNetworkDelay(
    page: Page,
    delay: number
  ): Promise<() => void> {
    const route = await page.route('**/*', (route) => {
      setTimeout(() => route.continue(), delay)
    })

    return () => route.abort()
  }

  /**
   * Log all network requests
   */
  static async logNetworkRequests(
    page: Page,
    filter?: (request: Request) => boolean
  ): Promise<() => void> {
    const requests: Request[] = []

    const handler = (request: Request) => {
      if (!filter || filter(request)) {
        requests.push(request)
        console.log(`[Network] ${request.method()} ${request.url()}`)
      }
    }

    page.on('request', handler)

    return () => {
      page.off('request', handler)
    }
  }

  /**
   * Wait for specific API call
   */
  static async waitForApiCall(
    page: Page,
    urlPattern: string | RegExp,
    options?: {
      method?: string
      timeout?: number
    }
  ): Promise<Request> {
    const { method, timeout = 5000 } = options || {}

    return await page.waitForRequest((request) => {
      const urlMatch = typeof urlPattern === 'string'
        ? request.url().includes(urlPattern)
        : urlPattern.test(request.url())

      const methodMatch = !method || request.method() === method

      return urlMatch && methodMatch
    }, { timeout })
  }

  /**
   * Simulate slow network
   */
  static async simulateSlowNetwork(
    page: Page
  ): Promise<() => void> {
    return await this.addNetworkDelay(page, 2000)
  }

  /**
   * Simulate offline mode
   */
  static async simulateOffline(page: Page): Promise<void> {
    await page.context().setOffline(true)
  }

  /**
   * Simulate online mode
   */
  static async simulateOnline(page: Page): Promise<void> {
    await page.context().setOffline(false)
  }

  /**
   * Get all requests
   */
  static async getRequests(page: Page): Promise<Request[]> {
    return new Promise((resolve) => {
      const requests: Request[] = []
      page.on('request', (request) => requests.push(request))
      setTimeout(() => resolve(requests), 100)
    })
  }
}
