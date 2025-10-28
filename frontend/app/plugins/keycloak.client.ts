import Keycloak, { type KeycloakProfile } from 'keycloak-js'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated' | 'error'
type AuthEventType = 'init' | 'login' | 'refresh' | 'logout' | 'error'
type AuthEventContext = 'bootstrap' | 'session' | 'token' | 'profile'

type AuthEvent = {
  type: AuthEventType
  at: string
  context?: AuthEventContext
  message?: string | null
}

export default defineNuxtPlugin(async (nuxtApp) => {
  if (import.meta.server) {
    nuxtApp.provide('keycloak', null)
    return
  }

  const config = useRuntimeConfig()
  const keycloak = new Keycloak({
    url: config.public.keycloakUrl,
    realm: config.public.keycloakRealm,
    clientId: config.public.keycloakClientId
  })

  const status = useState<AuthStatus>('auth-status', () => 'loading')
  const token = useState<string | null>('auth-token', () => null)
  const profile = useState<KeycloakProfile | null>('auth-profile', () => null)
  const ready = useState<boolean>('auth-ready', () => false)
  const lastError = useState<string | null>('auth-error', () => null)
  const lastEvent = useState<AuthEvent | null>('auth-event', () => null)

  const recordEvent = (type: AuthEventType, context?: AuthEventContext, message?: string | null) => {
    lastEvent.value = {
      type,
      context,
      message: message ?? null,
      at: new Date().toISOString()
    }
  }

  const syncState = async (loadProfile: boolean) => {
    token.value = keycloak.token ?? null

    if (keycloak.authenticated) {
      status.value = 'authenticated'
      if (loadProfile) {
        try {
          profile.value = await keycloak.loadUserProfile()
        } catch (error) {
          const message = error instanceof Error ? error.message : 'Unable to load profile'
          lastError.value = message
          recordEvent('error', 'profile', message)
        }
      }
    } else {
      status.value = 'unauthenticated'
      profile.value = null
    }
  }

  const refreshToken = async (minValidity = 30) => {
    if (!keycloak.authenticated) {
      return false
    }

    try {
      const refreshed = await keycloak.updateToken(minValidity)
      if (refreshed) {
        token.value = keycloak.token ?? null
        recordEvent('refresh', 'token')
      }
      return true
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Token refresh failed'
      lastError.value = message
      recordEvent('error', 'token', message)
      await keycloak.login()
      return false
    }
  }

  try {
    await keycloak.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      enableLogging: false
    })
    await syncState(true)
    recordEvent('init', 'bootstrap')
  } catch (error) {
    status.value = 'error'
    const message = error instanceof Error ? error.message : 'Authentication failed'
    lastError.value = message
    recordEvent('error', 'session', message)
  } finally {
    ready.value = true
  }

  keycloak.onAuthSuccess = async () => {
    lastError.value = null
    recordEvent('login', 'session')
    await syncState(true)
  }

  keycloak.onAuthRefreshSuccess = async () => {
    lastError.value = null
    recordEvent('refresh', 'token')
    await syncState(false)
  }

  keycloak.onAuthLogout = () => {
    token.value = null
    profile.value = null
    status.value = 'unauthenticated'
    recordEvent('logout', 'session')
  }

  keycloak.onAuthError = (errorData) => {
    const message = typeof errorData === 'object' && errorData !== null && 'error' in errorData
      ? String(errorData.error)
      : 'Authentication error'
    status.value = 'error'
    lastError.value = message
    recordEvent('error', 'session', message)
  }

  keycloak.onAuthRefreshError = (errorData) => {
    const message = typeof errorData === 'object' && errorData !== null && 'error' in errorData
      ? String(errorData.error)
      : 'Token refresh error'
    status.value = 'error'
    lastError.value = message
    recordEvent('error', 'token', message)
    keycloak.login().catch(() => undefined)
  }

  keycloak.onTokenExpired = () => {
    recordEvent('refresh', 'token', 'token-expired')
    refreshToken(30).catch(() => undefined)
  }

  const refreshInterval = window.setInterval(() => {
    refreshToken(60).catch(() => undefined)
  }, 20000)

  window.addEventListener('beforeunload', () => {
    window.clearInterval(refreshInterval)
  })

  nuxtApp.provide('keycloak', keycloak)
})
