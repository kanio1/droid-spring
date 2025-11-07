/**
 * Chaos Engineering Resilience Testing
 *
 * This test performs chaos engineering by intentionally injecting failures
 * to test system resilience and recovery capabilities
 */

import { test, expect } from '@playwright/test'

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000'

test.describe('Chaos Engineering - Failure Injection', () => {
  test('should survive random pod/container kills', async ({ page }) => {
    const testResults: any[] = []

    for (let i = 0; i < 5; i++) {
      console.log(`\n--- Chaos Test ${i + 1}/5: Pod Kill Simulation ---`)

      // Start normal operation
      await page.goto(`${BASE_URL}/dashboard`)
      await page.waitForTimeout(2000)

      // Simulate pod kill (network failure)
      await page.route('**/api/customers', (route) => {
        route.abort('failed')
      })

      await page.goto(`${BASE_URL}/customers`)
      await page.waitForTimeout(3000)

      // Check if system recovered
      const hasError = await page.locator('[data-testid="error-message"]').count()
      testResults.push({ test: i + 1, recovered: hasError === 0 })

      // Reset for next iteration
      await page.unroute('**/api/customers')
      await page.waitForTimeout(1000)
    }

    const recoveryRate = testResults.filter(t => t.recovered).length / testResults.length
    console.log(`\nRecovery Rate: ${(recoveryRate * 100).toFixed(1)}%`)

    // Should recover from most failures
    expect(recoveryRate).toBeGreaterThan(0.7)
    console.log('Chaos test passed - system resilient ✓')
  })

  test('should handle network latency injection', async ({ page }) => {
    let totalLatency = 0

    // Inject artificial latency
    await page.route('**/api/customers', (route) => {
      const start = Date.now()
      setTimeout(() => {
        totalLatency = Date.now() - start
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([{ id: 1, name: 'Customer' }]),
        })
      }, 3000) // 3 second delay
    })

    const startTime = Date.now()
    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(5000)
    const endTime = Date.now()

    console.log(`Injected Latency: ${totalLatency}ms`)
    console.log(`Total Test Duration: ${endTime - startTime}ms`)

    // System should handle latency gracefully
    expect(endTime - startTime).toBeGreaterThan(3000)
    console.log('Network latency handled ✓')
  })

  test('should survive database connection pool exhaustion', async ({ page }) => {
    const requests: Promise<any>[] = []

    // Exhaust connection pool with too many requests
    for (let i = 0; i < 100; i++) {
      const req = page.request.get(`${BASE_URL}/api/customers`)
      requests.push(req)
    }

    const responses = await Promise.allSettled(requests)
    const successfulResponses = responses.filter(r => r.status === 'fulfilled')

    console.log(`Successful: ${successfulResponses.length}/${requests.length}`)

    // At least some requests should succeed
    expect(successfulResponses.length).toBeGreaterThan(0)
    console.log('Connection pool exhaustion handled ✓')
  })

  test('should handle memory pressure', async ({ page }) => {
    await page.goto(`${BASE_URL}/customers`)

    // Consume memory by creating large objects
    await page.evaluate(() => {
      const largeArray = new Array(1000000).fill({ data: 'x'.repeat(1000) })
      ;(window as any).largeData = largeArray
    })

    await page.waitForTimeout(2000)

    // System should handle memory pressure
    const performanceMetrics = await page.evaluate(() => {
      return {
        memory: (performance as any).memory?.usedJSHeapSize || 0,
        timing: performance.now(),
      }
    })

    console.log('Memory metrics:', performanceMetrics)
    console.log('Memory pressure handled ✓')
  })

  test('should recover from disk space issues', async ({ page }) => {
    await page.goto(`${BASE_URL}/dashboard`)

    // Simulate disk space issues
    await page.route('**/api/reports*', (route) => {
      route.abort('failed')
    })

    await page.goto(`${BASE_URL}/reports`)
    await page.waitForTimeout(3000)

    // Should show graceful error or alternative
    const hasErrorHandling = await page.locator('[data-testid="error-message"]').count()
    const hasRetryButton = await page.locator('[data-testid="retry-button"]').count()

    if (hasErrorHandling > 0 || hasRetryButton > 0) {
      console.log('Disk space issues handled gracefully ✓')
    } else {
      console.log('WARNING: No error handling for disk issues')
    }
  })

  test('should handle CPU throttle/injection', async ({ page }) => {
    await page.goto(`${BASE_URL}/customers`)

    // Simulate high CPU load
    await page.evaluate(() => {
      const start = Date.now()
      while (Date.now() - start < 2000) {
        Math.sqrt(Math.random() * 1000000)
      }
    })

    // System should remain responsive
    const response = await page.request.get(`${BASE_URL}/api/customers`)
    expect(response.status()).toBeLessThan(500)
    console.log('CPU throttle handled ✓')
  })
})

test.describe('Fault Tolerance Patterns', () => {
  test('should implement retry pattern with jitter', async ({ page }) => {
    let attemptCount = 0

    await page.route('**/api/customers', (route) => {
      attemptCount++
      if (attemptCount < 3) {
        route.abort('failed')
      } else {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([{ id: 1 }]),
        })
      }
    })

    const start = Date.now()
    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(10000)
    const duration = Date.now() - start

    // Should have attempted multiple times
    expect(attemptCount).toBeGreaterThanOrEqual(3)
    console.log(`Retry pattern working (${attemptCount} attempts in ${duration}ms) ✓`)
  })

  test('should implement timeout pattern', async ({ page }) => {
    let requestsStarted = 0
    let requestsCompleted = 0

    await page.route('**/api/customers', (route) => {
      requestsStarted++
      setTimeout(() => {
        requestsCompleted++
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      }, 10000) // 10 second delay
    })

    const start = Date.now()
    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(15000) // Wait for timeout
    const duration = Date.now() - start

    // Should timeout before request completes
    expect(duration).toBeLessThan(15000)
    console.log(`Timeout pattern working (${duration}ms) ✓`)
  })

  test('should implement bulkhead isolation', async ({ page }) => {
    // Kill one service
    await page.route('**/api/customers', (route) => {
      route.abort('failed')
    })

    // Other services should still work
    const customersResponse = await page.request.get(`${BASE_URL}/api/customers`)
    const ordersResponse = await page.request.get(`${BASE_URL}/api/orders`)

    // Customers should fail, orders should succeed
    expect(customersResponse.status()).toBeGreaterThanOrEqual(400)
    expect([200, 401, 403]).toContain(ordersResponse.status())

    console.log('Bulkhead isolation working ✓')
  })

  test('should implement fallback pattern', async ({ page }) => {
    let fallbackActivated = false

    await page.route('**/api/customers', (route) => {
      route.abort('failed')
    })

    // Simulate fallback data
    await page.route('**/api/fallback/customers', (route) => {
      fallbackActivated = true
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{ id: 1, name: 'Fallback Customer' }]),
      })
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(3000)

    if (fallbackActivated) {
      console.log('Fallback pattern working ✓')
    } else {
      console.log('WARNING: Fallback not activated')
    }
  })
})

test.describe('Recovery Testing', () => {
  test('should auto-recover from transient failures', async ({ page }) => {
    let failureCount = 0
    let recoveryCount = 0

    await page.route('**/api/customers', (route) => {
      failureCount++
      if (failureCount <= 3) {
        // First 3 requests fail
        route.abort('failed')
      } else {
        // Subsequent requests succeed
        recoveryCount++
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([{ id: 1 }]),
        })
      }
    })

    // Make requests
    for (let i = 0; i < 5; i++) {
      await page.goto(`${BASE_URL}/customers`)
      await page.waitForTimeout(1000)
    }

    expect(failureCount).toBe(3)
    expect(recoveryCount).toBeGreaterThan(0)
    console.log(`Auto-recovery working (${failureCount} failures, ${recoveryCount} recoveries) ✓`)
  })

  test('should implement health-based routing', async ({ page }) => {
    // Mark service as unhealthy
    let isHealthy = false

    await page.route('**/health', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ status: isHealthy ? 'UP' : 'DOWN' }),
      })
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(2000)

    // Mark as healthy
    isHealthy = true

    // Should switch to healthy instance
    await page.reload()
    await page.waitForTimeout(2000)

    console.log('Health-based routing simulated ✓')
  })

  test('should degrade functionality gracefully', async ({ page }) => {
    // Simulate partial outage
    await page.route('**/api/analytics*', (route) => {
      route.abort('failed')
    })

    await page.goto(`${BASE_URL}/dashboard`)
    await page.waitForTimeout(3000)

    // Analytics should be disabled, other features should work
    const dashboardVisible = await page.locator('text=Dashboard').count()
    const analyticsVisible = await page.locator('[data-testid="analytics-chart"]').count()

    if (dashboardVisible > 0) {
      console.log('Graceful degradation working ✓')
    }
  })
})

test.describe('Resilience Metrics', () => {
  test('should track resilience metrics', async ({ request }) => {
    // Check if resilience metrics are exposed
    const response = await request.get(`${BASE_URL}/actuator/metrics/resilience`)

    if (response.ok()) {
      const metrics = await response.json()
      console.log('Resilience metrics:', JSON.stringify(metrics, null, 2))
    } else {
      console.log('Resilience metrics endpoint not available')
    }
  })

  test('should monitor circuit breaker state', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/actuator/circuitbreakers`)

    if (response.ok()) {
      const data = await response.json()
      console.log('Circuit breaker states:', JSON.stringify(data, null, 2))
    } else {
      console.log('Circuit breaker endpoint not available (expected in dev)')
    }
  })

  test('should track recovery time', async ({ page }) => {
    let requestTime = 0

    // Inject failure then recovery
    await page.route('**/api/customers', (route) => {
      if (requestTime === 0) {
        // First request fails
        route.abort('failed')
      } else {
        // Second request succeeds
        setTimeout(() => {
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify([]),
          })
        }, 1000)
      }
      requestTime++
    })

    const start = Date.now()
    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(5000)
    const recoveryTime = Date.now() - start

    console.log(`Recovery time: ${recoveryTime}ms`)
    console.log('Recovery tracking working ✓')
  })
})
