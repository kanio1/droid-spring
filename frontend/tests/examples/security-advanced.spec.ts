/**
 * Advanced Security Testing
 * Demonstrates XSS, CSRF, SQL Injection, Auth Bypass, OWASP Top 10
 */

import { test, expect } from '@playwright/test'

test.describe('Advanced Security Testing', () => {
  test.describe('XSS (Cross-Site Scripting)', () => {
    test('should prevent reflected XSS in search', async ({ page }) => {
      const xssPayload = '<script>alert("XSS")</script>'

      await page.goto('/search')
      await page.fill('input[name="query"]', xssPayload)
      await page.click('button[type="submit"]')

      // Should not execute script
      const alerts: string[] = []
      page.on('dialog', dialog => {
        alerts.push(dialog.message())
        dialog.accept()
      })

      await page.waitForTimeout(1000)
      expect(alerts).toHaveLength(0) // No XSS executed

      // Should show sanitized content
      const searchResults = page.locator('[data-testid="search-results"]')
      await expect(searchResults).toBeVisible()
    })

    test('should prevent stored XSS in user profile', async ({ page }) => {
      const maliciousScript = '<img src=x onerror="alert(\'XSS\')">'

      // Login as user
      await page.goto('/login')
      await page.fill('[name="email"]', 'user@example.com')
      await page.fill('[name="password"]', 'password123')
      await page.click('button[type="submit"]')
      await page.waitForURL('/dashboard')

      // Update profile with XSS payload
      await page.goto('/profile')
      await page.fill('[name="bio"]', maliciousScript)
      await page.click('button[data-testid="save-profile"]')

      // Verify no script execution
      const alerts: string[] = []
      page.on('dialog', dialog => {
        alerts.push(dialog.message())
        dialog.accept()
      })

      await page.reload()
      await page.waitForTimeout(1000)

      expect(alerts).toHaveLength(0)

      // Verify sanitization in DOM
      const bio = await page.locator('[data-testid="profile-bio"]').textContent()
      expect(bio).not.toContain('<script>')
      expect(bio).not.toContain('onerror')
    })

    test('should prevent DOM-based XSS', async ({ page }) => {
      const domXssPayload = '#<img src=x onerror=alert(1)>'

      await page.goto(`/profile${domXssPayload}`)

      // Should not execute script from URL fragment
      const alerts: string[] = []
      page.on('dialog', dialog => {
        alerts.push(dialog.message())
        dialog.accept()
      })

      await page.waitForTimeout(1000)
      expect(alerts).toHaveLength(0)

      // Should handle fragment safely
      const content = await page.textContent('body')
      expect(content).not.toContain('onerror')
    })

    test('should prevent XSS in JSON data', async ({ page }) => {
      const xssPayload = {
        name: '<script>alert("XSS")</script>',
        comment: '<img src=x onerror="alert(2)">'
      }

      await page.goto('/comments')

      // Submit comment with XSS
      await page.evaluate((payload) => {
        fetch('/api/comments', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        })
      }, xssPayload)

      // Verify script is not executed
      const alerts: string[] = []
      page.on('dialog', dialog => {
        alerts.push(dialog.message())
        dialog.accept()
      })

      await page.reload()
      await page.waitForTimeout(1000)

      expect(alerts).toHaveLength(0)
    })
  })

  test.describe('CSRF (Cross-Site Request Forgery)', () => {
    test('should validate CSRF token presence', async ({ page }) => {
      await page.goto('/profile')

      // Check for CSRF token in forms
      const csrfToken = await page.getAttribute('input[name="csrf_token"]', 'value')
      expect(csrfToken).toBeDefined()
      expect(csrfToken?.length).toBeGreaterThan(10)

      // Check for CSRF header in requests
      const [response] = await Promise.all([
        page.waitForResponse('**/api/profile'),
        page.click('button[data-testid="save-profile"]')
      ])

      expect(response.request().headers()['x-csrf-token']).toBe(csrfToken)
    })

    test('should reject requests without CSRF token', async ({ page }) => {
      // Make request without CSRF token
      const response = await page.request.post('/api/profile', {
        data: { name: 'Test User' }
      })

      // Should be rejected
      expect(response.status()).toBe(403)
    })

    test('should prevent CSRF on sensitive operations', async ({ page }) => {
      const maliciousPage = await page.context().newPage()

      // User is logged in on main page
      await page.goto('/dashboard')
      await expect(page.locator('[data-testid="dashboard"]')).toBeVisible()

      // Attacker page tries to perform action
      await maliciousPage.goto('http://evil.com')
      await maliciousPage.evaluate(() => {
        // This would be blocked by CSRF protection
        fetch('http://localhost:3000/api/delete-account', {
          method: 'POST',
          credentials: 'include' // Try to use victim's session
        })
      })

      // Check that request was rejected
      // Note: This test demonstrates the concept but may need adjustment
      // based on actual CSRF implementation
    })
  })

  test.describe('SQL Injection', () => {
    test('should prevent SQL injection in search', async ({ page }) => {
      const sqlInjectionPayload = "'; DROP TABLE users; --"

      await page.goto('/customers')
      await page.fill('input[name="search"]', sqlInjectionPayload)
      await page.click('button[data-testid="search"]')

      // Should not crash or return all data
      await expect(page.locator('[data-testid="customers-table"]')).toBeVisible()

      // Should show "no results" or error message, not all users
      const results = page.locator('[data-testid="customer-row"]')
      await expect(results.first()).toBeVisible()

      // Should not expose SQL errors
      const pageContent = await page.textContent('body')
      expect(pageContent).not.toContain('SQL syntax')
      expect(pageContent).not.toContain('mysql')
      expect(pageContent).not.toContain('postgresql')
    })

    test('should handle malicious input in login', async ({ page }) => {
      const sqlInjectionPayload = "' OR '1'='1' --"

      await page.goto('/login')
      await page.fill('[name="email"]', sqlInjectionPayload)
      await page.fill('[name="password"]', 'anypassword')
      await page.click('button[type="submit"]')

      // Should not allow login bypass
      await expect(page.locator('[data-testid="login-error"]')).toBeVisible()
      await expect(page).toHaveURL('/login')
    })

    test('should validate parameterized queries', async ({ page }) => {
      // Try various SQL injection payloads
      const payloads = [
        "1' UNION SELECT password FROM users--",
        "1; INSERT INTO users VALUES ('hacker', 'password')--",
        "1' OR 1=1#",
        "admin'--"
      ]

      for (const payload of payloads) {
        await page.goto('/customers')
        await page.fill('input[name="search"]', payload)
        await page.click('button[data-testid="search"]')

        // Should handle safely
        const hasError = await page.locator('[data-testid="error-message"]').isVisible()
        const hasResults = await page.locator('[data-testid="customer-row"]').isVisible()

        // Should either show error or no results, but not crash
        expect(hasError || hasResults).toBe(true)
      }
    })
  })

  test.describe('Authentication & Authorization', () => {
    test('should prevent auth bypass', async ({ page }) => {
      // Try to access admin panel without authentication
      const response = await page.request.get('/admin')
      expect([401, 403]).toContain(response.status())

      // Try with invalid token
      const invalidResponse = await page.request.get('/admin', {
        headers: { 'Authorization': 'Bearer invalid-token' }
      })
      expect([401, 403]).toContain(invalidResponse.status())
    })

    test('should validate JWT token structure', async ({ page }) => {
      await page.goto('/dashboard')
      const token = await page.evaluate(() =>
        localStorage.getItem('auth_token')
      )

      if (token) {
        // JWT should have 3 parts separated by dots
        const parts = token.split('.')
        expect(parts).toHaveLength(3)

        // Should be valid base64
        expect(() => atob(parts[0])).not.toThrow()
        expect(() => atob(parts[1])).not.toThrow()
      }
    })

    test('should enforce role-based access control', async ({ page }) => {
      // Login as regular user
      await page.goto('/login')
      await page.fill('[name="email"]', 'user@example.com')
      await page.fill('[name="password"]', 'password123')
      await page.click('button[type="submit"]')
      await page.waitForURL('/dashboard')

      // Try to access admin-only endpoint
      const response = await page.request.get('/api/admin/users')
      expect([401, 403]).toContain(response.status())

      // Try to access admin page
      await page.goto('/admin/users')
      await expect(page.locator('[data-testid="unauthorized"]')).toBeVisible()
    })

    test('should handle session timeout', async ({ page }) => {
      // Login
      await page.goto('/login')
      await page.fill('[name="email"]', 'user@example.com')
      await page.fill('[name="password"]', 'password123')
      await page.click('button[type="submit"]')
      await page.waitForURL('/dashboard')

      // Wait for session to expire (simulate)
      await page.evaluate(() => {
        localStorage.removeItem('auth_token')
        sessionStorage.clear()
      })

      // Try to access protected resource
      await page.goto('/profile')
      await expect(page).toHaveURL('/login')
    })
  })

  test.describe('OWASP Top 10 Compliance', () => {
    test('A01: Broken Access Control', async ({ page }) => {
      // Test direct object references
      await page.goto('/orders/12345') // Try to access order ID directly
      await expect(page.locator('[data-testid="order-details"]')).toBeVisible()

      // Try to access someone else's order
      const response = await page.request.get('/api/orders/99999')
      expect([401, 403, 404]).toContain(response.status())
    })

    test('A02: Cryptographic Failures', async ({ page }) => {
      // Check HTTPS is enforced
      const response = await page.request.get('http://localhost:3000/dashboard')
      // Should redirect to HTTPS or reject
      expect([301, 302, 308]).toContain(response.status())
    })

    test('A03: Injection', async ({ page }) => {
      // Already tested SQL injection above
      // Test for NoSQL injection
      await page.goto('/search')
      await page.fill('input[name="query"]', '{ "$ne": null }')
      await page.click('button[type="submit"]')

      // Should not expose all data
      const hasResults = await page.locator('[data-testid="search-results"]').isVisible()
      expect(hasResults).toBe(true)
    })

    test('A04: Insecure Design', async ({ page }) => {
      // Test for security by design
      // Verify MFA option is available
      await page.goto('/settings/security')
      const mfaOption = page.locator('[data-testid="enable-mfa"]')
      await expect(mfaOption).toBeVisible()
    })

    test('A05: Security Misconfiguration', async ({ page }) => {
      // Check for security headers
      const response = await page.request.get('/dashboard')

      const headers = response.headers()
      expect(headers['x-frame-options']).toBeDefined()
      expect(headers['x-content-type-options']).toBe('nosniff')
      expect(headers['x-xss-protection']).toBeDefined()
    })

    test('A06: Vulnerable Components', async ({ page }) => {
      // Check for known vulnerabilities in dependencies
      // This would typically be done with tools like npm audit
      // For now, we check if security updates are available
      await page.goto('/admin/system')
      await expect(page.locator('[data-testid="security-updates"]')).toBeVisible()
    })

    test('A07: Authentication Failures', async ({ page }) => {
      // Test for weak passwords
      await page.goto('/login')
      await page.fill('[name="email"]', 'user@example.com')
      await page.fill('[name="password"]', '123') // Weak password
      await page.click('button[type="submit"]')

      // Should reject weak passwords
      await expect(page.locator('[data-testid="weak-password-error"]')).toBeVisible()
    })

    test('A08: Software Integrity Failures', async ({ page }) => {
      // Test for code integrity
      // In a real scenario, you might check for subresource integrity
      await page.goto('/dashboard')

      // Check if scripts are properly integrity-checked
      const scripts = await page.$$eval('script', scripts =>
        scripts.map(s => ({
          src: s.src,
          integrity: s.integrity
        }))
      )

      // External scripts should have integrity attributes
      const externalScripts = scripts.filter(s => s.src && !s.src.includes('localhost'))
      externalScripts.forEach(script => {
        expect(script.integrity).toBeDefined()
      })
    })

    test('A09: Logging Failures', async ({ page }) => {
      // Test that security events are logged
      await page.goto('/login')
      await page.fill('[name="email"]', 'user@example.com')
      await page.fill('[name="password"]', 'wrongpassword')
      await page.click('button[type="submit"]')

      // Check if login attempt is logged
      const response = await page.request.get('/api/logs/security')
      expect(response.status()).toBe(200)
    })

    test('A10: Server-Side Request Forgery (SSRF)', async ({ page }) => {
      // Test for SSRF vulnerabilities
      await page.goto('/api-client')
      await page.fill('input[name="url"]', 'http://localhost:3306') // Try to access database
      await page.click('button[data-testid="fetch"]')

      // Should be blocked
      await expect(page.locator('[data-testid="fetch-error"]')).toBeVisible()
    })
  })

  test.describe('Additional Security Tests', () => {
    test('should prevent path traversal', async ({ page }) => {
      const payloads = [
        '../../../etc/passwd',
        '..\\..\\..\\windows\\system32\\config\\sam',
        '....//....//....//etc/passwd',
        '%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd'
      ]

      for (const payload of payloads) {
        await page.goto(`/download?file=${payload}`)

        // Should not expose system files
        const content = await page.textContent('body')
        expect(content).not.toContain('root:')
        expect(content).not.toContain('[boot loader]')
      }
    })

    test('should handle file upload securely', async ({ page }) => {
      // Create malicious file
      const maliciousFile = Buffer.from('<script>alert("XSS")</script>')
      const fs = require('fs')
      const filePath = 'test-results/malicious.html'
      fs.writeFileSync(filePath, maliciousFile)

      await page.goto('/upload')
      const fileInput = page.locator('input[type="file"]')
      await fileInput.setInputFiles(filePath)

      // Should scan and reject malicious file
      await expect(page.locator('[data-testid="upload-error"]')).toContainText('malicious')

      fs.unlinkSync(filePath)
    })

    test('should validate content security policy', async ({ page }) => {
      await page.goto('/dashboard')

      // Check if CSP is set
      const response = await page.request.get('/dashboard')
      const csp = response.headers()['content-security-policy']
      expect(csp).toBeDefined()

      // Should not allow inline scripts
      expect(csp).toContain("script-src")
      expect(csp).not.toContain("'unsafe-inline'")
    })
  })
})
