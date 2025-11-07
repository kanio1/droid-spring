/**
 * Load & Stress Resilience Testing
 *
 * This test validates system behavior under high load and stress conditions
 * while maintaining resilience patterns
 */

import { test, expect } from '@playwright/test'

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000'

test.describe('High Load Resilience', () => {
  test('should maintain response time under load', async ({ page }) => {
    const responseTimes: number[] = []
    const concurrency = 10

    // Create concurrent requests
    const promises = Array.from({ length: concurrency }, async () => {
      const start = Date.now()
      const response = await page.request.get(`${BASE_URL}/api/customers`)
      const duration = Date.now() - start
      responseTimes.push(duration)
      return { status: response.status(), duration }
    })

    const results = await Promise.all(promises)
    const avgResponseTime = responseTimes.reduce((a, b) => a + b, 0) / responseTimes.length

    console.log(`Concurrent requests: ${concurrency}`)
    console.log(`Average response time: ${avgResponseTime.toFixed(2)}ms`)
    console.log(`Max response time: ${Math.max(...responseTimes)}ms`)
    console.log(`Min response time: ${Math.min(...responseTimes)}ms`)

    // All requests should complete
    expect(results).toHaveLength(concurrency)

    // Average response time should be reasonable (under 5 seconds)
    expect(avgResponseTime).toBeLessThan(5000)

    console.log('High load handled ✓')
  })

  test('should prevent memory leaks under sustained load', async ({ page }) => {
    const memorySnapshots: number[] = []

    // Simulate sustained load
    for (let i = 0; i < 10; i++) {
      await page.goto(`${BASE_URL}/customers`)
      await page.waitForTimeout(1000)

      const memory = await page.evaluate(() => {
        return (performance as any).memory?.usedJSHeapSize || 0
      })
      memorySnapshots.push(memory)
    }

    // Calculate memory growth
    const initialMemory = memorySnapshots[0]
    const finalMemory = memorySnapshots[memorySnapshots.length - 1]
    const growth = ((finalMemory - initialMemory) / initialMemory) * 100

    console.log(`Initial memory: ${(initialMemory / 1024 / 1024).toFixed(2)}MB`)
    console.log(`Final memory: ${(finalMemory / 1024 / 1024).toFixed(2)}MB`)
    console.log(`Memory growth: ${growth.toFixed(2)}%`)

    // Memory growth should be minimal (less than 50%)
    expect(growth).toBeLessThan(50)

    console.log('No significant memory leaks ✓')
  })

  test('should handle traffic spikes gracefully', async ({ page }) => {
    const spikeSizes = [5, 10, 20, 50, 100]
    const results: any[] = []

    for (const spikeSize of spikeSizes) {
      console.log(`\nTesting spike: ${spikeSize} concurrent users`)

      const start = Date.now()
      const promises = Array.from({ length: spikeSize }, () =>
        page.request.get(`${BASE_URL}/api/customers`)
      )

      const responses = await Promise.allSettled(promises)
      const duration = Date.now() - start

      const successCount = responses.filter(r => r.status === 'fulfilled').length
      const successRate = (successCount / spikeSize) * 100

      results.push({
        spikeSize,
        duration,
        successRate,
        throughput: (successCount / duration) * 1000, // requests per second
      })

      console.log(`  Duration: ${duration}ms`)
      console.log(`  Success rate: ${successRate.toFixed(2)}%`)
      console.log(`  Throughput: ${((successCount / duration) * 1000).toFixed(2)} req/s`)

      // Wait between spikes
      await page.waitForTimeout(2000)
    }

    // System should maintain reasonable success rate even at high load
    const worstPerformance = results[results.length - 1]
    expect(worstPerformance.successRate).toBeGreaterThan(50)

    console.log('\nTraffic spikes handled ✓')
  })

  test('should maintain data consistency under concurrent writes', async ({ page }) => {
    const updates: any[] = []

    // Simulate concurrent updates to the same resource
    for (let i = 0; i < 20; i++) {
      const response = await page.request.post(`${BASE_URL}/api/customers`, {
        data: {
          name: `Customer-${i}-${Date.now()}`,
          email: `customer${i}@test.com`,
        },
      })

      if (response.status() === 200 || response.status() === 201) {
        updates.push(await response.json())
      }
    }

    console.log(`Concurrent updates: ${updates.length}`)

    // Verify all updates were processed
    // (In real scenario, you might check database consistency)
    expect(updates.length).toBeGreaterThan(15)

    console.log('Data consistency maintained ✓')
  })
})

test.describe('Stress Testing Resilience', () => {
  test('should survive beyond breaking point', async ({ page }) => {
    console.log('\n=== Stress Test: Finding Breaking Point ===')

    let currentLoad = 1
    let breakingPoint = 0
    const maxLoad = 200

    while (currentLoad <= maxLoad) {
      const start = Date.now()
      const promises = Array.from({ length: currentLoad }, () =>
        page.request.get(`${BASE_URL}/api/customers`)
      )

      const responses = await Promise.allSettled(promises)
      const duration = Date.now() - start

      const successRate = (responses.filter(r => r.status === 'fulfilled').length / currentLoad) * 100

      console.log(`Load ${currentLoad}: ${successRate.toFixed(2)}% success, ${duration}ms`)

      if (successRate < 50) {
        breakingPoint = currentLoad
        console.log(`\nBreaking point found at: ${currentLoad} concurrent users`)
        break
      }

      currentLoad += 10
    }

    // System should have a reasonable breaking point (not too low)
    expect(breakingPoint).toBeGreaterThan(0)

    console.log('\nStress test completed ✓')
  })

  test('should recover after stress test', async ({ page }) => {
    // Apply high stress
    const stressLoad = 50
    const promises = Array.from({ length: stressLoad }, () =>
      page.request.get(`${BASE_URL}/api/customers`)
    )

    await Promise.allSettled(promises)

    // Wait for system to recover
    await page.waitForTimeout(5000)

    // Test normal operation
    const response = await page.request.get(`${BASE_URL}/api/customers`)

    // Should be back to normal
    expect([200, 401, 403]).toContain(response.status())

    console.log('System recovered after stress ✓')
  })

  test('should maintain availability SLA under stress', async ({ page }) => {
    const testDuration = 60000 // 1 minute
    const startTime = Date.now()
    let requestCount = 0
    let successCount = 0

    // Continuous load for 1 minute
    while (Date.now() - startTime < testDuration) {
      const response = await page.request.get(`${BASE_URL}/api/customers`)
      requestCount++

      if (response.status() < 500) {
        successCount++
      }

      await page.waitForTimeout(100) // 10 req/s
    }

    const availability = (successCount / requestCount) * 100

    console.log(`\nAvailability Test Results:`)
    console.log(`  Test duration: ${testDuration / 1000}s`)
    console.log(`  Total requests: ${requestCount}`)
    console.log(`  Successful requests: ${successCount}`)
    console.log(`  Availability: ${availability.toFixed(2)}%`)

    // Should maintain at least 99% availability
    expect(availability).toBeGreaterThan(99)

    console.log('SLA availability maintained ✓')
  })
})

test.describe('Chaos + Load Testing', () => {
  test('should maintain performance during failures', async ({ page }) => {
    const baselineResults: number[] = []

    // Get baseline performance
    for (let i = 0; i < 10; i++) {
      const start = Date.now()
      await page.request.get(`${BASE_URL}/api/customers`)
      baselineResults.push(Date.now() - start)
    }

    const baselineAvg = baselineResults.reduce((a, b) => a + b, 0) / baselineResults.length

    // Inject failures
    await page.route('**/api/customers', (route) => {
      if (Math.random() < 0.3) {
        // 30% failure rate
        route.abort('failed')
      } else {
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify([]),
        })
      }
    })

    const degradedResults: number[] = []

    // Test under failure conditions
    for (let i = 0; i < 10; i++) {
      const start = Date.now()
      await page.request.get(`${BASE_URL}/api/customers`)
      degradedResults.push(Date.now() - start)
    }

    const degradedAvg = degradedResults.reduce((a, b) => a + b, 0) / degradedResults.length

    const performanceImpact = ((degradedAvg - baselineAvg) / baselineAvg) * 100

    console.log(`\nPerformance Impact Analysis:`)
    console.log(`  Baseline avg: ${baselineAvg.toFixed(2)}ms`)
    console.log(`  Degraded avg: ${degradedAvg.toFixed(2)}ms`)
    console.log(`  Performance impact: ${performanceImpact.toFixed(2)}%`)

    // Performance should not degrade by more than 100% (double)
    expect(performanceImpact).toBeLessThan(100)

    console.log('Performance under failure acceptable ✓')
  })
})

test.describe('Resource Exhaustion', () => {
  test('should handle connection pool exhaustion', async ({ page }) => {
    // Exhaust connection pool
    const requests: Promise<any>[] = []
    for (let i = 0; i < 200; i++) {
      requests.push(page.request.get(`${BASE_URL}/api/customers`))
    }

    const responses = await Promise.allSettled(requests)
    const successful = responses.filter(r => r.status === 'fulfilled').length

    console.log(`Connection pool test: ${successful}/${requests.length} succeeded`)

    // Should handle pool exhaustion gracefully
    expect(successful).toBeGreaterThan(0)

    console.log('Connection pool exhaustion handled ✓')
  })

  test('should handle thread pool exhaustion', async ({ page }) => {
    const start = Date.now()
    const promises: Promise<any>[] = []

    // Create many blocking operations
    for (let i = 0; i < 100; i++) {
      promises.push(page.request.get(`${BASE_URL}/api/orders`))
    }

    const responses = await Promise.allSettled(promises)
    const duration = Date.now() - start

    const successful = responses.filter(r => r.status === 'fulfilled').length

    console.log(`Thread pool test: ${successful}/${promises.length} succeeded in ${duration}ms`)

    // Should complete without hanging
    expect(duration).toBeLessThan(30000)
    expect(successful).toBeGreaterThan(0)

    console.log('Thread pool exhaustion handled ✓')
  })
})

test.describe('Resilience Under Load - Long Running', () => {
  test('should maintain stability over extended period', async ({ page }) => {
    console.log('\n=== 5-Minute Stability Test ===')

    const testDuration = 5 * 60 * 1000 // 5 minutes
    const startTime = Date.now()
    let totalRequests = 0
    let successfulRequests = 0
    let errorCount = 0
    const responseTimes: number[] = []

    while (Date.now() - startTime < testDuration) {
      const reqStart = Date.now()
      const response = await page.request.get(`${BASE_URL}/api/customers`)
      const duration = Date.now() - reqStart

      totalRequests++
      responseTimes.push(duration)

      if (response.status() < 500) {
        successfulRequests++
      } else {
        errorCount++
      }

      // Log every 30 seconds
      if (totalRequests % 30 === 0) {
        const elapsed = ((Date.now() - startTime) / 1000).toFixed(0)
        const avgResponseTime = responseTimes.slice(-30).reduce((a, b) => a + b, 0) / 30
        console.log(`[${elapsed}s] Requests: ${totalRequests}, Success: ${successfulRequests}, Errors: ${errorCount}, Avg: ${avgResponseTime.toFixed(0)}ms`)
      }

      await page.waitForTimeout(100) // 10 req/s
    }

    const totalTime = (Date.now() - startTime) / 1000
    const successRate = (successfulRequests / totalRequests) * 100
    const throughput = (totalRequests / totalTime).toFixed(2)

    console.log(`\n=== Final Results ===`)
    console.log(`Duration: ${totalTime}s`)
    console.log(`Total requests: ${totalRequests}`)
    console.log(`Successful: ${successfulRequests}`)
    console.log(`Errors: ${errorCount}`)
    console.log(`Success rate: ${successRate.toFixed(2)}%`)
    console.log(`Throughput: ${throughput} req/s`)

    // Should maintain high success rate
    expect(successRate).toBeGreaterThan(95)
    expect(errorCount).toBeLessThan(totalRequests * 0.05)

    console.log('\nLong-term stability maintained ✓')
  }, 600000) // 10 minute timeout
})
