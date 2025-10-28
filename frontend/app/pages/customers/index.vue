<template>
  <div class="customers-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Customers</h1>
        <p class="page-subtitle">Manage customer information and relationships</p>
      </div>
      <div class="page-header__actions">
        <AppButton 
          variant="primary" 
          icon="➕"
          @click="navigateTo('/customers/create')"
        >
          Add Customer
        </AppButton>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="customers-filters">
      <div class="filters-row">
        <AppInput
          v-model="searchTerm"
          placeholder="Search customers..."
          icon="🔍"
          size="md"
          clearable
          @input="handleSearch"
        />
        
        <AppSelect
          v-model="statusFilter"
          :options="statusOptions"
          placeholder="All Statuses"
          size="md"
          @change="handleStatusFilter"
        />
        
        <AppSelect
          v-model="sortOption"
          :options="sortOptions"
          size="md"
          @change="handleSortChange"
        />
      </div>
    </div>

    <!-- Customers Table -->
    <div class="customers-table">
      <AppTable
        :columns="tableColumns"
        :data="customers"
        :loading="loading"
        :show-pagination="true"
        :pagination="paginationData"
        clickable
        @sort="handleSort"
        @row-click="handleRowClick"
        @page-change="handlePageChange"
        @size-change="handleSizeChange"
      >
        <!-- Name column -->
        <template #cell-name="{ value, row }">
          <div class="customer-name">
            <div class="customer-avatar">
              {{ getInitials(row) }}
            </div>
            <div class="customer-info">
              <div class="customer-full-name">{{ formatCustomerName(row) }}</div>
              <div class="customer-email">{{ row.email }}</div>
            </div>
          </div>
        </template>

        <!-- Status column -->
        <template #cell-status="{ value, row }">
          <AppBadge
            :variant="getCustomerStatusVariant(row.status)"
            :text="CUSTOMER_STATUS_LABELS[row.status]"
            size="sm"
          />
        </template>

        <!-- Phone column -->
        <template #cell-phone="{ value }">
          <span v-if="value" class="customer-phone">{{ value }}</span>
          <span v-else class="customer-phone--empty">—</span>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="customer-actions">
            <button
              class="action-button"
              @click.stop="handleView(row)"
              title="View details"
            >
              👁️
            </button>
            <button
              class="action-button"
              @click.stop="handleEdit(row)"
              title="Edit customer"
            >
              ✏️
            </button>
            <button
              class="action-button action-button--danger"
              @click.stop="handleDelete(row)"
              title="Delete customer"
            >
              🗑️
            </button>
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <div class="empty-state__icon">👥</div>
            <h3 class="empty-state__title">No customers found</h3>
            <p class="empty-state__description">
              {{ searchTerm || statusFilter ? 
                'Try adjusting your search criteria' : 
                'Get started by adding your first customer' 
              }}
            </p>
            <AppButton 
              v-if="!searchTerm && !statusFilter"
              variant="primary"
              @click="navigateTo('/customers/create')"
            >
              Add First Customer
            </AppButton>
          </div>
        </template>

        <!-- Pagination -->
        <template #pagination="{ pagination }">
          <div class="table-pagination">
            <div class="pagination-info">
              {{ paginationInfo }}
            </div>
            <AppPagination
              :page="pagination.page"
              :size="pagination.size"
              :total="pagination.total"
              :page-count="pagination.pageCount"
              :has-next="pagination.hasNext"
              :has-previous="pagination.hasPrevious"
              @page-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </template>
      </AppTable>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { 
  Customer, 
  CustomerStatus, 
  CUSTOMER_STATUS_LABELS,
  CUSTOMER_STATUS_COLORS,
  formatCustomerName,
  getInitials,
  getCustomerStatusVariant
} from '~/types/customer'

// Page meta
definePageMeta({
  title: 'Customers'
})

// Composables
const { get, del } = useApi()
const { showToast, confirm } = useToast()
const { showLoading, hideLoading } = useModal()
const pagination = usePagination({
  initialPage: 0,
  initialSize: 20,
  initialSort: 'createdAt,desc'
})

// Reactive state
const customers = ref<Customer[]>([])
const loading = ref(false)
const searchTerm = ref('')
const statusFilter = ref<CustomerStatus | ''>('')
const sortOption = ref('createdAt,desc')

// Table columns configuration
const tableColumns = [
  {
    key: 'name',
    label: 'Customer',
    sortable: false, // Custom rendering for combined name + email
    width: '35%'
  },
  {
    key: 'phone',
    label: 'Phone',
    sortable: false,
    width: '15%'
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    width: '15%'
  },
  {
    key: 'createdAt',
    label: 'Created',
    sortable: true,
    width: '20%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    width: '15%'
  }
]

// Filter options
const statusOptions = [
  { value: '', label: 'All Statuses' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'SUSPENDED', label: 'Suspended' },
  { value: 'TERMINATED', label: 'Terminated' }
]

const sortOptions = [
  { value: 'createdAt,desc', label: 'Newest First' },
  { value: 'createdAt,asc', label: 'Oldest First' },
  { value: 'firstName,asc', label: 'Name A-Z' },
  { value: 'firstName,desc', label: 'Name Z-A' },
  { value: 'status,asc', label: 'Status A-Z' },
  { value: 'lastName,asc', label: 'Last Name A-Z' }
]

// Computed
const paginationData = computed(() => ({
  page: pagination.page.value,
  size: pagination.size.value,
  total: undefined, // Will be set from API response
  pageCount: undefined,
  hasNext: pagination.hasNext.value,
  hasPrevious: pagination.hasPrevious.value
}))

const paginationInfo = computed(() => {
  const total = customers.value.length
  if (!total) return 'No customers found'
  
  const start = pagination.startIndex.value
  const end = pagination.endIndex.value
  
  return `Showing ${start}-${end} of ${total} customers`
})

// Methods
const fetchCustomers = async () => {
  loading.value = true
  
  try {
    const query: any = {
      page: pagination.page.value,
      size: pagination.size.value,
      sort: pagination.sort.value
    }

    // Add search term if provided
    if (searchTerm.value.trim()) {
      query.search = searchTerm.value.trim()
    }

    // Add status filter if provided
    if (statusFilter.value) {
      query.status = statusFilter.value
    }

    const response = await get<{
      content: Customer[]
      totalElements: number
      totalPages: number
      size: number
      number: number
      first: boolean
      last: boolean
      numberOfElements: number
      empty: boolean
    }>('/customers', { query })

    customers.value = response.data.content
    
    // Update pagination total
    if (response.data.totalElements !== undefined) {
      pagination.setTotal(response.data.totalElements)
    }
    
  } catch (error) {
    console.error('Failed to fetch customers:', error)
    showToast({
      type: 'error',
      title: 'Error',
      message: 'Failed to load customers. Please try again.'
    })
  } finally {
    loading.value = false
  }
}

// Event handlers
const handleSearch = useDebounceFn(() => {
  pagination.setPage(0) // Reset to first page
  fetchCustomers()
}, 300)

const handleStatusFilter = () => {
  pagination.setPage(0) // Reset to first page
  fetchCustomers()
}

const handleSortChange = (sort: string) => {
  pagination.setSort(sort)
  fetchCustomers()
}

const handleSort = (column: any, direction: 'asc' | 'desc') => {
  pagination.setSortColumn(column.key, direction)
  fetchCustomers()
}

const handlePageChange = (page: number) => {
  pagination.setPage(page)
  fetchCustomers()
}

const handleSizeChange = (size: number) => {
  pagination.setSize(size)
  fetchCustomers()
}

const handleRowClick = (row: Customer) => {
  navigateTo(`/customers/${row.id}`)
}

const handleView = (row: Customer) => {
  navigateTo(`/customers/${row.id}`)
}

const handleEdit = (row: Customer) => {
  navigateTo(`/customers/${row.id}/edit`)
}

const handleDelete = async (row: Customer) => {
  const confirmed = await confirm(
    `Are you sure you want to delete ${formatCustomerName(row)}?`,
    {
      title: 'Delete Customer',
      message: `This action cannot be undone. ${formatCustomerName(row)} (${row.email}) will be permanently removed.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      variant: 'danger'
    }
  )

  if (confirmed) {
    try {
      showLoading('Deleting customer...')
      
      await del(`/customers/${row.id}`)
      
      hideLoading()
      showToast({
        type: 'success',
        title: 'Customer Deleted',
        message: `${formatCustomerName(row)} has been successfully deleted.`
      })
      
      // Refresh the list
      fetchCustomers()
      
    } catch (error) {
      hideLoading()
      console.error('Failed to delete customer:', error)
      // Error handling is done in useApi composable
    }
  }
}

// Lifecycle
onMounted(() => {
  fetchCustomers()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, () => {
  fetchCustomers()
})
</script>

<style scoped>
.customers-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* Page Header */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.page-header__content {
  flex: 1;
}

.page-title {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.page-header__actions {
  flex-shrink: 0;
}

/* Filters */
.customers-filters {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.filters-row {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  flex-wrap: wrap;
}

.filters-row .app-input,
.filters-row .app-select {
  flex: 1;
  min-width: 200px;
}

.filters-row .app-select {
  flex: 0 0 150px;
}

.filters-row .app-select:last-child {
  flex: 0 0 180px;
}

/* Table */
.customers-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Customer Name Cell */
.customer-name {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.customer-avatar {
  width: 40px;
  height: 40px;
  background: var(--color-primary);
  color: var(--color-text-primary);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  flex-shrink: 0;
}

.customer-info {
  flex: 1;
  min-width: 0;
}

.customer-full-name {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.customer-email {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Phone Cell */
.customer-phone {
  color: var(--color-text-primary);
}

.customer-phone--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Actions */
.customer-actions {
  display: flex;
  gap: var(--space-1);
  align-items: center;
}

.action-button {
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-base);
  transition: all var(--transition-fast) var(--transition-timing);
  color: var(--color-text-secondary);
}

.action-button:hover {
  background: var(--color-surface-alt);
  color: var(--color-text-primary);
}

.action-button--danger:hover {
  background: var(--color-danger-light);
  color: var(--color-danger);
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-12);
  text-align: center;
}

.empty-state__icon {
  font-size: 4rem;
  margin-bottom: var(--space-4);
  opacity: 0.5;
}

.empty-state__title {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.empty-state__description {
  margin: 0 0 var(--space-6) 0;
  color: var(--color-text-secondary);
  max-width: 400px;
}

/* Table Pagination */
.table-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  background: var(--color-surface-alt);
  border-top: 1px solid var(--color-border);
}

.pagination-info {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }
  
  .page-header__actions {
    align-self: flex-start;
  }
  
  .filters-row {
    flex-direction: column;
    gap: var(--space-3);
  }
  
  .filters-row .app-input,
  .filters-row .app-select {
    min-width: unset;
    flex: 1;
  }
  
  .customer-name {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }
  
  .customer-avatar {
    width: 32px;
    height: 32px;
  }
  
  .customer-full-name {
    font-size: var(--font-size-sm);
  }
  
  .customer-email {
    font-size: var(--font-size-xs);
  }
  
  .table-pagination {
    flex-direction: column;
    gap: var(--space-3);
    text-align: center;
  }
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .customer-name {
    gap: var(--space-2);
  }
  
  .customer-avatar {
    width: 36px;
    height: 36px;
  }
}
</style>
