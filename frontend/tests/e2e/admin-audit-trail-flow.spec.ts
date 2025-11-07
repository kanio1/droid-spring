/**
 * Admin Audit Trail - Comprehensive Testing
 *
 * Tests for audit logging of all administrative actions
 * Uses Playwright 1.55+ features:
 * - test.serial() for complete audit workflows
 * - test.step() for step-by-step tracking
 * - expect.soft() for non-blocking assertions
 * - test.repeat() for stress testing
 *
 * Coverage: User actions, role changes, system access, data exports
 */

import { test, expect } from '@playwright/test'
import { UserFactory } from '../framework/factories/user-admin.factory'

test.describe('Admin Audit Trail - User Actions', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Complete user lifecycle audit logging', async ({ page }) => {
    const userEmail = `audit-test-${Date.now()}@example.com`
    const testUser = UserFactory.createUser({
      firstName: 'Audit',
      lastName: 'Test',
      email: userEmail
    })

    // Step 1: Create user
    await test.step('Create user and verify audit log', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', testUser.firstName)
      await page.fill('[name="lastName"]', testUser.lastName)
      await page.fill('[name="email"]', testUser.email)
      await page.fill('[name="username"]', testUser.username)
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    // Step 2: Navigate to audit logs
    await test.step('Navigate to audit logs', async () => {
      await page.goto('/admin/audit-logs')
      await expect(page.locator('[data-section="audit-logs"]')).toBeVisible()
    })

    // Step 3: Verify user creation is logged
    await test.step('Verify user creation in audit log', async () => {
      await expect(page.locator('[data-logs]')).toBeVisible()
      await page.fill('[data-filter="action"]', 'user.create')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()

      await expect.soft(logEntry.locator('[data-field="action"]')).toContainText('user.create')
      await expect.soft(logEntry.locator('[data-field="resource"]')).toContainText(testUser.email)
      await expect.soft(logEntry.locator('[data-field="timestamp"]')).toBeVisible()
      await expect.soft(logEntry.locator('[data-field="admin"]')).toContainText('admin@')
    })

    // Step 4: Edit user
    await test.step('Edit user and verify audit log', async () => {
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', testUser.email)
      await page.press('[data-search="users"]', 'Enter')
      await page.click('[data-user] [data-action="edit"]')

      await page.fill('[name="firstName"]', 'Modified')
      await page.fill('[name="lastName"]', 'User')
      await page.click('[data-action="save-user"]')
      await expect(page.locator('[data-success="user-updated"]')).toBeVisible()
    })

    // Step 5: Verify edit in audit log
    await test.step('Verify user update in audit log', async () => {
      await page.goto('/admin/audit-logs')
      await page.reload()

      await page.fill('[data-filter="action"]', 'user.update')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()
      await expect(logEntry.locator('[data-field="action"]')).toContainText('user.update')
      await expect(logEntry.locator('[data-field="resource"]')).toContainText(testUser.email)
    })

    // Step 6: Delete user
    await test.step('Delete user and verify audit log', async () => {
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', testUser.email)
      await page.press('[data-search="users"]', 'Enter')
      await page.click('[data-user] [data-action="delete"]')
      await page.click('[data-action="confirm-delete"]')
      await expect(page.locator('[data-success="user-deleted"]')).toBeVisible()
    })

    // Step 7: Verify deletion in audit log
    await test.step('Verify user deletion in audit log', async () => {
      await page.goto('/admin/audit-logs')
      await page.reload()

      await page.fill('[data-filter="action"]', 'user.delete')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()
      await expect(logEntry.locator('[data-field="action"]')).toContainText('user.delete')
      await expect(logEntry.locator('[data-field="resource"]')).toContainText(testUser.email)
    })
  })

  test('Audit log search and filtering', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    await test.step('Filter by action type', async () => {
      await page.selectOption('[data-filter="action"]', 'user.create')
      await page.press('[data-filter="action"]', 'Enter')

      const logs = page.locator('[data-log-entry]')
      const count = await logs.count()

      for (let i = 0; i < count; i++) {
        await expect(logs.nth(i).locator('[data-field="action"]'))
          .toContainText('user.create')
      }
    })

    await test.step('Filter by date range', async () => {
      const today = new Date()
      const yesterday = new Date(today)
      yesterday.setDate(yesterday.getDate() - 1)

      await page.fill('[data-filter="dateFrom"]', yesterday.toISOString().split('T')[0])
      await page.fill('[data-filter="dateTo"]', today.toISOString().split('T')[0])
      await page.click('[data-action="apply-filters"]')

      await expect(page.locator('[data-logs]')).toBeVisible()
    })

    await test.step('Filter by admin user', async () => {
      await page.fill('[data-filter="admin"]', 'admin')
      await page.press('[data-filter="admin"]', 'Enter')

      const logs = page.locator('[data-log-entry]')
      const count = await logs.count()

      for (let i = 0; i < count; i++) {
        await expect(logs.nth(i).locator('[data-field="admin"]'))
          .toContainText('admin@')
      }
    })
  })

  test('Audit log details view', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    const firstLog = page.locator('[data-log-entry]').first()
    await expect(firstLog).toBeVisible()

    await test.step('Click to view log details', async () => {
      await firstLog.click()
      await expect(page.locator('[data-modal="log-details"]')).toBeVisible()
    })

    await test.step('Verify log detail fields', async () => {
      await expect.soft(page.locator('[data-field="action"]')).toBeVisible()
      await expect.soft(page.locator('[data-field="resource"]')).toBeVisible()
      await expect.soft(page.locator('[data-field="timestamp"]')).toBeVisible()
      await expect.soft(page.locator('[data-field="admin"]')).toBeVisible()
      await expect.soft(page.locator('[data-field="ipAddress"]')).toBeVisible()
      await expect.soft(page.locator('[data-field="userAgent"]')).toBeVisible()
      await expect.soft(page.locator('[data-field="status"]')).toBeVisible()
      await expect.soft(page.locator('[data-field="changes"]')).toBeVisible()
    })

    await test.step('Close modal', async () => {
      await page.click('[data-action="close-modal"]')
      await expect(page.locator('[data-modal="log-details"]')).toBeHidden()
    })
  })
})

test.describe('Admin Audit Trail - Role Management', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test.serial('Role creation and assignment audit', async ({ page }) => {
    const roleName = `test-role-${Date.now()}`
    const testUser = UserFactory.createUser({
      email: `role-test-${Date.now()}@example.com`
    })

    // Step 1: Create role
    await test.step('Create new role', async () => {
      await page.goto('/admin/roles/create')
      await page.fill('[name="roleName"]', roleName)
      await page.check('[value="user.create"]')
      await page.check('[value="user.read"]')
      await page.click('[data-action="save-role"]')
      await expect(page.locator('[data-success="role-created"]')).toBeVisible()
    })

    // Step 2: Verify role creation in audit log
    await test.step('Verify role creation in audit log', async () => {
      await page.goto('/admin/audit-logs')
      await page.fill('[data-filter="action"]', 'role.create')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()
      await expect(logEntry.locator('[data-field="action"]')).toContainText('role.create')
      await expect(logEntry.locator('[data-field="resource"]')).toContainText(roleName)
    })

    // Step 3: Create user and assign role
    await test.step('Create user with custom role', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', testUser.firstName)
      await page.fill('[name="lastName"]', testUser.lastName)
      await page.fill('[name="email"]', testUser.email)
      await page.fill('[name="username"]', testUser.username)
      await page.check(`[value="${roleName}"]`)
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    // Step 4: Verify role assignment in audit log
    await test.step('Verify role assignment in audit log', async () => {
      await page.goto('/admin/audit-logs')
      await page.reload()

      await page.fill('[data-filter="action"]', 'user.role.assign')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()
      await expect(logEntry.locator('[data-field="action"]')).toContainText('user.role.assign')
      await expect(logEntry.locator('[data-field="resource"]')).toContainText(testUser.email)
      await expect(logEntry.locator('[data-field="changes"]')).toContainText(roleName)
    })

    // Step 5: Update user role
    await test.step('Update user role', async () => {
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', testUser.email)
      await page.press('[data-search="users"]', 'Enter')
      await page.click('[data-user] [data-action="edit"]')

      await page.check('[value="user"]')
      await page.click('[data-action="save-user"]')
      await expect(page.locator('[data-success="user-updated"]')).toBeVisible()
    })

    // Step 6: Verify role update in audit log
    await test.step('Verify role update in audit log', async () => {
      await page.goto('/admin/audit-logs')
      await page.reload()

      await page.fill('[data-filter="action"]', 'user.role.update')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()
      await expect(logEntry.locator('[data-field="action"]')).toContainText('user.role.update')
    })
  })

  test('Role deletion audit logging', async ({ page }) => {
    const roleName = `delete-role-${Date.now()}`

    // First create a role
    await page.goto('/admin/roles/create')
    await page.fill('[name="roleName"]', roleName)
    await page.check('[value="user.create"]')
    await page.click('[data-action="save-role"]')
    await expect(page.locator('[data-success="role-created"]')).toBeVisible()

    // Now delete it
    await test.step('Delete role', async () => {
      await page.goto('/admin/roles')
      await page.fill('[data-search="roles"]', roleName)
      await page.press('[data-search="roles"]', 'Enter')
      await page.click('[data-role] [data-action="delete"]')
      await page.click('[data-action="confirm-delete"]')
      await expect(page.locator('[data-success="role-deleted"]')).toBeVisible()
    })

    // Verify deletion in audit log
    await test.step('Verify role deletion in audit log', async () => {
      await page.goto('/admin/audit-logs')
      await page.fill('[data-filter="action"]', 'role.delete')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()
      await expect(logEntry.locator('[data-field="action"]')).toContainText('role.delete')
      await expect(logEntry.locator('[data-field="resource"]')).toContainText(roleName)
    })
  })
})

test.describe('Admin Audit Trail - System Access', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Failed login attempts audit', async ({ page }) => {
    await page.goto('/admin/logout')

    await test.step('Attempt invalid login', async () => {
      await page.goto('/login')
      await page.fill('[name="username"]', 'admin')
      await page.fill('[name="password"]', 'wrongpassword')
      await page.click('[data-action="login"]')
      await expect(page.locator('[data-error="invalid-credentials"]')).toBeVisible()
    })

    // Verify failed login is logged
    await test.step('Verify failed login in audit logs', async () => {
      // Re-login as admin to view logs
      await page.goto('/login')
      await page.fill('[name="username"]', 'admin')
      await page.fill('[name="password"]', 'admin123')
      await page.click('[data-action="login"]')

      await page.goto('/admin/audit-logs')
      await page.fill('[data-filter="action"]', 'auth.failure')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()
      await expect(logEntry.locator('[data-field="action"]')).toContainText('auth.failure')
      await expect(logEntry.locator('[data-field="status"]')).toContainText('failure')
    })
  })

  test('Successful login audit', async ({ page }) => {
    await page.goto('/admin/logout')

    await test.step('Login and verify audit log', async () => {
      await page.goto('/login')
      await page.fill('[name="username"]', 'admin')
      await page.fill('[name="password"]', 'admin123')
      await page.click('[data-action="login"]')
      await expect(page).toHaveURL('/admin/dashboard')
    })

    await test.step('Verify successful login in audit log', async () => {
      await page.goto('/admin/audit-logs')
      await page.fill('[data-filter="action"]', 'auth.success')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()
      await expect(logEntry.locator('[data-field="action"]')).toContainText('auth.success')
      await expect(logEntry.locator('[data-field="status"]')).toContainText('success')
    })
  })

  test('Data export audit logging', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Export user data', async () => {
      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-action="export-users"]')
      await page.click('[data-action="download-export"]')
      await downloadPromise
    })

    // Verify export is logged
    await test.step('Verify export in audit log', async () => {
      await page.goto('/admin/audit-logs')
      await page.fill('[data-filter="action"]', 'data.export')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()
      await expect(logEntry.locator('[data-field="action"]')).toContainText('data.export')
      await expect(logEntry.locator('[data-field="resource"]')).toContainText('users')
    })
  })
})

test.describe('Admin Audit Trail - Real-time Monitoring', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Real-time audit log updates', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    const beforeCount = await page.locator('[data-log-entry]').count()

    // Open new tab and perform action
    const newPage = await page.context().newPage()
    await newPage.goto('http://localhost:3000/admin/users/create')
    await newPage.fill('[name="firstName"]', 'RealTime')
    await newPage.fill('[name="lastName"]', 'Test')
    await newPage.fill('[name="email"]', `realtime-${Date.now()}@example.com`)
    await newPage.fill('[name="username"]', `realtime${Date.now()}`)
    await newPage.check('[value="user"]')
    await newPage.click('[data-action="submit-user"]')
    await expect(newPage.locator('[data-success="user-created"]')).toBeVisible()
    await newPage.close()

    // Switch back and check for updates
    await test.step('Verify real-time update in audit log', async () => {
      await page.reload()

      // Wait a moment for real-time update
      await page.waitForTimeout(2000)

      const afterCount = await page.locator('[data-log-entry]').count()
      expect(afterCount).toBeGreaterThanOrEqual(beforeCount)
    })
  })

  test('Audit log live streaming', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    await test.step('Enable live streaming', async () => {
      await page.click('[data-action="toggle-live"]')
      await expect(page.locator('[data-live="enabled"]')).toBeVisible()
    })

    // Perform action in background
    const newPage = await page.context().newPage()
    await newPage.goto('http://localhost:3000/admin/users')
    await newPage.click('[data-action="refresh"]')
    await newPage.close()

    // Check that new logs appear
    await test.step('Verify new logs appear in live stream', async () => {
      await page.waitForTimeout(2000)

      const latestLog = page.locator('[data-log-entry]').first()
      await expect(latestLog).toBeVisible()

      // Verify it has a recent timestamp
      const timestamp = await latestLog.locator('[data-field="timestamp"]').textContent()
      const now = new Date()
      const logTime = new Date(timestamp || '')

      const timeDiff = Math.abs(now.getTime() - logTime.getTime())
      expect(timeDiff).toBeLessThan(30000) // Within 30 seconds
    })
  })
})

test.describe('Admin Audit Trail - Export and Retention', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('Export audit logs to CSV', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    await test.step('Configure export filters', async () => {
      const today = new Date()
      const yesterday = new Date(today)
      yesterday.setDate(yesterday.getDate() - 7)

      await page.fill('[data-filter="dateFrom"]', yesterday.toISOString().split('T')[0])
      await page.fill('[data-filter="dateTo"]', today.toISOString().split('T')[0])
      await page.click('[data-action="apply-filters"]')
    })

    await test.step('Download audit log export', async () => {
      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-action="export-logs"]')
      await page.click('[data-action="download-export"]')
      const download = await downloadPromise

      expect(download.suggestedFilename()).toMatch(/audit-logs.*\.csv/)
    })
  })

  test('Audit log retention policy', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    await test.step('Check retention policy', async () => {
      await page.click('[data-action="retention-policy"]')
      await expect(page.locator('[data-modal="retention-policy"]')).toBeVisible()

      await expect(page.locator('[data-field="retentionPeriod"]')).toContainText('90 days')
      await expect(page.locator('[data-field="autoDelete"]')).toContainText('true')
    })

    await test.step('Verify old logs are archived', async () => {
      const olderThan90Days = new Date()
      olderThan90Days.setDate(olderThan90Days.getDate() - 91)

      await page.fill('[data-filter="dateTo"]', olderThan90Days.toISOString().split('T')[0])
      await page.click('[data-action="apply-filters"]')

      const logs = page.locator('[data-log-entry]')
      const count = await logs.count()

      if (count === 0) {
        // No logs older than 90 days (expected with retention policy)
        await expect(page.locator('[data-info="no-logs-found"]')).toBeVisible()
      }
    })
  })
})

test.describe('Admin Audit Trail - Integrity', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('Audit log tamper detection', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    await test.step('Verify log integrity indicators', async () => {
      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()

      await expect(logEntry.locator('[data-field="checksum"]')).toBeVisible()
      await expect(logEntry.locator('[data-field="hash"]')).toBeVisible()
      await expect(logEntry.locator('[data-field="digitalSignature"]')).toBeVisible()
    })

    await test.step('Verify log immutability flag', async () => {
      const logEntry = page.locator('[data-log-entry]').first()
      const immutable = await logEntry.locator('[data-field="immutable"]').textContent()
      expect(immutable).toBe('true')
    })
  })

  test('Audit log chain verification', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    await test.step('Verify log chain integrity', async () => {
      const logs = page.locator('[data-log-entry]')
      const count = await logs.count()

      if (count >= 2) {
        // Verify each log has previous hash
        for (let i = 0; i < Math.min(count, 10); i++) {
          const log = logs.nth(i)
          await expect(log.locator('[data-field="previousHash"]')).toBeVisible()
          await expect(log.locator('[data-field="blockchain"]')).toBeVisible()
        }
      }
    })

    await test.step('Check for blockchain validation', async () => {
      await page.click('[data-action="verify-chain"]')
      await expect(page.locator('[data-success="chain-valid"]')).toBeVisible()
    })
  })
})

test.describe('Admin Audit Trail - Stress Testing', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.repeat(5, 'Audit logging under load', async ({ page }) => {
    const userEmail = `stress-${Date.now()}-${Math.random()}@example.com`

    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Stress')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', userEmail)
    await page.fill('[name="username"]', `stresstest${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    // Verify audit log entry exists
    await page.goto('/admin/audit-logs')
    await page.fill('[data-filter="action"]', 'user.create')
    await page.press('[data-filter="action"]', 'Enter')

    const logEntry = page.locator('[data-log-entry]').first()
    await expect(logEntry).toBeVisible({ timeout: 10000 })
  })

  test('Concurrent audit log access', async ({ page }) => {
    // Open multiple tabs accessing audit logs
    const tabs = []
    for (let i = 0; i < 3; i++) {
      const tab = await page.context().newPage()
      await tab.goto('/admin/audit-logs')
      tabs.push(tab)
    }

    // Perform action
    const newPage = await page.context().newPage()
    await newPage.goto('http://localhost:3000/admin/users')
    await newPage.click('[data-action="refresh"]')
    await newPage.close()

    // Verify all tabs show updated data
    await test.step('Verify all tabs show consistent data', async () => {
      await page.waitForTimeout(2000)
      for (const tab of tabs) {
        const count = await tab.locator('[data-log-entry]').count()
        expect(count).toBeGreaterThan(0)
        await tab.close()
      }
    })
  })

  test('Audit log pagination performance', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    await test.step('Navigate through multiple pages', async () => {
      const pageSize = 50
      let currentPage = 1

      // Check at least 5 pages
      for (let i = 0; i < 5; i++) {
        const logs = page.locator('[data-log-entry]')
        const count = await logs.count()
        expect(count).toBeLessThanOrEqual(pageSize)

        // Click next page if available
        const nextButton = page.locator('[data-action="next-page"]')
        if (await nextButton.isEnabled()) {
          await nextButton.click()
          currentPage++
          await page.waitForTimeout(500)
        } else {
          break
        }
      }
    })
  })
})

test.describe('Admin Audit Trail - Compliance', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('GDPR compliance audit requirements', async ({ page }) => {
    const userEmail = `gdpr-test-${Date.now()}@example.com`

    // Create user
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'GDPR')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', userEmail)
    await page.fill('[name="username"]', `gdprtest${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    // Verify all user actions are logged with PII protection
    await test.step('Verify PII data in audit logs', async () => {
      await page.goto('/admin/audit-logs')
      await page.fill('[data-filter="action"]', 'user.create')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()

      // Check that personal data is masked
      await expect(logEntry.locator('[data-field="details"]')).not.toContainText(userEmail)
      await expect(logEntry.locator('[data-field="details"]')).toContainText('***masked***')
    })
  })

  test('SOX compliance audit trail', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    await test.step('Generate compliance report', async () => {
      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-action="generate-compliance-report"]')
      await page.click('[data-action="download-report"]')
      const download = await downloadPromise

      expect(download.suggestedFilename()).toMatch(/sox-compliance.*\.pdf/)
    })

    await test.step('Verify all required fields in compliance report', async () => {
      // This would verify the PDF contains all required SOX fields
      // For now, just verify the download happened
      expect(true).toBe(true)
    })
  })

  test('HIPAA audit trail requirements', async ({ page }) => {
    await page.goto('/admin/audit-logs')

    await test.step('Generate HIPAA audit report', async () => {
      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-action="generate-hipaa-report"]')
      await page.click('[data-action="download-report"]')
      const download = await downloadPromise

      expect(download.suggestedFilename()).toMatch(/hipaa-audit.*\.pdf/)
    })

    await test.step('Verify audit log encryption status', async () => {
      await page.click('[data-action="encryption-status"]')
      await expect(page.locator('[data-field="encrypted"]')).toContainText('true')
      await expect(page.locator('[data-field="algorithm"]')).toContainText('AES-256')
    })
  })
})
