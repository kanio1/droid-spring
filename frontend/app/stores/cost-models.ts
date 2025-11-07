import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  CostModel,
  CreateCostModelCommand,
  UpdateCostModelCommand,
  DeleteCostModelCommand,
  CostModelSearchParams
} from '~/schemas/cost-models'

export const useCostModelsStore = defineStore('costModels', () => {
  // State
  const models = ref<CostModel[]>([])
  const currentModel = ref<CostModel | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const modelCount = computed(() => models.value.length)
  const activeModels = computed(() => models.value.filter(m => m.active))
  const inactiveModels = computed(() => models.value.filter(m => !m.active))

  const hourlyModels = computed(() => models.value.filter(m => m.billingPeriod === 'hourly'))
  const dailyModels = computed(() => models.value.filter(m => m.billingPeriod === 'daily'))
  const monthlyModels = computed(() => models.value.filter(m => m.billingPeriod === 'monthly'))
  const yearlyModels = computed(() => models.value.filter(m => m.billingPeriod === 'yearly'))

  const getModelById = (id: number) => computed(() =>
    models.value.find(m => m.id === id)
  )

  const getModelByModelName = (modelName: string) => computed(() =>
    models.value.find(m => m.modelName === modelName)
  )

  // Actions
  async function createModel(command: CreateCostModelCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const model = await post<CostModel>('/api/monitoring/cost-models', null, {
        params: command
      })

      models.value.unshift(model)
      return model
    } catch (err: any) {
      error.value = err.message || 'Failed to create cost model'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateModel(id: number, command: UpdateCostModelCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const model = await put<CostModel>(`/api/monitoring/cost-models/${id}`, null, {
        params: command
      })

      const index = models.value.findIndex(m => m.id === id)
      if (index !== -1) {
        models.value[index] = model
      }

      if (currentModel.value?.id === id) {
        currentModel.value = model
      }

      return model
    } catch (err: any) {
      error.value = err.message || 'Failed to update cost model'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteModel(id: number) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { del } = useApi()
      await del(`/api/monitoring/cost-models/${id}`)

      const index = models.value.findIndex(m => m.id === id)
      if (index !== -1) {
        models.value.splice(index, 1)
      }

      if (currentModel.value?.id === id) {
        currentModel.value = null
      }

      return true
    } catch (err: any) {
      error.value = err.message || 'Failed to delete cost model'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchActiveModels() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const modelsList = await get<CostModel[]>('/api/monitoring/cost-models')

      models.value = modelsList
      return modelsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch active cost models'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchModelById(id: number) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const model = await get<CostModel>(`/api/monitoring/cost-models/${id}`)

      const index = models.value.findIndex(m => m.id === id)
      if (index !== -1) {
        models.value[index] = model
      } else {
        models.value.push(model)
      }

      currentModel.value = model
      return model
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch cost model'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchModelByModelName(modelName: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const model = await get<CostModel>(`/api/monitoring/cost-models/by-name/${modelName}`)

      const index = models.value.findIndex(m => m.modelName === modelName)
      if (index !== -1) {
        models.value[index] = model
      } else {
        models.value.push(model)
      }

      currentModel.value = model
      return model
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch cost model by name'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchModels(params: Partial<CostModelSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      // For now, fetch all active models and filter client-side
      // This can be adjusted based on backend API capabilities
      const modelsList = await fetchActiveModels()

      let filtered = modelsList

      if (params.active !== undefined) {
        filtered = filtered.filter(m => m.active === params.active)
      }

      if (params.billingPeriod) {
        filtered = filtered.filter(m => m.billingPeriod === params.billingPeriod)
      }

      if (params.modelName) {
        const term = params.modelName.toLowerCase()
        filtered = filtered.filter(m => m.modelName.toLowerCase().includes(term))
      }

      models.value = filtered
      return filtered
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch cost models'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentModel() {
    currentModel.value = null
  }

  return {
    // State
    models,
    currentModel,
    loading,
    error,

    // Getters
    modelCount,
    activeModels,
    inactiveModels,
    hourlyModels,
    dailyModels,
    monthlyModels,
    yearlyModels,
    getModelById,
    getModelByModelName,

    // Actions
    createModel,
    updateModel,
    deleteModel,
    fetchActiveModels,
    fetchModelById,
    fetchModelByModelName,
    fetchModels,
    clearError,
    clearCurrentModel
  }
})
