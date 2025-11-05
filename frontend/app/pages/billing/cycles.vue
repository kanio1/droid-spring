<template>
  <div class="billing-cycles-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Billing Cycles</h1>
        <p class="page-subtitle">Manage billing cycles and invoice generation</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Start New Cycle"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/billing/cycles/create')"
        />
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-card__icon">
          <i class="pi pi-calendar"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Total Cycles</div>
          <div class="summary-card__value">{{ billingCycles.length }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon pending">
          <i class="pi pi-clock"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Pending</div>
          <div class="summary-card__value">{{ pendingCount }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon processing">
          <i class="pi pi-spinner"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Processing</div>
          <div class="summary-card__value">{{ processingCount }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon completed">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Completed</div>
          <div class="summary-card__value">{{ completedCount }}</div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="cycles-filters">
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

    <!-- Billing Cycles Table -->
    <div class="cycles-table">
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
        <!-- Cycle Name column -->
        <template #cell-cycleName="{ row }">
          <div class="cycle-name">
            <span class="cycle-name__text">{{ row.cycleName }}</span>
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

        <!-- Period column -->
        <template #cell-period="{ row }">
          <div class="period">
            <div class="period-dates">
              <div>{{ formatDate(row.startDate) }}</div>
              <div class="period-separator">to</div>
              <div>{{ formatDate(row.endDate) }}</div>
            </div>
          </div>
        </template>

        <!-- Status column -->
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="billing-cycle" size="small" />
        </template>

        <!-- Usage Records column -->
        <template #cell-usageRecordCount="{ row }">
          <span class="usage-count">
            {{ row.usageRecordCount || 0 }}
          </span>
        </template>

        <!-- Invoices column -->
        <template #cell-invoiceCount="{ row }">
          <span class="invoice-count">
            {{ row.invoiceCount || 0 }}
          </span>
        </template>

        <!-- Total Amount column -->
        <template #cell-totalAmount="{ row }">
          <span v-if="row.totalAmount" class="amount">
            {{ formatCurrency(row.totalAmount, row.currency) }}
          </span>
          <span v-else class="amount--empty">—</span>
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
              icon="pi pi-play"
              text
              rounded
              severity="info"
              @click.stop="handleProcess(row)"
              v-tooltip.top="'Process cycle'"
              v-if="canProcess(row)"
            />
            <Button
              icon="pi pi-file"
              text
              rounded
              @click.stop="handleViewInvoices(row)"
              v-tooltip.top="'View invoices'"
              v-if="row.invoiceCount > 0"
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
              label="Start First Cycle"
              icon="pi pi-plus"
              severity="primary"
              @click="navigateTo('/billing/cycles/create')"
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

// Page meta
definePageMeta({
  title: 'Billing Cycles'
})

// Store
const billingStore = useBillingStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const statusFilter = ref('')
const sortOption = ref('startDate,desc')

// Table columns configuration
const tableColumns = [
  {
    key: 'cycleName',
    label: 'Cycle Name',
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
    key: 'period',
    label: 'Period',
    sortable: true,
    style: 'width: 20%'
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'usageRecordCount',
    label: 'Usage Records',
    sortable: true,
    style: 'width: 12%',
    align: 'center'
  },
  {
    key: 'invoiceCount',
    label: 'Invoices',
    sortable: true,
    style: 'width: 10%',
    align: 'center'
  },
  {
    key: 'totalAmount',
    label: 'Total Amount',
    sortable: true,
    style: 'width: 12%',
    align: 'right'
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
  { label: 'Completed', value: 'COMPLETED' }
]

const sortOptions = [
  { label: 'Newest First', value: 'startDate,desc' },
  { label: 'Oldest First', value: 'startDate,asc' },
  { label: 'Amount High-Low', value: 'totalAmount,desc' },
  { label: 'Amount Low-High', value: 'totalAmount,asc' }
]

// Computed
const billingCycles = computed(() => billingStore.billingCycles)
const pendingCount = computed(() => billingStore.pendingBillingCycles.length)
const processingCount = computed(() => billingStore.processingBillingCycles.length)
const completedCount = computed(() => billingStore.completedBillingCycles.length)
const paginationProps = computed(() => ({
  page: billingStore.pagination.page,
  size: billingStore.pagination.size,
  total: billingStore.pagination.totalElements
}))

// Methods
const handleSearch = useDebounceFn(async () => {
  await billingStore.fetchBillingCycles({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}, 300)

const handleStatusFilter = async () => {
  await billingStore.fetchBillingCycles({
    page: 0,
    sort: sortOption.value,
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined
  })
}

const handleSortChange = async () => {
  await billingStore.fetchBillingCycles({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await billingStore.fetchBillingCycles({
    page: billingStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await billingStore.fetchBillingCycles({
    page,
    size: rows,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/billing/cycles/${row.id}`)
}

const handleViewInvoices = (row: any) => {
  navigateTo(`/invoices?cycleId=${row.id}`)
}

const handleProcess = async (row: any) => {
  const confirmed = confirm(`Are you sure you want to process billing cycle ${row.cycleName}?`)

  if (confirmed) {
    try {
      await billingStore.processBillingCycle(row.id)

      showToast({
        severity: 'success',
        summary: 'Cycle Processing',
        detail: `Billing cycle ${row.cycleName} is being processed.`,
        life: 3000
      })

      await billingStore.fetchBillingCycles({
        status: statusFilter.value || undefined,
        searchTerm: searchTerm.value || undefined
      })

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

const canProcess = (row: any) => {
  return row.status === 'PENDING' || row.status === 'PROCESSING'
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

const formatCurrency = (amount: number, currency: string = 'USD'): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency
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

/* Summary Cards */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.summary-card {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  align-items: center;
}

.summary-card__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  background: var(--color-info-100);
  color: var(--color-info-600);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  flex-shrink: 0;
}

.summary-card__icon.pending {
  background: var(--color-orange-100);
  color: var(--color-orange-600);
}

.summary-card__icon.processing {
  background: var(--color-blue-100);
  color: var(--color-blue-600);
}

.summary-card__icon.completed {
  background: var(--color-green-100);
  color: var(--color-green-600);
}

.summary-card__content {
  flex: 1;
}

.summary-card__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-1);
}

.summary-card__value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

/* Filters */
.cycles-filters {
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
.cycles-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Cycle Name Cell */
.cycle-name__text {
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

/* Period Cell */
.period {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.period-dates {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  color: var(--color-text-primary);
}

.period-separator {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Usage Count and Invoice Count */
.usage-count,
.invoice-count {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
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

/* Actions */
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
    align-self: flex-start;
  }

  .summary-cards {
    grid-template-columns: 1fr;
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
