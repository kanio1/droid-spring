/**
 * Settings E2E Flow Tests
 *
 * Complete test coverage for system settings functionality
 * Tests user preferences, system configuration, theme switching, and language selection
 *
 * Priority: 🟡 MEDIUM
 * Learning Value: ⭐⭐⭐
 */

import { test, expect } from '@playwright/test'

test.describe('Settings Flow', () => {
  test.describe('Settings Navigation', () => {
    test('should navigate to settings page', async ({ page }) => {
      await page.goto('/settings')

      await expect(page.locator('h1.page-title')).toContainText('Settings')
      await expect(page.locator('text=Configure your preferences')).toBeVisible()
    })

    test('should display settings categories', async ({ page }) => {
      await page.goto('/settings')

      await expect(page.locator('[data-testid="settings-sidebar"]')).toBeVisible()
      await expect(page.locator('text=Profile')).toBeVisible()
      await expect(page.locator('text=Preferences')).toBeVisible()
      await expect(page.locator('text=Security')).toBeVisible()
      await expect(page.locator('text=Notifications')).toBeVisible()
      await expect(page.locator('text=Billing')).toBeVisible()
    })

    test('should navigate between settings sections', async ({ page }) => {
      await page.goto('/settings')

      // Navigate to Profile
      await page.click('[data-testid="profile-section"]')
      await expect(page.locator('[data-testid="profile-form"]')).toBeVisible()

      // Navigate to Preferences
      await page.click('[data-testid="preferences-section"]')
      await expect(page.locator('[data-testid="preferences-form"]')).toBeVisible()

      // Navigate to Security
      await page.click('[data-testid="security-section"]')
      await expect(page.locator('[data-testid="security-form"]')).toBeVisible()
    })
  })

  test.describe('Profile Settings', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/user/profile**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 'user-123',
            firstName: 'John',
            lastName: 'Doe',
            email: 'john.doe@example.com',
            phone: '+1234567890',
            department: 'IT',
            timezone: 'America/New_York',
            language: 'en',
            avatar: null
          })
        })
      })
    })

    test('should display user profile information', async ({ page }) => {
      await page.goto('/settings')

      await page.click('[data-testid="profile-section"]')

      await expect(page.locator('[name="firstName"]')).toHaveValue('John')
      await expect(page.locator('[name="lastName"]')).toHaveValue('Doe')
      await expect(page.locator('[name="email"]')).toHaveValue('john.doe@example.com')
      await expect(page.locator('[name="phone"]')).toHaveValue('+1234567890')
    })

    test('should update profile information', async ({ page }) => {
      await page.route('**/api/user/profile**', async (route) => {
        if (route.request().method() === 'PUT') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              id: 'user-123',
              firstName: 'Jane',
              lastName: 'Doe',
              email: 'jane.doe@example.com',
              phone: '+1234567890',
              department: 'IT',
              timezone: 'America/New_York',
              language: 'en',
              avatar: null
            })
          })
        }
      })

      await page.goto('/settings')
      await page.click('[data-testid="profile-section"]')

      // Update profile
      await page.fill('[name="firstName"]', 'Jane')
      await page.fill('[name="lastName"]', 'Doe')
      await page.fill('[name="email"]', 'jane.doe@example.com')

      await page.click('[data-testid="save-profile-btn"]')

      // Verify success
      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Profile updated successfully')
    })

    test('should validate email format', async ({ page }) => {
      await page.goto('/settings')
      await page.click('[data-testid="profile-section"]')

      await page.fill('[name="email"]', 'invalid-email')
      await page.click('[data-testid="save-profile-btn"]')

      await expect(page.locator('.error-message')).toContainText('Please enter a valid email address')
    })

    test('should upload avatar', async ({ page }) => {
      await page.route('**/api/user/avatar**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              avatarUrl: '/uploads/avatar-123.png'
            })
          })
        }
      })

      await page.goto('/settings')
      await page.click('[data-testid="profile-section"]')

      // Upload avatar
      const filePath = 'test-assets/avatar.png'
      await page.setInputFiles('[name="avatar"]', filePath)

      await expect(page.locator('[data-testid="avatar-preview"]')).toBeVisible()
      await page.click('[data-testid="upload-avatar-btn"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Avatar uploaded successfully')
    })
  })

  test.describe('Preferences', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/user/preferences**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            theme: 'light',
            language: 'en',
            timezone: 'America/New_York',
            dateFormat: 'MM/DD/YYYY',
            currency: 'USD',
            pageSize: 20,
            autoSave: true,
            emailNotifications: true,
            pushNotifications: false,
            compactMode: false
          })
        })
      })
    })

    test('should display current preferences', async ({ page }) => {
      await page.goto('/settings')

      await page.click('[data-testid="preferences-section"]')

      await expect(page.locator('[value="light"]')).toBeSelected()
      await expect(page.locator('[value="en"]')).toBeSelected()
      await expect(page.locator('[value="America/New_York"]')).toBeSelected()
      await expect(page.locator('[name="autoSave"]')).toBeChecked()
    })

    test('should switch theme', async ({ page }) => {
      await page.route('**/api/user/preferences**', async (route) => {
        if (route.request().method() === 'PATCH') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              theme: 'dark'
            })
          })
        }
      })

      await page.goto('/settings')
      await page.click('[data-testid="preferences-section"]')

      // Switch to dark theme
      await page.click('[data-testid="theme-dark"]')

      // Verify immediate UI change
      await expect(page.locator('html')).toHaveClass(/dark-theme/)
    })

    test('should change language', async ({ page }) => {
      await page.route('**/api/user/preferences**', async (route) => {
        if (route.request().method() === 'PATCH') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              language: 'es'
            })
          })
        }
      })

      await page.goto('/settings')
      await page.click('[data-testid="preferences-section"]')

      // Change language to Spanish
      await page.selectOption('[name="language"]', 'es')

      // Verify save button is enabled
      await expect(page.locator('[data-testid="save-preferences-btn"]')).toBeEnabled()
    })

    test('should update date format and currency', async ({ page }) => {
      await page.goto('/settings')
      await page.click('[data-testid="preferences-section"]')

      await page.selectOption('[name="dateFormat"]', 'DD/MM/YYYY')
      await page.selectOption('[name="currency"]', 'EUR')

      await page.click('[data-testid="save-preferences-btn"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Preferences saved successfully')
    })

    test('should toggle notification settings', async ({ page }) => {
      await page.goto('/settings')
      await page.click('[data-testid="preferences-section"]')

      // Toggle email notifications
      await page.uncheck('[name="emailNotifications"]')
      await page.check('[name="pushNotifications"]')

      await page.click('[data-testid="save-preferences-btn"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Preferences saved successfully')
    })
  })

  test.describe('Security Settings', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/user/security**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            twoFactorEnabled: true,
            loginNotifications: true,
            sessionTimeout: 30,
            passwordLastChanged: '2024-01-01',
            activeSessions: 3,
            trustedDevices: 2
          })
        })
      })
    })

    test('should display security status', async ({ page }) => {
      await page.goto('/settings')

      await page.click('[data-testid="security-section"]')

      await expect(page.locator('[data-testid="two-factor-status"]')).toContainText('Enabled')
      await expect(page.locator('[data-testid="login-notifications-status"]')).toContainText('Enabled')
      await expect(page.locator('[data-testid="session-timeout"]')).toContainText('30 minutes')
    })

    test('should enable two-factor authentication', async ({ page }) => {
      await page.route('**/api/user/security/2fa**', async (route) => {
        if (route.request().method() === 'POST') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              qrCode: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUg...',
              backupCodes: ['123456', '789012', '345678']
            })
          })
        }
      })

      await page.goto('/settings')
      await page.click('[data-testid="security-section"]')

      await page.click('[data-testid="enable-2fa-btn"]')

      // Verify QR code is displayed
      await expect(page.locator('[data-testid="qr-code"]')).toBeVisible()
      await expect(page.locator('[data-testid="backup-codes"]')).toBeVisible()
    })

    test('should change password', async ({ page }) => {
      await page.route('**/api/user/password**', async (route) => {
        if (route.request().method() === 'PUT') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              message: 'Password changed successfully'
            })
          })
        }
      })

      await page.goto('/settings')
      await page.click('[data-testid="security-section"]')

      await page.click('[data-testid="change-password-btn"]')

      // Fill password change form
      await page.fill('[name="currentPassword"]', 'currentPassword123')
      await page.fill('[name="newPassword"]', 'newPassword456')
      await page.fill('[name="confirmPassword"]', 'newPassword456')

      await page.click('[data-testid="submit-password-change"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Password changed successfully')
    })

    test('should validate password strength', async ({ page }) => {
      await page.goto('/settings')
      await page.click('[data-testid="security-section"]')

      await page.click('[data-testid="change-password-btn"]')

      await page.fill('[name="newPassword"]', 'weak')

      // Verify password strength indicator
      await expect(page.locator('[data-testid="password-strength"]'))
        .toContainText('Weak')
      await expect(page.locator('.strength-bar')).toHaveClass(/strength-weak/)
    })

    test('should show active sessions', async ({ page }) => {
      await page.goto('/settings')
      await page.click('[data-testid="security-section"]')

      await expect(page.locator('[data-testid="active-sessions"]')).toContainText('3')
      await expect(page.locator('[data-testid="session-item"]')).toHaveCount(3)
    })

    test('should revoke session', async ({ page }) => {
      await page.route('**/api/user/security/sessions/session-123**', async (route) => {
        if (route.request().method() === 'DELETE') {
          await route.fulfill({
            status: 204
          })
        }
      })

      await page.goto('/settings')
      await page.click('[data-testid="security-section"]')

      await page.click('[data-testid="revoke-session-session-123"]')

      // Confirm revocation
      await expect(page.locator('[data-testid="confirm-dialog"]')).toBeVisible()
      await page.click('[data-testid="confirm-revoke-btn"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Session revoked successfully')
    })
  })

  test.describe('Notification Settings', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/user/notifications**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            email: {
              invoices: true,
              payments: true,
              orders: true,
              subscriptions: true,
              maintenance: false
            },
            push: {
              invoices: false,
              payments: true,
              orders: false,
              subscriptions: true,
              maintenance: true
            },
            sms: {
              invoices: false,
              payments: false,
              orders: false,
              subscriptions: false,
              maintenance: true
            }
          })
        })
      })
    })

    test('should display notification preferences', async ({ page }) => {
      await page.goto('/settings')

      await page.click('[data-testid="notifications-section"]')

      // Email notifications
      await expect(page.locator('[name="email-invoices"]')).toBeChecked()
      await expect(page.locator('[name="email-payments"]')).toBeChecked()
      await expect(page.locator('[name="email-maintenance"]')).not.toBeChecked()

      // Push notifications
      await expect(page.locator('[name="push-payments"]')).toBeChecked()
      await expect(page.locator('[name="push-invoices"]')).not.toBeChecked()
    })

    test('should update notification preferences', async ({ page }) => {
      await page.goto('/settings')

      await page.click('[data-testid="notifications-section"]')

      // Update email notifications
      await page.uncheck('[name="email-invoices"]')
      await page.check('[name="email-maintenance"]')

      // Update push notifications
      await page.check('[name="push-invoices"]')

      await page.click('[data-testid="save-notifications-btn"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Notification preferences saved')
    })

    test('should test email notification', async ({ page }) => {
      await page.route('**/api/user/notifications/test**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Test email sent'
          })
        })
      })

      await page.goto('/settings')

      await page.click('[data-testid="notifications-section"]')

      await page.click('[data-testid="send-test-email-btn"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Test email sent')
    })
  })

  test.describe('Billing Settings', () => {
    test.beforeEach(async ({ page }) => {
      await page.route('**/api/user/billing**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            plan: 'Pro',
            billingCycle: 'monthly',
            nextBillingDate: '2024-02-15',
            autoRenew: true,
            paymentMethod: {
              type: 'card',
              last4: '4242',
              brand: 'Visa',
              expiryMonth: 12,
              expiryYear: 2025
            }
          })
        })
      })
    })

    test('should display billing information', async ({ page }) => {
      await page.goto('/settings')

      await page.click('[data-testid="billing-section"]')

      await expect(page.locator('[data-testid="current-plan"]')).toContainText('Pro')
      await expect(page.locator('[data-testid="billing-cycle"]')).toContainText('Monthly')
      await expect(page.locator('[data-testid="next-billing"]')).toContainText('2024-02-15')
      await expect(page.locator('[data-testid="payment-method"]')).toContainText('Visa ending in 4242')
    })

    test('should update auto-renew setting', async ({ page }) => {
      await page.route('**/api/user/billing/auto-renew**', async (route) => {
        if (route.request().method() === 'PATCH') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              autoRenew: false
            })
          })
        }
      })

      await page.goto('/settings')

      await page.click('[data-testid="billing-section"]')

      await page.uncheck('[name="autoRenew"]')

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Auto-renew disabled')
    })

    test('should navigate to payment method management', async ({ page }) => {
      await page.goto('/settings')

      await page.click('[data-testid="billing-section"]')

      await page.click('[data-testid="manage-payment-method-btn"]')

      // Should open payment method modal or navigate to payment page
      await expect(page.locator('[data-testid="payment-method-modal"]')).toBeVisible()
    })

    test('should download invoice', async ({ page }) => {
      await page.route('**/api/user/billing/invoices/inv-123**', async (route) => {
        await route.fulfill({
          status: 200,
          contentType: 'application/pdf',
          body: '%PDF-1.4 mock invoice data'
        })
      })

      await page.goto('/settings')

      await page.click('[data-testid="billing-section"]')

      await page.click('[data-testid="download-invoice-inv-123"]')

      // Verify download started
      const download = await page.waitForEvent('download')
      expect(download.suggestedFilename()).toMatch(/invoice.*\.pdf/)
    })
  })

  test.describe('Settings Persistence', () => {
    test('should persist theme preference across page reload', async ({ page }) => {
      await page.route('**/api/user/preferences**', async (route) => {
        if (route.request().method() === 'GET') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ theme: 'dark' })
          })
        } else if (route.request().method() === 'PATCH') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ theme: 'dark' })
          })
        }
      })

      await page.goto('/settings')
      await page.click('[data-testid="preferences-section"]')
      await page.click('[data-testid="theme-dark"]')

      // Reload page
      await page.reload()

      // Verify dark theme is applied
      await expect(page.locator('html')).toHaveClass(/dark-theme/)
    })

    test('should show unsaved changes warning', async ({ page }) => {
      await page.goto('/settings')
      await page.click('[data-testid="preferences-section"]')

      // Make changes
      await page.click('[data-testid="theme-dark"]')

      // Try to navigate away
      await page.click('[data-testid="security-section"]')

      // Should show warning
      await expect(page.locator('[data-testid="unsaved-changes-dialog"]')).toBeVisible()
    })

    test('should auto-save preferences', async ({ page }) => {
      await page.route('**/api/user/preferences**', async (route) => {
        if (route.request().method() === 'PATCH') {
          await route.fulfill({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({ autoSave: true })
          })
        }
      })

      await page.goto('/settings')
      await page.click('[data-testid="preferences-section"]')

      // Auto-save should trigger after changes
      await page.click('[data-testid="theme-dark"]')

      // Wait for auto-save (should happen within 2 seconds)
      await page.waitForTimeout(2500)

      await expect(page.locator('[data-testid="toast-success"]'))
        .toContainText('Preferences saved automatically')
    })
  })

  test.describe('Accessibility', () => {
    test('should support keyboard navigation', async ({ page }) => {
      await page.goto('/settings')

      // Tab through settings sections
      await page.keyboard.press('Tab')
      await expect(page.locator('[data-testid="profile-section"]')).toBeFocused()

      await page.keyboard.press('ArrowDown')
      await expect(page.locator('[data-testid="preferences-section"]')).toBeFocused()

      await page.keyboard.press('ArrowDown')
      await expect(page.locator('[data-testid="security-section"]')).toBeFocused()
    })

    test('should have proper ARIA labels', async ({ page }) => {
      await page.goto('/settings')

      await expect(page.locator('[data-testid="settings-page"]'))
        .toHaveAttribute('aria-label', 'User settings')

      await expect(page.locator('[data-testid="preferences-section"]'))
        .toHaveAttribute('aria-label', 'User preferences settings')
    })

    test('should support keyboard shortcuts', async ({ page }) => {
      await page.goto('/settings')

      // Press 's' to focus on search
      await page.keyboard.press('s')

      const focused = await page.evaluate(() => document.activeElement?.getAttribute('name'))
      expect(focused).toBe('search')
    })
  })
})
