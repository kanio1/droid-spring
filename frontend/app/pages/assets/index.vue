<template>
  <div class="assets-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Assets</h1>
        <p class="page-subtitle">Manage network assets, SIM cards, and hardware inventory</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Create Asset"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/assets/create')"
        />
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-card__icon">
          <i class="pi pi-box"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Total Assets</div>
          <div class="summary-card__value">{{ assets.length }}</div>
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
        <div class="summary-card__icon maintenance">
          <i class="pi pi-wrench"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Maintenance</div>
          <div class="summary-card__value">{{ maintenanceCount }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon inactive">
          <i class="pi pi-times-circle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Inactive</div>
          <div class="summary-card__value">{{ inactiveCount }}</div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="assets-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search assets..."
            @input="handleSearch"
            style="width: 100%"
          />
        </span>

        <Dropdown
          v-model="assetTypeFilter"
          :options="assetTypeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Types"
          style="width: 180px"
          @change="handleAssetTypeFilter"
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

    <!-- Quick Access Cards -->
    <div class="quick-access">
      <h2 class="quick-access__title">Asset Categories</h2>
      <div class="category-cards">
        <div class="category-card" @click="navigateTo('/assets/network-elements')">
          <div class="category-card__icon">
            <i class="pi pi-sitemap"></i>
          </div>
          <div class="category-card__content">
            <div class="category-card__title">Network Elements</div>
            <div class="category-card__description">Routers, switches, base stations</div>
            <div class="category-card__count">{{ networkElementsCount }} items</div>
          </div>
        </div>

        <div class="category-card" @click="navigateTo('/assets/sim-cards')">
          <div class="category-card__icon">
            <i class="pi pi-credit-card"></i>
          </div>
          <div class="category-card__content">
            <div class="category-card__title">SIM Cards</div>
            <div class="category-card__description">Mobile and IoT SIM cards</div>
            <div class="category-card__count">{{ simCardsCount }} items</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Assets Table -->
    <div class="assets-table">
      <AppTable
        :columns="tableColumns"
        :data="assets"
        :loading="assetStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Asset ID column -->
        <template #cell-assetId="{ row }">
          <div class="asset-id">
            <span class="asset-id__text">{{ row.assetId }}</span>
          </div>
        </template>

        <!-- Name column -->
        <template #cell-name="{ row }">
          <div class="asset-name">
            <div class="asset-name__text">{{ row.name }}</div>
            <div class="asset-name__type">{{ row.assetType }}</div>
          </div>
        </template>

        <!-- Location column -->
        <template #cell-location="{ row }">
          <div class="location">
            <span>{{ row.location || 'N/A' }}</span>
          </div>
        </template>

        <!-- Status column -->
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="asset" size="small" />
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
          <span v-else class="text-muted">—</span>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="asset-actions">
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
              severity="info"
              @click.stop="handleEdit(row)"
              v-tooltip.top="'Edit asset'"
              v-if="canEdit(row)"
            />
            <Button
              icon="pi pi-trash"
              text
              rounded
              severity="danger"
              @click.stop="handleDelete(row)"
              v-tooltip.top="'Delete asset'"
              v-if="canDelete(row)"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-box empty-state__icon"></i>
            <h3 class="empty-state__title">No assets found</h3>
            <p class="empty-state__description">
              {{ searchTerm || assetTypeFilter || statusFilter ?
                'Try adjusting your search criteria' :
                'Get started by creating your first asset'
              }}
            </p>
            <Button
              v-if="!searchTerm && !assetTypeFilter && !statusFilter"
              label="Create First Asset"
              icon="pi pi-plus"
              severity="primary"
              @click="navigateTo('/assets/create')"
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
  title: 'Assets'
})

// Store
const assetStore = useAssetStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const assetTypeFilter = ref('')
const statusFilter = ref('')
const sortOption = ref('name,asc')

// Table columns configuration
const tableColumns = [
  {
    key: 'assetId',
    label: 'Asset ID',
    sortable: true,
    style: 'width: 15%'
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
    key: 'status',
    label: 'Status',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'customerName',
    label: 'Customer',
    sortable: false,
    style: 'width: 20%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 12%'
  }
]

// Filter options
const assetTypeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Network Element', value: 'NETWORK_ELEMENT' },
  { label: 'SIM Card', value: 'SIM_CARD' },
  { label: 'Hardware', value: 'HARDWARE' },
  { label: 'Software License', value: 'SOFTWARE_LICENSE' }
]

const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Maintenance', value: 'MAINTENANCE' }
]

const sortOptions = [
  { label: 'Name A-Z', value: 'name,asc' },
  { label: 'Name Z-A', value: 'name,desc' },
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' }
]

// Computed
const assets = computed(() => assetStore.assets)
const activeCount = computed(() => assets.value.filter(a => a.status === 'ACTIVE').length)
const maintenanceCount = computed(() => assets.value.filter(a => a.status === 'MAINTENANCE').length)
const inactiveCount = computed(() => assets.value.filter(a => a.status === 'INACTIVE').length)
const networkElementsCount = computed(() => assets.value.filter(a => a.assetType === 'NETWORK_ELEMENT').length)
const simCardsCount = computed(() => assets.value.filter(a => a.assetType === 'SIM_CARD').length)
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
    assetType: assetTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}, 300)

const handleAssetTypeFilter = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    assetType: assetTypeFilter.value || undefined
  })
}

const handleStatusFilter = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: assetTypeFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined
  })
}

const handleSortChange = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: assetTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await assetStore.fetchAssets({
    page: assetStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    assetType: assetTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await assetStore.fetchAssets({
    page,
    size: rows,
    sort: sortOption.value,
    assetType: assetTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/assets/${row.id}`)
}

const handleEdit = (row: any) => {
  navigateTo(`/assets/${row.id}/edit`)
}

const handleDelete = async (row: any) => {
  const confirmed = confirm(`Are you sure you want to delete asset ${row.name}?`)

  if (confirmed) {
    try {
      await assetStore.deleteAsset(row.id)

      showToast({
        severity: 'success',
        summary: 'Asset Deleted',
        detail: `Asset ${row.name} has been deleted.`,
        life: 3000
      })

      await assetStore.fetchAssets({
        assetType: assetTypeFilter.value || undefined,
        status: statusFilter.value || undefined,
        searchTerm: searchTerm.value || undefined
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to delete asset',
        life: 5000
      })
    }
  }
}

const canEdit = (row: any) => {
  return row.status === 'ACTIVE' || row.status === 'MAINTENANCE'
}

const canDelete = (row: any) => {
  return row.status === 'INACTIVE'
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

// Lifecycle
onMounted(async () => {
  await assetStore.fetchAssets()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await assetStore.fetchAssets()
})
</script>

<style scoped>
.assets-page {
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

.summary-card__icon.maintenance {
  background: var(--color-orange-100);
  color: var(--color-orange-600);
}

.summary-card__icon.inactive {
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
.assets-filters {
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

/* Quick Access */
.quick-access {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.quick-access__title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.category-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--space-4);
}

.category-card {
  display: flex;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 0.2s;
}

.category-card:hover {
  border-color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.category-card__icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  background: var(--color-primary-100);
  color: var(--color-primary-600);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.75rem;
  flex-shrink: 0;
}

.category-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.category-card__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.category-card__description {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.category-card__count {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  font-weight: var(--font-weight-medium);
}

/* Table */
.assets-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Asset ID Cell */
.asset-id__text {
  font-family: monospace;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* Asset Name Cell */
.asset-name {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.asset-name__text {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.asset-name__type {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Location Cell */
.location {
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
.asset-actions {
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

  .category-cards {
    grid-template-columns: 1fr;
  }

  .category-card {
    flex-direction: column;
    align-items: flex-start;
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
  .category-card {
    gap: var(--space-3);
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
