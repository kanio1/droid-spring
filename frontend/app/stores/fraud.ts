import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  FraudAlert,
  CreateFraudAlertCommand,
  AssignAlertCommand,
  ResolveAlertCommand,
  FalsePositiveCommand,
  EscalateAlertCommand,
  VelocityCheckCommand,
  HighValueCheckCommand,
  FraudSearchParams,
  FraudStatistics,
  FraudAlertStatus,
  FraudAlertType
} from '~/schemas/fraud'

export const useFraudStore = defineStore('fraud', () => {
  // State
  const alerts = ref<FraudAlert[]>([])
  const currentAlert = ref<FraudAlert | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const alertCount = computed(() => alerts.value.length)
  const newAlerts = computed(() => alerts.value.filter(a => a.status === 'NEW'))
  const assignedAlerts = computed(() => alerts.value.filter(a => a.status === 'ASSIGNED'))
  const inReviewAlerts = computed(() => alerts.value.filter(a => a.status === 'IN_REVIEW'))
  const escalatedAlerts = computed(() => alerts.value.filter(a => a.status === 'ESCALATED'))
  const resolvedAlerts = computed(() => alerts.value.filter(a => a.status === 'RESOLVED'))
  const closedAlerts = computed(() => alerts.value.filter(a => a.status === 'CLOSED'))
  const rejectedAlerts = computed(() => alerts.value.filter(a => a.status === 'REJECTED'))

  const openAlerts = computed(() =>
    alerts.value.filter(a => a.status === 'NEW' || a.status === 'ASSIGNED' || a.status === 'IN_REVIEW' || a.status === 'ESCALATED')
  )

  const highRiskAlerts = computed(() =>
    alerts.value.filter(a => a.severity === 'HIGH' || a.severity === 'CRITICAL')
  )

  const criticalAlerts = computed(() =>
    alerts.value.filter(a => a.severity === 'CRITICAL')
  )

  const falsePositives = computed(() =>
    alerts.value.filter(a => a.falsePositive === true)
  )

  const getAlertById = (id: string) => computed(() =>
    alerts.value.find(a => a.id === id)
  )

  const getAlertByAlertId = (alertId: string) => computed(() =>
    alerts.value.find(a => a.alertId === alertId)
  )

  // Actions
  async function fetchAlerts(params: Partial<FraudSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()

      const query: any = {}
      if (params.status) {
        query.status = params.status
      }
      if (params.severity) {
        query.severity = params.severity
      }
      if (params.customerId) {
        query.customerId = params.customerId
      }

      const alertsList = await get<FraudAlert[]>('/api/fraud/alerts', { params: query })

      alerts.value = alertsList
      return alertsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch fraud alerts'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchOpenAlerts() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const openAlertsList = await get<FraudAlert[]>('/api/fraud/alerts/open')

      alerts.value = openAlertsList
      return openAlertsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch open alerts'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchHighRiskAlerts() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const highRiskAlertsList = await get<FraudAlert[]>('/api/fraud/alerts/high-risk')

      alerts.value = highRiskAlertsList
      return highRiskAlertsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch high-risk alerts'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchAlertById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const alert = await get<FraudAlert>(`/api/fraud/alerts/${id}`)

      const index = alerts.value.findIndex(a => a.id === id)
      if (index !== -1) {
        alerts.value[index] = alert
      } else {
        alerts.value.push(alert)
      }

      currentAlert.value = alert
      return alert
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch fraud alert'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createAlert(command: CreateFraudAlertCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const alert = await post<FraudAlert>('/api/fraud/alerts', command)

      alerts.value.unshift(alert)
      return alert
    } catch (err: any) {
      error.value = err.message || 'Failed to create fraud alert'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function assignAlert(id: string, analystId: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const alert = await post<FraudAlert>(`/api/fraud/alerts/${id}/assign`, { analystId })

      const index = alerts.value.findIndex(a => a.id === id)
      if (index !== -1) {
        alerts.value[index] = alert
      }

      if (currentAlert.value?.id === id) {
        currentAlert.value = alert
      }

      return alert
    } catch (err: any) {
      error.value = err.message || 'Failed to assign alert'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function resolveAlert(id: string, resolvedBy: string, resolutionNotes: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const alert = await post<FraudAlert>(`/api/fraud/alerts/${id}/resolve`, { resolvedBy, resolutionNotes })

      const index = alerts.value.findIndex(a => a.id === id)
      if (index !== -1) {
        alerts.value[index] = alert
      }

      if (currentAlert.value?.id === id) {
        currentAlert.value = alert
      }

      return alert
    } catch (err: any) {
      error.value = err.message || 'Failed to resolve alert'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function markAsFalsePositive(id: string, notedBy: string, reason: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const alert = await post<FraudAlert>(`/api/fraud/alerts/${id}/false-positive`, { notedBy, reason })

      const index = alerts.value.findIndex(a => a.id === id)
      if (index !== -1) {
        alerts.value[index] = alert
      }

      if (currentAlert.value?.id === id) {
        currentAlert.value = alert
      }

      return alert
    } catch (err: any) {
      error.value = err.message || 'Failed to mark alert as false positive'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function escalateAlert(id: string, escalatedTo: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const alert = await post<FraudAlert>(`/api/fraud/alerts/${id}/escalate`, { escalatedTo })

      const index = alerts.value.findIndex(a => a.id === id)
      if (index !== -1) {
        alerts.value[index] = alert
      }

      if (currentAlert.value?.id === id) {
        currentAlert.value = alert
      }

      return alert
    } catch (err: any) {
      error.value = err.message || 'Failed to escalate alert'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function performVelocityCheck(command: VelocityCheckCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const alert = await post<FraudAlert>('/api/fraud/checks/velocity', command)

      alerts.value.unshift(alert)
      return alert
    } catch (err: any) {
      error.value = err.message || 'Failed to perform velocity check'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function performHighValueCheck(command: HighValueCheckCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const alert = await post<FraudAlert>('/api/fraud/checks/high-value', command)

      alerts.value.unshift(alert)
      return alert
    } catch (err: any) {
      error.value = err.message || 'Failed to perform high-value check'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchFraudStatistics() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const stats = await get<FraudStatistics>('/api/fraud/statistics')

      return stats
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch fraud statistics'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentAlert() {
    currentAlert.value = null
  }

  return {
    // State
    alerts,
    currentAlert,
    loading,
    error,

    // Getters
    alertCount,
    newAlerts,
    assignedAlerts,
    inReviewAlerts,
    escalatedAlerts,
    resolvedAlerts,
    closedAlerts,
    rejectedAlerts,
    openAlerts,
    highRiskAlerts,
    criticalAlerts,
    falsePositives,
    getAlertById,
    getAlertByAlertId,

    // Actions
    fetchAlerts,
    fetchOpenAlerts,
    fetchHighRiskAlerts,
    fetchAlertById,
    createAlert,
    assignAlert,
    resolveAlert,
    markAsFalsePositive,
    escalateAlert,
    performVelocityCheck,
    performHighValueCheck,
    fetchFraudStatistics,
    clearError,
    clearCurrentAlert
  }
})
