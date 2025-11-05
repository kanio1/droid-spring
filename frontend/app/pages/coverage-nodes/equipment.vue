<template>
  <div class="equipment-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <div class="breadcrumb">
          <NuxtLink to="/coverage-nodes" class="breadcrumb__link">Coverage Nodes</NuxtLink>
          <i class="pi pi-chevron-right breadcrumb__separator"></i>
          <NuxtLink :to="`/coverage-nodes/${nodeId}`" class="breadcrumb__link">{{ node?.name || 'Node' }}</NuxtLink>
          <i class="pi pi-chevron-right breadcrumb__separator"></i>
          <span class="breadcrumb__current">Equipment</span>
        </div>
        <h1 class="page-title">
          <i :class="node ? getTypeIcon(node.type) : ''" class="title-icon"></i>
          Equipment Management
        </h1>
        <p class="page-subtitle">
          {{ node ? `${node.name} (${node.city}, ${node.country})` : 'Loading node information...' }}
        </p>
      </div>
      <div class="page-header__actions">
        <NuxtLink :to="`/coverage-nodes/${nodeId}`">
          <Button label="Back to Node" icon="pi pi-arrow-left" severity="secondary" />
        </NuxtLink>
        <Button
          label="Add Equipment"
          icon="pi pi-plus"
          severity="primary"
          @click="handleAddEquipment"
        />
      </div>
    </div>

    <div v-if="loading && !node" class="loading-state">
      <ProgressSpinner />
      <p>Loading node details...</p>
    </div>

    <div v-else-if="error && !node" class="error-state">
      <i class="pi pi-exclamation-triangle"></i>
      <p>{{ error }}</p>
      <NuxtLink to="/coverage-nodes">
        <Button label="Back to List" severity="secondary" />
      </NuxtLink>
    </div>

    <div v-else-if="node" class="equipment-content">
      <!-- Equipment Stats -->
      <div class="equipment-stats">
        <div class="stat-card stat-card--primary">
          <div class="stat-card__icon">
            <i class="pi pi-box"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__value">{{ totalEquipment }}</div>
            <div class="stat-card__label">Total Equipment</div>
          </div>
        </div>

        <div class="stat-card stat-card--success">
          <div class="stat-card__icon">
            <i class="pi pi-check-circle"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__value">{{ activeEquipment }}</div>
            <div class="stat-card__label">Active</div>
          </div>
        </div>

        <div class="stat-card stat-card--warning">
          <div class="stat-card__icon">
            <i class="pi pi-wrench"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__value">{{ maintenanceEquipment }}</div>
            <div class="stat-card__label">Maintenance</div>
          </div>
        </div>

        <div class="stat-card stat-card--danger">
          <div class="stat-card__icon">
            <i class="pi pi-times-circle"></i>
          </div>
          <div class="stat-card__content">
            <div class="stat-card__value">{{ offlineEquipment }}</div>
            <div class="stat-card__label">Offline</div>
          </div>
        </div>
      </div>

      <!-- Filters -->
      <div class="equipment-filters">
        <div class="filters-row">
          <span class="p-input-icon-left search-input">
            <i class="pi pi-search" />
            <InputText
              v-model="searchTerm"
              placeholder="Search equipment..."
              style="width: 100%"
            />
          </span>

          <Dropdown
            v-model="statusFilter"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All Status"
            style="width: 180px"
          />

          <Dropdown
            v-model="typeFilter"
            :options="typeOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All Types"
            style="width: 180px"
          />

          <Button
            label="Clear Filters"
            icon="pi pi-filter-slash"
            severity="secondary"
            outlined
            @click="clearFilters"
          />
        </div>
      </div>

      <!-- Equipment Table -->
      <div class="equipment-table">
        <AppTable
          :columns="tableColumns"
          :data="filteredEquipment"
          :loading="loading"
          :clickable="true"
          @row-click="handleRowClick"
        >
          <!-- Custom cell templates -->
          <template #cell(type)="{ value }">
            <Tag :value="value" severity="info" />
          </template>

          <template #cell(status)="{ value }">
            <Tag :value="value" :severity="getStatusVariant(value)" />
          </template>

          <template #cell(serialNumber)="{ value }">
            <code class="serial-number">{{ value }}</code>
          </template>

          <template #cell(lastMaintenance)="{ value }">
            <span>{{ value ? formatDate(value) : 'N/A' }}</span>
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
                @click.stop="handleViewEquipment(row.id)"
              />
              <Button
                icon="pi pi-pencil"
                severity="info"
                size="small"
                text
                rounded
                v-tooltip.top="'Edit'"
                @click.stop="handleEditEquipment(row.id)"
              />
              <Button
                icon="pi pi-history"
                severity="warning"
                size="small"
                text
                rounded
                v-tooltip.top="'Maintenance History'"
                @click.stop="handleMaintenanceHistory(row.id)"
              />
              <Button
                icon="pi pi-trash"
                severity="danger"
                size="small"
                text
                rounded
                v-tooltip.top="'Remove'"
                @click.stop="handleDeleteEquipment(row.id)"
              />
            </div>
          </template>
        </AppTable>

        <!-- Empty State -->
        <div v-if="!loading && filteredEquipment.length === 0" class="empty-state">
          <div class="empty-state__icon">
            <i class="pi pi-box"></i>
          </div>
          <h3 class="empty-state__title">No equipment found</h3>
          <p class="empty-state__description">
            {{ searchTerm || statusFilter || typeFilter
              ? 'Try adjusting your filters or search terms'
              : 'Get started by adding equipment to this coverage node' }}
          </p>
          <div class="empty-state__actions">
            <Button
              label="Add Equipment"
              icon="pi pi-plus"
              severity="primary"
              @click="handleAddEquipment"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Equipment Modal -->
    <Dialog
      v-model:visible="showEquipmentDialog"
      modal
      :header="editingEquipment ? 'Edit Equipment' : 'Add Equipment'"
      :style="{ width: '600px' }"
    >
      <div class="equipment-form">
        <div class="form-field">
          <label for="equipmentName">Name <span class="required-mark">*</span></label>
          <InputText
            id="equipmentName"
            v-model="equipmentForm.name"
            placeholder="e.g., 5G Radio Unit"
            style="width: 100%"
          />
          <small v-if="equipmentErrors.name" class="p-error">{{ equipmentErrors.name }}</small>
        </div>

        <div class="form-field">
          <label for="equipmentType">Type <span class="required-mark">*</span></label>
          <Dropdown
            id="equipmentType"
            v-model="equipmentForm.type"
            :options="equipmentTypeOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Select type"
            style="width: 100%"
          />
          <small v-if="equipmentErrors.type" class="p-error">{{ equipmentErrors.type }}</small>
        </div>

        <div class="form-field">
          <label for="serialNumber">Serial Number <span class="required-mark">*</span></label>
          <InputText
            id="serialNumber"
            v-model="equipmentForm.serialNumber"
            placeholder="e.g., SN-12345-ABC"
            style="width: 100%"
          />
          <small v-if="equipmentErrors.serialNumber" class="p-error">{{ equipmentErrors.serialNumber }}</small>
        </div>

        <div class="form-field">
          <label for="manufacturer">Manufacturer</label>
          <InputText
            id="manufacturer"
            v-model="equipmentForm.manufacturer"
            placeholder="e.g., Huawei"
            style="width: 100%"
          />
        </div>

        <div class="form-field">
          <label for="model">Model</label>
          <InputText
            id="model"
            v-model="equipmentForm.model"
            placeholder="e.g., BBU5900"
            style="width: 100%"
          />
        </div>

        <div class="form-field">
          <label for="status">Status</label>
          <Dropdown
            id="status"
            v-model="equipmentForm.status"
            :options="statusOptions.slice(1)"
            optionLabel="label"
            optionValue="value"
            placeholder="Select status"
            style="width: 100%"
          />
        </div>
      </div>
      <template #footer>
        <Button
          label="Cancel"
          icon="pi pi-times"
          severity="secondary"
          @click="showEquipmentDialog = false"
        />
        <Button
          :label="editingEquipment ? 'Update' : 'Add'"
          :icon="editingEquipment ? 'pi pi-check' : 'pi pi-plus'"
          severity="primary"
          :loading="saving"
          @click="handleSaveEquipment"
        />
      </template>
    </Dialog>

    <!-- Delete Confirmation Dialog -->
    <Dialog
      v-model:visible="showDeleteDialog"
      modal
      header="Confirm Delete"
      :style="{ width: '450px' }"
    >
      <div class="confirmation-content">
        <i class="pi pi-exclamation-triangle confirmation-icon" />
        <p>Are you sure you want to remove this equipment?</p>
        <p class="text-sm text-gray-600 mt-2">{{ equipmentToDelete?.name }} ({{ equipmentToDelete?.serialNumber }})</p>
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
import { useRoute, useRouter } from 'vue-router'
import { useCoverageNodeStore } from '~/stores/coverage-node'
import { useToast } from 'primevue/usetoast'
import type { CoverageNode } from '~/schemas/coverage-node'
import { getTypeIcon } from '~/schemas/coverage-node'

// Meta
definePageMeta({
  title: 'Equipment Management'
})

// Route & Stores
const route = useRoute()
const router = useRouter()
const nodeStore = useCoverageNodeStore()
const toast = useToast()

// Reactive State
const loading = ref(false)
const error = ref<string | null>(null)
const node = ref<CoverageNode | null>(null)
const searchTerm = ref('')
const statusFilter = ref('')
const typeFilter = ref('')
const showEquipmentDialog = ref(false)
const showDeleteDialog = ref(false)
const editingEquipment = ref<any>(null)
const equipmentToDelete = ref<any>(null)
const saving = ref(false)

// Mock equipment data (in real app, would be from API)
const equipment = ref<any[]>([
  {
    id: '1',
    name: '5G Radio Unit',
    type: 'RADIO',
    status: 'ACTIVE',
    serialNumber: 'RU-2024-001',
    manufacturer: 'Ericsson',
    model: 'Radio 4443',
    lastMaintenance: '2024-10-15',
    nextMaintenance: '2025-01-15'
  },
  {
    id: '2',
    name: 'Base Band Unit',
    type: 'PROCESSING',
    status: 'ACTIVE',
    serialNumber: 'BBU-2024-045',
    manufacturer: 'Nokia',
    model: 'AirScale BBU',
    lastMaintenance: '2024-09-20',
    nextMaintenance: '2024-12-20'
  },
  {
    id: '3',
    name: 'Antenna System',
    type: 'ANTENNA',
    status: 'MAINTENANCE',
    serialNumber: 'ANT-2023-234',
    manufacturer: 'Kathrein',
    model: '800 10764',
    lastMaintenance: '2024-11-01',
    nextMaintenance: '2024-11-10'
  },
  {
    id: '4',
    name: 'GPS Clock',
    type: 'TIMING',
    status: 'ACTIVE',
    serialNumber: 'GPS-2024-012',
    manufacturer: 'Microsemi',
    model: 'SyncServer S600',
    lastMaintenance: '2024-08-10',
    nextMaintenance: '2025-02-10'
  },
  {
    id: '5',
    name: 'Power Amplifier',
    type: 'POWER',
    status: 'OFFLINE',
    serialNumber: 'PA-2023-156',
    manufacturer: 'Huawei',
    model: 'RTN380H',
    lastMaintenance: '2024-05-15',
    nextMaintenance: 'Overdue'
  }
])

const equipmentForm = ref({
  name: '',
  type: '',
  status: 'ACTIVE',
  serialNumber: '',
  manufacturer: '',
  model: ''
})

const equipmentErrors = ref<Record<string, string>>({})

// Computed
const nodeId = computed(() => route.params.id as string)

const filteredEquipment = computed(() => {
  let filtered = equipment.value

  if (searchTerm.value) {
    const term = searchTerm.value.toLowerCase()
    filtered = filtered.filter(e =>
      e.name.toLowerCase().includes(term) ||
      e.serialNumber.toLowerCase().includes(term) ||
      (e.manufacturer && e.manufacturer.toLowerCase().includes(term)) ||
      (e.model && e.model.toLowerCase().includes(term))
    )
  }

  if (statusFilter.value) {
    filtered = filtered.filter(e => e.status === statusFilter.value)
  }

  if (typeFilter.value) {
    filtered = filtered.filter(e => e.type === typeFilter.value)
  }

  return filtered
})

const totalEquipment = computed(() => equipment.value.length)
const activeEquipment = computed(() => equipment.value.filter(e => e.status === 'ACTIVE').length)
const maintenanceEquipment = computed(() => equipment.value.filter(e => e.status === 'MAINTENANCE').length)
const offlineEquipment = computed(() => equipment.value.filter(e => e.status === 'OFFLINE').length)

// Options
const statusOptions = [
  { label: 'All Status', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Maintenance', value: 'MAINTENANCE' },
  { label: 'Offline', value: 'OFFLINE' },
  { label: 'Planned', value: 'PLANNED' }
]

const typeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Radio', value: 'RADIO' },
  { label: 'Processing', value: 'PROCESSING' },
  { label: 'Antenna', value: 'ANTENNA' },
  { label: 'Timing', value: 'TIMING' },
  { label: 'Power', value: 'POWER' },
  { label: 'Cooling', value: 'COOLING' },
  { label: 'Monitoring', value: 'MONITORING' }
]

const equipmentTypeOptions = [
  { label: 'Radio Unit', value: 'RADIO' },
  { label: 'Processing Unit', value: 'PROCESSING' },
  { label: 'Antenna', value: 'ANTENNA' },
  { label: 'Timing System', value: 'TIMING' },
  { label: 'Power Supply', value: 'POWER' },
  { label: 'Cooling System', value: 'COOLING' },
  { label: 'Monitoring', value: 'MONITORING' }
]

// Table Columns
const tableColumns = [
  { key: 'name', label: 'Equipment Name', sortable: true, align: 'left', width: '200px' },
  { key: 'type', label: 'Type', sortable: true, align: 'center', width: '120px' },
  { key: 'status', label: 'Status', sortable: true, align: 'center', width: '120px' },
  { key: 'serialNumber', label: 'Serial Number', sortable: true, align: 'center', width: '150px' },
  { key: 'manufacturer', label: 'Manufacturer', sortable: true, align: 'left', width: '150px' },
  { key: 'model', label: 'Model', sortable: true, align: 'left', width: '150px' },
  { key: 'lastMaintenance', label: 'Last Maintenance', sortable: true, align: 'center', width: '150px' },
  { key: 'actions', label: 'Actions', sortable: false, align: 'center', width: '140px' }
]

// Helper Functions
function formatDate(dateString: string): string {
  const date = new Date(dateString)
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  }).format(date)
}

function getStatusVariant(status: string): 'success' | 'neutral' | 'warning' | 'info' | 'danger' {
  const variants: Record<string, 'success' | 'neutral' | 'warning' | 'info' | 'danger'> = {
    'ACTIVE': 'success',
    'MAINTENANCE': 'warning',
    'OFFLINE': 'danger',
    'PLANNED': 'info'
  }
  return variants[status] || 'neutral'
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

function clearFilters() {
  searchTerm.value = ''
  statusFilter.value = ''
  typeFilter.value = ''
}

function handleAddEquipment() {
  editingEquipment.value = null
  equipmentForm.value = {
    name: '',
    type: '',
    status: 'ACTIVE',
    serialNumber: '',
    manufacturer: '',
    model: ''
  }
  equipmentErrors.value = {}
  showEquipmentDialog.value = true
}

function handleEditEquipment(id: string) {
  const item = equipment.value.find(e => e.id === id)
  if (!item) return

  editingEquipment.value = item
  equipmentForm.value = {
    name: item.name,
    type: item.type,
    status: item.status,
    serialNumber: item.serialNumber,
    manufacturer: item.manufacturer,
    model: item.model
  }
  equipmentErrors.value = {}
  showEquipmentDialog.value = true
}

function handleSaveEquipment() {
  equipmentErrors.value = {}

  if (!equipmentForm.value.name) {
    equipmentErrors.value.name = 'Name is required'
  }

  if (!equipmentForm.value.type) {
    equipmentErrors.value.type = 'Type is required'
  }

  if (!equipmentForm.value.serialNumber) {
    equipmentErrors.value.serialNumber = 'Serial number is required'
  }

  if (Object.keys(equipmentErrors.value).length > 0) {
    toast.add({
      severity: 'error',
      summary: 'Validation Error',
      detail: 'Please correct the errors in the form',
      life: 5000
    })
    return
  }

  saving.value = true
  setTimeout(() => {
    if (editingEquipment.value) {
      // Update existing
      const index = equipment.value.findIndex(e => e.id === editingEquipment.value.id)
      if (index !== -1) {
        equipment.value[index] = {
          ...editingEquipment.value,
          ...equipmentForm.value
        }
      }
      toast.add({
        severity: 'success',
        summary: 'Success',
        detail: 'Equipment updated successfully',
        life: 5000
      })
    } else {
      // Add new
      const newItem = {
        id: String(equipment.value.length + 1),
        ...equipmentForm.value,
        lastMaintenance: new Date().toISOString().split('T')[0],
        nextMaintenance: ''
      }
      equipment.value.push(newItem)
      toast.add({
        severity: 'success',
        summary: 'Success',
        detail: 'Equipment added successfully',
        life: 5000
      })
    }

    showEquipmentDialog.value = false
    saving.value = false
  }, 1000)
}

function handleViewEquipment(id: string) {
  toast.add({
    severity: 'info',
    summary: 'View Details',
    detail: `Viewing equipment ${id}`,
    life: 3000
  })
}

function handleMaintenanceHistory(id: string) {
  toast.add({
    severity: 'info',
    summary: 'Maintenance History',
    detail: `Maintenance history for equipment ${id}`,
    life: 3000
  })
}

function handleDeleteEquipment(id: string) {
  equipmentToDelete.value = equipment.value.find(e => e.id === id)
  showDeleteDialog.value = true
}

function confirmDelete() {
  if (!equipmentToDelete.value) return

  loading.value = true
  setTimeout(() => {
    const index = equipment.value.findIndex(e => e.id === equipmentToDelete.value.id)
    if (index !== -1) {
      equipment.value.splice(index, 1)
    }

    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Equipment removed successfully',
      life: 5000
    })

    showDeleteDialog.value = false
    equipmentToDelete.value = null
    loading.value = false
  }, 1000)
}

function handleRowClick(row: any) {
  handleViewEquipment(row.id)
}

// Lifecycle
onMounted(async () => {
  await fetchNode()
})
</script>

<style scoped>
.equipment-page {
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

/* Equipment Stats */
.equipment-stats {
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

/* Filters */
.equipment-filters {
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

/* Table */
.equipment-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.serial-number {
  font-family: 'Monaco', 'Menlo', monospace;
  background: var(--color-gray-100);
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
}

.action-buttons {
  display: flex;
  gap: var(--space-1);
  justify-content: center;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: var(--space-12);
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

/* Equipment Form */
.equipment-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-4) 0;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-field label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.required-mark {
  color: var(--red-500);
  font-weight: var(--font-weight-bold);
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

  .equipment-stats {
    grid-template-columns: 1fr;
  }

  .filters-row {
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .action-buttons {
    flex-direction: column;
  }
}
</style>
