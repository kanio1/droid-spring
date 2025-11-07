/**
 * Admin Bulk Operations - Import/Export/Update
 *
 * Bulk operations tests using Playwright 1.55+:
 * - test.repeat() for stress testing bulk operations
 * - test.serial() for complete bulk workflows
 * - expect.soft() for validating multiple operations
 *
 * Coverage: CSV import, bulk update, bulk delete, export
 */

import { test, expect } from '@playwright/test'
import * as fs from 'fs'
import * as path from 'path'

test.describe('Bulk Import - CSV Upload', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Complete CSV import workflow', async ({ page }) => {
    await test.step('Navigate to import page', async () => {
      await page.goto('/admin/users')
      await page.click('[data-action="bulk-import"]')
      await expect(page).toHaveURL(/.*\/admin\/users\/import.*/)
    })

    await test.step('Download CSV template', async () => {
      await page.click('[data-action="download-template"]')
      // Template should download
      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-link="template-csv"]')
      const download = await downloadPromise
      expect(download.suggestedFilename()).toBe('users-template.csv')
    })

    await test.step('Upload CSV file', async () => {
      // Create mock CSV file
      const csvContent = `firstName,lastName,email,username,role
John,Doe,john.doe@example.com,johndoe,user
Jane,Smith,jane.smith@example.com,janesmith,admin
Bob,Johnson,bob.j@example.com,bobjohnson,user`

      await page.setInputFiles('[name="csvFile"]', {
        name: 'users.csv',
        mimeType: 'text/csv',
        buffer: Buffer.from(csvContent)
      })
    })

    await test.step('Review import preview', async () => {
      await page.click('[data-action="preview-import"]')
      await expect(page.locator('[data-preview="users"]')).toBeVisible()
      await expect(page.locator('[data-preview-row]')).toHaveCount(3)
    })

    await test.step('Confirm import', async () => {
      await page.click('[data-action="confirm-import"]')
      await expect(page.locator('[data-progress="importing"]')).toBeVisible()
    })

    await test.step('Verify import completion', async () => {
      await expect(page.locator('[data-success="import-complete"]')).toBeVisible()
      await expect(page.locator('[data-stat="imported"]')).toContainText('3')
    })
  })

  test('Import with invalid CSV format', async ({ page }) => {
    await page.goto('/admin/users/import')

    await test.step('Upload invalid CSV', async () => {
      const invalidCsv = `invalid,csv,format
missing,columns`

      await page.setInputFiles('[name="csvFile"]', {
        name: 'invalid.csv',
        mimeType: 'text/csv',
        buffer: Buffer.from(invalidCsv)
      })
    })

    await test.step('Verify validation errors', async () => {
      await page.click('[data-action="preview-import"]')
      await expect(page.locator('[data-error="missing-column"]')).toContainText('firstName')
    })
  })

  test('Import with duplicate emails', async ({ page }) => {
    await page.goto('/admin/users/import')

    await test.step('Upload CSV with duplicates', async () => {
      const csvWithDuplicates = `firstName,lastName,email,username,role
John,Doe,john@example.com,johndoe,user
Jane,Smith,john@example.com,janesmith,admin`

      await page.setInputFiles('[name="csvFile"]', {
        name: 'duplicates.csv',
        mimeType: 'text/csv',
        buffer: Buffer.from(csvWithDuplicates)
      })
    })

    await test.step('Preview shows duplicate warnings', async () => {
      await page.click('[data-action="preview-import"]')
      await expect(page.locator('[data-warning="duplicate-email"]')).toContainText('john@example.com')
    })

    await test.step('Handle duplicates on import', async () => {
      await page.selectOption('[name="duplicateHandling"]', 'skip')
      await page.click('[data-action="confirm-import"]')
      await expect(page.locator('[data-success="import-complete"]')).toBeVisible()
    })
  })

  test('Import large file (1000 users)', async ({ page }) => {
    await page.goto('/admin/users/import')

    await test.step('Generate large CSV', async () => {
      const users = Array.from({ length: 1000 }, (_, i) =>
        `User${i},Test${i},user${i}@example.com,usertest${i},user`
      ).join('\n')
      const csv = `firstName,lastName,email,username,role\n${users}`

      await page.setInputFiles('[name="csvFile"]', {
        name: 'large-import.csv',
        mimeType: 'text/csv',
        buffer: Buffer.from(csv)
      })
    })

    await test.step('Import with progress tracking', async () => {
      await page.click('[data-action="confirm-import"]')

      // Monitor progress
      await expect(page.locator('[data-progress="0%"]')).toBeVisible()
      await expect(page.locator('[data-progress="100%"]')).toBeVisible({ timeout: 30000 })
    })

    await test.step('Verify import results', async () => {
      await expect(page.locator('[data-stat="imported"]')).toContainText('1000')
    })
  })
})

test.describe('Bulk Export - CSV Download', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Export all users to CSV', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Click export button', async () => {
      await page.click('[data-action="export-users"]')
    })

    await test.step('Configure export options', async () => {
      await page.check('[name="includeHeaders"]')
      await page.selectOption('[name="format"]', 'csv')
    })

    await test.step('Download export file', async () => {
      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-action="download-export"]')
      const download = await downloadPromise

      expect(download.suggestedFilename()).toMatch(/users-\d{4}-\d{2}-\d{2}\.csv/)
    })
  })

  test('Export filtered users', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Apply filters', async () => {
      await page.selectOption('[data-filter="role"]', 'admin')
    })

    await test.step('Export filtered results', async () => {
      await page.click('[data-action="export-filtered"]')

      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-action="download-export"]')
      const download = await downloadPromise

      expect(download.suggestedFilename()).toMatch(/users-filtered.*\.csv/)
    })
  })
})

test.describe('Bulk Update Operations', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Bulk update user roles', async ({ page }) => {
    await test.step('Navigate to users list', async () => {
      await page.goto('/admin/users')
    })

    await test.step('Select multiple users', async () => {
      await page.check('[data-user]:nth-child(1) [data-checkbox]')
      await page.check('[data-user]:nth-child(2) [data-checkbox]')
      await page.check('[data-user]:nth-child(3) [data-checkbox]')
    })

    await test.step('Open bulk edit dialog', async () => {
      await page.click('[data-action="bulk-edit"]')
      await expect(page.locator('[data-modal="bulk-edit"]')).toBeVisible()
    })

    await test.step('Update role for selected users', async () => {
      await page.selectOption('[name="fieldToUpdate"]', 'role')
      await page.selectOption('[name="newValue"]', 'admin')
    })

    await test.step('Apply bulk update', async () => {
      await page.click('[data-action="apply-bulk-update"]')
      await expect(page.locator('[data-success="bulk-updated"]')).toBeVisible()
    })

    await test.step('Verify updates', async () => {
      await expect(page.locator('[data-user]:nth-child(1) [data-role="admin"]')).toBeVisible()
      await expect(page.locator('[data-user]:nth-child(2) [data-role="admin"]')).toBeVisible()
    })
  })

  test('Bulk update with preview', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Select users for bulk update', async () => {
      await page.check('[data-user]:nth-child(1) [data-checkbox]')
      await page.check('[data-user]:nth-child(2) [data-checkbox]')
      await page.click('[data-action="bulk-edit"]')
    })

    await test.step('Preview changes', async () => {
      await page.selectOption('[name="fieldToUpdate"]', 'department')
      await page.fill('[name="newValue"]', 'Engineering')
      await page.click('[data-action="preview-changes"]')
      await expect(page.locator('[data-preview="bulk-change"]')).toBeVisible()
    })

    await test.step('Apply after preview', async () => {
      await page.click('[data-action="apply-previewed-changes"]')
      await expect(page.locator('[data-success="bulk-updated"]')).toBeVisible()
    })
  })
})

test.describe('Bulk Delete Operations', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Bulk delete users', async ({ page }) => {
    // First, create some test users to delete
    await test.step('Create test users', async () => {
      for (let i = 0; i < 5; i++) {
        await createTestUser(page, `delete${i}${Date.now()}@example.com`)
      }
    })

    await test.step('Select users for deletion', async () => {
      await page.goto('/admin/users')
      await page.check('[data-user]:nth-child(1) [data-checkbox]')
      await page.check('[data-user]:nth-child(2) [data-checkbox]')
      await page.check('[data-user]:nth-child(3) [data-checkbox]')
    })

    await test.step('Open bulk delete dialog', async () => {
      await page.click('[data-action="bulk-delete"]')
      await expect(page.locator('[data-modal="confirm-bulk-delete"]')).toBeVisible()
    })

    await test.step('Confirm bulk deletion', async () => {
      await page.fill('[name="confirmation"]', 'DELETE')
      await page.click('[data-action="confirm-bulk-delete"]')
      await expect(page.locator('[data-success="users-deleted"]')).toBeVisible()
    })

    await test.step('Verify users removed', async () => {
      await expect(page.locator('[data-user]')).toHaveCount({ min: 0, max: 2 })
    })
  })

  test('Cancel bulk delete', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Select users', async () => {
      await page.check('[data-user]:nth-child(1) [data-checkbox]')
      await page.check('[data-user]:nth-child(2) [data-checkbox]')
    })

    await test.step('Open delete dialog', async () => {
      await page.click('[data-action="bulk-delete"]')
    })

    await test.step('Cancel deletion', async () => {
      await page.click('[data-action="cancel-bulk-delete"]')
      await expect(page.locator('[data-modal="confirm-bulk-delete"]')).toBeHidden()
    })

    await test.step('Verify users still exist', async () => {
      await expect(page.locator('[data-user]')).toHaveCount({ min: 5 })
    })
  })
})

test.describe('Bulk Operations - Stress Testing', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.repeat(10, 'Bulk import consistency', async ({ page }) => {
    // Test that bulk import works reliably
    await page.goto('/admin/users/import')

    const csv = `firstName,lastName,email,username,role
Test${Date.now()},User,test${Date.now()}@example.com,testuser${Date.now()},user`

    await page.setInputFiles('[name="csvFile"]', {
      name: 'test-import.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv)
    })

    await page.click('[data-action="confirm-import"]')
    await expect(page.locator('[data-success="import-complete"]')).toBeVisible({ timeout: 15000 })
  })

  test.repeat(5, 'Bulk export generates valid file', async ({ page }) => {
    await page.goto('/admin/users')

    const downloadPromise = page.waitForEvent('download')
    await page.click('[data-action="export-users"]')
    await page.click('[data-action="download-export"]')
    const download = await downloadPromise

    expect(download.suggestedFilename()).toMatch(/\.csv$/)
  })

  test.repeat(3, 'Bulk update performance', async ({ page }) => {
    // Test bulk update doesn't timeout
    await page.goto('/admin/users')

    // Select first 20 users
    for (let i = 1; i <= 20; i++) {
      await page.check(`[data-user]:nth-child(${i}) [data-checkbox]`)
    }

    await page.click('[data-action="bulk-edit"]')
    await page.selectOption('[name="fieldToUpdate"]', 'role')
    await page.selectOption('[name="newValue"]', 'user')

    const start = Date.now()
    await page.click('[data-action="apply-bulk-update"]')
    await expect(page.locator('[data-success="bulk-updated"]')).toBeVisible()
    const duration = Date.now() - start

    expect(duration).toBeLessThan(10000) // Should complete within 10 seconds
  })
})

test.describe('Bulk Operations - Error Handling', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Import with too many users (exceeds limit)', async ({ page }) => {
    await page.goto('/admin/users/import')

    const csv = `firstName,lastName,email,username,role\n${Array.from({ length: 5001 }, (_, i) =>
      `User${i},Test${i},user${i}@example.com,usertest${i},user`
    ).join('\n')}`

    await page.setInputFiles('[name="csvFile"]', {
      name: 'too-large.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv)
    })

    await page.click('[data-action="preview-import"]')
    await expect(page.locator('[data-error="exceeds-limit"]')).toContainText('5000')
  })

  test('Import with invalid file type', async ({ page }) => {
    await page.goto('/admin/users/import')

    await page.setInputFiles('[name="csvFile"]', {
      name: 'invalid.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('This is not a CSV file')
    })

    await page.click('[data-action="preview-import"]')
    await expect(page.locator('[data-error="invalid-file-type"]')).toBeVisible()
  })

  test('Bulk operation with insufficient permissions', async ({ page }) => {
    // This test assumes we're testing with a user who lacks bulk delete permissions
    await page.goto('/admin/users')

    await page.check('[data-user]:nth-child(1) [data-checkbox]')
    await page.click('[data-action="bulk-delete"]')

    await expect(page.locator('[data-error="insufficient-permissions"]')).toBeVisible()
  })
})

// Helper function to create test user
async function createTestUser(page, email: string) {
  await page.goto('/admin/users/create')
  await page.fill('[name="firstName"]', 'Test')
  await page.fill('[name="lastName"]', 'User')
  await page.fill('[name="email"]', email)
  await page.fill('[name="username"]', `testuser${Date.now()}`)
  await page.check('[value="user"]')
  await page.click('[data-action="submit-user"]')
  await expect(page.locator('[data-success="user-created"]')).toBeVisible()
}
