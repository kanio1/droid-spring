<template>
  <div class="coverage-nodes-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Coverage Nodes</h1>
        <p class="page-subtitle">
          Manage network infrastructure and coverage areas
        </p>
      </div>
      <div class="page-header__actions">
        <NuxtLink to="/coverage-nodes/create">
          <Button
            label="Add Node"
            icon="pi pi-plus"
            severity="primary"
          />
        </NuxtLink>
      </div>
    </div>

    <!-- View Toggle -->
    <div class="view-toggle">
      <Button
        :class="{ 'p-button-outlined': viewMode !== 'table' }"
        label="Table View"
        icon="pi pi-list"
        severity="secondary"
        @click="viewMode = 'table'"
      />
      <Button
        :class="{ 'p-button-outlined': viewMode !== 'map' }"
        label="Map View"
        icon="pi pi-map"
        severity="secondary"
        @click="viewMode = 'map'"
      />
    </div>

    <!-- Filters and Search -->
    <div class="coverage-nodes-filters">
      <div class="filters-row">
        <span class="p-input-icon-left search-input">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search nodes..."
            @input="handleSearch"
            style="width: 100%"
          />
        </span>

        <Dropdown
          v-model="typeFilter"
          :options="typeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Types"
          style="width: 180px"
          @change="handleTypeFilter"
        />

        <Dropdown
          v-model="statusFilter"
          :options="statusOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Status"
          style="width: 150px"
          @change="handleStatusFilter"
        />

        <Dropdown
          v-model="technologyFilter"
          :options="technologyOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Technologies"
          style="width: 180px"
          @change="handleTechnologyFilter"
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

    <!-- Coverage Statistics -->
    <div class="coverage-stats">
      <div class="stat-card">
        <div class="stat-card__icon">
          <i class="pi pi-sitemap"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ paginationProps.total }}</div>
          <div class="stat-card__label">Total Nodes</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--success">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ activeNodes.length }}</div>
          <div class="stat-card__label">Active</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--warning">
          <i class="pi pi-wrench"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ maintenanceNodes.length }}</div>
          <div class="stat-card__label">Maintenance</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--danger">
          <i class="pi pi-exclamation-triangle"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ overloadedNodes.length }}</div>
          <div class="stat-card__label">Overloaded</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--info">
          <i class="pi pi-chart-bar"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ averageCapacity }}%</div>
          <div class="stat-card__label">Avg Capacity</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--purple">
          <i class="pi pi-compass"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ totalCoverageArea.toFixed(0) }}</div>
          <div class="stat-card__label">Coverage Area (km²)</div>
        </div>
      </div>
    </div>

    <!-- Table View -->
    <div v-if="viewMode === 'table'" class="coverage-nodes-table">
      <AppTable
        :columns="tableColumns"
        :data="nodes"
        :loading="loading"
        :pagination="paginationProps"
        :clickable="true"
        @row-click="handleRowClick"
        @page-change="handlePageChange"
      >
        <!-- Custom cell templates -->
        <template #cell(type)="{ value, row }">
          <div class="type-cell">
            <i :class="getTypeIcon(value)" class="type-icon"></i>
            <Tag :value="value" severity="secondary" />
          </div>
        </template>

        <template #cell(status)="{ value }">
          <Tag :value="value" :severity="getStatusVariant(value)" />
        </template>

        <template #cell(technology)="{ value }">
          <Tag :value="value" severity="info" />
        </template>

        <template #cell(location)="{ row }">
          <div class="location-cell">
            <div class="location-cell__city">{{ row.city }}</div>
            <div class="location-cell__coordinates">{{ formatCoordinates(row) }}</div>
          </div>
        </template>

        <template #cell(coverage)="{ row }">
          <div class="coverage-cell">
            <div class="coverage-cell__radius">{{ row.coverageRadius }} km radius</div>
            <div class="coverage-cell__area">{{ formatCoverageArea(row.coverageRadius) }}</div>
          </div>
        </template>

        <template #cell(capacity)="{ row }">
          <div class="capacity-cell">
            <div class="capacity-cell__text">{{ formatCapacity(row.currentLoad, row.maxCapacity) }}</div>
            <ProgressBar :value="row.capacityPercentage" :showValue="false" style="height: 6px" />
          </div>
        </template>

        <template #cell(equipmentCount)="{ value }">
          <div class="equipment-cell">
            <i class="pi pi-box equipment-icon"></i>
            <span>{{ value }}</span>
          </div>
        </template>

        <template #cell(actions)="{ row }">
          <div class="action-buttons">
            <Button
              icon="pi pi-eye"
              severity="secondary"
              size="small"
              text
              rounded
              v-tooltip.top="'View Details'"
              @click.stop="handleViewNode(row.id)"
            />
            <Button
              icon="pi pi-pencil"
              severity="info"
              size="small"
              text
              rounded
              v-tooltip.top="'Edit'"
              @click.stop="handleEditNode(row.id)"
            />
            <Button
              icon="pi pi-cog"
              severity="warning"
              size="small"
              text
              rounded
              v-tooltip.top="'Equipment'"
              @click.stop="handleEquipment(row.id)"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              size="small"
              text
              rounded
              v-tooltip.top="'Delete'"
              @click.stop="handleDeleteNode(row.id)"
            />
          </div>
        </template>
      </AppTable>
    </div>

    <!-- Map View -->
    <div v-else class="coverage-nodes-map">
      <div class="map-placeholder">
        <i class="pi pi-map map-icon"></i>
        <h3>Interactive Map View</h3>
        <p>Geographic visualization of coverage nodes</p>
        <div class="map-features">
          <div class="map-feature">
            <i class="pi pi-circle-fill text-green-500"></i>
            <span>Active Nodes</span>
          </div>
          <div class="map-feature">
            <i class="pi pi-circle-fill text-orange-500"></i>
            <span>Maintenance</span>
          </div>
          <div class="map-feature">
            <i class="pi pi-circle-fill text-gray-400"></i>
            <span>Inactive</span>
          </div>
        </div>
        <p class="text-sm text-gray-600 mt-4">
          Map integration placeholder - Ready for Leaflet or Google Maps
        </p>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && nodes.length === 0" class="empty-state">
      <div class="empty-state__icon">
        <i class="pi pi-sitemap"></i>
      </div>
      <h3 class="empty-state__title">No coverage nodes found</h3>
      <p class="empty-state__description">
        {{ searchTerm || typeFilter || statusFilter || technologyFilter
          ? 'Try adjusting your filters or search terms'
          : 'Get started by adding your first coverage node' }}
      </p>
      <div class="empty-state__actions">
        <NuxtLink to="/coverage-nodes/create">
          <Button label="Add Node" icon="pi pi-plus" severity="primary" />
        </NuxtLink>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCoverageNodeStore } from '~/stores/coverage-node'
import { useToast } from 'primevue/usetoast'
import type { CoverageNodeType, CoverageNodeStatus, Technology } from '~/schemas/coverage-node'

// Meta
definePageMeta({
  title: 'Coverage Nodes'
})

// Stores & Composables
const nodeStore = useCoverageNodeStore()
const router = useRouter()
const toast = useToast()

// Reactive State
const viewMode = ref<'table' | 'map'>('table')
const searchTerm = ref('')
const typeFilter = ref<CoverageNodeType | ''>('')
const statusFilter = ref<CoverageNodeStatus | ''>('')
const technologyFilter = ref<Technology | ''>('')
const sortOption = ref('createdAt,desc')
const showDeleteDialog = ref(false)
const nodeToDelete = ref<string | null>(null)

// Computed
const nodes = computed(() => nodeStore.nodes)
const loading = computed(() => nodeStore.loading)

const activeNodes = computed(() => nodeStore.activeNodes)
const maintenanceNodes = computed(() => nodeStore.maintenanceNodes)
const overloadedNodes = computed(() => nodeStore.overloadedNodes)
const healthyNodes = computed(() => nodeStore.healthyNodes)
const averageCapacity = computed(() => nodeStore.averageCapacity)
const totalCoverageArea = computed(() => nodeStore.totalCoverageArea)

const paginationProps = computed(() => nodeStore.pagination)

// Filter Options
const typeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Cell Tower', value: 'CELL_TOWER' },
  { label: 'Satellite', value: 'SATELLITE' },
  { label: 'Fiber Hub', value: 'FIBER_HUB' },
  { label: 'WiFi Hotspot', value: 'WIFI_HOTSPOT' },
  { label: 'Microwave', value: 'MICROWAVE' },
  { label: 'Data Center', value: 'DATA_CENTER' },
  { label: 'Exchange Point', value: 'EXCHANGE_POINT' }
]

const statusOptions = [
  { label: 'All Status', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Maintenance', value: 'MAINTENANCE' },
  { label: 'Planned', value: 'PLANNED' },
  { label: 'Decommissioned', value: 'DECOMMISSIONED' }
]

const technologyOptions = [
  { label: 'All Technologies', value: '' },
  { label: '2G', value: '2G' },
  { label: '3G', value: '3G' },
  { label: '4G', value: '4G' },
  { label: '5G', value: '5G' },
  { label: 'LTE', value: 'LTE' },
  { label: 'WiFi', value: 'WIFI' },
  { label: 'Fiber', value: 'FIBER' },
  { label: 'Satellite', value: 'SATELLITE' },
  { label: 'Microwave', value: 'MICROWAVE' }
]

const sortOptions = [
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' },
  { label: 'Name A-Z', value: 'name,asc' },
  { label: 'Name Z-A', value: 'name,desc' },
  { label: 'Capacity High-Low', value: 'capacityPercentage,desc' },
  { label: 'City A-Z', value: 'city,asc' }
]

// Table Columns
const tableColumns = [
  { key: 'name', label: 'Name', sortable: true, align: 'left', width: '150px' },
  { key: 'code', label: 'Code', sortable: true, align: 'center', width: '100px' },
  { key: 'type', label: 'Type', sortable: true, align: 'center', width: '130px' },
  { key: 'status', label: 'Status', sortable: true, align: 'center', width: '120px' },
  { key: 'technology', label: 'Tech', sortable: true, align: 'center', width: '100px' },
  { key: 'location', label: 'Location', sortable: false, align: 'left', width: '200px' },
  { key: 'coverage', label: 'Coverage', sortable: false, align: 'center', width: '150px' },
  { key: 'capacity', label: 'Capacity', sortable: true, align: 'center', width: '150px' },
  { key: 'equipmentCount', label: 'Equip', sortable: true, align: 'center', width: '80px' },
  { key: 'actions', label: 'Actions', sortable: false, align: 'center', width: '140px' }
]

// Helper Functions (import from schema)
import {
  getStatusVariant,
  getTypeIcon,
  formatCoordinates,
  formatCoverageArea,
  formatCapacity
} from '~/schemas/coverage-node'

// Event Handlers
let searchTimeout: NodeJS.Timeout | null = null

const handleSearch = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(async () => {
    await fetchNodes()
  }, 300)
}

const handleTypeFilter = async () => {
  await fetchNodes()
}

const handleStatusFilter = async () => {
  await fetchNodes()
}

const handleTechnologyFilter = async () => {
  await fetchNodes()
}

const handleSortChange = async () => {
  await fetchNodes()
}

async function fetchNodes() {
  try {
    await nodeStore.fetchNodes({
      page: 0,
      searchTerm: searchTerm.value || undefined,
      type: typeFilter.value || undefined,
      status: statusFilter.value || undefined,
      technology: technologyFilter.value || undefined,
      sort: sortOption.value
    })
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to fetch coverage nodes',
      life: 5000
    })
  }
}

function handleRowClick(row: any) {
  handleViewNode(row.id)
}

function handleViewNode(id: string) {
  router.push(`/coverage-nodes/${id}`)
}

function handleEditNode(id: string) {
  router.push(`/coverage-nodes/${id}?edit=true`)
}

function handleEquipment(id: string) {
  router.push(`/coverage-nodes/${id}/equipment`)
}

function handleDeleteNode(id: string) {
  nodeToDelete.value = id
  showDeleteDialog.value = true
}

async function confirmDelete() {
  if (!nodeToDelete.value) return

  try {
    await nodeStore.deleteNode(nodeToDelete.value)
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Coverage node deleted successfully',
      life: 5000
    })
    showDeleteDialog.value = false
    nodeToDelete.value = null

    // Refresh the list
    await fetchNodes()
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to delete coverage node',
      life: 5000
    })
  }
}

async function handlePageChange({ page, size }: { page: number, size: number }) {
  nodeStore.setPage(page)
  nodeStore.setSize(size)
  await fetchNodes()
}

// Lifecycle
onMounted(async () => {
  await fetchNodes()
})
</script>

<style scoped>
.coverage-nodes-page {
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

/* View Toggle */
.view-toggle {
  display: flex;
  gap: var(--space-2);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-2);
  width: fit-content;
}

/* Filters */
.coverage-nodes-filters {
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

.search-input {
  flex: 1;
  min-width: 250px;
}

/* Stats */
.coverage-stats {
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
  align-items: center;
  gap: var(--space-3);
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
}

.stat-card__icon--success {
  background: var(--green-100);
  color: var(--green-600);
}

.stat-card__icon--warning {
  background: var(--orange-100);
  color: var(--orange-600);
}

.stat-card__icon--danger {
  background: var(--red-100);
  color: var(--red-600);
}

.stat-card__icon--info {
  background: var(--blue-100);
  color: var(--blue-600);
}

.stat-card__icon--purple {
  background: var(--purple-100);
  color: var(--purple-600);
}

.stat-card__content {
  flex: 1;
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

/* Table */
.coverage-nodes-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.type-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.type-icon {
  font-size: 1.2rem;
  color: var(--color-primary);
}

.location-cell__city {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.location-cell__coordinates {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.coverage-cell__radius {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.coverage-cell__area {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.capacity-cell {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.capacity-cell__text {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
}

.equipment-cell {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-weight: var(--font-weight-medium);
}

.equipment-icon {
  color: var(--color-primary);
}

.action-buttons {
  display: flex;
  gap: var(--space-1);
  justify-content: center;
}

/* Map View */
.coverage-nodes-map {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  min-height: 600px;
}

.map-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 600px;
  background: var(--color-gray-50);
}

.map-icon {
  font-size: 5rem;
  color: var(--color-primary);
  opacity: 0.6;
  margin-bottom: var(--space-4);
}

.map-placeholder h3 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.map-placeholder > p {
  margin: 0 0 var(--space-6) 0;
  color: var(--color-text-secondary);
}

.map-features {
  display: flex;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.map-feature {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: var(--space-12);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.empty-state__icon {
  font-size: 4rem;
  color: var(--color-text-secondary);
  opacity: 0.5;
  margin-bottom: var(--space-4);
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
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
}

.empty-state__actions {
  display: flex;
  gap: var(--space-3);
  justify-content: center;
}

/* Confirmation Dialog */
.confirmation-content {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) 0;
}

.confirmation-icon {
  font-size: 2rem;
  color: var(--orange-500);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .view-toggle {
    width: 100%;
  }

  .filters-row {
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .coverage-stats {
    grid-template-columns: 1fr;
  }

  .stat-card {
    padding: var(--space-3);
  }

  .map-features {
    flex-direction: column;
    gap: var(--space-2);
  }
}
</style>
