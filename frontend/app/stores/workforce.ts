import { defineStore } from 'pinia'
import { ref, computed, reactive } from 'vue'
import type {
  Employee,
  WorkOrder,
  CreateEmployeeCommand,
  UpdateEmployeeCommand,
  CreateWorkOrderCommand,
  AssignWorkOrderCommand,
  StartWorkOrderCommand,
  CompleteWorkOrderCommand,
  AutoAssignWorkOrderCommand,
  EmployeeSearchParams,
  WorkOrderSearchParams,
  WorkforceStatistics,
  EmployeeType,
  EmployeeStatus,
  WorkOrderType,
  WorkOrderStatus
} from '~/schemas/workforce'

export const useWorkforceStore = defineStore('workforce', () => {
  // State
  const employees = ref<Employee[]>([])
  const workOrders = ref<WorkOrder[]>([])
  const currentEmployee = ref<Employee | null>(null)
  const currentWorkOrder = ref<WorkOrder | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters - Employees
  const employeeCount = computed(() => employees.value.length)
  const activeEmployees = computed(() => employees.value.filter(e => e.status === 'ACTIVE'))
  const inactiveEmployees = computed(() => employees.value.filter(e => e.status === 'INACTIVE'))
  const onLeaveEmployees = computed(() => employees.value.filter(e => e.status === 'ON_LEAVE'))
  const sickEmployees = computed(() => employees.value.filter(e => e.status === 'SICK'))
  const suspendedEmployees = computed(() => employees.value.filter(e => e.status === 'SUSPENDED'))
  const terminatedEmployees = computed(() => employees.value.filter(e => e.status === 'TERMINATED'))
  const pendingEmployees = computed(() => employees.value.filter(e => e.status === 'PENDING'))

  const technicians = computed(() => employees.value.filter(e => e.employeeType === 'TECHNICIAN' || e.employeeType === 'SENIOR_TECHNICIAN'))
  const seniorTechnicians = computed(() => employees.value.filter(e => e.employeeType === 'SENIOR_TECHNICIAN'))
  const supervisors = computed(() => employees.value.filter(e => e.employeeType === 'SUPERVISOR'))
  const managers = computed(() => employees.value.filter(e => e.employeeType === 'MANAGER'))
  const specialists = computed(() => employees.value.filter(e => e.employeeType === 'SPECIALIST'))
  const contractors = computed(() => employees.value.filter(e => e.employeeType === 'CONTRACTOR'))
  const dispatchers = computed(() => employees.value.filter(e => e.employeeType === 'DISPATCHER'))
  const inspectors = computed(() => employees.value.filter(e => e.employeeType === 'INSPECTOR'))

  const onCallTechnicians = computed(() => employees.value.filter(e => e.onCall === true && e.status === 'ACTIVE'))

  const getEmployeeById = (id: string) => computed(() =>
    employees.value.find(e => e.id === id)
  )

  // Getters - Work Orders
  const workOrderCount = computed(() => workOrders.value.length)
  const pendingWorkOrders = computed(() => workOrders.value.filter(w => w.status === 'PENDING'))
  const scheduledWorkOrders = computed(() => workOrders.value.filter(w => w.status === 'SCHEDULED'))
  const assignedWorkOrders = computed(() => workOrders.value.filter(w => w.status === 'ASSIGNED'))
  const inProgressWorkOrders = computed(() => workOrders.value.filter(w => w.status === 'IN_PROGRESS'))
  const onHoldWorkOrders = computed(() => workOrders.value.filter(w => w.status === 'ON_HOLD'))
  const completedWorkOrders = computed(() => workOrders.value.filter(w => w.status === 'COMPLETED'))
  const cancelledWorkOrders = computed(() => workOrders.value.filter(w => w.status === 'CANCELLED'))
  const reviewRequiredWorkOrders = computed(() => workOrders.value.filter(w => w.status === 'REQUIRES_REVIEW'))
  const partsRequiredWorkOrders = computed(() => workOrders.value.filter(w => w.status === 'REQUIRES_PARTS'))

  const emergencyWorkOrders = computed(() => workOrders.value.filter(w => w.type === 'EMERGENCY'))
  const overdueWorkOrders = computed(() => workOrders.value.filter(w => {
    if (!w.dueDate) return false
    const dueDate = new Date(w.dueDate)
    const now = new Date()
    return dueDate < now && w.status !== 'COMPLETED'
  }))

  const installationWorkOrders = computed(() => workOrders.value.filter(w => w.type === 'INSTALLATION'))
  const repairWorkOrders = computed(() => workOrders.value.filter(w => w.type === 'REPAIR'))
  const maintenanceWorkOrders = computed(() => workOrders.value.filter(w => w.type === 'MAINTENANCE'))
  const inspectionWorkOrders = computed(() => workOrders.value.filter(w => w.type === 'INSPECTION'))

  const getWorkOrderById = (id: string) => computed(() =>
    workOrders.value.find(w => w.id === id)
  )

  // Actions - Employees
  async function fetchEmployees(params: Partial<EmployeeSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()

      const query: any = {}
      if (params.type) {
        query.type = params.type
      }
      if (params.status) {
        query.status = params.status
      }

      const employeesList = await get<Employee[]>('/api/workforce/employees', { params: query })

      employees.value = employeesList
      return employeesList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch employees'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchEmployeeById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const employee = await get<Employee>(`/api/workforce/employees/${id}`)

      const index = employees.value.findIndex(e => e.id === id)
      if (index !== -1) {
        employees.value[index] = employee
      } else {
        employees.value.push(employee)
      }

      currentEmployee.value = employee
      return employee
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch employee'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createEmployee(command: CreateEmployeeCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const employee = await post<Employee>('/api/workforce/employees', command)

      employees.value.unshift(employee)
      return employee
    } catch (err: any) {
      error.value = err.message || 'Failed to create employee'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateEmployee(id: string, command: UpdateEmployeeCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { put } = useApi()
      const employee = await put<Employee>(`/api/workforce/employees/${id}`, command)

      const index = employees.value.findIndex(e => e.id === id)
      if (index !== -1) {
        employees.value[index] = employee
      }

      if (currentEmployee.value?.id === id) {
        currentEmployee.value = employee
      }

      return employee
    } catch (err: any) {
      error.value = err.message || 'Failed to update employee'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function activateEmployee(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const employee = await post<Employee>(`/api/workforce/employees/${id}/activate`, {})

      const index = employees.value.findIndex(e => e.id === id)
      if (index !== -1) {
        employees.value[index] = employee
      }

      if (currentEmployee.value?.id === id) {
        currentEmployee.value = employee
      }

      return employee
    } catch (err: any) {
      error.value = err.message || 'Failed to activate employee'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function suspendEmployee(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const employee = await post<Employee>(`/api/workforce/employees/${id}/suspend`, {})

      const index = employees.value.findIndex(e => e.id === id)
      if (index !== -1) {
        employees.value[index] = employee
      }

      if (currentEmployee.value?.id === id) {
        currentEmployee.value = employee
      }

      return employee
    } catch (err: any) {
      error.value = err.message || 'Failed to suspend employee'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchEmployeesBySkill(skillName: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const employeesList = await get<Employee[]>(`/api/workforce/employees/skills/${skillName}`)

      return employeesList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch employees by skill'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchAvailableTechnicians() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const techniciansList = await get<Employee[]>('/api/workforce/employees/available')

      return techniciansList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch available technicians'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchOnCallTechnicians() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const techniciansList = await get<Employee[]>('/api/workforce/employees/on-call')

      return techniciansList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch on-call technicians'
      throw err
    } finally {
      loading.value = false
    }
  }

  // Actions - Work Orders
  async function fetchWorkOrders(params: Partial<WorkOrderSearchParams> = {}) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()

      const query: any = {}
      if (params.type) {
        query.type = params.type
      }
      if (params.status) {
        query.status = params.status
      }
      if (params.date) {
        query.date = params.date
      }

      const workOrdersList = await get<WorkOrder[]>('/api/workforce/work-orders', { params: query })

      workOrders.value = workOrdersList
      return workOrdersList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch work orders'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchWorkOrderById(id: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const workOrder = await get<WorkOrder>(`/api/workforce/work-orders/${id}`)

      const index = workOrders.value.findIndex(w => w.id === id)
      if (index !== -1) {
        workOrders.value[index] = workOrder
      } else {
        workOrders.value.push(workOrder)
      }

      currentWorkOrder.value = workOrder
      return workOrder
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch work order'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createWorkOrder(command: CreateWorkOrderCommand) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const workOrder = await post<WorkOrder>('/api/workforce/work-orders', command)

      workOrders.value.unshift(workOrder)
      return workOrder
    } catch (err: any) {
      error.value = err.message || 'Failed to create work order'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function assignWorkOrder(id: string, employeeId: string, assignedBy: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const workOrder = await post<WorkOrder>(`/api/workforce/work-orders/${id}/assign`, { employeeId, assignedBy })

      const index = workOrders.value.findIndex(w => w.id === id)
      if (index !== -1) {
        workOrders.value[index] = workOrder
      }

      if (currentWorkOrder.value?.id === id) {
        currentWorkOrder.value = workOrder
      }

      return workOrder
    } catch (err: any) {
      error.value = err.message || 'Failed to assign work order'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function autoAssignWorkOrder(id: string, performedBy: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const workOrdersList = await post<WorkOrder[]>(`/api/workforce/work-orders/${id}/auto-assign`, { performedBy })

      return workOrdersList
    } catch (err: any) {
      error.value = err.message || 'Failed to auto-assign work order'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function startWorkOrder(id: string, employeeId: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const workOrder = await post<WorkOrder>(`/api/workforce/work-orders/${id}/start`, { employeeId })

      const index = workOrders.value.findIndex(w => w.id === id)
      if (index !== -1) {
        workOrders.value[index] = workOrder
      }

      if (currentWorkOrder.value?.id === id) {
        currentWorkOrder.value = workOrder
      }

      return workOrder
    } catch (err: any) {
      error.value = err.message || 'Failed to start work order'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function completeWorkOrder(id: string, completionNotes: string) {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { post } = useApi()
      const workOrder = await post<WorkOrder>(`/api/workforce/work-orders/${id}/complete`, { completionNotes })

      const index = workOrders.value.findIndex(w => w.id === id)
      if (index !== -1) {
        workOrders.value[index] = workOrder
      }

      if (currentWorkOrder.value?.id === id) {
        currentWorkOrder.value = workOrder
      }

      return workOrder
    } catch (err: any) {
      error.value = err.message || 'Failed to complete work order'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchPendingWorkOrders() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const workOrdersList = await get<WorkOrder[]>('/api/workforce/work-orders/pending')

      workOrders.value = workOrdersList
      return workOrdersList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch pending work orders'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchOverdueWorkOrders() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const workOrdersList = await get<WorkOrder[]>('/api/workforce/work-orders/overdue')

      return workOrdersList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch overdue work orders'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchEmergencyWorkOrders() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const workOrdersList = await get<WorkOrder[]>('/api/workforce/work-orders/emergency')

      return workOrdersList
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch emergency work orders'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchWorkforceStatistics() {
    loading.value = true
    error.value = null

    try {
      const { useApi } = await import('~/composables/useApi')
      const { get } = useApi()
      const stats = await get<WorkforceStatistics>('/api/workforce/statistics')

      return stats
    } catch (err: any) {
      error.value = err.message || 'Failed to fetch workforce statistics'
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentEmployee() {
    currentEmployee.value = null
  }

  function clearCurrentWorkOrder() {
    currentWorkOrder.value = null
  }

  return {
    // State
    employees,
    workOrders,
    currentEmployee,
    currentWorkOrder,
    loading,
    error,

    // Getters - Employees
    employeeCount,
    activeEmployees,
    inactiveEmployees,
    onLeaveEmployees,
    sickEmployees,
    suspendedEmployees,
    terminatedEmployees,
    pendingEmployees,
    technicians,
    seniorTechnicians,
    supervisors,
    managers,
    specialists,
    contractors,
    dispatchers,
    inspectors,
    onCallTechnicians,
    getEmployeeById,

    // Getters - Work Orders
    workOrderCount,
    pendingWorkOrders,
    scheduledWorkOrders,
    assignedWorkOrders,
    inProgressWorkOrders,
    onHoldWorkOrders,
    completedWorkOrders,
    cancelledWorkOrders,
    reviewRequiredWorkOrders,
    partsRequiredWorkOrders,
    emergencyWorkOrders,
    overdueWorkOrders,
    installationWorkOrders,
    repairWorkOrders,
    maintenanceWorkOrders,
    inspectionWorkOrders,
    getWorkOrderById,

    // Actions - Employees
    fetchEmployees,
    fetchEmployeeById,
    createEmployee,
    updateEmployee,
    activateEmployee,
    suspendEmployee,
    fetchEmployeesBySkill,
    fetchAvailableTechnicians,
    fetchOnCallTechnicians,

    // Actions - Work Orders
    fetchWorkOrders,
    fetchWorkOrderById,
    createWorkOrder,
    assignWorkOrder,
    autoAssignWorkOrder,
    startWorkOrder,
    completeWorkOrder,
    fetchPendingWorkOrders,
    fetchOverdueWorkOrders,
    fetchEmergencyWorkOrders,

    // Statistics
    fetchWorkforceStatistics,

    // Utility
    clearError,
    clearCurrentEmployee,
    clearCurrentWorkOrder
  }
})
