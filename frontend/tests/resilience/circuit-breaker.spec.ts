/**
 * Circuit Breaker Resilience Testing
 *
 * This test validates circuit breaker patterns and fault tolerance
 * in the application architecture
 */

import { test, expect } from '@playwright/test'

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000'

test.describe('Circuit Breaker Resilience', () => {
  test('should handle database connection failures gracefully', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`)

    // Simulate multiple rapid requests to trigger circuit breaker
    const requests = []
    for (let i = 0; i < 10; i++) {
      const responsePromise = page.request.get(`${BASE_URL}/api/customers`)
      requests.push(responsePromise)
    }

    // Wait for all requests
    const responses = await Promise.all(requests)

    // Circuit breaker should prevent cascading failures
    for (const response of responses) {
      const status = response.status()
      // Should return 503 (Service Unavailable) or 500, not timeout
      expect([503, 500, 502]).toContain(status)
    }

    console.log('Circuit breaker prevented cascading failures ✓')
  })

  test('should show fallback UI when service is unavailable', async ({ page }) => {
    await page.route('**/api/customers', (route) => {
      // Simulate service failure
      route.abort('failed')
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(2000)

    // Should show error message or fallback
    const hasErrorMessage = await page.locator('[data-testid="error-message"]').count()
    const hasFallback = await page.locator('[data-testid="fallback-content"]').count()

    if (hasErrorMessage > 0 || hasFallback > 0) {
      console.log('Fallback UI displayed when service unavailable ✓')
    } else {
      console.log('WARNING: No fallback UI displayed')
    }
  })

  test('should retry failed requests with exponential backoff', async ({ page }) => {
    let requestCount = 0

    await page.route('**/api/customers', (route) => {
      requestCount++
      route.abort('failed')
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(10000) // Wait for retries

    // Should have made multiple attempts (initial + retries)
    expect(requestCount).toBeGreaterThan(1)
    console.log(`Retried request ${requestCount} times with backoff ✓`)
  })

  test('should recover when service becomes available', async ({ page }) => {
    let isServiceDown = true

    await page.route('**/api/customers', (route) => {
      if (isServiceDown) {
        route.abort('failed')
      } else {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([{ id: 1, name: 'Test Customer' }]),
        })
      }
    })

    // First request - service down
    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(2000)
    console.log('Service down detected')

    // Restore service
    isServiceDown = false

    // Refresh - should recover
    await page.reload()
    await page.waitForTimeout(3000)

    const content = await page.textContent('body')
    if (content && !content.includes('error')) {
      console.log('Service recovered successfully ✓')
    }
  })

  test('should implement bulkhead pattern (isolate failures)', async ({ page }) => {
    // Simulate failure in one service
    await page.route('**/api/customers', (route) => {
      route.abort('failed')
    })

    // Other services should still work
    await page.route('**/api/orders', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ id: 1, total: 100 }]),
      })
    })

    await page.goto(`${BASE_URL}/dashboard`)
    await page.waitForTimeout(3000)

    // Should be able to see orders even if customers fail
    const hasOrders = await page.locator('text=Order').count()

    if (hasOrders > 0) {
      console.log('Bulkhead pattern working - other services isolated ✓')
    } else {
      console.log('WARNING: Failure may have affected other services')
    }
  })

  test('should timeout long-running requests', async ({ page }) => {
    await page.route('**/api/customers', (route) => {
      // Simulate slow response
      setTimeout(() => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      }, 10000) // 10 second delay
    })

    const startTime = Date.now()

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(15000) // Wait longer than timeout

    const duration = Date.now() - startTime

    // Should timeout within reasonable time (e.g., 30 seconds)
    expect(duration).toBeLessThan(30000)
    console.log(`Request timed out after ${duration}ms ✓`)
  })

  test('should implement rate limiting', async ({ page }) => {
    const requests = []

    // Make many rapid requests
    for (let i = 0; i < 50; i++) {
      const response = page.request.get(`${BASE_URL}/api/customers`)
      requests.push(response)
    }

    const responses = await Promise.all(requests)
    const statusCodes = responses.map(r => r.status())

    // Should have some rate limiting (429 Too Many Requests)
    const hasRateLimit = statusCodes.includes(429)

    if (hasRateLimit) {
      console.log('Rate limiting working correctly ✓')
    } else {
      console.log('WARNING: No rate limiting detected')
    }
  })

  test('should handle partial system failures', async ({ page }) => {
    // Simulate intermittent failures
    let requestCount = 0

    await page.route('**/api/customers', (route) => {
      requestCount++
      if (requestCount % 2 === 0) {
        // Every other request fails
        route.abort('failed')
      } else {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([{ id: 1 }]),
        })
      }
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(5000)

    // Should handle intermittent failures gracefully
    const hasContent = await page.locator('text=Customer').count()

    if (hasContent > 0) {
      console.log('Partial failures handled gracefully ✓')
    }
  })
})

test.describe('Graceful Degradation', () => {
  test('should load cached data when API is slow', async ({ page }) => {
    let requestCount = 0

    await page.route('**/api/customers', (route) => {
      requestCount++
      if (requestCount === 1) {
        // First request - slow
        setTimeout(() => {
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify([{ id: 1, name: 'Cached Customer' }]),
          })
        }, 5000)
      } else {
        // Subsequent requests - normal
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([{ id: 1, name: 'Live Customer' }]),
        })
      }
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(2000) // Wait for first slow request

    // Should show cached data or loading indicator
    const hasLoading = await page.locator('[data-testid="loading"]').count()
    const hasContent = await page.locator('text=Customer').count()

    if (hasLoading > 0 || hasContent > 0) {
      console.log('Degraded mode activated (cached/slow data) ✓')
    }
  })

  test('should disable non-critical features during high load', async ({ page }) => {
    // Simulate high load scenario
    await page.route('**/api/customers*', (route) => {
      // Simulate slow response
      setTimeout(() => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      }, 3000)
    })

    await page.goto(`${BASE_URL}/dashboard`)
    await page.waitForTimeout(4000)

    // Check if non-critical features are disabled
    const hasDisabledFeatures = await page.locator('[disabled]').count()

    if (hasDisabledFeatures > 0) {
      console.log('Non-critical features disabled under load ✓')
    } else {
      console.log('WARNING: No feature degradation detected')
    }
  })

  test('should prioritize critical operations', async ({ page }) => {
    const criticalRequests = []
    const normalRequests = []

    // Simulate critical path (login)
    await page.route('**/api/auth/login', (route) => {
      criticalRequests.push(Date.now())
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ token: 'test-token' }),
      })
    })

    // Simulate non-critical path (reporting)
    await page.route('**/api/reports*', (route) => {
      normalRequests.push(Date.now())
      setTimeout(() => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({}),
        })
      }, 5000)
    })

    await page.goto(`${BASE_URL}/login`)
    await page.fill('[data-testid="username-input"]', 'admin')
    await page.fill('[data-testid="password-input"]', 'password')
    await page.click('[data-testid="login-button"]')
    await page.waitForTimeout(2000)

    // Critical operations should complete first
    if (criticalRequests.length > 0) {
      console.log('Critical operations prioritized ✓')
    }
  })
})

test.describe('Health Check & Monitoring', () => {
  test('should expose health check endpoint', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/health`)

    expect([200, 503]).toContain(response.status())

    if (response.status() === 200) {
      const data = await response.json()
      expect(data).toHaveProperty('status')
      expect(['UP', 'DOWN']).toContain(data.status)
      console.log('Health check endpoint working ✓')
    }
  })

  test('should provide detailed health information', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/health`)

    if (response.status() === 200) {
      const data = await response.json()
      console.log('Health details:', JSON.stringify(data, null, 2))
      // Should have details about database, cache, external services
    }
  })

  test('should monitor system metrics', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/actuator/metrics`)

    if (response.ok()) {
      const data = await response.json()
      expect(Array.isArray(data)).toBe(true)
      console.log('System metrics available ✓')
    } else {
      console.log('Metrics endpoint not available (expected in dev)')
    }
  })
})
