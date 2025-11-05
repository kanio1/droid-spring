<template>
  <div class="billing-cycles-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Billing Cycles</h1>
        <p class="page-subtitle">
          Manage customer billing cycles and track invoice generation
        </p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Create Cycle"
          icon="pi pi-plus"
          severity="primary"
          @click="handleCreateCycle"
        />
      </div>
    </div>

    <!-- View Toggle -->
    <div class="view-toggle">
      <div class="toggle-buttons">
        <Button
          :label="'Timeline View'"
          :variant="viewMode === 'timeline' ? 'primary' : 'secondary'"
          @click="viewMode = 'timeline'"
        >
          <template #icon>
            <i class="pi pi-list"></i>
          </template>
        </Button>
        <Button
          :label="'Table View'"
          :variant="viewMode === 'table' ? 'primary' : 'secondary'"
          @click="viewMode = 'table'"
        >
          <template #icon>
            <i class="pi pi-table"></i>
          </template>
        </Button>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="billing-cycles-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search billing cycles..."
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

        <Calendar
          v-model="dateRangeFilter"
          selectionMode="range"
          :showIcon="true"
          icon="pi pi-calendar"
          placeholder="Date Range"
          style="width: 200px"
          @change="handleDateRangeFilter"
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

    <!-- Billing Summary Stats -->
    <div class="billing-stats">
      <div class="stat-card">
        <div class="stat-card__icon">
          <i class="pi pi-calendar"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ paginationProps.total }}</div>
          <div class="stat-card__label">Total Cycles</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--info">
          <i class="pi pi-clock"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ pendingCount }}</div>
          <div class="stat-card__label">Pending</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--warning">
          <i class="pi pi-spin pi-spinner"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ processingCount }}</div>
          <div class="stat-card__label">Processing</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--success">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ completedCount }}</div>
          <div class="stat-card__label">Completed</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--danger">
          <i class="pi pi-exclamation-triangle"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ failedCount }}</div>
          <div class="stat-card__label">Failed</div>
        </div>
      </div>
    </div>

    <!-- Timeline View -->
    <div v-if="viewMode === 'timeline'" class="timeline-view">
      <div class="timeline-container">
        <div
          v-for="cycle in billingCycles"
          :key="cycle.id"
          class="timeline-item"
          @click="handleView(cycle)"
        >
          <div class="timeline-marker" :class="`timeline-marker--${cycle.status.toLowerCase()}`">
            <i :class="getStatusIcon(cycle.status)" />
          </div>
          <div class="timeline-content">
            <div class="timeline-header">
              <div class="timeline-title">
                <h3>Cycle #{{ cycle.cycleNumber }}</h3>
                <StatusBadge :status="cycle.status" type="billing-cycle" size="small" />
              </div>
              <div class="timeline-date">
                {{ formatDateRange(cycle.startDate, cycle.endDate) }}
              </div>
            </div>
            <div class="timeline-details">
              <div class="timeline-info">
                <span class="info-label">Customer:</span>
                <span class="info-value">{{ cycle.customerId.substring(0, 8) }}...</span>
              </div>
              <div class="timeline-info" v-if="cycle.totalCost">
                <span class="info-label">Total Cost:</span>
                <span class="info-value">{{ formatCurrency(cycle.totalCost, cycle.currency) }}</span>
              </div>
              <div class="timeline-info" v-if="cycle.totalRatedCost">
                <span class="info-label">Rated Cost:</span>
                <span class="info-value">{{ formatCurrency(cycle.totalRatedCost, cycle.currency) }}</span>
              </div>
              <div class="timeline-info" v-if="cycle.totalUsage">
                <span class="info-label">Total Usage:</span>
                <span class="info-value">{{ cycle.totalUsage }}</span>
              </div>
              <div class="timeline-info">
                <span class="info-label">Due Date:</span>
                <span class="info-value">{{ formatDate(cycle.dueDate) }}</span>
              </div>
            </div>
            <div class="timeline-actions">
              <Button
                v-if="cycle.status === 'PENDING'"
                label="Process"
                icon="pi pi-play"
                size="small"
                @click.stop="handleProcess(cycle)"
              />
              <Button
                v-if="cycle.status === 'COMPLETED' && cycle.invoiceId"
                label="View Invoice"
                icon="pi pi-file-pdf"
                size="small"
                variant="secondary"
                @click.stop="handleViewInvoice(cycle)"
              />
            </div>
          </div>
        </div>

        <!-- Empty Timeline -->
        <div v-if="billingCycles.length === 0" class="timeline-empty">
          <i class="pi pi-calendar timeline-empty-icon"></i>
          <h3>No billing cycles found</h3>
          <p>Get started by creating your first billing cycle</p>
          <Button
            label="Create First Cycle"
            icon="pi pi-plus"
            severity="primary"
            @click="handleCreateCycle"
          />
        </div>
      </div>
    </div>

    <!-- Table View -->
    <div v-else class="table-view">
      <AppTable
        :columns="tableColumns"
        :data="billingCycles"
        :loading="billingStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Cycle Number column -->
        <template #cell-cycleNumber="{ row }">
          <div class="cycle-number">
            <span class="cycle-number__text">#{{ row.cycleNumber }}</span>
          </div>
        </template>

        <!-- Customer column -->
        <template #cell-customerId="{ row }">
          <div class="customer-name">
            <div class="customer-avatar">
              {{ getCustomerIdShort(row.customerId) }}
            </div>
            <div class="customer-info">
              <div class="customer-id">{{ row.customerId.substring(0, 8) }}...</div>
            </div>
          </div>
        </template>

        <!-- Status column -->
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="billing-cycle" size="small" />
        </template>

        <!-- Date Range column -->
        <template #cell-startDate="{ row }">
          <div class="date-range">
            <div class="date-range__start">{{ formatDate(row.startDate) }}</div>
            <div class="date-range__separator">to</div>
            <div class="date-range__end">{{ formatDate(row.endDate) }}</div>
          </div>
        </template>

        <!-- Due Date column -->
        <template #cell-dueDate="{ row }">
          <span class="due-date">{{ formatDate(row.dueDate) }}</span>
        </template>

        <!-- Total Cost column -->
        <template #cell-totalCost="{ row }">
          <span v-if="row.totalCost" class="total-cost">
            {{ formatCurrency(row.totalCost, row.currency) }}
          </span>
          <span v-else class="total-cost--empty">—</span>
        </template>

        <!-- Total Rated Cost column -->
        <template #cell-totalRatedCost="{ row }">
          <span v-if="row.totalRatedCost" class="rated-cost">
            {{ formatCurrency(row.totalRatedCost, row.currency) }}
          </span>
          <span v-else class="rated-cost--empty">—</span>
        </template>

        <!-- Invoice column -->
        <template #cell-invoiceId="{ row }">
          <div v-if="row.invoiceId" class="invoice-info">
            <Button
              icon="pi pi-file-pdf"
              text
              rounded
              @click.stop="handleViewInvoice(row)"
              v-tooltip.top="'View invoice'"
            />
            <span class="invoice-id">{{ row.invoiceId.substring(0, 8) }}...</span>
          </div>
          <span v-else class="invoice-id--empty">—</span>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="cycle-actions">
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
              v-tooltip.top="'Edit cycle'"
              v-if="canEditCycle(row)"
            />
            <Button
              icon="pi pi-play"
              text
              rounded
              severity="info"
              @click.stop="handleProcess(row)"
              v-tooltip.top="'Process cycle'"
              v-if="row.status === 'PENDING'"
            />
            <Button
              icon="pi pi-times"
              text
              rounded
              severity="danger"
              @click.stop="handleCancel(row)"
              v-tooltip.top="'Cancel cycle'"
              v-if="canCancelCycle(row)"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-calendar empty-state__icon"></i>
            <h3 class="empty-state__title">No billing cycles found</h3>
            <p class="empty-state__description">
              {{ searchTerm || statusFilter ?
                'Try adjusting your search criteria' :
                'Get started by creating your first billing cycle'
              }}
            </p>
            <Button
              v-if="!searchTerm && !statusFilter"
              label="Create First Cycle"
              icon="pi pi-plus"
              severity="primary"
              @click="handleCreateCycle"
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
import { useBillingStore } from '~/stores/billing'
import {
  getBillingCycleStatusLabel,
  type BillingCycleStatus,
  type BillingCycle
} from '~/schemas/billing'

// Page meta
definePageMeta({
  title: 'Billing Cycles'
})

// Store
const billingStore = useBillingStore()
const { showToast } = useToast()

// Reactive state
const viewMode = ref<'timeline' | 'table'>('timeline')
const searchTerm = ref('')
const statusFilter = ref('')
const dateRangeFilter = ref<[Date, Date] | null>(null)
const sortOption = ref('startDate,desc')

// Table columns configuration
const tableColumns = [
  {
    key: 'cycleNumber',
    label: 'Cycle #',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'customerId',
    label: 'Customer',
    sortable: false,
    style: 'width: 18%'
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'startDate',
    label: 'Period',
    sortable: true,
    style: 'width: 18%'
  },
  {
    key: 'dueDate',
    label: 'Due Date',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'totalCost',
    label: 'Total Cost',
    sortable: true,
    style: 'width: 12%',
    align: 'right' as const
  },
  {
    key: 'totalRatedCost',
    label: 'Rated Cost',
    sortable: true,
    style: 'width: 12%',
    align: 'right' as const
  },
  {
    key: 'invoiceId',
    label: 'Invoice',
    sortable: false,
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
  { label: 'Scheduled', value: 'SCHEDULED' },
  { label: 'Processing', value: 'PROCESSING' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Failed', value: 'FAILED' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

const sortOptions = [
  { label: 'Newest First', value: 'startDate,desc' },
  { label: 'Oldest First', value: 'startDate,asc' },
  { label: 'Cycle Number High-Low', value: 'cycleNumber,desc' },
  { label: 'Cycle Number Low-High', value: 'cycleNumber,asc' },
  { label: 'Due Date', value: 'dueDate,asc' },
  { label: 'Status', value: 'status,asc' }
]

// Computed
const billingCycles = computed(() => billingStore.billingCycles)
const paginationProps = computed(() => ({
  page: billingStore.pagination.page,
  size: billingStore.pagination.size,
  total: billingStore.pagination.totalElements
}))

const pendingCount = computed(() =>
  billingCycles.value.filter(c => c.status === 'PENDING').length
)

const processingCount = computed(() =>
  billingCycles.value.filter(c => c.status === 'PROCESSING').length
)

const completedCount = computed(() =>
  billingCycles.value.filter(c => c.status === 'COMPLETED').length
)

const failedCount = computed(() =>
  billingCycles.value.filter(c => c.status === 'FAILED').length
)

// Methods
const handleSearch = useDebounceFn(async () => {
  await billingStore.fetchBillingCycles({
    page: 0,
    sort: sortOption.value,
    ...(statusFilter.value && { status: statusFilter.value as BillingCycleStatus }),
    ...(dateRangeFilter.value && {
      startDate: dateRangeFilter.value[0]?.toISOString(),
      endDate: dateRangeFilter.value[1]?.toISOString()
    })
  })
}, 300)

const handleStatusFilter = async () => {
  await billingStore.fetchBillingCycles({
    page: 0,
    sort: sortOption.value,
    ...(statusFilter.value && { status: statusFilter.value as BillingCycleStatus })
  })
}

const handleDateRangeFilter = async () => {
  await billingStore.fetchBillingCycles({
    page: 0,
    sort: sortOption.value,
    ...(statusFilter.value && { status: statusFilter.value as BillingCycleStatus }),
    ...(dateRangeFilter.value && {
      startDate: dateRangeFilter.value[0]?.toISOString(),
      endDate: dateRangeFilter.value[1]?.toISOString()
    })
  })
}

const handleSortChange = async () => {
  await billingStore.fetchBillingCycles({
    page: 0,
    sort: sortOption.value,
    ...(statusFilter.value && { status: statusFilter.value as BillingCycleStatus }),
    ...(dateRangeFilter.value && {
      startDate: dateRangeFilter.value[0]?.toISOString(),
      endDate: dateRangeFilter.value[1]?.toISOString()
    })
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await billingStore.fetchBillingCycles({
    page: billingStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    ...(statusFilter.value && { status: statusFilter.value as BillingCycleStatus }),
    ...(dateRangeFilter.value && {
      startDate: dateRangeFilter.value[0]?.toISOString(),
      endDate: dateRangeFilter.value[1]?.toISOString()
    })
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await billingStore.fetchBillingCycles({
    page,
    size: rows,
    sort: sortOption.value,
    ...(statusFilter.value && { status: statusFilter.value as BillingCycleStatus }),
    ...(dateRangeFilter.value && {
      startDate: dateRangeFilter.value[0]?.toISOString(),
      endDate: dateRangeFilter.value[1]?.toISOString()
    })
  })
}

const handleView = (row: BillingCycle) => {
  navigateTo(`/billing/cycles/${row.id}`)
}

const handleEdit = (row: BillingCycle) => {
  navigateTo(`/billing/cycles/${row.id}/edit`)
}

const handleProcess = async (row: BillingCycle) => {
  const confirmed = confirm(`Process billing cycle #${row.cycleNumber}?`)

  if (confirmed) {
    try {
      await billingStore.processBillingCycle(row.id)
      showToast({
        severity: 'success',
        summary: 'Cycle Processing Started',
        detail: `Billing cycle #${row.cycleNumber} is being processed`,
        life: 3000
      })

      // Refresh the list
      await billingStore.fetchBillingCycles()
    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to process billing cycle',
        life: 5000
      })
    }
  }
}

const handleCancel = async (row: BillingCycle) => {
  const confirmed = confirm(`Cancel billing cycle #${row.cycleNumber}?`)

  if (confirmed) {
    try {
      showToast({
        severity: 'info',
        summary: 'Cancel Cycle',
        detail: 'Cancel functionality will be implemented',
        life: 3000
      })
    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to cancel cycle',
        life: 5000
      })
    }
  }
}

const handleCreateCycle = () => {
  navigateTo('/billing/cycles/create')
}

const handleViewInvoice = (row: BillingCycle) => {
  if (row.invoiceId) {
    navigateTo(`/invoices/${row.invoiceId}`)
  }
}

const canEditCycle = (row: BillingCycle) => {
  return row.status === 'PENDING' || row.status === 'SCHEDULED'
}

const canCancelCycle = (row: BillingCycle) => {
  return ['PENDING', 'SCHEDULED', 'PROCESSING'].includes(row.status)
}

// Utility functions
const getStatusIcon = (status: BillingCycleStatus): string => {
  const icons: Record<BillingCycleStatus, string> = {
    PENDING: 'pi pi-clock',
    SCHEDULED: 'pi pi-calendar',
    PROCESSING: 'pi pi-spin pi-spinner',
    COMPLETED: 'pi pi-check-circle',
    FAILED: 'pi pi-times-circle',
    CANCELLED: 'pi pi-ban'
  }
  return icons[status] || 'pi pi-circle'
}

const getCustomerIdShort = (customerId: string): string => {
  return customerId.substring(0, 2).toUpperCase()
}

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const formatDateRange = (startDate: string, endDate: string): string => {
  return `${formatDate(startDate)} - ${formatDate(endDate)}`
}

const formatCurrency = (amount: number, currency: string = 'USD'): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2
  }).format(amount)
}

// Lifecycle
onMounted(async () => {
  await billingStore.fetchBillingCycles()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await billingStore.fetchBillingCycles()
})
</script>

<style scoped>
.billing-cycles-page {
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

/* View Toggle */
.view-toggle {
  display: flex;
  justify-content: flex-end;
}

.toggle-buttons {
  display: flex;
  gap: var(--space-2);
  background: var(--color-surface);
  padding: var(--space-1);
  border-radius: var(--radius-lg);
}

/* Filters */
.billing-cycles-filters {
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

/* Billing Stats */
.billing-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  gap: var(--space-3);
  align-items: center;
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card__icon--info {
  background: var(--color-info);
}

.stat-card__icon--warning {
  background: var(--color-warning);
}

.stat-card__icon--success {
  background: var(--color-success);
}

.stat-card__icon--danger {
  background: var(--color-danger);
}

.stat-card__icon i {
  font-size: 1.5rem;
}

.stat-card__content {
  flex: 1;
}

.stat-card__value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.stat-card__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* Timeline View */
.timeline-view {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.timeline-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.timeline-item {
  display: flex;
  gap: var(--space-4);
  padding: var(--space-4);
  border-left: 2px solid var(--color-border);
  margin-left: var(--space-3);
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.timeline-item:hover {
  background: var(--color-surface-elevated);
  border-radius: var(--radius-md);
}

.timeline-marker {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: absolute;
  left: -30px;
  background: var(--color-surface);
  border: 2px solid var(--color-border);
}

.timeline-marker i {
  font-size: 1.25rem;
}

.timeline-marker--pending {
  background: var(--color-warning);
  color: white;
  border-color: var(--color-warning);
}

.timeline-marker--scheduled {
  background: var(--color-info);
  color: white;
  border-color: var(--color-info);
}

.timeline-marker--processing {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.timeline-marker--completed {
  background: var(--color-success);
  color: white;
  border-color: var(--color-success);
}

.timeline-marker--failed {
  background: var(--color-danger);
  color: white;
  border-color: var(--color-danger);
}

.timeline-marker--cancelled {
  background: var(--color-text-muted);
  color: white;
  border-color: var(--color-text-muted);
}

.timeline-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-3);
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.timeline-title h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.timeline-date {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.timeline-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-3);
}

.timeline-info {
  display: flex;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
}

.info-label {
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.info-value {
  color: var(--color-text-primary);
  font-weight: var(--font-weight-semibold);
}

.timeline-actions {
  display: flex;
  gap: var(--space-2);
}

.timeline-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-12);
  text-align: center;
}

.timeline-empty-icon {
  font-size: 4rem;
  margin-bottom: var(--space-4);
  opacity: 0.5;
  color: var(--color-text-muted);
}

.timeline-empty h3 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.timeline-empty p {
  margin: 0 0 var(--space-6) 0;
  color: var(--color-text-secondary);
}

/* Table View */
.table-view {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Table Cell Styles */
.cycle-number__text {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-family: monospace;
}

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

.customer-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-family: monospace;
}

.date-range {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-size: var(--font-size-sm);
}

.date-range__separator {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.due-date {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.total-cost,
.rated-cost {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.total-cost--empty,
.rated-cost--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

.invoice-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.invoice-id {
  font-family: monospace;
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.invoice-id--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

.cycle-actions {
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
    width: 100%;
  }

  .page-header__actions .p-button {
    width: 100%;
  }

  .toggle-buttons {
    width: 100%;
  }

  .toggle-buttons .p-button {
    flex: 1;
  }

  .filters-row {
    flex-direction: column;
    gap: var(--space-3);
  }

  .filters-row .p-inputtext,
  .filters-row > * {
    width: 100%;
    min-width: unset;
  }

  .billing-stats {
    grid-template-columns: 1fr;
  }

  .timeline-details {
    grid-template-columns: 1fr;
  }

  .timeline-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .timeline-actions {
    flex-wrap: wrap;
  }
}
</style>
