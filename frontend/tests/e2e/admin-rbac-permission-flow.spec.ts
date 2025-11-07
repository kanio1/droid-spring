/**
 * RBAC (Role-Based Access Control) - Permission Matrix Tests
 *
 * Tests role permissions and access control using Playwright 1.55+:
 * - test.serial() for permission verification workflows
 * - expect.soft() for multiple permission checks
 * - test.step() for step-by-step access validation
 *
 * Coverage: Role permissions, access control, permission matrix
 */

import { test, expect } from '@playwright/test'

test.describe('RBAC - Super Admin Permissions', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test.serial('Super admin can access all resources', async ({ page }) => {
    await test.step('Access admin dashboard', async () => {
      await page.goto('/admin/dashboard')
      await expect(page.locator('[data-admin="dashboard"]')).toBeVisible()
    })

    await test.step('Access user management', async () => {
      await page.goto('/admin/users')
      await expect(page.locator('[data-section="users"]')).toBeVisible()
      await expect(page.locator('[data-action="create-user"]')).toBeVisible()
      await expect(page.locator('[data-action="delete-user"]')).toBeVisible()
    })

    await test.step('Access role management', async () => {
      await page.goto('/admin/roles')
      await expect(page.locator('[data-section="roles"]')).toBeVisible()
      await expect(page.locator('[data-action="create-role"]')).toBeVisible()
    })

    await test.step('Access permission management', async () => {
      await page.goto('/admin/permissions')
      await expect(page.locator('[data-section="permissions"]')).toBeVisible()
    })

    await test.step('Access system configuration', async () => {
      await page.goto('/admin/system')
      await expect(page.locator('[data-section="system"]')).toBeVisible()
    })

    await test.step('Access audit logs', async () => {
      await page.goto('/admin/audit-logs')
      await expect(page.locator('[data-section="audit-logs"]')).toBeVisible()
    })
  })

  test('Super admin can manage any user', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Create user as super admin', async () => {
      await page.click('[data-action="create-user"]')
      await expect(page).toHaveURL(/.*\/admin\/users\/create.*/)
    })

    await test.step('Delete user as super admin', async () => {
      await page.goto('/admin/users')
      await page.click('[data-user]:nth-child(1) [data-action="delete"]')
      await expect(page.locator('[data-modal="confirm-delete"]')).toBeVisible()
    })
  })

  test('Super admin can create and edit roles', async ({ page }) => {
    await page.goto('/admin/roles')

    await test.step('Create new role', async () => {
      await page.click('[data-action="create-role"]')
      await page.fill('[name="roleName"]', 'Custom Role')
      await page.check('[permission="user.read"]')
      await page.check('[permission="user.write"]')
      await page.click('[data-action="save-role"]')
      await expect(page.locator('[data-success="role-created"]')).toBeVisible()
    })

    await test.step('Edit existing role', async () => {
      await page.click('[data-role="Custom Role"] [data-action="edit"]')
      await page.check('[permission="user.delete"]')
      await page.click('[data-action="save-role"]')
      await expect(page.locator('[data-success="role-updated"]')).toBeVisible()
    })
  })
})

test.describe('RBAC - Admin Permissions', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Admin can access user management but not system config', async ({ page }) => {
    await test.step('Access admin dashboard', async () => {
      await page.goto('/admin/dashboard')
      await expect(page.locator('[data-admin="dashboard"]')).toBeVisible()
    })

    await test.step('Access user management', async () => {
      await page.goto('/admin/users')
      await expect(page.locator('[data-section="users"]')).toBeVisible()
      await expect(page.locator('[data-action="create-user"]')).toBeVisible()
    })

    await test.step('Access role management (read-only)', async () => {
      await page.goto('/admin/roles')
      await expect(page.locator('[data-section="roles"]')).toBeVisible()
      await expect.soft(page.locator('[data-action="create-role"]')).toBeHidden()
      await expect.soft(page.locator('[data-action="edit-role"]')).toBeHidden()
    })

    await test.step('Cannot access system configuration', async () => {
      await page.goto('/admin/system')
      await expect(page.locator('[data-error="access-denied"]')).toBeVisible()
    })

    await test.step('Cannot access audit logs', async () => {
      await page.goto('/admin/audit-logs')
      await expect(page.locator('[data-error="access-denied"]')).toBeVisible()
    })
  })

  test('Admin can manage users but not delete admin accounts', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Create regular user', async () => {
      await page.click('[data-action="create-user"]')
      await page.fill('[name="firstName"]', 'Regular')
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', `regular${Date.now()}@example.com`)
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    await test.step('Cannot delete admin user', async () => {
      await page.goto('/admin/users')
      const adminUser = page.locator('[data-user-admin]').first()
      await adminUser.hover()
      await expect.soft(adminUser.locator('[data-action="delete"]')).toBeHidden()
    })
  })
})

test.describe('RBAC - User Manager Permissions', () => {
  test.use({ storageState: 'usermanager-auth.json' })

  test.serial('User manager can only manage users', async ({ page }) => {
    await test.step('Access user management', async () => {
      await page.goto('/admin/users')
      await expect(page.locator('[data-section="users"]')).toBeVisible()
      await expect(page.locator('[data-action="create-user"]')).toBeVisible()
    })

    await test.step('Cannot access role management', async () => {
      await page.goto('/admin/roles')
      await expect(page.locator('[data-error="access-denied"]')).toBeVisible()
    })

    await test.step('Cannot access permissions', async () => {
      await page.goto('/admin/permissions')
      await expect(page.locator('[data-error="access-denied"]')).toBeVisible()
    })

    await test.step('Cannot access system config', async () => {
      await page.goto('/admin/system')
      await expect(page.locator('[data-error="access-denied"]')).toBeVisible()
    })

    await test.step('Cannot access audit logs', async () => {
      await page.goto('/admin/audit-logs')
      await expect(page.locator('[data-error="access-denied"]')).toBeVisible()
    })
  })

  test('User manager can create and edit users', async ({ page }) => {
    await page.goto('/admin/users')

    await test.step('Create new user', async () => {
      await page.click('[data-action="create-user"]')
      await page.fill('[name="firstName"]', 'New')
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', `new${Date.now()}@example.com`)
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    await test.step('Edit existing user', async () => {
      await page.goto('/admin/users')
      await page.click('[data-user]:nth-child(1) [data-action="edit"]')
      await page.fill('[name="firstName"]', 'Updated')
      await page.click('[data-action="save-user"]')
      await expect(page.locator('[data-success="user-updated"]')).toBeVisible()
    })
  })
})

test.describe('RBAC - Viewer Permissions', () => {
  test.use({ storageState: 'viewer-auth.json' })

  test.serial('Viewer has read-only access', async ({ page }) => {
    await test.step('View admin dashboard', async () => {
      await page.goto('/admin/dashboard')
      await expect(page.locator('[data-admin="dashboard"]')).toBeVisible()
    })

    await test.step('View users list (no actions)', async () => {
      await page.goto('/admin/users')
      await expect(page.locator('[data-section="users"]')).toBeVisible()
      await expect.soft(page.locator('[data-action="create-user"]')).toBeHidden()
      await expect.soft(page.locator('[data-action="delete-user"]')).toBeHidden()
      await expect.soft(page.locator('[data-action="edit-user"]')).toBeHidden()
    })

    await test.step('Cannot modify any data', async () => {
      await page.goto('/admin/users')
      const createButton = page.locator('[data-action="create-user"]')
      await expect(createButton).toBeHidden()

      const deleteLinks = page.locator('[data-action="delete"]')
      await expect(deleteLinks).toHaveCount(0)
    })
  })
})

test.describe('RBAC - Permission Inheritance', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Child roles inherit parent permissions', async ({ page }) => {
    await page.goto('/admin/roles')

    await test.step('Create parent role with base permissions', async () => {
      await page.click('[data-action="create-role"]')
      await page.fill('[name="roleName"]', 'Parent Role')
      await page.check('[permission="user.read"]')
      await page.check('[permission="user.write"]')
      await page.click('[data-action="save-role"]')
    })

    await test.step('Create child role inheriting from parent', async () => {
      await page.click('[data-action="create-role"]')
      await page.fill('[name="roleName"]', 'Child Role')
      await page.selectOption('[name="parentRole"]', 'Parent Role')
      await page.check('[permission="user.delete"]') // Add one more
      await page.click('[data-action="save-role"]')
    })

    await test.step('Verify child has parent permissions', async () => {
      await page.click('[data-role="Child Role"] [data-action="view-permissions"]')
      await expect.soft(page.locator('[data-permission="user.read"]')).toHaveAttribute('data-inherited', 'true')
      await expect.soft(page.locator('[data-permission="user.write"]')).toHaveAttribute('data-inherited', 'true')
      await expect(page.locator('[data-permission="user.delete"]')).toHaveAttribute('data-inherited', 'false')
    })
  })
})

test.describe('RBAC - Dynamic Permission Checks', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Permissions update in real-time', async ({ page }) => {
    await page.goto('/admin/roles')

    await test.step('Create role with limited permissions', async () => {
      await page.click('[data-action="create-role"]')
      await page.fill('[name="roleName"]', 'Dynamic Role')
      await page.check('[permission="user.read"]')
      await page.click('[data-action="save-role"]')
    })

    await test.step('Edit role to add permissions', async () => {
      await page.click('[data-role="Dynamic Role"] [data-action="edit"]')
      await page.check('[permission="user.write"]')
      await page.check('[permission="user.delete"]')
      await page.click('[data-action="save-role"]')
    })

    await test.step('Verify permissions updated', async () => {
      await page.goto('/admin/users')
      // New permissions should allow delete action
      await expect.soft(page.locator('[data-action="delete"]')).toBeVisible()
    })
  })

  test('Permission changes require confirmation', async ({ page }) => {
    await page.goto('/admin/roles')

    await test.step('Attempt to modify critical permissions', async () => {
      await page.click('[data-role="Admin"] [data-action="edit"]')
      await page.uncheck('[permission="user.delete"]')
      await page.click('[data-action="save-role"]')
    })

    await test.step('Confirmation dialog appears', async () => {
      await expect(page.locator('[data-modal="confirm-permission-change"]')).toBeVisible()
    })

    await test.step('Confirm permission change', async () => {
      await page.click('[data-action="confirm-change"]')
      await expect(page.locator('[data-success="role-updated"]')).toBeVisible()
    })
  })
})

test.describe('RBAC - Multi-Tenant Permissions', () => {
  test.use({ storageState: 'tenant-admin-auth.json' })

  test('Tenant admin limited to own tenant', async ({ page }) => {
    await test.step('Access tenant dashboard', async () => {
      await page.goto('/admin/dashboard')
      await expect(page.locator('[data-tenant="current"]')).toBeVisible()
    })

    await test.step('View only own tenant users', async () => {
      await page.goto('/admin/users')
      const users = page.locator('[data-user]')
      const count = await users.count()

      for (let i = 0; i < count; i++) {
        await expect.soft(users.nth(i)).toHaveAttribute('data-tenant', 'current-tenant-id')
      }
    })

    await test.step('Cannot access other tenant data', async () => {
      await page.goto('/admin/users?tenant=other-tenant')
      await expect(page.locator('[data-error="tenant-access-denied"]')).toBeVisible()
    })
  })
})

test.describe('RBAC - Permission Matrix Validation', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('Complete permission matrix check', async ({ page }) => {
    await page.goto('/admin/permissions')

    await test.step('Generate permission matrix', async () => {
      await page.click('[data-action="generate-matrix"]')
      await expect(page.locator('[data-matrix="permissions"]')).toBeVisible()
    })

    await test.step('Validate all roles have correct permissions', async () => {
      const roles = ['Super Admin', 'Admin', 'User Manager', 'Viewer']
      const permissions = ['user.read', 'user.write', 'user.delete', 'role.manage']

      for (const role of roles) {
        for (const permission of permissions) {
          const cell = page.locator(`[data-role="${role}"][data-permission="${permission}"]`)
          const hasPermission = await cell.getAttribute('data-has-permission') === 'true'

          // Validate based on role
          if (role === 'Super Admin') {
            expect(hasPermission).toBe(true)
          } else if (role === 'Admin' && permission !== 'role.manage') {
            expect(hasPermission).toBe(true)
          } else if (role === 'User Manager' && (permission === 'user.read' || permission === 'user.write')) {
            expect(hasPermission).toBe(true)
          } else if (role === 'Viewer' && permission === 'user.read') {
            expect(hasPermission).toBe(true)
          } else {
            expect(hasPermission).toBe(false)
          }
        }
      }
    })
  })
})
