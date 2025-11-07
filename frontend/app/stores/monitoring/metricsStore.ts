import { defineStore } from 'pinia'
import type { ResourceMetric, MetricSummary } from '~/types/monitoring/types'

export const useMetricsStore = defineStore('metrics', {
  state: () => ({
    metrics: [] as ResourceMetric[],
    summaries: [] as MetricSummary[],
    loading: false,
    error: null as string | null,
  }),

  actions: {
    async fetchMetrics(customerId: number, startTime?: Date, endTime?: Date) {
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

        const response = await $fetch<ResourceMetric[]>(
          `${config.public.apiBaseUrl}/metrics?${queryParams.toString()}`,
          {
            headers: {
              'Authorization': `Bearer ${await getAuthToken()}`,
            },
          }
        )

        this.metrics = response
      } catch (error: any) {
        this.error = error.message || 'Failed to fetch metrics'
        console.error('Error fetching metrics:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchResourceMetrics(resourceId: number) {
      this.loading = true
      this.error = null

      try {
        const config = useRuntimeConfig()
        const response = await $fetch<ResourceMetric[]>(
          `${config.public.apiBaseUrl}/metrics/resource/${resourceId}`,
          {
            headers: {
              'Authorization': `Bearer ${await getAuthToken()}`,
            },
          }
        )

        this.metrics = response
      } catch (error: any) {
        this.error = error.message || 'Failed to fetch resource metrics'
        console.error('Error fetching resource metrics:', error)
      } finally {
        this.loading = false
      }
    },

    async fetchLatestMetrics(resourceId: number) {
      try {
        const config = useRuntimeConfig()
        const response = await $fetch<ResourceMetric[]>(
          `${config.public.apiBaseUrl}/metrics/resource/${resourceId}/latest`,
          {
            headers: {
              'Authorization': `Bearer ${await getAuthToken()}`,
            },
          }
        )

        return response
      } catch (error: any) {
        console.error('Error fetching latest metrics:', error)
        return []
      }
    },

    async ingestMetric(metric: {
      customerId: number
      resourceId: number
      metricType: string
      value: number
      unit: string
      source: string
    }) {
      try {
        const config = useRuntimeConfig()
        await $fetch(`${config.public.apiBaseUrl}/metrics`, {
          method: 'POST',
          body: metric,
          headers: {
            'Authorization': `Bearer ${await getAuthToken()}`,
          },
        })
      } catch (error: any) {
        console.error('Error ingesting metric:', error)
        throw error
      }
    },

    async ingestBatchMetrics(metrics: Array<{
      customerId: number
      resourceId: number
      metricType: string
      value: number
      unit: string
      source: string
    }>) {
      try {
        const config = useRuntimeConfig()
        await $fetch(`${config.public.apiBaseUrl}/metrics/batch`, {
          method: 'POST',
          body: { metrics },
          headers: {
            'Authorization': `Bearer ${await getAuthToken()}`,
          },
        })
      } catch (error: any) {
        console.error('Error ingesting batch metrics:', error)
        throw error
      }
    },
  },

  getters: {
    getMetricsByType: (state) => (metricType: string) => {
      return state.metrics.filter(m => m.metricType === metricType)
    },

    getLatestMetric: (state) => (metricType: string) => {
      const filtered = state.metrics
        .filter(m => m.metricType === metricType)
        .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
      return filtered[0] || null
    },
  },
})

// Helper function to get auth token
async function getAuthToken(): Promise<string> {
  // In production, this would retrieve the actual JWT token
  // For now, return a placeholder
  return 'placeholder-token'
}
