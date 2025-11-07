import '@testing-library/jest-dom'
import { vi, beforeEach, afterEach } from 'vitest'

// =============================================================================
// GLOBAL TYPE DECLARATIONS
// =============================================================================

declare global {
  function useApi(): {
    get: ReturnType<typeof vi.fn>
    post: ReturnType<typeof vi.fn>
    put: ReturnType<typeof vi.fn>
    del: ReturnType<typeof vi.fn>
    patch: ReturnType<typeof vi.fn>
    create: ReturnType<typeof vi.fn>
    read: ReturnType<typeof vi.fn>
    update: ReturnType<typeof vi.fn>
    remove: ReturnType<typeof vi.fn>
    paginatedGet: ReturnType<typeof vi.fn>
    request: ReturnType<typeof vi.fn>
    loading: ReturnType<typeof vi.fn>
    handleSuccess: ReturnType<typeof vi.fn>
    buildUrl: ReturnType<typeof vi.fn>
    baseURL: string
  }
}

// =============================================================================
// COMPOSABLES MOCKS
// =============================================================================

// Mock useApi
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    del: vi.fn(),
    patch: vi.fn(),
    create: vi.fn(),
    read: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
    paginatedGet: vi.fn(),
    request: vi.fn(),
    loading: vi.fn(() => false),
    handleSuccess: vi.fn(),
    buildUrl: vi.fn((endpoint: string) => `http://localhost:8080/api${endpoint}`),
    baseURL: 'http://localhost:8080/api'
  })
}))

// Mock useAuth
vi.mock('~/composables/useAuth', () => ({
  useAuth: () => ({
    token: vi.fn(() => null),
    isAuthenticated: vi.fn(() => false),
    isReady: vi.fn(() => false),
    status: vi.fn(() => 'unauthenticated'),
    profile: vi.fn(() => null),
    ready: vi.fn(() => false),
    ensureReady: vi.fn(),
    refreshToken: vi.fn(() => Promise.resolve(true)),
    login: vi.fn(() => Promise.resolve()),
    logout: vi.fn(() => Promise.resolve()),
    reloadProfile: vi.fn(() => Promise.resolve(null)),
    authHeader: vi.fn(() => undefined),
    lastError: vi.fn(() => null)
  })
}))

// Mock useToast
vi.mock('~/composables/useToast', () => ({
  useToast: () => ({
    showToast: vi.fn()
  })
}))

// =============================================================================
// NUXT MOCKS
// =============================================================================

// Mock #app (Nuxt composables)
vi.mock('#app', async () => {
  const actual = await vi.importActual('#app')
  return {
    ...actual,
    useRuntimeConfig: () => ({
      public: {
        apiBaseUrl: 'http://localhost:8080/api/v1'
      }
    }),
    useNuxtApp: () => ({
      $keycloak: null
    })
  }
})

// Mock useRouter
vi.mock('vue-router', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
    go: vi.fn(),
    back: vi.fn(),
    forward: vi.fn()
  })
}))
