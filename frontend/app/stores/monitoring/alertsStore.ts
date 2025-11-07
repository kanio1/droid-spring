import { defineStore } from 'pinia'
import type { Alert, AlertStatistics } from '~/types/monitoring/types'

export const useAlertsStore = defineStore('alerts', {
  state: () => ({
    alerts: [] as Alert[],
    activeAlerts: [] as Alert[],
    statistics: null as AlertStatistics | null,
    loading: false,
    error: null as string | null,
  }),

  actions: {
    async fetchAlerts(customerId: number, startTime?: Date, endTime?: Date) {
      this.loading = true
      this.error = null

      try {
        const config = useRuntimeConfig()
        const queryParams = new URLSearchParams({
          customerId: customerId.toString(),
        })

        if (startTime) {
          queryParams.append('startTime', startTime.toISOString())
        }
        if (endTime) {
          queryParams.append('endTime', endTime.toISOString())
        }

        const response = await $fetch<Alert[]>(
          `${config.public.apiBaseUrl}/alerts?${queryParams.toString()}`,
          {
            headers: {
              'Authorization': `Bearer ${await getAuthToken()}`,
            },
          }
        )

        this.alerts = response
      } catch (error: any) {
        this.error = error.message || 'Failed to fetch alerts'
        console.error('Error fetching alerts:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchActiveAlerts() {
      this.loading = true
      this.error = null

      try {
        const config = useRuntimeConfig()
        const response = await $fetch<Alert[]>(
          `${config.public.apiBaseUrl}/alerts/active`,
          {
            headers: {
              'Authorization': `Bearer ${await getAuthToken()}`,
            },
          }
        )

        this.activeAlerts = response
      } catch (error: any) {
        this.error = error.message || 'Failed to fetch active alerts'
        console.error('Error fetching active alerts:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchActiveAlertsByCustomer(customerId: number) {
      this.loading = true
      this.error = null

      try {
        const config = useRuntimeConfig()
        const response = await $fetch<Alert[]>(
          `${config.public.apiBaseUrl}/alerts/customer/${customerId}/active`,
          {
            headers: {
              'Authorization': `Bearer ${await getAuthToken()}`,
            },
          }
        )

        this.activeAlerts = response
      } catch (error: any) {
        this.error = error.message || 'Failed to fetch customer active alerts'
        console.error('Error fetching customer active alerts:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchAlertById(alertId: number) {
      try {
        const config = useRuntimeConfig()
        const response = await $fetch<Alert>(
          `${config.public.apiBaseUrl}/alerts/${alertId}`,
          {
            headers: {
              'Authorization': `Bearer ${await getAuthToken()}`,
            },
          }
        )

        return response
      } catch (error: any) {
        console.error('Error fetching alert by ID:', error)
        throw error
      }
    },

    async acknowledgeAlert(alertId: number, userId: string) {
      try {
        const config = useRuntimeConfig()
        const response = await $fetch<Alert>(
          `${config.public.apiBaseUrl}/alerts/${alertId}/acknowledge`,
          {
            method: 'POST',
            body: { userId },
            headers: {
              'Authorization': `Bearer ${await getAuthToken()}`,
            },
          }
        )

        // Update local state
        const index = this.alerts.findIndex(a => a.id === alertId)
        if (index !== -1) {
          this.alerts[index] = response
        }

        const activeIndex = this.activeAlerts.findIndex(a => a.id === alertId)
        if (activeIndex !== -1) {
          this.activeAlerts[activeIndex] = response
        }

        return response
      } catch (error: any) {
        console.error('Error acknowledging alert:', error)
        throw error
      }
    },

    async resolveAlert(alertId: number, userId: string) {
      try {
        const config = useRuntimeConfig()
        const response = await $fetch<Alert>(
          `${config.public.apiBaseUrl}/alerts/${alertId}/resolve`,
          {
            method: 'POST',
            body: { userId },
            headers: {
              'Authorization': `Bearer ${await getAuthToken()}`,
            },
          }
        )

        // Update local state
        const index = this.alerts.findIndex(a => a.id === alertId)
        if (index !== -1) {
          this.alerts[index] = response
        }

        // Remove from active alerts
        this.activeAlerts = this.activeAlerts.filter(a => a.id !== alertId)

        return response
      } catch (error: any) {
        console.error('Error resolving alert:', error)
        throw error
      }
    },

    async deleteAlert(alertId: number) {
      try {
        const config = useRuntimeConfig()
        await $fetch(`${config.public.apiBaseUrl}/alerts/${alertId}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${await getAuthToken()}`,
          },
        })

        // Update local state
        this.alerts = this.alerts.filter(a => a.id !== alertId)
        this.activeAlerts = this.activeAlerts.filter(a => a.id !== alertId)

        return true
      } catch (error: any) {
        console.error('Error deleting alert:', error)
        throw error
      }
    },
  },

  getters: {
    getAlertsBySeverity: (state) => (severity: string) => {
      return state.alerts.filter(a => a.severity === severity)
    },

    getAlertsByStatus: (state) => (status: string) => {
      return state.alerts.filter(a => a.status === status)
    },

    criticalAlerts: (state) => {
      return state.alerts.filter(a => a.severity === 'CRITICAL' && a.status !== 'RESOLVED')
    },

    warningAlerts: (state) => {
      return state.alerts.filter(a => a.severity === 'WARNING' && a.status !== 'RESOLVED')
    },
  },
})

// Helper function to get auth token
async function getAuthToken(): Promise<string> {
  return 'placeholder-token'
}
