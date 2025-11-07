/**
 * Admin Testing Fixtures - Playwright 1.55+
 *
 * Custom fixtures using test.extend() for admin testing
 * Provides reusable admin contexts, page objects, and helpers
 *
 * Usage:
 * ```typescript
 * test('Admin test', async ({ adminPage }) => {
 *   await adminPage.createUser({ name: 'John' })
 * })
 * ```
 */

import { test as base, Page, BrowserContext, expect } from '@playwright/test'

// Define fixture types
interface AdminPageFixtures {
  adminPage: AdminPage
  superAdminPage: SuperAdminPage
  userManagerPage: UserManagerPage
  viewerPage: ViewerPage
  tenantAdminPage: TenantAdminPage
  createUser: (userData: UserData) => Promise<string>
  deleteUser: (email: string) => Promise<void>
  bulkImportUsers: (users: UserData[]) => Promise<ImportResult>
}

interface UserData {
  firstName: string
  lastName: string
  email: string
  username?: string
  role?: string
  department?: string
  status?: string
}

interface ImportResult {
  success: boolean
  imported: number
  errors: string[]
}

/**
 * Admin Page Object with common admin operations
 */
class AdminPage {
  constructor(public page: Page) {}

  async gotoDashboard() {
    await this.page.goto('/admin/dashboard')
    await expect(this.page.locator('[data-admin="dashboard"]')).toBeVisible()
  }

  async gotoUsers() {
    await this.page.goto('/admin/users')
    await expect(this.page.locator('[data-section="users"]')).toBeVisible()
  }

  async gotoRoles() {
    await this.page.goto('/admin/roles')
    await expect(this.page.locator('[data-section="roles"]')).toBeVisible()
  }

  async createUser(userData: UserData): Promise<string> {
    const timestamp = Date.now()
    const email = userData.email || `user${timestamp}@example.com`
    const username = userData.username || `user${timestamp}`

    await this.page.goto('/admin/users/create')

    await this.page.fill('[name="firstName"]', userData.firstName)
    await this.page.fill('[name="lastName"]', userData.lastName)
    await this.page.fill('[name="email"]', email)
    await this.page.fill('[name="username"]', username)

    if (userData.role) {
      await this.page.check(`[value="${userData.role}"]`)
    }

    if (userData.department) {
      await this.page.selectOption('[name="department"]', userData.department)
    }

    if (userData.status) {
      await this.page.selectOption('[name="status"]', userData.status)
    }

    await this.page.click('[data-action="submit-user"]')
    await expect(this.page.locator('[data-success="user-created"]')).toBeVisible()

    return email
  }

  async deleteUser(email: string) {
    await this.gotoUsers()
    await this.page.fill('[data-search="users"]', email)
    await this.page.press('[data-search="users"]', 'Enter')
    await this.page.click('[data-user] [data-action="delete"]')
    await this.page.click('[data-action="confirm-delete"]')
    await expect(this.page.locator('[data-success="user-deleted"]')).toBeVisible()
  }

  async editUser(email: string, updates: Partial<UserData>) {
    await this.gotoUsers()
    await this.page.fill('[data-search="users"]', email)
    await this.page.press('[data-search="users"]', 'Enter')
    await this.page.click('[data-user] [data-action="edit"]')

    if (updates.firstName) {
      await this.page.fill('[name="firstName"]', updates.firstName)
    }
    if (updates.lastName) {
      await this.page.fill('[name="lastName"]', updates.lastName)
    }
    if (updates.role) {
      await this.page.check(`[value="${updates.role}"]`)
    }

    await this.page.click('[data-action="save-user"]')
    await expect(this.page.locator('[data-success="user-updated"]')).toBeVisible()
  }

  async searchUsers(query: string) {
    await this.gotoUsers()
    await this.page.fill('[data-search="users"]', query)
    await this.page.press('[data-search="users"]', 'Enter')
  }

  async getUserCount(): Promise<number> {
    return await this.page.locator('[data-user]').count()
  }

  async exportUsers() {
    await this.gotoUsers()
    const downloadPromise = this.page.waitForEvent('download')
    await this.page.click('[data-action="export-users"]')
    await this.page.click('[data-action="download-export"]')
    return await downloadPromise
  }

  async importUsers(users: UserData[]): Promise<ImportResult> {
    await this.page.goto('/admin/users/import')

    const csv = this.generateCSV(users)
    await this.page.setInputFiles('[name="csvFile"]', {
      name: 'import.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from(csv)
    })

    await this.page.click('[data-action="preview-import"]')
    await this.page.click('[data-action="confirm-import"]')
    await expect(this.page.locator('[data-success="import-complete"]')).toBeVisible()

    const importedText = await this.page.locator('[data-stat="imported"]').textContent()
    return {
      success: true,
      imported: parseInt(importedText?.match(/\d+/)?.[0] || '0'),
      errors: []
    }
  }

  private generateCSV(users: UserData[]): string {
    const headers = 'firstName,lastName,email,username,role'
    const rows = users.map(user => {
      const username = user.username || `user${Date.now()}`
      return `${user.firstName},${user.lastName},${user.email},${username},${user.role || 'user'}`
    })
    return `${headers}\n${rows.join('\n')}`
  }

  async checkPermission(resource: string, action: string): Promise<boolean> {
    try {
      await this.page.goto(`/admin/${resource}`)
      const element = this.page.locator(`[data-action="${action}"]`)
      await element.waitFor({ timeout: 1000 })
      return await element.isVisible()
    } catch {
      return false
    }
  }

  async getSystemHealth() {
    await this.gotoDashboard()
    const cpu = await this.page.locator('[data-widget="cpu"] [data-value]').textContent()
    const memory = await this.page.locator('[data-widget="memory"] [data-value]').textContent()
    const disk = await this.page.locator('[data-widget="disk"] [data-value]').textContent()
    return { cpu, memory, disk }
  }
}

class SuperAdminPage extends AdminPage {
  constructor(page: Page) {
    super(page)
  }

  async canAccessAll() {
    const resources = ['users', 'roles', 'permissions', 'system', 'audit-logs']
    const permissions: Record<string, boolean> = {}

    for (const resource of resources) {
      permissions[resource] = await this.checkPermission(resource, 'create')
    }

    return permissions
  }

  async createRole(name: string, permissions: string[]) {
    await this.gotoRoles()
    await this.page.click('[data-action="create-role"]')
    await this.page.fill('[name="roleName"]', name)
    for (const perm of permissions) {
      await this.page.check(`[value="${perm}"]`)
    }
    await this.page.click('[data-action="save-role"]')
    await expect(this.page.locator('[data-success="role-created"]')).toBeVisible()
  }
}

class UserManagerPage extends AdminPage {
  constructor(page: Page) {
    super(page)
  }

  async canOnlyManageUsers() {
    const users = await this.checkPermission('users', 'create')
    const roles = await this.checkPermission('roles', 'create')
    const system = await this.checkPermission('system', 'view')
    return { users, roles, system }
  }
}

class ViewerPage extends AdminPage {
  constructor(page: Page) {
    super(page)
  }

  async hasReadOnlyAccess() {
    const createUser = await this.checkPermission('users', 'create')
    const editUser = await this.checkPermission('users', 'edit')
    const deleteUser = await this.checkPermission('users', 'delete')
    return { createUser, editUser, deleteUser }
  }
}

class TenantAdminPage extends AdminPage {
  constructor(page: Page) {
    super(page)
  }

  async isIsolatedToTenant(tenantId: string) {
    await this.gotoUsers()
    const users = this.page.locator('[data-user]')
    const count = await users.count()

    for (let i = 0; i < count; i++) {
      const tenant = await users.nth(i).getAttribute('data-tenant')
      if (tenant !== tenantId) {
        return false
      }
    }
    return true
  }

  async cannotAccessOtherTenant(tenantId: string) {
    await this.page.goto(`/admin/users?tenant=${tenantId}`)
    await expect(this.page.locator('[data-error="tenant-access-denied"]')).toBeVisible()
  }
}

// Extend test with custom fixtures
export const test = base.extend<AdminPageFixtures>({
  // Admin page with admin role
  adminPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: 'admin-auth.json'
    })
    const page = await context.newPage()
    const adminPage = new AdminPage(page)
    await use(adminPage)
    await context.close()
  },

  // Super admin page with full permissions
  superAdminPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: 'superadmin-auth.json'
    })
    const page = await context.newPage()
    const superAdminPage = new SuperAdminPage(page)
    await use(superAdminPage)
    await context.close()
  },

  // User manager page
  userManagerPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: 'usermanager-auth.json'
    })
    const page = await context.newPage()
    const userManagerPage = new UserManagerPage(page)
    await use(userManagerPage)
    await context.close()
  },

  // Viewer page with read-only access
  viewerPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: 'viewer-auth.json'
    })
    const page = await context.newPage()
    const viewerPage = new ViewerPage(page)
    await use(viewerPage)
    await context.close()
  },

  // Tenant admin page
  tenantAdminPage: async ({ browser }, use) => {
    const context = await browser.newContext({
      storageState: 'tenant-admin-auth.json'
    })
    const page = await context.newPage()
    const tenantAdminPage = new TenantAdminPage(page)
    await use(tenantAdminPage)
    await context.close()
  },

  // Helper function to create user
  createUser: async ({ adminPage }, use) => {
    await use(async (userData: UserData) => {
      return await adminPage.createUser(userData)
    })
  },

  // Helper function to delete user
  deleteUser: async ({ adminPage }, use) => {
    await use(async (email: string) => {
      await adminPage.deleteUser(email)
    })
  },

  // Helper function to bulk import users
  bulkImportUsers: async ({ adminPage }, use) => {
    await use(async (users: UserData[]) => {
      return await adminPage.importUsers(users)
    })
  }
})

// Export page classes for use in tests
export { AdminPage, SuperAdminPage, UserManagerPage, ViewerPage, TenantAdminPage }

// Export types
export type { UserData, ImportResult }
