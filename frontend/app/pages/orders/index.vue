<template>
  <div class="orders-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Orders</h1>
        <p class="page-subtitle">Manage customer orders and track their progress</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Create Order"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/orders/create')"
        />
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="orders-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search orders..."
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
          v-model="typeFilter"
          :options="typeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Types"
          style="width: 180px"
          @change="handleTypeFilter"
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

    <!-- Orders Table -->
    <div class="orders-table">
      <AppTable
        :columns="tableColumns"
        :data="orders"
        :loading="orderStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Order Number column -->
        <template #cell-orderNumber="{ row }">
          <div class="order-number">
            <span class="order-number__text">{{ row.orderNumber }}</span>
          </div>
        </template>

        <!-- Customer column -->
        <template #cell-customerName="{ row }">
          <div class="customer-name">
            <div class="customer-avatar">
              {{ getCustomerInitials(row) }}
            </div>
            <div class="customer-info">
              <div class="customer-name__text">{{ row.customerName || 'N/A' }}</div>
              <div class="customer-id">{{ row.customerId }}</div>
            </div>
          </div>
        </template>

        <!-- Type column -->
        <template #cell-orderType="{ row }">
          <StatusBadge :status="row.orderType" type="order-type" size="small" />
        </template>

        <!-- Status column -->
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="order" size="small" />
        </template>

        <!-- Priority column -->
        <template #cell-priority="{ row }">
          <StatusBadge :status="row.priority" type="priority" size="small" />
        </template>

        <!-- Amount column -->
        <template #cell-totalAmount="{ row }">
          <span v-if="row.totalAmount !== null && row.totalAmount !== undefined" class="amount">
            {{ formatCurrency(row.totalAmount, row.currency) }}
          </span>
          <span v-else class="amount--empty">—</span>
        </template>

        <!-- Date column -->
        <template #cell-requestedDate="{ row }">
          <span v-if="row.requestedDate" class="date">
            {{ formatDate(row.requestedDate) }}
          </span>
          <span v-else class="date--empty">—</span>
        </template>

        <!-- Created column -->
        <template #cell-createdAt="{ row }">
          <span class="created-date">{{ formatDate(row.createdAt) }}</span>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="order-actions">
            <Button
              icon="pi pi-eye"
              text
              rounded
              @click.stop="handleView(row)"
              v-tooltip.top="'View details'"
            />
            <Button
              icon="pi pi-pencil"
              text
              rounded
              @click.stop="handleEdit(row)"
              v-tooltip.top="'Edit order'"
              v-if="canEditOrder(row)"
            />
            <Button
              icon="pi pi-times"
              text
              rounded
              severity="danger"
              @click.stop="handleCancel(row)"
              v-tooltip.top="'Cancel order'"
              v-if="canCancelOrder(row)"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-shopping-cart empty-state__icon"></i>
            <h3 class="empty-state__title">No orders found</h3>
            <p class="empty-state__description">
              {{ searchTerm || statusFilter || typeFilter ?
                'Try adjusting your search criteria' :
                'Get started by creating your first order'
              }}
            </p>
            <Button
              v-if="!searchTerm && !statusFilter && !typeFilter"
              label="Create First Order"
              icon="pi pi-plus"
              severity="primary"
              @click="navigateTo('/orders/create')"
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
import { useOrderStore } from '~/stores/order'
import { formatCurrency, getOrderProgress, isOrderActive, canCancelOrder } from '~/schemas/order'

// Page meta
definePageMeta({
  title: 'Orders'
})

// Store
const orderStore = useOrderStore()
const { showToast } = useToast()

// Reactive state
const searchTerm = ref('')
const statusFilter = ref('')
const typeFilter = ref('')
const sortOption = ref('createdAt,desc')

// Table columns configuration
const tableColumns = [
  {
    key: 'orderNumber',
    label: 'Order #',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'customerName',
    label: 'Customer',
    sortable: false,
    style: 'width: 20%'
  },
  {
    key: 'orderType',
    label: 'Type',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'priority',
    label: 'Priority',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'totalAmount',
    label: 'Amount',
    sortable: true,
    style: 'width: 12%',
    align: 'right'
  },
  {
    key: 'requestedDate',
    label: 'Requested',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'createdAt',
    label: 'Created',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 10%'
  }
]

// Filter options
const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Confirmed', value: 'CONFIRMED' },
  { label: 'Processing', value: 'PROCESSING' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

const typeOptions = [
  { label: 'All Types', value: '' },
  { label: 'New', value: 'NEW' },
  { label: 'Upgrade', value: 'UPGRADE' },
  { label: 'Downgrade', value: 'DOWNGRADE' },
  { label: 'Cancel', value: 'CANCEL' },
  { label: 'Renew', value: 'RENEW' }
]

const sortOptions = [
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' },
  { label: 'Order # A-Z', value: 'orderNumber,asc' },
  { label: 'Order # Z-A', value: 'orderNumber,desc' },
  { label: 'Amount High-Low', value: 'totalAmount,desc' },
  { label: 'Amount Low-High', value: 'totalAmount,asc' },
  { label: 'Priority High-Low', value: 'priority,desc' },
  { label: 'Requested Date', value: 'requestedDate,desc' }
]

// Computed
const orders = computed(() => orderStore.orders)
const paginationProps = computed(() => ({
  page: orderStore.pagination.page,
  size: orderStore.pagination.size,
  total: orderStore.pagination.totalElements
}))

// Methods
const handleSearch = useDebounceFn(async () => {
  await orderStore.searchOrders(searchTerm.value, {
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined
  })
}, 300)

const handleStatusFilter = async () => {
  await orderStore.getOrdersByStatus(statusFilter.value as any, {
    page: 0,
    sort: sortOption.value,
    type: typeFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleTypeFilter = async () => {
  await orderStore.fetchOrders({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSortChange = async () => {
  await orderStore.fetchOrders({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await orderStore.fetchOrders({
    page: orderStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await orderStore.fetchOrders({
    page,
    size: rows,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/orders/${row.id}`)
}

const handleEdit = (row: any) => {
  navigateTo(`/orders/${row.id}/edit`)
}

const handleCancel = async (row: any) => {
  const confirmed = confirm(`Are you sure you want to cancel order ${row.orderNumber}?`)

  if (confirmed) {
    try {
      await orderStore.updateOrderStatus({
        id: row.id,
        status: 'CANCELLED'
      })

      showToast({
        severity: 'success',
        summary: 'Order Cancelled',
        detail: `Order ${row.orderNumber} has been successfully cancelled.`,
        life: 3000
      })

      // Refresh the list
      await orderStore.fetchOrders({
        status: statusFilter.value || undefined,
        type: typeFilter.value || undefined,
        searchTerm: searchTerm.value || undefined
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to cancel order',
        life: 5000
      })
    }
  }
}

const canEditOrder = (row: any) => {
  return isOrderActive(row)
}

const canCancelOrderFn = (row: any) => {
  return canCancelOrder(row)
}

// Utility functions
const getCustomerInitials = (row: any): string => {
  if (!row.customerName) return 'N/A'
  const names = row.customerName.split(' ')
  if (names.length >= 2) {
    return `${names[0][0]}${names[1][0]}`.toUpperCase()
  }
  return row.customerName.substring(0, 2).toUpperCase()
}

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

// Lifecycle
onMounted(async () => {
  await orderStore.fetchOrders()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await orderStore.fetchOrders()
})
</script>

<style scoped>
.orders-page {
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
.orders-filters {
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
.orders-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Order Number Cell */
.order-number__text {
  font-family: monospace;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
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

.customer-name__text {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.customer-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Amount Cell */
.amount {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.amount--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Date Cell */
.date,
.created-date {
  color: var(--color-text-primary);
}

.date--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Actions */
.order-actions {
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

  .customer-name__text {
    font-size: var(--font-size-sm);
  }

  .customer-id {
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
