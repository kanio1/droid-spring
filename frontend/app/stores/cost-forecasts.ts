import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  CostForecast,
  GenerateForecastCommand,
  CostForecastSearchParams
} from '~/schemas/cost-forecasts'

export const useCostForecastsStore = defineStore('costForecasts', () => {
  // State
  const forecasts = ref<CostForecast[]>([])
  const currentForecast = ref<CostForecast | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const forecastCount = computed(() => forecasts.value.length)
  const increasingForecasts = computed(() => forecasts.value.filter(f => f.trendDirection === 'INCREASING'))
  const decreasingForecasts = computed(() => forecasts.value.filter(f => f.trendDirection === 'DECREASING'))
  const stableForecasts = computed(() => forecasts.value.filter(f => f.trendDirection === 'STABLE'))

  const totalPredictedCost = computed(() =>
    forecasts.value.reduce((sum, f) => sum + f.predictedCost, 0)
  )

  const averageConfidence = computed(() => {
    if (forecasts.value.length === 0) return 0
    const sum = forecasts.value.reduce((s, f) => s + f.confidenceLevel, 0)
    return sum / forecasts.value.length
  })

  const getForecastById = (id: number) => computed(() =>
    forecasts.value.find(f => f.id === id)
  )

  // Actions
  async function generateForecast(command: GenerateForecastCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const forecastsList = await post<CostForecast[]>('/api/monitoring/cost-forecasts/generate', null, {
        params: command
      })

      forecasts.value.unshift(...forecastsList)
      return forecastsList
    } catch (err: any) {
      error.value = err.message || 'Failed to generate forecast'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchForecasts(params: Partial<CostForecastSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()

      const query: any = {}
      if (params.customerId) {
        query.customerId = params.customerId
      }
      if (params.resourceType) {
        query.resourceType = params.resourceType
      }
      if (params.startDate) {
        query.startDate = params.startDate
      }
      if (params.endDate) {
        query.endDate = params.endDate
      }

      // If both customerId and resourceType are provided, use the specific endpoint
      if (params.customerId && params.resourceType) {
        const forecastsList = await get<CostForecast[]>(
          `/api/monitoring/cost-forecasts/customer/${params.customerId}/resource/${params.resourceType}`
        )
        forecasts.value = forecastsList
      } else if (params.customerId && params.startDate && params.endDate) {
        const forecastsList = await get<CostForecast[]>(
          `/api/monitoring/cost-forecasts/customer/${params.customerId}`,
          { params: { startDate: params.startDate, endDate: params.endDate } }
        )
        forecasts.value = forecastsList
      } else if (params.forecastPeriodStart) {
        const forecastsList = await get<CostForecast[]>(
          `/api/monitoring/cost-forecasts/period/${params.forecastPeriodStart}`
        )
        forecasts.value = forecastsList
      } else {
        // If no specific params, fetch all (this may need to be adjusted based on API)
        forecasts.value = []
      }

      return forecasts.value
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch cost forecasts'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchForecastsByCustomerIdAndResourceType(
    customerId: number,
    resourceType: string
  ) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const forecastsList = await get<CostForecast[]>(
        `/api/monitoring/cost-forecasts/customer/${customerId}/resource/${resourceType}`
      )

      return forecastsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch forecasts by customer and resource type'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchForecastsByCustomerIdAndPeriod(
    customerId: number,
    startDate: string,
    endDate: string
  ) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const forecastsList = await get<CostForecast[]>(
        `/api/monitoring/cost-forecasts/customer/${customerId}`,
        { params: { startDate, endDate } }
      )

      return forecastsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch forecasts by customer and period'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchForecastsByForecastPeriodStart(forecastPeriodStart: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const forecastsList = await get<CostForecast[]>(
        `/api/monitoring/cost-forecasts/period/${forecastPeriodStart}`
      )

      return forecastsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch forecasts by forecast period'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentForecast() {
    currentForecast.value = null
  }

  return {
    // State
    forecasts,
    currentForecast,
    loading,
    error,

    // Getters
    forecastCount,
    increasingForecasts,
    decreasingForecasts,
    stableForecasts,
    totalPredictedCost,
    averageConfidence,
    getForecastById,

    // Actions
    generateForecast,
    fetchForecasts,
    fetchForecastsByCustomerIdAndResourceType,
    fetchForecastsByCustomerIdAndPeriod,
    fetchForecastsByForecastPeriodStart,
    clearError,
    clearCurrentForecast
  }
})
