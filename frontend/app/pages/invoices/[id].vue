<template>
  <div class="invoice-detail-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/invoices" class="back-link">
        ← Back to Invoices
      </NuxtLink>
      <div class="page-header__content">
        <div class="title-row">
          <h1 class="page-title">Invoice {{ invoice?.invoiceNumber || '...' }}</h1>
          <StatusBadge v-if="invoice" :status="invoice.status" type="invoice" size="large" />
        </div>
        <p class="page-subtitle" v-if="invoice">
          {{ invoice.invoiceType }} invoice for {{ invoice.customerName }}
        </p>
      </div>
      <div class="page-header__actions">
        <Button
          v-if="canEdit"
          label="Edit"
          icon="pi pi-pencil"
          severity="primary"
          @click="handleEdit"
        />
        <Button
          v-if="canSend"
          label="Send Invoice"
          icon="pi pi-send"
          @click="handleSend"
        />
        <Button
          v-if="canCancel"
          label="Cancel"
          icon="pi pi-times"
          severity="danger"
          @click="handleCancel"
        />
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner />
      <p>Loading invoice details...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <i class="pi pi-exclamation-triangle"></i>
      <p>{{ error }}</p>
      <Button label="Retry" @click="fetchInvoice" />
    </div>

    <div v-else-if="invoice" class="invoice-content">
      <!-- Invoice Summary Card -->
      <div class="card invoice-summary">
        <div class="card-header">
          <h2>Invoice Summary</h2>
        </div>
        <div class="card-body">
          <div class="summary-grid">
            <div class="summary-item">
              <label>Invoice Number</label>
              <span>{{ invoice.invoiceNumber }}</span>
            </div>
            <div class="summary-item">
              <label>Invoice Date</label>
              <span>{{ formatDate(invoice.invoiceDate) }}</span>
            </div>
            <div class="summary-item">
              <label>Due Date</label>
              <span :class="{ 'text-danger': isOverdue(invoice) }">
                {{ formatDate(invoice.dueDate) }}
                <i v-if="isOverdue(invoice)" class="pi pi-exclamation-triangle ml-2"></i>
              </span>
            </div>
            <div class="summary-item">
              <label>Invoice Type</label>
              <StatusBadge :status="invoice.invoiceType" type="invoice-type" />
            </div>
            <div class="summary-item">
              <label>Customer</label>
              <div class="customer-info">
                <div class="customer-avatar">
                  {{ getCustomerInitials(invoice) }}
                </div>
                <div>
                  <div class="customer-name">{{ invoice.customerName }}</div>
                  <div class="customer-id">{{ invoice.customerId }}</div>
                </div>
              </div>
            </div>
            <div class="summary-item">
              <label>Currency</label>
              <span>{{ invoice.currency }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Invoice Items -->
      <div class="card invoice-items">
        <div class="card-header">
          <h2>Invoice Items</h2>
        </div>
        <div class="card-body">
          <AppTable :columns="itemColumns" :data="invoice.items || []" :show-pagination="false">
            <template #cell-description="{ row }">
              <div class="item-description">
                <div class="description-text">{{ row.description }}</div>
                <div v-if="row.notes" class="description-notes">{{ row.notes }}</div>
              </div>
            </template>
            <template #cell-unitPrice="{ row }">
              <span class="amount">{{ formatCurrency(row.unitPrice, invoice.currency) }}</span>
            </template>
            <template #cell-total="{ row }">
              <span class="amount total">{{ formatCurrency(row.total, invoice.currency) }}</span>
            </template>
            <template #empty>
              <div class="empty-items">No items in this invoice</div>
            </template>
          </AppTable>
        </div>
      </div>

      <!-- Totals -->
      <div class="card invoice-totals">
        <div class="card-body">
          <div class="totals-grid">
            <div class="total-row">
              <span>Subtotal:</span>
              <span class="amount">{{ formatCurrency(invoice.subtotal, invoice.currency) }}</span>
            </div>
            <div class="total-row">
              <span>Tax ({{ invoice.taxRate }}%):</span>
              <span class="amount">{{ formatCurrency(invoice.taxAmount, invoice.currency) }}</span>
            </div>
            <div class="total-row total-final">
              <span>Total:</span>
              <span class="amount">{{ formatCurrency(invoice.totalAmount, invoice.currency) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Notes -->
      <div v-if="invoice.notes" class="card invoice-notes">
        <div class="card-header">
          <h2>Notes</h2>
        </div>
        <div class="card-body">
          <p>{{ invoice.notes }}</p>
        </div>
      </div>

      <!-- Payment History -->
      <div v-if="payments.length > 0" class="card payment-history">
        <div class="card-header">
          <h2>Payment History</h2>
        </div>
        <div class="card-body">
          <AppTable :columns="paymentColumns" :data="payments" :show-pagination="false">
            <template #cell-amount="{ row }">
              <span class="amount">{{ formatCurrency(row.amount, invoice.currency) }}</span>
            </template>
            <template #cell-paymentDate="{ row }">
              {{ formatDate(row.paymentDate) }}
            </template>
            <template #cell-status="{ row }">
              <StatusBadge :status="row.status" type="payment" size="small" />
            </template>
          </AppTable>
        </div>
      </div>

      <!-- Metadata -->
      <div class="card invoice-metadata">
        <div class="card-header">
          <h2>Metadata</h2>
        </div>
        <div class="card-body">
          <div class="metadata-grid">
            <div class="metadata-item">
              <label>Created</label>
              <span>{{ formatDateTime(invoice.createdAt) }}</span>
            </div>
            <div class="metadata-item">
              <label>Last Updated</label>
              <span>{{ formatDateTime(invoice.updatedAt) }}</span>
            </div>
            <div class="metadata-item">
              <label>Version</label>
              <span>{{ invoice.version }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast for notifications -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { useInvoiceStore } from '~/stores/invoice'
import { isInvoiceOverdue, canCancelInvoice, canSendInvoice } from '~/schemas/invoice'

// Page meta
definePageMeta({
  title: 'Invoice Details'
})

// Route params
const route = useRoute()
const invoiceId = route.params.id as string

// Store
const invoiceStore = useInvoiceStore()
const { showToast } = useToast()

// Reactive state
const loading = ref(true)
const error = ref<string | null>(null)
const payments = ref<any[]>([])

// Computed
const invoice = computed(() => invoiceStore.currentInvoice)
const canEdit = computed(() => invoice.value && (invoice.value.status === 'DRAFT' || invoice.value.status === 'ISSUED'))
const canSend = computed(() => invoice.value && canSendInvoice(invoice.value))
const canCancel = computed(() => invoice.value && canCancelInvoice(invoice.value))

// Table columns
const itemColumns = [
  { key: 'description', label: 'Description', style: 'width: 50%' },
  { key: 'quantity', label: 'Qty', style: 'width: 10%' },
  { key: 'unitPrice', label: 'Unit Price', style: 'width: 20%', align: 'right' },
  { key: 'total', label: 'Total', style: 'width: 20%', align: 'right' }
]

const paymentColumns = [
  { key: 'paymentNumber', label: 'Payment #', style: 'width: 20%' },
  { key: 'amount', label: 'Amount', style: 'width: 20%', align: 'right' },
  { key: 'paymentDate', label: 'Date', style: 'width: 20%' },
  { key: 'paymentMethod', label: 'Method', style: 'width: 20%' },
  { key: 'status', label: 'Status', style: 'width: 20%' }
]

// Methods
const fetchInvoice = async () => {
  try {
    loading.value = true
    error.value = null
    await invoiceStore.fetchInvoiceById(invoiceId)
  } catch (err: any) {
    error.value = err.message || 'Failed to load invoice'
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  navigateTo(`/invoices/${invoiceId}/edit`)
}

const handleSend = async () => {
  if (!invoice.value) return

  try {
    await invoiceStore.changeInvoiceStatus({
      id: invoiceId,
      status: 'SENT'
    })

    showToast({
      severity: 'success',
      summary: 'Invoice Sent',
      detail: `Invoice ${invoice.value.invoiceNumber} has been sent to customer.`,
      life: 3000
    })

    await fetchInvoice()

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to send invoice',
      life: 5000
    })
  }
}

const handleCancel = async () => {
  if (!invoice.value) return

  const confirmed = confirm(`Are you sure you want to cancel invoice ${invoice.value.invoiceNumber}?`)

  if (confirmed) {
    try {
      await invoiceStore.changeInvoiceStatus({
        id: invoiceId,
        status: 'CANCELLED'
      })

      showToast({
        severity: 'success',
        summary: 'Invoice Cancelled',
        detail: `Invoice ${invoice.value.invoiceNumber} has been cancelled.`,
        life: 3000
      })

      await fetchInvoice()

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to cancel invoice',
        life: 5000
      })
    }
  }
}

// Utility functions
const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const formatDateTime = (dateString: string): string => {
  return new Date(dateString).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatCurrency = (amount: number, currency: string): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency
  }).format(amount)
}

const getCustomerInitials = (row: any): string => {
  if (!row.customerName) return 'N/A'
  const names = row.customerName.split(' ')
  if (names.length >= 2) {
    return `${names[0][0]}${names[1][0]}`.toUpperCase()
  }
  return row.customerName.substring(0, 2).toUpperCase()
}

const isOverdue = (row: any): boolean => {
  return isInvoiceOverdue(row)
}

// Lifecycle
onMounted(async () => {
  await fetchInvoice()
})
</script>

<style scoped>
.invoice-detail-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* Page Header */
.page-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
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
}

.back-link:hover {
  text-decoration: underline;
}

.page-header__content {
  flex: 1;
}

.title-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-1);
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

.page-header__actions {
  display: flex;
  gap: var(--space-2);
  align-self: flex-start;
}

/* Loading and Error States */
.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  text-align: center;
}

.error-state i {
  font-size: 3rem;
  color: var(--color-red-500);
  margin-bottom: var(--space-4);
}

/* Cards */
.card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.card-header {
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
}

.card-header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.card-body {
  padding: var(--space-6);
}

/* Invoice Summary */
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.summary-item label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.summary-item > span {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
}

.customer-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.customer-avatar {
  width: 40px;
  height: 40px;
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  flex-shrink: 0;
}

.customer-name {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.customer-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Invoice Items */
.item-description {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.description-text {
  color: var(--color-text-primary);
}

.description-notes {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-style: italic;
}

.empty-items {
  text-align: center;
  padding: var(--space-4);
  color: var(--color-text-muted);
}

/* Totals */
.totals-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  max-width: 400px;
  margin-left: auto;
}

.total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--color-text-primary);
}

.total-row.total-final {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
  padding-top: var(--space-3);
  border-top: 2px solid var(--color-border);
}

.amount {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.amount.total {
  font-size: var(--font-size-lg);
}

/* Notes */
.invoice-notes p {
  margin: 0;
  color: var(--color-text-primary);
  white-space: pre-wrap;
}

/* Payment History */
.payment-history {
  border: 2px solid var(--color-green-200);
}

/* Metadata */
.metadata-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.metadata-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.metadata-item label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.metadata-item > span {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    gap: var(--space-3);
  }

  .title-row {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }

  .page-header__actions {
    width: 100%;
    flex-direction: column;
  }

  .page-header__actions button {
    width: 100%;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .totals-grid {
    max-width: 100%;
  }

  .metadata-grid {
    grid-template-columns: 1fr;
  }
}
</style>
