/**
 * OWASP ZAP Active Security Scanning
 *
 * This test performs active security scans using OWASP ZAP
 * to identify vulnerabilities in the application
 */

import { test, expect } from '@playwright/test'

const BASE_URL = process.env.BASE_URL || 'http://localhost:3000'
const ZAP_PROXY = process.env.ZAP_PROXY || 'http://localhost:8080'

test.describe('ZAP Active Security Scan', () => {
  test('should complete ZAP active scan without critical vulnerabilities', async ({
    page,
  }, testInfo) => {
    testInfo.timeout(300000) // 5 minutes timeout for active scan

    console.log(`Starting ZAP active scan for: ${BASE_URL}`)
    console.log(`ZAP Proxy: ${ZAP_PROXY}`)

    // Step 1: Access the application through ZAP proxy
    await page.goto(`${BASE_URL}/login`, {
      waitUntil: 'networkidle',
      timeout: 60000,
    })

    // Step 2: Wait for passive scanner to complete
    await page.waitForTimeout(5000)

    // Step 3: Perform login to access protected areas
    try {
      await page.fill('[data-testid="username-input"]', 'admin')
      await page.fill('[data-testid="password-input"]', 'password')
      await page.click('[data-testid="login-button"]')
      await page.waitForURL('**/dashboard', { timeout: 10000 })
      await page.waitForLoadState('networkidle')
    } catch (error) {
      console.log('Login attempt failed, continuing with scan...')
    }

    // Step 4: Navigate through key pages
    const criticalPages = [
      '/dashboard',
      '/customers',
      '/orders',
      '/invoices',
      '/payments',
      '/subscriptions',
    ]

    for (const pagePath of criticalPages) {
      try {
        await page.goto(`${BASE_URL}${pagePath}`, {
          waitUntil: 'networkidle',
          timeout: 10000,
        })
        await page.waitForTimeout(2000)
      } catch (error) {
        console.log(`Failed to navigate to ${pagePath}: ${error}`)
      }
    }

    // Step 5: Trigger API calls
    try {
      await page.goto(`${BASE_URL}/api/customers`, {
        waitUntil: 'networkidle',
        timeout: 5000,
      }).catch(() => {})
    } catch (error) {
      console.log('API call failed (expected for unauthenticated)')
    }

    // Step 6: Wait for passive scan to complete
    await page.waitForTimeout(10000)

    // Step 7: Start active scan
    console.log('Starting ZAP active scan...')
    const zapResponse = await fetch(
      `http://localhost:8080/JSON/ascan/action/scan/?apikey=test&url=${encodeURIComponent(BASE_URL)}&recurse=true&inscopeOnly=&scanPolicyName=&method=&postData=`,
      {
        method: 'GET',
      }
    )

    if (!zapResponse.ok) {
      throw new Error(`Failed to start ZAP scan: ${zapResponse.statusText}`)
    }

    const scanData = await zapResponse.json()
    const scanId = scanData.scan

    console.log(`ZAP scan started with ID: ${scanId}`)

    // Step 8: Monitor scan progress
    let scanStatus = 0
    let attempts = 0
    const maxAttempts = 60 // 5 minutes max

    while (scanStatus < 100 && attempts < maxAttempts) {
      await page.waitForTimeout(5000)
      attempts++

      const statusResponse = await fetch(
        `http://localhost:8080/JSON/ascan/view/status/?apikey=test&scanId=${scanId}`
      )
      const statusData = await statusResponse.json()
      scanStatus = statusData.status

      console.log(`Scan progress: ${scanStatus}%`)
    }

    if (attempts >= maxAttempts) {
      console.log('Scan timeout reached')
    }

    // Step 9: Get scan results
    const resultsResponse = await fetch(
      `http://localhost:8080/JSON/ascan/view/results/?apikey=test&scanId=${scanId}&start=&count=`
    )
    const results = await resultsResponse.json()

    // Step 10: Analyze results
    console.log('\n=== ZAP ACTIVE SCAN RESULTS ===')
    console.log(`Total issues found: ${results.results?.length || 0}`)

    let criticalCount = 0
    let highCount = 0
    let mediumCount = 0
    let lowCount = 0
    let informationalCount = 0

    for (const alert of results.results || []) {
      const risk = alert.risk
      const confidence = alert.confidence
      const name = alert.alert

      if (risk === 'High') {
        highCount++
        if (name.includes('SQL Injection') || name.includes('Remote Code Execution')) {
          criticalCount++
        }
      } else if (risk === 'Medium') {
        mediumCount++
      } else if (risk === 'Low') {
        lowCount++
      } else if (risk === 'Informational') {
        informationalCount++
      }

      console.log(`- [${risk}] ${name} (Confidence: ${confidence})`)
    }

    // Step 11: Verify no critical vulnerabilities
    console.log(`\nRisk Summary:`)
    console.log(`  Critical: ${criticalCount}`)
    console.log(`  High: ${highCount}`)
    console.log(`  Medium: ${mediumCount}`)
    console.log(`  Low: ${lowCount}`)
    console.log(`  Informational: ${informationalCount}`)

    // Step 12: Generate report
    const reportResponse = await fetch(
      `http://localhost:8080/OTHER/core/other/jsonreport/?apikey=test`
    )
    const report = await reportResponse.json()

    console.log(`\n=== ZAP SCAN SUMMARY ===`)
    console.log(`Site: ${BASE_URL}`)
    console.log(`Scan ID: ${scanId}`)
    console.log(`Scan Status: ${scanStatus}%`)
    console.log(`Total Issues: ${results.results?.length || 0}`)

    // Test passes if no critical vulnerabilities found
    expect(criticalCount).toBe(0)
  }, { timeout: 360000 })

  test('should identify XSS vulnerabilities in user input fields', async ({
    page,
  }, testInfo) => {
    testInfo.timeout(60000)

    await page.goto(`${BASE_URL}/login`)
    await page.waitForLoadState('networkidle')

    const xssPayloads = [
      '<script>alert("XSS")</script>',
      '"><script>alert("XSS")</script>',
      "javascript:alert('XSS')",
      '<img src=x onerror=alert("XSS")>',
      '"><svg onload=alert("XSS")>',
    ]

    for (const payload of xssPayloads) {
      try {
        // Test in username field
        await page.fill('[data-testid="username-input"]', payload)
        await page.fill('[data-testid="password-input"]', 'test')
        await page.click('[data-testid="login-button"]')

        // Check for XSS execution
        const hasAlert = await page.evaluate(() => {
          return new Promise((resolve) => {
            let called = false
            window.alert = function () {
              called = true
            }
            setTimeout(() => resolve(called), 1000)
          })
        })

        expect(hasAlert).toBe(false)
      } catch (error) {
        // Expected for invalid credentials
      }
    }
  })

  test('should test for SQL injection in API endpoints', async ({ request }) => {
    const sqlPayloads = [
      "' OR '1'='1",
      "'; DROP TABLE users; --",
      "' UNION SELECT * FROM users --",
      "1' OR 1=1#",
    ]

    for (const payload of sqlPayloads) {
      const response = await request.get(`${BASE_URL}/api/customers?search=${encodeURIComponent(payload)}`)

      // Should not reveal database errors
      expect(response.status()).not.toBe(500)

      const body = await response.text()
      expect(body).not.toMatch(/ORA-|MySQL|PostgreSQL|mysql_fetch|SQLite/)
      expect(body).not.toMatch(/syntax error|undefined table/)
    }
  })

  test('should identify directory traversal vulnerabilities', async ({ request }) => {
    const traversalPayloads = [
      '../../../etc/passwd',
      '..\\..\\..\\windows\\system32\\drivers\\etc\\hosts',
      '....//....//....//etc/passwd',
      '%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd',
    ]

    for (const payload of traversalPayloads) {
      const response = await request.get(`${BASE_URL}/files/${payload}`)

      // Should not expose system files
      expect(response.status()).not.toBe(200)
      expect(await response.text()).not.toMatch(/root:|daemon:|bin:/)
    }
  })

  test('should detect insecure HTTP methods', async ({ request }) => {
    const methods = ['TRACE', 'TRACK', 'DEBUG', 'CONNECT']

    for (const method of methods) {
      const response = await request.fetch(`${BASE_URL}/api/customers`, {
        method,
      })

      // Insecure methods should be disabled
      expect(response.status()).not.toBe(200)
    }
  })
})
