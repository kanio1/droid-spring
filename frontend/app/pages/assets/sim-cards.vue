<template>
  <div class="sim-cards-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">SIM Cards</h1>
        <p class="page-subtitle">Manage mobile and IoT SIM cards</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Create SIM Card"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/assets/create?type=SIM_CARD')"
        />
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-card__icon">
          <i class="pi pi-credit-card"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Total SIMs</div>
          <div class="summary-card__value">{{ simCards.length }}</div>
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
        <div class="summary-card__icon reserved">
          <i class="pi pi-lock"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Reserved</div>
          <div class="summary-card__value">{{ reservedCount }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon suspended">
          <i class="pi pi-pause-circle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Suspended</div>
          <div class="summary-card__value">{{ suspendedCount }}</div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="sim-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search SIM cards..."
            @input="handleSearch"
            style="width: 100%"
          />
        </span>

        <Dropdown
          v-model="simTypeFilter"
          :options="simTypeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Types"
          style="width: 180px"
          @change="handleSimTypeFilter"
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
          v-model="operatorFilter"
          :options="operatorOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Operators"
          style="width: 180px"
          @change="handleOperatorFilter"
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

    <!-- SIM Cards Table -->
    <div class="sim-table">
      <AppTable
        :columns="tableColumns"
        :data="simCards"
        :loading="assetStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- ICCID column -->
        <template #cell-iccid="{ row }">
          <div class="iccid">
            <span class="iccid__text">{{ row.iccid }}</span>
          </div>
        </template>

        <!-- IMSI column -->
        <template #cell-imsi="{ row }">
          <div v-if="row.imsi" class="imsi">
            <code>{{ row.imsi }}</code>
          </div>
          <span v-else class="text-muted">Not provisioned</span>
        </template>

        <!-- Phone Number column -->
        <template #cell-phoneNumber="{ row }">
          <div v-if="row.phoneNumber" class="phone-number">
            <i class="pi pi-phone"></i>
            <span>{{ formatPhoneNumber(row.phoneNumber) }}</span>
          </div>
          <span v-else class="text-muted">—</span>
        </template>

        <!-- Type column -->
        <template #cell-subType="{ row }">
          <StatusBadge :status="row.subType" type="sim-type" size="small" />
        </template>

        <!-- Operator column -->
        <template #cell-operator="{ row }">
          <div class="operator">
            <span>{{ row.operator || 'N/A' }}</span>
          </div>
        </template>

        <!-- Status column -->
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="sim-status" size="small" />
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
          <div class="sim-actions">
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
              v-tooltip.top="'Activate SIM'"
              v-if="canActivate(row)"
            />
            <Button
              icon="pi pi-pause"
              text
              rounded
              severity="warning"
              @click.stop="handleSuspend(row)"
              v-tooltip.top="'Suspend SIM'"
              v-if="canSuspend(row)"
            />
            <Button
              icon="pi pi-lock"
              text
              rounded
              severity="info"
              @click.stop="handleReserve(row)"
              v-tooltip.top="'Reserve SIM'"
              v-if="canReserve(row)"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-credit-card empty-state__icon"></i>
            <h3 class="empty-state__title">No SIM cards found</h3>
            <p class="empty-state__description">
              {{ searchTerm || simTypeFilter || statusFilter || operatorFilter ?
                'Try adjusting your search criteria' :
                'Get started by adding your first SIM card'
              }}
            </p>
            <Button
              v-if="!searchTerm && !simTypeFilter && !statusFilter && !operatorFilter"
              label="Add SIM Card"
              icon="pi pi-plus"
              severity="primary"
              @click="navigateTo('/assets/create?type=SIM_CARD')"
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
  title: 'SIM Cards'
})

// Store
const assetStore = useAssetStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const simTypeFilter = ref('')
const statusFilter = ref('')
const operatorFilter = ref('')
const sortOption = ref('iccid,asc')

// Table columns configuration
const tableColumns = [
  {
    key: 'iccid',
    label: 'ICCID',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'imsi',
    label: 'IMSI',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'phoneNumber',
    label: 'Phone Number',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'subType',
    label: 'Type',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'operator',
    label: 'Operator',
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
    style: 'width: 17%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 12%'
  }
]

// Filter options
const simTypeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Physical SIM', value: 'PHYSICAL_SIM' },
  { label: 'eSIM', value: 'ESIM' },
  { label: 'IoT SIM', value: 'IOT_SIM' }
]

const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Reserved', value: 'RESERVED' },
  { label: 'Suspended', value: 'SUSPENDED' },
  { label: 'Inactive', value: 'INACTIVE' }
]

const operatorOptions = [
  { label: 'All Operators', value: '' },
  { label: 'Verizon', value: 'VERIZON' },
  { label: 'AT&T', value: 'ATT' },
  { label: 'T-Mobile', value: 'TMobile' },
  { label: 'Vodafone', value: 'VODAFONE' }
]

const sortOptions = [
  { label: 'ICCID', value: 'iccid,asc' },
  { label: 'Phone Number', value: 'phoneNumber,asc' },
  { label: 'Operator', value: 'operator,asc' },
  { label: 'Status', value: 'status,asc' }
]

// Computed
const simCards = computed(() => {
  return assetStore.assets.filter(asset => asset.assetType === 'SIM_CARD')
})
const activeCount = computed(() => simCards.value.filter(s => s.status === 'ACTIVE').length)
const reservedCount = computed(() => simCards.value.filter(s => s.status === 'RESERVED').length)
const suspendedCount = computed(() => simCards.value.filter(s => s.status === 'SUSPENDED').length)
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
    assetType: 'SIM_CARD',
    subType: simTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    operator: operatorFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}, 300)

const handleSimTypeFilter = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: 'SIM_CARD',
    status: statusFilter.value || undefined,
    operator: operatorFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    subType: simTypeFilter.value || undefined
  })
}

const handleStatusFilter = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: 'SIM_CARD',
    subType: simTypeFilter.value || undefined,
    operator: operatorFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined
  })
}

const handleOperatorFilter = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: 'SIM_CARD',
    subType: simTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    operator: operatorFilter.value || undefined
  })
}

const handleSortChange = async () => {
  await assetStore.fetchAssets({
    page: 0,
    sort: sortOption.value,
    assetType: 'SIM_CARD',
    subType: simTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    operator: operatorFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await assetStore.fetchAssets({
    page: assetStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    assetType: 'SIM_CARD',
    subType: simTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    operator: operatorFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await assetStore.fetchAssets({
    page,
    size: rows,
    sort: sortOption.value,
    assetType: 'SIM_CARD',
    subType: simTypeFilter.value || undefined,
    status: statusFilter.value || undefined,
    operator: operatorFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/assets/${row.id}`)
}

const handleActivate = async (row: any) => {
  try {
    showToast({
      severity: 'success',
      summary: 'SIM Activated',
      detail: `SIM card ${row.iccid} has been activated.`,
      life: 3000
    })

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to activate SIM',
      life: 5000
    })
  }
}

const handleSuspend = async (row: any) => {
  try {
    showToast({
      severity: 'info',
      summary: 'SIM Suspended',
      detail: `SIM card ${row.iccid} has been suspended.`,
      life: 3000
    })

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to suspend SIM',
      life: 5000
    })
  }
}

const handleReserve = async (row: any) => {
  try {
    showToast({
      severity: 'info',
      summary: 'SIM Reserved',
      detail: `SIM card ${row.iccid} has been reserved.`,
      life: 3000
    })

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to reserve SIM',
      life: 5000
    })
  }
}

const canActivate = (row: any) => {
  return row.status === 'INACTIVE' || row.status === 'RESERVED'
}

const canSuspend = (row: any) => {
  return row.status === 'ACTIVE'
}

const canReserve = (row: any) => {
  return row.status === 'INACTIVE'
}

// Utility functions
const formatPhoneNumber = (phoneNumber: string): string => {
  // Simple US phone number formatting
  if (phoneNumber.length === 10) {
    return `(${phoneNumber.substring(0, 3)}) ${phoneNumber.substring(3, 6)}-${phoneNumber.substring(6)}`
  }
  return phoneNumber
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
  await assetStore.fetchAssets({ assetType: 'SIM_CARD' })
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await assetStore.fetchAssets({ assetType: 'SIM_CARD' })
})
</script>

<style scoped>
.sim-cards-page {
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

.summary-card__icon.reserved {
  background: var(--color-blue-100);
  color: var(--color-blue-600);
}

.summary-card__icon.suspended {
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
.sim-filters {
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
.sim-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* ICCID Cell */
.iccid__text {
  font-family: 'Courier New', monospace;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* IMSI Cell */
.imsi code {
  font-family: 'Courier New', monospace;
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

/* Phone Number Cell */
.phone-number {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-primary);
}

/* Operator Cell */
.operator {
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
.sim-actions {
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
