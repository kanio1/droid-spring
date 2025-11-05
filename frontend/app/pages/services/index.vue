<template>
  <div class="services-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Service Catalog</h1>
        <p class="page-subtitle">
          Manage service offerings and pricing
        </p>
      </div>
      <div class="page-header__actions">
        <NuxtLink to="/services/activations">
          <Button
            label="Activations"
            icon="pi pi-list"
            severity="secondary"
          />
        </NuxtLink>
        <NuxtLink to="/services/create">
          <Button
            label="Add Service"
            icon="pi pi-plus"
            severity="primary"
          />
        </NuxtLink>
      </div>
    </div>

    <!-- Service Statistics -->
    <div class="service-stats">
      <div class="stat-card">
        <div class="stat-card__icon">
          <i class="pi pi-cog"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ paginationProps.total }}</div>
          <div class="stat-card__label">Total Services</div>
        </div>
      </div>

      <div class="stat-card stat-card--success">
        <div class="stat-card__icon">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ activeServices.length }}</div>
          <div class="stat-card__label">Active</div>
        </div>
      </div>

      <div class="stat-card stat-card--warning">
        <div class="stat-card__icon">
          <i class="pi pi-clock"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ plannedServices.length }}</div>
          <div class="stat-card__label">Planned</div>
        </div>
      </div>

      <div class="stat-card stat-card--info">
        <div class="stat-card__icon">
          <i class="pi pi-dollar"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ formatPrice(averagePrice, 'USD') }}</div>
          <div class="stat-card__label">Avg Price</div>
        </div>
      </div>

      <div class="stat-card stat-card--purple">
        <div class="stat-card__icon">
          <i class="pi pi-chart-line"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ formatPrice(totalRevenue, 'USD') }}</div>
          <div class="stat-card__label">Total Revenue</div>
        </div>
      </div>

      <div class="stat-card stat-card--neutral">
        <div class="stat-card__icon">
          <i class="pi pi-users"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ totalCustomers }}</div>
          <div class="stat-card__label">Total Customers</div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="services-filters">
      <div class="filters-row">
        <span class="p-input-icon-left search-input">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search services..."
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

    <!-- Services Table -->
    <div class="services-table">
      <AppTable
        :columns="tableColumns"
        :data="services"
        :loading="loading"
        :pagination="paginationProps"
        :clickable="true"
        @row-click="handleRowClick"
        @page-change="handlePageChange"
      >
        <!-- Custom cell templates -->
        <template #cell(type)="{ value }">
          <div class="type-cell">
            <i :class="getTypeIcon(value)" class="type-icon"></i>
            <Tag :value="value" severity="secondary" />
          </div>
        </template>

        <template #cell(status)="{ value }">
          <Tag :value="value" :severity="getStatusVariant(value)" />
        </template>

        <template #cell(category)="{ value }">
          <Tag :value="value" severity="info" />
        </template>

        <template #cell(technology)="{ value }">
          <Tag :value="value" severity="success" />
        </template>

        <template #cell(price)="{ row }">
          <div class="price-cell">
            <div class="price-cell__main">{{ formatPrice(row.price, row.currency) }}</div>
            <div class="price-cell__cycle">{{ getBillingCycleLabel(row.billingCycle) }}</div>
          </div>
        </template>

        <template #cell(dataLimit)="{ value }">
          <div class="data-cell">
            <span>{{ formatDataLimit(value) }}</span>
          </div>
        </template>

        <template #cell(speed)="{ value }">
          <div class="speed-cell">
            <span>{{ formatSpeed(value) }}</span>
          </div>
        </template>

        <template #cell(activeCustomerCount)="{ value }">
          <div class="customers-cell">
            <i class="pi pi-users"></i>
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
              @click.stop="handleViewService(row.id)"
            />
            <Button
              icon="pi pi-pencil"
              severity="info"
              size="small"
              text
              rounded
              v-tooltip.top="'Edit'"
              @click.stop="handleEditService(row.id)"
            />
            <Button
              icon="pi pi-play"
              severity="success"
              size="small"
              text
              rounded
              v-tooltip.top="'Activate for Customer'"
              @click.stop="handleActivateService(row.id)"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              size="small"
              text
              rounded
              v-tooltip.top="'Delete'"
              @click.stop="handleDeleteService(row.id)"
            />
          </div>
        </template>
      </AppTable>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && services.length === 0" class="empty-state">
      <div class="empty-state__icon">
        <i class="pi pi-cog"></i>
      </div>
      <h3 class="empty-state__title">No services found</h3>
      <p class="empty-state__description">
        {{ searchTerm || typeFilter || categoryFilter || statusFilter || technologyFilter
          ? 'Try adjusting your filters or search terms'
          : 'Get started by adding your first service' }}
      </p>
      <div class="empty-state__actions">
        <NuxtLink to="/services/create">
          <Button label="Add Service" icon="pi pi-plus" severity="primary" />
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
        <p>Are you sure you want to delete this service?</p>
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
import { useServiceStore } from '~/stores/service'
import { useToast } from 'primevue/usetoast'
import type { ServiceType, ServiceStatus, ServiceCategory, ServiceTechnology } from '~/schemas/service'

// Meta
definePageMeta({
  title: 'Service Catalog'
})

// Stores & Composables
const serviceStore = useServiceStore()
const router = useRouter()
const toast = useToast()

// Reactive State
const searchTerm = ref('')
const typeFilter = ref<ServiceType | ''>('')
const categoryFilter = ref<ServiceCategory | ''>('')
const statusFilter = ref<ServiceStatus | ''>('')
const technologyFilter = ref<ServiceTechnology | ''>('')
const sortOption = ref('createdAt,desc')
const showDeleteDialog = ref(false)
const serviceToDelete = ref<string | null>(null)

// Computed
const services = computed(() => serviceStore.services)
const loading = computed(() => serviceStore.loading)

const activeServices = computed(() => serviceStore.activeServices)
const plannedServices = computed(() => serviceStore.plannedServices)
const averagePrice = computed(() => serviceStore.averagePrice)
const totalRevenue = computed(() => serviceStore.totalRevenue)
const totalCustomers = computed(() =>
  services.value.reduce((acc, s) => acc + s.activeCustomerCount, 0)
)

const paginationProps = computed(() => serviceStore.pagination)

// Filter Options
const typeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Internet', value: 'INTERNET' },
  { label: 'Voice', value: 'VOICE' },
  { label: 'Television', value: 'TELEVISION' },
  { label: 'Mobile', value: 'MOBILE' },
  { label: 'Cloud Services', value: 'CLOUD_SERVICES' },
  { label: 'IoT', value: 'IoT' },
  { label: 'VPN', value: 'VPN' },
  { label: 'CDN', value: 'CDN' },
  { label: 'Security', value: 'SECURITY' },
  { label: 'Consulting', value: 'CONSULTING' }
]

const categoryOptions = [
  { label: 'All Categories', value: '' },
  { label: 'Broadband', value: 'BROADBAND' },
  { label: 'Voice', value: 'VOICE' },
  { label: 'Video', value: 'VIDEO' },
  { label: 'Mobile', value: 'MOBILE' },
  { label: 'Cloud', value: 'CLOUD' },
  { label: 'Enterprise', value: 'ENTERPRISE' },
  { label: 'Emerging', value: 'EMERGING' }
]

const statusOptions = [
  { label: 'All Status', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Planned', value: 'PLANNED' },
  { label: 'Deprecated', value: 'DEPRECATED' },
  { label: 'Suspended', value: 'SUSPENDED' }
]

const technologyOptions = [
  { label: 'All Technologies', value: '' },
  { label: 'DSL', value: 'DSL' },
  { label: 'Fiber', value: 'FIBER' },
  { label: 'Cable', value: 'CABLE' },
  { label: '4G', value: '4G' },
  { label: '5G', value: '5G' },
  { label: 'WiFi', value: 'WIFI' },
  { label: 'Satellite', value: 'SATELLITE' },
  { label: 'Ethernet', value: 'ETHERNET' },
  { label: 'VoIP', value: 'VOIP' },
  { label: 'Cloud Native', value: 'CLOUD_NATIVE' }
]

const sortOptions = [
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' },
  { label: 'Name A-Z', value: 'name,asc' },
  { label: 'Name Z-A', value: 'name,desc' },
  { label: 'Price Low-High', value: 'price,asc' },
  { label: 'Price High-Low', value: 'price,desc' },
  { label: 'Popularity', value: 'activeCustomerCount,desc' }
]

// Table Columns
const tableColumns = [
  { key: 'name', label: 'Name', sortable: true, align: 'left', width: '200px' },
  { key: 'code', label: 'Code', sortable: true, align: 'center', width: '120px' },
  { key: 'type', label: 'Type', sortable: true, align: 'center', width: '130px' },
  { key: 'category', label: 'Category', sortable: true, align: 'center', width: '120px' },
  { key: 'status', label: 'Status', sortable: true, align: 'center', width: '120px' },
  { key: 'technology', label: 'Tech', sortable: true, align: 'center', width: '120px' },
  { key: 'price', label: 'Price', sortable: true, align: 'center', width: '130px' },
  { key: 'dataLimit', label: 'Data', sortable: false, align: 'center', width: '120px' },
  { key: 'speed', label: 'Speed', sortable: false, align: 'center', width: '100px' },
  { key: 'activeCustomerCount', label: 'Customers', sortable: true, align: 'center', width: '110px' },
  { key: 'actions', label: 'Actions', sortable: false, align: 'center', width: '160px' }
]

// Helper Functions (import from schema)
import {
  getStatusVariant,
  getTypeIcon,
  formatPrice,
  formatDataLimit,
  formatSpeed,
  getBillingCycleLabel
} from '~/schemas/service'

// Event Handlers
let searchTimeout: NodeJS.Timeout | null = null

const handleSearch = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(async () => {
    await fetchServices()
  }, 300)
}

const handleTypeFilter = async () => {
  await fetchServices()
}

const handleCategoryFilter = async () => {
  await fetchServices()
}

const handleStatusFilter = async () => {
  await fetchServices()
}

const handleTechnologyFilter = async () => {
  await fetchServices()
}

const handleSortChange = async () => {
  await fetchServices()
}

async function fetchServices() {
  try {
    await serviceStore.fetchServices({
      page: 0,
      searchTerm: searchTerm.value || undefined,
      type: typeFilter.value || undefined,
      category: categoryFilter.value || undefined,
      status: statusFilter.value || undefined,
      technology: technologyFilter.value || undefined,
      sort: sortOption.value
    })
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to fetch services',
      life: 5000
    })
  }
}

function handleRowClick(row: any) {
  handleViewService(row.id)
}

function handleViewService(id: string) {
  router.push(`/services/${id}`)
}

function handleEditService(id: string) {
  router.push(`/services/${id}?edit=true`)
}

function handleActivateService(id: string) {
  router.push(`/services/activate?serviceId=${id}`)
}

function handleDeleteService(id: string) {
  serviceToDelete.value = id
  showDeleteDialog.value = true
}

async function confirmDelete() {
  if (!serviceToDelete.value) return

  try {
    await serviceStore.deleteService(serviceToDelete.value)
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Service deleted successfully',
      life: 5000
    })
    showDeleteDialog.value = false
    serviceToDelete.value = null

    // Refresh the list
    await fetchServices()
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to delete service',
      life: 5000
    })
  }
}

async function handlePageChange({ page, size }: { page: number, size: number }) {
  serviceStore.setPage(page)
  serviceStore.setSize(size)
  await fetchServices()
}

// Lifecycle
onMounted(async () => {
  await fetchServices()
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
  display: flex;
  gap: var(--space-2);
}

/* Service Statistics */
.service-stats {
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

.stat-card--success {
  border-color: var(--green-400);
}

.stat-card--warning {
  border-color: var(--orange-400);
}

.stat-card--info {
  border-color: var(--blue-400);
}

.stat-card--purple {
  border-color: var(--purple-400);
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
}

.stat-card--success .stat-card__icon {
  background: var(--green-100);
  color: var(--green-600);
}

.stat-card--warning .stat-card__icon {
  background: var(--orange-100);
  color: var(--orange-600);
}

.stat-card--info .stat-card__icon {
  background: var(--blue-100);
  color: var(--blue-600);
}

.stat-card--purple .stat-card__icon {
  background: var(--purple-100);
  color: var(--purple-600);
}

.stat-card--neutral .stat-card__icon {
  background: var(--gray-100);
  color: var(--gray-600);
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

.search-input {
  flex: 1;
  min-width: 250px;
}

/* Table */
.services-table {
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

.price-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.price-cell__main {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.price-cell__cycle {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.data-cell, .speed-cell {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.customers-cell {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-weight: var(--font-weight-medium);
}

.customers-cell i {
  color: var(--color-primary);
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

  .service-stats {
    grid-template-columns: 1fr;
  }

  .filters-row {
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }
}
</style>
