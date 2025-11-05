<template>
  <div class="invoice-form-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/invoices" class="back-link">
        ← Back to Invoices
      </NuxtLink>
      <h1 class="page-title">Create New Invoice</h1>
      <p class="page-subtitle">Create a new customer invoice</p>
    </div>

    <!-- Form -->
    <div class="invoice-form">
      <form @submit.prevent="handleSubmit" class="form">
        <!-- Invoice Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Invoice Information</h2>
            <p>Basic invoice details</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.invoiceNumber"
              label="Invoice Number"
              placeholder="e.g., INV-2024-001"
              :error="errors.invoiceNumber"
              required
              @blur="validateField('invoiceNumber')"
            />

            <Dropdown
              v-model="formData.invoiceType"
              :options="invoiceTypeOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Select invoice type"
              label="Invoice Type"
              :class="{ 'p-invalid': errors.invoiceType }"
              style="width: 100%"
              @change="validateField('invoiceType')"
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
              v-model="formData.invoiceDate"
              dateFormat="yy-mm-dd"
              placeholder="Select invoice date"
              label="Invoice Date"
              :class="{ 'p-invalid': errors.invoiceDate }"
              style="width: 100%"
              @blur="validateField('invoiceDate')"
            />

            <Calendar
              v-model="formData.dueDate"
              dateFormat="yy-mm-dd"
              placeholder="Select due date"
              label="Due Date"
              :class="{ 'p-invalid': errors.dueDate }"
              style="width: 100%"
              @blur="validateField('dueDate')"
            />
          </div>
        </div>

        <!-- Customer Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Customer Information</h2>
            <p>Select the customer for this invoice</p>
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

        <!-- Invoice Items Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Invoice Items</h2>
            <p>Add line items to this invoice</p>
          </div>

          <div class="items-list">
            <div v-for="(item, index) in formData.items" :key="index" class="item-row">
              <div class="item-row__header">
                <h3>Item {{ index + 1 }}</h3>
                <Button
                  icon="pi pi-trash"
                  text
                  rounded
                  severity="danger"
                  @click="removeItem(index)"
                  v-tooltip.top="'Remove item'"
                  :disabled="formData.items.length === 1"
                />
              </div>

              <div class="form-grid">
                <div class="item-description">
                  <label>Description</label>
                  <InputText
                    v-model="item.description"
                    placeholder="Enter item description"
                    :class="{ 'p-invalid': errors[`items.${index}.description`] }"
                    @blur="validateField(`items.${index}.description`)"
                  />
                </div>

                <div class="item-quantity">
                  <label>Quantity</label>
                  <InputNumber
                    v-model="item.quantity"
                    :min="1"
                    :step="1"
                    :class="{ 'p-invalid': errors[`items.${index}.quantity`] }"
                    @blur="validateField(`items.${index}.quantity`)"
                  />
                </div>

                <div class="item-unit-price">
                  <label>Unit Price</label>
                  <InputNumber
                    v-model="item.unitPrice"
                    :min="0"
                    :step="0.01"
                    mode="currency"
                    currency="USD"
                    locale="en-US"
                    :class="{ 'p-invalid': errors[`items.${index}.unitPrice`] }"
                    @blur="validateField(`items.${index}.unitPrice`)"
                  />
                </div>

                <div class="item-total">
                  <label>Total</label>
                  <InputText
                    :value="formatCurrency(item.quantity * item.unitPrice, formData.currency)"
                    readonly
                  />
                </div>
              </div>

              <div class="form-grid form-grid--single">
                <div class="item-notes">
                  <label>Notes (Optional)</label>
                  <InputText
                    v-model="item.notes"
                    placeholder="Additional notes for this item"
                  />
                </div>
              </div>
            </div>

            <Button
              label="Add Item"
              icon="pi pi-plus"
              outlined
              @click="addItem"
            />
          </div>
        </div>

        <!-- Financial Summary Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Financial Summary</h2>
            <p>Invoice totals and tax information</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model.number="formData.taxRate"
              label="Tax Rate (%)"
              type="number"
              placeholder="e.g., 23"
              :error="errors.taxRate"
              @blur="validateField('taxRate')"
            />

            <div class="summary-display">
              <div class="summary-row">
                <span>Subtotal:</span>
                <span class="amount">{{ formatCurrency(subtotal, formData.currency) }}</span>
              </div>
              <div class="summary-row">
                <span>Tax ({{ formData.taxRate }}%):</span>
                <span class="amount">{{ formatCurrency(taxAmount, formData.currency) }}</span>
              </div>
              <div class="summary-row summary-final">
                <span>Total:</span>
                <span class="amount">{{ formatCurrency(total, formData.currency) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Notes Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Additional Notes</h2>
            <p>Any additional information for the invoice</p>
          </div>

          <div class="form-grid form-grid--single">
            <div class="notes-field">
              <label>Notes (Optional)</label>
              <Textarea
                v-model="formData.notes"
                placeholder="Enter any additional notes or terms"
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
            @click="navigateTo('/invoices')"
          />
          <Button
            type="submit"
            label="Create Invoice"
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
import { useInvoiceStore } from '~/stores/invoice'
import type { CreateInvoiceCommand } from '~/schemas/invoice'

// Page meta
definePageMeta({
  title: 'Create Invoice'
})

// Store
const invoiceStore = useInvoiceStore()
const { showToast } = useToast()

// Reactive state
const submitting = ref(false)
const errors = ref<Record<string, string>>({})

// Form data
const formData = ref<CreateInvoiceCommand>({
  invoiceNumber: '',
  invoiceType: 'ONE_TIME',
  status: 'DRAFT',
  currency: 'USD',
  invoiceDate: new Date(),
  dueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000), // 30 days from now
  customerId: '',
  taxRate: 23,
  notes: '',
  items: [
    {
      description: '',
      quantity: 1,
      unitPrice: 0,
      total: 0,
      notes: ''
    }
  ]
})

// Options
const invoiceTypeOptions = [
  { label: 'One Time', value: 'ONE_TIME' },
  { label: 'Recurring', value: 'RECURRING' },
  { label: 'Adjustment', value: 'ADJUSTMENT' }
]

const statusOptions = [
  { label: 'Draft', value: 'DRAFT' },
  { label: 'Issued', value: 'ISSUED' }
]

// Computed
const subtotal = computed(() => {
  return formData.value.items.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0)
})

const taxAmount = computed(() => {
  return subtotal.value * (formData.value.taxRate / 100)
})

const total = computed(() => {
  return subtotal.value + taxAmount.value
})

// Methods
const addItem = () => {
  formData.value.items.push({
    description: '',
    quantity: 1,
    unitPrice: 0,
    total: 0,
    notes: ''
  })
}

const removeItem = (index: number) => {
  if (formData.value.items.length > 1) {
    formData.value.items.splice(index, 1)
  }
}

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
    if (!formData.value.invoiceNumber) {
      errors.value.invoiceNumber = 'Invoice number is required'
      return
    }

    if (!formData.value.customerId) {
      errors.value.customerId = 'Customer ID is required'
      return
    }

    const invoiceData: CreateInvoiceCommand = {
      ...formData.value,
      subtotal: subtotal.value,
      taxAmount: taxAmount.value,
      totalAmount: total.value
    }

    await invoiceStore.createInvoice(invoiceData)

    showToast({
      severity: 'success',
      summary: 'Invoice Created',
      detail: `Invoice ${formData.value.invoiceNumber} has been created successfully.`,
      life: 3000
    })

    navigateTo('/invoices')

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to create invoice',
      life: 5000
    })
  } finally {
    submitting.value = false
  }
}

// Utility functions
const formatCurrency = (amount: number, currency: string = 'USD'): string => {
  if (!amount) return formatCurrency(0, currency)
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency
  }).format(amount)
}
</script>

<style scoped>
.invoice-form-page {
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
.invoice-form {
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

/* Items List */
.items-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.item-row {
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-surface-secondary);
}

.item-row__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.item-row__header h3 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.item-description,
.item-quantity,
.item-unit-price,
.item-total,
.item-notes {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.item-description label,
.item-quantity label,
.item-unit-price label,
.item-total label,
.item-notes label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

/* Summary Display */
.summary-display {
  padding: var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) 0;
  color: var(--color-text-primary);
}

.summary-final {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  padding-top: var(--space-3);
  border-top: 2px solid var(--color-border);
  margin-top: var(--space-2);
}

.amount {
  font-weight: var(--font-weight-semibold);
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
