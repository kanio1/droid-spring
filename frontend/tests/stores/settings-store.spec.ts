/**
 * SettingsStore Tests
 *
 * Comprehensive test coverage for settings store
 * Tests preferences, theme, localization, and application settings
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSettingsStore } from '@/stores/settings'
import type { Theme, Language, Settings } from '@/types/settings'

// Mock API client
vi.mock('@/services/api', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn()
  }
}))

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn()
}
vi.stubGlobal('localStorage', localStorageMock)

// Mock theme detection
vi.mock('@/utils/theme', () => ({
  detectSystemTheme: vi.fn(),
  applyTheme: vi.fn()
}))

describe('SettingsStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorageMock.clear.mockClear()
  })

  describe('Initial State', () => {
    it('should have correct initial state', () => {
      const settingsStore = useSettingsStore()

      expect(settingsStore.theme).toBe('system')
      expect(settingsStore.language).toBe('en')
      expect(settingsStore.currency).toBe('USD')
      expect(settingsStore.timezone).toBe('UTC')
      expect(settingsStore.dateFormat).toBe('YYYY-MM-DD')
      expect(settingsStore.timeFormat).toBe('24h')
      expect(settingsStore.notifications).toEqual({
        email: true,
        push: true,
        sms: false,
        invoiceUpdates: true,
        orderUpdates: true,
        marketingEmails: false
      })
      expect(settingsStore.preferences).toEqual({
        itemsPerPage: 20,
        autoSave: true,
        confirmActions: true,
        showTutorials: true,
        compactMode: false
      })
      expect(settingsStore.isLoading).toBe(false)
      expect(settingsStore.error).toBeNull()
    })
  })

  describe('Theme Management', () => {
    it('should set light theme', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('light')

      expect(settingsStore.theme).toBe('light')
      expect(localStorageMock.setItem).toHaveBeenCalledWith('theme', 'light')
    })

    it('should set dark theme', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('dark')

      expect(settingsStore.theme).toBe('dark')
      expect(localStorageMock.setItem).toHaveBeenCalledWith('theme', 'dark')
    })

    it('should set system theme', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('system')

      expect(settingsStore.theme).toBe('system')
      expect(localStorageMock.setItem).toHaveBeenCalledWith('theme', 'system')
    })

    it('should toggle theme', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('light')
      settingsStore.toggleTheme()

      expect(settingsStore.theme).toBe('dark')

      settingsStore.toggleTheme()
      expect(settingsStore.theme).toBe('light')
    })

    it('should detect and apply system theme', () => {
      const settingsStore = useSettingsStore()
      const { detectSystemTheme } = require('@/utils/theme')
      const { applyTheme } = require('@/utils/theme')

      vi.mocked(detectSystemTheme).mockReturnValue('dark')

      settingsStore.applySystemTheme()

      expect(detectSystemTheme).toHaveBeenCalled()
      expect(applyTheme).toHaveBeenCalledWith('dark')
    })

    it('should initialize theme from localStorage', () => {
      localStorageMock.getItem.mockImplementation((key: string) => {
        if (key === 'theme') return 'dark'
        return null
      })

      const settingsStore = useSettingsStore()

      expect(settingsStore.theme).toBe('dark')
      expect(localStorageMock.getItem).toHaveBeenCalledWith('theme')
    })

    it('should use default theme when localStorage is empty', () => {
      localStorageMock.getItem.mockReturnValue(null)

      const settingsStore = useSettingsStore()

      expect(settingsStore.theme).toBe('system')
    })

    it('should persist theme to localStorage when changed', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('dark')

      expect(localStorageMock.setItem).toHaveBeenCalledWith('theme', 'dark')
    })
  })

  describe('Language & Localization', () => {
    it('should set language', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setLanguage('pl')

      expect(settingsStore.language).toBe('pl')
      expect(localStorageMock.setItem).toHaveBeenCalledWith('language', 'pl')
    })

    it('should set currency', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setCurrency('EUR')

      expect(settingsStore.currency).toBe('EUR')
      expect(localStorageMock.setItem).toHaveBeenCalledWith('currency', 'EUR')
    })

    it('should set timezone', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTimezone('Europe/Warsaw')

      expect(settingsStore.timezone).toBe('Europe/Warsaw')
      expect(localStorageMock.setItem).toHaveBeenCalledWith('timezone', 'Europe/Warsaw')
    })

    it('should set date format', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setDateFormat('DD/MM/YYYY')

      expect(settingsStore.dateFormat).toBe('DD/MM/YYYY')
      expect(localStorageMock.setItem).toHaveBeenCalledWith('dateFormat', 'DD/MM/YYYY')
    })

    it('should set time format', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTimeFormat('12h')

      expect(settingsStore.timeFormat).toBe('12h')
      expect(localStorageMock.setItem).toHaveBeenCalledWith('timeFormat', '12h')
    })

    it('should get available languages', () => {
      const settingsStore = useSettingsStore()

      const languages = settingsStore.getAvailableLanguages()

      expect(languages).toEqual(
        expect.arrayContaining([
          expect.objectContaining({ code: 'en', name: 'English' }),
          expect.objectContaining({ code: 'pl', name: 'Polski' }),
          expect.objectContaining({ code: 'de', name: 'Deutsch' })
        ])
      )
    })

    it('should get available currencies', () => {
      const settingsStore = useSettingsStore()

      const currencies = settingsStore.getAvailableCurrencies()

      expect(currencies).toEqual(
        expect.arrayContaining([
          expect.objectContaining({ code: 'USD', symbol: '$' }),
          expect.objectContaining({ code: 'EUR', symbol: '€' }),
          expect.objectContaining({ code: 'PLN', symbol: 'zł' })
        ])
      )
    })

    it('should initialize localization from localStorage', () => {
      localStorageMock.getItem.mockImplementation((key: string) => {
        if (key === 'language') return 'pl'
        if (key === 'currency') return 'EUR'
        if (key === 'timezone') return 'Europe/Warsaw'
        return null
      })

      const settingsStore = useSettingsStore()

      expect(settingsStore.language).toBe('pl')
      expect(settingsStore.currency).toBe('EUR')
      expect(settingsStore.timezone).toBe('Europe/Warsaw')
    })

    it('should format currency based on locale', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setCurrency('USD')
      settingsStore.setLanguage('en')

      const formatted = settingsStore.formatCurrency(1234.56)

      expect(formatted).toBe('$1,234.56')
    })

    it('should format date based on settings', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setDateFormat('DD/MM/YYYY')
      const date = new Date('2024-11-05')

      const formatted = settingsStore.formatDate(date)

      expect(formatted).toBe('05/11/2024')
    })

    it('should format time based on settings', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTimeFormat('12h')
      const time = new Date('2024-11-05T14:30:00')

      const formatted = settingsStore.formatTime(time)

      expect(formatted).toBe('2:30 PM')
    })
  })

  describe('Notification Settings', () => {
    it('should update email notification setting', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updateNotification('email', false)

      expect(settingsStore.notifications.email).toBe(false)
      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'notifications',
        expect.stringContaining('"email":false')
      )
    })

    it('should update push notification setting', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updateNotification('push', false)

      expect(settingsStore.notifications.push).toBe(false)
    })

    it('should update SMS notification setting', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updateNotification('sms', true)

      expect(settingsStore.notifications.sms).toBe(true)
    })

    it('should update invoice notification setting', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updateNotification('invoiceUpdates', false)

      expect(settingsStore.notifications.invoiceUpdates).toBe(false)
    })

    it('should update order notification setting', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updateNotification('orderUpdates', false)

      expect(settingsStore.notifications.orderUpdates).toBe(false)
    })

    it('should update marketing email setting', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updateNotification('marketingEmails', true)

      expect(settingsStore.notifications.marketingEmails).toBe(true)
    })

    it('should enable all notifications', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updateNotification('email', false)
      settingsStore.updateNotification('push', false)

      settingsStore.enableAllNotifications()

      expect(settingsStore.notifications.email).toBe(true)
      expect(settingsStore.notifications.push).toBe(true)
      expect(settingsStore.notifications.sms).toBe(false) // SMS defaults to false
    })

    it('should disable all notifications', () => {
      const settingsStore = useSettingsStore()

      settingsStore.disableAllNotifications()

      expect(settingsStore.notifications.email).toBe(false)
      expect(settingsStore.notifications.push).toBe(false)
      expect(settingsStore.notifications.sms).toBe(false)
    })

    it('should initialize notifications from localStorage', () => {
      const notificationsJson = JSON.stringify({
        email: false,
        push: false,
        sms: true,
        invoiceUpdates: false,
        orderUpdates: true,
        marketingEmails: true
      })
      localStorageMock.getItem.mockImplementation((key: string) => {
        if (key === 'notifications') return notificationsJson
        return null
      })

      const settingsStore = useSettingsStore()

      expect(settingsStore.notifications.email).toBe(false)
      expect(settingsStore.notifications.push).toBe(false)
      expect(settingsStore.notifications.sms).toBe(true)
    })
  })

  describe('User Preferences', () => {
    it('should update items per page preference', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updatePreference('itemsPerPage', 50)

      expect(settingsStore.preferences.itemsPerPage).toBe(50)
      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'preferences',
        expect.stringContaining('"itemsPerPage":50')
      )
    })

    it('should toggle auto save preference', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updatePreference('autoSave', false)

      expect(settingsStore.preferences.autoSave).toBe(false)
    })

    it('should toggle confirm actions preference', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updatePreference('confirmActions', false)

      expect(settingsStore.preferences.confirmActions).toBe(false)
    })

    it('should toggle show tutorials preference', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updatePreference('showTutorials', false)

      expect(settingsStore.preferences.showTutorials).toBe(false)
    })

    it('should toggle compact mode preference', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updatePreference('compactMode', true)

      expect(settingsStore.preferences.compactMode).toBe(true)
    })

    it('should reset preferences to defaults', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updatePreference('itemsPerPage', 100)
      settingsStore.updatePreference('compactMode', true)

      settingsStore.resetPreferences()

      expect(settingsStore.preferences.itemsPerPage).toBe(20)
      expect(settingsStore.preferences.compactMode).toBe(false)
    })

    it('should initialize preferences from localStorage', () => {
      const preferencesJson = JSON.stringify({
        itemsPerPage: 50,
        autoSave: false,
        confirmActions: true,
        showTutorials: false,
        compactMode: true
      })
      localStorageMock.getItem.mockImplementation((key: string) => {
        if (key === 'preferences') return preferencesJson
        return null
      })

      const settingsStore = useSettingsStore()

      expect(settingsStore.preferences.itemsPerPage).toBe(50)
      expect(settingsStore.preferences.autoSave).toBe(false)
      expect(settingsStore.preferences.compactMode).toBe(true)
    })
  })

  describe('Server Synchronization', () => {
    it('should load settings from server', async () => {
      const settingsStore = useSettingsStore()
      const { apiClient } = await import('@/services/api')

      const serverSettings = {
        theme: 'dark',
        language: 'pl',
        currency: 'EUR',
        timezone: 'Europe/Warsaw',
        notifications: {
          email: false,
          push: true,
          sms: false,
          invoiceUpdates: true,
          orderUpdates: true,
          marketingEmails: false
        },
        preferences: {
          itemsPerPage: 50,
          autoSave: true,
          confirmActions: true,
          showTutorials: false,
          compactMode: true
        }
      }

      vi.mocked(apiClient.get).mockResolvedValue({ data: serverSettings })

      await settingsStore.loadSettingsFromServer()

      expect(settingsStore.theme).toBe('dark')
      expect(settingsStore.language).toBe('pl')
      expect(settingsStore.notifications.email).toBe(false)
      expect(settingsStore.preferences.itemsPerPage).toBe(50)
    })

    it('should save settings to server', async () => {
      const settingsStore = useSettingsStore()
      const { apiClient } = await import('@/services/api')

      settingsStore.setTheme('dark')
      settingsStore.setLanguage('pl')

      vi.mocked(apiClient.put).mockResolvedValue({ success: true })

      await settingsStore.saveSettingsToServer()

      expect(apiClient.put).toHaveBeenCalledWith('/api/users/settings', {
        theme: 'dark',
        language: 'pl',
        currency: 'USD',
        timezone: 'UTC',
        dateFormat: 'YYYY-MM-DD',
        timeFormat: '24h',
        notifications: settingsStore.notifications,
        preferences: settingsStore.preferences
      })
    })

    it('should handle server error when loading settings', async () => {
      const settingsStore = useSettingsStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.get).mockRejectedValue(new Error('Server error'))

      await settingsStore.loadSettingsFromServer()

      expect(settingsStore.error).toBe('Failed to load settings from server')
    })

    it('should handle server error when saving settings', async () => {
      const settingsStore = useSettingsStore()
      const { apiClient } = await import('@/services/api')

      vi.mocked(apiClient.put).mockRejectedValue(new Error('Server error'))

      await settingsStore.saveSettingsToServer()

      expect(settingsStore.error).toBe('Failed to save settings to server')
    })

    it('should sync settings across tabs', async () => {
      const settingsStore = useSettingsStore()

      // Simulate storage event
      window.dispatchEvent(
        new StorageEvent('storage', {
          key: 'settings',
          newValue: JSON.stringify({ theme: 'dark' })
        })
      )

      // Settings should be updated
      expect(settingsStore.theme).toBe('dark')
    })
  })

  describe('Settings Export/Import', () => {
    it('should export settings', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('dark')
      settingsStore.setLanguage('pl')

      const exported = settingsStore.exportSettings()

      expect(exported).toEqual({
        theme: 'dark',
        language: 'pl',
        currency: 'USD',
        timezone: 'UTC',
        dateFormat: 'YYYY-MM-DD',
        timeFormat: '24h',
        notifications: settingsStore.notifications,
        preferences: settingsStore.preferences
      })
    })

    it('should import settings', () => {
      const settingsStore = useSettingsStore()

      const importedSettings = {
        theme: 'light',
        language: 'de',
        currency: 'EUR',
        timezone: 'Europe/Berlin',
        dateFormat: 'DD.MM.YYYY',
        timeFormat: '24h',
        notifications: {
          email: false,
          push: false,
          sms: false,
          invoiceUpdates: false,
          orderUpdates: false,
          marketingEmails: false
        },
        preferences: {
          itemsPerPage: 100,
          autoSave: false,
          confirmActions: false,
          showTutorials: false,
          compactMode: false
        }
      }

      settingsStore.importSettings(importedSettings)

      expect(settingsStore.theme).toBe('light')
      expect(settingsStore.language).toBe('de')
      expect(settingsStore.currency).toBe('EUR')
      expect(settingsStore.preferences.itemsPerPage).toBe(100)
    })

    it('should validate imported settings', () => {
      const settingsStore = useSettingsStore()

      const invalidSettings = {
        theme: 'invalid-theme',
        language: 'xx',
        currency: 'INVALID'
      }

      settingsStore.importSettings(invalidSettings)

      expect(settingsStore.error).toBe('Invalid settings format')
    })

    it('should export settings to file', async () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('dark')
      settingsStore.setLanguage('pl')

      const blob = settingsStore.exportToFile()

      expect(blob).toBeInstanceOf(Blob)
    })

    it('should import settings from file', async () => {
      const settingsStore = useSettingsStore()

      const settingsFile = new File(
        [JSON.stringify({ theme: 'dark', language: 'pl' })],
        'settings.json',
        { type: 'application/json' }
      )

      await settingsStore.importFromFile(settingsFile)

      expect(settingsStore.theme).toBe('dark')
      expect(settingsStore.language).toBe('pl')
    })
  })

  describe('Computed Properties', () => {
    it('should return isDarkTheme', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('dark')
      expect(settingsStore.isDarkTheme).toBe(true)

      settingsStore.setTheme('light')
      expect(settingsStore.isDarkTheme).toBe(false)
    })

    it('should return isLightTheme', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('light')
      expect(settingsStore.isLightTheme).toBe(true)

      settingsStore.setTheme('dark')
      expect(settingsStore.isLightTheme).toBe(false)
    })

    it('should return isSystemTheme', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('system')
      expect(settingsStore.isSystemTheme).toBe(true)

      settingsStore.setTheme('dark')
      expect(settingsStore.isSystemTheme).toBe(false)
    })

    it('should return current locale', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setLanguage('pl')
      settingsStore.setCurrency('EUR')

      expect(settingsStore.locale).toBe('pl-PL')
    })

    it('should return formatted timezone', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTimezone('Europe/Warsaw')

      expect(settingsStore.formattedTimezone).toBe('Europe/Warsaw (GMT+1)')
    })

    it('should return isEmailNotificationsEnabled', () => {
      const settingsStore = useSettingsStore()

      expect(settingsStore.isEmailNotificationsEnabled).toBe(true)

      settingsStore.updateNotification('email', false)
      expect(settingsStore.isEmailNotificationsEnabled).toBe(false)
    })

    it('should return isPushNotificationsEnabled', () => {
      const settingsStore = useSettingsStore()

      expect(settingsStore.isPushNotificationsEnabled).toBe(true)

      settingsStore.updateNotification('push', false)
      expect(settingsStore.isPushNotificationsEnabled).toBe(false)
    })

    it('should return hasUnreadNotifications setting', () => {
      const settingsStore = useSettingsStore()

      expect(settingsStore.hasUnreadNotifications).toBe(false)

      settingsStore.setUnreadNotifications(true)
      expect(settingsStore.hasUnreadNotifications).toBe(true)
    })
  })

  describe('Reset and Defaults', () => {
    it('should reset all settings to defaults', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('dark')
      settingsStore.setLanguage('pl')
      settingsStore.updatePreference('itemsPerPage', 100)

      settingsStore.resetAllSettings()

      expect(settingsStore.theme).toBe('system')
      expect(settingsStore.language).toBe('en')
      expect(settingsStore.preferences.itemsPerPage).toBe(20)
    })

    it('should reset theme to default', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('dark')
      settingsStore.resetTheme()

      expect(settingsStore.theme).toBe('system')
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('theme')
    })

    it('should reset notifications to defaults', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updateNotification('email', false)
      settingsStore.resetNotifications()

      expect(settingsStore.notifications.email).toBe(true)
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('notifications')
    })

    it('should reset preferences to defaults', () => {
      const settingsStore = useSettingsStore()

      settingsStore.updatePreference('itemsPerPage', 100)
      settingsStore.resetPreferences()

      expect(settingsStore.preferences.itemsPerPage).toBe(20)
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('preferences')
    })

    it('should clear all localStorage entries', () => {
      const settingsStore = useSettingsStore()

      settingsStore.clearAllData()

      expect(localStorageMock.clear).toHaveBeenCalled()
    })
  })

  describe('Error Handling', () => {
    it('should set error state', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setError('Test error')

      expect(settingsStore.error).toBe('Test error')
    })

    it('should clear error', () => {
      const settingsStore = useSettingsStore()

      settingsStore.error = 'Some error'
      settingsStore.clearError()

      expect(settingsStore.error).toBeNull()
    })

    it('should handle localStorage errors gracefully', () => {
      localStorageMock.getItem.mockImplementation(() => {
        throw new Error('localStorage not available')
      })

      const settingsStore = useSettingsStore()

      // Should fall back to defaults
      expect(settingsStore.theme).toBe('system')
      expect(settingsStore.language).toBe('en')
    })
  })

  describe('Settings Validation', () => {
    it('should validate theme setting', () => {
      const settingsStore = useSettingsStore()

      const validThemes = ['light', 'dark', 'system']
      validThemes.forEach(theme => {
        expect(() => settingsStore.setTheme(theme)).not.toThrow()
      })

      expect(() => settingsStore.setTheme('invalid-theme')).toThrow('Invalid theme')
    })

    it('should validate language code', () => {
      const settingsStore = useSettingsStore()

      expect(() => settingsStore.setLanguage('en')).not.toThrow()
      expect(() => settingsStore.setLanguage('pl')).not.toThrow()

      expect(() => settingsStore.setLanguage('invalid')).toThrow('Invalid language code')
    })

    it('should validate currency code', () => {
      const settingsStore = useSettingsStore()

      expect(() => settingsStore.setCurrency('USD')).not.toThrow()
      expect(() => settingsStore.setCurrency('EUR')).not.toThrow()

      expect(() => settingsStore.setCurrency('INVALID')).toThrow('Invalid currency code')
    })

    it('should validate items per page', () => {
      const settingsStore = useSettingsStore()

      expect(() => settingsStore.updatePreference('itemsPerPage', 10)).not.toThrow()
      expect(() => settingsStore.updatePreference('itemsPerPage', 100)).not.toThrow()

      expect(() => settingsStore.updatePreference('itemsPerPage', 5)).toThrow('Items per page must be between 10 and 100')
      expect(() => settingsStore.updatePreference('itemsPerPage', 200)).toThrow('Items per page must be between 10 and 100')
    })
  })

  describe('Auto-save', () => {
    it('should auto-save settings when enabled', async () => {
      const settingsStore = useSettingsStore()
      const { apiClient } = await import('@/services/api')

      settingsStore.updatePreference('autoSave', true)
      vi.mocked(apiClient.put).mockResolvedValue({ success: true })

      settingsStore.setTheme('dark')

      await new Promise(resolve => setTimeout(resolve, 100))

      expect(apiClient.put).toHaveBeenCalled()
    })

    it('should not auto-save when disabled', async () => {
      const settingsStore = useSettingsStore()
      const { apiClient } = await import('@/services/api')

      settingsStore.updatePreference('autoSave', false)

      settingsStore.setTheme('dark')

      await new Promise(resolve => setTimeout(resolve, 100))

      expect(apiClient.put).not.toHaveBeenCalled()
    })

    it('should debounce auto-save', async () => {
      const settingsStore = useSettingsStore()
      const { apiClient } = await import('@/services/api')

      settingsStore.updatePreference('autoSave', true)
      vi.mocked(apiClient.put).mockResolvedValue({ success: true })

      settingsStore.setTheme('dark')
      settingsStore.setLanguage('pl')
      settingsStore.setCurrency('EUR')

      await new Promise(resolve => setTimeout(resolve, 100))

      // Should only save once, not three times
      expect(apiClient.put).toHaveBeenCalledTimes(1)
    })
  })

  describe('Settings History', () => {
    it('should track setting changes', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('dark')
      settingsStore.setLanguage('pl')

      const history = settingsStore.getSettingsHistory()

      expect(history).toHaveLength(2)
      expect(history[0]).toEqual(expect.objectContaining({ setting: 'theme', value: 'dark' }))
      expect(history[1]).toEqual(expect.objectContaining({ setting: 'language', value: 'pl' }))
    })

    it('should limit history length', () => {
      const settingsStore = useSettingsStore()

      // Change many settings
      for (let i = 0; i < 15; i++) {
        settingsStore.setTheme(i % 2 === 0 ? 'light' : 'dark')
      }

      const history = settingsStore.getSettingsHistory()

      // Should keep only last 10 changes
      expect(history).toHaveLength(10)
    })

    it('should clear history', () => {
      const settingsStore = useSettingsStore()

      settingsStore.setTheme('dark')
      settingsStore.clearHistory()

      const history = settingsStore.getSettingsHistory()

      expect(history).toHaveLength(0)
    })
  })
})
