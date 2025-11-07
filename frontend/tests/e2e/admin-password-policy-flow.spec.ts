/**
 * Admin Password Policy - Comprehensive Testing
 *
 * Tests for password security policies and validation
 * Uses Playwright 1.55+ features:
 * - test.serial() for complete password workflows
 * - test.step() for step-by-step validation
 * - expect.soft() for non-blocking password checks
 * - test.repeat() for password strength testing
 *
 * Coverage: Password complexity, history, expiration, lockout, MFA
 */

import { test, expect } from '@playwright/test'
import { UserFactory } from '../framework/factories/user-admin.factory'

test.describe('Admin Password Policy - Complexity Requirements', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Password complexity validation - all requirements', async ({ page }) => {
    const userEmail = `password-test-${Date.now()}@example.com`

    await test.step('Navigate to password policy settings', async () => {
      await page.goto('/admin/security/password-policy')
      await expect(page.locator('[data-section="password-policy"]')).toBeVisible()
    })

    await test.step('View current password requirements', async () => {
      await expect(page.locator('[data-policy="minLength"]')).toContainText('12 characters')
      await expect(page.locator('[data-policy="requireUppercase"]')).toContainText('true')
      await expect(page.locator('[data-policy="requireLowercase"]')).toContainText('true')
      await expect(page.locator('[data-policy="requireNumbers"]')).toContainText('true')
      await expect(page.locator('[data-policy="requireSpecialChars"]')).toContainText('true')
      await expect(page.locator('[data-policy="specialChars"]')).toContainText('!@#$%^&*')
    })

    await test.step('Create user with password validation', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Password')
      await page.fill('[name="lastName"]', 'Test')
      await page.fill('[name="email"]', userEmail)
      await page.fill('[name="username"]', `passwordtest${Date.now()}`)
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    await test.step('Test weak password (too short)', async () => {
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', userEmail)
      await page.press('[data-search="users"]', 'Enter')
      await page.click('[data-user] [data-action="edit"]')

      await page.fill('[name="password"]', 'short')
      await page.click('[data-action="save-user"]')

      await expect(page.locator('[data-error="password-too-short"]'))
        .toContainText('Password must be at least 12 characters')
    })

    await test.step('Test password without uppercase', async () => {
      await page.fill('[name="password"]', 'password123!')
      await page.click('[data-action="save-user"]')

      await expect(page.locator('[data-error="password-no-uppercase"]'))
        .toContainText('Password must contain at least one uppercase letter')
    })

    await test.step('Test password without lowercase', async () => {
      await page.fill('[name="password"]', 'PASSWORD123!')
      await page.click('[data-action="save-user"]')

      await expect(page.locator('[data-error="password-no-lowercase"]'))
        .toContainText('Password must contain at least one lowercase letter')
    })

    await test.step('Test password without numbers', async () => {
      await page.fill('[name="password"]', 'Password!')
      await page.click('[data-action="save-user"]')

      await expect(page.locator('[data-error="password-no-numbers"]'))
        .toContainText('Password must contain at least one number')
    })

    await test.step('Test password without special chars', async () => {
      await page.fill('[name="password"]', 'Password123')
      await page.click('[data-action="save-user"]')

      await expect(page.locator('[data-error="password-no-special"]'))
        .toContainText('Password must contain at least one special character')
    })

    await test.step('Test valid strong password', async () => {
      await page.fill('[name="password"]', 'StrongPass123!')
      await page.click('[data-action="save-user"]')
      await expect(page.locator('[data-success="user-updated"]')).toBeVisible()
    })
  })

  test('Password strength meter', async ({ page }) => {
    await page.goto('/admin/users/create')

    await test.step('Test password strength levels', async () => {
      const passwordField = page.locator('[name="password"]')
      const strengthMeter = page.locator('[data-strength-meter]')

      // Very weak password
      await passwordField.fill('123')
      await expect(strengthMeter).toHaveAttribute('data-strength', 'very-weak')
      await expect(page.locator('[data-strength="very-weak"]')).toBeVisible()

      // Weak password
      await passwordField.fill('password')
      await expect(strengthMeter).toHaveAttribute('data-strength', 'weak')
      await expect(page.locator('[data-strength="weak"]')).toBeVisible()

      // Medium password
      await passwordField.fill('Password123')
      await expect(strengthMeter).toHaveAttribute('data-strength', 'medium')
      await expect(page.locator('[data-strength="medium"]')).toBeVisible()

      // Strong password
      await passwordField.fill('Password123!')
      await expect(strengthMeter).toHaveAttribute('data-strength', 'strong')
      await expect(page.locator('[data-strength="strong"]')).toBeVisible()

      // Very strong password
      await passwordField.fill('VeryStrongP@ssw0rd2024!')
      await expect(strengthMeter).toHaveAttribute('data-strength', 'very-strong')
      await expect(page.locator('[data-strength="very-strong"]')).toBeVisible()
    })

    await test.step('Verify strength score calculation', async () => {
      const score = await page.locator('[data-strength-score]').textContent()
      const scoreNum = parseInt(score || '0')
      expect(scoreNum).toBeGreaterThan(80)
    })
  })

  test('Password prohibited patterns', async ({ page }) => {
    await page.goto('/admin/users/create')

    await test.step('Test common password patterns', async () => {
      // Test sequential characters
      await page.fill('[name="password"]', '123456789012')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-error="password-sequential"]'))
        .toContainText('Password cannot contain sequential characters')

      // Test repeated characters
      await page.fill('[name="password"]', 'aaaaaaaaaaaa')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-error="password-repeated"]'))
        .toContainText('Password cannot contain repeated characters')

      // Test common passwords
      await page.fill('[name="password"]', 'password123!')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-error="password-common"]'))
        .toContainText('Password is too common')

      // Test company name
      await page.fill('[name="password"]', 'Company123!')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-error="password-company"]'))
        .toContainText('Password cannot contain company name')
    })
  })
})

test.describe('Admin Password Policy - History & Expiration', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Password history enforcement', async ({ page }) => {
    const userEmail = `history-test-${Date.now()}@example.com`

    // Step 1: Create user
    await test.step('Create user with initial password', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'History')
      await page.fill('[name="lastName"]', 'Test')
      await page.fill('[name="email"]', userEmail)
      await page.fill('[name="username"]', `historytest${Date.now()}`)
      await page.fill('[name="password"]', 'OldPassword123!')
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    // Step 2: Try to reuse the same password
    await test.step('Update user with same password (should fail)', async () => {
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', userEmail)
      await page.press('[data-search="users"]', 'Enter')
      await page.click('[data-user] [data-action="edit"]')

      await page.fill('[name="password"]', 'OldPassword123!')
      await page.click('[data-action="save-user"]')

      await expect(page.locator('[data-error="password-reused"]'))
        .toContainText('Password was used recently. Choose a different password')
    })

    // Step 3: Use different password
    await test.step('Update with different password', async () => {
      await page.fill('[name="password"]', 'NewPassword456!')
      await page.click('[data-action="save-user"]')
      await expect(page.locator('[data-success="user-updated"]')).toBeVisible()
    })

    // Step 4: Try to reuse the new password
    await test.step('Update user with same password again (should fail)', async () => {
      await page.fill('[name="password"]', 'NewPassword456!')
      await page.click('[data-action="save-user"]')

      await expect(page.locator('[data-error="password-reused"]'))
        .toContainText('Password was used recently. Choose a different password')
    })
  })

  test('Password expiration policy', async ({ page }) => {
    await page.goto('/admin/security/password-policy')

    await test.step('Check expiration settings', async () => {
      await expect(page.locator('[data-policy="expirationDays"]')).toContainText('90 days')
      await expect(page.locator('[data-policy="expirationEnabled"]')).toContainText('true')
    })

    // Create a user
    const userEmail = `expiration-test-${Date.now()}@example.com`
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Expiration')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', userEmail)
    await page.fill('[name="username"]', `expirationtest${Date.now()}`)
    await page.fill('[name="password"]', 'TempPassword123!')
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await test.step('Verify user receives password expiration warning', async () => {
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', userEmail)
      await page.press('[data-search="users"]', 'Enter')

      // Check if expiration warning is shown
      const warningIcon = page.locator('[data-user] [data-warning="password-expiring"]')
      await expect(warningIcon).toBeVisible()
    })
  })

  test('Password age calculation', async ({ page }) => {
    const userEmail = `age-test-${Date.now()}@example.com`

    // Create user
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'Age')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', userEmail)
    await page.fill('[name="username"]', `agetest${Date.now()}`)
    await page.fill('[name="password"]', 'TestPassword123!')
    await page.check('[value="user"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await test.step('Check password age in user profile', async () => {
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', userEmail)
      await page.press('[data-search="users"]', 'Enter')
      await page.click('[data-user] [data-action="view"]')

      await expect(page.locator('[data-field="passwordAge"]')).toContainText('0 days')
      await expect(page.locator('[data-field="passwordLastChanged"]')).toBeVisible()
    })
  })
})

test.describe('Admin Password Policy - Account Lockout', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('Account lockout after failed attempts', async ({ page }) => {
    await page.goto('/admin/logout')

    const testEmail = 'admin@company.com'

    await test.step('Attempt invalid login multiple times', async () => {
      for (let i = 0; i < 5; i++) {
        await page.goto('/login')
        await page.fill('[name="username"]', testEmail)
        await page.fill('[name="password"]', 'wrongpassword')
        await page.click('[data-action="login"]')
        await page.waitForTimeout(500)
      }
    })

    await test.step('Verify account is locked', async () => {
      await page.goto('/login')
      await page.fill('[name="username"]', testEmail)
      await page.fill('[name="password"]', 'wrongpassword')
      await page.click('[data-action="login"]')

      await expect(page.locator('[data-error="account-locked"]'))
        .toContainText('Account is locked due to too many failed attempts')
    })

    await test.step('Check lockout duration', async () => {
      await expect(page.locator('[data-info="lockout-duration"]'))
        .toContainText('15 minutes')
    })
  })

  test('Account lockout with progressive delays', async ({ page }) => {
    await page.goto('/admin/logout')

    const testEmail = 'admin@company.com'

    await test.step('Test progressive lockout delays', async () => {
      const attempts = [1, 2, 3, 4, 5]
      const expectedDelays = ['0s', '30s', '1m', '5m', '15m']

      for (let i = 0; i < attempts.length; i++) {
        await page.goto('/login')
        await page.fill('[name="username"]', testEmail)
        await page.fill('[name="password"]', 'wrongpassword')
        await page.click('[data-action="login"]')

        const errorMsg = await page.locator('[data-error]').textContent()
        if (i >= 1) {
          // Check that delay is mentioned
          expect(errorMsg || '').toContain(expectedDelays[i])
        }
      }
    })
  })

  test('Admin unlock account functionality', async ({ page }) => {
    // First lock an account
    await page.goto('/admin/logout')
    const testEmail = 'testuser@company.com'

    for (let i = 0; i < 5; i++) {
      await page.goto('/login')
      await page.fill('[name="username"]', testEmail)
      await page.fill('[name="password"]', 'wrongpassword')
      await page.click('[data-action="login"]')
    }

    // Re-login as admin
    await page.goto('/login')
    await page.fill('[name="username"]', 'admin')
    await page.fill('[name="password"]', 'admin123')
    await page.click('[data-action="login"]')
    await expect(page).toHaveURL('/admin/dashboard')

    await test.step('Admin unlocks user account', async () => {
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', testEmail)
      await page.press('[data-search="users"]', 'Enter')

      const unlockButton = page.locator('[data-user] [data-action="unlock"]')
      await expect(unlockButton).toBeVisible()
      await unlockButton.click()

      await expect(page.locator('[data-success="account-unlocked"]'))
        .toContainText('Account unlocked successfully')
    })

    await test.step('Verify user can login after unlock', async () => {
      await page.goto('/admin/logout')

      await page.goto('/login')
      await page.fill('[name="username"]', testEmail)
      await page.fill('[name="password"]', 'correctpassword')
      await page.click('[data-action="login"]')

      // Should not see lockout error
      await expect(page.locator('[data-error="account-locked"]')).toBeHidden()
    })
  })
})

test.describe('Admin Password Policy - Reset Flow', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.serial('Password reset request flow', async ({ page }) => {
    const userEmail = `reset-test-${Date.now()}@example.com`

    // Step 1: Create user
    await test.step('Create user for password reset test', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Reset')
      await page.fill('[name="lastName"]', 'Test')
      await page.fill('[name="email"]', userEmail)
      await page.fill('[name="username"]', `resettest${Date.now()}`)
      await page.fill('[name="password"]', 'OriginalPass123!')
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()
    })

    // Step 2: Request password reset
    await test.step('Request password reset', async () => {
      await page.goto('/login')
      await page.click('[data-action="forgot-password"]')
      await expect(page).toHaveURL('/forgot-password')

      await page.fill('[name="email"]', userEmail)
      await page.click('[data-action="request-reset"]')
      await expect(page.locator('[data-success="reset-email-sent"]'))
        .toContainText('Password reset instructions sent to your email')
    })

    // Step 3: Simulate email link (in real app, would come from email)
    await test.step('Navigate to reset link (simulated)', async () => {
      // In a real scenario, this would be accessed via email link
      // For testing, we'll construct the reset URL
      const resetToken = `token-${Date.now()}`
      await page.goto(`/reset-password?token=${resetToken}&email=${encodeURIComponent(userEmail)}`)
      await expect(page).toHaveURL(/.*reset-password.*/)
    })

    // Step 4: Set new password
    await test.step('Set new password', async () => {
      await page.fill('[name="password"]', 'NewStrongPass456!')
      await page.fill('[name="confirmPassword"]', 'NewStrongPass456!')
      await page.click('[data-action="reset-password"]')
      await expect(page.locator('[data-success="password-reset"]'))
        .toContainText('Password has been reset successfully')
    })

    // Step 5: Login with new password
    await test.step('Login with new password', async () => {
      await page.goto('/login')
      await page.fill('[name="username"]', userEmail)
      await page.fill('[name="password"]', 'NewStrongPass456!')
      await page.click('[data-action="login"]')
      await expect(page).toHaveURL(/.*dashboard.*/)
    })
  })

  test('Password reset token validation', async ({ page }) => {
    await test.step('Test with invalid reset token', async () => {
      await page.goto('/reset-password?token=invalid-token&email=test@example.com')
      await expect(page.locator('[data-error="invalid-token"]'))
        .toContainText('Invalid or expired reset token')
    })

    await test.step('Test with expired token', async () => {
      // Simulate expired token (24 hours old)
      const expiredTime = Date.now() - (24 * 60 * 60 * 1000) - 1000
      const expiredToken = `expired-${expiredTime}`
      await page.goto(`/reset-password?token=${expiredToken}&email=test@example.com`)

      await expect(page.locator('[data-error="expired-token"]'))
        .toContainText('Reset token has expired. Please request a new one')
    })

    await test.step('Test token reuse prevention', async () => {
      // Create valid token
      const token = `valid-${Date.now()}`
      await page.goto(`/reset-password?token=${token}&email=test@example.com`)

      // First use should work
      await page.fill('[name="password"]', 'NewPass123!')
      await page.fill('[name="confirmPassword"]', 'NewPass123!')
      await page.click('[data-action="reset-password"]')

      // Second use should fail
      await page.goto(`/reset-password?token=${token}&email=test@example.com`)
      await expect(page.locator('[data-error="token-already-used"]'))
        .toContainText('Reset token has already been used')
    })
  })

  test('Password reset rate limiting', async ({ page }) => {
    const testEmail = 'ratelimit-test@example.com'

    await test.step('Request multiple password resets', async () => {
      for (let i = 0; i < 5; i++) {
        await page.goto('/forgot-password')
        await page.fill('[name="email"]', testEmail)
        await page.click('[data-action="request-reset"]')
        await page.waitForTimeout(500)
      }
    })

    await test.step('Verify rate limiting is applied', async () => {
      await page.goto('/forgot-password')
      await page.fill('[name="email"]', testEmail)
      await page.click('[data-action="request-reset"]')

      await expect(page.locator('[data-error="rate-limited"]'))
        .toContainText('Too many password reset requests. Please try again later')
    })
  })
})

test.describe('Admin Password Policy - Multi-Factor Authentication', () => {
  test.use({ storageState: 'admin-auth.json' })

  test('MFA enforcement policy', async ({ page }) => {
    await page.goto('/admin/security/password-policy')

    await test.step('Check MFA requirements', async () => {
      await expect(page.locator('[data-policy="mfaRequired"]')).toContainText('true')
      await expect(page.locator('[data-policy="mfaMethods"]')).toContainText('TOTP, SMS, Email')
    })

    // Create user with MFA requirement
    const userEmail = `mfa-test-${Date.now()}@example.com`
    await page.goto('/admin/users/create')
    await page.fill('[name="firstName"]', 'MFA')
    await page.fill('[name="lastName"]', 'Test')
    await page.fill('[name="email"]', userEmail)
    await page.fill('[name="username"]', `mfatest${Date.now()}`)
    await page.check('[value="user"]')
    await page.check('[name="requireMFA"]')
    await page.click('[data-action="submit-user"]')
    await expect(page.locator('[data-success="user-created"]')).toBeVisible()

    await test.step('Verify user must set up MFA', async () => {
      // User would be redirected to MFA setup on first login
      // For testing, check the user profile
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', userEmail)
      await page.press('[data-search="users"]', 'Enter')
      await page.click('[data-user] [data-action="view"]')

      await expect(page.locator('[data-field="mfaStatus"]')).toContainText('Not configured')
    })
  })

  test('MFA setup flow', async ({ page }) => {
    await page.goto('/admin/logout')

    // Login as a user who needs to set up MFA
    await page.goto('/login')
    await page.fill('[name="username"]', 'mfa-test-user')
    await page.fill('[name="password"]', 'SecurePass123!')
    await page.click('[data-action="login"]')

    await test.step('Redirected to MFA setup', async () => {
      await expect(page).toHaveURL(/.*mfa-setup.*/)
      await expect(page.locator('[data-section="mfa-setup"]')).toBeVisible()
    })

    await test.step('Configure TOTP MFA', async () => {
      await page.click('[data-action="setup-totp"]')

      // QR code should be displayed
      await expect(page.locator('[data-mfa="qr-code"]')).toBeVisible()

      // Show manual entry key
      await page.click('[data-action="show-secret-key"]')
      const secretKey = await page.locator('[data-field="secret-key"]').textContent()
      expect(secretKey).toBeTruthy()

      // Enter verification code (simulated)
      await page.fill('[name="verificationCode"]', '123456')
      await page.click('[data-action="verify-mfa"]')
      await expect(page.locator('[data-success="mfa-configured"]'))
        .toContainText('Multi-factor authentication configured successfully')
    })
  })
})

test.describe('Admin Password Policy - Bulk Operations', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('Bulk password reset', async ({ page }) => {
    // Create multiple users
    await test.step('Create test users', async () => {
      for (let i = 0; i < 5; i++) {
        await page.goto('/admin/users/create')
        await page.fill('[name="firstName"]', `User${i}`)
        await page.fill('[name="lastName"]', `Test${i}`)
        await page.fill('[name="email"]', `bulk-test-${i}-${Date.now()}@example.com`)
        await page.fill('[name="username"]', `bulktest${i}${Date.now()}`)
        await page.check('[value="user"]')
        await page.click('[data-action="submit-user"]')
        await expect(page.locator('[data-success="user-created"]')).toBeVisible()
      }
    })

    await test.step('Select users for bulk password reset', async () => {
      await page.goto('/admin/users')

      for (let i = 1; i <= 3; i++) {
        await page.check(`[data-user]:nth-child(${i}) [data-checkbox]`)
      }

      await page.click('[data-action="bulk-actions"]')
      await page.click('[data-action="bulk-reset-password"]')
    })

    await test.step('Confirm bulk reset', async () => {
      await page.fill('[name="confirmation"]', 'RESET')
      await page.click('[data-action="confirm-bulk-reset"]')
      await expect(page.locator('[data-success="bulk-reset-complete"]'))
        .toContainText('Passwords reset for 3 users')
    })

    await test.step('Verify users must change password on next login', async () => {
      await page.goto('/admin/users')

      for (let i = 1; i <= 3; i++) {
        const userRow = page.locator(`[data-user]:nth-child(${i})`)
        const forceChange = await userRow.locator('[data-field="forcePasswordChange"]').textContent()
        expect(forceChange).toBe('true')
      }
    })
  })

  test('Password policy override for admin users', async ({ page }) => {
    await page.goto('/admin/security/password-policy')

    await test.step('Configure admin password policy', async () => {
      await page.click('[data-action="edit-admin-policy"]')

      // Admins might have stricter requirements
      await page.fill('[name="minLength"]', '16')
      await page.check('[requireUppercase"]')
      await page.check('[requireNumbers"]')
      await page.check('[requireSpecialChars"]')
      await page.check('[requireSpecialChars2"]') // Additional special chars

      await page.click('[data-action="save-policy"]')
      await expect(page.locator('[data-success="policy-updated"]'))
        .toContainText('Password policy updated successfully')
    })

    await test.step('Verify admin password requirements', async () => {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Admin')
      await page.fill('[name="lastName"]', 'Test')
      await page.fill('[name="email"]', `admin-test-${Date.now()}@example.com`)
      await page.fill('[name="username"]', `admintest${Date.now()}`)

      // Try weak password - should fail
      await page.fill('[name="password"]', 'Admin123!')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-error="password-too-short-admin"]'))
        .toContainText('Password must be at least 16 characters for admin users')
    })
  })
})

test.describe('Admin Password Policy - Compliance', () => {
  test.use({ storageState: 'superadmin-auth.json' })

  test('Password encryption verification', async ({ page }) => {
    await page.goto('/admin/security/password-policy')

    await test.step('Check password encryption settings', async () => {
      await expect(page.locator('[data-policy="encryption"]')).toContainText('bcrypt')
      await expect(page.locator('[data-policy="bcryptCost"]')).toContainText('12')
      await expect(page.locator('[data-policy="peppering"]')).toContainText('true')
    })

    await test.step('Verify passwords are encrypted in database', async () => {
      // This would typically require backend API verification
      // For UI testing, we check that plain text is never displayed
      const userEmail = `encryption-test-${Date.now()}@example.com`

      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', 'Encryption')
      await page.fill('[name="lastName"]', 'Test')
      await page.fill('[name="email"]', userEmail)
      await page.fill('[name="username"]', `encryptiontest${Date.now()}`)
      await page.fill('[name="password"]', 'TestPass123!')
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
      await expect(page.locator('[data-success="user-created"]')).toBeVisible()

      // View user details - password should not be visible
      await page.goto('/admin/users')
      await page.fill('[data-search="users"]', userEmail)
      await page.press('[data-search="users"]', 'Enter')
      await page.click('[data-user] [data-action="view"]')

      const passwordField = page.locator('[data-field="password"]')
      await expect(passwordField).toHaveText('***encrypted***')
    })
  })

  test('Password policy compliance report', async ({ page }) => {
    await page.goto('/admin/security/password-policy')

    await test.step('Generate compliance report', async () => {
      const downloadPromise = page.waitForEvent('download')
      await page.click('[data-action="generate-compliance-report"]')
      await page.click('[data-action="download-report"]')
      const download = await downloadPromise

      expect(download.suggestedFilename()).toMatch(/password-policy-compliance.*\.pdf/)
    })

    await test.step('Verify report contents', async () => {
      // Check that all compliance requirements are met
      await expect(page.locator('[data-compliance="sox"]')).toContainText('Compliant')
      await expect(page.locator('[data-compliance="pci-dss"]')).toContainText('Compliant')
      await expect(page.locator('[data-compliance="gdpr"]')).toContainText('Compliant')
    })
  })
})

test.describe('Admin Password Policy - Performance Testing', () => {
  test.use({ storageState: 'admin-auth.json' })

  test.repeat(10, 'Password validation performance', async ({ page }) => {
    const start = Date.now()

    await page.goto('/admin/users/create')
    await page.fill('[name="password"]', 'StrongPassword123!')
    await page.click('[data-action="submit-user"]')

    const duration = Date.now() - start
    expect(duration).toBeLessThan(1000) // Should validate within 1 second
  })

  test('Password hashing performance under load', async ({ page }) => {
    await page.goto('/admin/security/password-policy')

    const start = Date.now()

    // Create many users to test password hashing
    for (let i = 0; i < 50; i++) {
      await page.goto('/admin/users/create')
      await page.fill('[name="firstName"]', `Load${i}`)
      await page.fill('[name="lastName"]', 'Test')
      await page.fill('[name="email"]', `load-test-${i}-${Date.now()}@example.com`)
      await page.fill('[name="username"]', `loadtest${i}${Date.now()}`)
      await page.fill('[name="password"]', `StrongPass${i}!`)
      await page.check('[value="user"]')
      await page.click('[data-action="submit-user"]')
    }

    const duration = Date.now() - start
    expect(duration).toBeLessThan(60000) // Should complete within 60 seconds
  })
})
