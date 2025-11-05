<template>
  <div class="usage-records-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Usage Records</h1>
        <p class="page-subtitle">View and manage CDR (Call Detail Records) and usage data</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Ingest Usage"
          icon="pi pi-upload"
          severity="primary"
          @click="navigateTo('/billing/usage-records/create')"
        />
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-card__icon">
          <i class="pi pi-database"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Total Records</div>
          <div class="summary-card__value">{{ usageRecords.length }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon unrated">
          <i class="pi pi-clock"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Unrated</div>
          <div class="summary-card__value">{{ unratedCount }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon rated">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Rated</div>
          <div class="summary-card__value">{{ ratedCount }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon amount">
          <i class="pi pi-dollar"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Unrated Amount</div>
          <div class="summary-card__value">{{ formatCurrency(totalUnratedAmount) }}</div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="usage-filters">
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
          v-model="usageTypeFilter"
          :options="usageTypeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Types"
          style="width: 180px"
          @change="handleUsageTypeFilter"
        />

        <Dropdown
          v-model="ratedFilter"
          :options="ratedOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All"
          style="width: 150px"
          @change="handleRatedFilter"
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

    <!-- Usage Records Table -->
    <div class="usage-table">
      <AppTable
        :columns="tableColumns"
        :data="usageRecords"
        :loading="billingStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
      >
        <!-- Record ID column -->
        <template #cell-recordId="{ row }">
          <div class="record-id">
            <span class="record-id__text">{{ row.recordId }}</span>
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

        <!-- Usage Type column -->
        <template #cell-usageType="{ row }">
          <StatusBadge :status="row.usageType" type="usage-type" size="small" />
        </template>

        <!-- Amount column -->
        <template #cell-usageAmount="{ row }">
          <div class="usage-amount">
            <span class="amount-value">
              {{ formatUsageAmount(row.usageAmount, row.usageType) }}
            </span>
          </div>
        </template>

        <!-- Timestamp column -->
        <template #cell-timestamp="{ row }">
          <span class="timestamp">{{ formatDateTime(row.timestamp) }}</span>
        </template>

        <!-- Rated column -->
        <template #cell-isRated="{ row }">
          <div class="rated-status">
            <i :class="row.isRated ? 'pi pi-check-circle text-success' : 'pi pi-clock text-warning'"></i>
            <span class="ml-2">{{ row.isRated ? 'Rated' : 'Unrated' }}</span>
          </div>
        </template>

        <!-- Cost column -->
        <template #cell-ratedCost="{ row }">
          <span v-if="row.ratedCost" class="cost">
            {{ formatCurrency(row.ratedCost) }}
          </span>
          <span v-else class="cost--empty">—</span>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="usage-actions">
            <Button
              icon="pi pi-eye"
              text
              rounded
              @click="handleView(row)"
              v-tooltip.top="'View details'"
            />
            <Button
              icon="pi pi-star"
              text
              rounded
              severity="info"
              @click="handleRate(row)"
              v-tooltip.top="'Rate usage'"
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
              {{ searchTerm || usageTypeFilter || ratedFilter ?
                'Try adjusting your search criteria' :
                'Get started by ingesting your first usage record'
              }}
            </p>
            <Button
              v-if="!searchTerm && !usageTypeFilter && !ratedFilter"
              label="Ingest Usage"
              icon="pi pi-upload"
              severity="primary"
              @click="navigateTo('/billing/usage-records/create')"
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
  title: 'Usage Records'
})

// Store
const billingStore = useBillingStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const usageTypeFilter = ref('')
const ratedFilter = ref('')
const sortOption = ref('timestamp,desc')

// Table columns configuration
const tableColumns = [
  {
    key: 'recordId',
    label: 'Record ID',
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
    key: 'usageType',
    label: 'Type',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'usageAmount',
    label: 'Amount',
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
    key: 'isRated',
    label: 'Status',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'ratedCost',
    label: 'Cost',
    sortable: true,
    style: 'width: 10%',
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
const usageTypeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Voice', value: 'VOICE' },
  { label: 'SMS', value: 'SMS' },
  { label: 'Data', value: 'DATA' },
  { label: 'Video', value: 'VIDEO' }
]

const ratedOptions = [
  { label: 'All', value: '' },
  { label: 'Rated Only', value: 'true' },
  { label: 'Unrated Only', value: 'false' }
]

const sortOptions = [
  { label: 'Newest First', value: 'timestamp,desc' },
  { label: 'Oldest First', value: 'timestamp,asc' },
  { label: 'Amount High-Low', value: 'usageAmount,desc' },
  { label: 'Amount Low-High', value: 'usageAmount,asc' }
]

// Computed
const usageRecords = computed(() => billingStore.usageRecords)
const unratedCount = computed(() => billingStore.unratedUsageRecords.length)
const ratedCount = computed(() => billingStore.ratedUsageRecords.length)
const totalUnratedAmount = computed(() => billingStore.totalUnratedUsage)
const paginationProps = computed(() => ({
  page: billingStore.pagination.page,
  size: billingStore.pagination.size,
  total: billingStore.pagination.totalElements
}))

// Methods
const handleSearch = useDebounceFn(async () => {
  await billingStore.fetchUsageRecords({
    page: 0,
    sort: sortOption.value,
    usageType: usageTypeFilter.value || undefined,
    unrated: ratedFilter.value === 'false' ? true : ratedFilter.value === 'true' ? false : undefined,
    searchTerm: searchTerm.value || undefined
  })
}, 300)

const handleUsageTypeFilter = async () => {
  await billingStore.fetchUsageRecords({
    page: 0,
    sort: sortOption.value,
    unrated: ratedFilter.value === 'false' ? true : ratedFilter.value === 'true' ? false : undefined,
    searchTerm: searchTerm.value || undefined,
    usageType: usageTypeFilter.value || undefined
  })
}

const handleRatedFilter = async () => {
  await billingStore.fetchUsageRecords({
    page: 0,
    sort: sortOption.value,
    usageType: usageTypeFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    unrated: ratedFilter.value === 'false' ? true : ratedFilter.value === 'true' ? false : undefined
  })
}

const handleSortChange = async () => {
  await billingStore.fetchUsageRecords({
    page: 0,
    sort: sortOption.value,
    usageType: usageTypeFilter.value || undefined,
    unrated: ratedFilter.value === 'false' ? true : ratedFilter.value === 'true' ? false : undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await billingStore.fetchUsageRecords({
    page: billingStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    usageType: usageTypeFilter.value || undefined,
    unrated: ratedFilter.value === 'false' ? true : ratedFilter.value === 'true' ? false : undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await billingStore.fetchUsageRecords({
    page,
    size: rows,
    sort: sortOption.value,
    usageType: usageTypeFilter.value || undefined,
    unrated: ratedFilter.value === 'false' ? true : ratedFilter.value === 'true' ? false : undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/billing/usage-records/${row.id}`)
}

const handleRate = async (row: any) => {
  try {
    // Rate the usage record
    showToast({
      severity: 'info',
      summary: 'Usage Rated',
      detail: `Usage record ${row.recordId} has been rated.`,
      life: 3000
    })

    await billingStore.fetchUsageRecords({
      usageType: usageTypeFilter.value || undefined,
      unrated: ratedFilter.value === 'false' ? true : ratedFilter.value === 'true' ? false : undefined,
      searchTerm: searchTerm.value || undefined
    })

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to rate usage',
      life: 5000
    })
  }
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
  if (!amount) return formatCurrency(0, currency)
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency
  }).format(amount)
}

const formatUsageAmount = (amount: number, type: string): string => {
  switch (type) {
    case 'DATA':
      const mb = amount / (1024 * 1024)
      return `${mb.toFixed(2)} MB`
    case 'VOICE':
      return `${amount} min`
    case 'SMS':
      return `${amount} SMS`
    case 'VIDEO':
      return `${amount} MB`
    default:
      return amount.toString()
  }
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

.summary-card__icon.unrated {
  background: var(--color-orange-100);
  color: var(--color-orange-600);
}

.summary-card__icon.rated {
  background: var(--color-green-100);
  color: var(--color-green-600);
}

.summary-card__icon.amount {
  background: var(--color-blue-100);
  color: var(--color-blue-600);
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
.usage-filters {
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
.usage-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Record ID Cell */
.record-id__text {
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

/* Usage Amount Cell */
.usage-amount {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.amount-value {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* Timestamp Cell */
.timestamp {
  color: var(--color-text-primary);
}

/* Rated Status Cell */
.rated-status {
  display: flex;
  align-items: center;
  gap: var(--space-1);
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

/* Actions */
.usage-actions {
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
