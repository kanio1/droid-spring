<template>
  <div class="payment-detail-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/payments" class="back-link">
        ← Back to Payments
      </NuxtLink>
      <div class="page-header__content">
        <div class="title-row">
          <h1 class="page-title">Payment {{ payment?.paymentNumber || '...' }}</h1>
          <StatusBadge v-if="payment" :status="payment.paymentStatus" type="payment" size="large" />
        </div>
        <p class="page-subtitle" v-if="payment">
          {{ payment.paymentMethod }} payment for {{ payment.customerName }}
        </p>
      </div>
      <div class="page-header__actions">
        <Button
          v-if="canRetry"
          label="Retry Payment"
          icon="pi pi-refresh"
          severity="info"
          @click="handleRetry"
        />
        <Button
          v-if="canEdit"
          label="Edit"
          icon="pi pi-pencil"
          severity="primary"
          @click="handleEdit"
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
      <p>Loading payment details...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <i class="pi pi-exclamation-triangle"></i>
      <p>{{ error }}</p>
      <Button label="Retry" @click="fetchPayment" />
    </div>

    <div v-else-if="payment" class="payment-content">
      <!-- Payment Summary Card -->
      <div class="card payment-summary">
        <div class="card-header">
          <h2>Payment Summary</h2>
        </div>
        <div class="card-body">
          <div class="summary-grid">
            <div class="summary-item">
              <label>Payment Number</label>
              <span>{{ payment.paymentNumber }}</span>
            </div>
            <div class="summary-item">
              <label>Payment Date</label>
              <span>{{ payment.paymentDate ? formatDate(payment.paymentDate) : 'Not paid yet' }}</span>
            </div>
            <div class="summary-item">
              <label>Amount</label>
              <span class="amount">{{ formatCurrency(payment.amount, payment.currency) }}</span>
            </div>
            <div class="summary-item">
              <label>Payment Method</label>
              <StatusBadge :status="payment.paymentMethod" type="payment-method" />
            </div>
            <div class="summary-item">
              <label>Transaction ID</label>
              <span class="mono">{{ payment.transactionId || 'N/A' }}</span>
            </div>
            <div class="summary-item">
              <label>Customer</label>
              <div class="customer-info">
                <div class="customer-avatar">
                  {{ getCustomerInitials(payment) }}
                </div>
                <div>
                  <div class="customer-name">{{ payment.customerName }}</div>
                  <div class="customer-id">{{ payment.customerId }}</div>
                </div>
              </div>
            </div>
            <div v-if="payment.invoiceId" class="summary-item">
              <label>Invoice</label>
              <NuxtLink :to="`/invoices/${payment.invoiceId}`" class="invoice-link">
                {{ payment.invoiceNumber || payment.invoiceId }}
                <i class="pi pi-external-link ml-1"></i>
              </NuxtLink>
            </div>
            <div class="summary-item">
              <label>Reference</label>
              <span class="mono">{{ payment.reference || 'N/A' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Payment Details -->
      <div v-if="payment.gatewayResponse" class="card payment-details">
        <div class="card-header">
          <h2>Payment Details</h2>
        </div>
        <div class="card-body">
          <div class="details-grid">
            <div class="detail-item">
              <label>Gateway</label>
              <span>{{ payment.gatewayResponse.gateway || 'N/A' }}</span>
            </div>
            <div class="detail-item">
              <label>Gateway Status</label>
              <StatusBadge :status="payment.gatewayResponse.status || 'UNKNOWN'" type="gateway" />
            </div>
            <div class="detail-item">
              <label>Authorization Code</label>
              <span class="mono">{{ payment.gatewayResponse.authCode || 'N/A' }}</span>
            </div>
            <div class="detail-item">
              <label>Last 4 Digits</label>
              <span class="mono">{{ payment.gatewayResponse.lastFour || 'N/A' }}</span>
            </div>
          </div>

          <div v-if="payment.gatewayResponse.rawResponse" class="raw-response">
            <label>Raw Gateway Response</label>
            <pre>{{ JSON.stringify(payment.gatewayResponse.rawResponse, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <!-- Billing Information -->
      <div v-if="payment.billingAddress" class="card billing-address">
        <div class="card-header">
          <h2>Billing Address</h2>
        </div>
        <div class="card-body">
          <div class="address-block">
            <p>{{ payment.billingAddress.street }}</p>
            <p>{{ payment.billingAddress.city }}, {{ payment.billingAddress.state }} {{ payment.billingAddress.postalCode }}</p>
            <p>{{ payment.billingAddress.country }}</p>
          </div>
        </div>
      </div>

      <!-- Notes -->
      <div v-if="payment.notes" class="card payment-notes">
        <div class="card-header">
          <h2>Notes</h2>
        </div>
        <div class="card-body">
          <p>{{ payment.notes }}</p>
        </div>
      </div>

      <!-- Audit Log -->
      <div class="card audit-log">
        <div class="card-header">
          <h2>Audit Log</h2>
        </div>
        <div class="card-body">
          <AppTable :columns="auditColumns" :data="payment.auditLog || []" :show-pagination="false">
            <template #cell-timestamp="{ row }">
              {{ formatDateTime(row.timestamp) }}
            </template>
            <template #cell-action="{ row }">
              <span class="audit-action">{{ row.action }}</span>
            </template>
            <template #cell-user="{ row }">
              <span class="audit-user">{{ row.user || 'System' }}</span>
            </template>
            <template #cell-details="{ row }">
              <span class="audit-details">{{ row.details }}</span>
            </template>
            <template #empty>
              <div class="empty-audit">No audit log entries</div>
            </template>
          </AppTable>
        </div>
      </div>

      <!-- Metadata -->
      <div class="card payment-metadata">
        <div class="card-header">
          <h2>Metadata</h2>
        </div>
        <div class="card-body">
          <div class="metadata-grid">
            <div class="metadata-item">
              <label>Created</label>
              <span>{{ formatDateTime(payment.createdAt) }}</span>
            </div>
            <div class="metadata-item">
              <label>Last Updated</label>
              <span>{{ formatDateTime(payment.updatedAt) }}</span>
            </div>
            <div class="metadata-item">
              <label>Version</label>
              <span>{{ payment.version }}</span>
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
import { usePaymentStore } from '~/stores/payment'
import { canCancelPayment, canRetryPayment } from '~/schemas/payment'

// Page meta
definePageMeta({
  title: 'Payment Details'
})

// Route params
const route = useRoute()
const paymentId = route.params.id as string

// Store
const paymentStore = usePaymentStore()
const { showToast } = useToast()

// Reactive state
const loading = ref(true)
const error = ref<string | null>(null)

// Computed
const payment = computed(() => paymentStore.currentPayment)
const canEdit = computed(() => payment.value && (payment.value.paymentStatus === 'PENDING' || payment.value.paymentStatus === 'PROCESSING'))
const canRetry = computed(() => payment.value && canRetryPayment(payment.value))
const canCancel = computed(() => payment.value && canCancelPayment(payment.value))

// Table columns
const auditColumns = [
  { key: 'timestamp', label: 'Timestamp', style: 'width: 20%' },
  { key: 'action', label: 'Action', style: 'width: 20%' },
  { key: 'user', label: 'User', style: 'width: 20%' },
  { key: 'details', label: 'Details', style: 'width: 40%' }
]

// Methods
const fetchPayment = async () => {
  try {
    loading.value = true
    error.value = null
    await paymentStore.fetchPaymentById(paymentId)
  } catch (err: any) {
    error.value = err.message || 'Failed to load payment'
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  navigateTo(`/payments/${paymentId}/edit`)
}

const handleRetry = async () => {
  if (!payment.value) return

  const confirmed = confirm(`Are you sure you want to retry payment ${payment.value.paymentNumber}?`)

  if (confirmed) {
    try {
      await paymentStore.changePaymentStatus({
        id: paymentId,
        status: 'PENDING'
      })

      showToast({
        severity: 'success',
        summary: 'Payment Retried',
        detail: `Payment ${payment.value.paymentNumber} has been queued for retry.`,
        life: 3000
      })

      await fetchPayment()

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to retry payment',
        life: 5000
      })
    }
  }
}

const handleCancel = async () => {
  if (!payment.value) return

  const confirmed = confirm(`Are you sure you want to cancel payment ${payment.value.paymentNumber}?`)

  if (confirmed) {
    try {
      await paymentStore.changePaymentStatus({
        id: paymentId,
        status: 'FAILED'
      })

      showToast({
        severity: 'success',
        summary: 'Payment Cancelled',
        detail: `Payment ${payment.value.paymentNumber} has been cancelled.`,
        life: 3000
      })

      await fetchPayment()

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to cancel payment',
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

// Lifecycle
onMounted(async () => {
  await fetchPayment()
})
</script>

<style scoped>
.payment-detail-page {
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

/* Payment Summary */
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

.amount {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-size: var(--font-size-lg);
}

.mono {
  font-family: monospace;
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

.invoice-link {
  color: var(--color-primary);
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
}

.invoice-link:hover {
  text-decoration: underline;
}

/* Payment Details */
.details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.detail-item label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.detail-item > span {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.raw-response {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.raw-response label {
  display: block;
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.raw-response pre {
  background: var(--color-surface-secondary);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--font-size-xs);
  color: var(--color-text-primary);
  overflow-x: auto;
}

/* Billing Address */
.address-block {
  color: var(--color-text-primary);
  line-height: 1.6;
}

.address-block p {
  margin: 0 0 var(--space-1) 0;
}

/* Notes */
.payment-notes p {
  margin: 0;
  color: var(--color-text-primary);
  white-space: pre-wrap;
}

/* Audit Log */
.audit-action {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.audit-user {
  color: var(--color-text-primary);
}

.audit-details {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.empty-audit {
  text-align: center;
  padding: var(--space-4);
  color: var(--color-text-muted);
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

  .summary-grid,
  .details-grid,
  .metadata-grid {
    grid-template-columns: 1fr;
  }
}
</style>
