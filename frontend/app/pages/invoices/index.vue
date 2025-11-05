<template>
  <div class="invoices-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Invoices</h1>
        <p class="page-subtitle">Manage customer invoices and track payments</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Create Invoice"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/invoices/create')"
        />
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="invoices-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search invoices..."
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

    <!-- Invoices Table -->
    <div class="invoices-table">
      <AppTable
        :columns="tableColumns"
        :data="invoices"
        :loading="invoiceStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Invoice Number column -->
        <template #cell-invoiceNumber="{ row }">
          <div class="invoice-number">
            <span class="invoice-number__text">{{ row.invoiceNumber }}</span>
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
        <template #cell-invoiceType="{ row }">
          <StatusBadge :status="row.invoiceType" type="invoice-type" size="small" />
        </template>

        <!-- Status column -->
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="invoice" size="small" />
        </template>

        <!-- Amount column -->
        <template #cell-totalAmount="{ row }">
          <span class="amount">
            {{ formatCurrency(row.totalAmount, row.currency) }}
          </span>
        </template>

        <!-- Due Date column -->
        <template #cell-dueDate="{ row }">
          <span class="date" :class="{ 'date--overdue': isOverdue(row) }">
            {{ formatDate(row.dueDate) }}
          </span>
          <i v-if="isOverdue(row)" class="pi pi-exclamation-triangle text-danger text-xs ml-1"></i>
        </template>

        <!-- Created column -->
        <template #cell-createdAt="{ row }">
          <span class="created-date">{{ formatDate(row.createdAt) }}</span>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="invoice-actions">
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
              v-tooltip.top="'Edit invoice'"
              v-if="canEditInvoice(row)"
            />
            <Button
              icon="pi pi-send"
              text
              rounded
              @click.stop="handleSend(row)"
              v-tooltip.top="'Send invoice'"
              v-if="canSendInvoice(row)"
            />
            <Button
              icon="pi pi-times"
              text
              rounded
              severity="danger"
              @click.stop="handleCancel(row)"
              v-tooltip.top="'Cancel invoice'"
              v-if="canCancelInvoice(row)"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-file empty-state__icon"></i>
            <h3 class="empty-state__title">No invoices found</h3>
            <p class="empty-state__description">
              {{ searchTerm || statusFilter || typeFilter ?
                'Try adjusting your search criteria' :
                'Get started by creating your first invoice'
              }}
            </p>
            <Button
              v-if="!searchTerm && !statusFilter && !typeFilter"
              label="Create First Invoice"
              icon="pi pi-plus"
              severity="primary"
              @click="navigateTo('/invoices/create')"
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
import { useInvoiceStore } from '~/stores/invoice'
import { formatCurrency, isInvoiceOverdue, canCancelInvoice, canSendInvoice } from '~/schemas/invoice'

// Page meta
definePageMeta({
  title: 'Invoices'
})

// Store
const invoiceStore = useInvoiceStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const statusFilter = ref('')
const typeFilter = ref('')
const sortOption = ref('createdAt,desc')

// Table columns configuration
const tableColumns = [
  {
    key: 'invoiceNumber',
    label: 'Invoice #',
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
    key: 'invoiceType',
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
    key: 'totalAmount',
    label: 'Amount',
    sortable: true,
    style: 'width: 12%',
    align: 'right'
  },
  {
    key: 'dueDate',
    label: 'Due Date',
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
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Issued', value: 'ISSUED' },
  { label: 'Sent', value: 'SENT' },
  { label: 'Paid', value: 'PAID' },
  { label: 'Overdue', value: 'OVERDUE' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

const typeOptions = [
  { label: 'All Types', value: '' },
  { label: 'One Time', value: 'ONE_TIME' },
  { label: 'Recurring', value: 'RECURRING' },
  { label: 'Adjustment', value: 'ADJUSTMENT' }
]

const sortOptions = [
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' },
  { label: 'Invoice # A-Z', value: 'invoiceNumber,asc' },
  { label: 'Invoice # Z-A', value: 'invoiceNumber,desc' },
  { label: 'Amount High-Low', value: 'totalAmount,desc' },
  { label: 'Amount Low-High', value: 'totalAmount,asc' },
  { label: 'Due Date', value: 'dueDate,desc' }
]

// Computed
const invoices = computed(() => invoiceStore.invoices)
const paginationProps = computed(() => ({
  page: invoiceStore.pagination.page,
  size: invoiceStore.pagination.size,
  total: invoiceStore.pagination.totalElements
}))

// Methods
const handleSearch = useDebounceFn(async () => {
  await invoiceStore.fetchInvoices({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    query: searchTerm.value || undefined
  })
}, 300)

const handleStatusFilter = async () => {
  await invoiceStore.fetchInvoices({
    page: 0,
    sort: sortOption.value,
    type: typeFilter.value || undefined,
    query: searchTerm.value || undefined,
    status: statusFilter.value || undefined
  })
}

const handleTypeFilter = async () => {
  await invoiceStore.fetchInvoices({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    query: searchTerm.value || undefined,
    type: typeFilter.value || undefined
  })
}

const handleSortChange = async () => {
  await invoiceStore.fetchInvoices({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    query: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await invoiceStore.fetchInvoices({
    page: invoiceStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    query: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await invoiceStore.fetchInvoices({
    page,
    size: rows,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    query: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/invoices/${row.id}`)
}

const handleEdit = (row: any) => {
  navigateTo(`/invoices/${row.id}/edit`)
}

const handleSend = async (row: any) => {
  try {
    await invoiceStore.changeInvoiceStatus({
      id: row.id,
      status: 'SENT'
    })

    showToast({
      severity: 'success',
      summary: 'Invoice Sent',
      detail: `Invoice ${row.invoiceNumber} has been sent to customer.`,
      life: 3000
    })

    await invoiceStore.fetchInvoices({
      status: statusFilter.value || undefined,
      type: typeFilter.value || undefined,
      query: searchTerm.value || undefined
    })

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to send invoice',
      life: 5000
    })
  }
}

const handleCancel = async (row: any) => {
  const confirmed = confirm(`Are you sure you want to cancel invoice ${row.invoiceNumber}?`)

  if (confirmed) {
    try {
      await invoiceStore.changeInvoiceStatus({
        id: row.id,
        status: 'CANCELLED'
      })

      showToast({
        severity: 'success',
        summary: 'Invoice Cancelled',
        detail: `Invoice ${row.invoiceNumber} has been cancelled.`,
        life: 3000
      })

      await invoiceStore.fetchInvoices({
        status: statusFilter.value || undefined,
        type: typeFilter.value || undefined,
        query: searchTerm.value || undefined
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to cancel invoice',
        life: 5000
      })
    }
  }
}

const canEditInvoice = (row: any) => {
  return row.status === 'DRAFT' || row.status === 'ISSUED'
}

const canSendInvoice = (row: any) => {
  return canSendInvoice(row)
}

const canCancelInvoiceFn = (row: any) => {
  return canCancelInvoice(row)
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

const isOverdue = (row: any): boolean => {
  return isInvoiceOverdue(row)
}

// Lifecycle
onMounted(async () => {
  await invoiceStore.fetchInvoices()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await invoiceStore.fetchInvoices()
})
</script>

<style scoped>
.invoices-page {
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
.invoices-filters {
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
.invoices-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Invoice Number Cell */
.invoice-number__text {
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

.date--overdue {
  color: var(--color-red-600);
  font-weight: var(--font-weight-semibold);
}

/* Actions */
.invoice-actions {
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
