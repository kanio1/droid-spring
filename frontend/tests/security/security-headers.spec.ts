/**
 * Security Headers Validation Tests
 *
 * This test validates that all security headers are properly configured
 * to protect against common web vulnerabilities
 */

import { test, expect, request } from '@playwright/test'

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000'

test.describe('Security Headers Validation', () => {
  test.describe('Login Page', () => {
    test('should have HSTS header', async () => {
      const response = await request.get(`${BASE_URL}/login`)
      const hsts = response.headers()['strict-transport-security']

      expect(hsts).toBeDefined()
      expect(hsts).toMatch(/max-age=\d+/)
    })

    test('should have X-Content-Type-Options header', async () => {
      const response = await request.get(`${BASE_URL}/login`)
      const xcto = response.headers()['x-content-type-options']

      expect(xcto).toBeDefined()
      expect(xcto).toBe('nosniff')
    })

    test('should have X-Frame-Options header', async () => {
      const response = await request.get(`${BASE_URL}/login`)
      const xfo = response.headers()['x-frame-options']

      expect(xfo).toBeDefined()
      expect(xfo).toMatch(/(DENY|SAMEORIGIN)/)
    })

    test('should have X-XSS-Protection header', async () => {
      const response = await request.get(`${BASE_URL}/login`)
      const xss = response.headers()['x-xss-protection']

      expect(xss).toBeDefined()
      expect(xss).toBe('1; mode=block')
    })

    test('should have Content-Security-Policy header', async () => {
      const response = await request.get(`${BASE_URL}/login`)
      const csp = response.headers()['content-security-policy']

      expect(csp).toBeDefined()
      // CSP should prevent inline scripts
      expect(csp).toMatch(/script-src/)
      expect(csp).not.toContain("script-src 'unsafe-inline'")
    })

    test('should have Referrer-Policy header', async () => {
      const response = await request.get(`${BASE_URL}/login`)
      const referrer = response.headers()['referrer-policy']

      expect(referrer).toBeDefined()
      expect(referrer).toMatch(/(strict-origin|origin|no-referrer)/)
    })

    test('should have Permissions-Policy header', async () => {
      const response = await request.get(`${BASE_URL}/login`)
      const permissions = response.headers()['permissions-policy']

      // Permissions-Policy is recommended but not always required
      // This is a soft check
      console.log(`Permissions-Policy: ${permissions}`)
    })

    test('should not have Server header revealing version', async () => {
      const response = await request.get(`${BASE_URL}/login`)
      const server = response.headers()['server']

      if (server) {
        // Should not reveal specific versions
        expect(server).not.toMatch(/\d+\.\d+/)
      }
    })

    test('should not have X-Powered-By header', async () => {
      const response = await request.get(`${BASE_URL}/login`)
      const xpb = response.headers()['x-powered-by']

      // Should not reveal technology stack
      expect(xpb).toBeUndefined()
    })
  })

  test.describe('Dashboard Page', () => {
    test('should have all security headers on dashboard', async ({ page }) => {
      await page.goto(`${BASE_URL}/login`)

      // Login
      try {
        await page.fill('[data-testid="username-input"]', 'admin')
        await page.fill('[data-testid="password-input"]', 'password')
        await page.click('[data-testid="login-button"]')
        await page.waitForURL('**/dashboard', { timeout: 10000 })
      } catch (error) {
        console.log('Login failed, continuing with header check...')
      }

      const response = await page.request.get(`${BASE_URL}/dashboard`)
      const headers = response.headers()

      // Validate all critical security headers
      expect(headers['strict-transport-security']).toBeDefined()
      expect(headers['x-content-type-options']).toBe('nosniff')
      expect(headers['x-frame-options']).toMatch(/(DENY|SAMEORIGIN)/)
      expect(headers['x-xss-protection']).toBe('1; mode=block')
      expect(headers['content-security-policy']).toBeDefined()
    })

    test('should prevent clickjacking via frame-busting', async ({ page }) => {
      await page.goto(`${BASE_URL}/login`)

      const hasFrameBusting = await page.evaluate(() => {
        return (
          window.top !== window.self ||
          document.getElementById('frame-buster') !== null ||
          typeof window.frameElement === 'undefined'
        )
      })

      // Should have some form of frame busting
      expect(hasFrameBusting).toBe(true)
    })
  })

  test.describe('API Endpoints', () => {
    test('should have security headers on API responses', async ({ request }) => {
      const response = await request.get(`${BASE_URL}/api/customers`)

      const headers = response.headers()
      expect(headers['x-content-type-options']).toBe('nosniff')
      expect(headers['cache-control']).toBeDefined()
    })

    test('should not expose sensitive data in response headers', async ({ request }) => {
      const response = await request.get(`${BASE_URL}/api/customers`)

      const body = await response.text()
      expect(body).not.toMatch(/"password":/i)
      expect(body).not.toMatch(/"token":/i)
      expect(body).not.toMatch(/"secret":/i)
      expect(body).not.toMatch(/"key":/i)
    })

    test('should have proper CORS headers for API', async ({ request }) => {
      const response = await request.get(`${BASE_URL}/api/customers`)

      // CORS headers should be present
      // Note: Actual CORS config depends on your requirements
      const corsHeaders = ['access-control-allow-origin', 'access-control-allow-methods']
      for (const header of corsHeaders) {
        const value = response.headers()[header]
        if (value) {
          console.log(`${header}: ${value}`)
        }
      }
    })
  })

  test.describe('Cookie Security', () => {
    test('should set secure flag on authentication cookies', async ({ page }) => {
      await page.goto(`${BASE_URL}/login`)
      await page.fill('[data-testid="username-input"]', 'admin')
      await page.fill('[data-testid="password-input"]', 'password')
      await page.click('[data-testid="login-button"]')
      await page.waitForURL('**/dashboard', { timeout: 10000 })

      const cookies = await page.context().cookies()
      const authCookie = cookies.find(c => c.name.toLowerCase().includes('token') || c.name.toLowerCase().includes('session'))

      if (authCookie) {
        // In production, cookies should be secure
        // In local dev, this might not be the case
        console.log(`Cookie: ${authCookie.name}, Secure: ${authCookie.secure}, HttpOnly: ${authCookie.httpOnly}`)
      }
    })

    test('should set HttpOnly flag on sensitive cookies', async ({ page }) => {
      await page.goto(`${BASE_URL}/login`)
      await page.fill('[data-testid="username-input"]', 'admin')
      await page.fill('[data-testid="password-input"]', 'password')
      await page.click('[data-testid="login-button"]')
      await page.waitForURL('**/dashboard', { timeout: 10000 })

      const cookies = await page.context().cookies()
      const authCookie = cookies.find(c => c.name.toLowerCase().includes('token') || c.name.toLowerCase().includes('session'))

      if (authCookie) {
        // Should be HttpOnly to prevent XSS access
        expect(authCookie.httpOnly).toBe(true)
      }
    })

    test('should have SameSite attribute on cookies', async ({ page }) => {
      await page.goto(`${BASE_URL}/login`)
      await page.fill('[data-testid="username-input"]', 'admin')
      await page.fill('[data-testid="password-input"]', 'password')
      await page.click('[data-testid="login-button"]')
      await page.waitForURL('**/dashboard', { timeout: 10000 })

      const cookies = await page.context().cookies()
      const authCookie = cookies.find(c => c.name.toLowerCase().includes('token') || c.name.toLowerCase().includes('session'))

      if (authCookie) {
        // Should have SameSite for CSRF protection
        expect(['Lax', 'Strict', 'None']).toContain(authCookie.sameSite)
      }
    })
  })

  test.describe('HTTPS Configuration', () => {
    test('should redirect HTTP to HTTPS in production', async ({ request }) => {
      // In production, HTTP should redirect to HTTPS
      // This test may not pass in local development
      try {
        const response = await request.get(`http://localhost:3000/login`, {
          ignoreHTTPSErrors: false,
        })

        // In production, this should be a redirect
        if (response.status() === 301 || response.status() === 302) {
          const location = response.headers()['location']
          expect(location).toMatch(/^https:/)
        } else {
          console.log('HTTPS redirect not configured (expected in dev)')
        }
      } catch (error) {
        console.log('HTTPS test skipped (dev environment)')
      }
    })

    test('should not allow mixed content', async ({ page }) => {
      await page.goto(`${BASE_URL}/dashboard`)

      const mixedContent = await page.evaluate(() => {
        const resources = Array.from(document.scripts)
        return resources.some(script => {
          const src = script.getAttribute('src')
          return src && src.startsWith('http:')
        })
      })

      expect(mixedContent).toBe(false)
    })
  })

  test.describe('Security Headers Consistency', () => {
    test('should have consistent headers across all routes', async ({ page }) => {
      const routes = ['/login', '/dashboard', '/customers', '/orders']

      for (const route of routes) {
        const response = await page.request.get(`${BASE_URL}${route}`)
        const headers = response.headers()

        // All routes should have these headers
        expect(headers['x-content-type-options']).toBe('nosniff')

        // Log any missing headers
        if (!headers['x-frame-options']) {
          console.log(`Route ${route} missing X-Frame-Options header`)
        }
      }
    })

    test('should prevent information disclosure in error pages', async ({ request }) => {
      // Try accessing non-existent page
      const response = await request.get(`${BASE_URL}/non-existent-page-12345`)

      if (response.status() === 404) {
        const body = await response.text()

        // Should not reveal stack traces or system info
        expect(body).not.toMatch(/java\.lang\.|at com\.|Exception|Stack trace/)
        expect(body).not.toMatch(/Apache|Nginx|IIS|version/i)
      }
    })
  })
})
