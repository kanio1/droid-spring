import { watch, computed } from 'vue'
import type Keycloak from 'keycloak-js'
import type { KeycloakProfile } from 'keycloak-js'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated' | 'error'

type AuthEvent = {
  type: 'init' | 'login' | 'refresh' | 'logout' | 'error'
  at: string
  context?: 'bootstrap' | 'session' | 'token' | 'profile'
  message?: string | null
}

export const useAuth = () => {
  const nuxtApp = useNuxtApp()
  const keycloak = computed<Keycloak | null>(() => nuxtApp.$keycloak)

  const status = useState<AuthStatus>('auth-status', () => 'loading')
  const token = useState<string | null>('auth-token', () => null)
  const profile = useState<KeycloakProfile | null>('auth-profile', () => null)
  const ready = useState<boolean>('auth-ready', () => false)
  const lastError = useState<string | null>('auth-error', () => null)
  const lastEvent = useState<AuthEvent | null>('auth-event', () => null)

  const isAuthenticated = computed(() => status.value === 'authenticated')
  const isReady = computed(() => ready.value)
  const authHeader = computed(() => (token.value ? `Bearer ${token.value}` : undefined))

  const ensureReady = async () => {
    if (ready.value || import.meta.server) {
      return
    }

    await new Promise<void>((resolve) => {
      const stop = watch(ready, (value) => {
        if (value) {
          stop()
          resolve()
        }
      })
    })
  }

  const refreshToken = async (minValidity = 30) => {
    if (!process.client) {
      return false
    }

    const client = keycloak.value
    if (!client || !client.authenticated) {
      return false
    }

    try {
      const refreshed = await client.updateToken(minValidity)
      if (refreshed) {
        token.value = client.token ?? null
      }
      return true
    } catch (error) {
      lastError.value = error instanceof Error ? error.message : 'Token refresh failed'
      await client.login()
      return false
    }
  }

  const login = async () => {
    if (!process.client) {
      return
    }

    const client = keycloak.value
    if (client) {
      await client.login()
    }
  }

  const logout = async () => {
    if (!process.client) {
      return
    }

    const client = keycloak.value
    if (client) {
      await client.logout({ redirectUri: window.location.origin })
    }
  }

  const reloadProfile = async () => {
    if (!process.client) {
      return null
    }

    const client = keycloak.value
    if (!client || !client.authenticated) {
      profile.value = null
      return null
    }

    try {
      const data = await client.loadUserProfile()
      profile.value = data
      return data
    } catch (error) {
      lastError.value = error instanceof Error ? error.message : 'Unable to load profile'
      return null
    }
  }

  return {
    keycloak,
    status,
    token,
    profile,
    ready,
    isReady,
    lastError,
    lastEvent,
    isAuthenticated,
    authHeader,
    ensureReady,
    refreshToken,
    login,
    logout,
    reloadProfile
  }
}
