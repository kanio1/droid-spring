<template>
  <div class="usage-records-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Usage Records</h1>
        <p class="page-subtitle">
          View and manage customer usage data (CDRs - Call Detail Records)
        </p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Import CDR"
          icon="pi pi-upload"
          severity="secondary"
          @click="handleImportCDR"
        />
        <Button
          label="Create Record"
          icon="pi pi-plus"
          severity="primary"
          @click="handleCreateRecord"
        />
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="usage-records-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search usage records..."
            @input="handleSearch"
            style="width: 100%"
          />
        </span>

        <Dropdown
          v-model="unratedFilter"
          :options="unratedOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Records"
          style="width: 150px"
          @change="handleUnratedFilter"
        />

        <Dropdown
          v-model="usageTypeFilter"
          :options="usageTypeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Types"
          style="width: 150px"
          @change="handleUsageTypeFilter"
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

    <!-- Usage Summary Stats -->
    <div class="usage-stats">
      <div class="stat-card">
        <div class="stat-card__icon">
          <i class="pi pi-database"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ paginationProps.total }}</div>
          <div class="stat-card__label">Total Records</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--warning">
          <i class="pi pi-clock"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ unratedCount }}</div>
          <div class="stat-card__label">Unrated</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--success">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ ratedCount }}</div>
          <div class="stat-card__label">Rated</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--info">
          <i class="pi pi-dollar"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ formatCurrency(totalCost) }}</div>
          <div class="stat-card__label">Total Cost</div>
        </div>
      </div>
    </div>

    <!-- Usage Records Table -->
    <div class="usage-records-table">
      <AppTable
        :columns="tableColumns"
        :data="usageRecords"
        :loading="billingStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Record ID column -->
        <template #cell-id="{ row }">
          <div class="record-id">
            <span class="record-id__text">{{ row.id.substring(0, 8) }}...</span>
          </div>
        </template>

        <!-- Usage Type column -->
        <template #cell-usageType="{ row }">
          <StatusBadge :status="row.usageType" type="usage-type" size="small" />
        </template>

        <!-- Usage Amount column -->
        <template #cell-usageAmount="{ row }">
          <div class="usage-amount">
            <span class="usage-amount__value">
              {{ formatUsageAmount(row.usageAmount, row.unit) }}
            </span>
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

        <!-- Rating Status column -->
        <template #cell-isRated="{ row }">
          <StatusBadge :status="row.isRated ? 'RATED' : 'PENDING'" type="rating" size="small" />
        </template>

        <!-- Cost column -->
        <template #cell-cost="{ row }">
          <span v-if="row.cost !== null && row.cost !== undefined" class="cost">
            {{ formatCurrency(row.cost, row.currency) }}
          </span>
          <span v-else class="cost--empty">—</span>
        </template>

        <!-- Source column -->
        <template #cell-source="{ row }">
          <span v-if="row.source" class="source">
            {{ row.source }}
          </span>
          <span v-else class="source--empty">—</span>
        </template>

        <!-- Destination column -->
        <template #cell-destination="{ row }">
          <span v-if="row.destination" class="destination">
            {{ row.destination }}
          </span>
          <span v-else class="destination--empty">—</span>
        </template>

        <!-- Timestamp column -->
        <template #cell-timestamp="{ row }">
          <span v-if="row.timestamp" class="timestamp">
            {{ formatDateTime(row.timestamp) }}
          </span>
          <span v-else class="timestamp--empty">—</span>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="record-actions">
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
              v-tooltip.top="'Edit record'"
              v-if="canEditRecord(row)"
            />
            <Button
              icon="pi pi-star"
              text
              rounded
              severity="warning"
              @click.stop="handleRate(row)"
              v-tooltip.top="'Mark as rated'"
              v-if="!row.isRated"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-database empty-state__icon"></i>
            <h3 class="empty-state__title">No usage records found</h3>
            <p class="empty-state__description">
              {{ searchTerm || unratedFilter || usageTypeFilter || dateRangeFilter ?
                'Try adjusting your search criteria' :
                'Get started by importing CDR files or creating usage records'
              }}
            </p>
            <Button
              v-if="!searchTerm && !unratedFilter && !usageTypeFilter && !dateRangeFilter"
              label="Import CDR Files"
              icon="pi pi-upload"
              severity="primary"
              @click="handleImportCDR"
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
  formatUsageAmount,
  calculateTotalCost,
  getUnratedUsageCount,
  getRatedUsageCount,
  getUsageTypeLabel,
  type UsageType,
  type UsageRecord
} from '~/schemas/billing'

// Page meta
definePageMeta({
  title: 'Usage Records'
})

// Store
const billingStore = useBillingStore()
const { showToast } = useToast()

// Reactive state
const searchTerm = ref('')
const unratedFilter = ref('')
const usageTypeFilter = ref('')
const dateRangeFilter = ref<[Date, Date] | null>(null)
const sortOption = ref('timestamp,desc')

// Table columns configuration
const tableColumns = [
  {
    key: 'id',
    label: 'Record ID',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'usageType',
    label: 'Type',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'usageAmount',
    label: 'Usage',
    sortable: true,
    style: 'width: 12%',
    align: 'right'
  },
  {
    key: 'customerId',
    label: 'Customer',
    sortable: false,
    style: 'width: 15%'
  },
  {
    key: 'isRated',
    label: 'Rating',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'cost',
    label: 'Cost',
    sortable: true,
    style: 'width: 12%',
    align: 'right'
  },
  {
    key: 'source',
    label: 'Source',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'destination',
    label: 'Destination',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'timestamp',
    label: 'Timestamp',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 10%'
  }
]

// Filter options
const unratedOptions = [
  { label: 'All Records', value: '' },
  { label: 'Unrated Only', value: 'true' },
  { label: 'Rated Only', value: 'false' }
]

const usageTypeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Voice', value: 'VOICE' },
  { label: 'SMS', value: 'SMS' },
  { label: 'Data', value: 'DATA' },
  { label: 'Service', value: 'SERVICE' }
]

const sortOptions = [
  { label: 'Newest First', value: 'timestamp,desc' },
  { label: 'Oldest First', value: 'timestamp,asc' },
  { label: 'Highest Usage', value: 'usageAmount,desc' },
  { label: 'Lowest Usage', value: 'usageAmount,asc' },
  { label: 'Cost High-Low', value: 'cost,desc' },
  { label: 'Cost Low-High', value: 'cost,asc' }
]

// Computed
const usageRecords = computed(() => billingStore.usageRecords)
const paginationProps = computed(() => ({
  page: billingStore.pagination.page,
  size: billingStore.pagination.size,
  total: billingStore.pagination.totalElements
}))

const unratedCount = computed(() => getUnratedUsageCount(usageRecords.value))
const ratedCount = computed(() => getRatedUsageCount(usageRecords.value))
const totalCost = computed(() => calculateTotalCost(usageRecords.value))

// Methods
const handleSearch = useDebounceFn(async () => {
  await billingStore.fetchUsageRecords({
    page: 0,
    sort: sortOption.value,
    ...(unratedFilter.value && { unrated: unratedFilter.value === 'true' }),
    ...(usageTypeFilter.value && { usageType: usageTypeFilter.value as UsageType }),
    ...(dateRangeFilter.value && {
      startDate: dateRangeFilter.value[0]?.toISOString(),
      endDate: dateRangeFilter.value[1]?.toISOString()
    })
  })
}, 300)

const handleUnratedFilter = async () => {
  await billingStore.fetchUsageRecords({
    page: 0,
    sort: sortOption.value,
    ...(unratedFilter.value && { unrated: unratedFilter.value === 'true' }),
    ...(usageTypeFilter.value && { usageType: usageTypeFilter.value as UsageType })
  })
}

const handleUsageTypeFilter = async () => {
  await billingStore.fetchUsageRecords({
    page: 0,
    sort: sortOption.value,
    ...(unratedFilter.value && { unrated: unratedFilter.value === 'true' }),
    ...(usageTypeFilter.value && { usageType: usageTypeFilter.value as UsageType })
  })
}

const handleDateRangeFilter = async () => {
  await billingStore.fetchUsageRecords({
    page: 0,
    sort: sortOption.value,
    ...(unratedFilter.value && { unrated: unratedFilter.value === 'true' }),
    ...(usageTypeFilter.value && { usageType: usageTypeFilter.value as UsageType }),
    ...(dateRangeFilter.value && {
      startDate: dateRangeFilter.value[0]?.toISOString(),
      endDate: dateRangeFilter.value[1]?.toISOString()
    })
  })
}

const handleSortChange = async () => {
  await billingStore.fetchUsageRecords({
    page: 0,
    sort: sortOption.value,
    ...(unratedFilter.value && { unrated: unratedFilter.value === 'true' }),
    ...(usageTypeFilter.value && { usageType: usageTypeFilter.value as UsageType }),
    ...(dateRangeFilter.value && {
      startDate: dateRangeFilter.value[0]?.toISOString(),
      endDate: dateRangeFilter.value[1]?.toISOString()
    })
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await billingStore.fetchUsageRecords({
    page: billingStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    ...(unratedFilter.value && { unrated: unratedFilter.value === 'true' }),
    ...(usageTypeFilter.value && { usageType: usageTypeFilter.value as UsageType }),
    ...(dateRangeFilter.value && {
      startDate: dateRangeFilter.value[0]?.toISOString(),
      endDate: dateRangeFilter.value[1]?.toISOString()
    })
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await billingStore.fetchUsageRecords({
    page,
    size: rows,
    sort: sortOption.value,
    ...(unratedFilter.value && { unrated: unratedFilter.value === 'true' }),
    ...(usageTypeFilter.value && { usageType: usageTypeFilter.value as UsageType }),
    ...(dateRangeFilter.value && {
      startDate: dateRangeFilter.value[0]?.toISOString(),
      endDate: dateRangeFilter.value[1]?.toISOString()
    })
  })
}

const handleView = (row: UsageRecord) => {
  navigateTo(`/billing/usage-records/${row.id}`)
}

const handleEdit = (row: UsageRecord) => {
  navigateTo(`/billing/usage-records/${row.id}/edit`)
}

const handleRate = async (row: UsageRecord) => {
  try {
    // TODO: Implement rating logic
    showToast({
      severity: 'info',
      summary: 'Rate Record',
      detail: 'Rating functionality will be implemented',
      life: 3000
    })
  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to rate record',
      life: 5000
    })
  }
}

const handleImportCDR = () => {
  navigateTo('/billing/usage-records/import')
}

const handleCreateRecord = () => {
  navigateTo('/billing/usage-records/create')
}

const canEditRecord = (row: UsageRecord) => {
  return !row.isRated
}

// Utility functions
const getCustomerIdShort = (customerId: string): string => {
  return customerId.substring(0, 2).toUpperCase()
}

const formatDateTime = (dateString: string): string => {
  return new Date(dateString).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
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
  await billingStore.fetchUsageRecords()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await billingStore.fetchUsageRecords()
})
</script>

<style scoped>
.usage-records-page {
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
  display: flex;
  gap: var(--space-3);
}

/* Filters */
.usage-records-filters {
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

/* Usage Stats */
.usage-stats {
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

.stat-card__icon--warning {
  background: var(--color-warning);
}

.stat-card__icon--success {
  background: var(--color-success);
}

.stat-card__icon--info {
  background: var(--color-info);
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

/* Table */
.usage-records-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Record ID Cell */
.record-id__text {
  font-family: monospace;
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

/* Customer Cell */
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

/* Usage Amount Cell */
.usage-amount__value {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* Cost Cell */
.cost {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.cost--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Source & Destination Cells */
.source,
.destination {
  color: var(--color-text-primary);
}

.source--empty,
.destination--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Timestamp Cell */
.timestamp {
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
}

.timestamp--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Actions */
.record-actions {
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
    flex-direction: column;
    width: 100%;
  }

  .page-header__actions .p-button {
    width: 100%;
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

  .usage-stats {
    grid-template-columns: 1fr;
  }

  .stat-card {
    padding: var(--space-3);
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
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .filters-row {
    gap: var(--space-2);
  }

  .customer-name {
    gap: var(--space-2);
  }

  .customer-avatar {
    width: 36px;
    height: 36px;
  }
}
</style>
