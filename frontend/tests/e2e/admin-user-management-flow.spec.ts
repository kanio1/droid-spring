/**
 * Admin User Management - CRUD Operations
 *
 * Comprehensive user management tests using Playwright 1.55+:
 * - test.serial() for complete user lifecycle
 * - expect.soft() for non-blocking form validations
 * - test.step() for detailed workflow validation
 *
 * Coverage: Create, Read, Update, Delete, List users
 */

import { test, expect } from '@playwright/test'

test.describe('User Management - Create User', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Complete user creation workflow', async ({ page }) => {
    await test.step('Navigate to user creation page', async () => {
      await page.goto('/admin/users')
      await page.click('[data-action="create-user"]')
      await expect(page).toHaveURL(/.*\/admin\/users\/create.*/)
    })

    await test.step('Fill basic user information', async () => {
      await page.fill('[name="firstName"]', 'John')
      await page.fill('[name="lastName"]', 'Doe')
      await page.fill('[name="email"]', `john.doe.${Date.now()}@example.com`)
      await page.fill('[name="username"]', `johndoe${Date.now()}`)
    })

    await test.step('Select user roles', async () => {
      await page.check('[value="user"]')
      await page.check('[value="viewer"]')
    })

    await test.step('Set department and team', async () => {
      await page.selectOption('[name="department"]', 'Engineering')
      await page.selectOption('[name="team"]', 'Backend')
    })

    await test.step('Set user status', async () => {
      await page.selectOption('[name="status"]', 'active')
    })

    await test.step('Submit user creation', async () => {
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    await test.step('Verify user appears in list', async () => {
      await page.goto('/admin/users')
      await expect(page.locator('[data-user]')).toContainText('John Doe')
    })
  })

  test('Create user with invalid email', async ({ page }) => {
    await page.goto('/admin/users/create')

    await test.step('Fill form with invalid email', async () => {
      await page.fill('[name="email"]', 'invalid-email')
      await page.fill('[name="firstName"]', 'Test')
      await page.fill('[name="lastName"]', 'User')
    })

    await test.step('Attempt to submit', async () => {
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-error="email-invalid"]')).toBeVisible()
    })

    await test.step('Verify form not submitted', async () => {
      await expect(page).toHaveURL(/.*\/admin\/users\/create.*/)
    })
  })

  test('Create user with duplicate email', async ({ page }) => {
    const existingEmail = 'existing@example.com'

    await page.goto('/admin/users/create')

    await test.step('Fill form with existing email', async () => {
      await page.fill('[name="email"]', existingEmail)
      await page.fill('[name="firstName"]', 'Test')
      await page.fill('[name="lastName"]', 'User')
    })

    await test.step('Attempt to submit', async () => {
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-error="email-exists"]')).toBeVisible()
    })
  })

  test('Create user with weak password', async ({ page }) => {
    await page.goto('/admin/users/create')

    await test.step('Fill basic info', async () => {
      await page.fill('[name="email"]', `test${Date.now()}@example.com`)
      await page.fill('[name="firstName"]', 'Test')
      await page.fill('[name="lastName"]', 'User')
    })

    await test.step('Enter weak password', async () => {
      await page.fill('[name="password"]', '123')
      await page.blur('[name="password"]')
    })

    await test.step('Verify password strength warning', async () => {
      await expect.soft(page.locator('[data-warning="weak-password"]')).toBeVisible()
      await expect.soft(page.locator('[data-strength="very-weak"]')).toBeVisible()
    })

    await test.step('Verify submit disabled', async () => {
      const submitButton = page.locator('[data-action="submit-user"]')
      await expect(submitButton).toBeDisabled()
    })
  })

  test('Create user with strong password', async ({ page }) => {
    await page.goto('/admin/users/create')

    await test.step('Fill basic info', async () => {
      await page.fill('[name="email"]', `test${Date.now()}@example.com`)
      await page.fill('[name="firstName"]', 'Test')
      await page.fill('[name="lastName"]', 'User')
    })

    await test.step('Enter strong password', async () => {
      await page.fill('[name="password"]', 'StrongP@ssw0rd123!')
      await page.blur('[name="password"]')
    })

    await test.step('Verify password strength', async () => {
      await expect.soft(page.locator('[data-strength="strong"]')).toBeVisible()
      await expect(page.locator('[data-checklist="password-requirements"] li')).toHaveCount(5)
    })

    await test.step('Verify submit enabled', async () => {
      const submitButton = page.locator('[data-action="submit-user"]')
      await expect(submitButton).toBeEnabled()
    })
  })
})

test.describe('User Management - List & Search', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('User list pagination', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Verify pagination controls', async () => {
      await expect.soft(page.locator('[data-pagination]')).toBeVisible()
    })

    await test.step('Navigate to next page', async () => {
      await page.click('[data-pagination="next"]')
      await expect(page.locator('[data-page="2"]')).toHaveAttribute('data-current', 'true')
    })

    await test.step('Navigate to previous page', async () => {
      await page.click('[data-pagination="prev"]')
      await expect(page.locator('[data-page="1"]')).toHaveAttribute('data-current', 'true')
    })
  })

  test('User search functionality', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Search by name', async () => {
      await page.fill('[data-search="users"]', 'John Doe')
      await page.press('[data-search="users"]', 'Enter')
    })

    await test.step('Verify search results', async () => {
      const users = page.locator('[data-user]')
      await expect(users).toHaveCount(1)
      await expect(users.first()).toContainText('John Doe')
    })

    await test.step('Clear search', async () => {
      await page.click('[data-action="clear-search"]')
      const allUsers = page.locator('[data-user]')
      await expect(allUsers).toHaveCount({ min: 10 })
    })
  })

  test('User filtering by role', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Filter by admin role', async () => {
      await page.selectOption('[data-filter="role"]', 'admin')
    })

    await test.step('Verify filtered results', async () => {
      const users = page.locator('[data-user]')
      const count = await users.count()

      for (let i = 0; i < count; i++) {
        await expect.soft(users.nth(i)).toContainText('[data-role="admin"]')
      }
    })

    await test.step('Clear filter', async () => {
      await page.selectOption('[data-filter="role"]', '')
      const allUsers = page.locator('[data-user]')
      await expect(allUsers).toHaveCount({ min: 10 })
    })
  })

  test('User filtering by status', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Filter by active status', async () => {
      await page.selectOption('[data-filter="status"]', 'active')
    })

    await test.step('Verify filtered results', async () => {
      const users = page.locator('[data-user]')
      await expect(users.first()).toHaveAttribute('data-status', 'active')
    })
  })
})

test.describe('User Management - Edit User', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Complete user update workflow', async ({ page }) => {
    // First, create a user to edit
    await test.step('Create test user', async () => {
      const email = `edit${Date.now()}@example.com`
      await createTestUser(page, email)
    })

    await test.step('Navigate to user edit page', async () => {
      await page.goto('/admin/users')
      await page.click('[data-user] [data-action="edit"]')
      await expect(page).toHaveURL(/.*\/admin\/users\/edit.*/)
    })

    await test.step('Update user information', async () => {
      await page.fill('[name="firstName"]', 'Jane')
      await page.fill('[name="lastName"]', 'Smith')
    })

    await test.step('Update user roles', async () => {
      await page.uncheck('[value="user"]')
      await page.check('[value="admin"]')
    })

    await test.step('Update department', async () => {
      await page.selectOption('[name="department"]', 'Product')
    })

    await test.step('Save changes', async () => {
      await page.click('[data-action="save-user"]')
      await expect(page.locator('[data-success="user-updated"]')).toBeVisible()
    })

    await test.step('Verify changes in user list', async () => {
      await page.goto('/admin/users')
      await expect(page.locator('[data-user]')).toContainText('Jane Smith')
    })
  })

  test('Edit user without required fields', async ({ page }) => {
    await page.goto('/admin/users')
    await page.click('[data-user] [data-action="edit"]')

    await test.step('Clear required field', async () => {
      await page.fill('[name="email"]', '')
      await page.blur('[name="email"]')
    })

    await test.step('Attempt to save', async () => {
      await page.click('[data-action="save-user"]')
      await expect(page.locator('[data-error="email-required"]')).toBeVisible()
    })
  })
})

test.describe('User Management - Delete User', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Delete user workflow', async ({ page }) => {
    // Create a user to delete
    await test.step('Create test user for deletion', async () => {
      const email = `delete${Date.now()}@example.com`
      await createTestUser(page, email)
    })

    await test.step('Navigate to users list', async () => {
      await page.goto('/admin/users')
    })

    await test.step('Click delete on user', async () => {
      await page.click('[data-user] [data-action="delete"]')
      await expect(page.locator('[data-modal="confirm-delete"]')).toBeVisible()
    })

    await test.step('Confirm deletion', async () => {
      await page.click('[data-action="confirm-delete"]')
      await expect(page.locator('[data-success="user-deleted"]')).toBeVisible()
    })

    await test.step('Verify user removed from list', async () => {
      // User should no longer be visible
      await expect(page.locator('[data-user]')).not.toContainText('delete')
    })
  })

  test('Cancel user deletion', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Click delete button', async () => {
      await page.click('[data-user] [data-action="delete"]')
    })

    await test.step('Cancel deletion', async () => {
      await page.click('[data-action="cancel-delete"]')
      await expect(page.locator('[data-modal="confirm-delete"]')).toBeHidden()
    })

    await test.step('Verify user still exists', async () => {
      await expect(page.locator('[data-user]')).toBeVisible()
    })
  })

  test('Delete multiple users (bulk)', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Select multiple users', async () => {
      await page.check('[data-user]:nth-child(1) [data-checkbox]')
      await page.check('[data-user]:nth-child(2) [data-checkbox]')
      await page.check('[data-user]:nth-child(3) [data-checkbox]')
    })

    await test.step('Click bulk delete', async () => {
      await page.click('[data-action="bulk-delete"]')
      await expect(page.locator('[data-modal="confirm-bulk-delete"]')).toBeVisible()
    })

    await test.step('Confirm bulk delete', async () => {
      await page.click('[data-action="confirm-bulk-delete"]')
      await expect(page.locator('[data-success="users-deleted"]')).toBeVisible()
    })
  })
})

test.describe('User Management - Form Validations', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Real-time email validation', async ({ page }) => {
    await page.goto('/admin/users/create')

    await test.step('Enter valid email', async () => {
      await page.fill('[name="email"]', 'valid@example.com')
      await page.blur('[name="email"]')
      await expect.soft(page.locator('[data-error="email-invalid"]')).toBeHidden()
    })

    await test.step('Enter invalid email', async () => {
      await page.fill('[name="email"]', 'invalid')
      await page.blur('[name="email"]')
      await expect.soft(page.locator('[data-error="email-invalid"]')).toBeVisible()
    })
  })

  test('Required field validation', async ({ page }) => {
    await page.goto('/admin/users/create')

    await test.step('Leave all fields empty', async () => {
      await page.click('[data-action="submit-user"]')
    })

    await test.step('Verify all required errors', async () => {
      await expect.soft(page.locator('[data-error="firstName-required"]')).toBeVisible()
      await expect.soft(page.locator('[data-error="lastName-required"]')).toBeVisible()
      await expect.soft(page.locator('[data-error="email-required"]')).toBeVisible()
      await expect.soft(page.locator('[data-error="username-required"]')).toBeVisible()
    })
  })

  test('Username uniqueness validation', async ({ page }) => {
    await page.goto('/admin/users/create')

    await test.step('Enter existing username', async () => {
      await page.fill('[name="username"]', 'existinguser')
      await page.blur('[name="username"]')
    })

    await test.step('Verify username taken error', async () => {
      await expect(page.locator('[data-error="username-taken"]')).toBeVisible()
    })
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
