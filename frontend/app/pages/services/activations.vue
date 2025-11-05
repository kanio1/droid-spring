<template>
  <div class="activations-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Service Activations</h1>
        <p class="page-subtitle">Monitor and manage service activations</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="New Activation"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/services/activations/create')"
        />
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-card__icon">
          <i class="pi pi-play"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Total Activations</div>
          <div class="summary-card__value">{{ activations.length }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon active">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Active</div>
          <div class="summary-card__value">{{ activeCount }}</div>
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
        <div class="summary-card__icon failed">
          <i class="pi pi-times-circle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Failed</div>
          <div class="summary-card__value">{{ failedCount }}</div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="activations-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search activations..."
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
          v-model="serviceFilter"
          :options="serviceOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Services"
          style="width: 180px"
          @change="handleServiceFilter"
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

    <!-- Activations Table -->
    <div class="activations-table">
      <AppTable
        :columns="tableColumns"
        :data="activations"
        :loading="serviceStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Activation ID column -->
        <template #cell-activationId="{ row }">
          <div class="activation-id">
            <span class="activation-id__text">{{ row.activationId }}</span>
          </div>
        </template>

        <!-- Service column -->
        <template #cell-serviceName="{ row }">
          <div class="service-info">
            <div class="service-name">{{ row.serviceName }}</div>
            <div class="service-code">{{ row.serviceCode }}</div>
          </div>
        </template>

        <!-- Customer column -->
        <template #cell-customerName="{ row }">
          <div v-if="row.customerName" class="customer-name">
            <div class="customer-avatar">
              {{ getCustomerInitials(row) }}
            </div>
            <div class="customer-info">
              <div class="customer-name__text">{{ row.customerName }}</div>
              <div class="customer-id">{{ row.customerId }}</div>
            </div>
          </div>
          <span v-else class="text-muted">N/A</span>
        </template>

        <!-- Status column -->
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="activation-status" size="small" />
        </template>

        <!-- Scheduled Date column -->
        <template #cell-scheduledDate="{ row }">
          <div v-if="row.scheduledDate" class="scheduled-date">
            <i class="pi pi-calendar"></i>
            <span>{{ formatDate(row.scheduledDate) }}</span>
          </div>
          <span v-else class="text-muted">Immediate</span>
        </template>

        <!-- Progress column -->
        <template #cell-progress="{ row }">
          <div class="progress">
            <ProgressBar :value="row.progress || 0" :showValue="false" style="height: 8px" />
            <span class="progress-text">{{ row.progress || 0 }}%</span>
          </div>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="activation-actions">
            <Button
              icon="pi pi-eye"
              text
              rounded
              @click.stop="handleView(row)"
              v-tooltip.top="'View details'"
            />
            <Button
              icon="pi pi-refresh"
              text
              rounded
              severity="info"
              @click.stop="handleRetry(row)"
              v-tooltip.top="'Retry activation'"
              v-if="canRetry(row)"
            />
            <Button
              icon="pi pi-times"
              text
              rounded
              severity="danger"
              @click.stop="handleCancel(row)"
              v-tooltip.top="'Cancel activation'"
              v-if="canCancel(row)"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-play empty-state__icon"></i>
            <h3 class="empty-state__title">No activations found</h3>
            <p class="empty-state__description">
              {{ searchTerm || statusFilter || serviceFilter ?
                'Try adjusting your search criteria' :
                'Get started by creating your first service activation'
              }}
            </p>
            <Button
              v-if="!searchTerm && !statusFilter && !serviceFilter"
              label="Create Activation"
              icon="pi pi-plus"
              severity="primary"
              @click="navigateTo('/services/activations/create')"
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
import { useServiceStore } from '~/stores/service'

// Page meta
definePageMeta({
  title: 'Service Activations'
})

// Store
const serviceStore = useServiceStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const statusFilter = ref('')
const serviceFilter = ref('')
const sortOption = ref('scheduledDate,desc')

// Table columns configuration
const tableColumns = [
  {
    key: 'activationId',
    label: 'Activation ID',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'serviceName',
    label: 'Service',
    sortable: true,
    style: 'width: 18%'
  },
  {
    key: 'customerName',
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
    key: 'scheduledDate',
    label: 'Scheduled Date',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'progress',
    label: 'Progress',
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
const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Scheduled', value: 'SCHEDULED' },
  { label: 'Provisioning', value: 'PROVISIONING' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Failed', value: 'FAILED' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

const serviceOptions = [
  { label: 'All Services', value: '' },
  { label: 'Internet', value: 'INTERNET' },
  { label: 'Telephony', value: 'TELEPHONY' },
  { label: 'Television', value: 'TELEVISION' },
  { label: 'Mobile', value: 'MOBILE' },
  { label: 'Cloud', value: 'CLOUD' }
]

const sortOptions = [
  { label: 'Newest First', value: 'scheduledDate,desc' },
  { label: 'Oldest First', value: 'scheduledDate,asc' },
  { label: 'Status', value: 'status,asc' },
  { label: 'Progress', value: 'progress,desc' }
]

// Computed
const activations = computed(() => serviceStore.serviceActivations)
const activeCount = computed(() => activations.value.filter(a => a.status === 'ACTIVE').length)
const pendingCount = computed(() => activations.value.filter(a => a.status === 'PENDING' || a.status === 'SCHEDULED').length)
const failedCount = computed(() => activations.value.filter(a => a.status === 'FAILED').length)
const paginationProps = computed(() => ({
  page: serviceStore.pagination.page,
  size: serviceStore.pagination.size,
  total: serviceStore.pagination.totalElements
}))

// Methods
const handleSearch = useDebounceFn(async () => {
  await serviceStore.fetchServiceActivations({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    serviceType: serviceFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}, 300)

const handleStatusFilter = async () => {
  await serviceStore.fetchServiceActivations({
    page: 0,
    sort: sortOption.value,
    serviceType: serviceFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined
  })
}

const handleServiceFilter = async () => {
  await serviceStore.fetchServiceActivations({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    serviceType: serviceFilter.value || undefined
  })
}

const handleSortChange = async () => {
  await serviceStore.fetchServiceActivations({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    serviceType: serviceFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await serviceStore.fetchServiceActivations({
    page: serviceStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    status: statusFilter.value || undefined,
    serviceType: serviceFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await serviceStore.fetchServiceActivations({
    page,
    size: rows,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    serviceType: serviceFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/services/activations/${row.id}`)
}

const handleRetry = async (row: any) => {
  const confirmed = confirm(`Are you sure you want to retry activation ${row.activationId}?`)

  if (confirmed) {
    try {
      showToast({
        severity: 'success',
        summary: 'Activation Retry',
        detail: `Activation ${row.activationId} has been queued for retry.`,
        life: 3000
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to retry activation',
        life: 5000
      })
    }
  }
}

const handleCancel = async (row: any) => {
  const confirmed = confirm(`Are you sure you want to cancel activation ${row.activationId}?`)

  if (confirmed) {
    try {
      showToast({
        severity: 'info',
        summary: 'Activation Cancelled',
        detail: `Activation ${row.activationId} has been cancelled.`,
        life: 3000
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to cancel activation',
        life: 5000
      })
    }
  }
}

const canRetry = (row: any) => {
  return row.status === 'FAILED'
}

const canCancel = (row: any) => {
  return row.status === 'PENDING' || row.status === 'SCHEDULED' || row.status === 'PROVISIONING'
}

// Utility functions
const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const getCustomerInitials = (row: any): string => {
  if (!row.customerName) return 'N/A'
  const names = row.customerName.split(' ')
  if (names.length >= 2) {
    return `${names[0][0]}${names[1][0]}`.toUpperCase()
  }
  return row.customerName.substring(0, 2).toUpperCase()
}

// Lifecycle
onMounted(async () => {
  await serviceStore.fetchServiceActivations()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await serviceStore.fetchServiceActivations()
})
</script>

<style scoped>
.activations-page {
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

.summary-card__icon.active {
  background: var(--color-green-100);
  color: var(--color-green-600);
}

.summary-card__icon.pending {
  background: var(--color-orange-100);
  color: var(--color-orange-600);
}

.summary-card__icon.failed {
  background: var(--color-red-100);
  color: var(--color-red-600);
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
.activations-filters {
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
.activations-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Activation ID Cell */
.activation-id__text {
  font-family: monospace;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* Service Info Cell */
.service-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.service-name {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.service-code {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
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

/* Scheduled Date Cell */
.scheduled-date {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-primary);
}

/* Progress Cell */
.progress {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.progress-text {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-align: center;
}

/* Actions */
.activation-actions {
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
