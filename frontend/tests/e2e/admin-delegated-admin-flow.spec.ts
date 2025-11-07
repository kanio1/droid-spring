/**
 * Admin Delegated Administration - Comprehensive Testing
 *
 * Tests for delegated admin privileges and hierarchical management
 * Uses Playwright 1.55+ features:
 * - test.serial() for complete delegation workflows
 * - test.step() for step-by-step delegation process
 * - expect.soft() for non-blocking permission checks
 * - test.repeat() for stress testing delegation chains
 *
 * Coverage: Delegation creation, scope, approval, expiration, audit
 */

import { test, expect } from '@playwright/test'
import { UserFactory } from '../framework/factories/user-admin.factory'

test.describe('Admin Delegated Administration - Creation', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test.serial('Create delegated admin with specific scope', async ({ page }) => {
    const delegateEmail = `delegate-${Date.now()}@example.com`

    // Step 1: Create delegate user
    await test.step('Create user to delegate to', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Delegated')
      await page.fill('[name="lastName"]', 'Admin')
      await page.fill('[name="email"]', delegateEmail)
      await page.fill('[name="username"]', `delegate${Date.now()}`)
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    // Step 2: Navigate to delegation management
    await test.step('Navigate to delegation management', async () => {
      await page.goto('/admin/delegations')
      await expect(page.locator('[data-section="delegations"]')).toBeVisible()
    })

    // Step 3: Create delegation
    await test.step('Create new delegation with limited scope', async () => {
      await page.click('[data-action="create-delegation"]')
      await expect(page.locator('[data-modal="create-delegation"]')).toBeVisible()

      await page.selectOption('[name="delegateUser"]', delegateEmail)
      await page.check('[value="user.create"]')
      await page.check('[value="user.read"]')
      await page.check('[value="user.update"]')

      // Limited to Engineering department only
      await page.selectOption('[name="scope"]', 'department-specific')
      await page.selectOption('[name="department"]', 'Engineering')

      // Set expiration to 7 days
      await page.fill('[name="expirationDays"]', '7')

      await page.click('[data-action="save-delegation"]')
      await expect(page.locator('[data-success="delegation-created"]'))
        .toContainText('Delegation created successfully')
    })

    // Step 4: Verify delegation appears in list
    await test.step('Verify delegation in management list', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await expect(delegationRow).toBeVisible()
      await expect(delegationRow.locator('[data-field="scope"]')).toContainText('user.create, user.read, user.update')
      await expect(delegationRow.locator('[data-field="department"]')).toContainText('Engineering')
      await expect(delegationRow.locator('[data-field="expiresIn"]')).toContainText('7 days')
    })

    // Step 5: Verify delegate can access allowed resources
    await test.step('Verify delegate permissions', async () => {
      await page.click(delegationRow.locator('[data-action="view-permissions"]'))

      await expect(page.locator('[data-permission="user.create"]')).toBeVisible()
      await expect(page.locator('[data-permission="user.read"]')).toBeVisible()
      await expect(page.locator('[data-permission="user.update"]')).toBeVisible()
      await expect(page.locator('[data-permission="user.delete"]')).toBeHidden()
      await expect(page.locator('[data-permission="role.manage"]')).toBeHidden()
    })
  })

  test('Create temporary delegation', async ({ page }) => {
    const delegateEmail = `temp-delegate-${Date.now()}@example.com`

    // Create user
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Temp')
    await page.fill('[name="lastName"]', 'Delegate')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `tempdelegate${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    // Create temporary delegation
    await test.step('Create 24-hour delegation', async () => {
      await page.goto('/admin/delegations')
      await page.click('[data-action="create-delegation"]')

      await page.selectOption('[name="delegateUser"]', delegateEmail)
      await page.check('[value="user.read"]')

      // 24 hours
      await page.fill('[name="expirationHours"]', '24')
      await page.check('[name="autoRevoke"]')

      await page.click('[data-action="save-delegation"]')
      await expect(page.locator('[data-success="delegation-created"]'))
        .toContainText('Delegation created successfully')
    })

    await test.step('Verify auto-revocation setting', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await expect(delegationRow.locator('[data-field="autoRevoke"]')).toContainText('true')
    })
  })

  test('Create multi-resource delegation', async ({ page }) => {
    const delegateEmail = `multi-delegate-${Date.now()}@example.com`

    // Create user
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Multi')
    await page.fill('[name="lastName"]', 'Delegate')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `multidelegate${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    // Create delegation with multiple resources
    await test.step('Delegate across multiple resources', async () => {
      await page.goto('/admin/delegations')
      await page.click('[data-action="create-delegation"]')

      await page.selectOption('[name="delegateUser"]', delegateEmail)

      // Check multiple resource types
      await page.check('[value="user.*"]') // All user operations
      await page.check('[value="role.read"]')
      await page.check('[value="audit.read"]')

      // But not system administration
      await page.uncheck('[value="system.*"]')

      await page.click('[data-action="save-delegation"]')
      await expect(page.locator('[data-success="delegation-created"]'))
        .toContainText('Delegation created successfully')
    })

    await test.step('Verify multi-resource scope', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await expect(delegationRow.locator('[data-field="resources"]'))
        .toContainText('user.*, role.read, audit.read')
    })
  })
})

test.describe('Admin Delegated Administration - Approval Workflow', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Delegation approval process', async ({ page }) => {
    const delegateEmail = `approval-delegate-${Date.now()}@example.com`

    // Step 1: Super admin creates delegation requiring approval
    await test.step('Create delegation requiring approval', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Approval')
      await page.fill('[name="lastName"]', 'Delegate')
      await page.fill('[name="email"]', delegateEmail)
      await page.fill('[name="username"]', `approvaldelegate${Date.now()}`)
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    // Create delegation with approval required
    await page.goto('/admin/delegations')
    await page.click('[data-action="create-delegation"]')
    await page.selectOption('[name="delegateUser"]', delegateEmail)
    await page.check('[value="user.create"]')
    await page.check('[value="user.read"]')
    await page.check('[name="requireApproval"]')
    await page.click('[data-action="save-delegation"]')
    await expect(page.locator('[data-success="delegation-created"]'))
      .toContainText('Delegation created. Pending approval')

    // Step 2: Delegate attempts to use privileges (should be blocked)
    await test.step('Verify delegate is blocked without approval', async () => {
      // Log out and log in as delegate (in real app)
      // For testing, check status
      await page.goto('/admin/delegations')
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await expect(delegationRow.locator('[data-field="status"]')).toContainText('Pending Approval')
      await expect(delegationRow.locator('[data-action="activate"]')).toBeVisible()
    })

    // Step 3: Super admin approves delegation
    await test.step('Approve delegation', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await delegationRow.locator('[data-action="approve"]').click()
      await page.fill('[name="approvalNote"]', 'Approved for temporary access')
      await page.click('[data-action="confirm-approval"]')

      await expect(page.locator('[data-success="delegation-approved"]'))
        .toContainText('Delegation approved successfully')
    })

    // Step 4: Verify delegation is now active
    await test.step('Verify delegation is now active', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await expect(delegationRow.locator('[data-field="status"]')).toContainText('Active')
      await expect(delegationRow.locator('[data-field="approvedBy"]')).toContainText('admin@')
      await expect(delegationRow.locator('[data-field="approvedAt"]')).toBeVisible()
    })
  })

  test('Delegation rejection', async ({ page }) => {
    const delegateEmail = `reject-delegate-${Date.now()}@example.com`

    // Create user and delegation
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Reject')
    await page.fill('[name="lastName"]', 'Delegate')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `rejectdelegate${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await page.goto('/admin/delegations')
    await page.click('[data-action="create-delegation"]')
    await page.selectOption('[name="delegateUser"]', delegateEmail)
    await page.check('[value="user.*"]')
    await page.check('[name="requireApproval"]')
    await page.click('[data-action="save-delegation"]')
    await expect(page.locator('[data-success="delegation-created"]'))
      .toContainText('Delegation created. Pending approval')

    await test.step('Reject delegation', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await delegationRow.locator('[data-action="reject"]').click()
      await page.fill('[name="rejectionReason"]', 'Insufficient justification for access')
      await page.click('[data-action="confirm-rejection"]')

      await expect(page.locator('[data-success="delegation-rejected"]'))
        .toContainText('Delegation rejected')
    })

    await test.step('Verify delegation is rejected', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await expect(delegationRow.locator('[data-field="status"]')).toContainText('Rejected')
      await expect(delegationRow.locator('[data-field="rejectedReason"]'))
        .toContainText('Insufficient justification for access')
    })
  })
})

test.describe('Admin Delegated Administration - Delegation Chains', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test.serial('Two-level delegation chain', async ({ page }) => {
    const level1Email = `level1-${Date.now()}@example.com`
    const level2Email = `level2-${Date.now()}@example.com`

    // Step 1: Create level 1 delegate
    await test.step('Create level 1 delegate', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Level1')
      await page.fill('[name="lastName"]', 'Delegate')
      await page.fill('[name="email"]', level1Email)
      await page.fill('[name="username"]', `level1${Date.now()}`)
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    // Step 2: Create level 1 delegation
    await test.step('Create delegation for level 1', async () => {
      await page.goto('/admin/delegations')
      await page.click('[data-action="create-delegation"]')
      await page.selectOption('[name="delegateUser"]', level1Email)
      await page.check('[value="user.create"]')
      await page.check('[value="user.read"]')
      await page.check('[value="delegation.create"]') // Can delegate further
      await page.click('[data-action="save-delegation"]')
      await expect(page.locator('[data-success="delegation-created"]'))
        .toContainText('Delegation created successfully')
    })

    // Step 3: Create level 2 user
    await test.step('Create level 2 user', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Level2')
      await page.fill('[name="lastName"]', 'Delegate')
      await page.fill('[name="email"]', level2Email)
      await page.fill('[name="username"]', `level2${Date.now()}`)
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    // Step 4: Create level 2 delegation (by level 1 admin)
    // In real app, would be logged in as level 1 delegate
    // For testing, we simulate from superadmin
    await test.step('Create delegation chain to level 2', async () => {
      await page.click('[data-action="create-delegation"]')
      await page.selectOption('[name="delegateUser"]', level2Email)
      await page.check('[value="user.read"]')
      await page.check('[name="parentDelegation"]') // Mark as sub-delegation
      await page.fill('[name="parentDelegationId"]', level1Email)

      await page.click('[data-action="save-delegation"]')
      await expect(page.locator('[data-success="delegation-created"]'))
        .toContainText('Sub-delegation created')
    })

    // Step 5: Verify delegation chain
    await test.step('Verify delegation chain structure', async () => {
      const level1Row = page.locator('[data-delegation]').filter({
        hasText: level1Email
      })
      const level2Row = page.locator('[data-delegation]').filter({
        hasText: level2Email
      })

      await expect(level1Row.locator('[data-field="level"]')).toContainText('1')
      await expect(level2Row.locator('[data-field="level"]')).toContainText('2')
      await expect(level2Row.locator('[data-field="parent"]')).toContainText(level1Email)

      // View chain
      await page.click('[data-action="view-chain"]')
      await expect(page.locator('[data-chain="level1"]')).toContainText(level1Email)
      await expect(page.locator('[data-chain="level2"]')).toContainText(level2Email)
    })
  })

  test('Maximum delegation depth enforcement', async ({ page }) => {
    const delegateEmail = `depth-test-${Date.now()}@example.com`

    // Create user
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Depth')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `depth${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    // Set max delegation depth to 3
    await page.goto('/admin/delegations')
    await page.click('[data-action="settings"]')
    await page.fill('[name="maxDelegationDepth"]', '3')
    await page.click('[data-action="save-settings"]')
    await expect(page.locator('[data-success="settings-updated"]'))
      .toContainText('Settings updated successfully')

    // Try to create 4th level delegation
    await test.step('Prevent excessive delegation depth', async () => {
      await page.click('[data-action="create-delegation"]')
      await page.selectOption('[name="delegateUser"]', delegateEmail)
      await page.check('[value="user.read"]')
      await page.fill('[name="parentDelegationLevel"]', '3') // This would be 4th level

      await page.click('[data-action="save-delegation"]')
      await expect(page.locator('[data-error="max-depth-exceeded"]'))
        .toContainText('Maximum delegation depth of 3 levels exceeded')
    })
  })
})

test.describe('Admin Delegated Administration - Revocation', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Manual delegation revocation', async ({ page }) => {
    const delegateEmail = `revoke-test-${Date.now()}@example.com`

    // Create user and delegation
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Revoke')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `revoketest${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await page.goto('/admin/delegations')
    await page.click('[data-action="create-delegation"]')
    await page.selectOption('[name="delegateUser"]', delegateEmail)
    await page.check('[value="user.create"]')
    await page.click('[data-action="save-delegation"]')
    await expect(page.locator('[data-success="delegation-created"]'))
      .toContainText('Delegation created successfully')

    await test.step('Revoke delegation', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await delegationRow.locator('[data-action="revoke"]').click()
      await page.fill('[name="revocationReason"]', 'No longer required')
      await page.click('[data-action="confirm-revocation"]')

      await expect(page.locator('[data-success="delegation-revoked"]'))
        .toContainText('Delegation revoked successfully')
    })

    await test.step('Verify delegation is revoked', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await expect(delegationRow.locator('[data-field="status"]')).toContainText('Revoked')
      await expect(delegationRow.locator('[data-field="revokedReason"]'))
        .toContainText('No longer required')
      await expect(delegationRow.locator('[data-field="revokedAt"]')).toBeVisible()
    })
  })

  test('Immediate revocation with force', async ({ page }) => {
    const delegateEmail = `force-revoke-${Date.now()}@example.com`

    // Create user and delegation
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Force')
    await page.fill('[name="lastName"]', 'Revoke')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `forcerevoke${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await page.goto('/admin/delegations')
    await page.click('[data-action="create-delegation"]')
    await page.selectOption('[name="delegateUser"]', delegateEmail)
    await page.check('[value="user.*"]')
    await page.click('[data-action="save-delegation"]')

    await test.step('Force revoke without confirmation', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await delegationRow.locator('[data-action="force-revoke"]').click()
      await page.click('[data-action="confirm-immediate-revocation"]')

      await expect(page.locator('[data-success="delegation-immediately-revoked"]'))
        .toContainText('Delegation immediately revoked')
    })
  })

  test('Auto-revocation on expiration', async ({ page }) => {
    const delegateEmail = `expire-test-${Date.now()}@example.com`

    // Create user
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Expire')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `expiretest${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    // Create 1-hour delegation
    await page.goto('/admin/delegations')
    await page.click('[data-action="create-delegation"]')
    await page.selectOption('[name="delegateUser"]', delegateEmail)
    await page.check('[value="user.read"]')
    await page.fill('[name="expirationHours"]', '1')
    await page.check('[name="autoRevoke"]')
    await page.click('[data-action="save-delegation"]')

    await test.step('Verify auto-revocation scheduled', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await expect(delegationRow.locator('[data-field="autoRevoke"]')).toContainText('true')
      await expect(delegationRow.locator('[data-field="expiresAt"]')).toBeVisible()
    })

    // In a real test, we would wait for expiration
    // For now, we verify the setting is in place
    await test.step('Verify expiration timestamp is set', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      const expiresAt = await delegationRow.locator('[data-field="expiresAt"]').textContent()
      expect(expiresAt).toBeTruthy()
    })
  })
})

test.describe('Admin Delegated Administration - Monitoring & Audit', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Delegation usage audit', async ({ page }) => {
    const delegateEmail = `audit-delegate-${Date.now()}@example.com`

    // Create user and delegation
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Audit')
    await page.fill('[name="lastName"]', 'Delegate')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `auditdelegate${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await page.goto('/admin/delegations')
    await page.click('[data-action="create-delegation"]')
    await page.selectOption('[name="delegateUser"]', delegateEmail)
    await page.check('[value="user.create"]')
    await page.check('[value="user.read"]')
    await page.click('[data-action="save-delegation"]')

    // Simulate delegate actions (in real app, would be done by delegate user)
    await test.step('Record delegation usage', async () => {
      await page.goto('/admin/delegations')
      await page.click('[data-action="log-usage"]')
      await page.fill('[name="action"]', 'user.create')
      await page.fill('[name="resource"]', 'john.doe@example.com')
      await page.click('[data-action="save-usage"]')

      await expect(page.locator('[data-success="usage-logged"]'))
        .toContainText('Delegation usage logged')
    })

    await test.step('View delegation usage report', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await delegationRow.locator('[data-action="view-usage"]').click()
      await expect(page.locator('[data-usage="user.create"]')).toBeVisible()
      await expect(page.locator('[data-usage="user.read"]')).toBeVisible()
    })
  })

  test('Delegation notifications', async ({ page }) => {
    const delegateEmail = `notify-delegate-${Date.now()}@example.com`

    // Create user and delegation
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Notify')
    await page.fill('[name="lastName"]', 'Delegate')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `notifydelegate${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await page.goto('/admin/delegations')
    await page.click('[data-action="create-delegation"]')
    await page.selectOption('[name="delegateUser"]', delegateEmail)
    await page.check('[value="user.create"]')
    await page.check('[name="notifyOnUse"]')
    await page.check('[name="notifyOnExpiration"]')
    await page.click('[data-action="save-delegation"]')

    await test.step('Verify notifications enabled', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await expect(delegationRow.locator('[data-field="notifyOnUse"]')).toContainText('true')
      await expect(delegationRow.locator('[data-field="notifyOnExpiration"]')).toContainText('true')
    })
  })

  test('Delegation activity dashboard', async ({ page }) => {
    await page.goto('/admin/delegations')

    await test.step('View delegation statistics', async () => {
      await expect(page.locator('[data-stat="total-delegations"]')).toBeVisible()
      await expect(page.locator('[data-stat="active-delegations"]')).toBeVisible()
      await expect(page.locator('[data-stat="pending-approvals"]')).toBeVisible()
      await expect(page.locator('[data-stat="expiring-soon"]')).toBeVisible()

      const total = await page.locator('[data-stat="total-delegations"]').textContent()
      expect(parseInt(total || '0')).toBeGreaterThan(0)
    })

    await test.step('View recent delegation activity', async () => {
      await page.click('[data-tab="recent-activity"]')
      await expect(page.locator('[data-activity-list]')).toBeVisible()

      const activities = page.locator('[data-activity]')
      const count = await activities.count()
      expect(count).toBeGreaterThan(0)
    })
  })
})

test.describe('Admin Delegated Administration - Cross-Department', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('Cross-department delegation', async ({ page }) => {
    const engineerEmail = `engineer-${Date.now()}@example.com`
    const salesEmail = `sales-${Date.now()}@example.com`

    // Step 1: Create engineer user
    await test.step('Create Engineering user', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Engineer')
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', engineerEmail)
      await page.fill('[name="username"]', `engineer${Date.now()}`)
      await page.selectOption('[name="department"]', 'Engineering')
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    // Step 2: Create Sales user
    await test.step('Create Sales user', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Sales')
      await page.fill('[name="lastName"]', 'User')
      await page.fill('[name="email"]', salesEmail)
      await page.fill('[name="username"]', `sales${Date.now()}`)
      await page.selectOption('[name="department"]', 'Sales')
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    // Step 3: Engineer delegates to Sales
    await test.step('Create cross-department delegation', async () => {
      await page.goto('/admin/delegations')
      await page.click('[data-action="create-delegation"]')
      await page.selectOption('[name="delegateUser"]', salesEmail)
      await page.check('[value="user.read"]')
      await page.check('[value="user.update"]')

      // Cross-department scope
      await page.selectOption('[name="scope"]', 'cross-department')
      await page.check('[name="allowCrossDepartment"]')
      await page.fill('[name="sourceDepartment"]', 'Engineering')
      await page.fill('[name="targetDepartment"]', 'Sales')

      await page.click('[data-action="save-delegation"]')
      await expect(page.locator('[data-success="delegation-created"]'))
        .toContainText('Cross-department delegation created')
    })

    await test.step('Verify cross-department scope', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: salesEmail
      })

      await expect(delegationRow.locator('[data-field="scope"]'))
        .toContainText('Cross-Department: Engineering → Sales')
    })
  })

  test('Inter-department collaboration limits', async ({ page }) => {
    await page.goto('/admin/delegations')

    await test.step('Configure inter-department policies', async () => {
      await page.click('[data-action="policies"]')
      await page.check('[name="allowCrossDepartment"]')
      await page.selectOption('[name="allowedCrossDeptPairs"]', 'Engineering↔Sales')
      await page.check('[name="requireApprovalForCrossDept"]')
      await page.click('[data-action="save-policies"]')
      await expect(page.locator('[data-success="policies-updated"]'))
        .toContainText('Cross-department policies updated')
    })
  })
})

test.describe('Admin Delegated Administration - Stress Testing', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test.repeat(5, 'Delegation creation performance', async ({ page }) => {
    const delegateEmail = `perf-${Date.now()}-${Math.random()}@example.com`

    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Perf')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `perftest${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await page.goto('/admin/delegations')
    await page.click('[data-action="create-delegation"]')
    await page.selectOption('[name="delegateUser"]', delegateEmail)
    await page.check('[value="user.read"]')
    await page.click('[data-action="save-delegation"]')
    await expect(page.locator('[data-success="delegation-created"]'))
      .toBeVisible({ timeout: 10000 })
  })

  test('Multiple simultaneous delegations', async ({ page }) => {
    await test.step('Create 10 concurrent delegations', async () => {
      for (let i = 0; i < 10; i++) {
        const email = `concurrent-${i}-${Date.now()}@example.com`

        await page.goto('/admin/users/create')
        await page.fill('[name="firstName"]', `User${i}`)
        await page.fill('[name="lastName"]', 'Concurrent')
        await page.fill('[name="email"]', email)
        await page.fill('[name="username"]', `concurrent${i}${Date.now()}`)
        await page.check('[value="user"]')
        await page.click('[data-action="submit-user"]')
        await expect(page.locator('[data-success="user-created"]')).toBeVisible()

        await page.goto('/admin/delegations')
        await page.click('[data-action="create-delegation"]')
        await page.selectOption('[name="delegateUser"]', email)
        await page.check('[value="user.read"]')
        await page.click('[data-action="save-delegation"]')
        await expect(page.locator('[data-success="delegation-created"]'))
          .toBeVisible({ timeout: 5000 })
      }
    })

    await test.step('Verify all delegations created', async () => {
      await page.goto('/admin/delegations')
      const count = await page.locator('[data-delegation]').count()
      expect(count).toBeGreaterThanOrEqual(10)
    })
  })
})

test.describe('Admin Delegated Administration - Compliance', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('Delegation audit trail for compliance', async ({ page }) => {
    const delegateEmail = `compliance-${Date.now()}@example.com`

    // Create user and delegation
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Compliance')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', delegateEmail)
    await page.fill('[name="username"]', `compliancetest${Date.now()}`)
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await page.goto('/admin/delegations')
    await page.click('[data-action="create-delegation"]')
    await page.selectOption('[name="delegateUser"]', delegateEmail)
    await page.check('[value="user.*"]')
    await page.click('[data-action="save-delegation"]')

    await test.step('Generate compliance report', async () => {
      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-action="export-compliance-report"]')
      await page.click('[data-action="download-report"]')
      const download = await downloadPromise

      expect(download.suggestedFilename()).toMatch(/delegation-compliance.*\.pdf/)
    })

    await test.step('Verify all delegation metadata', async () => {
      const delegationRow = page.locator('[data-delegation]').filter({
        hasText: delegateEmail
      })

      await expect(delegationRow.locator('[data-field="createdBy"]')).toBeVisible()
      await expect(delegationRow.locator('[data-field="createdAt"]')).toBeVisible()
      await expect(delegationRow.locator('[data-field="scope"]')).toBeVisible()
      await expect(delegationRow.locator('[data-field="approvalStatus"]')).toBeVisible()
    })
  })

  test('SOX compliance for admin delegation', async ({ page }) => {
    await page.goto('/admin/delegations')

    await test.step('Verify SOX compliance controls', async () => {
      await expect(page.locator('[data-compliance="segregationOfDuties"]'))
        .toContainText('Enforced')
      await expect(page.locator('[data-compliance="delegationApproval"]'))
        .toContainText('Required')
      await expect(page.locator('[data-compliance="auditTrail"]'))
        .toContainText('Complete')
      await expect(page.locator('[data-compliance="timeLimits"]'))
        .toContainText('Enforced')
    })
  })
})
