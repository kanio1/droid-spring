<template>
  <div class="usage-record-detail-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <NuxtLink to="/billing/usage-records" class="back-link">
          ← Back to Usage Records
        </NuxtLink>
        <div class="record-header">
          <div class="record-icon">
            <i class="pi pi-phone"></i>
          </div>
          <div class="record-info">
            <h1 class="record-id">Usage Record #{{ usageRecord?.id?.substring(0, 8) }}</h1>
            <p class="record-customer">Customer: {{ usageRecord?.customerId?.substring(0, 8) }}...</p>
          </div>
        </div>
      </div>
      <div class="page-header__actions">
        <Button
          icon="pi pi-pencil"
          variant="secondary"
          @click="handleEdit"
          v-if="canEditRecord"
        >
          Edit
        </Button>
        <Button
          icon="pi pi-star"
          variant="warning"
          @click="handleRate"
          v-if="!usageRecord?.isRated"
        >
          Mark as Rated
        </Button>
        <Button
          icon="pi pi-trash"
          variant="danger"
          @click="handleDelete"
          v-if="canDeleteRecord"
        >
          Delete
        </Button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading usage record details...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">⚠️</div>
      <h3>Error Loading Usage Record</h3>
      <p>{{ error }}</p>
      <Button variant="primary" @click="fetchUsageRecord">
        Try Again
      </Button>
    </div>

    <!-- Usage Record Details -->
    <div v-else-if="usageRecord" class="record-content">
      <!-- Status and Rating -->
      <div class="status-section">
        <div class="status-badges">
          <StatusBadge :status="usageRecord.usageType" type="usage-type" size="lg" />
          <StatusBadge :status="usageRecord.isRated ? 'RATED' : 'PENDING'" type="rating" size="lg" />
        </div>
        <div class="record-dates">
          <div class="date-item">
            <span class="date-label">Created:</span>
            <span class="date-value">{{ formatDateTime(usageRecord.createdAt) }}</span>
          </div>
          <div class="date-item">
            <span class="date-label">Usage Date:</span>
            <span class="date-value">{{ formatDateTime(usageRecord.timestamp) }}</span>
          </div>
          <div class="date-item" v-if="usageRecord.updatedAt">
            <span class="date-label">Last Updated:</span>
            <span class="date-value">{{ formatDateTime(usageRecord.updatedAt) }}</span>
          </div>
        </div>
      </div>

      <!-- Usage Information Card -->
      <div class="info-card">
        <div class="info-card__header">
          <h2>Usage Information</h2>
        </div>
        <div class="info-card__content">
          <div class="info-grid">
            <div class="info-item">
              <label class="info-label">Record ID</label>
              <div class="info-value">
                <code>{{ usageRecord.id }}</code>
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Usage Type</label>
              <div class="info-value">
                <StatusBadge :status="usageRecord.usageType" type="usage-type" size="small" />
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Usage Amount</label>
              <div class="info-value usage-amount">
                {{ formatUsageAmount(usageRecord.usageAmount, usageRecord.unit) }}
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Customer ID</label>
              <div class="info-value">
                <code>{{ usageRecord.customerId }}</code>
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Subscription ID</label>
              <div class="info-value">
                <code v-if="usageRecord.subscriptionId">{{ usageRecord.subscriptionId }}</code>
                <span v-else class="text-muted">N/A</span>
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Rating Status</label>
              <div class="info-value">
                <StatusBadge :status="usageRecord.isRated ? 'RATED' : 'PENDING'" type="rating" size="small" />
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Source</label>
              <div class="info-value">{{ usageRecord.source }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">Destination</label>
              <div class="info-value">
                <span v-if="usageRecord.destination">{{ usageRecord.destination }}</span>
                <span v-else class="text-muted">N/A</span>
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Currency</label>
              <div class="info-value">{{ usageRecord.currency }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">Cost</label>
              <div class="info-value cost">
                <span v-if="usageRecord.cost !== null && usageRecord.cost !== undefined">
                  {{ formatCurrency(usageRecord.cost, usageRecord.currency) }}
                </span>
                <span v-else class="text-muted">Not rated</span>
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Rated Amount</label>
              <div class="info-value">
                <span v-if="usageRecord.ratedAmount !== null && usageRecord.ratedAmount !== undefined">
                  {{ formatCurrency(usageRecord.ratedAmount, usageRecord.currency) }}
                </span>
                <span v-else class="text-muted">N/A</span>
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Timestamp</label>
              <div class="info-value">{{ formatDateTime(usageRecord.timestamp) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Call/Service Details (for Voice calls) -->
      <div v-if="usageRecord.usageType === 'VOICE' && (usageRecord.source || usageRecord.destination)" class="details-card">
        <div class="details-card__header">
          <h2>Call Details</h2>
        </div>
        <div class="details-card__content">
          <div class="call-info">
            <div class="call-participant">
              <div class="participant-label">
                <i class="pi pi-phone"></i>
                <span>From</span>
              </div>
              <div class="participant-number">{{ usageRecord.source || 'N/A' }}</div>
            </div>
            <div class="call-arrow">
              <i class="pi pi-arrow-right"></i>
            </div>
            <div class="call-participant">
              <div class="participant-label">
                <i class="pi pi-phone"></i>
                <span>To</span>
              </div>
              <div class="participant-number">{{ usageRecord.destination || 'N/A' }}</div>
            </div>
          </div>
          <div class="call-metadata" v-if="usageRecord.metadata">
            <h3>Additional Information</h3>
            <div class="metadata-grid">
              <div v-for="(value, key) in usageRecord.metadata" :key="key" class="metadata-item">
                <label class="metadata-key">{{ formatMetadataKey(key) }}</label>
                <div class="metadata-value">{{ value }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Usage Analytics Card -->
      <div class="analytics-card">
        <div class="analytics-card__header">
          <h2>Usage Analytics</h2>
        </div>
        <div class="analytics-card__content">
          <div class="analytics-grid">
            <div class="analytics-item">
              <div class="analytics-label">Rating Progress</div>
              <div class="analytics-value">
                <div class="progress-bar">
                  <div
                    class="progress-fill"
                    :style="{ width: usageRecord.isRated ? '100%' : '0%' }"
                  ></div>
                </div>
                <span class="progress-text">
                  {{ usageRecord.isRated ? 'Rated' : 'Unrated' }}
                </span>
              </div>
            </div>
            <div class="analytics-item">
              <div class="analytics-label">Age</div>
              <div class="analytics-value">{{ getUsageAge(usageRecord.timestamp) }}</div>
            </div>
            <div class="analytics-item">
              <div class="analytics-label">Rate per Unit</div>
              <div class="analytics-value">
                <span v-if="usageRecord.cost && usageRecord.usageAmount">
                  {{ formatCurrency(usageRecord.cost / usageRecord.usageAmount, usageRecord.currency) }}/{{ usageRecord.unit }}
                </span>
                <span v-else class="text-muted">N/A</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Audit Trail -->
      <div class="audit-card">
        <div class="audit-card__header">
          <h2>Audit Trail</h2>
        </div>
        <div class="audit-card__content">
          <div class="audit-timeline">
            <div class="audit-item">
              <div class="audit-icon audit-icon--created">
                <i class="pi pi-plus"></i>
              </div>
              <div class="audit-content">
                <div class="audit-title">Record Created</div>
                <div class="audit-time">{{ formatDateTime(usageRecord.createdAt) }}</div>
              </div>
            </div>
            <div class="audit-item" v-if="usageRecord.updatedAt !== usageRecord.createdAt">
              <div class="audit-icon audit-icon--updated">
                <i class="pi pi-pencil"></i>
              </div>
              <div class="audit-content">
                <div class="audit-title">Last Updated</div>
                <div class="audit-time">{{ formatDateTime(usageRecord.updatedAt) }}</div>
              </div>
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
import { useBillingStore } from '~/stores/billing'
import {
  formatUsageAmount,
  type UsageRecord
} from '~/schemas/billing'

// Page meta
definePageMeta({
  title: 'Usage Record Details'
})

// Route
const route = useRoute()
const id = route.params.id as string

// Store
const billingStore = useBillingStore()
const { showToast } = useToast()

// Reactive state
const loading = ref(true)
const error = ref<string | null>(null)
const usageRecord = ref<UsageRecord | null>(null)

// Computed
const canEditRecord = computed(() => {
  return usageRecord.value && !usageRecord.value.isRated
})

const canDeleteRecord = computed(() => {
  return usageRecord.value !== null
})

// Methods
const fetchUsageRecord = async () => {
  loading.value = true
  error.value = null

  try {
    // TODO: Implement fetchUsageRecordById in store
    // For now, we'll fetch all and find the record
    await billingStore.fetchUsageRecords({ size: 1 })
    showToast({
      severity: 'info',
      summary: 'Info',
      detail: 'Detailed view will be implemented with backend API',
      life: 3000
    })
  } catch (err: any) {
    error.value = err.message || 'Failed to fetch usage record'
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  navigateTo(`/billing/usage-records/${id}/edit`)
}

const handleRate = async () => {
  if (!usageRecord.value) return

  const confirmed = confirm('Mark this usage record as rated?')

  if (confirmed) {
    try {
      // TODO: Implement rating logic in store
      showToast({
        severity: 'info',
        summary: 'Rate Record',
        detail: 'Rating functionality will be implemented',
        life: 3000
      })

      // Refresh the record
      await fetchUsageRecord()

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to rate record',
        life: 5000
      })
    }
  }
}

const handleDelete = async () => {
  if (!usageRecord.value) return

  const confirmed = confirm('Are you sure you want to delete this usage record?')

  if (confirmed) {
    try {
      // TODO: Implement delete logic in store
      showToast({
        severity: 'info',
        summary: 'Delete Record',
        detail: 'Delete functionality will be implemented',
        life: 3000
      })

      // Navigate back to list
      navigateTo('/billing/usage-records')

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to delete record',
        life: 5000
      })
    }
  }
}

// Utility functions
const formatDateTime = (dateString: string): string => {
  return new Date(dateString).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const formatCurrency = (amount: number, currency: string = 'USD'): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2
  }).format(amount)
}

const formatMetadataKey = (key: string): string => {
  return key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())
}

const getUsageAge = (timestamp: string): string => {
  const now = new Date().getTime()
  const usageTime = new Date(timestamp).getTime()
  const diffMs = now - usageTime
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffHours / 24)

  if (diffDays > 0) {
    return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`
  } else if (diffHours > 0) {
    return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`
  } else {
    return 'Less than 1 hour ago'
  }
}

// Lifecycle
onMounted(async () => {
  await fetchUsageRecord()
})
</script>

<style scoped>
.usage-record-detail-page {
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
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.page-header__content {
  flex: 1;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-3);
  transition: color 0.2s;
}

.back-link:hover {
  color: var(--color-primary);
}

.record-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.record-icon {
  width: 64px;
  height: 64px;
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  flex-shrink: 0;
}

.record-info {
  flex: 1;
}

.record-id {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  font-family: monospace;
}

.record-customer {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.page-header__actions {
  flex-shrink: 0;
  display: flex;
  gap: var(--space-3);
}

/* Loading & Error States */
.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--space-4);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-icon {
  font-size: 3rem;
  margin-bottom: var(--space-4);
}

/* Status Section */
.status-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.status-badges {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.record-dates {
  display: flex;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.date-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.date-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  font-weight: var(--font-weight-medium);
}

.date-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

/* Info Cards */
.info-card,
.details-card,
.analytics-card,
.audit-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.info-card__header,
.details-card__header,
.analytics-card__header,
.audit-card__header {
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-elevated);
}

.info-card__header h2,
.details-card__header h2,
.analytics-card__header h2,
.audit-card__header h2 {
  margin: 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.info-card__content,
.details-card__content,
.analytics-card__content,
.audit-card__content {
  padding: var(--space-4);
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.info-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  font-weight: var(--font-weight-medium);
  letter-spacing: 0.5px;
}

.info-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

.info-value code {
  padding: var(--space-1) var(--space-2);
  background: var(--color-surface-elevated);
  border-radius: var(--radius-sm);
  font-family: monospace;
  font-size: var(--font-size-xs);
}

.usage-amount {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
}

.cost {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-success);
}

.text-muted {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Call Details */
.call-info {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--color-surface-elevated);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-4);
}

.call-participant {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.participant-label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  font-weight: var(--font-weight-medium);
}

.participant-number {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-family: monospace;
}

.call-arrow {
  color: var(--color-text-secondary);
  font-size: 1.5rem;
}

.call-metadata h3 {
  margin: 0 0 var(--space-3) 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.metadata-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-3);
}

.metadata-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.metadata-key {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  font-weight: var(--font-weight-medium);
}

.metadata-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-family: monospace;
}

/* Analytics */
.analytics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.analytics-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.analytics-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  font-weight: var(--font-weight-medium);
}

.analytics-value {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-semibold);
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--color-surface-elevated);
  border-radius: var(--radius-full);
  overflow: hidden;
  margin-bottom: var(--space-2);
}

.progress-fill {
  height: 100%;
  background: var(--color-success);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* Audit Trail */
.audit-timeline {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.audit-item {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
}

.audit-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.audit-icon--created {
  background: var(--color-success);
  color: white;
}

.audit-icon--updated {
  background: var(--color-info);
  color: white;
}

.audit-content {
  flex: 1;
}

.audit-title {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.audit-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .usage-record-detail-page {
    padding: var(--space-4);
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }

  .page-header__actions {
    flex-direction: column;
    width: 100%;
  }

  .page-header__actions .p-button {
    width: 100%;
  }

  .record-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }

  .record-icon {
    width: 48px;
    height: 48px;
    font-size: 1.5rem;
  }

  .record-id {
    font-size: var(--font-size-2xl);
  }

  .status-section {
    flex-direction: column;
    align-items: stretch;
  }

  .info-grid,
  .analytics-grid {
    grid-template-columns: 1fr;
  }

  .call-info {
    flex-direction: column;
  }

  .call-arrow {
    transform: rotate(90deg);
  }

  .metadata-grid {
    grid-template-columns: 1fr;
  }
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
