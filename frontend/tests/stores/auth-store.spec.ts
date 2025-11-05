/**
 * AuthStore Tests
 *
 * Comprehensive test coverage for authentication store
 * Tests login, logout, token refresh, session management, and user state
 *
 * Priority: 🔴 HIGH
 * Learning Value: ⭐⭐⭐⭐⭐
 */

import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { ref } from 'vue'

// Mock Keycloak
vi.mock('@/plugins/keycloak', () => ({
  default: {
    init: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    accountManagement: vi.fn(),
    createLoginUrl: vi.fn().mockReturnValue('http://localhost/login'),
    createLogoutUrl: vi.fn().mockReturnValue('http://localhost/logout'),
    createAccountUrl: vi.fn().mockReturnValue('http://localhost/account'),
    token: ref('mock-token'),
    tokenParsed: ref({
      sub: 'user-123',
      preferred_username: 'testuser',
      email: 'test@example.com',
      given_name: 'Test',
      family_name: 'User',
      realm_access: { roles: ['user', 'admin'] },
      resource_access: {
        'bss-frontend': { roles: ['admin'] }
      }
    }),
    authenticated: ref(true),
    hasRealmRole: vi.fn().mockImplementation((role: string) => {
      const roles = ['user', 'admin']
      return Promise.resolve(roles.includes(role))
    }),
    hasResourceRole: vi.fn().mockImplementation((role: string, resource: string) => {
      if (resource === 'bss-frontend') {
        const roles = ['admin']
        return Promise.resolve(roles.includes(role))
      }
      return Promise.resolve(false)
    }),
    loadUserProfile: vi.fn().mockResolvedValue({
      id: 'user-123',
      username: 'testuser',
      email: 'test@example.com',
      firstName: 'Test',
      lastName: 'User'
    }),
    updateToken: vi.fn().mockResolvedValue(true),
    clearToken: vi.fn()
  }
}))

// Mock API
vi.mock('@/services/api', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}))

describe('AuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('Initial State', () => {
    it('should have correct initial state', () => {
      const authStore = useAuthStore()

      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.user).toBeNull()
      expect(authStore.token).toBe('')
      expect(authStore.roles).toEqual([])
      expect(authStore.permissions).toEqual([])
      expect(authStore.isLoading).toBe(false)
      expect(authStore.error).toBe(null)
    })
  })

  describe('initialize', () => {
    it('should initialize authentication successfully', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      // Mock Keycloak as authenticated
      keycloakPlugin.default.authenticated.value = true
      keycloakPlugin.default.token.value = 'test-token-123'
      keycloakPlugin.default.tokenParsed.value = {
        sub: 'user-123',
        preferred_username: 'testuser',
        email: 'test@example.com',
        given_name: 'Test',
        family_name: 'User',
        realm_access: { roles: ['user', 'admin'] }
      }

      await authStore.initialize()

      expect(authStore.isAuthenticated).toBe(true)
      expect(authStore.token).toBe('test-token-123')
      expect(authStore.user).toEqual({
        id: 'user-123',
        username: 'testuser',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User'
      })
      expect(authStore.roles).toEqual(['user', 'admin'])
      expect(authStore.error).toBeNull()
    })

    it('should handle unauthenticated initialization', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      // Mock Keycloak as not authenticated
      keycloakPlugin.default.authenticated.value = false
      keycloakPlugin.default.token.value = ''

      await authStore.initialize()

      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.user).toBeNull()
      expect(authStore.token).toBe('')
      expect(authStore.roles).toEqual([])
    })

    it('should handle initialization error', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      // Mock Keycloak initialization failure
      vi.mocked(keycloakPlugin.default.init).mockRejectedValue(new Error('Init failed'))

      await authStore.initialize()

      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.error).toBe('Failed to initialize authentication')
    })
  })

  describe('login', () => {
    it('should initiate login process', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      await authStore.login()

      expect(keycloakPlugin.default.login).toHaveBeenCalled()
    })

    it('should redirect to login page', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      const loginUrl = await authStore.login()

      expect(loginUrl).toBe('http://localhost/login')
      expect(keycloakPlugin.default.login).toHaveBeenCalled()
    })

    it('should handle login with redirect', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      const redirectUri = '/dashboard'
      await authStore.login(redirectUri)

      expect(keycloakPlugin.default.login).toHaveBeenCalledWith({ redirectUri })
    })
  })

  describe('logout', () => {
    it('should logout successfully', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      // Set authenticated state
      authStore.isAuthenticated = true
      authStore.user = { id: '123', username: 'test' }
      authStore.token = 'test-token'

      await authStore.logout()

      expect(keycloakPlugin.default.logout).toHaveBeenCalled()
      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.user).toBeNull()
      expect(authStore.token).toBe('')
      expect(authStore.roles).toEqual([])
    })

    it('should logout with redirect', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      await authStore.logout('/login')

      expect(keycloakPlugin.default.logout).toHaveBeenCalledWith({ redirectUri: '/login' })
    })
  })

  describe('refreshToken', () => {
    it('should refresh token successfully', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      authStore.token = 'old-token'
      authStore.isAuthenticated = true

      vi.mocked(keycloakPlugin.default.updateToken).mockResolvedValue(true)
      keycloakPlugin.default.token.value = 'new-token-123'

      const refreshed = await authStore.refreshToken()

      expect(refreshed).toBe(true)
      expect(keycloakPlugin.default.updateToken).toHaveBeenCalled()
      expect(authStore.token).toBe('new-token-123')
    })

    it('should handle refresh failure', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      authStore.token = 'old-token'
      authStore.isAuthenticated = true

      vi.mocked(keycloakPlugin.default.updateToken).mockRejectedValue(new Error('Refresh failed'))

      const refreshed = await authStore.refreshToken()

      expect(refreshed).toBe(false)
      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.user).toBeNull()
    })

    it('should not refresh when already logged out', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      authStore.isAuthenticated = false

      const refreshed = await authStore.refreshToken()

      expect(refreshed).toBe(false)
      expect(keycloakPlugin.default.updateToken).not.toHaveBeenCalled()
    })
  })

  describe('hasRole', () => {
    it('should check if user has specific role', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      authStore.roles = ['user', 'admin']
      vi.mocked(keycloakPlugin.default.hasRealmRole).mockImplementation((role) => {
        const roles = ['user', 'admin']
        return Promise.resolve(roles.includes(role))
      })

      const hasAdminRole = await authStore.hasRole('admin')
      const hasUserRole = await authStore.hasRole('user')
      const hasGuestRole = await authStore.hasRole('guest')

      expect(hasAdminRole).toBe(true)
      expect(hasUserRole).toBe(true)
      expect(hasGuestRole).toBe(false)
    })
  })

  describe('hasPermission', () => {
    it('should check if user has specific permission', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      authStore.permissions = ['read:customers', 'write:orders']
      vi.mocked(keycloakPlugin.default.hasResourceRole).mockImplementation((role, resource) => {
        if (resource === 'bss-frontend') {
          const permissions = ['read:customers', 'write:orders']
          return Promise.resolve(permissions.includes(role))
        }
        return Promise.resolve(false)
      })

      const hasPermission = await authStore.hasPermission('read:customers')
      const lacksPermission = await authStore.hasPermission('delete:invoices')

      expect(hasPermission).toBe(true)
      expect(lacksPermission).toBe(false)
    })
  })

  describe('loadUserProfile', () => {
    it('should load user profile successfully', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      const mockProfile = {
        id: 'user-123',
        username: 'testuser',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User'
      }
      vi.mocked(keycloakPlugin.default.loadUserProfile).mockResolvedValue(mockProfile)

      const profile = await authStore.loadUserProfile()

      expect(profile).toEqual(mockProfile)
      expect(authStore.user).toEqual(mockProfile)
    })

    it('should handle profile load error', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      vi.mocked(keycloakPlugin.default.loadUserProfile).mockRejectedValue(new Error('Load failed'))

      await expect(authStore.loadUserProfile()).rejects.toThrow('Load failed')
    })
  })

  describe('checkPermission', () => {
    it('should check permission and return boolean', async () => {
      const authStore = useAuthStore()
      vi.spyOn(authStore, 'hasPermission').mockResolvedValue(true)

      const result = await authStore.checkPermission('read:customers')

      expect(result).toBe(true)
    })

    it('should throw error for missing permission', async () => {
      const authStore = useAuthStore()
      vi.spyOn(authStore, 'hasPermission').mockResolvedValue(false)

      await expect(authStore.checkPermission('admin:all'))
        .rejects.toThrow('Insufficient permissions: admin:all')
    })
  })

  describe('hasAnyRole', () => {
    it('should check if user has any of the specified roles', () => {
      const authStore = useAuthStore()
      authStore.roles = ['user', 'viewer']

      expect(authStore.hasAnyRole(['admin', 'user'])).toBe(true)
      expect(authStore.hasAnyRole(['admin', 'guest'])).toBe(false)
    })
  })

  describe('hasAllRoles', () => {
    it('should check if user has all specified roles', () => {
      const authStore = useAuthStore()
      authStore.roles = ['user', 'admin', 'viewer']

      expect(authStore.hasAllRoles(['user', 'admin'])).toBe(true)
      expect(authStore.hasAllRoles(['user', 'admin', 'guest'])).toBe(false)
    })
  })

  describe('getAccessToken', () => {
    it('should return current access token', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      authStore.token = 'test-token-123'

      const token = authStore.getAccessToken()

      expect(token).toBe('test-token-123')
    })

    it('should refresh and return token when expired', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      vi.mocked(keycloakPlugin.default.updateToken).mockResolvedValue(true)
      keycloakPlugin.default.token.value = 'refreshed-token'

      authStore.isAuthenticated = true

      const token = await authStore.getAccessToken(true)

      expect(token).toBe('refreshed-token')
      expect(keycloakPlugin.default.updateToken).toHaveBeenCalled()
    })
  })

  describe('handleCallback', () => {
    it('should handle login callback successfully', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      // Mock authenticated state after callback
      keycloakPlugin.default.authenticated.value = true
      keycloakPlugin.default.token.value = 'callback-token'
      keycloakPlugin.default.tokenParsed.value = {
        sub: 'user-123',
        preferred_username: 'testuser',
        email: 'test@example.com'
      }

      await authStore.handleCallback()

      expect(authStore.isAuthenticated).toBe(true)
      expect(authStore.token).toBe('callback-token')
      expect(authStore.user).not.toBeNull()
    })

    it('should handle callback error', async () => {
      const authStore = useAuthStore()

      await authStore.handleCallback()

      expect(authStore.error).toBe('Authentication callback failed')
    })
  })

  describe('autoRefresh', () => {
    it('should setup auto token refresh', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      authStore.isAuthenticated = true
      authStore.token = 'test-token'
      vi.mocked(keycloakPlugin.default.updateToken).mockResolvedValue(true)

      authStore.autoRefresh(60) // Refresh every 60 seconds

      // Wait for refresh interval
      await new Promise(resolve => setTimeout(resolve, 100))

      expect(keycloakPlugin.default.updateToken).toHaveBeenCalled()
    })

    it('should not refresh when logged out', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      authStore.isAuthenticated = false

      authStore.autoRefresh(60)

      await new Promise(resolve => setTimeout(resolve, 100))

      expect(keycloakPlugin.default.updateToken).not.toHaveBeenCalled()
    })
  })

  describe('reset', () => {
    it('should reset store to initial state', () => {
      const authStore = useAuthStore()

      // Set some state
      authStore.isAuthenticated = true
      authStore.user = { id: '123', username: 'test' }
      authStore.token = 'test-token'
      authStore.roles = ['admin']
      authStore.error = 'Some error'

      authStore.reset()

      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.user).toBeNull()
      expect(authStore.token).toBe('')
      expect(authStore.roles).toEqual([])
      expect(authStore.error).toBeNull()
    })
  })

  describe('Session Management', () => {
    it('should check if session is valid', () => {
      const authStore = useAuthStore()
      authStore.isAuthenticated = true
      authStore.token = 'valid-token'

      expect(authStore.isSessionValid).toBe(true)
    })

    it('should detect invalid session', () => {
      const authStore = useAuthStore()
      authStore.isAuthenticated = false
      authStore.token = ''

      expect(authStore.isSessionValid).toBe(false)
    })

    it('should get time until expiration', () => {
      const authStore = useAuthStore()
      const mockParsedToken = {
        exp: Math.floor(Date.now() / 1000) + 3600 // 1 hour from now
      }

      // This would depend on actual implementation
      const timeUntilExpiration = authStore.getTimeUntilExpiration(mockParsedToken as any)

      expect(timeUntilExpiration).toBeGreaterThan(0)
      expect(timeUntilExpiration).toBeLessThanOrEqual(3600)
    })
  })

  describe('Error Handling', () => {
    it('should set error state', () => {
      const authStore = useAuthStore()
      const error = new Error('Authentication failed')

      authStore.setError(error)

      expect(authStore.error).toBe('Authentication failed')
    })

    it('should clear error', () => {
      const authStore = useAuthStore()
      authStore.error = 'Some error'

      authStore.clearError()

      expect(authStore.error).toBeNull()
    })

    it('should handle network errors during login', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      vi.mocked(keycloakPlugin.default.login).mockRejectedValue(new Error('Network error'))

      await authStore.login()

      expect(authStore.error).toContain('Network error')
    })

    it('should handle token refresh errors', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      authStore.isAuthenticated = true
      vi.mocked(keycloakPlugin.default.updateToken).mockRejectedValue(new Error('Token refresh failed'))

      const refreshed = await authStore.refreshToken()

      expect(refreshed).toBe(false)
      expect(authStore.error).toContain('Token refresh failed')
      expect(authStore.isAuthenticated).toBe(false)
    })
  })

  describe('Computed Properties', () => {
    it('should correctly identify admin user', () => {
      const authStore = useAuthStore()
      authStore.roles = ['user', 'admin']

      expect(authStore.isAdmin).toBe(true)
    })

    it('should return false for non-admin user', () => {
      const authStore = useAuthStore()
      authStore.roles = ['user']

      expect(authStore.isAdmin).toBe(false)
    })

    it('should return user full name', () => {
      const authStore = useAuthStore()
      authStore.user = {
        id: '123',
        username: 'testuser',
        email: 'test@example.com',
        firstName: 'Test',
        lastName: 'User'
      }

      expect(authStore.fullName).toBe('Test User')
    })

    it('should return username when full name not available', () => {
      const authStore = useAuthStore()
      authStore.user = {
        id: '123',
        username: 'testuser',
        email: 'test@example.com'
      }

      expect(authStore.fullName).toBe('testuser')
    })
  })

  describe('Integration Tests', () => {
    it('should handle complete login flow', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      // Start unauthenticated
      expect(authStore.isAuthenticated).toBe(false)

      // Mock successful login
      keycloakPlugin.default.authenticated.value = true
      keycloakPlugin.default.token.value = 'login-token'
      keycloakPlugin.default.tokenParsed.value = {
        sub: 'user-123',
        preferred_username: 'testuser',
        email: 'test@example.com',
        given_name: 'Test',
        family_name: 'User',
        realm_access: { roles: ['user'] }
      }

      // Initialize after login
      await authStore.initialize()

      expect(authStore.isAuthenticated).toBe(true)
      expect(authStore.user?.username).toBe('testuser')
      expect(authStore.roles).toContain('user')

      // Logout
      await authStore.logout()

      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.user).toBeNull()
    })

    it('should handle token expiration and refresh', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      // Login
      authStore.isAuthenticated = true
      authStore.token = 'original-token'

      // Mock refresh
      vi.mocked(keycloakPlugin.default.updateToken).mockResolvedValue(true)
      keycloakPlugin.default.token.value = 'refreshed-token'

      // Refresh token
      const refreshed = await authStore.refreshToken()

      expect(refreshed).toBe(true)
      expect(authStore.token).toBe('refreshed-token')
    })

    it('should handle refresh failure and logout', async () => {
      const authStore = useAuthStore()
      const keycloakPlugin = await import('@/plugins/keycloak')

      // Login
      authStore.isAuthenticated = true
      authStore.user = { id: '123', username: 'test' }
      authStore.token = 'original-token'

      // Mock refresh failure
      vi.mocked(keycloakPlugin.default.updateToken).mockRejectedValue(new Error('Refresh failed'))

      const refreshed = await authStore.refreshToken()

      expect(refreshed).toBe(false)
      expect(authStore.isAuthenticated).toBe(false)
      expect(authStore.user).toBeNull()
      expect(authStore.token).toBe('')
    })
  })
})
