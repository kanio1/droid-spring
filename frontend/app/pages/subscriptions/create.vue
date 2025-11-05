<template>
  <div class="subscription-form-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/subscriptions" class="back-link">
        ← Back to Subscriptions
      </NuxtLink>
      <h1 class="page-title">Create New Subscription</h1>
      <p class="page-subtitle">Create a new customer subscription</p>
    </div>

    <!-- Form -->
    <div class="subscription-form">
      <form @submit.prevent="handleSubmit" class="form">
        <!-- Subscription Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Subscription Information</h2>
            <p>Basic subscription details</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.subscriptionNumber"
              label="Subscription Number"
              placeholder="e.g., SUB-2024-001"
              :error="errors.subscriptionNumber"
              required
              @blur="validateField('subscriptionNumber')"
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
            <p>Select the customer for this subscription</p>
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

        <!-- Product Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Product Information</h2>
            <p>Select the product for this subscription</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.productId"
              label="Product ID"
              placeholder="Enter product UUID"
              :error="errors.productId"
              required
              @blur="validateField('productId')"
            />

            <Dropdown
              v-model="formData.billingPeriod"
              :options="billingPeriodOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select billing period"
              label="Billing Period"
              :class="{ 'p-invalid': errors.billingPeriod }"
              style="width: 100%"
              @change="validateField('billingPeriod')"
            />
          </div>

          <div class="form-grid">
            <div class="amount-field">
              <label>Amount</label>
              <InputNumber
                v-model="formData.amount"
                :min="0"
                :step="0.01"
                mode="currency"
                currency="USD"
                locale="en-US"
                :class="{ 'p-invalid': errors.amount }"
                @blur="validateField('amount')"
                style="width: 100%"
              />
            </div>

            <AppInput
              v-model="formData.currency"
              label="Currency"
              placeholder="e.g., USD, EUR"
              :error="errors.currency"
              required
              @blur="validateField('currency')"
            />
          </div>
        </div>

        <!-- Date Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Date Information</h2>
            <p>Subscription start and end dates</p>
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
              placeholder="Select end date (optional)"
              label="End Date (Optional)"
              style="width: 100%"
            />
          </div>

          <div class="form-grid">
            <Calendar
              v-model="formData.nextBillingDate"
              dateFormat="yy-mm-dd"
              placeholder="Select next billing date"
              label="Next Billing Date"
              :class="{ 'p-invalid': errors.nextBillingDate }"
              style="width: 100%"
              @blur="validateField('nextBillingDate')"
            />
          </div>
        </div>

        <!-- Renewal Settings Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Renewal Settings</h2>
            <p>Configure automatic renewal and notifications</p>
          </div>

          <div class="form-grid">
            <div class="checkbox-field">
              <Checkbox
                v-model="formData.autoRenew"
                :binary="true"
                inputId="autoRenew"
              />
              <label for="autoRenew">Enable Auto Renew</label>
            </div>

            <div class="checkbox-field">
              <Checkbox
                v-model="formData.sendNotifications"
                :binary="true"
                inputId="sendNotifications"
              />
              <label for="sendNotifications">Send Renewal Notifications</label>
            </div>
          </div>
        </div>

        <!-- Features Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Features</h2>
            <p>Add subscription features (optional)</p>
          </div>

          <div class="form-grid form-grid--single">
            <div class="features-field">
              <label>Features (one per line)</label>
              <Textarea
                v-model="featuresText"
                placeholder="Unlimited data&#10;24/7 support&#10;Free installation"
                rows="5"
                auto-resize
              />
            </div>
          </div>
        </div>

        <!-- Notes Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Additional Notes</h2>
            <p>Any additional information for this subscription</p>
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
            @click="navigateTo('/subscriptions')"
          />
          <Button
            type="submit"
            label="Create Subscription"
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
import { useSubscriptionStore } from '~/stores/subscription'
import type { CreateSubscriptionCommand } from '~/schemas/subscription'

// Page meta
definePageMeta({
  title: 'Create Subscription'
})

// Store
const subscriptionStore = useSubscriptionStore()
const { showToast } = useToast()

// Reactive state
const submitting = ref(false)
const errors = ref<Record<string, string>>({})
const featuresText = ref('')

// Form data
const formData = ref<CreateSubscriptionCommand>({
  subscriptionNumber: '',
  customerId: '',
  productId: '',
  status: 'ACTIVE',
  billingPeriod: 'MONTHLY',
  startDate: new Date(),
  endDate: null,
  nextBillingDate: new Date(),
  amount: 0,
  currency: 'USD',
  autoRenew: false,
  sendNotifications: true,
  features: [],
  notes: ''
})

// Options
const statusOptions = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Suspended', value: 'SUSPENDED' }
]

const billingPeriodOptions = [
  { label: 'Monthly', value: 'MONTHLY' },
  { label: 'Quarterly', value: 'QUARTERLY' },
  { label: 'Yearly', value: 'YEARLY' }
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
    if (!formData.value.subscriptionNumber) {
      errors.value.subscriptionNumber = 'Subscription number is required'
      return
    }

    if (!formData.value.customerId) {
      errors.value.customerId = 'Customer ID is required'
      return
    }

    if (!formData.value.productId) {
      errors.value.productId = 'Product ID is required'
      return
    }

    if (formData.value.amount <= 0) {
      errors.value.amount = 'Amount must be greater than 0'
      return
    }

    // Parse features from text
    const features = featuresText.value
      .split('\n')
      .map(f => f.trim())
      .filter(f => f.length > 0)

    const subscriptionData: CreateSubscriptionCommand = {
      ...formData.value,
      features
    }

    await subscriptionStore.createSubscription(subscriptionData)

    showToast({
      severity: 'success',
      summary: 'Subscription Created',
      detail: `Subscription ${formData.value.subscriptionNumber} has been created successfully.`,
      life: 3000
    })

    navigateTo('/subscriptions')

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to create subscription',
      life: 5000
    })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.subscription-form-page {
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
.subscription-form {
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

.amount-field,
.features-field,
.notes-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.amount-field label,
.features-field label,
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
