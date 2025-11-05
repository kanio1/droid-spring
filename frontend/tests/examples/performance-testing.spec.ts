/**
 * Performance Testing Examples
 *
 * Demonstrates how to measure and validate web performance metrics
 * Includes Core Web Vitals, load times, and network throttling
 *
 * Core Web Vitals:
 * - LCP (Largest Contentful Paint): < 2.5s
 * - FID (First Input Delay): < 100ms
 * - CLS (Cumulative Layout Shift): < 0.1
 */

import { test, expect } from '@playwright/test'

test.describe('Performance Testing', () => {
  test('page should load quickly', async ({ page }) => {
    await page.goto('/')

    // Measure page load time
    const loadTime = await page.evaluate(() => {
      const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming
      return {
        domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
        loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
        firstPaint: performance.getEntriesByName('first-paint')[0]?.startTime || 0
      }
    })

    console.log('Load times:', loadTime)

    // Verify load time is acceptable
    expect(loadTime.domContentLoaded).toBeLessThan(2000) // 2 seconds
    expect(loadTime.loadComplete).toBeLessThan(3000) // 3 seconds
  })

  test('dashboard should have good LCP', async ({ page }) => {
    await page.goto('/dashboard')

    // Measure LCP
    const lcp = await page.evaluate(() => {
      return new Promise<number>((resolve) => {
        const observer = new PerformanceObserver((list) => {
          const entries = list.getEntries()
          const lastEntry = entries[entries.length - 1]
          resolve(lastEntry.startTime)
        })
        observer.observe({ entryTypes: ['largest-contentful-paint'] })
        setTimeout(() => resolve(0), 5000) // Timeout after 5s
      })
    })

    console.log('LCP:', lcp)

    // LCP should be < 2.5s (2500ms)
    expect(lcp).toBeLessThan(2500)
  })

  test('page should have stable layout (CLS)', async ({ page }) => {
    await page.goto('/customers')

    // Measure CLS
    const cls = await page.evaluate(() => {
      return new Promise<number>((resolve) => {
        let clsValue = 0
        const observer = new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            if (!entry.hadRecentInput) {
              clsValue += entry.value
            }
          }
        })
        observer.observe({ entryTypes: ['layout-shift'] })
        setTimeout(() => resolve(clsValue), 2000) // Wait for layout shifts
      })
    })

    console.log('CLS:', cls)

    // CLS should be < 0.1
    expect(cls).toBeLessThan(0.1)
  })

  test('first input should be responsive (FID)', async ({ page }) => {
    await page.goto('/')

    // Measure FID
    const fidPromise = page.evaluate(() => {
      return new Promise<number>((resolve) => {
        const observer = new PerformanceObserver((list) => {
          const entries = list.getEntries()
          if (entries.length > 0) {
            resolve(entries[0].processingStart - entries[0].startTime)
          }
        })
        observer.observe({ entryTypes: ['first-input'] })
        setTimeout(() => resolve(-1), 5000) // Timeout
      })
    })

    // Trigger first input
    await page.click('h1')

    const fid = await fidPromise

    if (fid >= 0) {
      console.log('FID:', fid)
      // FID should be < 100ms
      expect(fid).toBeLessThan(100)
    }
  })

  test('memory usage should be reasonable', async ({ page }) => {
    await page.goto('/dashboard')

    // Navigate to multiple pages to test memory
    const pages = ['/customers', '/orders', '/invoices', '/products']
    for (const url of pages) {
      await page.goto(url)
    }

    // Return to dashboard
    await page.goto('/dashboard')

    // Measure memory usage
    const memoryInfo = await page.evaluate(() => {
      if ('memory' in performance) {
        const memory = (performance as any).memory
        return {
          used: memory.usedJSHeapSize,
          total: memory.totalJSHeapSize,
          limit: memory.jsHeapSizeLimit
        }
      }
      return null
    })

    if (memoryInfo) {
      console.log('Memory usage:', {
        used: `${(memoryInfo.used / 1024 / 1024).toFixed(2)} MB`,
        total: `${(memoryInfo.total / 1024 / 1024).toFixed(2)} MB`,
        limit: `${(memoryInfo.limit / 1024 / 1024).toFixed(2)} MB`
      })

      // Memory usage should be reasonable (< 100MB)
      expect(memoryInfo.used / 1024 / 1024).toBeLessThan(100)
    }
  })

  test('images should be optimized', async ({ page }) => {
    await page.goto('/customers')

    // Check all images
    const imageInfo = await page.evaluate(() => {
      const images = Array.from(document.querySelectorAll('img'))
      return images.map(img => ({
        src: img.src,
        naturalWidth: img.naturalWidth,
        naturalHeight: img.naturalHeight,
        width: img.width,
        height: img.height,
        lazy: img.loading === 'lazy'
      }))
    })

    console.log('Images:', imageInfo)

    // Verify images have reasonable sizes
    for (const img of imageInfo) {
      if (img.src.includes('placeholder')) continue // Skip placeholder images

      expect(img.naturalWidth).toBeGreaterThan(0)
      expect(img.naturalHeight).toBeGreaterThan(0)

      // Image shouldn't be too large (max 1920px for desktop)
      expect(img.naturalWidth).toBeLessThanOrEqual(1920)
    }
  })

  test('JavaScript bundle size should be acceptable', async ({ page }) => {
    await page.goto('/')

    // Check network requests
    const jsRequests = page.waitForResponse(response => {
      return response.url().includes('.js') && response.request().resourceType() === 'script'
    })

    await page.goto('/customers')

    const response = await jsRequests
    const contentLength = response.headers()['content-length'] || 0

    console.log('JS bundle size:', `${Number(contentLength) / 1024} KB`)

    // Bundle should be < 500KB
    expect(Number(contentLength) / 1024).toBeLessThan(500)
  })

  test('CSS should be optimized', async ({ page }) => {
    await page.goto('/')

    // Check CSS files
    const cssRequests = page.waitForResponse(response => {
      return response.url().includes('.css') && response.request().resourceType() === 'stylesheet'
    })

    await page.goto('/orders')

    const response = await cssRequests
    const contentLength = response.headers()['content-length'] || 0

    console.log('CSS size:', `${Number(contentLength) / 1024} KB`)

    // CSS should be < 100KB
    expect(Number(contentLength) / 1024).toBeLessThan(100)
  })

  test('API responses should be fast', async ({ page }) => {
    const startTime = Date.now()

    await page.goto('/customers')

    // Wait for API call
    await page.waitForResponse('**/api/customers')

    const responseTime = Date.now() - startTime

    console.log('API response time:', responseTime, 'ms')

    // API response should be < 500ms
    expect(responseTime).toBeLessThan(500)
  })

  test('virtual scrolling should be efficient', async ({ page }) => {
    await page.goto('/customers')

    // Generate many items (simulate)
    await page.evaluate(() => {
      const list = document.querySelector('[data-testid="customer-list"]')
      for (let i = 0; i < 1000; i++) {
        const item = document.createElement('div')
        item.textContent = `Customer ${i}`
        item.className = 'customer-item'
        list?.appendChild(item)
      }
    })

    // Scroll through list
    await page.evaluate(() => {
      const list = document.querySelector('[data-testid="customer-list"]')
      list?.scrollTo(0, 10000)
    })

    // Count rendered items
    const renderedCount = await page.evaluate(() => {
      const list = document.querySelector('[data-testid="customer-list"]')
      return list?.querySelectorAll('.customer-item').length || 0
    })

    console.log('Rendered items:', renderedCount)

    // Should only render visible items (virtual scrolling)
    expect(renderedCount).toBeLessThan(50)
  })

  test('animations should be performant', async ({ page }) => {
    await page.goto('/dashboard')

    // Start animation
    await page.click('[data-testid="start-animation"]')

    // Measure frame rate
    let frames = 0
    let lastTime = performance.now()

    await page.evaluate(() => {
      return new Promise<void>((resolve) => {
        let frameCount = 0
        function countFrames() {
          frameCount++
          requestAnimationFrame(countFrames)
          if (frameCount === 60) { // Measure 60 frames
            resolve()
          }
        }
        countFrames()
      })
    })

    // Verify animation is smooth (60 FPS)
    // This is a simplified check
    const animationDuration = 1000 // 1 second
    expect(frames).toBeGreaterThan(50) // At least 50 FPS
  })
})

// Network throttling tests
test.describe('Network Throttling', () => {
  test('page should work on slow 3G', async ({ page }) => {
    // Simulate slow 3G
    await page.context().setOffline(false)
    await page.context().setExtraHTTPHeaders({})

    await page.route('**/*', (route) => {
      // Slow down requests
      setTimeout(() => route.continue(), 500)
    })

    const startTime = Date.now()
    await page.goto('/')
    const loadTime = Date.now() - startTime

    console.log('Load time on 3G:', loadTime, 'ms')

    // On 3G, load time should be reasonable (< 10 seconds)
    expect(loadTime).toBeLessThan(10000)
  })

  test('app should handle offline mode', async ({ page }) => {
    await page.goto('/customers')

    // Go offline
    await page.context().setOffline(true)

    // Try to load data
    const [response] = await Promise.all([
      page.waitForResponse('**/api/customers', { throwOnError: false }),
      page.reload()
    ])

    // Should show offline message or cached data
    await expect(page.locator('[data-testid="offline-message"]')).toBeVisible()

    // Go back online
    await page.context().setOffline(false)
    await page.reload()

    // Should work normally
    await expect(page.locator('[data-testid="customer-list"]')).toBeVisible()
  })

  test('images should lazy load', async ({ page }) => {
    await page.goto('/customers')

    // Check if images have loading="lazy"
    const lazyImages = await page.evaluate(() => {
      const images = Array.from(document.querySelectorAll('img'))
      return images.filter(img => img.loading === 'lazy').length
    })

    console.log('Lazy loaded images:', lazyImages)

    // Most images should be lazy loaded
    expect(lazyImages).toBeGreaterThan(5)
  })

  test('service worker should cache resources', async ({ page }) => {
    await page.goto('/')

    // Check if service worker is registered
    const swRegistered = await page.evaluate(() => {
      return 'serviceWorker' in navigator && navigator.serviceWorker.controller !== null
    })

    expect(swRegistered).toBe(true)

    // Cache hit should be faster than cache miss
    const start1 = Date.now()
    await page.goto('/customers')
    const time1 = Date.now() - start1

    const start2 = Date.now()
    await page.goto('/customers')
    const time2 = Date.now() - start2

    console.log('First load:', time1, 'ms')
    console.log('Cached load:', time2, 'ms')

    // Cached load should be faster
    expect(time2).toBeLessThan(time1)
  })
})

// Lighthouse CI integration
test.describe('Lighthouse Metrics', () => {
  test('dashboard should pass Lighthouse audit', async ({ page }) => {
    await page.goto('/dashboard')

    // Run Lighthouse
    const metrics = await page.evaluate(() => {
      return new Promise((resolve) => {
        ;(window as any).runLighthouse().then((results: any) => {
          resolve({
            performance: results.lhr.categories.performance.score * 100,
            accessibility: results.lhr.categories.accessibility.score * 100,
            bestPractices: results.lhr.categories['best-practices'].score * 100,
            seo: results.lhr.categories.seo.score * 100
          })
        })
      })
    })

    console.log('Lighthouse scores:', metrics)

    // Scores should be > 90
    expect(metrics.performance).toBeGreaterThanOrEqual(90)
    expect(metrics.accessibility).toBeGreaterThanOrEqual(90)
    expect(metrics.bestPractices).toBeGreaterThanOrEqual(90)
    expect(metrics.seo).toBeGreaterThanOrEqual(90)
  })
})
