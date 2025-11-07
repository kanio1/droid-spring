import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  CostCalculation,
  CalculateCostCommand,
  RecalculateCostCommand,
  CostCalculationSearchParams
} from '~/schemas/cost-calculations'

export const useCostCalculationsStore = defineStore('costCalculations', () => {
  // State
  const calculations = ref<CostCalculation[]>([])
  const currentCalculation = ref<CostCalculation | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const calculationCount = computed(() => calculations.value.length)
  const draftCalculations = computed(() => calculations.value.filter(c => c.status === 'DRAFT'))
  const finalCalculations = computed(() => calculations.value.filter(c => c.status === 'FINAL'))
  const invoicedCalculations = computed(() => calculations.value.filter(c => c.status === 'INVOICED'))

  const totalCalculatedCost = computed(() =>
    calculations.value.reduce((sum, c) => sum + c.totalCost, 0)
  )

  const totalOverageCost = computed(() =>
    calculations.value.reduce((sum, c) => sum + c.overageCost, 0)
  )

  const getCalculationById = (id: number) => computed(() =>
    calculations.value.find(c => c.id === id)
  )

  // Actions
  async function calculate(command: CalculateCostCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const calculation = await post<CostCalculation>('/api/monitoring/cost-calculations', null, {
        params: command
      })

      calculations.value.unshift(calculation)
      return calculation
    } catch (err: any) {
      error.value = err.message || 'Failed to calculate cost'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function recalculate(id: number) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const calculation = await post<CostCalculation>(`/api/monitoring/cost-calculations/${id}/recalculate`, {})

      const index = calculations.value.findIndex(c => c.id === id)
      if (index !== -1) {
        calculations.value[index] = calculation
      }

      if (currentCalculation.value?.id === id) {
        currentCalculation.value = calculation
      }

      return calculation
    } catch (err: any) {
      error.value = err.message || 'Failed to recalculate cost'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchCalculations(params: Partial<CostCalculationSearchParams> = {}) {
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
      if (params.status) {
        query.status = params.status
      }
      if (params.startDate) {
        query.startDate = params.startDate
      }
      if (params.endDate) {
        query.endDate = params.endDate
      }

      const calculationsList = await get<CostCalculation[]>('/api/monitoring/cost-calculations', { params: query })

      calculations.value = calculationsList
      return calculationsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch cost calculations'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchCalculationById(id: number) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const calculationsList = await get<CostCalculation[]>(`/api/monitoring/cost-calculations/${id}`)

      // API returns a list, take the first one
      const calculation = calculationsList[0]

      if (calculation) {
        const index = calculations.value.findIndex(c => c.id === id)
        if (index !== -1) {
          calculations.value[index] = calculation
        } else {
          calculations.value.push(calculation)
        }

        currentCalculation.value = calculation
      }

      return calculation
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch cost calculation'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchCalculationsByCustomerAndPeriod(
    customerId: number,
    startDate: string,
    endDate: string
  ) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const calculationsList = await get<CostCalculation[]>(
        `/api/monitoring/cost-calculations/customer/${customerId}`,
        { params: { startDate, endDate } }
      )

      return calculationsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch cost calculations by customer and period'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchCalculationsByCustomerAndResourceType(
    customerId: number,
    resourceType: string
  ) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const calculationsList = await get<CostCalculation[]>(
        `/api/monitoring/cost-calculations/customer/${customerId}/resource/${resourceType}`
      )

      return calculationsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch cost calculations by resource type'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchCalculationsByStatus(status: 'DRAFT' | 'FINAL' | 'INVOICED') {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const calculationsList = await get<CostCalculation[]>(
        `/api/monitoring/cost-calculations/status/${status}`
      )

      return calculationsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch cost calculations by status'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentCalculation() {
    currentCalculation.value = null
  }

  return {
    // State
    calculations,
    currentCalculation,
    loading,
    error,

    // Getters
    calculationCount,
    draftCalculations,
    finalCalculations,
    invoicedCalculations,
    totalCalculatedCost,
    totalOverageCost,
    getCalculationById,

    // Actions
    calculate,
    recalculate,
    fetchCalculations,
    fetchCalculationById,
    fetchCalculationsByCustomerAndPeriod,
    fetchCalculationsByCustomerAndResourceType,
    fetchCalculationsByStatus,
    clearError,
    clearCurrentCalculation
  }
})
