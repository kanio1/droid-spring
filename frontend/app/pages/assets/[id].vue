<template>
  <div class="asset-detail-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/assets" class="back-link">
        ← Back to Assets
      </NuxtLink>
      <div class="page-header__content">
        <div class="title-row">
          <h1 class="page-title">Asset {{ asset?.assetId || '...' }}</h1>
          <StatusBadge v-if="asset" :status="asset.status" type="asset" size="large" />
        </div>
        <p class="page-subtitle" v-if="asset">
          {{ asset.name }} - {{ asset.assetType }}
        </p>
      </div>
      <div class="page-header__actions">
        <Button
          v-if="canEdit"
          label="Edit"
          icon="pi pi-pencil"
          severity="primary"
          @click="handleEdit"
        />
        <Button
          v-if="canChangeStatus"
          label="Change Status"
          icon="pi pi-refresh"
          severity="info"
          @click="handleChangeStatus"
        />
        <Button
          v-if="canDelete"
          label="Delete"
          icon="pi pi-trash"
          severity="danger"
          @click="handleDelete"
        />
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner />
      <p>Loading asset details...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <i class="pi pi-exclamation-triangle"></i>
      <p>{{ error }}</p>
      <Button label="Retry" @click="fetchAsset" />
    </div>

    <div v-else-if="asset" class="asset-content">
      <!-- Asset Summary Card -->
      <div class="card asset-summary">
        <div class="card-header">
          <h2>Asset Summary</h2>
        </div>
        <div class="card-body">
          <div class="summary-grid">
            <div class="summary-item">
              <label>Asset ID</label>
              <span>{{ asset.assetId }}</span>
            </div>
            <div class="summary-item">
              <label>Name</label>
              <span>{{ asset.name }}</span>
            </div>
            <div class="summary-item">
              <label>Type</label>
              <span>{{ formatAssetType(asset.assetType) }}</span>
            </div>
            <div class="summary-item">
              <label>Location</label>
              <span>{{ asset.location || 'Not specified' }}</span>
            </div>
            <div class="summary-item">
              <label>Serial Number</label>
              <span>{{ asset.serialNumber || 'N/A' }}</span>
            </div>
            <div class="summary-item">
              <label>Manufacturer</label>
              <span>{{ asset.manufacturer || 'N/A' }}</span>
            </div>
            <div class="summary-item">
              <label>Model</label>
              <span>{{ asset.model || 'N/A' }}</span>
            </div>
            <div class="summary-item">
              <label>Customer</label>
              <div v-if="asset.customerName" class="customer-info">
                <div class="customer-avatar">
                  {{ getCustomerInitials(asset) }}
                </div>
                <div>
                  <div class="customer-name">{{ asset.customerName }}</div>
                  <div class="customer-id">{{ asset.customerId }}</div>
                </div>
              </div>
              <span v-else class="text-muted">Unassigned</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Technical Specifications -->
      <div v-if="asset.specifications && Object.keys(asset.specifications).length > 0" class="card technical-specs">
        <div class="card-header">
          <h2>Technical Specifications</h2>
        </div>
        <div class="card-body">
          <div class="specs-grid">
            <div v-for="(value, key) in asset.specifications" :key="key" class="spec-item">
              <label>{{ formatSpecKey(key) }}</label>
              <span>{{ value }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Warranty Information -->
      <div v-if="asset.warrantyStart || asset.warrantyEnd" class="card warranty-info">
        <div class="card-header">
          <h2>Warranty Information</h2>
        </div>
        <div class="card-body">
          <div class="summary-grid">
            <div class="summary-item" v-if="asset.warrantyStart">
              <label>Warranty Start</label>
              <span>{{ formatDate(asset.warrantyStart) }}</span>
            </div>
            <div class="summary-item" v-if="asset.warrantyEnd">
              <label>Warranty End</label>
              <span :class="{ 'text-warning': isWarrantyExpiringSoon(asset) }">
                {{ formatDate(asset.warrantyEnd) }}
                <i v-if="isWarrantyExpiringSoon(asset)" class="pi pi-exclamation-triangle ml-1"></i>
              </span>
            </div>
            <div class="summary-item" v-if="asset.warrantyProvider">
              <label>Provider</label>
              <span>{{ asset.warrantyProvider }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Assignment History -->
      <div class="card assignment-history">
        <div class="card-header">
          <h2>Assignment History</h2>
        </div>
        <div class="card-body">
          <AppTable :columns="assignmentColumns" :data="asset.assignments || []" :show-pagination="false">
            <template #cell-assignedDate="{ row }">
              {{ formatDateTime(row.assignedDate) }}
            </template>
            <template #cell-unassignedDate="{ row }">
              <span v-if="row.unassignedDate">{{ formatDateTime(row.unassignedDate) }}</span>
              <span v-else class="text-success">Currently Assigned</span>
            </template>
            <template #cell-customerName="{ row }">
              <div v-if="row.customerName" class="customer-name">
                <div class="customer-avatar small">
                  {{ getInitials(row.customerName) }}
                </div>
                <div>
                  <div class="customer-name__text">{{ row.customerName }}</div>
                  <div class="customer-id">{{ row.customerId }}</div>
                </div>
              </div>
              <span v-else>—</span>
            </template>
            <template #empty>
              <div class="empty-history">No assignment history</div>
            </template>
          </AppTable>
        </div>
      </div>

      <!-- Maintenance Records -->
      <div class="card maintenance-records">
        <div class="card-header">
          <h2>Maintenance Records</h2>
        </div>
        <div class="card-body">
          <AppTable :columns="maintenanceColumns" :data="asset.maintenanceRecords || []" :show-pagination="false">
            <template #cell-date="{ row }">
              {{ formatDate(row.date) }}
            </template>
            <template #cell-type="{ row }">
              <StatusBadge :status="row.type" type="maintenance" size="small" />
            </template>
            <template #cell-status="{ row }">
              <StatusBadge :status="row.status" type="maintenance-status" size="small" />
            </template>
            <template #cell-cost="{ row }">
              <span v-if="row.cost">{{ formatCurrency(row.cost) }}</span>
              <span v-else>—</span>
            </template>
            <template #empty>
              <div class="empty-maintenance">No maintenance records</div>
            </template>
          </AppTable>
        </div>
      </div>

      <!-- Notes -->
      <div v-if="asset.notes" class="card asset-notes">
        <div class="card-header">
          <h2>Notes</h2>
        </div>
        <div class="card-body">
          <p>{{ asset.notes }}</p>
        </div>
      </div>

      <!-- Metadata -->
      <div class="card asset-metadata">
        <div class="card-header">
          <h2>Metadata</h2>
        </div>
        <div class="card-body">
          <div class="metadata-grid">
            <div class="metadata-item">
              <label>Created</label>
              <span>{{ formatDateTime(asset.createdAt) }}</span>
            </div>
            <div class="metadata-item">
              <label>Last Updated</label>
              <span>{{ formatDateTime(asset.updatedAt) }}</span>
            </div>
            <div class="metadata-item">
              <label>Version</label>
              <span>{{ asset.version }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast for notifications -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { useAssetStore } from '~/stores/asset'

// Page meta
definePageMeta({
  title: 'Asset Details'
})

// Route params
const route = useRoute()
const assetId = route.params.id as string

// Store
const assetStore = useAssetStore()
const { showToast } = useToast()

// Reactive state
const loading = ref(true)
const error = ref<string | null>(null)

// Computed
const asset = computed(() => assetStore.currentAsset)
const canEdit = computed(() => asset.value && asset.value.status !== 'INACTIVE')
const canChangeStatus = computed(() => !!asset.value)
const canDelete = computed(() => asset.value && asset.value.status === 'INACTIVE')

// Table columns
const assignmentColumns = [
  { key: 'customerName', label: 'Customer', style: 'width: 30%' },
  { key: 'assignedDate', label: 'Assigned Date', style: 'width: 20%' },
  { key: 'unassignedDate', label: 'Unassigned Date', style: 'width: 20%' },
  { key: 'notes', label: 'Notes', style: 'width: 30%' }
]

const maintenanceColumns = [
  { key: 'date', label: 'Date', style: 'width: 15%' },
  { key: 'type', label: 'Type', style: 'width: 15%' },
  { key: 'description', label: 'Description', style: 'width: 30%' },
  { key: 'technician', label: 'Technician', style: 'width: 15%' },
  { key: 'status', label: 'Status', style: 'width: 15%' },
  { key: 'cost', label: 'Cost', style: 'width: 10%', align: 'right' }
]

// Methods
const fetchAsset = async () => {
  try {
    loading.value = true
    error.value = null
    await assetStore.fetchAssetById(assetId)
  } catch (err: any) {
    error.value = err.message || 'Failed to load asset'
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  navigateTo(`/assets/${assetId}/edit`)
}

const handleChangeStatus = async () => {
  if (!asset.value) return

  const newStatus = asset.value.status === 'ACTIVE' ? 'MAINTENANCE' : 'ACTIVE'
  const confirmed = confirm(`Are you sure you want to change status to ${newStatus}?`)

  if (confirmed) {
    try {
      await assetStore.changeAssetStatus({
        id: assetId,
        status: newStatus
      })

      showToast({
        severity: 'success',
        summary: 'Status Changed',
        detail: `Asset status has been changed to ${newStatus}.`,
        life: 3000
      })

      await fetchAsset()

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to change status',
        life: 5000
      })
    }
  }
}

const handleDelete = async () => {
  if (!asset.value) return

  const confirmed = confirm(`Are you sure you want to delete asset ${asset.value.name}?`)

  if (confirmed) {
    try {
      await assetStore.deleteAsset(assetId)

      showToast({
        severity: 'success',
        summary: 'Asset Deleted',
        detail: `Asset ${asset.value.name} has been deleted.`,
        life: 3000
      })

      navigateTo('/assets')

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

// Utility functions
const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
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
    currency: currency
  }).format(amount)
}

const formatAssetType = (type: string): string => {
  const types: Record<string, string> = {
    NETWORK_ELEMENT: 'Network Element',
    SIM_CARD: 'SIM Card',
    HARDWARE: 'Hardware',
    SOFTWARE_LICENSE: 'Software License'
  }
  return types[type] || type
}

const formatSpecKey = (key: string): string => {
  return key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())
}

const getCustomerInitials = (row: any): string => {
  if (!row.customerName) return 'N/A'
  const names = row.customerName.split(' ')
  if (names.length >= 2) {
    return `${names[0][0]}${names[1][0]}`.toUpperCase()
  }
  return row.customerName.substring(0, 2).toUpperCase()
}

const getInitials = (name: string): string => {
  if (!name) return 'N/A'
  const names = name.split(' ')
  if (names.length >= 2) {
    return `${names[0][0]}${names[1][0]}`.toUpperCase()
  }
  return name.substring(0, 2).toUpperCase()
}

const isWarrantyExpiringSoon = (row: any): boolean => {
  if (!row.warrantyEnd) return false
  const today = new Date()
  const warrantyEnd = new Date(row.warrantyEnd)
  const diffTime = warrantyEnd.getTime() - today.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays <= 30 && diffDays >= 0
}

// Lifecycle
onMounted(async () => {
  await fetchAsset()
})
</script>

<style scoped>
.asset-detail-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* Page Header */
.page-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.back-link {
  color: var(--color-primary);
  text-decoration: none;
  font-size: var(--font-size-sm);
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.back-link:hover {
  text-decoration: underline;
}

.page-header__content {
  flex: 1;
}

.title-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-1);
}

.page-title {
  margin: 0;
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
  display: flex;
  gap: var(--space-2);
  align-self: flex-start;
}

/* Loading and Error States */
.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  text-align: center;
}

.error-state i {
  font-size: 3rem;
  color: var(--color-red-500);
  margin-bottom: var(--space-4);
}

/* Cards */
.card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.card-header {
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
}

.card-header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.card-body {
  padding: var(--space-6);
}

/* Asset Summary */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.summary-item label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.summary-item > span {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

.customer-info {
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

.customer-avatar.small {
  width: 32px;
  height: 32px;
  font-size: var(--font-size-xs);
}

.customer-name {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.customer-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Technical Specifications */
.specs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.spec-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.spec-item label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.spec-item > span {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

/* Assignment History */
.empty-history {
  text-align: center;
  padding: var(--space-4);
  color: var(--color-text-muted);
}

/* Maintenance Records */
.empty-maintenance {
  text-align: center;
  padding: var(--space-4);
  color: var(--color-text-muted);
}

/* Notes */
.asset-notes p {
  margin: 0;
  color: var(--color-text-primary);
  white-space: pre-wrap;
}

/* Metadata */
.metadata-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.metadata-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.metadata-item label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.metadata-item > span {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    gap: var(--space-3);
  }

  .title-row {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }

  .page-header__actions {
    width: 100%;
    flex-direction: column;
  }

  .page-header__actions button {
    width: 100%;
  }

  .summary-grid,
  .specs-grid,
  .metadata-grid {
    grid-template-columns: 1fr;
  }

  .customer-info {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }
}
</style>
