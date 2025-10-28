import { computed } from 'vue'

type ChecklistStatus = 'pending' | 'success' | 'error'

type ChecklistItem = {
  id: string
  label: string
  description: string
  status: ChecklistStatus
}

export const useLoginDiagnostics = () => {
  const auth = useAuth()

  const steps = computed<ChecklistItem[]>(() => {
    const ready = auth.ready.value
    const status = auth.status.value
    const token = auth.token.value
    const profile = auth.profile.value
    const hasError = status === 'error'

    const toStatus = (condition: boolean): ChecklistStatus => {
      if (condition) {
        return 'success'
      }
      if (hasError) {
        return 'error'
      }
      return 'pending'
    }

    return [
      {
        id: 'bootstrap',
        label: 'Application session initialized',
        description: 'Keycloak SDK configured on client',
        status: toStatus(ready)
      },
      {
        id: 'authentication',
        label: 'User authenticated in Keycloak',
        description: 'Keycloak login completed and session established',
        status: toStatus(status === 'authenticated')
      },
      {
        id: 'token',
        label: 'Access token available',
        description: 'Token ready for backend API calls',
        status: toStatus(Boolean(token))
      },
      {
        id: 'profile',
        label: 'User profile loaded',
        description: 'Profile fetched to power UI personalization',
        status: toStatus(Boolean(profile))
      }
    ]
  })

  const hasBlockingError = computed(() => auth.status.value === 'error')
  const canRetryLogin = computed(() => auth.status.value === 'unauthenticated' || auth.status.value === 'error')
  const lastEvent = computed(() => auth.lastEvent.value)

  const primaryActionLabel = computed(() => {
    if (canRetryLogin.value) {
      return 'Sign in again'
    }
    return 'Reload profile'
  })

  const runPrimaryAction = async () => {
    if (canRetryLogin.value) {
      await auth.login()
      return
    }

    await auth.reloadProfile()
  }

  return {
    steps,
    hasBlockingError,
    canRetryLogin,
    lastEvent,
    primaryActionLabel,
    runPrimaryAction
  }
}
