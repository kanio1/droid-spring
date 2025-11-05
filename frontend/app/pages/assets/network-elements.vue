<template>
  <div class="network-elements-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Network Elements</h1>
        <p class="page-subtitle">Manage network infrastructure equipment</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Create Element"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/assets/create?type=NETWORK_ELEMENT')"
        />
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-card__icon">
          <i class="pi pi-sitemap"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Total Elements</div>
          <div class="summary-card__value">{{ networkElements.length }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon active">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Online</div>
          <div class="summary-card__value">{{ onlineCount }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon warning">
          <i class="pi pi-exclamation-triangle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Warning</div>
          <div class="summary-card__value">{{ warningCount }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon offline">
          <i class="pi pi-times-circle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Offline</div>
          <div class="summary-card__value">{{ offlineCount }}</div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="elements-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search network elements..."
            @input="handleSearch"
            style="width: 100%"
          />
        </span>

        <Dropdown
          v-model="elementTypeFilter"
          :options="elementTypeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Types"
          style="width: 180px"
          @change="handleElementTypeFilter"
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
          v-model="locationFilter"
          :options="locationOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Locations"
          style="width: 180px"
          @change="handleLocationFilter"
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

    <!-- Network Elements Table -->
    <div class="elements-table">
      <AppTable
        :columns="tableColumns"
        :data="networkElements"
        :loading="assetStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Element ID column -->
        <template #cell-assetId="{ row }">
          <div class="element-id">
            <span class="element-id__text">{{ row.assetId }}</span>
          </div>
        </template>

        <!-- Name column -->
        <template #cell-name="{ row }">
          <div class="element-name">
            <i :class="getElementIcon(row.subType)" class="element-icon"></i>
            <div class="element-info">
              <div class="element-name__text">{{ row.name }}</div>
              <div class="element-subtype">{{ formatElementSubType(row.subType) }}</div>
            </div>
          </div>
        </template>

        <!-- Location column -->
        <template #cell-location="{ row }">
          <div class="location">
            <i class="pi pi-map-marker"></i>
            <span>{{ row.location || 'N/A' }}</span>
          </div>
        </template>

        <!-- IP Address column -->
        <template #cell-ipAddress="{ row }">
          <div v-if="row.ipAddress" class="ip-address">
            <code>{{ row.ipAddress }}</code>
          </div>
          <span v-else class="text-muted">—</span>
        </template>

        <!-- Status column -->
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="network-element" size="small" />
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
          <span v-else class="text-muted">Unassigned</span>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="element-actions">
            <Button
              icon="pi pi-eye"
              text
              rounded
              @click.stop="handleView(row)"
              v-tooltip.top="'View details'"
            />
            <Button
              icon="pi pi-wrench"
              text
              rounded
              severity="info"
              @click.stop="handleMaintenance(row)"
              v-tooltip.top="'Maintenance mode'"
              v-if="canToggleMaintenance(row)"
            />
            <Button
              icon="pi pi-signal"
              text
              rounded
              severity="success"
              @click.stop="handlePing(row)"
              v-tooltip.top="'Ping element'"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-sitemap empty-state__icon"></i>
            <h3 class="empty-state__title">No network elements found</h3>
            <p class="empty-state__description">
              {{ searchTerm || elementTypeFilter || statusFilter || locationFilter ?
                'Try adjusting your search criteria' :
                'Get started by adding your first network element'
              }}
            </p>
            <Button
              v-if="!searchTerm && !elementTypeFilter && !statusFilter && !locationFilter"
              label="Add Network Element"
              icon="pi pi-plus"
              severity="primary"
              @click="navigateTo('/assets/create?type=NETWORK_ELEMENT')"
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
import { useAssetStore } from '~/stores/asset'

// Page meta
definePageMeta({
  title: 'Network Elements'
})

// Store
const assetStore = useAssetStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const elementTypeFilter = ref('')
const statusFilter = ref('')
const locationFilter = ref('')
const sortOption = ref('name,asc')

// Table columns configuration
const tableColumns = [
  {
    key: 'assetId',
    label: 'Element ID',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'name',
    label: 'Name',
    sortable: true,
    style: 'width: 20%'
  },
  {
    key: 'location',
    label: 'Location',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'ipAddress',
    label: 'IP Address',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'customerName',
    label: 'Customer',
    sortable: false,
    style: 'width: 18%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 13%'
  }
]

// Filter options
const elementTypeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Router', value: 'ROUTER' },
  { label: 'Switch', value: 'SWITCH' },
  { label: 'Base Station', value: 'BASE_STATION' },
  { label: 'Gateway', value: 'GATEWAY' },
  { label: 'Access Point', value: 'ACCESS_POINT' }
]

const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'Online', value: 'ACTIVE' },
  { label: 'Warning', value: 'MAINTENANCE' },
  { label: 'Offline', value: 'INACTIVE' }
]

const locationOptions = [
  { label: 'All Locations', value: '' },
  { label: 'Data Center 1', value: 'DC1' },
  { label: 'Data Center 2', value: 'DC2' },
  { label: 'Regional Hub', value: 'REGIONAL' }
]

const sortOptions = [
  { label: 'Name A-Z', value: 'name,asc' },
  { label: 'Name Z-A', value: 'name,desc' },
  { label: 'Location', value: 'location,asc' },
  { label: 'Status', value: 'status,asc' }
]

// Computed
const networkElements = computed(() => {
  return assetStore.assets.filter(asset => asset.assetType === 'NETWORK_ELEMENT')
})
const onlineCount = computed(() => networkElements.value.filter(e => e.status === 'ACTIVE').length)
const warningCount = computed(() => networkElements.value.filter(e => e.status === 'MAINTENANCE').length)
const offlineCount = computed(() => networkElements.value.filter(e => e.status === 'INACTIVE').length)
const paginationProps = computed(() => ({
  page: assetStore.pagination.page,
  size: assetStore.pagination.size,
  total: assetStore.pagination.totalElements
}))

// Methods
const handleSearch = useDebounceFn(async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: 'NETWORK_ELEMENT',
    subType: elementTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    location: locationFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}, 300)

const handleElementTypeFilter = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: 'NETWORK_ELEMENT',
    status: statusFilter.value || undefined,
    location: locationFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    subType: elementTypeFilter.value || undefined
  })
}

const handleStatusFilter = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: 'NETWORK_ELEMENT',
    subType: elementTypeFilter.value || undefined,
    location: locationFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined
  })
}

const handleLocationFilter = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: 'NETWORK_ELEMENT',
    subType: elementTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    location: locationFilter.value || undefined
  })
}

const handleSortChange = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: 'NETWORK_ELEMENT',
    subType: elementTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    location: locationFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await assetStore.fetchAssets({
    page: assetStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    assetType: 'NETWORK_ELEMENT',
    subType: elementTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    location: locationFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await assetStore.fetchAssets({
    page,
    size: rows,
    sort: sortOption.value,
    assetType: 'NETWORK_ELEMENT',
    subType: elementTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    location: locationFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/assets/${row.id}`)
}

const handleMaintenance = async (row: any) => {
  const newStatus = row.status === 'MAINTENANCE' ? 'ACTIVE' : 'MAINTENANCE'

  try {
    showToast({
      severity: 'info',
      summary: 'Maintenance Mode',
      detail: `Element ${row.name} ${newStatus === 'MAINTENANCE' ? 'entered' : 'exited'} maintenance mode.`,
      life: 3000
    })

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to change maintenance status',
      life: 5000
    })
  }
}

const handlePing = async (row: any) => {
  try {
    showToast({
      severity: 'success',
      summary: 'Ping Successful',
      detail: `Element ${row.name} is reachable.`,
      life: 3000
    })

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Ping Failed',
      detail: `Element ${row.name} is not reachable.`,
      life: 5000
    })
  }
}

const canToggleMaintenance = (row: any) => {
  return row.status === 'ACTIVE' || row.status === 'MAINTENANCE'
}

// Utility functions
const getElementIcon = (subType: string): string => {
  const icons: Record<string, string> = {
    ROUTER: 'pi pi-share-alt',
    SWITCH: 'pi pi-sitemap',
    BASE_STATION: 'pi pi-broadcast',
    GATEWAY: 'pi pi-box',
    ACCESS_POINT: 'pi pi-wifi'
  }
  return icons[subType] || 'pi pi-server'
}

const formatElementSubType = (subType: string): string => {
  const types: Record<string, string> = {
    ROUTER: 'Router',
    SWITCH: 'Switch',
    BASE_STATION: 'Base Station',
    GATEWAY: 'Gateway',
    ACCESS_POINT: 'Access Point'
  }
  return types[subType] || subType
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
  await assetStore.fetchAssets({ assetType: 'NETWORK_ELEMENT' })
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await assetStore.fetchAssets({ assetType: 'NETWORK_ELEMENT' })
})
</script>

<style scoped>
.network-elements-page {
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

.summary-card__icon.warning {
  background: var(--color-orange-100);
  color: var(--color-orange-600);
}

.summary-card__icon.offline {
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
.elements-filters {
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
.elements-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Element ID Cell */
.element-id__text {
  font-family: monospace;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* Element Name Cell */
.element-name {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.element-icon {
  font-size: 1.25rem;
  color: var(--color-primary);
}

.element-info {
  flex: 1;
  min-width: 0;
}

.element-name__text {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.element-subtype {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Location Cell */
.location {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-primary);
}

/* IP Address Cell */
.ip-address code {
  font-family: 'Courier New', monospace;
  font-size: var(--font-size-sm);
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

/* Actions */
.element-actions {
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

  .element-name {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
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
  .element-name {
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
