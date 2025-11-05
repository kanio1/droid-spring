<template>
  <div class="address-create-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <div class="breadcrumb">
          <NuxtLink to="/addresses" class="breadcrumb__link">Addresses</NuxtLink>
          <i class="pi pi-chevron-right breadcrumb__separator"></i>
          <span class="breadcrumb__current">Create New Address</span>
        </div>
        <h1 class="page-title">Create New Address</h1>
        <p class="page-subtitle">
          Add a new address for a customer
        </p>
      </div>
      <div class="page-header__actions">
        <NuxtLink to="/addresses">
          <Button label="Cancel" icon="pi pi-times" severity="secondary" />
        </NuxtLink>
      </div>
    </div>

    <!-- Create Form -->
    <div class="create-form-container">
      <div class="form-card">
        <div class="form-card__header">
          <h2>Address Information</h2>
          <p class="form-card__description">
            Please fill in all required fields marked with <span class="required-mark">*</span>
          </p>
        </div>

        <form @submit.prevent="handleSubmit" class="address-form">
          <!-- Customer Selection -->
          <div class="form-section">
            <h3 class="form-section__title">Customer Information</h3>
            <div class="form-grid">
              <div class="form-field form-field--full">
                <label for="customerId" class="form-field__label">
                  Customer <span class="required-mark">*</span>
                </label>
                <Dropdown
                  id="customerId"
                  v-model="formData.customerId"
                  :options="customerOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Select a customer"
                  :class="{ 'p-invalid': errors.customerId }"
                  filter
                  showClear
                  style="width: 100%"
                />
                <small v-if="errors.customerId" class="p-error">{{ errors.customerId }}</small>
                <small v-else class="form-field__help">Select the customer this address belongs to</small>
              </div>
            </div>
          </div>

          <!-- Address Type -->
          <div class="form-section">
            <h3 class="form-section__title">Address Type</h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="type" class="form-field__label">
                  Type <span class="required-mark">*</span>
                </label>
                <Dropdown
                  id="type"
                  v-model="formData.type"
                  :options="typeOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Select type"
                  :class="{ 'p-invalid': errors.type }"
                  style="width: 100%"
                />
                <small v-if="errors.type" class="p-error">{{ errors.type }}</small>
                <small v-else class="form-field__help">Select the purpose of this address</small>
              </div>

              <div class="form-field form-field--checkbox">
                <div class="checkbox-wrapper">
                  <Checkbox
                    id="isPrimary"
                    v-model="formData.isPrimary"
                    binary
                  />
                  <label for="isPrimary" class="checkbox-label">
                    Primary Address
                    <span class="checkbox-help">Set as customer's primary address</span>
                  </label>
                </div>
              </div>
            </div>
          </div>

          <!-- Street Address -->
          <div class="form-section">
            <h3 class="form-section__title">Street Address</h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="street" class="form-field__label">
                  Street Name <span class="required-mark">*</span>
                </label>
                <InputText
                  id="street"
                  v-model="formData.street"
                  placeholder="e.g., Main Street"
                  :class="{ 'p-invalid': errors.street }"
                  style="width: 100%"
                />
                <small v-if="errors.street" class="p-error">{{ errors.street }}</small>
                <small v-else class="form-field__help">Enter the street name</small>
              </div>

              <div class="form-field">
                <label for="houseNumber" class="form-field__label">
                  House Number <span class="required-mark">*</span>
                </label>
                <InputText
                  id="houseNumber"
                  v-model="formData.houseNumber"
                  placeholder="e.g., 123"
                  :class="{ 'p-invalid': errors.houseNumber }"
                  style="width: 100%"
                />
                <small v-if="errors.houseNumber" class="p-error">{{ errors.houseNumber }}</small>
                <small v-else class="form-field__help">House or building number</small>
              </div>

              <div class="form-field">
                <label for="apartmentNumber" class="form-field__label">
                  Apartment Number
                </label>
                <InputText
                  id="apartmentNumber"
                  v-model="formData.apartmentNumber"
                  placeholder="e.g., Apt 4B"
                  style="width: 100%"
                />
                <small class="form-field__help">Apartment, suite, or unit number (optional)</small>
              </div>
            </div>
          </div>

          <!-- Location Details -->
          <div class="form-section">
            <h3 class="form-section__title">Location Details</h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="postalCode" class="form-field__label">
                  Postal Code <span class="required-mark">*</span>
                </label>
                <InputText
                  id="postalCode"
                  v-model="formData.postalCode"
                  placeholder="XX-XXX"
                  :class="{ 'p-invalid': errors.postalCode }"
                  style="width: 100%"
                />
                <small v-if="errors.postalCode" class="p-error">{{ errors.postalCode }}</small>
                <small v-else class="form-field__help">Format: XX-XXX</small>
              </div>

              <div class="form-field">
                <label for="city" class="form-field__label">
                  City <span class="required-mark">*</span>
                </label>
                <InputText
                  id="city"
                  v-model="formData.city"
                  placeholder="e.g., Warsaw"
                  :class="{ 'p-invalid': errors.city }"
                  style="width: 100%"
                />
                <small v-if="errors.city" class="p-error">{{ errors.city }}</small>
                <small v-else class="form-field__help">City or town name</small>
              </div>

              <div class="form-field">
                <label for="region" class="form-field__label">
                  Region/State
                </label>
                <InputText
                  id="region"
                  v-model="formData.region"
                  placeholder="e.g., Mazovia"
                  style="width: 100%"
                />
                <small class="form-field__help">State, province, or region (optional)</small>
              </div>

              <div class="form-field">
                <label for="country" class="form-field__label">
                  Country <span class="required-mark">*</span>
                </label>
                <Dropdown
                  id="country"
                  v-model="formData.country"
                  :options="countryOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Select country"
                  :class="{ 'p-invalid': errors.country }"
                  filter
                  showClear
                  style="width: 100%"
                />
                <small v-if="errors.country" class="p-error">{{ errors.country }}</small>
                <small v-else class="form-field__help">Select the country</small>
              </div>
            </div>
          </div>

          <!-- Coordinates (Optional) -->
          <div class="form-section">
            <h3 class="form-section__title">
              Geographic Coordinates
              <span class="form-section__subtitle">(Optional)</span>
            </h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="latitude" class="form-field__label">Latitude</label>
                <InputNumber
                  id="latitude"
                  v-model="formData.latitude"
                  mode="decimal"
                  :min="-90"
                  :max="90"
                  :minFractionDigits="6"
                  :maxFractionDigits="6"
                  placeholder="e.g., 52.2297"
                  style="width: 100%"
                />
                <small class="form-field__help">Decimal degrees (-90 to 90)</small>
              </div>

              <div class="form-field">
                <label for="longitude" class="form-field__label">Longitude</label>
                <InputNumber
                  id="longitude"
                  v-model="formData.longitude"
                  mode="decimal"
                  :min="-180"
                  :max="180"
                  :minFractionDigits="6"
                  :maxFractionDigits="6"
                  placeholder="e.g., 21.0122"
                  style="width: 100%"
                />
                <small class="form-field__help">Decimal degrees (-180 to 180)</small>
              </div>
            </div>
          </div>

          <!-- Form Actions -->
          <div class="form-actions">
            <NuxtLink to="/addresses">
              <Button label="Cancel" icon="pi pi-times" severity="secondary" />
            </NuxtLink>
            <Button
              type="submit"
              label="Create Address"
              icon="pi pi-check"
              severity="primary"
              :loading="loading"
              :disabled="loading"
            />
          </div>
        </form>
      </div>

      <!-- Preview Card -->
      <div class="preview-card">
        <div class="preview-card__header">
          <h3>Preview</h3>
          <i class="pi pi-eye preview-icon"></i>
        </div>
        <div class="preview-card__content">
          <div v-if="formData.customerId && selectedCustomer" class="preview-section">
            <span class="preview-label">Customer:</span>
            <span class="preview-value">{{ selectedCustomer.label }}</span>
          </div>

          <div v-if="formData.type" class="preview-section">
            <Tag :value="formData.type" :severity="getTypeVariant(formData.type)" />
          </div>

          <div class="preview-section preview-address">
            <div class="preview-street">
              {{ formData.street }} {{ formData.houseNumber }}{{ formData.apartmentNumber ? `/${formData.apartmentNumber}` : '' }}
            </div>
            <div class="preview-city">
              {{ formData.postalCode }} {{ formData.city }}
            </div>
            <div v-if="formData.region" class="preview-region">
              {{ formData.region }}
            </div>
            <div class="preview-country">
              {{ getCountryLabel(formData.country) }}
            </div>
          </div>

          <div v-if="formData.latitude && formData.longitude" class="preview-section">
            <span class="preview-label">Coordinates:</span>
            <span class="preview-value">{{ formData.latitude }}, {{ formData.longitude }}</span>
          </div>

          <div v-if="formData.isPrimary" class="preview-section">
            <i class="pi pi-star-fill text-yellow-500"></i>
            <span class="preview-label">Primary Address</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAddressStore } from '~/stores/address'
import { useCustomerStore } from '~/stores/customer'
import { useToast } from 'primevue/usetoast'
import type { CreateAddressCommand, AddressType, Country } from '~/schemas/address'
import { getTypeVariant, getCountryLabel } from '~/schemas/address'

// Meta
definePageMeta({
  title: 'Create Address'
})

// Route & Stores
const router = useRouter()
const addressStore = useAddressStore()
const customerStore = useCustomerStore()
const toast = useToast()

// Reactive State
const loading = ref(false)
const formData = ref<CreateAddressCommand>({
  customerId: '',
  type: 'SERVICE' as AddressType,
  street: '',
  houseNumber: '',
  apartmentNumber: '',
  postalCode: '',
  city: '',
  region: '',
  country: 'PL' as Country,
  latitude: undefined,
  longitude: undefined,
  isPrimary: false
})

const errors = ref<Record<string, string>>({})

// Computed
const selectedCustomer = computed(() =>
  customerOptions.value.find(c => c.value === formData.value.customerId)
)

// Options
const typeOptions = [
  { label: 'Billing Address', value: 'BILLING' },
  { label: 'Shipping Address', value: 'SHIPPING' },
  { label: 'Service Address', value: 'SERVICE' },
  { label: 'Correspondence Address', value: 'CORRESPONDENCE' }
]

const countryOptions = [
  { label: 'Poland', value: 'PL' },
  { label: 'Germany', value: 'DE' },
  { label: 'France', value: 'FR' },
  { label: 'Spain', value: 'ES' },
  { label: 'Italy', value: 'IT' },
  { label: 'United Kingdom', value: 'UK' },
  { label: 'Netherlands', value: 'NL' },
  { label: 'Sweden', value: 'SE' },
  { label: 'Norway', value: 'NO' },
  { label: 'Denmark', value: 'DK' }
]

const customerOptions = ref<Array<{ label: string, value: string }>>([])

// Validation
function validateForm(): boolean {
  errors.value = {}

  if (!formData.value.customerId) {
    errors.value.customerId = 'Customer is required'
  }

  if (!formData.value.type) {
    errors.value.type = 'Type is required'
  }

  if (!formData.value.street.trim()) {
    errors.value.street = 'Street is required'
  }

  if (!formData.value.houseNumber.trim()) {
    errors.value.houseNumber = 'House number is required'
  }

  if (!formData.value.postalCode.trim()) {
    errors.value.postalCode = 'Postal code is required'
  } else if (!/^\d{2}-\d{3}$/.test(formData.value.postalCode)) {
    errors.value.postalCode = 'Invalid format. Use XX-XXX'
  }

  if (!formData.value.city.trim()) {
    errors.value.city = 'City is required'
  }

  if (!formData.value.country) {
    errors.value.country = 'Country is required'
  }

  return Object.keys(errors.value).length === 0
}

// Event Handlers
async function fetchCustomers() {
  try {
    await customerStore.fetchCustomers({ size: 1000 })
    customerOptions.value = customerStore.customers.map(c => ({
      label: `${c.firstName} ${c.lastName} (${c.email})`,
      value: c.id
    }))
  } catch (err: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to load customers',
      life: 5000
    })
  }
}

async function handleSubmit() {
  if (!validateForm()) {
    toast.add({
      severity: 'error',
      summary: 'Validation Error',
      detail: 'Please correct the errors in the form',
      life: 5000
    })
    return
  }

  loading.value = true
  try {
    await addressStore.createAddress(formData.value)
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Address created successfully',
      life: 5000
    })
    router.push('/addresses')
  } catch (err: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: err.message || 'Failed to create address',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

// Lifecycle
onMounted(async () => {
  await fetchCustomers()
})
</script>

<style scoped>
.address-create-page {
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
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.page-header__actions {
  flex-shrink: 0;
}

/* Create Form Container */
.create-form-container {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: var(--space-6);
}

/* Form Card */
.form-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.form-card__header {
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.form-card__header h2 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.form-card__description {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.required-mark {
  color: var(--red-500);
  font-weight: var(--font-weight-bold);
}

/* Form Sections */
.form-section {
  margin-bottom: var(--space-6);
}

.form-section__title {
  margin: 0 0 var(--space-4) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.form-section__subtitle {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-normal);
  color: var(--color-text-secondary);
}

/* Form Grid */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-field--full {
  grid-column: 1 / -1;
}

.form-field--checkbox {
  justify-content: flex-end;
}

.form-field__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.form-field__help {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Checkbox */
.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3);
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
}

.checkbox-label {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  cursor: pointer;
}

.checkbox-help {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-normal);
  color: var(--color-text-secondary);
}

/* Form Actions */
.form-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
  margin-top: var(--space-6);
}

/* Preview Card */
.preview-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  height: fit-content;
  position: sticky;
  top: var(--space-6);
}

.preview-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.preview-card__header h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.preview-icon {
  color: var(--color-primary);
  font-size: 1.25rem;
}

.preview-card__content {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.preview-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.preview-label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
}

.preview-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.preview-address {
  padding: var(--space-3);
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.preview-street {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.preview-city, .preview-region, .preview-country {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* Mobile Responsive */
@media (max-width: 1024px) {
  .create-form-container {
    grid-template-columns: 1fr;
  }

  .preview-card {
    position: static;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>
