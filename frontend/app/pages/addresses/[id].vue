<template>
  <div class="address-details-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <div class="breadcrumb">
          <NuxtLink to="/addresses" class="breadcrumb__link">Addresses</NuxtLink>
          <i class="pi pi-chevron-right breadcrumb__separator"></i>
          <span class="breadcrumb__current">{{ address?.street || 'Loading...' }}</span>
        </div>
        <h1 class="page-title">
          Address Details
          <i v-if="address?.isPrimary" class="pi pi-star-fill text-yellow-500 ml-2" v-tooltip.top="'Primary Address'"></i>
        </h1>
        <p class="page-subtitle">
          {{ address ? formatFullAddress(address) : 'Loading address information...' }}
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
          label="Delete"
          icon="pi pi-trash"
          severity="danger"
          @click="handleDelete"
        />
      </div>
    </div>

    <div v-if="loading && !address" class="loading-state">
      <ProgressSpinner />
      <p>Loading address details...</p>
    </div>

    <div v-else-if="error && !address" class="error-state">
      <i class="pi pi-exclamation-triangle"></i>
      <p>{{ error }}</p>
      <NuxtLink to="/addresses">
        <Button label="Back to List" severity="secondary" />
      </NuxtLink>
    </div>

    <div v-else-if="address" class="address-content">
      <!-- Status and Info Cards -->
      <div class="info-cards">
        <div class="info-card">
          <div class="info-card__header">
            <h3>Address Information</h3>
            <Tag :value="address.status" :severity="getStatusVariant(address.status)" />
          </div>
          <div class="info-card__content">
            <div class="info-row">
              <span class="info-row__label">Type:</span>
              <Tag :value="address.type" :severity="getTypeVariant(address.type)" />
            </div>
            <div class="info-row">
              <span class="info-row__label">Customer:</span>
              <NuxtLink :to="`/customers/${address.customerId}`" class="customer-link">
                {{ address.customerName }}
              </NuxtLink>
            </div>
            <div class="info-row">
              <span class="info-row__label">Street:</span>
              <span class="info-row__value">{{ address.street }} {{ address.houseNumber }}{{ address.apartmentNumber ? `/${address.apartmentNumber}` : '' }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Postal Code:</span>
              <span class="info-row__value">{{ address.postalCode }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">City:</span>
              <span class="info-row__value">{{ address.city }}</span>
            </div>
            <div class="info-row" v-if="address.region">
              <span class="info-row__label">Region:</span>
              <span class="info-row__value">{{ address.region }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Country:</span>
              <span class="info-row__value">{{ getCountryLabel(address.country) }}</span>
            </div>
            <div class="info-row" v-if="address.latitude && address.longitude">
              <span class="info-row__label">Coordinates:</span>
              <span class="info-row__value">{{ address.latitude }}, {{ address.longitude }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Primary Address:</span>
              <span class="info-row__value">
                <i v-if="address.isPrimary" class="pi pi-check-circle text-green-600"></i>
                <i v-else class="pi pi-times-circle text-gray-400"></i>
              </span>
            </div>
          </div>
        </div>

        <div class="info-card">
          <div class="info-card__header">
            <h3>Timeline</h3>
          </div>
          <div class="info-card__content">
            <div class="info-row">
              <span class="info-row__label">Created:</span>
              <span class="info-row__value">{{ formatDateTime(address.createdAt) }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Last Updated:</span>
              <span class="info-row__value">{{ formatDateTime(address.updatedAt) }}</span>
            </div>
            <div class="info-row">
              <span class="info-row__label">Version:</span>
              <span class="info-row__value">v{{ address.version }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Map Section (if coordinates available) -->
      <div v-if="address.latitude && address.longitude" class="map-section">
        <div class="map-section__header">
          <h3>Location Map</h3>
          <p class="map-section__description">Geographic location of this address</p>
        </div>
        <div class="map-placeholder">
          <i class="pi pi-map"></i>
          <p>Map integration placeholder</p>
          <p class="text-sm text-gray-600">Coordinates: {{ address.latitude }}, {{ address.longitude }}</p>
        </div>
      </div>

      <!-- Edit Mode Notice -->
      <div v-if="isEditMode" class="edit-notice">
        <i class="pi pi-info-circle"></i>
        <p>Edit mode is not available on this page. Please use the Edit button in the list view or create a new address.</p>
      </div>

      <!-- Audit Trail -->
      <div class="audit-section">
        <h3>Related Information</h3>
        <div class="related-info">
          <div class="related-card">
            <div class="related-card__header">
              <i class="pi pi-user"></i>
              <h4>Customer</h4>
            </div>
            <div class="related-card__content">
              <NuxtLink :to="`/customers/${address.customerId}`" class="related-link">
                <Button label="View Customer Details" icon="pi pi-external-link" severity="secondary" outlined />
              </NuxtLink>
            </div>
          </div>

          <div class="related-card">
            <div class="related-card__header">
              <i class="pi pi-map-marker"></i>
              <h4>Same Customer Addresses</h4>
            </div>
            <div class="related-card__content">
              <p class="text-sm text-gray-600 mb-3">
                This customer has {{ customerAddressCount }} address(es) in total
              </p>
              <NuxtLink :to="`/addresses?customerId=${address.customerId}`">
                <Button label="View All Addresses" icon="pi pi-list" severity="secondary" text />
              </NuxtLink>
            </div>
          </div>

          <div class="related-card">
            <div class="related-card__header">
              <i class="pi pi-building"></i>
              <h4>Coverage Node</h4>
            </div>
            <div class="related-card__content">
              <p class="text-sm text-gray-600 mb-3">
                Find coverage information for this location
              </p>
              <NuxtLink to="/coverage-nodes">
                <Button label="View Coverage Nodes" icon="pi pi-map" severity="secondary" text />
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
        <p>Are you sure you want to delete this address?</p>
        <p class="text-sm text-gray-600 mt-2">{{ formatFullAddress(address) }}</p>
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
import { useAddressStore } from '~/stores/address'
import { useCustomerStore } from '~/stores/customer'
import { useToast } from 'primevue/usetoast'
import type { Address } from '~/schemas/address'

// Meta
definePageMeta({
  title: 'Address Details'
})

// Route & Stores
const route = useRoute()
const router = useRouter()
const addressStore = useAddressStore()
const customerStore = useCustomerStore()
const toast = useToast()

// Reactive State
const loading = ref(false)
const error = ref<string | null>(null)
const isEditMode = ref(false)
const showDeleteDialog = ref(false)
const address = ref<Address | null>(null)
const customerAddressCount = ref(0)

// Computed
const addressId = computed(() => route.params.id as string)
const isEditRoute = computed(() => route.query.edit === 'true')

// Helper Functions (import from schema)
import {
  formatFullAddress,
  getStatusVariant,
  getTypeVariant,
  getCountryLabel
} from '~/schemas/address'

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
async function fetchAddress() {
  loading.value = true
  error.value = null

  try {
    const data = await addressStore.fetchAddressById(addressId.value)
    address.value = data
  } catch (err: any) {
    error.value = err.message || 'Failed to fetch address details'
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

async function fetchCustomerAddresses(customerId: string) {
  try {
    const data = await addressStore.getAddressesByCustomer(customerId)
    customerAddressCount.value = data.totalElements
  } catch (err: any) {
    console.error('Failed to fetch customer addresses:', err)
  }
}

const handleEdit = () => {
  toast.add({
    severity: 'info',
    summary: 'Edit Mode',
    detail: 'Please use the list view to edit addresses',
    life: 5000
  })
}

const handleDelete = () => {
  showDeleteDialog.value = true
}

const confirmDelete = async () => {
  if (!address.value) return

  loading.value = true
  try {
    await addressStore.deleteAddress(address.value.id)
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Address deleted successfully',
      life: 5000
    })
    showDeleteDialog.value = false
    router.push('/addresses')
  } catch (err: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: err.message || 'Failed to delete address',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}


// Lifecycle
onMounted(async () => {
  await fetchAddress()

  if (address.value) {
    await fetchCustomerAddresses(address.value.customerId)
  }
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
.address-details-page {
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

.customer-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
}

.customer-link:hover {
  text-decoration: underline;
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

.map-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  background: var(--color-gray-50);
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
}

.map-placeholder i {
  font-size: 4rem;
  margin-bottom: var(--space-4);
  opacity: 0.5;
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

/* Edit Form */
.edit-form-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.edit-form-section h3 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.form-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
}

/* Audit Section */
.audit-section {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.audit-section h3 {
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

.related-link {
  text-decoration: none;
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

  .info-cards {
    grid-template-columns: 1fr;
  }

  .related-info {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>
