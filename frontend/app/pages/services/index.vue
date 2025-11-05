<template>
  <div class="services-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Services</h1>
        <p class="page-subtitle">Manage service catalog and activations</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="View Activations"
          icon="pi pi-list"
          severity="secondary"
          @click="navigateTo('/services/activations')"
        />
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-card__icon">
          <i class="pi pi-cog"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Total Services</div>
          <div class="summary-card__value">{{ services.length }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon active">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Active Services</div>
          <div class="summary-card__value">{{ activeCount }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon activation">
          <i class="pi pi-play"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Activations</div>
          <div class="summary-card__value">{{ totalActivations }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon pending">
          <i class="pi pi-clock"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Pending</div>
          <div class="summary-card__value">{{ pendingActivations }}</div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="services-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search services..."
            @input="handleSearch"
            style="width: 100%"
          />
        </span>

        <Dropdown
          v-model="serviceTypeFilter"
          :options="serviceTypeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Types"
          style="width: 180px"
          @change="handleServiceTypeFilter"
        />

        <Dropdown
          v-model="categoryFilter"
          :options="categoryOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Categories"
          style="width: 180px"
          @change="handleCategoryFilter"
        />

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

    <!-- Services Table -->
    <div class="services-table">
      <AppTable
        :columns="tableColumns"
        :data="services"
        :loading="serviceStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Service Code column -->
        <template #cell-serviceCode="{ row }">
          <div class="service-code">
            <span class="service-code__text">{{ row.serviceCode }}</span>
          </div>
        </template>

        <!-- Service Name column -->
        <template #cell-serviceName="{ row }">
          <div class="service-name">
            <div class="service-name__text">{{ row.serviceName }}</div>
            <div class="service-type">{{ formatServiceType(row.serviceType) }}</div>
          </div>
        </template>

        <!-- Category column -->
        <template #cell-category="{ row }">
          <div class="category">
            <span>{{ formatCategory(row.category) }}</span>
          </div>
        </template>

        <!-- Status column -->
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="service" size="small" />
        </template>

        <!-- Provisioning Time column -->
        <template #cell-provisioningTime="{ row }">
          <div class="provisioning-time">
            <i class="pi pi-clock"></i>
            <span>{{ row.provisioningTime }} min</span>
          </div>
        </template>

        <!-- Activations column -->
        <template #cell-activations="{ row }">
          <div class="activations">
            <div class="activations-count">{{ row.activations || 0 }}</div>
            <div class="activations-label">activations</div>
          </div>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="service-actions">
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
              severity="success"
              @click.stop="handleActivate(row)"
              v-tooltip.top="'Activate service'"
              v-if="canActivate(row)"
            />
            <Button
              icon="pi pi-cog"
              text
              rounded
              severity="info"
              @click.stop="handleConfigure(row)"
              v-tooltip.top="'Configure'"
              v-if="canConfigure(row)"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-cog empty-state__icon"></i>
            <h3 class="empty-state__title">No services found</h3>
            <p class="empty-state__description">
              {{ searchTerm || serviceTypeFilter || categoryFilter || statusFilter ?
                'Try adjusting your search criteria' :
                'No services are available at this time'
              }}
            </p>
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
  title: 'Services'
})

// Store
const serviceStore = useServiceStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const serviceTypeFilter = ref('')
const categoryFilter = ref('')
const statusFilter = ref('')
const sortOption = ref('serviceName,asc')

// Table columns configuration
const tableColumns = [
  {
    key: 'serviceCode',
    label: 'Code',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'serviceName',
    label: 'Service Name',
    sortable: true,
    style: 'width: 25%'
  },
  {
    key: 'category',
    label: 'Category',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'provisioningTime',
    label: 'Provisioning Time',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'activations',
    label: 'Activations',
    sortable: true,
    style: 'width: 10%',
    align: 'center'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 11%'
  }
]

// Filter options
const serviceTypeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Internet', value: 'INTERNET' },
  { label: 'Telephony', value: 'TELEPHONY' },
  { label: 'Television', value: 'TELEVISION' },
  { label: 'Mobile', value: 'MOBILE' },
  { label: 'Cloud', value: 'CLOUD' }
]

const categoryOptions = [
  { label: 'All Categories', value: '' },
  { label: 'Connectivity', value: 'CONNECTIVITY' },
  { label: 'Communication', value: 'COMMUNICATION' },
  { label: 'Entertainment', value: 'ENTERTAINMENT' },
  { label: 'Cloud Services', value: 'CLOUD_SERVICES' }
]

const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Deprecated', value: 'DEPRECATED' },
  { label: 'Planned', value: 'PLANNED' }
]

const sortOptions = [
  { label: 'Name A-Z', value: 'serviceName,asc' },
  { label: 'Name Z-A', value: 'serviceName,desc' },
  { label: 'Category', value: 'category,asc' },
  { label: 'Provisioning Time', value: 'provisioningTime,asc' }
]

// Computed
const services = computed(() => serviceStore.services)
const activeCount = computed(() => services.value.filter(s => s.status === 'ACTIVE').length)
const totalActivations = computed(() => serviceStore.totalActivations)
const pendingActivations = computed(() => serviceStore.pendingActivations.length)
const paginationProps = computed(() => ({
  page: serviceStore.pagination.page,
  size: serviceStore.pagination.size,
  total: serviceStore.pagination.totalElements
}))

// Methods
const handleSearch = useDebounceFn(async () => {
  await serviceStore.fetchServices({
    page: 0,
    sort: sortOption.value,
    serviceType: serviceTypeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}, 300)

const handleServiceTypeFilter = async () => {
  await serviceStore.fetchServices({
    page: 0,
    sort: sortOption.value,
    category: categoryFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    serviceType: serviceTypeFilter.value || undefined
  })
}

const handleCategoryFilter = async () => {
  await serviceStore.fetchServices({
    page: 0,
    sort: sortOption.value,
    serviceType: serviceTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    category: categoryFilter.value || undefined
  })
}

const handleStatusFilter = async () => {
  await serviceStore.fetchServices({
    page: 0,
    sort: sortOption.value,
    serviceType: serviceTypeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined
  })
}

const handleSortChange = async () => {
  await serviceStore.fetchServices({
    page: 0,
    sort: sortOption.value,
    serviceType: serviceTypeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await serviceStore.fetchServices({
    page: serviceStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    serviceType: serviceTypeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await serviceStore.fetchServices({
    page,
    size: rows,
    sort: sortOption.value,
    serviceType: serviceTypeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/services/${row.id}`)
}

const handleActivate = async (row: any) => {
  try {
    showToast({
      severity: 'success',
      summary: 'Service Activation',
      detail: `Service ${row.serviceName} activation initiated.`,
      life: 3000
    })

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to initiate activation',
      life: 5000
    })
  }
}

const handleConfigure = async (row: any) => {
  showToast({
    severity: 'info',
    summary: 'Configuration',
    detail: `Service configuration for ${row.serviceName}.`,
    life: 3000
  })
}

const canActivate = (row: any) => {
  return row.status === 'ACTIVE'
}

const canConfigure = (row: any) => {
  return row.status === 'ACTIVE' || row.status === 'INACTIVE'
}

// Utility functions
const formatServiceType = (type: string): string => {
  const types: Record<string, string> = {
    INTERNET: 'Internet',
    TELEPHONY: 'Telephony',
    TELEVISION: 'Television',
    MOBILE: 'Mobile',
    CLOUD: 'Cloud'
  }
  return types[type] || type
}

const formatCategory = (category: string): string => {
  const categories: Record<string, string> = {
    CONNECTIVITY: 'Connectivity',
    COMMUNICATION: 'Communication',
    ENTERTAINMENT: 'Entertainment',
    CLOUD_SERVICES: 'Cloud Services'
  }
  return categories[category] || category
}

// Lifecycle
onMounted(async () => {
  await serviceStore.fetchServices()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await serviceStore.fetchServices()
})
</script>

<style scoped>
.services-page {
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

.summary-card__icon.activation {
  background: var(--color-blue-100);
  color: var(--color-blue-600);
}

.summary-card__icon.pending {
  background: var(--color-orange-100);
  color: var(--color-orange-600);
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
.services-filters {
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
.services-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Service Code Cell */
.service-code__text {
  font-family: monospace;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* Service Name Cell */
.service-name {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.service-name__text {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.service-type {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Category Cell */
.category {
  color: var(--color-text-primary);
}

/* Provisioning Time Cell */
.provisioning-time {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-primary);
}

/* Activations Cell */
.activations {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
}

.activations-count {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.activations-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Actions */
.service-actions {
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
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .summary-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
