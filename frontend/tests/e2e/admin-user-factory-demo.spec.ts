/**
 * UserFactory & AdminFactory - Demo Tests
 *
 * Demonstrates how to use UserFactory and AdminFactory for generating test data
 * Shows different user creation scenarios
 */

import { test, expect } from '@playwright/test'
import { UserFactory, UserProfiles } from '../framework/factories/user-admin.factory'

test.describe('UserFactory - Single User Creation', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Create standard user', async ({ page }) => {
    const user = UserFactory.createUser()

    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', user.firstName)
    await page.fill('[name="lastName"]', user.lastName)
    await page.fill('[name="email"]', user.email)
    await page.fill('[name="username"]', user.username)
    await page.check(`[value="${user.role}"]`)
    await page.selectOption('[name="department"]', user.department)

    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()
  })

  test('Create admin user', async ({ page }) => {
    const admin = UserFactory.createAdmin()

    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', admin.firstName)
    await page.fill('[name="lastName"]', admin.lastName)
    await page.fill('[name="email"]', admin.email)
    await page.fill('[name="username"]', admin.username)
    await page.check(`[value="${admin.role}"]`)
    await page.selectOption('[name="department"]', admin.department)

    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()
  })

  test('Create super admin', async ({ page }) => {
    const superAdmin = UserFactory.createSuperAdmin()

    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', superAdmin.firstName)
    await page.fill('[name="lastName"]', superAdmin.lastName)
    await page.fill('[name="email"]', superAdmin.email)
    await page.fill('[name="username"]', superAdmin.username)
    await page.check(`[value="${superAdmin.role}"]`)

    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()
  })

  test('Create user with custom overrides', async ({ page }) => {
    const user = UserFactory.createUser({
      firstName: 'Custom',
      lastName: 'User',
      email: 'custom@test.com',
      role: 'user-manager',
      department: 'Product',
      status: 'pending'
    })

    expect(user.firstName).toBe('Custom')
    expect(user.email).toBe('custom@test.com')
    expect(user.role).toBe('user-manager')
  })
})

test.describe('UserFactory - Bulk User Creation', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Create 10 standard users', async ({ page }) => {
    const users = UserFactory.createMany(10)

    expect(users).toHaveLength(10)
    expect(users[0].role).toBe('user')

    // Create first user
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', users[0].firstName)
    await page.fill('[name="lastName"]', users[0].lastName)
    await page.fill('[name="email"]', users[0].email)
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()
  })

  test('Create 5 admins', async ({ page }) => {
    const admins = UserFactory.createAdmins(5)

    expect(admins).toHaveLength(5)
    admins.forEach(admin => {
      expect(admin.role).toBe('admin')
      expect(admin.department).toBe('Operations')
    })
  })

  test('Create users with different roles', async () => {
    const users = UserFactory.createMixedRoles(20)

    expect(users).toHaveLength(20)
    const roles = new Set(users.map(u => u.role))
    expect(roles.size).toBeGreaterThan(1)
  })

  test('Create users by department', async ({ page }) => {
    const engineers = UserFactory.createByDepartment('Engineering', 5)

    expect(engineers).toHaveLength(5)
    engineers.forEach(user => {
      expect(user.department).toBe('Engineering')
    })
  })

  test('Create inactive users', async ({ page }) => {
    const inactiveUsers = UserFactory.createInactiveUsers(3)

    expect(inactiveUsers).toHaveLength(3)
    inactiveUsers.forEach(user => {
      expect(user.status).toBe('inactive')
    })
  })

  test('Create pending users', async ({ page }) => {
    const pendingUsers = UserFactory.createPendingUsers(3)

    expect(pendingUsers).toHaveLength(3)
    pendingUsers.forEach(user => {
      expect(user.status).toBe('pending')
    })
  })
})

test.describe('UserFactory - Predefined Profiles', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Use standardUser profile', async ({ page }) => {
    const user = UserProfiles.standardUser()

    expect(user.role).toBe('user')
    await page.goto('/admin/users/create')
    await page.fill('[name="email"]', user.email)
  })

  test('Use admin profile', async ({ page }) => {
    const admin = UserProfiles.admin()

    expect(admin.role).toBe('admin')
  })

  test('Use superAdmin profile', async ({ page }) => {
    const superAdmin = UserProfiles.superAdmin()

    expect(superAdmin.role).toBe('super-admin')
  })

  test('Create engineering team', async ({ page }) => {
    const team = UserProfiles.engineeringTeam(5)

    expect(team).toHaveLength(5)
    team.forEach(member => {
      expect(member.department).toBe('Engineering')
    })
  })

  test('Create sales team', async ({ page }) => {
    const team = UserProfiles.salesTeam(3)

    expect(team).toHaveLength(3)
    team.forEach(member => {
      expect(member.department).toBe('Sales')
    })
  })

  test('Use mixed profiles', async ({ page }) => {
    const users = UserProfiles.mixedRoles(15)

    expect(users).toHaveLength(15)
    const roles = users.map(u => u.role)
    expect(roles).toContain('user')
    expect(roles).toContain('viewer')
    expect(roles).toContain('user-manager')
  })
})

test.describe('UserFactory - Utility Methods', () => {
  test('Validate user data', async ({ page }) => {
    const validUser = UserFactory.createUser()
    const validation = UserFactory.validateUser(validUser)

    expect(validation.valid).toBe(true)
    expect(validation.errors).toHaveLength(0)
  })

  test('Detect invalid user data', async ({ page }) => {
    const invalidUser = {
      firstName: 'A', // Too short
      lastName: 'B', // Too short
      email: 'invalid-email', // Invalid format
      username: 'x', // Too short
      role: 'invalid-role',
      department: 'Invalid',
      status: 'invalid'
    }

    const validation = UserFactory.validateUser(invalidUser as any)

    expect(validation.valid).toBe(false)
    expect(validation.errors.length).toBeGreaterThan(0)
  })

  test('Generate unique email', async ({ page }) => {
    const baseEmail = 'test@example.com'
    const existing = ['test@example.com', 'test+1@example.com']
    const unique = UserFactory.generateUniqueEmail(baseEmail, existing)

    expect(unique).toBe('test+2@example.com')
  })

  test('Convert users to CSV', async ({ page }) => {
    const users = UserFactory.createMany(3)
    const csv = UserFactory.toCSV(users)

    expect(csv).toContain('firstName,lastName,email')
    expect(csv).toContain(users[0].firstName)
    expect(csv).toContain(users[1].email)
  })

  test('Parse CSV to users', async ({ page }) => {
    const csv = `firstName,lastName,email,username,role,department,team,status
John,Doe,john@example.com,johndoe,user,Engineering,Backend,active
Jane,Smith,jane@example.com,janesmith,admin,Operations,Admin,active`

    const users = UserFactory.fromCSV(csv)

    expect(users).toHaveLength(2)
    expect(users[0].firstName).toBe('John')
    expect(users[1].email).toBe('jane@example.com')
  })
})

test.describe('UserFactory - Large Scale Testing', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Create 100 standard users', async ({ page }) => {
    const users = UserFactory.createMany(100)

    expect(users).toHaveLength(100)
    // Verify all are unique
    const emails = new Set(users.map(u => u.email))
    expect(emails.size).toBe(100)
  })

  test('Create 50 users in each department', async ({ page }) => {
    const departments: any[] = ['Engineering', 'Product', 'Sales', 'Marketing', 'HR']
    let totalUsers = 0

    for (const dept of departments) {
      const users = UserFactory.createByDepartment(dept, 50)
      expect(users).toHaveLength(50)
      users.forEach(user => {
        expect(user.department).toBe(dept)
      })
      totalUsers += 50
    }

    expect(totalUsers).toBe(250)
  })

  test('Create sequential users for testing', async ({ page }) => {
    const users = UserFactory.createMany(5, { sequential: true })

    expect(users[0].email).toContain('user1@')
    expect(users[4].email).toContain('user5@')
  })

  test('Generate users with specific role', async ({ page }) => {
    const viewers = UserFactory.createMany(20, { role: 'viewer' })

    expect(viewers).toHaveLength(20)
    viewers.forEach(viewer => {
      expect(viewer.role).toBe('viewer')
    })
  })
})
