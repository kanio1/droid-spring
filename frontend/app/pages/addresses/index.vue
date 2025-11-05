<template>
  <div class="addresses-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Addresses</h1>
        <p class="page-subtitle">
          Manage customer addresses and service locations
        </p>
      </div>
      <div class="page-header__actions">
        <NuxtLink to="/addresses/create">
          <Button
            label="Add Address"
            icon="pi pi-plus"
            severity="primary"
          />
        </NuxtLink>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="addresses-filters">
      <div class="filters-row">
        <span class="p-input-icon-left search-input">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search addresses..."
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
          style="width: 150px"
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
          v-model="countryFilter"
          :options="countryOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Countries"
          style="width: 180px"
          @change="handleCountryFilter"
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

    <!-- Address Summary Stats -->
    <div class="address-stats">
      <div class="stat-card">
        <div class="stat-card__icon">
          <i class="pi pi-map-marker"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ paginationProps.total }}</div>
          <div class="stat-card__label">Total Addresses</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--success">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ activeAddresses.length }}</div>
          <div class="stat-card__label">Active</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--warning">
          <i class="pi pi-clock"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ pendingAddresses.length }}</div>
          <div class="stat-card__label">Pending</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-card__icon stat-card__icon--info">
          <i class="pi pi-home"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ billingAddresses.length }}</div>
          <div class="stat-card__label">Billing</div>
        </div>
      </div>
    </div>

    <!-- Addresses Table -->
    <div class="addresses-table">
      <AppTable
        :columns="tableColumns"
        :data="addresses"
        :loading="loading"
        :pagination="paginationProps"
        :clickable="true"
        @row-click="handleRowClick"
        @page-change="handlePageChange"
      >
        <!-- Custom cell templates -->
        <template #cell(type)="{ value }">
          <Tag :value="value" :severity="getTypeVariant(value)" />
        </template>

        <template #cell(status)="{ value }">
          <Tag :value="value" :severity="getStatusVariant(value)" />
        </template>

        <template #cell(country)="{ value }">
          {{ getCountryLabel(value) }}
        </template>

        <template #cell(isPrimary)="{ value }">
          <i v-if="value" class="pi pi-star-fill text-yellow-500" v-tooltip.top="'Primary Address'"></i>
        </template>

        <template #cell(address)="{ row }">
          <div class="address-cell">
            <div class="address-cell__street">
              {{ row.street }} {{ row.houseNumber }}{{ row.apartmentNumber ? `/${row.apartmentNumber}` : '' }}
            </div>
            <div class="address-cell__city">
              {{ row.postalCode }} {{ row.city }}
            </div>
          </div>
        </template>

        <template #cell(customerName)="{ value }">
          <NuxtLink :to="`/customers`" class="customer-link">
            {{ value }}
          </NuxtLink>
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
              @click.stop="handleViewAddress(row.id)"
            />
            <Button
              icon="pi pi-pencil"
              severity="info"
              size="small"
              text
              rounded
              v-tooltip.top="'Edit'"
              @click.stop="handleEditAddress(row.id)"
            />
            <Button
              icon="pi pi-trash"
              severity="danger"
              size="small"
              text
              rounded
              v-tooltip.top="'Delete'"
              @click.stop="handleDeleteAddress(row.id)"
            />
          </div>
        </template>
      </AppTable>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && addresses.length === 0" class="empty-state">
      <div class="empty-state__icon">
        <i class="pi pi-map-marker"></i>
      </div>
      <h3 class="empty-state__title">No addresses found</h3>
      <p class="empty-state__description">
        {{ searchTerm || typeFilter || statusFilter || countryFilter
          ? 'Try adjusting your filters or search terms'
          : 'Get started by adding your first address' }}
      </p>
      <div class="empty-state__actions">
        <NuxtLink to="/addresses/create">
          <Button label="Add Address" icon="pi pi-plus" severity="primary" />
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
        <p>Are you sure you want to delete this address?</p>
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
import { useAddressStore } from '~/stores/address'
import { useToast } from 'primevue/usetoast'
import type { AddressType, AddressStatus, Country } from '~/schemas/address'

// Meta
definePageMeta({
  title: 'Addresses'
})

// Stores & Composables
const addressStore = useAddressStore()
const router = useRouter()
const toast = useToast()

// Reactive State
const searchTerm = ref('')
const typeFilter = ref<AddressType | ''>('')
const statusFilter = ref<AddressStatus | ''>('')
const countryFilter = ref<Country | ''>('')
const sortOption = ref('createdAt,desc')
const showDeleteDialog = ref(false)
const addressToDelete = ref<string | null>(null)

// Computed
const addresses = computed(() => addressStore.addresses)
const loading = computed(() => addressStore.loading)

const activeAddresses = computed(() => addressStore.activeAddresses)
const pendingAddresses = computed(() => addressStore.pendingAddresses)
const billingAddresses = computed(() => addressStore.billingAddresses)

const paginationProps = computed(() => addressStore.pagination)

// Filter Options
const typeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Billing', value: 'BILLING' },
  { label: 'Shipping', value: 'SHIPPING' },
  { label: 'Service', value: 'SERVICE' },
  { label: 'Correspondence', value: 'CORRESPONDENCE' }
]

const statusOptions = [
  { label: 'All Status', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Pending', value: 'PENDING' }
]

const countryOptions = [
  { label: 'All Countries', value: '' },
  { label: 'Poland', value: 'PL' },
  { label: 'Germany', value: 'DE' },
  { label: 'France', value: 'FR' },
  { label: 'Spain', value: 'ES' },
  { label: 'Italy', value: 'IT' },
  { label: 'United Kingdom', value: 'UK' }
]

const sortOptions = [
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' },
  { label: 'City A-Z', value: 'city,asc' },
  { label: 'City Z-A', value: 'city,desc' },
  { label: 'Street A-Z', value: 'street,asc' },
  { label: 'Street Z-A', value: 'street,desc' }
]

// Table Columns
const tableColumns = [
  { key: 'customerName', label: 'Customer', sortable: true, align: 'left' },
  { key: 'type', label: 'Type', sortable: true, align: 'center', width: '120px' },
  { key: 'address', label: 'Address', sortable: false, align: 'left', width: '300px' },
  { key: 'city', label: 'City', sortable: true, align: 'left', width: '150px' },
  { key: 'country', label: 'Country', sortable: true, align: 'center', width: '120px' },
  { key: 'status', label: 'Status', sortable: true, align: 'center', width: '100px' },
  { key: 'isPrimary', label: 'Primary', sortable: false, align: 'center', width: '80px' },
  { key: 'actions', label: 'Actions', sortable: false, align: 'center', width: '120px' }
]

// Helper Functions (import from schema)
import {
  getTypeVariant,
  getStatusVariant,
  getCountryLabel
} from '~/schemas/address'

// Event Handlers
let searchTimeout: NodeJS.Timeout | null = null

const handleSearch = () => {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
  searchTimeout = setTimeout(async () => {
    await fetchAddresses()
  }, 300)
}

const handleTypeFilter = async () => {
  await fetchAddresses()
}

const handleStatusFilter = async () => {
  await fetchAddresses()
}

const handleCountryFilter = async () => {
  await fetchAddresses()
}

const handleSortChange = async () => {
  await fetchAddresses()
}

async function fetchAddresses() {
  try {
    await addressStore.fetchAddresses({
      page: 0,
      searchTerm: searchTerm.value || undefined,
      type: typeFilter.value || undefined,
      status: statusFilter.value || undefined,
      country: countryFilter.value || undefined,
      sort: sortOption.value
    })
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to fetch addresses',
      life: 5000
    })
  }
}

function handleRowClick(row: Address) {
  handleViewAddress(row.id)
}

function handleViewAddress(id: string) {
  router.push(`/addresses/${id}`)
}

function handleEditAddress(id: string) {
  router.push(`/addresses/${id}?edit=true`)
}

function handleDeleteAddress(id: string) {
  addressToDelete.value = id
  showDeleteDialog.value = true
}

async function confirmDelete() {
  if (!addressToDelete.value) return

  try {
    await addressStore.deleteAddress(addressToDelete.value)
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Address deleted successfully',
      life: 5000
    })
    showDeleteDialog.value = false
    addressToDelete.value = null

    // Refresh the list
    await fetchAddresses()
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to delete address',
      life: 5000
    })
  }
}

async function handlePageChange({ page, size }: { page: number, size: number }) {
  addressStore.setPage(page)
  addressStore.setSize(size)
  await fetchAddresses()
}

// Lifecycle
onMounted(async () => {
  await fetchAddresses()
})
</script>

<style scoped>
.addresses-page {
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

/* Filters */
.addresses-filters {
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
.address-stats {
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

.stat-card__icon--info {
  background: var(--blue-100);
  color: var(--blue-600);
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
.addresses-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.address-cell__street {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.address-cell__city {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.customer-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
}

.customer-link:hover {
  text-decoration: underline;
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

  .filters-row {
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .address-stats {
    grid-template-columns: 1fr;
  }

  .stat-card {
    padding: var(--space-3);
  }
}
</style>
