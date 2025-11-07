/**
 * Admin Fixtures - Demo Tests
 *
 * Demonstrates how to use custom fixtures created with test.extend()
 * Shows reusable admin operations and page objects
 */

import { test, expect } from '../framework/fixtures/admin-fixtures'

test.describe('Using Admin Fixtures', () => {
  test('Create user using adminPage fixture', async ({ adminPage }) => {
    const email = await adminPage.createUser({
      firstName: 'John',
      lastName: 'Doe',
      email: `john${Date.now()}@example.com`,
      role: 'user',
      department: 'Engineering'
    })

    expect(email).toMatch(/@example\.com$/)
  })

  test('Delete user using helper function', async ({ adminPage, createUser, deleteUser }) => {
    const email = await createUser({
      firstName: 'Delete',
      lastName: 'Me',
      email: `delete${Date.now()}@example.com`
    })

    await deleteUser(email)
    // User should be deleted
  })

  test('Use superAdminPage fixture for full access', async ({ superAdminPage }) => {
    const permissions = await superAdminPage.canAccessAll()

    expect(permissions.users).toBe(true)
    expect(permissions.roles).toBe(true)
    expect(permissions.permissions).toBe(true)
  })

  test('Use userManagerPage fixture for limited access', async ({ userManagerPage }) => {
    const permissions = await userManagerPage.canOnlyManageUsers()

    expect(permissions.users).toBe(true)
    expect(permissions.roles).toBe(false)
    expect(permissions.system).toBe(false)
  })

  test('Use viewerPage fixture for read-only', async ({ viewerPage }) => {
    const readOnly = await viewerPage.hasReadOnlyAccess()

    expect(readOnly.createUser).toBe(false)
    expect(readOnly.editUser).toBe(false)
    expect(readOnly.deleteUser).toBe(false)
  })

  test('Bulk import using adminPage', async ({ adminPage, bulkImportUsers }) => {
    const users = [
      {
        firstName: 'User1',
        lastName: 'Test1',
        email: `user1${Date.now()}@example.com`,
        role: 'user'
      },
      {
        firstName: 'User2',
        lastName: 'Test2',
        email: `user2${Date.now()}@example.com`,
        role: 'user'
      }
    ]

    const result = await bulkImportUsers(users)

    expect(result.success).toBe(true)
    expect(result.imported).toBe(2)
  })

  test('System health check', async ({ adminPage }) => {
    const health = await adminPage.getSystemHealth()

    expect(health.cpu).toMatch(/\d+%/)
    expect(health.memory).toMatch(/\d+%/)
    expect(health.disk).toMatch(/\d+%/)
  })

  test('Edit user with adminPage', async ({ adminPage, createUser }) => {
    const email = await createUser({
      firstName: 'Original',
      lastName: 'Name',
      email: `edit${Date.now()}@example.com`
    })

    await adminPage.editUser(email, {
      firstName: 'Updated',
      lastName: 'Name'
    })

    // User should be updated
  })

  test('Export users', async ({ adminPage, createUser }) => {
    await createUser({
      firstName: 'Export',
      lastName: 'Test',
      email: `export${Date.now()}@example.com`
    })

    const download = await adminPage.exportUsers()
    expect(download.suggestedFilename()).toMatch(/\.csv$/)
  })
})
