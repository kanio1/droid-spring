<template>
  <div class="payment-form-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/payments" class="back-link">
        ← Back to Payments
      </NuxtLink>
      <h1 class="page-title">Create New Payment</h1>
      <p class="page-subtitle">Record a new customer payment</p>
    </div>

    <!-- Form -->
    <div class="payment-form">
      <form @submit.prevent="handleSubmit" class="form">
        <!-- Payment Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Payment Information</h2>
            <p>Basic payment details</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.paymentNumber"
              label="Payment Number"
              placeholder="e.g., PAY-2024-001"
              :error="errors.paymentNumber"
              required
              @blur="validateField('paymentNumber')"
            />

            <Dropdown
              v-model="formData.paymentMethod"
              :options="paymentMethodOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select payment method"
              label="Payment Method"
              :class="{ 'p-invalid': errors.paymentMethod }"
              style="width: 100%"
              @change="validateField('paymentMethod')"
            />
          </div>

          <div class="form-grid">
            <Dropdown
              v-model="formData.paymentStatus"
              :options="statusOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select status"
              label="Status"
              :class="{ 'p-invalid': errors.paymentStatus }"
              style="width: 100%"
              @change="validateField('paymentStatus')"
            />

            <AppInput
              v-model="formData.currency"
              label="Currency"
              placeholder="e.g., USD, EUR"
              :error="errors.currency"
              required
              @blur="validateField('currency')"
            />
          </div>

          <div class="form-grid">
            <Calendar
              v-model="formData.paymentDate"
              dateFormat="yy-mm-dd"
              placeholder="Select payment date"
              label="Payment Date"
              :class="{ 'p-invalid': errors.paymentDate }"
              style="width: 100%"
              @blur="validateField('paymentDate')"
            />

            <AppInput
              v-model="formData.reference"
              label="Reference"
              placeholder="e.g., INV-2024-001, TXN-12345"
              :error="errors.reference"
              @blur="validateField('reference')"
            />
          </div>
        </div>

        <!-- Customer Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Customer Information</h2>
            <p>Select the customer for this payment</p>
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

        <!-- Financial Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Financial Information</h2>
            <p>Payment amount and transaction details</p>
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

            <div class="transaction-field">
              <label>Transaction ID (Optional)</label>
              <InputText
                v-model="formData.transactionId"
                placeholder="e.g., TXN-12345, AUTH-98765"
                :class="{ 'p-invalid': errors.transactionId }"
                @blur="validateField('transactionId')"
              />
            </div>
          </div>
        </div>

        <!-- Gateway Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Gateway Information</h2>
            <p>Payment gateway response (if applicable)</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.gateway"
              label="Gateway"
              placeholder="e.g., Stripe, PayPal"
              :error="errors.gateway"
              @blur="validateField('gateway')"
            />

            <AppInput
              v-model="formData.authCode"
              label="Authorization Code"
              placeholder="e.g., AUTH12345"
              :error="errors.authCode"
              @blur="validateField('authCode')"
            />
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.lastFour"
              label="Last 4 Digits"
              placeholder="e.g., 1234"
              :error="errors.lastFour"
              @blur="validateField('lastFour')"
            />

            <Dropdown
              v-model="formData.gatewayStatus"
              :options="gatewayStatusOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select gateway status"
              label="Gateway Status"
              style="width: 100%"
            />
          </div>
        </div>

        <!-- Billing Address Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Billing Address</h2>
            <p>Billing address for this payment (optional)</p>
          </div>

          <div class="form-grid form-grid--single">
            <div class="address-field">
              <label>Street Address</label>
              <InputText
                v-model="formData.billingStreet"
                placeholder="e.g., 123 Main St, Apt 4B"
              />
            </div>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.billingCity"
              label="City"
              placeholder="e.g., New York"
            />

            <AppInput
              v-model="formData.billingState"
              label="State/Province"
              placeholder="e.g., NY"
            />
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.billingPostalCode"
              label="Postal Code"
              placeholder="e.g., 10001"
            />

            <AppInput
              v-model="formData.billingCountry"
              label="Country"
              placeholder="e.g., USA"
            />
          </div>
        </div>

        <!-- Notes Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Additional Notes</h2>
            <p>Any additional information for this payment</p>
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
            @click="navigateTo('/payments')"
          />
          <Button
            type="submit"
            label="Create Payment"
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
import { usePaymentStore } from '~/stores/payment'
import type { CreatePaymentCommand } from '~/schemas/payment'

// Page meta
definePageMeta({
  title: 'Create Payment'
})

// Store
const paymentStore = usePaymentStore()
const { showToast } = useToast()

// Reactive state
const submitting = ref(false)
const errors = ref<Record<string, string>>({})

// Form data
const formData = ref<CreatePaymentCommand>({
  paymentNumber: '',
  paymentMethod: 'CARD',
  paymentStatus: 'PENDING',
  currency: 'USD',
  paymentDate: new Date(),
  customerId: '',
  amount: 0,
  reference: '',
  transactionId: '',
  gateway: '',
  authCode: '',
  lastFour: '',
  gatewayStatus: 'SUCCESS',
  notes: '',
  billingAddress: {
    street: '',
    city: '',
    state: '',
    postalCode: '',
    country: ''
  }
})

// Options
const paymentMethodOptions = [
  { label: 'Credit Card', value: 'CARD' },
  { label: 'Bank Transfer', value: 'BANK_TRANSFER' },
  { label: 'Cash', value: 'CASH' },
  { label: 'Direct Debit', value: 'DIRECT_DEBIT' },
  { label: 'Mobile Pay', value: 'MOBILE_PAY' }
]

const statusOptions = [
  { label: 'Pending', value: 'PENDING' },
  { label: 'Processing', value: 'PROCESSING' },
  { label: 'Completed', value: 'COMPLETED' }
]

const gatewayStatusOptions = [
  { label: 'Success', value: 'SUCCESS' },
  { label: 'Failed', value: 'FAILED' },
  { label: 'Declined', value: 'DECLINED' },
  { label: 'Pending', value: 'PENDING' }
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
    if (!formData.value.paymentNumber) {
      errors.value.paymentNumber = 'Payment number is required'
      return
    }

    if (!formData.value.customerId) {
      errors.value.customerId = 'Customer ID is required'
      return
    }

    if (formData.value.amount <= 0) {
      errors.value.amount = 'Amount must be greater than 0'
      return
    }

    const paymentData: CreatePaymentCommand = {
      ...formData.value,
      gatewayResponse: {
        gateway: formData.value.gateway,
        status: formData.value.gatewayStatus,
        authCode: formData.value.authCode,
        lastFour: formData.value.lastFour
      }
    }

    await paymentStore.createPayment(paymentData)

    showToast({
      severity: 'success',
      summary: 'Payment Created',
      detail: `Payment ${formData.value.paymentNumber} has been created successfully.`,
      life: 3000
    })

    navigateTo('/payments')

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to create payment',
      life: 5000
    })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.payment-form-page {
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
.payment-form {
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
.transaction-field,
.address-field,
.notes-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.amount-field label,
.transaction-field label,
.address-field label,
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

  .form-actions {
    flex-direction: column;
  }

  .form-actions button {
    width: 100%;
  }
}
</style>
