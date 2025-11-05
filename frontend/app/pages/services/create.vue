<template>
  <div class="service-create-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <div class="breadcrumb">
          <NuxtLink to="/services" class="breadcrumb__link">Services</NuxtLink>
          <i class="pi pi-chevron-right breadcrumb__separator"></i>
          <span class="breadcrumb__current">Create New Service</span>
        </div>
        <h1 class="page-title">Create New Service</h1>
        <p class="page-subtitle">
          Add a new service offering to the catalog
        </p>
      </div>
      <div class="page-header__actions">
        <NuxtLink to="/services">
          <Button label="Cancel" icon="pi pi-times" severity="secondary" />
        </NuxtLink>
      </div>
    </div>

    <!-- Create Form -->
    <div class="create-form-container">
      <div class="form-card">
        <div class="form-card__header">
          <h2>Service Information</h2>
          <p class="form-card__description">
            Please fill in all required fields marked with <span class="required-mark">*</span>
          </p>
        </div>

        <form @submit.prevent="handleSubmit" class="service-form">
          <!-- Basic Information -->
          <div class="form-section">
            <h3 class="form-section__title">Basic Information</h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="name" class="form-field__label">
                  Service Name <span class="required-mark">*</span>
                </label>
                <InputText
                  id="name"
                  v-model="formData.name"
                  placeholder="e.g., Premium Fiber Internet"
                  :class="{ 'p-invalid': errors.name }"
                  style="width: 100%"
                />
                <small v-if="errors.name" class="p-error">{{ errors.name }}</small>
                <small v-else class="form-field__help">Descriptive name for the service</small>
              </div>

              <div class="form-field">
                <label for="code" class="form-field__label">
                  Service Code <span class="required-mark">*</span>
                </label>
                <InputText
                  id="code"
                  v-model="formData.code"
                  placeholder="e.g., FIBER-PREM-1000"
                  :class="{ 'p-invalid': errors.code }"
                  style="width: 100%"
                />
                <small v-if="errors.code" class="p-error">{{ errors.code }}</small>
                <small v-else class="form-field__help">Unique identifier (A-Z, 0-9, -, _)</small>
              </div>

              <div class="form-field">
                <label for="type" class="form-field__label">
                  Service Type <span class="required-mark">*</span>
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
                <small v-else class="form-field__help">Select the type of service</small>
              </div>

              <div class="form-field">
                <label for="category" class="form-field__label">
                  Category <span class="required-mark">*</span>
                </label>
                <Dropdown
                  id="category"
                  v-model="formData.category"
                  :options="categoryOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Select category"
                  :class="{ 'p-invalid': errors.category }"
                  style="width: 100%"
                />
                <small v-if="errors.category" class="p-error">{{ errors.category }}</small>
                <small v-else class="form-field__help">Select the service category</small>
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

              <div class="form-field">
                <label for="status" class="form-field__label">
                  Status <span class="required-mark">*</span>
                </label>
                <Dropdown
                  id="status"
                  v-model="formData.status"
                  :options="statusOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Select status"
                  :class="{ 'p-invalid': errors.status }"
                  style="width: 100%"
                />
                <small v-if="errors.status" class="p-error">{{ errors.status }}</small>
                <small v-else class="form-field__help">Initial status of the service</small>
              </div>
            </div>
          </div>

          <!-- Service Description -->
          <div class="form-section">
            <h3 class="form-section__title">Service Description</h3>
            <div class="form-grid">
              <div class="form-field form-field--full">
                <label for="description" class="form-field__label">
                  Description
                </label>
                <Textarea
                  id="description"
                  v-model="formData.description"
                  placeholder="Describe the service offering..."
                  :autoResize="true"
                  rows="3"
                  style="width: 100%"
                />
                <small class="form-field__help">Detailed description of the service (optional)</small>
              </div>

              <div class="form-field form-field--full">
                <label for="features" class="form-field__label">
                  Features
                </label>
                <Chips
                  id="features"
                  v-model="formData.features"
                  placeholder="Type a feature and press Enter"
                  style="width: 100%"
                />
                <small class="form-field__help">Add service features as separate tags (optional)</small>
              </div>
            </div>
          </div>

          <!-- Pricing -->
          <div class="form-section">
            <h3 class="form-section__title">Pricing Information</h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="price" class="form-field__label">
                  Price <span class="required-mark">*</span>
                </label>
                <InputNumber
                  id="price"
                  v-model="formData.price"
                  mode="currency"
                  currency="USD"
                  locale="en-US"
                  :min="0"
                  :max="999999.99"
                  :minFractionDigits="2"
                  :maxFractionDigits="2"
                  placeholder="0.00"
                  :class="{ 'p-invalid': errors.price }"
                  style="width: 100%"
                />
                <small v-if="errors.price" class="p-error">{{ errors.price }}</small>
                <small v-else class="form-field__help">Service price per billing cycle</small>
              </div>

              <div class="form-field">
                <label for="currency" class="form-field__label">
                  Currency <span class="required-mark">*</span>
                </label>
                <Dropdown
                  id="currency"
                  v-model="formData.currency"
                  :options="currencyOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Select currency"
                  :class="{ 'p-invalid': errors.currency }"
                  style="width: 100%"
                />
                <small v-if="errors.currency" class="p-error">{{ errors.currency }}</small>
                <small v-else class="form-field__help">Currency code (ISO 4217)</small>
              </div>

              <div class="form-field">
                <label for="billingCycle" class="form-field__label">
                  Billing Cycle <span class="required-mark">*</span>
                </label>
                <Dropdown
                  id="billingCycle"
                  v-model="formData.billingCycle"
                  :options="billingCycleOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Select billing cycle"
                  :class="{ 'p-invalid': errors.billingCycle }"
                  style="width: 100%"
                />
                <small v-if="errors.billingCycle" class="p-error">{{ errors.billingCycle }}</small>
                <small v-else class="form-field__help">How often the service is billed</small>
              </div>

              <div class="form-field">
                <label class="form-field__label">Annual Cost</label>
                <div class="annual-cost">
                  {{ calculateAnnualCost() }}
                </div>
                <small class="form-field__help">Calculated automatically from price and billing cycle</small>
              </div>
            </div>
          </div>

          <!-- Service Limits -->
          <div class="form-section">
            <h3 class="form-section__title">Service Limits</h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="dataLimit" class="form-field__label">
                  Data Limit (GB/month)
                </label>
                <InputNumber
                  id="dataLimit"
                  v-model="formData.dataLimit"
                  :min="0"
                  placeholder="e.g., 1000"
                  style="width: 100%"
                />
                <small class="form-field__help">Monthly data allowance (0 for unlimited)</small>
              </div>

              <div class="form-field">
                <label for="speed" class="form-field__label">
                  Speed (Mbps)
                </label>
                <InputNumber
                  id="speed"
                  v-model="formData.speed"
                  :min="0"
                  placeholder="e.g., 1000"
                  style="width: 100%"
                />
                <small class="form-field__help">Connection speed in megabits per second</small>
              </div>

              <div class="form-field">
                <label for="voiceMinutes" class="form-field__label">
                  Voice Minutes
                </label>
                <InputNumber
                  id="voiceMinutes"
                  v-model="formData.voiceMinutes"
                  :min="0"
                  placeholder="e.g., 500"
                  style="width: 100%"
                />
                <small class="form-field__help">Monthly voice minutes allowance</small>
              </div>

              <div class="form-field">
                <label for="smsCount" class="form-field__label">
                  SMS Count
                </label>
                <InputNumber
                  id="smsCount"
                  v-model="formData.smsCount"
                  :min="0"
                  placeholder="e.g., 1000"
                  style="width: 100%"
                />
                <small class="form-field__help">Monthly SMS allowance</small>
              </div>

              <div class="form-field">
                <label for="bandwidth" class="form-field__label">
                  Bandwidth
                </label>
                <InputText
                  id="bandwidth"
                  v-model="formData.bandwidth"
                  placeholder="e.g., 1 Gbps"
                  style="width: 100%"
                />
                <small class="form-field__help">Available bandwidth (optional)</small>
              </div>

              <div class="form-field">
                <label for="latency" class="form-field__label">
                  Latency (ms)
                </label>
                <InputNumber
                  id="latency"
                  v-model="formData.latency"
                  :min="0"
                  placeholder="e.g., 10"
                  style="width: 100%"
                />
                <small class="form-field__help">Maximum latency in milliseconds (optional)</small>
              </div>
            </div>
          </div>

          <!-- Coverage Requirements -->
          <div class="form-section">
            <h3 class="form-section__title">Coverage Requirements</h3>
            <div class="form-grid">
              <div class="form-field form-field--full">
                <label for="requiredCoverageNodes" class="form-field__label">
                  Required Coverage Nodes
                </label>
                <Chips
                  id="requiredCoverageNodes"
                  v-model="formData.requiredCoverageNodes"
                  placeholder="Type a node ID and press Enter"
                  style="width: 100%"
                />
                <small class="form-field__help">List of coverage nodes required for this service (optional)</small>
              </div>
            </div>
          </div>

          <!-- SLA & Support -->
          <div class="form-section">
            <h3 class="form-section__title">SLA & Support</h3>
            <div class="form-grid">
              <div class="form-field">
                <label for="slaUptime" class="form-field__label">
                  SLA Uptime (%)
                </label>
                <InputNumber
                  id="slaUptime"
                  v-model="formData.slaUptime"
                  :min="0"
                  :max="100"
                  placeholder="e.g., 99.9"
                  :minFractionDigits="1"
                  :maxFractionDigits="1"
                  style="width: 100%"
                />
                <small class="form-field__help">Service Level Agreement uptime percentage</small>
              </div>

              <div class="form-field">
                <label for="supportLevel" class="form-field__label">
                  Support Level
                </label>
                <Dropdown
                  id="supportLevel"
                  v-model="formData.supportLevel"
                  :options="supportLevelOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Select support level"
                  style="width: 100%"
                />
                <small class="form-field__help">Level of customer support provided</small>
              </div>

              <div class="form-field">
                <label for="maxCustomerCount" class="form-field__label">
                  Max Customers
                </label>
                <InputNumber
                  id="maxCustomerCount"
                  v-model="formData.maxCustomerCount"
                  :min="0"
                  placeholder="e.g., 10000"
                  style="width: 100%"
                />
                <small class="form-field__help">Maximum number of customers for this service (optional)</small>
              </div>
            </div>
          </div>

          <!-- Form Actions -->
          <div class="form-actions">
            <Button
              type="submit"
              label="Create Service"
              icon="pi pi-check"
              severity="primary"
              :loading="loading"
            />
            <NuxtLink to="/services">
              <Button
                label="Cancel"
                icon="pi pi-times"
                severity="secondary"
                outlined
              />
            </NuxtLink>
          </div>
        </form>
      </div>

      <!-- Live Preview Sidebar -->
      <div class="preview-card">
        <div class="preview-card__header">
          <h2>Live Preview</h2>
          <p class="preview-card__description">
            Preview of the service as it will appear
          </p>
        </div>
        <div class="preview-card__body">
          <div class="preview-section">
            <h3 class="preview-section__title">Service Overview</h3>
            <div class="preview-item">
              <span class="preview-item__label">Name:</span>
              <span class="preview-item__value">{{ formData.name || '---' }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-item__label">Code:</span>
              <span class="preview-item__value">{{ formData.code || '---' }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-item__label">Type:</span>
              <Tag :value="formData.type" severity="secondary" />
            </div>
            <div class="preview-item">
              <span class="preview-item__label">Category:</span>
              <Tag :value="formData.category" severity="info" />
            </div>
            <div class="preview-item">
              <span class="preview-item__label">Status:</span>
              <Tag :value="formData.status" />
            </div>
          </div>

          <div class="preview-section" v-if="formData.price">
            <h3 class="preview-section__title">Pricing</h3>
            <div class="preview-item">
              <span class="preview-item__label">Price:</span>
              <span class="preview-item__value price-highlight">
                {{ formatPrice(formData.price, formData.currency) }}
              </span>
            </div>
            <div class="preview-item">
              <span class="preview-item__label">Billing:</span>
              <span class="preview-item__value">{{ getBillingCycleLabel(formData.billingCycle) }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-item__label">Annual:</span>
              <span class="preview-item__value">{{ calculateAnnualCost() }}</span>
            </div>
          </div>

          <div class="preview-section" v-if="formData.dataLimit || formData.speed">
            <h3 class="preview-section__title">Service Limits</h3>
            <div class="preview-item" v-if="formData.dataLimit">
              <span class="preview-item__label">Data:</span>
              <span class="preview-item__value">{{ formatDataLimit(formData.dataLimit) }}</span>
            </div>
            <div class="preview-item" v-if="formData.speed">
              <span class="preview-item__label">Speed:</span>
              <span class="preview-item__value">{{ formatSpeed(formData.speed) }}</span>
            </div>
          </div>

          <div class="preview-section" v-if="formData.features && formData.features.length > 0">
            <h3 class="preview-section__title">Features</h3>
            <ul class="preview-features">
              <li v-for="(feature, index) in formData.features.slice(0, 5)" :key="index">
                <i class="pi pi-check"></i>
                <span>{{ feature }}</span>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useServiceStore } from '~/stores/service'
import { useToast } from 'primevue/usetoast'
import { useRouter } from 'vue-router'
import type { CreateServiceCommand } from '~/schemas/service'
import {
  formatPrice,
  formatDataLimit,
  formatSpeed,
  getBillingCycleLabel
} from '~/schemas/service'

// Meta
definePageMeta({
  title: 'Create Service'
})

// Stores & Composables
const serviceStore = useServiceStore()
const toast = useToast()
const router = useRouter()

// Reactive State
const loading = ref(false)

const formData = reactive<CreateServiceCommand>({
  name: '',
  code: '',
  type: undefined as any,
  category: undefined as any,
  status: 'PLANNED' as any,
  technology: undefined as any,
  description: '',
  features: [],
  price: 0,
  currency: 'USD',
  billingCycle: undefined as any,
  dataLimit: undefined,
  speed: undefined,
  voiceMinutes: undefined,
  smsCount: undefined,
  bandwidth: '',
  latency: undefined,
  requiredCoverageNodes: [],
  slaUptime: undefined,
  supportLevel: undefined as any,
  maxCustomerCount: undefined
})

const errors = ref<Record<string, string>>({})

// Options
const typeOptions = [
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
  { label: 'Broadband', value: 'BROADBAND' },
  { label: 'Voice', value: 'VOICE' },
  { label: 'Video', value: 'VIDEO' },
  { label: 'Mobile', value: 'MOBILE' },
  { label: 'Cloud', value: 'CLOUD' },
  { label: 'Enterprise', value: 'ENTERPRISE' },
  { label: 'Emerging', value: 'EMERGING' }
]

const statusOptions = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Planned', value: 'PLANNED' },
  { label: 'Deprecated', value: 'DEPRECATED' },
  { label: 'Suspended', value: 'SUSPENDED' }
]

const technologyOptions = [
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

const currencyOptions = [
  { label: 'USD - US Dollar', value: 'USD' },
  { label: 'EUR - Euro', value: 'EUR' },
  { label: 'GBP - British Pound', value: 'GBP' },
  { label: 'PLN - Polish Złoty', value: 'PLN' }
]

const billingCycleOptions = [
  { label: 'Monthly', value: 'MONTHLY' },
  { label: 'Quarterly', value: 'QUARTERLY' },
  { label: 'Yearly', value: 'YEARLY' },
  { label: 'One-time', value: 'ONE_TIME' }
]

const supportLevelOptions = [
  { label: 'Basic', value: 'BASIC' },
  { label: 'Standard', value: 'STANDARD' },
  { label: 'Premium', value: 'PREMIUM' },
  { label: 'Enterprise', value: 'ENTERPRISE' }
]

// Helper Functions
function calculateAnnualCost(): string {
  if (!formData.price || !formData.billingCycle) return '---'

  const cycles: Record<string, number> = {
    MONTHLY: 12,
    QUARTERLY: 4,
    YEARLY: 1,
    ONE_TIME: 1
  }

  const yearlyPrice = formData.price * cycles[formData.billingCycle]
  return formatPrice(yearlyPrice, formData.currency)
}

function validateForm(): boolean {
  errors.value = {}

  if (!formData.name || formData.name.trim() === '') {
    errors.value.name = 'Service name is required'
  }

  if (!formData.code || formData.code.trim() === '') {
    errors.value.code = 'Service code is required'
  } else if (!/^[A-Z0-9-_]+$/.test(formData.code)) {
    errors.value.code = 'Code can only contain A-Z, 0-9, -, _'
  }

  if (!formData.type) {
    errors.value.type = 'Service type is required'
  }

  if (!formData.category) {
    errors.value.category = 'Category is required'
  }

  if (!formData.technology) {
    errors.value.technology = 'Technology is required'
  }

  if (!formData.status) {
    errors.value.status = 'Status is required'
  }

  if (formData.price === undefined || formData.price < 0) {
    errors.value.price = 'Price must be 0 or greater'
  }

  if (!formData.currency) {
    errors.value.currency = 'Currency is required'
  }

  if (!formData.billingCycle) {
    errors.value.billingCycle = 'Billing cycle is required'
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

  try {
    loading.value = true
    await serviceStore.createService(formData)
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Service created successfully',
      life: 5000
    })
    router.push('/services')
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to create service',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.service-create-page {
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
  margin-bottom: var(--space-3);
  font-size: var(--font-size-sm);
}

.breadcrumb__link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
}

.breadcrumb__link:hover {
  text-decoration: underline;
}

.breadcrumb__separator {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
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

/* Form Container */
.create-form-container {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: var(--space-6);
  align-items: start;
}

.form-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.form-card__header {
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
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
}

/* Form Sections */
.service-form {
  padding: var(--space-6);
}

.form-section {
  margin-bottom: var(--space-8);
}

.form-section:last-child {
  margin-bottom: 0;
}

.form-section__title {
  margin: 0 0 var(--space-4) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  padding-bottom: var(--space-2);
  border-bottom: 2px solid var(--color-primary);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.form-field--full {
  grid-column: 1 / -1;
}

.form-field__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.form-field__help {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Form Actions */
.form-actions {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-6);
  padding-top: var(--space-6);
  border-top: 1px solid var(--color-border);
}

/* Preview Card */
.preview-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  position: sticky;
  top: var(--space-6);
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.preview-card__header {
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
  position: sticky;
  top: 0;
  z-index: 10;
}

.preview-card__header h2 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.preview-card__description {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.preview-card__body {
  padding: var(--space-4);
}

.preview-section {
  margin-bottom: var(--space-4);
}

.preview-section:last-child {
  margin-bottom: 0;
}

.preview-section__title {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.preview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--color-border);
}

.preview-item:last-child {
  border-bottom: none;
}

.preview-item__label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.preview-item__value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
  text-align: right;
}

.price-highlight {
  font-size: var(--font-size-lg) !important;
  font-weight: var(--font-weight-bold) !important;
  color: var(--green-600);
}

.annual-cost {
  padding: var(--space-2) var(--space-3);
  background: var(--color-surface-secondary);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-bold);
  color: var(--green-600);
  font-size: var(--font-size-lg);
}

.preview-features {
  list-style: none;
  padding: 0;
  margin: var(--space-2) 0 0 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.preview-features li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.preview-features i {
  color: var(--green-500);
  font-size: 0.9rem;
}

/* Mobile Responsive */
@media (max-width: 1024px) {
  .create-form-container {
    grid-template-columns: 1fr;
  }

  .preview-card {
    position: static;
    max-height: none;
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
