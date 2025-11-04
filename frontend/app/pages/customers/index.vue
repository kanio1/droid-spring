<template>
  <div class="customers-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Customers</h1>
        <p class="page-subtitle">Manage customer information and relationships</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Add Customer"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/customers/create')"
        />
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="customers-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search customers..."
            @input="handleSearch"
            style="width: 100%"
          />
        </span>

        <Dropdown
          v-model="statusFilter"
          :options="statusOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Statuses"
          style="width: 180px"
          @change="handleStatusFilter"
        />

        <Dropdown
          v-model="sortOption"
          :options="sortOptions"
          optionLabel="label"
          optionValue="value"
          style="width: 180px"
          @change="handleSortChange"
        />
      </div>
    </div>

    <!-- Customers Table -->
    <div class="customers-table">
      <AppTable
        :columns="tableColumns"
        :data="customers"
        :loading="customerStore.loading"
        :show-pagination="true"
        :pagination="customerStore.pagination"
        @page="handlePage"
        @sort="handleSort"
        @update:sortField="sortField = $event"
        @update:sortOrder="sortOrder = $event"
      >
        <!-- Name column -->
        <template #cell-name="{ row }">
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
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="customer" size="small" />
        </template>

        <!-- Phone column -->
        <template #cell-phone="{ value }">
          <span v-if="value" class="customer-phone">{{ value }}</span>
          <span v-else class="customer-phone--empty">—</span>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="customer-actions">
            <Button
              icon="pi pi-eye"
              text
              rounded
              @click="handleView(row)"
              v-tooltip.top="'View details'"
            />
            <Button
              icon="pi pi-pencil"
              text
              rounded
              @click="handleEdit(row)"
              v-tooltip.top="'Edit customer'"
            />
            <Button
              icon="pi pi-trash"
              text
              rounded
              severity="danger"
              @click="handleDelete(row)"
              v-tooltip.top="'Delete customer'"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-users empty-state__icon"></i>
            <h3 class="empty-state__title">No customers found</h3>
            <p class="empty-state__description">
              {{ searchTerm || statusFilter ?
                'Try adjusting your search criteria' :
                'Get started by adding your first customer'
              }}
            </p>
            <Button
              v-if="!searchTerm && !statusFilter"
              label="Add First Customer"
              icon="pi pi-plus"
              severity="primary"
              @click="navigateTo('/customers/create')"
            />
          </div>
        </template>
      </AppTable>
    </div>

    <!-- Toast for notifications -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { useCustomerStore } from '~/stores/customer'
import type { Customer, CustomerStatus } from '~/schemas/customer'
import { CUSTOMER_STATUS_LABELS } from '~/schemas/customer'
import { formatCustomerName, getInitials } from '~/schemas/customer'

// Page meta
definePageMeta({
  title: 'Customers'
})

// Store
const customerStore = useCustomerStore()
const { showToast } = useToast()

// Reactive state
const searchTerm = ref('')
const statusFilter = ref<CustomerStatus | ''>('')
const sortOption = ref('createdAt,desc')
const sortField = ref('createdAt')
const sortOrder = ref(-1)

// Table columns configuration
const tableColumns = [
  {
    key: 'name',
    label: 'Customer',
    sortable: false,
    style: 'width: 35%'
  },
  {
    key: 'phone',
    label: 'Phone',
    sortable: false,
    style: 'width: 15%'
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'createdAt',
    label: 'Created',
    sortable: true,
    style: 'width: 20%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 15%'
  }
]

// Filter options
const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Suspended', value: 'SUSPENDED' },
  { label: 'Terminated', value: 'TERMINATED' }
]

const sortOptions = [
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' },
  { label: 'Name A-Z', value: 'firstName,asc' },
  { label: 'Name Z-A', value: 'firstName,desc' },
  { label: 'Status A-Z', value: 'status,asc' },
  { label: 'Last Name A-Z', value: 'lastName,asc' }
]

// Computed
const customers = computed(() => customerStore.customers)

// Methods
const handleSearch = useDebounceFn(async () => {
  await customerStore.fetchCustomers({
    searchTerm: searchTerm.value || undefined,
    page: 0,
    sort: `${sortField.value},${sortOrder.value === -1 ? 'desc' : 'asc'}`
  })
}, 300)

const handleStatusFilter = async () => {
  await customerStore.fetchCustomers({
    status: statusFilter.value || undefined,
    page: 0,
    sort: `${sortField.value},${sortOrder.value === -1 ? 'desc' : 'asc'}`
  })
}

const handleSortChange = async () => {
  const [field, order] = sortOption.value.split(',')
  sortField.value = field
  sortOrder.value = order === 'desc' ? -1 : 1

  await customerStore.fetchCustomers({
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined,
    page: 0,
    sort: sortOption.value
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  sortField.value = field
  sortOrder.value = order

  await customerStore.fetchCustomers({
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined,
    page: customerStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await customerStore.fetchCustomers({
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined,
    page,
    size: rows,
    sort: `${sortField.value},${sortOrder.value === -1 ? 'desc' : 'asc'}`
  })
}

const handleView = (row: Customer) => {
  navigateTo(`/customers/${row.id}`)
}

const handleEdit = (row: Customer) => {
  navigateTo(`/customers/${row.id}/edit`)
}

const handleDelete = async (row: Customer) => {
  const confirmed = confirm(
    `Are you sure you want to delete ${formatCustomerName(row)}?`
  )

  if (confirmed) {
    try {
      await customerStore.deleteCustomer(row.id)

      showToast({
        severity: 'success',
        summary: 'Customer Deleted',
        detail: `${formatCustomerName(row)} has been successfully deleted.`,
        life: 3000
      })

      // Refresh the list
      await customerStore.fetchCustomers({
        searchTerm: searchTerm.value || undefined,
        status: statusFilter.value || undefined
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to delete customer',
        life: 5000
      })
    }
  }
}

// Lifecycle
onMounted(async () => {
  await customerStore.fetchCustomers()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await customerStore.fetchCustomers()
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

.filters-row > * {
  flex-shrink: 0;
}

.filters-row .p-inputtext {
  width: 100%;
  min-width: 250px;
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
  color: white;
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
  color: var(--color-text-muted);
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

  .filters-row .p-inputtext {
    min-width: unset;
    width: 100%;
  }

  .filters-row > * {
    width: 100%;
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
