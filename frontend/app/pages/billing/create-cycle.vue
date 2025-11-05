<template>
  <div class="create-cycle-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/billing/cycles" class="back-link">
        ← Back to Billing Cycles
      </NuxtLink>
      <h1 class="page-title">Create New Billing Cycle</h1>
      <p class="page-subtitle">Start a new billing cycle for a customer</p>
    </div>

    <!-- Form -->
    <div class="cycle-form">
      <form @submit.prevent="handleSubmit" class="form">
        <!-- Cycle Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Cycle Information</h2>
            <p>Basic billing cycle details</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.cycleName"
              label="Cycle Name"
              placeholder="e.g., January 2024"
              :error="errors.cycleName"
              required
              @blur="validateField('cycleName')"
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
        </div>

        <!-- Customer Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Customer Information</h2>
            <p>Select the customer for this billing cycle</p>
          </div>

          <div class="form-grid form-grid--single">
            <AppInput
              v-model="formData.customerId"
              label="Customer ID"
              placeholder="Enter customer UUID"
              :error="errors.customerId"
              required
              @blur="validateField('customerId')"
            />
          </div>

          <div class="form-help-text">
            <i class="pi pi-info-circle"></i>
            <span>You can also search for customers by entering their ID</span>
          </div>
        </div>

        <!-- Period Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Billing Period</h2>
            <p>Define the billing cycle period</p>
          </div>

          <div class="form-grid">
            <Calendar
              v-model="formData.startDate"
              dateFormat="yy-mm-dd"
              placeholder="Select start date"
              label="Start Date"
              :class="{ 'p-invalid': errors.startDate }"
              style="width: 100%"
              @blur="validateField('startDate')"
            />

            <Calendar
              v-model="formData.endDate"
              dateFormat="yy-mm-dd"
              placeholder="Select end date"
              label="End Date"
              :class="{ 'p-invalid': errors.endDate }"
              style="width: 100%"
              @blur="validateField('endDate')"
            />
          </div>

          <div class="form-grid">
            <div class="checkbox-field">
              <Checkbox
                v-model="formData.autoProcess"
                :binary="true"
                inputId="autoProcess"
              />
              <label for="autoProcess">Auto-Process Cycle</label>
            </div>

            <div class="checkbox-field">
              <Checkbox
                v-model="formData.generateInvoices"
                :binary="true"
                inputId="generateInvoices"
              />
              <label for="generateInvoices">Generate Invoices</label>
            </div>
          </div>
        </div>

        <!-- Usage Configuration Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Usage Configuration</h2>
            <p>Configure usage collection and rating</p>
          </div>

          <div class="form-grid">
            <Dropdown
              v-model="formData.usageCollectionMethod"
              :options="collectionMethodOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select collection method"
              label="Usage Collection Method"
              :class="{ 'p-invalid': errors.usageCollectionMethod }"
              style="width: 100%"
              @change="validateField('usageCollectionMethod')"
            />

            <AppInput
              v-model="formData.ratingProfile"
              label="Rating Profile"
              placeholder="e.g., STANDARD, PREMIUM"
              :error="errors.ratingProfile"
              @blur="validateField('ratingProfile')"
            />
          </div>

          <div class="form-grid">
            <div class="checkbox-field">
              <Checkbox
                v-model="formData.includeUnratedUsage"
                :binary="true"
                inputId="includeUnratedUsage"
              />
              <label for="includeUnratedUsage">Include Unrated Usage</label>
            </div>

            <div class="checkbox-field">
              <Checkbox
                v-model="formData.applyAdjustments"
                :binary="true"
                inputId="applyAdjustments"
              />
              <label for="applyAdjustments">Apply Pending Adjustments</label>
            </div>
          </div>
        </div>

        <!-- Notifications Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Notifications</h2>
            <p>Configure cycle notifications</p>
          </div>

          <div class="form-grid">
            <div class="checkbox-field">
              <Checkbox
                v-model="formData.sendCycleStartNotification"
                :binary="true"
                inputId="sendCycleStartNotification"
              />
              <label for="sendCycleStartNotification">Send Cycle Start Notification</label>
            </div>

            <div class="checkbox-field">
              <Checkbox
                v-model="formData.sendCycleCompleteNotification"
                :binary="true"
                inputId="sendCycleCompleteNotification"
              />
              <label for="sendCycleCompleteNotification">Send Cycle Complete Notification</label>
            </div>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.notificationEmails"
              label="Notification Emails (comma-separated)"
              placeholder="email1@example.com, email2@example.com"
              :error="errors.notificationEmails"
              @blur="validateField('notificationEmails')"
            />
          </div>
        </div>

        <!-- Notes Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Additional Notes</h2>
            <p>Any additional information for this billing cycle</p>
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
            @click="navigateTo('/billing/cycles')"
          />
          <Button
            type="submit"
            label="Create Cycle"
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
import { useBillingStore } from '~/stores/billing'
import type { CreateBillingCycleCommand } from '~/schemas/billing'

// Page meta
definePageMeta({
  title: 'Create Billing Cycle'
})

// Store
const billingStore = useBillingStore()
const { showToast } = useToast()

// Reactive state
const submitting = ref(false)
const errors = ref<Record<string, string>>({})

// Form data
const formData = ref<CreateBillingCycleCommand>({
  cycleName: '',
  customerId: '',
  status: 'PENDING',
  startDate: new Date(),
  endDate: null,
  autoProcess: false,
  generateInvoices: true,
  usageCollectionMethod: 'AUTOMATIC',
  ratingProfile: 'STANDARD',
  includeUnratedUsage: true,
  applyAdjustments: true,
  sendCycleStartNotification: true,
  sendCycleCompleteNotification: true,
  notificationEmails: '',
  notes: ''
})

// Options
const statusOptions = [
  { label: 'Pending', value: 'PENDING' },
  { label: 'Processing', value: 'PROCESSING' }
]

const collectionMethodOptions = [
  { label: 'Automatic', value: 'AUTOMATIC' },
  { label: 'Manual', value: 'MANUAL' },
  { label: 'Scheduled', value: 'SCHEDULED' }
]

// Methods
const validateField = (field: string) => {
  // Validation logic here
  // This is a simplified version
  delete errors.value[field]
}

const handleSubmit = async () => {
  try {
    submitting.value = true
    errors.value = {}

    // Basic validation
    if (!formData.value.cycleName) {
      errors.value.cycleName = 'Cycle name is required'
      return
    }

    if (!formData.value.customerId) {
      errors.value.customerId = 'Customer ID is required'
      return
    }

    if (!formData.value.startDate) {
      errors.value.startDate = 'Start date is required'
      return
    }

    if (!formData.value.endDate) {
      errors.value.endDate = 'End date is required'
      return
    }

    if (formData.value.startDate && formData.value.endDate) {
      const start = new Date(formData.value.startDate)
      const end = new Date(formData.value.endDate)
      if (start >= end) {
        errors.value.endDate = 'End date must be after start date'
        return
      }
    }

    const cycleData: CreateBillingCycleCommand = {
      ...formData.value
    }

    await billingStore.createBillingCycle(cycleData)

    showToast({
      severity: 'success',
      summary: 'Billing Cycle Created',
      detail: `Billing cycle ${formData.value.cycleName} has been created successfully.`,
      life: 3000
    })

    navigateTo('/billing/cycles')

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to create billing cycle',
      life: 5000
    })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-cycle-page {
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
.cycle-form {
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

.checkbox-field {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
}

.checkbox-field label {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  cursor: pointer;
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

  .form-actions {
    flex-direction: column;
  }

  .form-actions button {
    width: 100%;
  }
}
</style>
