<template>
  <div class="order-form-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/orders" class="back-link">
        ← Back to Orders
      </NuxtLink>
      <h1 class="page-title">Create New Order</h1>
      <p class="page-subtitle">Create a new customer order</p>
    </div>

    <!-- Form -->
    <div class="order-form">
      <form @submit.prevent="handleSubmit" class="form">
        <!-- Order Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Order Information</h2>
            <p>Basic order details</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.orderNumber"
              label="Order Number"
              placeholder="e.g., ORD-2024-001"
              :error="errors.orderNumber"
              required
              @blur="validateField('orderNumber')"
            />

            <Dropdown
              v-model="formData.orderType"
              :options="orderTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select order type"
              label="Order Type"
              :class="{ 'p-invalid': errors.orderType }"
              style="width: 100%"
              @change="validateField('orderType')"
            />
          </div>

          <div class="form-grid">
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

            <Dropdown
              v-model="formData.priority"
              :options="priorityOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select priority"
              label="Priority"
              :class="{ 'p-invalid': errors.priority }"
              style="width: 100%"
              @change="validateField('priority')"
            />
          </div>
        </div>

        <!-- Customer Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Customer Information</h2>
            <p>Select the customer for this order</p>
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
            <p>Order amounts and payment terms</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model.number="formData.totalAmount"
              label="Total Amount"
              type="number"
              placeholder="0.00"
              :error="errors.totalAmount"
              @blur="validateField('totalAmount')"
            />

            <Dropdown
              v-model="formData.currency"
              :options="currencyOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select currency"
              label="Currency"
              style="width: 100%"
            />
          </div>
        </div>

        <!-- Scheduling Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Scheduling</h2>
            <p>Important dates for this order</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.requestedDate"
              label="Requested Date"
              type="date"
              :error="errors.requestedDate"
              @blur="validateField('requestedDate')"
            />

            <AppInput
              v-model="formData.promisedDate"
              label="Promised Date"
              type="date"
              :error="errors.promisedDate"
              @blur="validateField('promisedDate')"
            />
          </div>
        </div>

        <!-- Additional Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Additional Information</h2>
            <p>Channel, sales rep, and notes</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.orderChannel"
              label="Order Channel"
              placeholder="e.g., Web, Phone, Email"
              :error="errors.orderChannel"
              @blur="validateField('orderChannel')"
            />

            <AppInput
              v-model="formData.salesRepId"
              label="Sales Representative ID"
              placeholder="Enter sales rep ID"
              :error="errors.salesRepId"
              @blur="validateField('salesRepId')"
            />
          </div>

          <div class="form-grid form-grid--single">
            <div class="form-field">
              <label class="form-label">Notes</label>
              <Textarea
                v-model="formData.notes"
                rows="4"
                placeholder="Enter any additional notes..."
                :class="{ 'p-invalid': errors.notes }"
                style="width: 100%"
                @blur="validateField('notes')"
              />
              <small v-if="errors.notes" class="p-error">{{ errors.notes }}</small>
            </div>
          </div>
        </div>

        <!-- Form Actions -->
        <div class="form-actions">
          <Button
            type="button"
            variant="secondary"
            @click="handleCancel"
            :disabled="submitting"
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            :loading="submitting"
            :disabled="!isFormValid || submitting"
          >
            {{ submitting ? 'Creating...' : 'Create Order' }}
          </Button>
        </div>
      </form>
    </div>

    <!-- Toast for notifications -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { useOrderStore } from '~/stores/order'
import type { CreateOrderCommand } from '~/schemas/order'

// Page meta
definePageMeta({
  title: 'Create Order'
})

// Store
const orderStore = useOrderStore()
const { showToast } = useToast()

// Reactive state
const submitting = ref(false)
const formData = reactive<CreateOrderCommand>({
  orderNumber: '',
  customerId: '',
  orderType: 'NEW',
  status: 'PENDING',
  priority: 'NORMAL',
  totalAmount: 0,
  currency: 'PLN',
  requestedDate: '',
  promisedDate: '',
  orderChannel: '',
  salesRepId: '',
  notes: ''
})

const errors = reactive<Record<string, string>>({})

// Form options
const orderTypeOptions = [
  { label: 'New', value: 'NEW' },
  { label: 'Upgrade', value: 'UPGRADE' },
  { label: 'Downgrade', value: 'DOWNGRADE' },
  { label: 'Cancel', value: 'CANCEL' },
  { label: 'Renew', value: 'RENEW' }
]

const statusOptions = [
  { label: 'Pending', value: 'PENDING' },
  { label: 'Confirmed', value: 'CONFIRMED' },
  { label: 'Processing', value: 'PROCESSING' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

const priorityOptions = [
  { label: 'Low', value: 'LOW' },
  { label: 'Normal', value: 'NORMAL' },
  { label: 'High', value: 'HIGH' },
  { label: 'Urgent', value: 'URGENT' }
]

const currencyOptions = [
  { label: 'Polish Złoty (PLN)', value: 'PLN' },
  { label: 'US Dollar (USD)', value: 'USD' },
  { label: 'Euro (EUR)', value: 'EUR' }
]

// Computed
const isFormValid = computed(() => {
  return !!(
    formData.orderNumber &&
    formData.customerId &&
    formData.orderType &&
    formData.status &&
    formData.priority &&
    Object.keys(errors).length === 0
  )
})

// Validation methods
const validateField = (field: string) => {
  switch (field) {
    case 'orderNumber':
      if (!formData.orderNumber) {
        errors.orderNumber = 'Order number is required'
      } else if (formData.orderNumber.length < 3) {
        errors.orderNumber = 'Order number must be at least 3 characters'
      } else if (formData.orderNumber.length > 50) {
        errors.orderNumber = 'Order number must not exceed 50 characters'
      } else {
        delete errors.orderNumber
      }
      break

    case 'customerId':
      if (!formData.customerId) {
        errors.customerId = 'Customer ID is required'
      } else if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(formData.customerId)) {
        errors.customerId = 'Invalid customer ID format'
      } else {
        delete errors.customerId
      }
      break

    case 'orderType':
      if (!formData.orderType) {
        errors.orderType = 'Order type is required'
      } else {
        delete errors.orderType
      }
      break

    case 'status':
      if (!formData.status) {
        errors.status = 'Status is required'
      } else {
        delete errors.status
      }
      break

    case 'priority':
      if (!formData.priority) {
        errors.priority = 'Priority is required'
      } else {
        delete errors.priority
      }
      break

    case 'totalAmount':
      if (formData.totalAmount < 0) {
        errors.totalAmount = 'Total amount must be positive'
      } else {
        delete errors.totalAmount
      }
      break

    case 'notes':
      if (formData.notes && formData.notes.length > 2000) {
        errors.notes = 'Notes must not exceed 2000 characters'
      } else {
        delete errors.notes
      }
      break
  }
}

// Form methods
const handleSubmit = async () => {
  // Validate all fields
  Object.keys(formData).forEach(field => {
    if (typeof formData[field as keyof CreateOrderCommand] !== 'function') {
      validateField(field)
    }
  })

  if (!isFormValid.value) {
    showToast({
      severity: 'error',
      summary: 'Validation Error',
      detail: 'Please fix the errors in the form',
      life: 5000
    })
    return
  }

  submitting.value = true

  try {
    const orderData = {
      ...formData,
      requestedDate: formData.requestedDate || undefined,
      promisedDate: formData.promisedDate || undefined,
      orderChannel: formData.orderChannel || undefined,
      salesRepId: formData.salesRepId || undefined,
      notes: formData.notes || undefined
    }

    const newOrder = await orderStore.createOrder(orderData)

    showToast({
      severity: 'success',
      summary: 'Order Created',
      detail: `Order ${newOrder.orderNumber} has been successfully created.`,
      life: 3000
    })

    // Redirect to order details
    navigateTo(`/orders/${newOrder.id}`)

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to create order',
      life: 5000
    })
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  navigateTo('/orders')
}
</script>

<style scoped>
.order-form-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* Page Header */
.page-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
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
  transition: color var(--transition-fast) var(--transition-timing);
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

/* Form */
.order-form {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.form {
  display: flex;
  flex-direction: column;
}

.form-section {
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
}

.form-section:last-child {
  border-bottom: none;
}

.form-section__header {
  margin-bottom: var(--space-4);
}

.form-section__header h2 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.form-section__header p {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.form-grid--single {
  grid-template-columns: 1fr;
}

.form-grid:last-child {
  margin-bottom: 0;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.form-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.form-help-text {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3);
  background: var(--color-surface-alt);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: var(--space-2);
}

.form-help-text i {
  color: var(--color-info);
}

/* Form Actions */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding: var(--space-6);
  background: var(--color-surface-alt);
  border-top: 1px solid var(--color-border);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .form-actions > * {
    width: 100%;
  }
}
</style>
