<template>
  <div class="coverage-node-details-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <div class="breadcrumb">
          <NuxtLink to="/coverage-nodes" class="breadcrumb__link">Coverage Nodes</NuxtLink>
          <i class="pi pi-chevron-right breadcrumb__separator"></i>
          <span class="breadcrumb__current">{{ node?.name || 'Loading...' }}</span>
        </div>
        <h1 class="page-title">
          <i :class="node ? getTypeIcon(node.type) : ''" class="title-icon"></i>
          Coverage Node Details
          <Tag v-if="node" :value="node.status" :severity="getStatusVariant(node.status)" class="status-tag" />
        </h1>
        <p class="page-subtitle">
          {{ node ? `${node.city}, ${node.country}` : 'Loading coverage node information...' }}
        </p>
      </div>
      <div class="page-header__actions">
        <Button
          v-if="!isEditMode"
          label="Edit"
          icon="pi pi-pencil"
          severity="info"
          @click="handleEdit"
        />
        <Button
          v-else
          label="View Mode"
          icon="pi pi-eye"
          severity="secondary"
          @click="handleCancelEdit"
        />
        <Button
          label="Equipment"
          icon="pi pi-cog"
          severity="warning"
          @click="handleEquipment"
        />
        <Button
          label="Delete"
          icon="pi pi-trash"
          severity="danger"
          @click="handleDelete"
        />
      </div>
    </div>

    <div v-if="loading && !node" class="loading-state">
      <ProgressSpinner />
      <p>Loading coverage node details...</p>
    </div>

    <div v-else-if="error && !node" class="error-state">
      <i class="pi pi-exclamation-triangle"></i>
      <p>{{ error }}</p>
      <NuxtLink to="/coverage-nodes">
        <Button label="Back to List" severity="secondary" />
      </NuxtLink>
    </div>

    <div v-else-if="node" class="node-content">
      <!-- Statistics Cards -->
      <div class="stats-cards">
        <div class="stat-card stat-card--primary">
          <div class="stat-card__icon">
            <i class="pi pi-sitemap"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__value">{{ node.capacityPercentage }}%</div>
            <div class="stat-card__label">Capacity Usage</div>
            <div class="stat-card__detail">{{ formatCapacity(node.currentLoad, node.maxCapacity) }}</div>
          </div>
        </div>

        <div class="stat-card" :class="getCapacityCardClass(node.capacityPercentage)">
          <div class="stat-card__icon">
            <i class="pi pi-chart-bar"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__value">{{ node.coverageRadius }} km</div>
            <div class="stat-card__label">Coverage Radius</div>
            <div class="stat-card__detail">{{ formatCoverageArea(node.coverageRadius) }}</div>
          </div>
        </div>

        <div class="stat-card stat-card--info">
          <div class="stat-card__icon">
            <i class="pi pi-box"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__value">{{ node.equipmentCount }}</div>
            <div class="stat-card__label">Equipment</div>
            <div class="stat-card__detail">Total Devices</div>
          </div>
        </div>

        <div class="stat-card" :class="getUptimeCardClass(node.uptime)">
          <div class="stat-card__icon">
            <i class="pi pi-clock"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__value">{{ node.uptime || 0 }}%</div>
            <div class="stat-card__label">Uptime</div>
            <div class="stat-card__detail">{{ node.uptime ? 'Last 30 days' : 'No data' }}</div>
          </div>
        </div>
      </div>

      <!-- Status and Info Cards -->
      <div class="info-cards">
        <div class="info-card">
          <div class="info-card__header">
            <h3>Coverage Node Information</h3>
            <Tag :value="node.type" :severity="getTypeVariant(node.type)" />
          </div>
          <div class="info-card__content">
            <div class="info-row">
              <span class="info-row__label">Name:</span>
              <span class="info-row__value">{{ node.name }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Code:</span>
              <span class="info-row__value code">{{ node.code }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Technology:</span>
              <Tag :value="node.technology" severity="info" />
            </div>
            <div class="info-row">
              <span class="info-row__label">Status:</span>
              <Tag :value="node.status" :severity="getStatusVariant(node.status)" />
            </div>
            <div class="info-row">
              <span class="info-row__label">Coverage Area:</span>
              <span class="info-row__value">{{ formatCoverageArea(node.coverageRadius) }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Addresses Covered:</span>
              <span class="info-row__value">{{ node.addressCount.toLocaleString() }}</span>
            </div>
          </div>
        </div>

        <div class="info-card">
          <div class="info-card__header">
            <h3>Location Details</h3>
          </div>
          <div class="info-card__content">
            <div class="info-row">
              <span class="info-row__label">City:</span>
              <span class="info-row__value">{{ node.city }}</span>
            </div>
            <div class="info-row" v-if="node.region">
              <span class="info-row__label">Region:</span>
              <span class="info-row__value">{{ node.region }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Country:</span>
              <span class="info-row__value">{{ node.country }}</span>
            </div>
            <div class="info-row" v-if="node.address">
              <span class="info-row__label">Address:</span>
              <span class="info-row__value">{{ node.address }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Coordinates:</span>
              <span class="info-row__value coordinates">{{ formatCoordinates(node) }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Coverage Radius:</span>
              <span class="info-row__value">{{ node.coverageRadius }} km</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Map Section -->
      <div v-if="node.latitude && node.longitude" class="map-section">
        <div class="map-section__header">
          <h3>Coverage Map</h3>
          <p class="map-section__description">Geographic location and coverage area</p>
        </div>
        <div class="map-container">
          <div class="map-placeholder">
            <i class="pi pi-map"></i>
            <h4>Interactive Coverage Map</h4>
            <p class="text-sm text-gray-600 mb-3">Location: {{ node.latitude }}, {{ node.longitude }}</p>
            <p class="text-sm text-gray-600 mb-3">Coverage Radius: {{ node.coverageRadius }} km</p>
            <div class="map-coverage-circle">
              <div class="circle-label">Coverage Area</div>
            </div>
            <p class="text-sm text-gray-600 mt-3">
              Map integration placeholder - Ready for Leaflet or Google Maps
            </p>
          </div>
        </div>
      </div>

      <!-- Edit Mode Notice -->
      <div v-if="isEditMode" class="edit-notice">
        <i class="pi pi-info-circle"></i>
        <p>Edit mode is not available on this page. Please use the Edit button in the list view or create a new coverage node.</p>
      </div>

      <!-- Maintenance Section -->
      <div v-if="node.lastMaintenance || node.nextMaintenance" class="maintenance-section">
        <h3>Maintenance Schedule</h3>
        <div class="maintenance-cards">
          <div v-if="node.lastMaintenance" class="maintenance-card">
            <div class="maintenance-card__header">
              <i class="pi pi-check-circle"></i>
              <h4>Last Maintenance</h4>
            </div>
            <div class="maintenance-card__content">
              <p class="maintenance-date">{{ formatDateTime(node.lastMaintenance) }}</p>
            </div>
          </div>

          <div v-if="node.nextMaintenance" class="maintenance-card" :class="{ 'maintenance-card--urgent': isMaintenanceDue(node.nextMaintenance) }">
            <div class="maintenance-card__header">
              <i class="pi pi-calendar"></i>
              <h4>Next Maintenance</h4>
            </div>
            <div class="maintenance-card__content">
              <p class="maintenance-date">{{ formatDateTime(node.nextMaintenance) }}</p>
              <Tag v-if="isMaintenanceDue(node.nextMaintenance)" value="DUE" severity="danger" />
            </div>
          </div>
        </div>
      </div>

      <!-- Related Information -->
      <div class="related-section">
        <h3>Related Information</h3>
        <div class="related-info">
          <div class="related-card">
            <div class="related-card__header">
              <i class="pi pi-list"></i>
              <h4>Equipment</h4>
            </div>
            <div class="related-card__content">
              <p class="text-sm text-gray-600 mb-3">
                This node has {{ node.equipmentCount }} equipment item(s)
              </p>
              <Button label="View Equipment" icon="pi pi-cog" severity="secondary" outlined @click="handleEquipment" />
            </div>
          </div>

          <div class="related-card">
            <div class="related-card__header">
              <i class="pi pi-map-marker"></i>
              <h4>Addresses in Coverage</h4>
            </div>
            <div class="related-card__content">
              <p class="text-sm text-gray-600 mb-3">
                {{ node.addressCount.toLocaleString() }} addresses covered
              </p>
              <NuxtLink :to="`/addresses?coverageNodeId=${node.id}`">
                <Button label="View Covered Addresses" icon="pi pi-external-link" severity="secondary" text />
              </NuxtLink>
            </div>
          </div>

          <div class="related-card">
            <div class="related-card__header">
              <i class="pi pi-users"></i>
              <h4>Similar Nodes</h4>
            </div>
            <div class="related-card__content">
              <p class="text-sm text-gray-600 mb-3">
                Other {{ node.type.toLowerCase().replace('_', ' ') }} nodes in {{ node.country }}
              </p>
              <NuxtLink :to="`/coverage-nodes?type=${node.type}&country=${node.country}`">
                <Button label="View Similar Nodes" icon="pi pi-filter" severity="secondary" text />
              </NuxtLink>
            </div>
          </div>

          <div class="related-card">
            <div class="related-card__header">
              <i class="pi pi-sitemap"></i>
              <h4>Capacity Management</h4>
            </div>
            <div class="related-card__content">
              <p class="text-sm text-gray-600 mb-3">
                Current load: {{ node.capacityPercentage }}% of {{ node.maxCapacity.toLocaleString() }}
              </p>
              <NuxtLink to="/coverage-nodes">
                <Button label="View All Nodes" icon="pi pi-chart-bar" severity="secondary" text />
              </NuxtLink>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Dialog -->
    <Dialog
      v-model:visible="showDeleteDialog"
      modal
      header="Confirm Delete"
      :style="{ width: '450px' }"
    >
      <div class="confirmation-content">
        <i class="pi pi-exclamation-triangle confirmation-icon" />
        <p>Are you sure you want to delete this coverage node?</p>
        <p class="text-sm text-gray-600 mt-2">{{ node?.name }} ({{ node?.code }})</p>
      </div>
      <template #footer>
        <Button
          label="Cancel"
          icon="pi pi-times"
          severity="secondary"
          @click="showDeleteDialog = false"
        />
        <Button
          label="Delete"
          icon="pi pi-trash"
          severity="danger"
          :loading="loading"
          @click="confirmDelete"
        />
      </template>
    </Dialog>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCoverageNodeStore } from '~/stores/coverage-node'
import { useToast } from 'primevue/usetoast'
import type { CoverageNode } from '~/schemas/coverage-node'

// Meta
definePageMeta({
  title: 'Coverage Node Details'
})

// Route & Stores
const route = useRoute()
const router = useRouter()
const nodeStore = useCoverageNodeStore()
const toast = useToast()

// Reactive State
const loading = ref(false)
const error = ref<string | null>(null)
const isEditMode = ref(false)
const showDeleteDialog = ref(false)
const node = ref<CoverageNode | null>(null)

// Computed
const nodeId = computed(() => route.params.id as string)
const isEditRoute = computed(() => route.query.edit === 'true')

// Helper Functions (import from schema)
import {
  getStatusVariant,
  getTypeIcon,
  formatCoordinates,
  formatCoverageArea,
  formatCapacity,
  getTypeLabel
} from '~/schemas/coverage-node'

// Status/Type variants for tags
function getTypeVariant(type: string): 'success' | 'neutral' | 'warning' | 'info' | 'danger' {
  const typeLower = type.toLowerCase()
  if (typeLower.includes('cell') || typeLower.includes('tower')) return 'info'
  if (typeLower.includes('satellite')) return 'warning'
  if (typeLower.includes('fiber')) return 'success'
  if (typeLower.includes('wifi')) return 'neutral'
  return 'neutral'
}

// Capacity card styling
function getCapacityCardClass(capacityPercentage: number): string {
  if (capacityPercentage > 90) return 'stat-card--danger'
  if (capacityPercentage > 75) return 'stat-card--warning'
  return 'stat-card--success'
}

// Uptime card styling
function getUptimeCardClass(uptime?: number): string {
  if (!uptime) return 'stat-card--neutral'
  if (uptime < 95) return 'stat-card--danger'
  if (uptime < 99) return 'stat-card--warning'
  return 'stat-card--success'
}

// Maintenance due check
function isMaintenanceDue(nextMaintenance?: string): boolean {
  if (!nextMaintenance) return false
  const nextDate = new Date(nextMaintenance)
  const now = new Date()
  const daysUntil = Math.floor((nextDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  return daysUntil <= 7
}

// Date formatting
function formatDateTime(dateString: string): string {
  const date = new Date(dateString)
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

// Event Handlers
async function fetchNode() {
  loading.value = true
  error.value = null

  try {
    const data = await nodeStore.fetchNodeById(nodeId.value)
    node.value = data
  } catch (err: any) {
    error.value = err.message || 'Failed to fetch coverage node details'
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.value,
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  toast.add({
    severity: 'info',
    summary: 'Edit Mode',
    detail: 'Please use the list view to edit coverage nodes',
    life: 5000
  })
}

const handleCancelEdit = () => {
  isEditMode.value = false
}

const handleEquipment = () => {
  router.push(`/coverage-nodes/${nodeId.value}/equipment`)
}

const handleDelete = () => {
  showDeleteDialog.value = true
}

const confirmDelete = async () => {
  if (!node.value) return

  loading.value = true
  try {
    await nodeStore.deleteNode(node.value.id)
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Coverage node deleted successfully',
      life: 5000
    })
    showDeleteDialog.value = false
    router.push('/coverage-nodes')
  } catch (err: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: err.message || 'Failed to delete coverage node',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

// Lifecycle
onMounted(async () => {
  await fetchNode()
})

// Watch for edit mode query parameter
watch(isEditRoute, (newEditMode) => {
  if (newEditMode) {
    handleEdit()
    router.replace({ query: { ...route.query, edit: undefined } })
  }
}, { immediate: true })
</script>

<style scoped>
.coverage-node-details-page {
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

.breadcrumb {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
  font-size: var(--font-size-sm);
}

.breadcrumb__link {
  color: var(--color-primary);
  text-decoration: none;
}

.breadcrumb__link:hover {
  text-decoration: underline;
}

.breadcrumb__separator {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.breadcrumb__current {
  color: var(--color-text-secondary);
}

.page-title {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.title-icon {
  color: var(--color-primary);
  font-size: 2rem;
}

.status-tag {
  margin-left: var(--space-2);
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.page-header__actions {
  flex-shrink: 0;
  display: flex;
  gap: var(--space-2);
}

/* Loading & Error States */
.loading-state, .error-state {
  text-align: center;
  padding: var(--space-12);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.loading-state i, .error-state i {
  font-size: 4rem;
  color: var(--color-text-secondary);
  opacity: 0.5;
  margin-bottom: var(--space-4);
}

.error-state p, .loading-state p {
  color: var(--color-text-secondary);
  margin: var(--space-4) 0;
}

/* Statistics Cards */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  transition: all var(--transition-base) var(--transition-timing);
}

.stat-card--primary {
  border-color: var(--color-primary);
}

.stat-card--success {
  border-color: var(--green-400);
}

.stat-card--warning {
  border-color: var(--orange-400);
}

.stat-card--danger {
  border-color: var(--red-400);
}

.stat-card--info {
  border-color: var(--blue-400);
}

.stat-card--neutral {
  border-color: var(--color-border);
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  background: var(--color-primary-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 1.5rem;
  flex-shrink: 0;
}

.stat-card--primary .stat-card__icon {
  background: var(--color-primary-100);
  color: var(--color-primary);
}

.stat-card--success .stat-card__icon {
  background: var(--green-100);
  color: var(--green-600);
}

.stat-card--warning .stat-card__icon {
  background: var(--orange-100);
  color: var(--orange-600);
}

.stat-card--danger .stat-card__icon {
  background: var(--red-100);
  color: var(--red-600);
}

.stat-card--info .stat-card__icon {
  background: var(--blue-100);
  color: var(--blue-600);
}

.stat-card--neutral .stat-card__icon {
  background: var(--gray-100);
  color: var(--gray-600);
}

.stat-card__content {
  flex: 1;
  min-width: 0;
}

.stat-card__value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1;
}

.stat-card__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: var(--space-1);
}

.stat-card__detail {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-top: 2px;
}

/* Info Cards */
.info-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: var(--space-4);
}

.info-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.info-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.info-card__header h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.info-card__content {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.info-row__label {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.info-row__value {
  color: var(--color-text-primary);
  font-weight: var(--font-weight-normal);
}

.info-row__value.code {
  font-family: 'Monaco', 'Menlo', monospace;
  background: var(--color-gray-100);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.info-row__value.coordinates {
  font-family: 'Monaco', 'Menlo', monospace;
  color: var(--color-primary);
}

/* Map Section */
.map-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.map-section__header {
  margin-bottom: var(--space-4);
}

.map-section__header h3 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.map-section__description {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.map-container {
  height: 400px;
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.map-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: var(--color-gray-50);
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
  position: relative;
}

.map-placeholder i {
  font-size: 4rem;
  margin-bottom: var(--space-4);
  opacity: 0.5;
}

.map-placeholder h4 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.map-coverage-circle {
  width: 150px;
  height: 150px;
  border: 3px solid var(--color-primary);
  border-radius: 50%;
  background: var(--color-primary-50);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: var(--space-4) 0;
}

.circle-label {
  font-size: var(--font-size-xs);
  color: var(--color-primary);
  font-weight: var(--font-weight-semibold);
  background: white;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
}

/* Edit Notice */
.edit-notice {
  background: var(--blue-50);
  border: 1px solid var(--blue-200);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin: var(--space-4) 0;
}

.edit-notice i {
  color: var(--blue-600);
  font-size: 1.5rem;
}

.edit-notice p {
  margin: 0;
  color: var(--blue-900);
}

/* Maintenance Section */
.maintenance-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.maintenance-section h3 {
  margin: 0 0 var(--space-4) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.maintenance-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--space-4);
}

.maintenance-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.maintenance-card--urgent {
  border-color: var(--red-400);
  background: var(--red-50);
}

.maintenance-card__header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.maintenance-card__header i {
  color: var(--color-primary);
  font-size: 1.25rem;
}

.maintenance-card__header h4 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.maintenance-card__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.maintenance-date {
  margin: 0;
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

/* Related Information */
.related-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.related-section h3 {
  margin: 0 0 var(--space-4) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.related-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--space-4);
}

.related-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  transition: all var(--transition-base) var(--transition-timing);
}

.related-card:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.related-card__header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.related-card__header i {
  color: var(--color-primary);
  font-size: 1.25rem;
}

.related-card__header h4 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.related-card__content {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

/* Confirmation Dialog */
.confirmation-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) 0;
}

.confirmation-icon {
  font-size: 3rem;
  color: var(--orange-500);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header__actions {
    width: 100%;
  }

  .stats-cards {
    grid-template-columns: 1fr;
  }

  .info-cards {
    grid-template-columns: 1fr;
  }

  .maintenance-cards {
    grid-template-columns: 1fr;
  }

  .related-info {
    grid-template-columns: 1fr;
  }
}
</style>
