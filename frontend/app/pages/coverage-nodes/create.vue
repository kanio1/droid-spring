<template>
  <div class="coverage-node-create-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <div class="breadcrumb">
          <NuxtLink to="/coverage-nodes" class="breadcrumb__link">Coverage Nodes</NuxtLink>
          <i class="pi pi-chevron-right breadcrumb__separator"></i>
          <span class="breadcrumb__current">Create New Node</span>
        </div>
        <h1 class="page-title">Create New Coverage Node</h1>
        <p class="page-subtitle">
          Add a new coverage node to the network infrastructure
        </p>
      </div>
      <div class="page-header__actions">
        <NuxtLink to="/coverage-nodes">
          <Button label="Cancel" icon="pi pi-times" severity="secondary" />
        </NuxtLink>
      </div>
    </div>

    <!-- Create Form -->
    <div class="create-form-container">
      <div class="form-card">
        <div class="form-card__header">
          <h2>Coverage Node Information</h2>
          <p class="form-card__description">
            Please fill in all required fields marked with <span class="required-mark">*</span>
          </p>
        </div>

        <form @submit.prevent="handleSubmit" class="node-form">
          <!-- Basic Information -->
          <div class="form-section">
            <h3 class="form-section__title">Basic Information</h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="name" class="form-field__label">
                  Node Name <span class="required-mark">*</span>
                </label>
                <InputText
                  id="name"
                  v-model="formData.name"
                  placeholder="e.g., Warsaw Central Tower"
                  :class="{ 'p-invalid': errors.name }"
                  style="width: 100%"
                />
                <small v-if="errors.name" class="p-error">{{ errors.name }}</small>
                <small v-else class="form-field__help">Descriptive name for the coverage node</small>
              </div>

              <div class="form-field">
                <label for="code" class="form-field__label">
                  Node Code <span class="required-mark">*</span>
                </label>
                <InputText
                  id="code"
                  v-model="formData.code"
                  placeholder="e.g., WAR-CT-001"
                  :class="{ 'p-invalid': errors.code }"
                  style="width: 100%"
                />
                <small v-if="errors.code" class="p-error">{{ errors.code }}</small>
                <small v-else class="form-field__help">Unique identifier (A-Z, 0-9, -, _)</small>
              </div>

              <div class="form-field">
                <label for="type" class="form-field__label">
                  Node Type <span class="required-mark">*</span>
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
                <small v-else class="form-field__help">Select the type of coverage node</small>
              </div>

              <div class="form-field">
                <label for="technology" class="form-field__label">
                  Technology <span class="required-mark">*</span>
                </label>
                <Dropdown
                  id="technology"
                  v-model="formData.technology"
                  :options="technologyOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Select technology"
                  :class="{ 'p-invalid': errors.technology }"
                  style="width: 100%"
                />
                <small v-if="errors.technology" class="p-error">{{ errors.technology }}</small>
                <small v-else class="form-field__help">Select the technology standard</small>
              </div>
            </div>
          </div>

          <!-- Location Details -->
          <div class="form-section">
            <h3 class="form-section__title">Location Details</h3>
            <div class="form-grid">
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

              <div class="form-field form-field--full">
                <label for="address" class="form-field__label">
                  Street Address
                </label>
                <InputText
                  id="address"
                  v-model="formData.address"
                  placeholder="e.g., 123 Main Street"
                  style="width: 100%"
                />
                <small class="form-field__help">Street address or location description (optional)</small>
              </div>
            </div>
          </div>

          <!-- Geographic Coordinates -->
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
                  placeholder="e.g., 52.229700"
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
                  placeholder="e.g., 21.012200"
                  style="width: 100%"
                />
                <small class="form-field__help">Decimal degrees (-180 to 180)</small>
              </div>
            </div>
          </div>

          <!-- Coverage Details -->
          <div class="form-section">
            <h3 class="form-section__title">Coverage & Capacity</h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="coverageRadius" class="form-field__label">
                  Coverage Radius (km) <span class="required-mark">*</span>
                </label>
                <InputNumber
                  id="coverageRadius"
                  v-model="formData.coverageRadius"
                  :min="0.1"
                  :max="1000"
                  :minFractionDigits="1"
                  :maxFractionDigits="2"
                  placeholder="e.g., 25.0"
                  :class="{ 'p-invalid': errors.coverageRadius }"
                  style="width: 100%"
                />
                <small v-if="errors.coverageRadius" class="p-error">{{ errors.coverageRadius }}</small>
                <small v-else class="form-field__help">Maximum coverage radius in kilometers</small>
              </div>

              <div class="form-field">
                <label for="maxCapacity" class="form-field__label">
                  Maximum Capacity <span class="required-mark">*</span>
                </label>
                <InputNumber
                  id="maxCapacity"
                  v-model="formData.maxCapacity"
                  :min="1"
                  placeholder="e.g., 10000"
                  :class="{ 'p-invalid': errors.maxCapacity }"
                  style="width: 100%"
                />
                <small v-if="errors.maxCapacity" class="p-error">{{ errors.maxCapacity }}</small>
                <small v-else class="form-field__help">Maximum number of concurrent connections</small>
              </div>
            </div>
            <div v-if="formData.coverageRadius && formData.coverageRadius > 0" class="calculated-info">
              <i class="pi pi-info-circle"></i>
              <span>Calculated coverage area: {{ calculatedCoverageArea }} km²</span>
            </div>
          </div>

          <!-- Form Actions -->
          <div class="form-actions">
            <NuxtLink to="/coverage-nodes">
              <Button label="Cancel" icon="pi pi-times" severity="secondary" />
            </NuxtLink>
            <Button
              type="submit"
              label="Create Coverage Node"
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
          <div v-if="formData.type" class="preview-section">
            <Tag :value="formData.type" severity="info" />
          </div>

          <div v-if="formData.name" class="preview-section">
            <span class="preview-label">Node Name:</span>
            <span class="preview-value">{{ formData.name }}</span>
          </div>

          <div v-if="formData.code" class="preview-section">
            <span class="preview-label">Node Code:</span>
            <span class="preview-value code">{{ formData.code }}</span>
          </div>

          <div v-if="formData.technology" class="preview-section">
            <Tag :value="formData.technology" severity="info" />
          </div>

          <div class="preview-section preview-location">
            <div class="preview-city">
              {{ formData.city }}
            </div>
            <div v-if="formData.region" class="preview-region">
              {{ formData.region }}
            </div>
            <div class="preview-country">
              {{ getCountryLabel(formData.country) }}
            </div>
            <div v-if="formData.address" class="preview-address">
              {{ formData.address }}
            </div>
          </div>

          <div v-if="formData.latitude && formData.longitude" class="preview-section">
            <span class="preview-label">Coordinates:</span>
            <span class="preview-value">{{ formData.latitude.toFixed(6) }}, {{ formData.longitude.toFixed(6) }}</span>
          </div>

          <div v-if="formData.coverageRadius" class="preview-section">
            <span class="preview-label">Coverage Radius:</span>
            <span class="preview-value">{{ formData.coverageRadius }} km</span>
          </div>

          <div v-if="formData.maxCapacity" class="preview-section">
            <span class="preview-label">Max Capacity:</span>
            <span class="preview-value">{{ formData.maxCapacity.toLocaleString() }}</span>
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
import { useCoverageNodeStore } from '~/stores/coverage-node'
import { useToast } from 'primevue/usetoast'
import type { CreateCoverageNodeCommand, CoverageNodeType, Technology } from '~/schemas/coverage-node'
import { getTypeIcon } from '~/schemas/coverage-node'

// Meta
definePageMeta({
  title: 'Create Coverage Node'
})

// Route & Stores
const router = useRouter()
const nodeStore = useCoverageNodeStore()
const toast = useToast()

// Reactive State
const loading = ref(false)
const formData = ref<CreateCoverageNodeCommand>({
  name: '',
  code: '',
  type: 'CELL_TOWER' as CoverageNodeType,
  technology: '5G' as Technology,
  latitude: undefined,
  longitude: undefined,
  address: '',
  city: '',
  region: '',
  country: 'PL',
  coverageRadius: 25,
  maxCapacity: 10000
})

const errors = ref<Record<string, string>>({})

// Computed
const calculatedCoverageArea = computed(() => {
  if (!formData.value.coverageRadius) return '0.00'
  const area = Math.PI * Math.pow(formData.value.coverageRadius, 2)
  return area.toFixed(2)
})

// Options
const typeOptions = [
  { label: 'Cell Tower', value: 'CELL_TOWER' },
  { label: 'Satellite', value: 'SATELLITE' },
  { label: 'Fiber Hub', value: 'FIBER_HUB' },
  { label: 'WiFi Hotspot', value: 'WIFI_HOTSPOT' },
  { label: 'Microwave', value: 'MICROWAVE' },
  { label: 'Data Center', value: 'DATA_CENTER' },
  { label: 'Exchange Point', value: 'EXCHANGE_POINT' }
]

const technologyOptions = [
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

// Country labels (simplified - could be in schema)
function getCountryLabel(code: string): string {
  const country = countryOptions.find(c => c.value === code)
  return country?.label || code
}

// Validation
function validateForm(): boolean {
  errors.value = {}

  if (!formData.value.name.trim()) {
    errors.value.name = 'Name is required'
  } else if (formData.value.name.length > 100) {
    errors.value.name = 'Name must not exceed 100 characters'
  }

  if (!formData.value.code.trim()) {
    errors.value.code = 'Code is required'
  } else if (!/^[A-Z0-9-_]+$/.test(formData.value.code)) {
    errors.value.code = 'Code must contain only uppercase letters, numbers, hyphens, or underscores'
  } else if (formData.value.code.length > 20) {
    errors.value.code = 'Code must not exceed 20 characters'
  }

  if (!formData.value.type) {
    errors.value.type = 'Type is required'
  }

  if (!formData.value.technology) {
    errors.value.technology = 'Technology is required'
  }

  if (!formData.value.city.trim()) {
    errors.value.city = 'City is required'
  }

  if (!formData.value.country) {
    errors.value.country = 'Country is required'
  }

  if (formData.value.latitude !== undefined && (formData.value.latitude < -90 || formData.value.latitude > 90)) {
    errors.value.latitude = 'Latitude must be between -90 and 90'
  }

  if (formData.value.longitude !== undefined && (formData.value.longitude < -180 || formData.value.longitude > 180)) {
    errors.value.longitude = 'Longitude must be between -180 and 180'
  }

  if (!formData.value.coverageRadius || formData.value.coverageRadius < 0.1) {
    errors.value.coverageRadius = 'Coverage radius must be at least 0.1 km'
  } else if (formData.value.coverageRadius > 1000) {
    errors.value.coverageRadius = 'Coverage radius must not exceed 1000 km'
  }

  if (!formData.value.maxCapacity || formData.value.maxCapacity < 1) {
    errors.value.maxCapacity = 'Maximum capacity must be at least 1'
  }

  return Object.keys(errors.value).length === 0
}

// Event Handlers
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
    await nodeStore.createNode(formData.value)
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Coverage node created successfully',
      life: 5000
    })
    router.push('/coverage-nodes')
  } catch (err: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: err.message || 'Failed to create coverage node',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

// Lifecycle
onMounted(() => {
  // Any initialization can go here
})
</script>

<style scoped>
.coverage-node-create-page {
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

.form-field__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.form-field__help {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Calculated Info */
.calculated-info {
  margin-top: var(--space-3);
  padding: var(--space-3);
  background: var(--blue-50);
  border: 1px solid var(--blue-200);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  color: var(--blue-900);
}

.calculated-info i {
  color: var(--blue-600);
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

.preview-value.code {
  font-family: 'Monaco', 'Menlo', monospace;
  background: var(--color-gray-100);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.preview-location {
  padding: var(--space-3);
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
}

.preview-city {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-size: var(--font-size-base);
}

.preview-region, .preview-country, .preview-address {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: 2px;
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
