<template>
  <div class="payments-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Payments</h1>
        <p class="page-subtitle">Manage customer payments and transactions</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Create Payment"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/payments/create')"
        />
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="payments-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search payments..."
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
          v-model="methodFilter"
          :options="methodOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Methods"
          style="width: 180px"
          @change="handleMethodFilter"
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

    <!-- Payments Table -->
    <div class="payments-table">
      <AppTable
        :columns="tableColumns"
        :data="payments"
        :loading="paymentStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Payment Number column -->
        <template #cell-paymentNumber="{ row }">
          <div class="payment-number">
            <span class="payment-number__text">{{ row.paymentNumber }}</span>
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

        <!-- Amount column -->
        <template #cell-amount="{ row }">
          <span class="amount">
            {{ formatCurrency(row.amount, row.currency) }}
          </span>
        </template>

        <!-- Method column -->
        <template #cell-paymentMethod="{ row }">
          <StatusBadge :status="row.paymentMethod" type="payment-method" size="small" />
        </template>

        <!-- Status column -->
        <template #cell-paymentStatus="{ row }">
          <StatusBadge :status="row.paymentStatus" type="payment" size="small" />
        </template>

        <!-- Date column -->
        <template #cell-paymentDate="{ row }">
          <span v-if="row.paymentDate" class="date">
            {{ formatDate(row.paymentDate) }}
          </span>
          <span v-else class="date--empty">—</span>
        </template>

        <!-- Created column -->
        <template #cell-createdAt="{ row }">
          <span class="created-date">{{ formatDate(row.createdAt) }}</span>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="payment-actions">
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
              v-tooltip.top="'Edit payment'"
              v-if="canEditPayment(row)"
            />
            <Button
              icon="pi pi-refresh"
              text
              rounded
              severity="info"
              @click.stop="handleRetry(row)"
              v-tooltip.top="'Retry payment'"
              v-if="canRetryPayment(row)"
            />
            <Button
              icon="pi pi-times"
              text
              rounded
              severity="danger"
              @click.stop="handleCancel(row)"
              v-tooltip.top="'Cancel payment'"
              v-if="canCancelPayment(row)"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-credit-card empty-state__icon"></i>
            <h3 class="empty-state__title">No payments found</h3>
            <p class="empty-state__description">
              {{ searchTerm || statusFilter || methodFilter ?
                'Try adjusting your search criteria' :
                'Get started by creating your first payment'
              }}
            </p>
            <Button
              v-if="!searchTerm && !statusFilter && !methodFilter"
              label="Create First Payment"
              icon="pi pi-plus"
              severity="primary"
              @click="navigateTo('/payments/create')"
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
import { usePaymentStore } from '~/stores/payment'
import { canCancelPayment, canRetryPayment } from '~/schemas/payment'

// Page meta
definePageMeta({
  title: 'Payments'
})

// Store
const paymentStore = usePaymentStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const statusFilter = ref('')
const methodFilter = ref('')
const sortOption = ref('createdAt,desc')

// Table columns configuration
const tableColumns = [
  {
    key: 'paymentNumber',
    label: 'Payment #',
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
    key: 'amount',
    label: 'Amount',
    sortable: true,
    style: 'width: 12%',
    align: 'right'
  },
  {
    key: 'paymentMethod',
    label: 'Method',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'paymentStatus',
    label: 'Status',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'paymentDate',
    label: 'Payment Date',
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
  { label: 'Processing', value: 'PROCESSING' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Failed', value: 'FAILED' },
  { label: 'Refunded', value: 'REFUNDED' }
]

const methodOptions = [
  { label: 'All Methods', value: '' },
  { label: 'Credit Card', value: 'CARD' },
  { label: 'Bank Transfer', value: 'BANK_TRANSFER' },
  { label: 'Cash', value: 'CASH' },
  { label: 'Direct Debit', value: 'DIRECT_DEBIT' },
  { label: 'Mobile Pay', value: 'MOBILE_PAY' }
]

const sortOptions = [
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' },
  { label: 'Payment # A-Z', value: 'paymentNumber,asc' },
  { label: 'Payment # Z-A', value: 'paymentNumber,desc' },
  { label: 'Amount High-Low', value: 'amount,desc' },
  { label: 'Amount Low-High', value: 'amount,asc' },
  { label: 'Payment Date', value: 'paymentDate,desc' }
]

// Computed
const payments = computed(() => paymentStore.payments)
const paginationProps = computed(() => ({
  page: paymentStore.pagination.page,
  size: paymentStore.pagination.size,
  total: paymentStore.pagination.totalElements
}))

// Methods
const handleSearch = useDebounceFn(async () => {
  await paymentStore.fetchPayments({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    paymentMethod: methodFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}, 300)

const handleStatusFilter = async () => {
  await paymentStore.fetchPayments({
    page: 0,
    sort: sortOption.value,
    paymentMethod: methodFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined
  })
}

const handleMethodFilter = async () => {
  await paymentStore.fetchPayments({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    paymentMethod: methodFilter.value || undefined
  })
}

const handleSortChange = async () => {
  await paymentStore.fetchPayments({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    paymentMethod: methodFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await paymentStore.fetchPayments({
    page: paymentStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    status: statusFilter.value || undefined,
    paymentMethod: methodFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await paymentStore.fetchPayments({
    page,
    size: rows,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    paymentMethod: methodFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/payments/${row.id}`)
}

const handleEdit = (row: any) => {
  navigateTo(`/payments/${row.id}/edit`)
}

const handleRetry = async (row: any) => {
  const confirmed = confirm(`Are you sure you want to retry payment ${row.paymentNumber}?`)

  if (confirmed) {
    try {
      await paymentStore.changePaymentStatus({
        id: row.id,
        status: 'PENDING'
      })

      showToast({
        severity: 'success',
        summary: 'Payment Retried',
        detail: `Payment ${row.paymentNumber} has been queued for retry.`,
        life: 3000
      })

      await paymentStore.fetchPayments({
        status: statusFilter.value || undefined,
        paymentMethod: methodFilter.value || undefined,
        searchTerm: searchTerm.value || undefined
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to retry payment',
        life: 5000
      })
    }
  }
}

const handleCancel = async (row: any) => {
  const confirmed = confirm(`Are you sure you want to cancel payment ${row.paymentNumber}?`)

  if (confirmed) {
    try {
      await paymentStore.changePaymentStatus({
        id: row.id,
        status: 'FAILED'
      })

      showToast({
        severity: 'success',
        summary: 'Payment Cancelled',
        detail: `Payment ${row.paymentNumber} has been cancelled.`,
        life: 3000
      })

      await paymentStore.fetchPayments({
        status: statusFilter.value || undefined,
        paymentMethod: methodFilter.value || undefined,
        searchTerm: searchTerm.value || undefined
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to cancel payment',
        life: 5000
      })
    }
  }
}

const canEditPayment = (row: any) => {
  return row.paymentStatus === 'PENDING' || row.paymentStatus === 'PROCESSING'
}

const canRetryPaymentFn = (row: any) => {
  return canRetryPayment(row)
}

const canCancelPaymentFn = (row: any) => {
  return canCancelPayment(row)
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
  await paymentStore.fetchPayments()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await paymentStore.fetchPayments()
})
</script>

<style scoped>
.payments-page {
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
.payments-filters {
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
.payments-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Payment Number Cell */
.payment-number__text {
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
.payment-actions {
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
