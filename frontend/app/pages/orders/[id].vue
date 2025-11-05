<template>
  <div class="order-detail-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <NuxtLink to="/orders" class="back-link">
          ← Back to Orders
        </NuxtLink>
        <div class="order-header">
          <div class="order-icon">📦</div>
          <div class="order-info">
            <h1 class="order-number">Order #{{ order?.orderNumber }}</h1>
            <p class="order-customer">{{ order?.customerName || 'N/A' }}</p>
          </div>
        </div>
      </div>
      <div class="page-header__actions">
        <Button
          icon="pi pi-pencil"
          variant="secondary"
          @click="handleEdit"
          v-if="canEditOrder"
        >
          Edit
        </Button>
        <Button
          icon="pi pi-times"
          variant="danger"
          @click="handleCancel"
          v-if="canCancelOrder"
        >
          Cancel Order
        </Button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading order details...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">⚠️</div>
      <h3>Error Loading Order</h3>
      <p>{{ error }}</p>
      <Button variant="primary" @click="fetchOrder">
        Try Again
      </Button>
    </div>

    <!-- Order Details -->
    <div v-else-if="order" class="order-content">
      <!-- Status and Progress -->
      <div class="status-section">
        <div class="status-badges">
          <StatusBadge :status="order.status" type="order" size="lg" />
          <StatusBadge :status="order.orderType" type="order-type" size="lg" />
          <StatusBadge :status="order.priority" type="priority" size="lg" />
        </div>
        <div class="order-dates">
          <div class="date-item">
            <span class="date-label">Created:</span>
            <span class="date-value">{{ formatDateTime(order.createdAt) }}</span>
          </div>
          <div class="date-item" v-if="order.requestedDate">
            <span class="date-label">Requested:</span>
            <span class="date-value">{{ formatDate(order.requestedDate) }}</span>
          </div>
          <div class="date-item" v-if="order.promisedDate">
            <span class="date-label">Promised:</span>
            <span class="date-value">{{ formatDate(order.promisedDate) }}</span>
          </div>
          <div class="date-item" v-if="order.completedDate">
            <span class="date-label">Completed:</span>
            <span class="date-value">{{ formatDate(order.completedDate) }}</span>
          </div>
        </div>
      </div>

      <!-- Order Information Card -->
      <div class="info-card">
        <div class="info-card__header">
          <h2>Order Information</h2>
        </div>
        <div class="info-card__content">
          <div class="info-grid">
            <div class="info-item">
              <label class="info-label">Order Number</label>
              <div class="info-value">{{ order.orderNumber }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">Order Type</label>
              <div class="info-value">
                <StatusBadge :status="order.orderType" type="order-type" size="small" />
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Status</label>
              <div class="info-value">
                <StatusBadge :status="order.status" type="order" size="small" />
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Priority</label>
              <div class="info-value">
                <StatusBadge :status="order.priority" type="priority" size="small" />
              </div>
            </div>
            <div class="info-item" v-if="order.totalAmount !== null && order.totalAmount !== undefined">
              <label class="info-label">Total Amount</label>
              <div class="info-value amount">{{ formatCurrency(order.totalAmount, order.currency) }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">Currency</label>
              <div class="info-value">{{ order.currency }}</div>
            </div>
            <div class="info-item" v-if="order.orderChannel">
              <label class="info-label">Channel</label>
              <div class="info-value">{{ order.orderChannel }}</div>
            </div>
            <div class="info-item" v-if="order.salesRepName">
              <label class="info-label">Sales Representative</label>
              <div class="info-value">{{ order.salesRepName }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Customer Information Card -->
      <div class="info-card">
        <div class="info-card__header">
          <h2>Customer Information</h2>
          <NuxtLink :to="`/customers/${order.customerId}`">
            <Button label="View Customer" icon="pi pi-external-link" text size="small" />
          </NuxtLink>
        </div>
        <div class="info-card__content">
          <div class="info-grid">
            <div class="info-item">
              <label class="info-label">Customer ID</label>
              <div class="info-value">
                <code>{{ order.customerId }}</code>
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Customer Name</label>
              <div class="info-value">{{ order.customerName || 'N/A' }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Order Items Card -->
      <div class="items-card" v-if="order.itemCount > 0">
        <div class="info-card__header">
          <h2>Order Items</h2>
          <span class="item-count">{{ order.itemCount }} item(s)</span>
        </div>
        <div class="info-card__content">
          <div class="items-placeholder">
            <i class="pi pi-box items-icon"></i>
            <p>Order items will be displayed here</p>
            <small>Total: {{ order.itemCount }} item(s)</small>
          </div>
        </div>
      </div>

      <!-- Notes Card -->
      <div class="info-card" v-if="order.notes">
        <div class="info-card__header">
          <h2>Notes</h2>
        </div>
        <div class="info-card__content">
          <div class="notes-content">
            {{ order.notes }}
          </div>
        </div>
      </div>

      <!-- Audit Information -->
      <div class="audit-card">
        <div class="info-card__header">
          <h3>Audit Information</h3>
        </div>
        <div class="info-card__content">
          <div class="info-grid">
            <div class="info-item">
              <label class="info-label">Created By</label>
              <div class="info-value">{{ order.createdBy || 'System' }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">Created At</label>
              <div class="info-value">{{ formatDateTime(order.createdAt) }}</div>
            </div>
            <div class="info-item" v-if="order.updatedBy">
              <label class="info-label">Updated By</label>
              <div class="info-value">{{ order.updatedBy }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">Last Updated</label>
              <div class="info-value">{{ formatDateTime(order.updatedAt) }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">Version</label>
              <div class="info-value">{{ order.version }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useOrderStore } from '~/stores/order'
import { Order, ORDER_STATUS_LABELS } from '~/schemas/order'

// Page meta
definePageMeta({
  title: 'Order Details'
})

// Route and store
const route = useRoute()
const orderStore = useOrderStore()
const { showToast } = useToast()

// Reactive state
const loading = ref(true)
const error = ref<string | null>(null)
const order = ref<Order | null>(null)

// Computed
const canEditOrder = computed(() => {
  return order.value ? ['PENDING', 'CONFIRMED', 'PROCESSING'].includes(order.value.status) : false
})

const canCancelOrder = computed(() => {
  return order.value ? order.value.canBeCancelled && canEditOrder.value : false
})

// Methods
const fetchOrder = async () => {
  const orderId = route.params.id as string

  loading.value = true
  error.value = null

  try {
    const orderData = await orderStore.fetchOrderById(orderId)
    order.value = orderData
  } catch (err: any) {
    error.value = err.message || 'Failed to load order details'
    console.error('Error fetching order:', err)
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  if (order.value) {
    navigateTo(`/orders/${order.value.id}/edit`)
  }
}

const handleCancel = async () => {
  if (!order.value) return

  const confirmed = confirm(
    `Are you sure you want to cancel order ${order.value.orderNumber}? This action cannot be undone.`
  )

  if (confirmed) {
    try {
      await orderStore.updateOrderStatus({
        id: order.value.id,
        status: 'CANCELLED'
      })

      showToast({
        severity: 'success',
        summary: 'Order Cancelled',
        detail: `Order ${order.value.orderNumber} has been successfully cancelled.`,
        life: 3000
      })

      // Refresh the order
      await fetchOrder()

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to cancel order',
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
    month: 'long',
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

// Lifecycle
onMounted(async () => {
  await fetchOrder()
})

// Watch for route changes
watch(() => route.fullPath, async () => {
  if (route.params.id) {
    await fetchOrder()
  }
})
</script>

<style scoped>
.order-detail-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
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
  transition: color var(--transition-fast) var(--transition-timing);
}

.back-link:hover {
  color: var(--color-primary);
}

.order-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.order-icon {
  font-size: 3rem;
  line-height: 1;
}

.order-info {
  flex: 1;
}

.order-number {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  font-family: monospace;
}

.order-customer {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.page-header__actions {
  flex-shrink: 0;
  display: flex;
  gap: var(--space-2);
}

/* Loading State */
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

.error-state h3 {
  margin: 0 0 var(--space-2) 0;
  color: var(--color-text-primary);
}

/* Status Section */
.status-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.status-badges {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.order-dates {
  display: flex;
  gap: var(--space-6);
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
  letter-spacing: 0.05em;
}

.date-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

/* Info Cards */
.info-card,
.items-card,
.audit-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.info-card__header,
.audit-card .info-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-alt);
}

.info-card__header h2,
.info-card__header h3 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.item-count {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  background: var(--color-surface);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);
}

.info-card__content {
  padding: var(--space-6);
}

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
  letter-spacing: 0.05em;
  font-weight: var(--font-weight-medium);
}

.info-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.info-value.amount {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
}

.info-value code {
  font-family: monospace;
  font-size: var(--font-size-xs);
  background: var(--color-surface-alt);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
}

/* Items Placeholder */
.items-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-8);
  text-align: center;
  color: var(--color-text-muted);
}

.items-icon {
  font-size: 3rem;
  margin-bottom: var(--space-3);
  opacity: 0.5;
}

.items-placeholder p {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-base);
}

.items-placeholder small {
  font-size: var(--font-size-sm);
}

/* Notes */
.notes-content {
  white-space: pre-wrap;
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  background: var(--color-surface-alt);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  line-height: 1.6;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }

  .page-header__actions {
    align-self: flex-start;
    width: 100%;
  }

  .order-header {
    gap: var(--space-3);
  }

  .order-icon {
    font-size: 2rem;
  }

  .order-number {
    font-size: var(--font-size-xl);
  }

  .status-badges {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-dates {
    flex-direction: column;
    gap: var(--space-3);
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
