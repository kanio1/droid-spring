import { abortNavigation } from '#app'

export default defineNuxtRouteMiddleware(async () => {
  if (import.meta.server) {
    return
  }

  const auth = useAuth()
  await auth.ensureReady()

  if (!auth.isAuthenticated.value) {
    if (auth.status.value === 'error') {
      return
    }
    await auth.login()
    return abortNavigation()
  }
})
