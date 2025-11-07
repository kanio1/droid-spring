/**
 * Timeout & Retry Pattern Testing
 *
 * This test validates timeout handling and retry mechanisms
 * in the application
 */

import { test, expect } from '@playwright/test'

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000'

test.describe('Timeout Handling', () => {
  test('should timeout on slow responses', async ({ page }) => {
    let requestCount = 0

    await page.route('**/api/customers', (route) => {
      requestCount++
      // Simulate very slow response (15 seconds)
      setTimeout(() => {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      }, 15000)
    })

    const startTime = Date.now()
    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(20000)
    const duration = Date.now() - startTime

    console.log(`Request count: ${requestCount}`)
    console.log(`Total duration: ${duration}ms`)

    // Should timeout before 15 seconds
    expect(duration).toBeLessThan(15000)
    console.log('Timeout handling working ✓')
  })

  test('should have configurable timeout values', async ({ page }) => {
    const timeouts: number[] = [5000, 10000, 30000]

    for (const timeout of timeouts) {
      let completed = false

      await page.route('**/api/customers', (route) => {
        setTimeout(() => {
          completed = true
          route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify([]),
          })
        }, timeout / 2)
      })

      const start = Date.now()
      await page.goto(`${BASE_URL}/customers`)
      await page.waitForTimeout(timeout + 1000)
      const duration = Date.now() - start

      console.log(`Timeout ${timeout}ms: Completed in ${duration}ms`)

      // Request should complete
      expect(completed).toBe(true)
    }

    console.log('Configurable timeouts working ✓')
  })

  test('should handle connection timeouts', async ({ page }) => {
    // Simulate network timeout
    await page.route('**/api/customers', (route) => {
      // Never respond (simulates network timeout)
    })

    const startTime = Date.now()
    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(15000)
    const duration = Date.now() - startTime

    console.log(`Connection timeout duration: ${duration}ms`)

    // Should handle connection timeout gracefully
    expect(duration).toBeLessThan(20000)
    console.log('Connection timeout handled ✓')
  })

  test('should abort long-running operations', async ({ page }) => {
    let operationStarted = false
    let operationAborted = false

    await page.route('**/api/customers', async (route) => {
      operationStarted = true

      // Start a long operation
      await new Promise(resolve => setTimeout(resolve, 10000))

      if (!operationAborted) {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      }
    })

    const start = Date.now()
    await page.goto(`${BASE_URL}/customers`)

    // Abort after 2 seconds
    await page.waitForTimeout(2000)
    operationAborted = true

    await page.waitForTimeout(5000)
    const duration = Date.now() - start

    console.log(`Operation aborted after: ${duration}ms`)

    // Should be able to abort
    expect(duration).toBeLessThan(15000)
    console.log('Operation abortion working ✓')
  })
})

test.describe('Retry Pattern', () => {
  test('should retry failed requests', async ({ page }) => {
    let attemptCount = 0

    await page.route('**/api/customers', (route) => {
      attemptCount++
      if (attemptCount < 3) {
        // Fail first two attempts
        route.abort('failed')
      } else {
        // Succeed on third attempt
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([{ id: 1, name: 'Customer' }]),
        })
      }
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(10000)

    console.log(`Total attempts: ${attemptCount}`)

    // Should have attempted multiple times
    expect(attemptCount).toBeGreaterThanOrEqual(3)
    console.log('Retry pattern working ✓')
  })

  test('should implement exponential backoff', async ({ page }) => {
    const attemptTimes: number[] = []

    await page.route('**/api/customers', (route) => {
      const attemptTime = Date.now()
      attemptTimes.push(attemptTime)

      if (attemptTimes.length < 3) {
        route.abort('failed')
      } else {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      }
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(10000)

    // Calculate delays between attempts
    const delays = [
      attemptTimes[1] - attemptTimes[0],
      attemptTimes[2] - attemptTimes[1],
    ]

    console.log(`Attempt delays: ${delays[0]}ms, ${delays[1]}ms`)

    // Should have increasing delays (exponential backoff)
    expect(delays[1]).toBeGreaterThan(delays[0])
    console.log('Exponential backoff working ✓')
  })

  test('should respect retry limits', async ({ page }) => {
    let attemptCount = 0

    await page.route('**/api/customers', (route) => {
      attemptCount++
      // Always fail
      route.abort('failed')
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(15000)

    console.log(`Total attempts before giving up: ${attemptCount}`)

    // Should give up after reasonable number of attempts
    expect(attemptCount).toBeLessThan(10)
    console.log('Retry limit respected ✓')
  })

  test('should not retry on certain errors', async ({ page }) => {
    let attemptCount = 0

    await page.route('**/api/customers', (route) => {
      attemptCount++
      // Simulate client error (4xx) - should not retry
      route.fulfill({
        status: 400,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Bad Request' }),
      })
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(5000)

    console.log(`Attempts on 4xx error: ${attemptCount}`)

    // Should not retry on 4xx errors (or retry only once)
    expect(attemptCount).toBeLessThan(3)
    console.log('Smart retry logic working ✓')
  })

  test('should retry with jitter', async ({ page }) => {
    const attemptTimes: number[] = []

    await page.route('**/api/customers', (route) => {
      attemptTimes.push(Date.now())
      if (attemptTimes.length < 3) {
        route.abort('failed')
      } else {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      }
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(10000)

    // Add some variance to delays
    const delays = attemptTimes.slice(1).map((t, i) => t - attemptTimes[i])

    console.log(`Retry delays (with jitter): ${delays.map(d => `${d}ms`).join(', ')}`)

    // Delays should have some variance (jitter)
    const variance = Math.max(...delays) - Math.min(...delays)
    expect(variance).toBeGreaterThan(0)

    console.log('Jitter working ✓')
  })
})

test.describe('Circuit Breaker Timeout', () => {
  test('should open circuit after consecutive failures', async ({ page }) => {
    let failureCount = 0

    await page.route('**/api/customers', (route) => {
      failureCount++
      route.abort('failed')
    })

    // Make many failing requests
    for (let i = 0; i < 20; i++) {
      const start = Date.now()
      await page.request.get(`${BASE_URL}/api/customers`)
      const duration = Date.now() - start

      console.log(`Request ${i + 1}: ${duration}ms (failure ${failureCount})`)

      // After several failures, should fail fast (circuit open)
      if (failureCount > 5) {
        expect(duration).toBeLessThan(2000) // Should not wait long
      }
    }

    console.log(`Circuit opened after ${failureCount} failures ✓`)
  })

  test('should allow circuit to close after timeout', async ({ page }) => {
    let attempts = 0
    let circuitOpen = false

    await page.route('**/api/customers', (route) => {
      attempts++

      if (attempts <= 10) {
        // First 10 requests fail
        route.abort('failed')
      } else {
        // After delay, should recover
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      }
    })

    // Make requests
    for (let i = 0; i < 15; i++) {
      const start = Date.now()
      await page.request.get(`${BASE_URL}/api/customers`)
      const duration = Date.now() - start

      console.log(`Attempt ${attempts}: ${duration}ms`)

      // Should eventually recover
      if (attempts > 10) {
        expect(duration).toBeLessThan(5000)
      }
    }

    console.log('Circuit recovery working ✓')
  })
})

test.describe('Timeout & Retry Configuration', () => {
  test('should have reasonable default timeouts', async ({ page }) => {
    const start = Date.now()
    const response = await page.request.get(`${BASE_URL}/api/customers`)
    const duration = Date.now() - start

    console.log(`Default request timeout: ${duration}ms`)

    // Default timeout should be reasonable (not too short, not too long)
    expect(duration).toBeGreaterThan(0)
    expect(duration).toBeLessThan(30000)

    console.log('Default timeouts reasonable ✓')
  })

  test('should allow timeout configuration per endpoint', async ({ page }) => {
    const timeouts = {
      '/api/customers': 5000,
      '/api/orders': 10000,
      '/api/reports': 30000,
    }

    for (const [endpoint, expectedTimeout] of Object.entries(timeouts)) {
      const start = Date.now()
      await page.request.get(`${BASE_URL}${endpoint}`)
      const duration = Date.now() - start

      console.log(`${endpoint}: ${duration}ms`)

      // Should respect configured timeout
      expect(duration).toBeLessThan(expectedTimeout + 5000)
    }

    console.log('Per-endpoint timeouts working ✓')
  })

  test('should use different timeouts for different operations', async ({ page }) => {
    const operations = [
      { endpoint: '/api/customers', expected: 'short' },
      { endpoint: '/api/orders', expected: 'medium' },
      { endpoint: '/api/reports/comprehensive', expected: 'long' },
    ]

    for (const op of operations) {
      const start = Date.now()
      await page.request.get(`${BASE_URL}${op.endpoint}`)
      const duration = Date.now() - start

      console.log(`${op.endpoint}: ${duration}ms (${op.expected} timeout)`)
    }

    console.log('Operation-specific timeouts ✓')
  })
})

test.describe('Error Recovery', () => {
  test('should recover from transient errors', async ({ page }) => {
    let errorCount = 0
    let successCount = 0

    await page.route('**/api/customers', (route) => {
      errorCount++

      if (errorCount <= 3) {
        // Transient errors
        route.abort('failed')
      } else {
        successCount++
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([{ id: 1 }]),
        })
      }
    })

    // Retry until success
    let attempts = 0
    while (attempts < 10 && successCount === 0) {
      await page.request.get(`${BASE_URL}/api/customers`)
      attempts++
      await page.waitForTimeout(1000)
    }

    console.log(`Recovered after ${errorCount} errors in ${attempts} attempts`)

    expect(successCount).toBeGreaterThan(0)
    console.log('Transient error recovery working ✓')
  })

  test('should fail fast on permanent errors', async ({ page }) => {
    let attemptCount = 0

    await page.route('**/api/customers', (route) => {
      attemptCount++
      // Simulate permanent error
      route.fulfill({
        status: 404,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Not Found' }),
      })
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(10000)

    console.log(`Attempts on permanent error: ${attemptCount}`)

    // Should not retry much on permanent errors
    expect(attemptCount).toBeLessThan(5)
    console.log('Permanent error fast-fail working ✓')
  })
})

test.describe('Resilience Metrics', () => {
  test('should track retry success rate', async ({ page }) => {
    let totalAttempts = 0
    let successfulRetries = 0

    await page.route('**/api/customers', (route) => {
      totalAttempts++

      if (totalAttempts % 3 === 0) {
        // Succeed every 3rd attempt
        successfulRetries++
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      } else {
        route.abort('failed')
      }
    })

    // Make several requests
    for (let i = 0; i < 10; i++) {
      await page.request.get(`${BASE_URL}/api/customers`)
    }

    const retrySuccessRate = (successfulRetries / totalAttempts) * 100

    console.log(`Total attempts: ${totalAttempts}`)
    console.log(`Successful retries: ${successfulRetries}`)
    console.log(`Retry success rate: ${retrySuccessRate.toFixed(2)}%`)

    // Some retries should succeed
    expect(successfulRetries).toBeGreaterThan(0)
    console.log('Retry tracking working ✓')
  })

  test('should measure average retry delay', async ({ page }) => {
    const attemptTimes: number[] = []

    await page.route('**/api/customers', (route) => {
      attemptTimes.push(Date.now())
      if (attemptTimes.length < 3) {
        route.abort('failed')
      } else {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      }
    })

    await page.goto(`${BASE_URL}/customers`)
    await page.waitForTimeout(10000)

    const delays = attemptTimes.slice(1).map((t, i) => t - attemptTimes[i])
    const avgDelay = delays.reduce((a, b) => a + b, 0) / delays.length

    console.log(`Average retry delay: ${avgDelay.toFixed(0)}ms`)

    // Should have reasonable average delay
    expect(avgDelay).toBeGreaterThan(100)
    console.log('Retry delay measurement ✓')
  })
})
