/**
 * Test Observability Utilities
 * Track test performance, metrics, and analytics
 */

import { test as base, type Page, type TestInfo } from '@playwright/test'

export interface TestMetrics {
  testName: string
  duration: number
  timestamp: Date
  browser: string
  viewport: string
  networkRequests: number
  networkBytes: number
  memoryUsage?: number
  errors: string[]
  warnings: string[]
  assertions: {
    total: number
    passed: number
    failed: number
  }
}

export interface NetworkMetrics {
  url: string
  method: string
  status: number
  duration: number
  size: number
  timestamp: Date
}

class TestObserver {
  private metrics: TestMetrics[] = []
  private networkCalls: NetworkMetrics[] = []

  startTest(testInfo: TestInfo) {
    // Track start time
    ;(testInfo as any)._startTime = Date.now()
  }

  endTest(testInfo: TestInfo) {
    const startTime = (testInfo as any)._startTime || Date.now()
    const duration = Date.now() - startTime

    const metrics: TestMetrics = {
      testName: testInfo.title,
      duration,
      timestamp: new Date(),
      browser: testInfo.projectName || 'unknown',
      viewport: '', // Will be populated from page
      networkRequests: this.networkCalls.length,
      networkBytes: this.networkCalls.reduce((sum, call) => sum + call.size, 0),
      errors: [],
      warnings: [],
      assertions: {
        total: testInfo.attachments.length, // Approximation
        passed: 0,
        failed: 0
      }
    }

    this.metrics.push(metrics)
  }

  trackNetworkCall(url: string, method: string, status: number, duration: number, size: number) {
    this.networkCalls.push({
      url,
      method,
      status,
      duration,
      size,
      timestamp: new Date()
    })
  }

  trackError(error: string) {
    if (this.metrics.length > 0) {
      this.metrics[this.metrics.length - 1].errors.push(error)
    }
  }

  trackWarning(warning: string) {
    if (this.metrics.length > 0) {
      this.metrics[this.metrics.length - 1].warnings.push(warning)
    }
  }

  getMetrics(): TestMetrics[] {
    return this.metrics
  }

  getNetworkCalls(): NetworkMetrics[] {
    return this.networkCalls
  }

  generateReport() {
    console.log('\n=== Test Observability Report ===\n')

    // Overall statistics
    const totalTests = this.metrics.length
    const avgDuration = this.metrics.reduce((sum, m) => sum + m.duration, 0) / totalTests
    const totalNetworkRequests = this.metrics.reduce((sum, m) => sum + m.networkRequests, 0)
    const totalNetworkBytes = this.metrics.reduce((sum, m) => sum + m.networkBytes, 0)

    console.log(`Total Tests: ${totalTests}`)
    console.log(`Average Duration: ${avgDuration.toFixed(2)}ms`)
    console.log(`Total Network Requests: ${totalNetworkRequests}`)
    console.log(`Total Network Data: ${(totalNetworkBytes / 1024).toFixed(2)}KB`)

    // Slowest tests
    const slowestTests = [...this.metrics]
      .sort((a, b) => b.duration - a.duration)
      .slice(0, 5)

    console.log('\nSlowest Tests:')
    slowestTests.forEach(test => {
      console.log(`  ${test.testName}: ${test.duration}ms`)
    })

    // Most network calls
    const mostNetworkCalls = [...this.metrics]
      .sort((a, b) => b.networkRequests - a.networkRequests)
      .slice(0, 5)

    console.log('\nMost Network Calls:')
    mostNetworkCalls.forEach(test => {
      console.log(`  ${test.testName}: ${test.networkRequests} requests (${(test.networkBytes / 1024).toFixed(2)}KB)`)
    })

    // Browser breakdown
    const byBrowser: Record<string, number> = {}
    this.metrics.forEach(test => {
      byBrowser[test.browser] = (byBrowser[test.browser] || 0) + 1
    })

    console.log('\nTests by Browser:')
    Object.entries(byBrowser).forEach(([browser, count]) => {
      console.log(`  ${browser}: ${count} tests`)
    })

    console.log('\n==================================\n')

    return {
      totalTests,
      avgDuration,
      totalNetworkRequests,
      totalNetworkBytes,
      slowestTests,
      mostNetworkCalls,
      byBrowser
    }
  }

  exportToJSON(filename = 'test-metrics.json') {
    const fs = require('fs')
    const report = this.generateReport()
    fs.writeFileSync(filename, JSON.stringify(report, null, 2))
    console.log(`Metrics exported to ${filename}`)
  }
}

export const testObserver = new TestObserver()

// Extended test fixture with observability
export const testWithObservability = base.extend<{}, { page: Page & { observability: TestObserver } }>({
  page: [
    async ({ page, testInfo }, use) => {
      testObserver.startTest(testInfo)

      // Track network requests
      page.on('response', (response) => {
        const startTime = response.timing().start
        const endTime = response.timing().responseEnd
        const duration = endTime - startTime

        testObserver.trackNetworkCall(
          response.url(),
          response.request().method(),
          response.status(),
          duration,
          response.body().then(buf => buf.length).catch(() => 0)
        )
      })

      // Track errors
      page.on('console', (msg) => {
        if (msg.type() === 'error') {
          testObserver.trackError(msg.text())
        } else if (msg.type() === 'warning') {
          testObserver.trackWarning(msg.text())
        }
      })

      // Add observer to page
      ;(page as any).observability = testObserver

      await use(page)

      testObserver.endTest(testInfo)
    },
    { scope: 'test' }
  ]
})

export { testWithObservability as test }

// Custom matchers for test results
export async function measurePagePerformance(page: Page) {
  const metrics = await page.evaluate(() => {
    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming
    const paint = performance.getEntriesByType('paint')

    return {
      loadTime: navigation.loadEventEnd - navigation.fetchStart,
      domContentLoaded: navigation.domContentLoadedEventEnd - navigation.fetchStart,
      firstPaint: paint.find(p => p.name === 'first-paint')?.startTime || 0,
      firstContentfulPaint: paint.find(p => p.name === 'first-contentful-paint')?.startTime || 0,
      memory: (performance as any).memory ? {
        used: (performance as any).memory.usedJSHeapSize,
        total: (performance as any).memory.totalJSHeapSize,
        limit: (performance as any).memory.jsHeapSizeLimit
      } : null
    }
  })

  return metrics
}

export async function checkAccessibility(page: Page, options?: { skipFailures?: boolean }) {
  const results = await page.evaluate(async () => {
    // @ts-ignore
    if (!window.axe) {
      throw new Error('axe-core not loaded. Please include axe-core in your test environment.')
    }

    // @ts-ignore
    const results = await window.axe.run(document, {
      rules: {
        'color-contrast': { enabled: true },
        'image-alt': { enabled: true },
        'label': { enabled: true }
      }
    })

    return results
  })

  if (!options?.skipFailures && results.violations.length > 0) {
    console.log('Accessibility violations found:', results.violations)
  }

  return results
}

export async function generateTestReport(testInfo: TestInfo, page?: Page) {
  const report = {
    testName: testInfo.title,
    status: testInfo.status,
    duration: testInfo.duration,
    attachments: testInfo.attachments.map(a => ({
      name: a.name,
      contentType: a.contentType
    })),
    errors: testInfo.errors
  }

  if (page) {
    const performance = await measurePagePerformance(page)
    ;(report as any).performance = performance
  }

  return report
}
