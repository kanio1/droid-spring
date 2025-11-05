<template>
  <div class="create-cycle-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <NuxtLink to="/billing/cycles" class="back-link">
          ← Back to Billing Cycles
        </NuxtLink>
        <h1 class="page-title">Create New Billing Cycle</h1>
        <p class="page-subtitle">
          Define a new billing cycle for customer usage tracking and invoicing
        </p>
      </div>
    </div>

    <!-- Form -->
    <div class="form-container">
      <Card>
        <template #header>
          <div class="form-header">
            <h2>Billing Cycle Details</h2>
            <p>Enter the basic information for the new billing cycle</p>
          </div>
        </template>

        <template #content>
          <form @submit.prevent="handleSubmit" class="cycle-form">
            <!-- Customer Selection -->
            <div class="form-section">
              <h3 class="section-title">Customer Information</h3>
              <div class="form-row">
                <div class="form-field form-field--full">
                  <label for="customerId" class="form-label">
                    Customer <span class="required">*</span>
                  </label>
                  <Dropdown
                    id="customerId"
                    v-model="formData.customerId"
                    :options="customerOptions"
                    optionLabel="label"
                    optionValue="value"
                    placeholder="Select a customer"
                    :class="{ 'p-invalid': errors.customerId }"
                    showClear
                    filter
                  />
                  <small v-if="errors.customerId" class="p-error">{{ errors.customerId }}</small>
                </div>
              </div>
            </div>

            <!-- Billing Period -->
            <div class="form-section">
              <h3 class="section-title">Billing Period</h3>
              <div class="form-row">
                <div class="form-field">
                  <label for="startDate" class="form-label">
                    Start Date <span class="required">*</span>
                  </label>
                  <Calendar
                    id="startDate"
                    v-model="formData.startDate"
                    :minDate="minDate"
                    :showIcon="true"
                    dateFormat="yy-mm-dd"
                    :class="{ 'p-invalid': errors.startDate }"
                  />
                  <small v-if="errors.startDate" class="p-error">{{ errors.startDate }}</small>
                </div>

                <div class="form-field">
                  <label for="endDate" class="form-label">
                    End Date <span class="required">*</span>
                  </label>
                  <Calendar
                    id="endDate"
                    v-model="formData.endDate"
                    :minDate="formData.startDate || minDate"
                    :showIcon="true"
                    dateFormat="yy-mm-dd"
                    :class="{ 'p-invalid': errors.endDate }"
                  />
                  <small v-if="errors.endDate" class="p-error">{{ errors.endDate }}</small>
                </div>

                <div class="form-field">
                  <label for="dueDate" class="form-label">
                    Due Date <span class="required">*</span>
                  </label>
                  <Calendar
                    id="dueDate"
                    v-model="formData.dueDate"
                    :minDate="formData.endDate || new Date()"
                    :showIcon="true"
                    dateFormat="yy-mm-dd"
                    :class="{ 'p-invalid': errors.dueDate }"
                  />
                  <small v-if="errors.dueDate" class="p-error">{{ errors.dueDate }}</small>
                </div>
              </div>

              <!-- Quick Date Presets -->
              <div class="form-row">
                <div class="form-field form-field--full">
                  <label class="form-label">Quick Select Period</label>
                  <div class="preset-buttons">
                    <Button
                      v-for="preset in datePresets"
                      :key="preset.label"
                      :label="preset.label"
                      variant="secondary"
                      size="small"
                      @click="applyDatePreset(preset)"
                    />
                  </div>
                </div>
              </div>
            </div>

            <!-- Billing Options -->
            <div class="form-section">
              <h3 class="section-title">Billing Options</h3>
              <div class="form-row">
                <div class="form-field">
                  <label class="form-label">Auto-Process</label>
                  <div class="checkbox-group">
                    <Checkbox
                      id="autoProcess"
                      v-model="formData.autoProcess"
                      :binary="true"
                    />
                    <label for="autoProcess" class="checkbox-label">
                      Automatically process the billing cycle after creation
                    </label>
                  </div>
                  <small class="form-help">
                    If enabled, the billing cycle will be processed immediately after creation
                  </small>
                </div>
              </div>

              <div class="form-row">
                <div class="form-field">
                  <label class="form-label">Generate Invoice</label>
                  <div class="checkbox-group">
                    <Checkbox
                      id="generateInvoice"
                      v-model="formData.generateInvoice"
                      :binary="true"
                    />
                    <label for="generateInvoice" class="checkbox-label">
                      Generate invoice upon completion
                    </label>
                  </div>
                  <small class="form-help">
                    An invoice will be generated once the billing cycle is successfully processed
                  </small>
                </div>
              </div>

              <div class="form-row">
                <div class="form-field">
                  <label class="form-label">Send Notification</label>
                  <div class="checkbox-group">
                    <Checkbox
                      id="sendNotification"
                      v-model="formData.sendNotification"
                      :binary="true"
                    />
                    <label for="sendNotification" class="checkbox-label">
                      Send email notification to customer
                    </label>
                  </div>
                  <small class="form-help">
                    Customer will receive email notifications about billing cycle status changes
                  </small>
                </div>
              </div>
            </div>

            <!-- Additional Notes -->
            <div class="form-section">
              <h3 class="section-title">Additional Information</h3>
              <div class="form-row">
                <div class="form-field form-field--full">
                  <label for="notes" class="form-label">Notes</label>
                  <Textarea
                    id="notes"
                    v-model="formData.notes"
                    rows="4"
                    placeholder="Enter any additional notes or comments..."
                  />
                </div>
              </div>
            </div>

            <!-- Form Actions -->
            <div class="form-actions">
              <Button
                type="button"
                label="Cancel"
                variant="secondary"
                @click="handleCancel"
                :disabled="isSubmitting"
              />
              <Button
                type="button"
                label="Save as Draft"
                variant="outline"
                @click="handleSaveDraft"
                :loading="isSubmitting"
                :disabled="isSubmitting"
              />
              <Button
                type="submit"
                label="Create & Process"
                icon="pi pi-check"
                variant="primary"
                :loading="isSubmitting"
                :disabled="isSubmitting"
              />
            </div>
          </form>
        </template>
      </Card>
    </div>

    <!-- Preview Card -->
    <div v-if="formData.startDate && formData.endDate" class="preview-card">
      <Card>
        <template #header>
          <div class="preview-header">
            <h3><i class="pi pi-eye"></i> Cycle Preview</h3>
          </div>
        </template>

        <template #content>
          <div class="preview-content">
            <div class="preview-item">
              <span class="preview-label">Period:</span>
              <span class="preview-value">
                {{ formatDate(formData.startDate) }} - {{ formatDate(formData.endDate) }}
              </span>
            </div>
            <div class="preview-item">
              <span class="preview-label">Duration:</span>
              <span class="preview-value">{{ getDuration(formData.startDate, formData.endDate) }}</span>
            </div>
            <div class="preview-item" v-if="formData.dueDate">
              <span class="preview-label">Due Date:</span>
              <span class="preview-value">{{ formatDate(formData.dueDate) }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">Days Until Due:</span>
              <span class="preview-value">{{ getDaysUntilDue() }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">Auto-Process:</span>
              <span class="preview-value">{{ formData.autoProcess ? 'Yes' : 'No' }}</span>
            </div>
            <div class="preview-item">
              <span class="preview-label">Generate Invoice:</span>
              <span class="preview-value">{{ formData.generateInvoice ? 'Yes' : 'No' }}</span>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <!-- Toast for notifications -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { useBillingStore } from '~/stores/billing'
import { type CreateBillingCycleCommand } from '~/schemas/billing'

// Page meta
definePageMeta({
  title: 'Create Billing Cycle'
})

// Store
const billingStore = useBillingStore()
const { showToast } = useToast()

// Reactive state
const isSubmitting = ref(false)
const minDate = ref(new Date())

const formData = reactive({
  customerId: '',
  startDate: null as Date | null,
  endDate: null as Date | null,
  dueDate: null as Date | null,
  autoProcess: false,
  generateInvoice: true,
  sendNotification: false,
  notes: ''
})

const errors = reactive({
  customerId: '',
  startDate: '',
  endDate: '',
  dueDate: ''
})

// Date presets
const datePresets = [
  {
    label: 'Current Month',
    getValue: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      const end = new Date(now.getFullYear(), now.getMonth() + 1, 0)
      const dueDate = new Date(end)
      dueDate.setDate(dueDate.getDate() + 30)
      return { start, end, dueDate }
    }
  },
  {
    label: 'Previous Month',
    getValue: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
      const end = new Date(now.getFullYear(), now.getMonth(), 0)
      const dueDate = new Date(end)
      dueDate.setDate(dueDate.getDate() + 30)
      return { start, end, dueDate }
    }
  },
  {
    label: 'Quarterly (90 days)',
    getValue: () => {
      const start = new Date()
      const end = new Date(start)
      end.setDate(end.getDate() + 90)
      const dueDate = new Date(end)
      dueDate.setDate(dueDate.getDate() + 30)
      return { start, end, dueDate }
    }
  },
  {
    label: 'Semi-Annual (180 days)',
    getValue: () => {
      const start = new Date()
      const end = new Date(start)
      end.setDate(end.getDate() + 180)
      const dueDate = new Date(end)
      dueDate.setDate(dueDate.getDate() + 30)
      return { start, end, dueDate }
    }
  }
]

// Mock customer options (replace with actual API call)
const customerOptions = ref([
  { label: 'Customer 1', value: 'customer-1' },
  { label: 'Customer 2', value: 'customer-2' },
  { label: 'Customer 3', value: 'customer-3' }
])

// Methods
const validateForm = (): boolean => {
  let isValid = true

  // Reset errors
  Object.keys(errors).forEach(key => {
    errors[key as keyof typeof errors] = ''
  })

  if (!formData.customerId) {
    errors.customerId = 'Customer is required'
    isValid = false
  }

  if (!formData.startDate) {
    errors.startDate = 'Start date is required'
    isValid = false
  }

  if (!formData.endDate) {
    errors.endDate = 'End date is required'
    isValid = false
  } else if (formData.startDate && formData.endDate <= formData.startDate) {
    errors.endDate = 'End date must be after start date'
    isValid = false
  }

  if (!formData.dueDate) {
    errors.dueDate = 'Due date is required'
    isValid = false
  } else if (formData.endDate && formData.dueDate < formData.endDate) {
    errors.dueDate = 'Due date must be on or after end date'
    isValid = false
  }

  return isValid
}

const applyDatePreset = (preset: typeof datePresets[0]) => {
  const dates = preset.getValue()
  formData.startDate = dates.start
  formData.endDate = dates.end
  formData.dueDate = dates.dueDate
}

const handleSubmit = async () => {
  if (!validateForm()) return

  isSubmitting.value = true

  try {
    const command: CreateBillingCycleCommand = {
      customerId: formData.customerId,
      startDate: formData.startDate!.toISOString(),
      endDate: formData.endDate!.toISOString(),
      dueDate: formData.dueDate!.toISOString()
    }

    const cycle = await billingStore.startBillingCycle(command)

    showToast({
      severity: 'success',
      summary: 'Billing Cycle Created',
      detail: `Billing cycle #${cycle.cycleNumber} has been created successfully`,
      life: 5000
    })

    if (formData.autoProcess) {
      await billingStore.processBillingCycle(cycle.id)
      showToast({
        severity: 'info',
        summary: 'Processing Started',
        detail: 'Billing cycle is being processed',
        life: 3000
      })
    }

    navigateTo('/billing/cycles')

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to create billing cycle',
      life: 5000
    })
  } finally {
    isSubmitting.value = false
  }
}

const handleSaveDraft = async () => {
  isSubmitting.value = true

  try {
    showToast({
      severity: 'info',
      summary: 'Draft Saved',
      detail: 'Billing cycle saved as draft (functionality will be implemented)',
      life: 3000
    })
  } finally {
    isSubmitting.value = false
  }
}

const handleCancel = () => {
  navigateTo('/billing/cycles')
}

// Utility functions
const formatDate = (date: Date | null): string => {
  if (!date) return ''
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const getDuration = (start: Date | null, end: Date | null): string => {
  if (!start || !end) return ''

  const diffTime = Math.abs(end.getTime() - start.getTime())
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays === 1) return '1 day'
  return `${diffDays} days`
}

const getDaysUntilDue = (): string => {
  if (!formData.dueDate) return ''

  const today = new Date()
  const dueDate = new Date(formData.dueDate)
  const diffTime = dueDate.getTime() - today.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays < 0) return `${Math.abs(diffDays)} days overdue`
  if (diffDays === 0) return 'Due today'
  if (diffDays === 1) return '1 day'
  return `${diffDays} days`
}
</script>

<style scoped>
.create-cycle-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  padding: var(--space-6);
  max-width: 1400px;
  margin: 0 auto;
}

/* Page Header */
.page-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: var(--font-size-sm);
  transition: color 0.2s;
}

.back-link:hover {
  color: var(--color-primary);
}

.page-title {
  margin: 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

/* Form Container */
.form-container {
  flex: 1;
}

.form-header {
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
}

.form-header h2 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.form-header p {
  margin: 0;
  color: var(--color-text-secondary);
}

/* Form Styles */
.cycle-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  padding: var(--space-6);
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.section-title {
  margin: 0 0 var(--space-3) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--color-border);
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
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

.form-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.required {
  color: var(--color-danger);
}

.checkbox-group {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.checkbox-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  cursor: pointer;
}

.form-help {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  margin-top: var(--space-1);
}

.preset-buttons {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

/* Preview Card */
.preview-card {
  width: 400px;
  flex-shrink: 0;
}

.preview-header {
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-elevated);
}

.preview-header h3 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.preview-content {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.preview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-3);
}

.preview-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-medium);
}

.preview-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-semibold);
}

/* Error Styles */
.p-error {
  color: var(--color-danger);
  font-size: var(--font-size-xs);
}

/* Mobile Responsive */
@media (max-width: 1200px) {
  .create-cycle-page {
    flex-direction: column;
  }

  .preview-card {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .create-cycle-page {
    padding: var(--space-4);
  }

  .form-header,
  .cycle-form {
    padding: var(--space-4);
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .preset-buttons {
    flex-direction: column;
  }

  .preset-buttons .p-button {
    width: 100%;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions .p-button {
    width: 100%;
  }
}
</style>
