import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  CustomerResourceConfiguration,
  CreateConfigurationCommand,
  UpdateConfigurationCommand,
  DeleteConfigurationCommand,
  ConfigurationSearchParams
} from '~/schemas/customer-resource-configurations'

export const useCustomerResourceConfigurationsStore = defineStore('customerResourceConfigurations', () => {
  // State
  const configurations = ref<CustomerResourceConfiguration[]>([])
  const currentConfiguration = ref<CustomerResourceConfiguration | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const configurationCount = computed(() => configurations.value.length)
  const activeConfigurations = computed(() => configurations.value.filter(c => c.status === 'ACTIVE'))
  const inactiveConfigurations = computed(() => configurations.value.filter(c => c.status === 'INACTIVE'))
  const suspendedConfigurations = computed(() => configurations.value.filter(c => c.status === 'SUSPENDED'))

  const autoScalingConfigurations = computed(() =>
    configurations.value.filter(c => c.autoScalingEnabled === true)
  )

  const getConfigurationById = (id: number) => computed(() =>
    configurations.value.find(c => c.id === id)
  )

  const getConfigurationsByResourceId = (resourceId: string) => computed(() =>
    configurations.value.filter(c => c.resourceId === resourceId)
  )

  // Actions
  async function createConfiguration(command: CreateConfigurationCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const configuration = await post<CustomerResourceConfiguration>('/api/monitoring/customer-resource-configurations', null, {
        params: command
      })

      configurations.value.unshift(configuration)
      return configuration
    } catch (err: any) {
      error.value = err.message || 'Failed to create configuration'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateConfiguration(id: number, command: UpdateConfigurationCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const configuration = await put<CustomerResourceConfiguration>(`/api/monitoring/customer-resource-configurations/${id}`, null, {
        params: command
      })

      const index = configurations.value.findIndex(c => c.id === id)
      if (index !== -1) {
        configurations.value[index] = configuration
      }

      if (currentConfiguration.value?.id === id) {
        currentConfiguration.value = configuration
      }

      return configuration
    } catch (err: any) {
      error.value = err.message || 'Failed to update configuration'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteConfiguration(id: number) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { del } = useApi()
      await del(`/api/monitoring/customer-resource-configurations/${id}`)

      const index = configurations.value.findIndex(c => c.id === id)
      if (index !== -1) {
        configurations.value.splice(index, 1)
      }

      if (currentConfiguration.value?.id === id) {
        currentConfiguration.value = null
      }

      return true
    } catch (err: any) {
      error.value = err.message || 'Failed to delete configuration'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchConfigurationsByCustomerId(customerId: number) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const configurationsList = await get<CustomerResourceConfiguration[]>(
        `/api/monitoring/customer-resource-configurations/customer/${customerId}`
      )

      return configurationsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch configurations by customer'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchConfigurationsByCustomerIdAndResourceType(
    customerId: number,
    resourceType: string
  ) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const configurationsList = await get<CustomerResourceConfiguration[]>(
        `/api/monitoring/customer-resource-configurations/customer/${customerId}/resource-type/${resourceType}`
      )

      return configurationsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch configurations by customer and resource type'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchConfigurationsByResourceId(resourceId: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const configurationsList = await get<CustomerResourceConfiguration[]>(
        `/api/monitoring/customer-resource-configurations/resource/${resourceId}`
      )

      return configurationsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch configurations by resource ID'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchConfigurationsByStatus(status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED') {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const configurationsList = await get<CustomerResourceConfiguration[]>(
        `/api/monitoring/customer-resource-configurations/status/${status}`
      )

      return configurationsList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch configurations by status'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchConfigurations(params: Partial<ConfigurationSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      let configurationsList: CustomerResourceConfiguration[] = []

      // Use specific endpoints based on params
      if (params.customerId && params.resourceType) {
        configurationsList = await fetchConfigurationsByCustomerIdAndResourceType(
          params.customerId,
          params.resourceType
        )
        configurations.value = configurationsList
      } else if (params.customerId) {
        configurationsList = await fetchConfigurationsByCustomerId(params.customerId)
        configurations.value = configurationsList
      } else if (params.resourceId) {
        configurationsList = await fetchConfigurationsByResourceId(params.resourceId)
        configurations.value = configurationsList
      } else if (params.status) {
        configurationsList = await fetchConfigurationsByStatus(params.status)
        configurations.value = configurationsList
      } else {
        // If no specific params, fetch all configurations (need to implement or adjust)
        configurations.value = []
      }

      return configurations.value
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch configurations'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentConfiguration() {
    currentConfiguration.value = null
  }

  return {
    // State
    configurations,
    currentConfiguration,
    loading,
    error,

    // Getters
    configurationCount,
    activeConfigurations,
    inactiveConfigurations,
    suspendedConfigurations,
    autoScalingConfigurations,
    getConfigurationById,
    getConfigurationsByResourceId,

    // Actions
    createConfiguration,
    updateConfiguration,
    deleteConfiguration,
    fetchConfigurationsByCustomerId,
    fetchConfigurationsByCustomerIdAndResourceType,
    fetchConfigurationsByResourceId,
    fetchConfigurationsByStatus,
    fetchConfigurations,
    clearError,
    clearCurrentConfiguration
  }
})
