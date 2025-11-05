<template>
  <div class="subscription-detail-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/subscriptions" class="back-link">
        ← Back to Subscriptions
      </NuxtLink>
      <div class="page-header__content">
        <div class="title-row">
          <h1 class="page-title">Subscription {{ subscription?.subscriptionNumber || '...' }}</h1>
          <StatusBadge v-if="subscription" :status="subscription.status" type="subscription" size="large" />
        </div>
        <p class="page-subtitle" v-if="subscription">
          {{ subscription.productName }} for {{ subscription.customerName }}
        </p>
      </div>
      <div class="page-header__actions">
        <Button
          v-if="canRenew"
          label="Renew"
          icon="pi pi-refresh"
          severity="info"
          @click="handleRenew"
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
      <p>Loading subscription details...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <i class="pi pi-exclamation-triangle"></i>
      <p>{{ error }}</p>
      <Button label="Retry" @click="fetchSubscription" />
    </div>

    <div v-else-if="subscription" class="subscription-content">
      <!-- Subscription Summary Card -->
      <div class="card subscription-summary">
        <div class="card-header">
          <h2>Subscription Summary</h2>
        </div>
        <div class="card-body">
          <div class="summary-grid">
            <div class="summary-item">
              <label>Subscription Number</label>
              <span>{{ subscription.subscriptionNumber }}</span>
            </div>
            <div class="summary-item">
              <label>Status</label>
              <StatusBadge :status="subscription.status" type="subscription" />
            </div>
            <div class="summary-item">
              <label>Customer</label>
              <div class="customer-info">
                <div class="customer-avatar">
                  {{ getCustomerInitials(subscription) }}
                </div>
                <div>
                  <div class="customer-name">{{ subscription.customerName }}</div>
                  <div class="customer-id">{{ subscription.customerId }}</div>
                </div>
              </div>
            </div>
            <div class="summary-item">
              <label>Product</label>
              <div class="product-info">
                <div class="product-name">{{ subscription.productName }}</div>
                <div class="product-id">{{ subscription.productId }}</div>
              </div>
            </div>
            <div class="summary-item">
              <label>Billing Period</label>
              <span>{{ formatBillingPeriod(subscription.billingPeriod) }}</span>
            </div>
            <div class="summary-item">
              <label>Amount</label>
              <span class="amount">{{ formatCurrency(subscription.amount, subscription.currency) }}</span>
            </div>
            <div class="summary-item">
              <label>Start Date</label>
              <span>{{ formatDate(subscription.startDate) }}</span>
            </div>
            <div class="summary-item">
              <label>End Date</label>
              <span v-if="subscription.endDate">{{ formatDate(subscription.endDate) }}</span>
              <span v-else class="text-muted">Ongoing</span>
            </div>
            <div class="summary-item">
              <label>Next Billing Date</label>
              <span v-if="subscription.nextBillingDate" :class="{ 'text-warning': isExpiringSoon(subscription) }">
                {{ formatDate(subscription.nextBillingDate) }}
                <i v-if="isExpiringSoon(subscription)" class="pi pi-exclamation-triangle ml-1"></i>
              </span>
              <span v-else class="text-muted">—</span>
            </div>
            <div class="summary-item">
              <label>Auto Renew</label>
              <div class="auto-renew">
                <i :class="subscription.autoRenew ? 'pi pi-check-circle text-success' : 'pi pi-times-circle text-muted'"></i>
                <span class="ml-2">{{ subscription.autoRenew ? 'Enabled' : 'Disabled' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Product Features -->
      <div v-if="subscription.features && subscription.features.length > 0" class="card product-features">
        <div class="card-header">
          <h2>Product Features</h2>
        </div>
        <div class="card-body">
          <div class="features-list">
            <div v-for="(feature, index) in subscription.features" :key="index" class="feature-item">
              <i class="pi pi-check-circle text-success"></i>
              <span>{{ feature }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Usage Statistics -->
      <div class="card usage-statistics">
        <div class="card-header">
          <h2>Usage Statistics</h2>
        </div>
        <div class="card-body">
          <div class="stats-grid">
            <div class="stat-item">
              <label>Data Used</label>
              <span>{{ formatDataUsage(subscription.usage?.dataUsed || 0) }}</span>
            </div>
            <div class="stat-item">
              <label>Data Limit</label>
              <span>{{ formatDataUsage(subscription.usage?.dataLimit || 0) }}</span>
            </div>
            <div class="stat-item">
              <label>Call Minutes</label>
              <span>{{ subscription.usage?.callMinutes || 0 }} min</span>
            </div>
            <div class="stat-item">
              <label>SMS Count</label>
              <span>{{ subscription.usage?.smsCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Billing History -->
      <div class="card billing-history">
        <div class="card-header">
          <h2>Billing History</h2>
        </div>
        <div class="card-body">
          <AppTable :columns="billingColumns" :data="subscription.billingHistory || []" :show-pagination="false">
            <template #cell-invoiceNumber="{ row }">
              <NuxtLink :to="`/invoices/${row.invoiceId}`" class="invoice-link">
                {{ row.invoiceNumber || row.invoiceId }}
              </NuxtLink>
            </template>
            <template #cell-billingDate="{ row }">
              {{ formatDate(row.billingDate) }}
            </template>
            <template #cell-amount="{ row }">
              <span class="amount">{{ formatCurrency(row.amount, subscription.currency) }}</span>
            </template>
            <template #cell-status="{ row }">
              <StatusBadge :status="row.status" type="invoice" size="small" />
            </template>
            <template #empty>
              <div class="empty-history">No billing history available</div>
            </template>
          </AppTable>
        </div>
      </div>

      <!-- Notes -->
      <div v-if="subscription.notes" class="card subscription-notes">
        <div class="card-header">
          <h2>Notes</h2>
        </div>
        <div class="card-body">
          <p>{{ subscription.notes }}</p>
        </div>
      </div>

      <!-- Metadata -->
      <div class="card subscription-metadata">
        <div class="card-header">
          <h2>Metadata</h2>
        </div>
        <div class="card-body">
          <div class="metadata-grid">
            <div class="metadata-item">
              <label>Created</label>
              <span>{{ formatDateTime(subscription.createdAt) }}</span>
            </div>
            <div class="metadata-item">
              <label>Last Updated</label>
              <span>{{ formatDateTime(subscription.updatedAt) }}</span>
            </div>
            <div class="metadata-item">
              <label>Version</label>
              <span>{{ subscription.version }}</span>
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
import { useSubscriptionStore } from '~/stores/subscription'
import { canCancelSubscription, canRenewSubscription } from '~/schemas/subscription'

// Page meta
definePageMeta({
  title: 'Subscription Details'
})

// Route params
const route = useRoute()
const subscriptionId = route.params.id as string

// Store
const subscriptionStore = useSubscriptionStore()
const { showToast } = useToast()

// Reactive state
const loading = ref(true)
const error = ref<string | null>(null)

// Computed
const subscription = computed(() => subscriptionStore.currentSubscription)
const canEdit = computed(() => subscription.value && (subscription.value.status === 'ACTIVE' || subscription.value.status === 'SUSPENDED'))
const canRenew = computed(() => subscription.value && canRenewSubscription(subscription.value))
const canCancel = computed(() => subscription.value && canCancelSubscription(subscription.value))

// Table columns
const billingColumns = [
  { key: 'invoiceNumber', label: 'Invoice', style: 'width: 25%' },
  { key: 'billingDate', label: 'Date', style: 'width: 20%' },
  { key: 'amount', label: 'Amount', style: 'width: 20%', align: 'right' },
  { key: 'status', label: 'Status', style: 'width: 20%' },
  { key: 'period', label: 'Period', style: 'width: 15%' }
]

// Methods
const fetchSubscription = async () => {
  try {
    loading.value = true
    error.value = null
    await subscriptionStore.fetchSubscriptionById(subscriptionId)
  } catch (err: any) {
    error.value = err.message || 'Failed to load subscription'
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  navigateTo(`/subscriptions/${subscriptionId}/edit`)
}

const handleRenew = async () => {
  if (!subscription.value) return

  const confirmed = confirm(`Are you sure you want to renew subscription ${subscription.value.subscriptionNumber}?`)

  if (confirmed) {
    try {
      await subscriptionStore.renewSubscription({
        id: subscriptionId
      })

      showToast({
        severity: 'success',
        summary: 'Subscription Renewed',
        detail: `Subscription ${subscription.value.subscriptionNumber} has been renewed.`,
        life: 3000
      })

      await fetchSubscription()

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to renew subscription',
        life: 5000
      })
    }
  }
}

const handleCancel = async () => {
  if (!subscription.value) return

  const confirmed = confirm(`Are you sure you want to cancel subscription ${subscription.value.subscriptionNumber}?`)

  if (confirmed) {
    try {
      await subscriptionStore.changeSubscriptionStatus({
        id: subscriptionId,
        status: 'CANCELLED'
      })

      showToast({
        severity: 'success',
        summary: 'Subscription Cancelled',
        detail: `Subscription ${subscription.value.subscriptionNumber} has been cancelled.`,
        life: 3000
      })

      await fetchSubscription()

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to cancel subscription',
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

const formatBillingPeriod = (period: string): string => {
  const periods: Record<string, string> = {
    MONTHLY: 'Monthly',
    QUARTERLY: 'Quarterly',
    YEARLY: 'Yearly'
  }
  return periods[period] || period
}

const getCustomerInitials = (row: any): string => {
  if (!row.customerName) return 'N/A'
  const names = row.customerName.split(' ')
  if (names.length >= 2) {
    return `${names[0][0]}${names[1][0]}`.toUpperCase()
  }
  return row.customerName.substring(0, 2).toUpperCase()
}

const formatDataUsage = (bytes: number): string => {
  if (bytes === 0) return '0 GB'
  const gb = bytes / (1024 * 1024 * 1024)
  return `${gb.toFixed(2)} GB`
}

const isExpiringSoon = (row: any): boolean => {
  if (!row.nextBillingDate) return false
  const today = new Date()
  const nextBilling = new Date(row.nextBillingDate)
  const diffTime = nextBilling.getTime() - today.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays <= 30 && diffDays >= 0
}

// Lifecycle
onMounted(async () => {
  await fetchSubscription()
})
</script>

<style scoped>
.subscription-detail-page {
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

/* Subscription Summary */
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

.customer-info,
.product-info {
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

.customer-name,
.product-name {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.customer-id,
.product-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.auto-renew {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

/* Product Features */
.features-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-primary);
}

/* Usage Statistics */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.stat-item label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-item > span {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

/* Billing History */
.invoice-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
}

.invoice-link:hover {
  text-decoration: underline;
}

.empty-history {
  text-align: center;
  padding: var(--space-4);
  color: var(--color-text-muted);
}

/* Notes */
.subscription-notes p {
  margin: 0;
  color: var(--color-text-primary);
  white-space: pre-wrap;
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
  .stats-grid,
  .metadata-grid {
    grid-template-columns: 1fr;
  }
}
</style>
