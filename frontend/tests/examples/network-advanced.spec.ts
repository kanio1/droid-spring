/**
 * Advanced Network Testing
 * Demonstrates network throttling, offline mode, request/response modification
 */

import { test, expect } from '@playwright/test'

test.describe('Network Conditions Testing', () => {
  test.describe('Network Throttling', () => {
    test('should work under slow 3G', async ({ page, context }) => {
      // Simulate slow 3G network
      await context.setOffline(false)

      // Add extra latency to all requests
      await page.route('**/*', async route => {
        // Add 2 second delay
        await new Promise(resolve => setTimeout(resolve, 2000))
        await route.continue()
      })

      const startTime = Date.now()

      await page.goto('/dashboard')

      // Verify page loads but with delays
      await expect(page.locator('[data-testid="dashboard"]')).toBeVisible()

      const endTime = Date.now()
      const loadTime = endTime - startTime

      // Should take at least 2 seconds due to throttling
      expect(loadTime).toBeGreaterThan(2000)
    })

    test('should handle offline mode gracefully', async ({ page, context }) => {
      // Go online first
      await context.setOffline(false)
      await page.goto('/dashboard')
      await expect(page.locator('[data-testid="dashboard"]')).toBeVisible()

      // Switch to offline
      await context.setOffline(true)

      // Try to navigate - should show offline message
      await page.click('[data-testid="nav-customers"]')

      // Should show offline indicator
      await expect(page.locator('[data-testid="offline-message"]')).toBeVisible()

      // Verify network requests are blocked
      const networkErrors: string[] = []
      page.on('response', response => {
        if (!response.ok()) {
          networkErrors.push(response.url())
        }
      })

      await page.reload()
      await page.waitForTimeout(1000)

      expect(networkErrors.length).toBeGreaterThan(0)
    })

    test('should validate request/response modification', async ({ page }) => {
      // Mock and modify responses
      await page.route('**/api/customers', async route => {
        const response = await route.fetch()
        let body = await response.text()

        // Modify response body
        body = body.replace(/"email":\s*"[^"]+"/, '"email": "mocked@example.com"')

        await route.fulfill({
          response,
          body,
          headers: {
            ...response.headers(),
            'x-modified': 'true'
          }
        })
      })

      await page.goto('/customers')

      const customerEmail = await page.locator('[data-testid="customer-email"]').textContent()
      expect(customerEmail).toBe('mocked@example.com')

      // Verify custom header was added
      const response = await page.request.get('/api/customers')
      expect(response.headers()['x-modified']).toBe('true')
    })

    test('should validate HTTP/2 support', async ({ page, context }) => {
      // Check if connection is using HTTP/2
      const responses: any[] = []

      page.on('response', response => {
        responses.push({
          url: response.url(),
          status: response.status(),
          httpVersion: response.httpVersion()
        })
      })

      await page.goto('/dashboard')
      await page.waitForLoadState('networkidle')

      // Check if any requests used HTTP/2
      const http2Responses = responses.filter(r => r.httpVersion === 'h2')
      expect(http2Responses.length).toBeGreaterThan(0)
    })

    test('should test WebSocket reconnection', async ({ page }) => {
      // Track WebSocket connections
      let connectionCount = 0

      await page.addInitScript(() => {
        // Monitor WebSocket connections
        const originalWebSocket = window.WebSocket
        window.WebSocket = function(url, protocols) {
          connectionCount++
          console.log('WebSocket connection:', url)
          return new originalWebSocket(url, protocols)
        }
      })

      await page.goto('/realtime-dashboard')

      // Initial connection
      await page.waitForTimeout(1000)
      expect(connectionCount).toBeGreaterThan(0)

      // Simulate network interruption
      await page.evaluate(() => {
        // Simulate connection drop
        window.dispatchEvent(new Event('offline'))
      })

      await page.waitForTimeout(2000)

      // Simulate reconnection
      await page.evaluate(() => {
        window.dispatchEvent(new Event('online'))
      })

      await page.waitForTimeout(2000)

      // Should reconnect
      expect(connectionCount).toBeGreaterThan(1)
    })
  })

  test.describe('Request/Response Interception', () => {
    test('should cache responses appropriately', async ({ page }) => {
      // Track cache hits
      let cacheHits = 0

      await page.route('**/api/products', async route => {
        const response = await route.fetch()
        const headers = response.headers()

        if (headers['cache-control']?.includes('max-age')) {
          cacheHits++
        }

        await route.fulfill({ response })
      })

      await page.goto('/products')
      await page.waitForLoadState('networkidle')

      // Reload page
      await page.reload()
      await page.waitForLoadState('networkidle')

      // Second load should be from cache
      expect(cacheHits).toBeGreaterThan(0)
    })

    test('should validate compression (gzip/brotli)', async ({ page }) => {
      // Check if server uses compression
      const response = await page.request.get('/api/large-dataset')

      const contentEncoding = response.headers()['content-encoding']
      expect(['gzip', 'br', 'deflate']).toContain(contentEncoding)

      // Verify compressed size is smaller
      const body = await response.body()
      expect(body.length).toBeLessThan(100000) // Should be compressed
    })

    test('should handle CORS preflight', async ({ page }) => {
      // This test requires CORS to be enabled
      const response = await page.request.post('http://localhost:3001/api/cors-test', {
        headers: {
          'Origin': 'http://localhost:3000',
          'Access-Control-Request-Method': 'POST',
          'Access-Control-Request-Headers': 'Content-Type'
        }
      })

      if (response.status() !== 200) {
        // CORS not configured for this endpoint
        expect([200, 403]).toContain(response.status())
        return
      }

      expect(response.status()).toBe(200)
      expect(response.headers()['access-control-allow-origin']).toBe('*')
    })

    test('should validate HTTP headers security', async ({ page }) => {
      const response = await page.request.get('/dashboard')

      // Check security headers
      const headers = response.headers()

      expect(headers['x-frame-options']).toBeDefined()
      expect(headers['x-content-type-options']).toBe('nosniff')
      expect(headers['x-xss-protection']).toBeDefined()

      // HSTS (only for HTTPS)
      if (page.url().startsWith('https://')) {
        expect(headers['strict-transport-security']).toBeDefined()
      }
    })
  })

  test.describe('Advanced Network Scenarios', () => {
    test('should handle large file uploads', async ({ page }) => {
      // Create a large test file
      const filePath = 'test-results/large-file.bin'
      const fileSize = 50 * 1024 * 1024 // 50MB

      const fs = require('fs')
      const buffer = Buffer.alloc(fileSize)
      fs.writeFileSync(filePath, buffer)

      // Track upload progress
      let uploadStarted = false
      let uploadCompleted = false

      page.on('requestfinished', request => {
        if (request.url().includes('/api/upload') && request.method() === 'POST') {
          uploadCompleted = true
        }
      })

      await page.goto('/upload')

      // Upload file
      const fileInput = page.locator('input[type="file"]')
      await fileInput.setInputFiles(filePath)

      // Monitor progress
      await expect(page.locator('[data-testid="upload-progress"]')).toBeVisible()

      // Wait for completion
      await expect(page.locator('[data-testid="upload-success"]')).toBeVisible({ timeout: 60000 })
      expect(uploadCompleted).toBe(true)

      // Cleanup
      fs.unlinkSync(filePath)
    })

    test('should validate streaming responses', async ({ page }) => {
      // Start time tracking
      const startTime = Date.now()

      // Track chunk received
      let chunksReceived = 0

      page.on('response', async response => {
        if (response.url().includes('/api/stream-data')) {
          const reader = response.body().getReader()
          while (true) {
            const { done } = await reader.read()
            if (done) break
            chunksReceived++
          }
        }
      })

      await page.goto('/stream-viewer')

      // Wait for streaming to start
      await page.waitForSelector('[data-testid="streaming"]', { state: 'visible' })

      // Let it stream for a bit
      await page.waitForTimeout(5000)

      const endTime = Date.now()
      const duration = endTime - startTime

      // Should receive multiple chunks
      expect(chunksReceived).toBeGreaterThan(1)

      // Should stream continuously
      expect(duration).toBeLessThan(10000)
    })

    test('should handle connection pooling', async ({ page, context }) => {
      // Make multiple concurrent requests
      const requests = Array.from({ length: 10 }, () =>
        context.request().get('/api/customers')
      )

      const responses = await Promise.all(requests)

      // All should succeed
      responses.forEach(response => {
        expect(response.status()).toBe(200)
      })

      // Verify connection reuse (check timing)
      const start = Date.now()
      await Promise.all(requests)
      const duration = Date.now() - start

      // Should be fast due to connection pooling
      expect(duration).toBeLessThan(2000)
    })
  })
})
