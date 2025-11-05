<template>
  <div class="create-asset-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/assets" class="back-link">
        ← Back to Assets
      </NuxtLink>
      <h1 class="page-title">Create New Asset</h1>
      <p class="page-subtitle">Add a new asset to the inventory</p>
    </div>

    <!-- Form -->
    <div class="asset-form">
      <form @submit.prevent="handleSubmit" class="form">
        <!-- Asset Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Asset Information</h2>
            <p>Basic asset details</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.assetId"
              label="Asset ID"
              placeholder="e.g., ASSET-2024-001"
              :error="errors.assetId"
              required
              @blur="validateField('assetId')"
            />

            <Dropdown
              v-model="formData.assetType"
              :options="assetTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select type"
              label="Asset Type"
              :class="{ 'p-invalid': errors.assetType }"
              style="width: 100%"
              @change="handleAssetTypeChange"
            />
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.name"
              label="Name"
              placeholder="Enter asset name"
              :error="errors.name"
              required
              @blur="validateField('name')"
            />

            <Dropdown
              v-model="formData.status"
              :options="statusOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select status"
              label="Status"
              :class="{ 'p-invalid': errors.status }"
              style="width: 100%"
              @change="validateField('status')"
            />
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.location"
              label="Location"
              placeholder="e.g., Data Center 1, Rack 5"
              :error="errors.location"
              @blur="validateField('location')"
            />

            <AppInput
              v-model="formData.serialNumber"
              label="Serial Number"
              placeholder="Enter serial number"
              :error="errors.serialNumber"
              @blur="validateField('serialNumber')"
            />
          </div>
        </div>

        <!-- Hardware Details Section -->
        <div class="form-section" v-if="formData.assetType">
          <div class="form-section__header">
            <h2>Hardware Details</h2>
            <p>Technical specifications</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.manufacturer"
              label="Manufacturer"
              placeholder="e.g., Cisco, Huawei"
              :error="errors.manufacturer"
              @blur="validateField('manufacturer')"
            />

            <AppInput
              v-model="formData.model"
              label="Model"
              placeholder="e.g., Catalyst 2960"
              :error="errors.model"
              @blur="validateField('model')"
            />
          </div>

          <!-- Network Element Specific Fields -->
          <div v-if="formData.assetType === 'NETWORK_ELEMENT'" class="form-grid">
            <Dropdown
              v-model="formData.subType"
              :options="networkElementSubTypes"
              optionLabel="label"
              optionValue="value"
              placeholder="Select element type"
              label="Element Type"
              :class="{ 'p-invalid': errors.subType }"
              style="width: 100%"
              @change="validateField('subType')"
            />

            <AppInput
              v-model="formData.ipAddress"
              label="IP Address"
              placeholder="e.g., 192.168.1.1"
              :error="errors.ipAddress"
              @blur="validateField('ipAddress')"
            />
          </div>

          <!-- SIM Card Specific Fields -->
          <div v-if="formData.assetType === 'SIM_CARD'" class="form-grid">
            <AppInput
              v-model="formData.iccid"
              label="ICCID"
              placeholder="Enter ICCID (19-20 digits)"
              :error="errors.iccid"
              @blur="validateField('iccid')"
            />

            <AppInput
              v-model="formData.imsi"
              label="IMSI"
              placeholder="Enter IMSI (15 digits)"
              :error="errors.imsi"
              @blur="validateField('imsi')"
            />
          </div>

          <div v-if="formData.assetType === 'SIM_CARD'" class="form-grid">
            <AppInput
              v-model="formData.phoneNumber"
              label="Phone Number"
              placeholder="e.g., +1234567890"
              :error="errors.phoneNumber"
              @blur="validateField('phoneNumber')"
            />

            <Dropdown
              v-model="formData.subType"
              :options="simSubTypes"
              optionLabel="label"
              optionValue="value"
              placeholder="Select SIM type"
              label="SIM Type"
              :class="{ 'p-invalid': errors.subType }"
              style="width: 100%"
              @change="validateField('subType')"
            />
          </div>

          <div v-if="formData.assetType === 'SIM_CARD'" class="form-grid">
            <Dropdown
              v-model="formData.operator"
              :options="operatorOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select operator"
              label="Operator"
              :class="{ 'p-invalid': errors.operator }"
              style="width: 100%"
              @change="validateField('operator')"
            />
          </div>
        </div>

        <!-- Assignment Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Assignment</h2>
            <p>Assign asset to a customer (optional)</p>
          </div>

          <div class="form-grid form-grid--single">
            <AppInput
              v-model="formData.customerId"
              label="Customer ID"
              placeholder="Enter customer UUID (optional)"
              :error="errors.customerId"
              @blur="validateField('customerId')"
            />
          </div>

          <div class="form-help-text">
            <i class="pi pi-info-circle"></i>
            <span>Leave empty to keep the asset unassigned</span>
          </div>
        </div>

        <!-- Warranty Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Warranty Information</h2>
            <p>Warranty details (optional)</p>
          </div>

          <div class="form-grid">
            <Calendar
              v-model="formData.warrantyStart"
              dateFormat="yy-mm-dd"
              placeholder="Select warranty start date"
              label="Warranty Start Date"
              style="width: 100%"
            />

            <Calendar
              v-model="formData.warrantyEnd"
              dateFormat="yy-mm-dd"
              placeholder="Select warranty end date"
              label="Warranty End Date"
              style="width: 100%"
            />
          </div>

          <div class="form-grid form-grid--single">
            <AppInput
              v-model="formData.warrantyProvider"
              label="Warranty Provider"
              placeholder="e.g., Manufacturer, Third-party"
              :error="errors.warrantyProvider"
              @blur="validateField('warrantyProvider')"
            />
          </div>
        </div>

        <!-- Technical Specifications Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Technical Specifications</h2>
            <p>Add custom specifications (optional)</p>
          </div>

          <div class="specs-builder">
            <div v-for="(spec, index) in formData.specifications" :key="index" class="spec-row">
              <AppInput
                v-model="spec.key"
                placeholder="Specification name (e.g., CPU, RAM)"
                style="flex: 1"
              />
              <AppInput
                v-model="spec.value"
                placeholder="Value (e.g., Intel Xeon, 16GB)"
                style="flex: 1"
              />
              <Button
                icon="pi pi-trash"
                severity="danger"
                text
                rounded
                @click="removeSpec(index)"
                v-tooltip.top="'Remove specification'"
              />
            </div>
            <Button
              label="Add Specification"
              icon="pi pi-plus"
              severity="secondary"
              @click="addSpec"
              class="add-spec-btn"
            />
          </div>
        </div>

        <!-- Notes Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Additional Notes</h2>
            <p>Any additional information for this asset</p>
          </div>

          <div class="form-grid form-grid--single">
            <div class="notes-field">
              <label>Notes (Optional)</label>
              <Textarea
                v-model="formData.notes"
                placeholder="Enter any additional notes or comments"
                rows="4"
                auto-resize
              />
            </div>
          </div>
        </div>

        <!-- Form Actions -->
        <div class="form-actions">
          <Button
            type="button"
            label="Cancel"
            icon="pi pi-times"
            severity="secondary"
            @click="navigateTo('/assets')"
          />
          <Button
            type="submit"
            label="Create Asset"
            icon="pi pi-check"
            severity="primary"
            :loading="submitting"
          />
        </div>
      </form>
    </div>

    <!-- Toast for notifications -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { useAssetStore } from '~/stores/asset'
import type { CreateAssetCommand } from '~/schemas/asset'

// Page meta
definePageMeta({
  title: 'Create Asset'
})

// Store
const assetStore = useAssetStore()
const { showToast } = useToast()

// Reactive state
const submitting = ref(false)
const errors = ref<Record<string, string>>({})

// Form data
const formData = ref<CreateAssetCommand>({
  assetId: '',
  name: '',
  assetType: '',
  subType: '',
  status: 'INACTIVE',
  location: '',
  serialNumber: '',
  manufacturer: '',
  model: '',
  ipAddress: '',
  iccid: '',
  imsi: '',
  phoneNumber: '',
  operator: '',
  customerId: '',
  warrantyStart: null,
  warrantyEnd: null,
  warrantyProvider: '',
  specifications: [],
  notes: ''
})

// Options
const assetTypeOptions = [
  { label: 'Network Element', value: 'NETWORK_ELEMENT' },
  { label: 'SIM Card', value: 'SIM_CARD' },
  { label: 'Hardware', value: 'HARDWARE' },
  { label: 'Software License', value: 'SOFTWARE_LICENSE' }
]

const statusOptions = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Maintenance', value: 'MAINTENANCE' },
  { label: 'Reserved', value: 'RESERVED' }
]

const networkElementSubTypes = [
  { label: 'Router', value: 'ROUTER' },
  { label: 'Switch', value: 'SWITCH' },
  { label: 'Base Station', value: 'BASE_STATION' },
  { label: 'Gateway', value: 'GATEWAY' },
  { label: 'Access Point', value: 'ACCESS_POINT' }
]

const simSubTypes = [
  { label: 'Physical SIM', value: 'PHYSICAL_SIM' },
  { label: 'eSIM', value: 'ESIM' },
  { label: 'IoT SIM', value: 'IOT_SIM' }
]

const operatorOptions = [
  { label: 'Verizon', value: 'VERIZON' },
  { label: 'AT&T', value: 'ATT' },
  { label: 'T-Mobile', value: 'TMobile' },
  { label: 'Vodafone', value: 'VODAFONE' }
]

// Methods
const handleAssetTypeChange = () => {
  // Clear subType when asset type changes
  formData.value.subType = ''
  validateField('assetType')
}

const validateField = (field: string) => {
  // Validation logic here
  // This is a simplified version
  delete errors.value[field]
}

const addSpec = () => {
  formData.value.specifications.push({ key: '', value: '' })
}

const removeSpec = (index: number) => {
  formData.value.specifications.splice(index, 1)
}

const handleSubmit = async () => {
  try {
    submitting.value = true
    errors.value = {}

    // Basic validation
    if (!formData.value.assetId) {
      errors.value.assetId = 'Asset ID is required'
      return
    }

    if (!formData.value.name) {
      errors.value.name = 'Name is required'
      return
    }

    if (!formData.value.assetType) {
      errors.value.assetType = 'Asset type is required'
      return
    }

    // SIM Card specific validation
    if (formData.value.assetType === 'SIM_CARD') {
      if (!formData.value.iccid) {
        errors.value.iccid = 'ICCID is required for SIM cards'
        return
      }
    }

    // Build specifications object
    const specifications: Record<string, string> = {}
    formData.value.specifications.forEach(spec => {
      if (spec.key && spec.value) {
        specifications[spec.key] = spec.value
      }
    })

    const assetData: CreateAssetCommand = {
      ...formData.value,
      specifications
    }

    await assetStore.createAsset(assetData)

    showToast({
      severity: 'success',
      summary: 'Asset Created',
      detail: `Asset ${formData.value.name} has been created successfully.`,
      life: 3000
    })

    navigateTo('/assets')

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to create asset',
      life: 5000
    })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-asset-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* Page Header */
.page-header {
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
  margin-bottom: var(--space-3);
}

.back-link:hover {
  text-decoration: underline;
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

/* Form */
.asset-form {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

/* Form Sections */
.form-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.form-section__header {
  padding-bottom: var(--space-3);
  border-bottom: 2px solid var(--color-border);
}

.form-section__header h2 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.form-section__header p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}

.form-grid--single {
  grid-template-columns: 1fr;
}

.form-help-text {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: var(--color-info-50);
  border-radius: var(--radius-md);
  color: var(--color-info-700);
  font-size: var(--font-size-sm);
}

/* Specifications Builder */
.specs-builder {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.spec-row {
  display: flex;
  gap: var(--space-3);
  align-items: center;
}

.add-spec-btn {
  align-self: flex-start;
}

.notes-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.notes-field label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

/* Form Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding-top: var(--space-6);
  border-top: 1px solid var(--color-border);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .spec-row {
    flex-direction: column;
    align-items: stretch;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions button {
    width: 100%;
  }
}
</style>
