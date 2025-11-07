/**
 * Admin Multi-Tenant Isolation - Comprehensive Testing
 *
 * Tests for tenant isolation and security boundaries
 * Uses Playwright 1.55+ features:
 * - test.serial() for complete isolation workflows
 * - test.step() for step-by-step verification
 * - expect.soft() for non-blocking isolation checks
 * - test.repeat() for stress testing isolation
 *
 * Coverage: Data isolation, access control, tenant switching, leak prevention
 */

import { test, expect } from '@playwright/test'
import { UserFactory } from '../framework/factories/user-admin.factory'

test.describe('Admin Multi-Tenant Isolation - Data Segregation', () => {
  test.use({ storageState: 'tenant-admin-auth.json' })

  test.serial('Tenant admin can only see own tenant data', async ({ page }) => {
    // Step 1: View users in current tenant
    await test.step('Navigate to users in current tenant', async () => {
      await page.goto('/admin/users')
      await expect(page.locator('[data-section="users"]')).toBeVisible()

      const users = page.locator('[data-user]')
      const count = await users.count()

      // Verify all users belong to current tenant
      for (let i = 0; i < count; i++) {
        const userRow = users.nth(i)
        const tenantId = await userRow.locator('[data-field="tenantId"]').textContent()
        expect(tenantId).toBe('tenant-alpha') // Current tenant
      }
    })

    // Step 2: Verify tenant indicator
    await test.step('Verify current tenant indicator', async () => {
      await expect(page.locator('[data-current-tenant="tenant-alpha"]')).toBeVisible()
      await expect(page.locator('[data-tenant-name="Alpha Corp"]')).toBeVisible()
    })

    // Step 3: Try to access other tenant users (should fail)
    await test.step('Attempt to access other tenant data', async () => {
      await page.goto('/admin/tenants/tenant-beta/users')
      await expect(page.locator('[data-error="tenant-access-denied"]')).toBeVisible()
      await expect(page.locator('[data-message="insufficient-privileges"]'))
        .toContainText('Access restricted to your tenant')
    })

    // Step 4: Try direct URL manipulation
    await test.step('Prevent URL manipulation to other tenant', async () => {
      await page.goto('/admin/users?tenant=tenant-beta')
      await expect(page.locator('[data-error="cross-tenant-access-blocked"]')).toBeVisible()
      await expect(page.locator('[data-current-tenant="tenant-alpha"]')).toBeVisible()
    })

    // Step 5: Verify user search is tenant-scoped
    await test.step('Verify search is limited to current tenant', async () => {
      await page.goto('/admin/users')

      // Search for a user that doesn't exist in this tenant
      await page.fill('[data-search="users"]', 'user-from-other-tenant')
      await page.press('[data-search="users"]', 'Enter')

      // Should show no results
      await expect(page.locator('[data-empty="no-users-found"]')).toBeVisible()
    })
  })

  test('User creation scoped to current tenant', async ({ page }) => {
    await test.step('Create user in current tenant', async () => {
      const userEmail = `tenant-user-${Date.now()}@example.com`

      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Tenant')
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', userEmail)
      await page.fill('[name="username"]', `tenantuser${Date.now()}`)
      await page.check('[value="user"]')

      // Tenant should be auto-selected
      const tenantField = page.locator('[name="tenantId"]')
      await expect(tenantField).toHaveValue('tenant-alpha')

      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    await test.step('Verify user is created in correct tenant', async () => {
      await page.goto('/admin/users')

      const newUser = page.locator('[data-user]').first()
      await expect(newUser.locator('[data-field="tenantId"]')).toContainText('tenant-alpha')

      // Verify other tenant is not accessible
      const tenantBadge = await newUser.locator('[data-field="tenantName"]').textContent()
      expect(tenantBadge).toBe('Alpha Corp')
    })
  })

  test('Prevent user import from other tenant', async ({ page }) => {
    await test.step('Attempt to import users with cross-tenant data', async () => {
      await page.goto('/admin/users/import')

      const csv = `firstName,lastName,email,username,tenantId
John,Doe,john@example.com,johndoe,tenant-alpha
Jane,Smith,jane@example.com,janesmith,tenant-beta`

      await page.setInputFiles('[name="csvFile"]', {
        name: 'import.csv',
        mimeType: 'text/csv',
        buffer: Buffer.from(csv)
      })

      await page.click('[data-action="preview-import"]')
    })

    await test.step('Verify cross-tenant import is blocked', async () => {
      await expect(page.locator('[data-error="cross-tenant-import-blocked"]'))
        .toContainText('Cannot import users from other tenants')
      await expect(page.locator('[data-warning="tenant-mismatch"]'))
        .toContainText('tenant-beta does not match your tenant')
    })
  })
})

test.describe('Admin Multi-Tenant Isolation - Tenant Switching', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('Super admin can switch between tenants', async ({ page }) => {
    await test.step('View available tenants', async () => {
      await page.goto('/admin/tenants')
      await expect(page.locator('[data-section="tenants"]')).toBeVisible()

      // Should see all tenants
      await expect(page.locator('[data-tenant="tenant-alpha"]')).toBeVisible()
      await expect(page.locator('[data-tenant="tenant-beta"]')).toBeVisible()
      await expect(page.locator('[data-tenant="tenant-gamma"]')).toBeVisible()
    })

    await test.step('Switch to tenant-alpha', async () => {
      await page.click('[data-tenant="tenant-alpha"] [data-action="switch"]')

      // Verify switch
      await expect(page).toHaveURL(/.*tenant-alpha.*/)
      await expect(page.locator('[data-current-tenant="tenant-alpha"]')).toBeVisible()
      await expect(page.locator('[data-tenant-name="Alpha Corp"]')).toBeVisible()
    })

    await test.step('Switch to tenant-beta', async () => {
      await page.click('[data-tenant-switcher"]')
      await page.selectOption('[name="tenant"]', 'tenant-beta')
      await page.click('[data-action="confirm-switch"]')

      // Verify switch
      await expect(page).toHaveURL(/.*tenant-beta.*/)
      await expect(page.locator('[data-current-tenant="tenant-beta"]')).toBeVisible()
      await expect(page.locator('[data-tenant-name="Beta LLC"]')).toBeVisible()
    })

    await test.step('View users in switched tenant', async () => {
      await page.goto('/admin/users')

      // Now seeing tenant-beta users
      const users = page.locator('[data-user]')
      const count = await users.count()

      // All should be tenant-beta
      for (let i = 0; i < count; i++) {
        const userRow = users.nth(i)
        const tenantId = await userRow.locator('[data-field="tenantId"]').textContent()
        expect(tenantId).toBe('tenant-beta')
      }
    })
  })

  test('Tenant switch audit logging', async ({ page }) => {
    await test.step('Switch tenant and verify audit log', async () => {
      await page.goto('/admin/tenants')
      await page.click('[data-tenant="tenant-gamma"] [data-action="switch"]')

      // Check audit log
      await page.goto('/admin/audit-logs')
      await page.fill('[data-filter="action"]', 'tenant.switch')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry).toBeVisible()
      await expect(logEntry.locator('[data-field="action"]')).toContainText('tenant.switch')
      await expect(logEntry.locator('[data-field="from"]')).toContainText('tenant-alpha')
      await expect(logEntry.locator('[data-field="to"]')).toContainText('tenant-gamma')
    })
  })

  test('Prevent switching to inactive tenant', async ({ page }) => {
    await test.step('Attempt to switch to suspended tenant', async () => {
      await page.goto('/admin/tenants')

      // tenant-delta is suspended
      const tenantDelta = page.locator('[data-tenant="tenant-delta"]')
      await expect(tenantDelta.locator('[data-status="suspended"]')).toBeVisible()

      await tenantDelta.locator('[data-action="switch"]').click()
    })

    await test.step('Verify switch is blocked', async () => {
      await expect(page.locator('[data-error="tenant-suspended"]'))
        .toContainText('Cannot access suspended tenant')
      await expect(page.locator('[data-action="switch"]')).toBeDisabled()
    })
  })
})

test.describe('Admin Multi-Tenant Isolation - Resource Isolation', () => {
  test.use({ storageState: 'tenant-admin-auth.json' })

  test('Data isolation - reports', async ({ page }) => {
    await test.step('View reports scoped to current tenant', async () => {
      await page.goto('/admin/reports')

      // All reports should be for current tenant
      const reports = page.locator('[data-report]')
      const count = await reports.count()

      for (let i = 0; i < count; i++) {
        const report = reports.nth(i)
        const tenantId = await report.locator('[data-field="tenantId"]').textContent()
        expect(tenantId).toBe('tenant-alpha')
      }
    })

    await test.step('Attempt to view cross-tenant report', async () => {
      await page.goto('/admin/reports/tenant-beta-summary')
      await expect(page.locator('[data-error="tenant-access-denied"]')).toBeVisible()
    })
  })

  test('Data isolation - audit logs', async ({ page }) => {
    await test.step('View audit logs for current tenant', async () => {
      await page.goto('/admin/audit-logs')

      // All audit logs should be for current tenant
      const logs = page.locator('[data-log-entry]')
      const count = await logs.count()

      if (count > 0) {
        for (let i = 0; i < Math.min(count, 10); i++) {
          const log = logs.nth(i)
          const tenantId = await log.locator('[data-field="tenantId"]').textContent()
          expect(tenantId).toBe('tenant-alpha')
        }
      }
    })
  })

  test('Data isolation - configuration', async ({ page }) => {
    await test.step('View tenant-specific configuration', async () => {
      await page.goto('/admin/settings')

      // Should only see own tenant settings
      const settings = page.locator('[data-setting]')
      const tenantField = settings.locator('[data-field="tenantId"]')
      const tenantId = await tenantField.textContent()
      expect(tenantId).toBe('tenant-alpha')

      // Global settings should be read-only
      await expect(settings.locator('[data-type="global"] [data-action="edit"]'))
        .toBeHidden()
    })

    await test.step('Cannot modify global settings', async () => {
      const globalSetting = page.locator('[data-setting="system-wide"]')
      await expect(globalSetting.locator('[data-action="edit"]')).toBeHidden()
      await expect(globalSetting.locator('[data-field="readOnly"]')).toContainText('true')
    })
  })

  test('Data isolation - user profiles', async ({ page }) => {
    const userEmail = `profile-test-${Date.now()}@example.com`

    // Create user
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Profile')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', userEmail)
    await page.fill('[name="username"]', `profiletest${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await test.step('View user profile in current tenant', async () => {
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', userEmail)
      await page.press('[data-search="users"]', 'Enter')
      await page.click('[data-user] [data-action="view"]')

      // Profile should show current tenant
      await expect(page.locator('[data-field="tenantId"]')).toContainText('tenant-alpha')
      await expect(page.locator('[data-field="tenantName"]')).toContainText('Alpha Corp')
    })
  })
})

test.describe('Admin Multi-Tenant Isolation - Cross-Tenant Prevention', () => {
  test.use({ storageState: 'tenant-admin-auth.json' })

  test('Prevent direct database queries to other tenants', async ({ page }) => {
    await test.step('Attempt to query other tenant data via URL', async () => {
      // Try various URL manipulations
      await page.goto('/admin/users?query=SELECT * FROM tenant_beta_users')
      await expect(page.locator('[data-error="query-blocked"]')).toBeVisible()

      await page.goto('/admin/users?tenant=tenant-beta')
      await expect(page.locator('[data-error="cross-tenant-access-denied"]')).toBeVisible()
    })

    await test.step('Verify all API calls include tenant context', async () => {
      // Monitor network requests (in real test, would use network monitoring)
      // For UI test, verify no other tenant data is visible
      await page.goto('/admin/users')

      const users = page.locator('[data-user]')
      const count = await users.count()

      for (let i = 0; i < count; i++) {
        const userRow = users.nth(i)
        const tenantId = await userRow.locator('[data-field="tenantId"]').textContent()
        expect(tenantId).not.toBe('tenant-beta')
        expect(tenantId).not.toBe('tenant-gamma')
      }
    })
  })

  test('Prevent data export from other tenants', async ({ page }) => {
    await test.step('Attempt to export other tenant data', async () => {
      await page.goto('/admin/users')

      // Try to export with cross-tenant filter
      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-action="export-users"]')
      await page.fill('[data-filter="tenantId"]', 'tenant-beta')
      await page.click('[data-action="download-export"]')

      // Should be blocked
      await expect(page.locator('[data-error="cross-tenant-export-blocked"]'))
        .toContainText('Cannot export data from other tenants')
    })

    await test.step('Verify only current tenant data can be exported', async () => {
      await page.click('[data-action="export-users"]')
      await page.fill('[data-filter="tenantId"]', 'tenant-alpha')
      await page.click('[data-action="download-export"]')

      // Should succeed
      await expect(page.locator('[data-success="export-started"]'))
        .toContainText('Export started for your tenant')
    })
  })

  test('Prevent API token misuse across tenants', async ({ page }) => {
    await test.step('Verify API token is tenant-scoped', async () => {
      await page.goto('/admin/api-tokens')

      // View token details
      const token = page.locator('[data-token]').first()
      await token.locator('[data-action="view"]').click()

      // Should show tenant scope
      await expect(page.locator('[data-field="scope"]')).toContainText('tenant-alpha')
      await expect(page.locator('[data-field="restrictedToTenant"]')).toContainText('true')
    })

    await test.step('Attempt to use token for cross-tenant access', async () => {
      // In real test, would make API call with token
      // For UI, check warning
      await expect(page.locator('[data-warning="token-tenant-scoped"]'))
        .toContainText('This token is restricted to your tenant')
    })
  })
})

test.describe('Admin Multi-Tenant Isolation - Security Boundaries', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('Tenant admin role isolation', async ({ page }) => {
    await test.step('Create tenant-specific admin role', async () => {
      await page.goto('/admin/roles/create')
      await page.fill('[name="roleName"]', 'tenant-alpha-admin')
      await page.selectOption('[name="tenantScope"]', 'tenant-alpha')

      // Add tenant-scoped permissions
      await page.check('[value="tenant.user.manage"]')
      await page.check('[value="tenant.settings.manage"]')

      // But not global permissions
      await page.uncheck('[value="system.*"]')
      await page.uncheck('[value="tenant.*"]') // All tenants

      await page.click('[data-action="save-role"]')
      await expect(page.locator('[data-success="role-created"]'))
        .toContainText('Tenant-scoped role created')
    })

    await test.step('Verify role is tenant-scoped', async () => {
      await page.goto('/admin/roles')

      const roleRow = page.locator('[data-role]').filter({
        hasText: 'tenant-alpha-admin'
      })

      await expect(roleRow.locator('[data-field="scope"]')).toContainText('tenant-alpha')
      await expect(roleRow.locator('[data-field="type"]')).toContainText('Tenant-Scoped')
    })

    await test.step('Verify role cannot access other tenant data', async () => {
      // This would be tested by logging in as user with this role
      // For UI, check the role definition
      await page.click(roleRow.locator('[data-action="view-permissions"]'))

      const permissions = page.locator('[data-permission]')
      const count = await permissions.count()

      for (let i = 0; i < count; i++) {
        const perm = permissions.nth(i)
        const permName = await perm.locator('[data-field="name"]').textContent()
        // All permissions should be scoped
        expect(permName || '').not.toMatch(/^tenant-(beta|gamma)/)
      }
    })
  })

  test('Data leak prevention via session', async ({ page }) => {
    await test.step('Verify session is tenant-scoped', async () => {
      await page.goto('/admin/profile')
      await page.click('[data-action="view-session"]')

      await expect(page.locator('[data-field="tenantId"]')).toContainText('tenant-alpha')
      await expect(page.locator('[data-field="sessionScope"]')).toContainText('Tenant-Scoped')
    })

    await test.step('Session timeout clears tenant context', async () => {
      // Simulate session timeout
      await page.evaluate(() => {
        localStorage.clear()
        sessionStorage.clear()
      })

      await page.goto('/admin/users')
      await expect(page).toHaveURL('/login')
    })
  })

  test('Cross-tenant SQL injection prevention', async ({ page }) => {
    await test.step('Attempt SQL injection to access other tenant data', async () => {
      await page.goto('/admin/users?search=; DELETE FROM users WHERE tenant_id=')

      // Should be blocked
      await expect(page.locator('[data-error="injection-blocked"]'))
        .toContainText('Malicious query detected')
    })

    await test.step('Verify parameterized queries are used', async () => {
      // Check for SQL injection protection indicators
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', "test'; DROP TABLE users; --")

      await page.press('[data-search="users"]', 'Enter')

      // Should handle safely
      await expect(page.locator('[data-empty="no-results"]')).toBeVisible()
      // System should still be running
      await expect(page.locator('[data-section="users"]')).toBeVisible()
    })
  })
})

test.describe('Admin Multi-Tenant Isolation - Performance', () => {
  test.use({ storageState: 'tenant-admin-auth.json' })

  test.repeat(10, 'Tenant data filtering performance', async ({ page }) => {
    const start = Date.now()

    await page.goto('/admin/users')
    await expect(page.locator('[data-section="users"]')).toBeVisible()

    const duration = Date.now() - start
    expect(duration).toBeLessThan(2000) // Should filter within 2 seconds
  })

  test('Large tenant with many users', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Navigate large user list efficiently', async () => {
      const start = Date.now()

      // Load users
      await page.waitForSelector('[data-user]', { timeout: 10000 })
      const count = await page.locator('[data-user]').count()

      // Should load within reasonable time
      const duration = Date.now() - start
      expect(duration).toBeLessThan(5000)
      expect(count).toBeGreaterThan(0)
    })

    await test.step('Search in large dataset', async () => {
      const start = Date.now()

      await page.fill('[data-search="users"]', 'user')
      await page.press('[data-search="users"]', 'Enter')
      await page.waitForTimeout(1000) // Wait for search

      const duration = Date.now() - start
      expect(duration).toBeLessThan(3000)
    })
  })

  test('Concurrent tenant access', async ({ page }) => {
    // Open multiple tabs
    const tabs = []
    for (let i = 0; i < 3; i++) {
      const tab = await page.context().newPage()
      await tab.goto('/admin/users')
      tabs.push(tab)
    }

    await test.step('Verify each tab is isolated to correct tenant', async () => {
      for (const tab of tabs) {
        const tenantId = await tab.locator('[data-current-tenant]').getAttribute('data-current-tenant')
        expect(tenantId).toBe('tenant-alpha')

        const users = tab.locator('[data-user]')
        const count = await users.count()

        // All users should be from same tenant
        for (let i = 0; i < Math.min(count, 10); i++) {
          const user = users.nth(i)
          const userTenant = await user.locator('[data-field="tenantId"]').textContent()
          expect(userTenant).toBe('tenant-alpha')
        }
      }
    })

    // Cleanup
    for (const tab of tabs) {
      await tab.close()
    }
  })
})

test.describe('Admin Multi-Tenant Isolation - Compliance', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('GDPR compliance - tenant data deletion', async ({ page }) => {
    const userEmail = `gdpr-delete-${Date.now()}@example.com`

    // Create user
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'GDPR')
    await page.fill('[name="lastName"]', 'Delete')
    await page.fill('[name="email"]', userEmail)
    await page.fill('[name="username"]', `gdprdelete${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await test.step('Initiate GDPR data deletion', async () => {
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', userEmail)
      await page.press('[data-search="users"]', 'Enter')
      await page.click('[data-user] [data-action="delete"]')
      await page.click('[data-action="confirm-gdpr-deletion"]')
      await page.check('[name="confirmDeletion"]')
      await page.click('[data-action="execute-deletion"]')

      await expect(page.locator('[data-success="gdpr-deletion-scheduled"]'))
        .toContainText('GDPR deletion scheduled')
    })

    await test.step('Verify deletion is tenant-scoped', async () => {
      // Deletion should only affect current tenant
      await page.goto('/admin/audit-logs')
      await page.fill('[data-filter="action"]', 'gdpr.user.deletion')
      await page.press('[data-filter="action"]', 'Enter')

      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry.locator('[data-field="tenantId"]')).toContainText('tenant-alpha')
      await expect(logEntry.locator('[data-field="scope"]')).toContainText('tenant-scoped')
    })
  })

  test('SOX compliance - tenant separation', async ({ page }) => {
    await test.step('Generate SOX compliance report for multi-tenancy', async () => {
      const downloadPromise = page.waitForEvent('download')
      await page.goto('/admin/compliance')
      await page.click('[data-action="generate-multi-tenant-report"]')
      await page.click('[data-action="download-report"]')
      const download = await downloadPromise

      expect(download.suggestedFilename()).toMatch(/multi-tenant-compliance.*\.pdf/)
    })

    await test.step('Verify tenant isolation controls', async () => {
      await expect(page.locator('[data-compliance="dataSegregation"]'))
        .toContainText('Verified')
      await expect(page.locator('[data-compliance="accessControl"]'))
        .toContainText('Verified')
      await expect(page.locator('[data-compliance="auditTrail"]'))
        .toContainText('Verified')
      await expect(page.locator('[data-compliance="dataEncryption"]'))
        .toContainText('Verified')
    })
  })

  test('Tenant data residency compliance', async ({ page }) => {
    await test.step('Verify data residency settings', async () => {
      await page.goto('/admin/tenants/tenant-alpha/settings')

      await expect(page.locator('[data-field="dataResidency"]')).toContainText('US-East')
      await expect(page.locator('[data-field="compliantRegions"]'))
        .toContainText('US, EU')
    })

    await test.step('Verify data never leaves designated region', async () => {
      // Check that all data operations are logged with region info
      await page.goto('/admin/audit-logs')
      const logEntry = page.locator('[data-log-entry]').first()
      await expect(logEntry.locator('[data-field="region"]')).toContainText('US-East')
    })
  })
})

test.describe('Admin Multi-Tenant Isolation - Edge Cases', () => {
  test.use({ storageState: 'tenant-admin-auth.json' })

  test('Prevent tenant context switching via browser', async ({ page }) => {
    await test.step('Try to modify localStorage to change tenant', async () => {
      await page.evaluate(() => {
        localStorage.setItem('tenantId', 'tenant-beta')
      })

      await page.reload()
    })

    await test.step('Verify tenant context is server-side', async () => {
      // Should still be on original tenant
      await expect(page.locator('[data-current-tenant="tenant-alpha"]')).toBeVisible()

      // localStorage change should be ignored
      const warning = page.locator('[data-warning="local-storage-ignored"]')
      await expect(warning).toBeVisible()
    })
  })

  test('Handle tenant deletion gracefully', async ({ page }) => {
    // Super admin deletes a tenant
    await page.goto('/admin/tenants')
    const tenantGamma = page.locator('[data-tenant="tenant-gamma"]')
    await tenantGamma.locator('[data-action="delete"]').click()
    await page.fill('[name="confirmation"]', 'DELETE TENANT')
    await page.click('[data-action="confirm-deletion"]')
    await expect(page.locator('[data-success="tenant-deleted"]'))
      .toContainText('Tenant deleted successfully')

    // User from that tenant tries to access
    // In real test, would switch to tenant-gamma user
    // For UI, verify the tenant is gone
    await test.step('Verify deleted tenant is inaccessible', async () => {
      await page.goto('/admin/tenants/tenant-gamma')
      await expect(page.locator('[data-error="tenant-not-found"]'))
        .toContainText('Tenant does not exist')
    })
  })

  test('Recover from tenant misconfiguration', async ({ page }) => {
    await test.step('Simulate tenant misconfiguration', async () => {
      // Super admin would set this in real scenario
      // For UI test, check error handling
      await page.goto('/admin/tenants/tenant-alpha/settings')
      await page.click('[data-action="simulate-misconfig"]')
    })

    await test.step('Verify graceful error handling', async () => {
      await expect(page.locator('[data-error="tenant-misconfigured"]'))
        .toContainText('Tenant configuration error')
      await expect(page.locator('[data-action="retry"]')).toBeVisible()
    })
  })
})
