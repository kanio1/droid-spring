<template>
  <div class="customer-detail-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <NuxtLink to="/customers" class="back-link">
          ← Back to Customers
        </NuxtLink>
        <div class="customer-header">
          <div class="customer-avatar">
            {{ getInitials(customer) }}
          </div>
          <div class="customer-info">
            <h1 class="customer-name">{{ formatCustomerName(customer) }}</h1>
            <p class="customer-email">{{ customer.email }}</p>
          </div>
        </div>
      </div>
      <div class="page-header__actions">
        <AppButton 
          variant="secondary"
          icon="✏️"
          @click="handleEdit"
        >
          Edit
        </AppButton>
        <AppButton 
          variant="primary"
          icon="📝"
          @click="handleChangeStatus"
        >
          Change Status
        </AppButton>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading customer details...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">⚠️</div>
      <h3>Error Loading Customer</h3>
      <p>{{ error }}</p>
      <AppButton variant="primary" @click="fetchCustomer">
        Try Again
      </AppButton>
    </div>

    <!-- Customer Details -->
    <div v-else class="customer-content">
      <!-- Status Badge -->
      <div class="status-section">
        <AppBadge
          :variant="getCustomerStatusVariant(customer.status)"
          :text="CUSTOMER_STATUS_LABELS[customer.status]"
          size="lg"
        />
        <div class="customer-dates">
          <span>Created: {{ formatDate(customer.createdAt) }}</span>
          <span>Updated: {{ formatDate(customer.updatedAt) }}</span>
        </div>
      </div>

      <!-- Personal Information Card -->
      <div class="info-card">
        <div class="info-card__header">
          <h2>Personal Information</h2>
          <button 
            class="edit-button"
            @click="handleEdit"
            title="Edit personal information"
          >
            ✏️
          </button>
        </div>
        <div class="info-card__content">
          <div class="info-grid">
            <div class="info-item">
              <label class="info-label">First Name</label>
              <div class="info-value">{{ customer.firstName }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">Last Name</label>
              <div class="info-value">{{ customer.lastName }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">PESEL</label>
              <div class="info-value">
                <span v-if="customer.pesel">{{ customer.pesel }}</span>
                <span v-else class="info-value--empty">Not provided</span>
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">NIP</label>
              <div class="info-value">
                <span v-if="customer.nip">{{ customer.nip }}</span>
                <span v-else class="info-value--empty">Not provided</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Contact Information Card -->
      <div class="info-card">
        <div class="info-card__header">
          <h2>Contact Information</h2>
          <button 
            class="edit-button"
            @click="handleEdit"
            title="Edit contact information"
          >
            ✏️
          </button>
        </div>
        <div class="info-card__content">
          <div class="info-grid">
            <div class="info-item">
              <label class="info-label">Email</label>
              <div class="info-value">
                <a :href="`mailto:${customer.email}`" class="info-link">
                  {{ customer.email }}
                </a>
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Phone</label>
              <div class="info-value">
                <span v-if="customer.phone">
                  <a :href="`tel:${customer.phone}`" class="info-link">
                    {{ customer.phone }}
                  </a>
                </span>
                <span v-else class="info-value--empty">Not provided</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- System Information Card -->
      <div class="info-card">
        <div class="info-card__header">
          <h2>System Information</h2>
        </div>
        <div class="info-card__content">
          <div class="info-grid">
            <div class="info-item">
              <label class="info-label">Customer ID</label>
              <div class="info-value info-value--mono">{{ customer.id }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">Version</label>
              <div class="info-value">{{ customer.version }}</div>
            </div>
            <div class="info-item">
              <label class="info-label">Status</label>
              <div class="info-value">
                <AppBadge
                  :variant="getCustomerStatusVariant(customer.status)"
                  :text="CUSTOMER_STATUS_LABELS[customer.status]"
                  size="sm"
                />
              </div>
            </div>
            <div class="info-item">
              <label class="info-label">Last Updated</label>
              <div class="info-value">{{ formatDateTime(customer.updatedAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Related Addresses (Placeholder) -->
      <div class="info-card">
        <div class="info-card__header">
          <h2>Addresses</h2>
          <AppButton 
            variant="ghost" 
            size="sm"
            @click="navigateTo('/addresses')"
          >
            View All
          </AppButton>
        </div>
        <div class="info-card__content">
          <div class="empty-content">
            <div class="empty-icon">📍</div>
            <p>No addresses associated with this customer yet.</p>
            <AppButton 
              variant="primary" 
              size="sm"
              @click="handleAddAddress"
            >
              Add Address
            </AppButton>
          </div>
        </div>
      </div>
    </div>

    <!-- Status Change Modal -->
    <AppModal
      v-model="showStatusModal"
      title="Change Customer Status"
      size="md"
      :closable="true"
    >
      <div class="status-modal-content">
        <p>Change status for {{ formatCustomerName(customer) }}</p>
        <div class="current-status">
          <span class="current-status-label">Current Status:</span>
          <AppBadge
            :variant="getCustomerStatusVariant(customer.status)"
            :text="CUSTOMER_STATUS_LABELS[customer.status]"
            size="lg"
          />
        </div>

        <div class="status-options">
          <label class="status-option-label">Select New Status:</label>
          <div class="status-option-grid">
            <button
              v-for="status in availableStatuses"
              :key="status"
              class="status-option-button"
              :class="{ active: selectedStatus === status }"
              @click="selectedStatus = status"
            >
              <AppBadge
                :variant="getCustomerStatusVariant(status)"
                :text="CUSTOMER_STATUS_LABELS[status]"
                size="md"
              />
            </button>
          </div>
        </div>

        <div v-if="selectedStatus && selectedStatus !== customer.status" class="status-change-notice">
          <p class="notice-text">
            <i class="pi pi-info-circle"></i>
            Changing status will update the customer's service access.
          </p>
        </div>

        <div class="modal-actions">
          <AppButton
            variant="secondary"
            @click="handleCancelStatusChange"
            :disabled="changingStatus"
          >
            Cancel
          </AppButton>
          <AppButton
            variant="primary"
            :loading="changingStatus"
            :disabled="!selectedStatus || selectedStatus === customer.status"
            @click="handleSubmitStatusChange"
          >
            Update Status
          </AppButton>
        </div>
      </div>
    </AppModal>

    <!-- Delete Confirmation Modal -->
    <AppModal
      v-model="showDeleteModal"
      title="Delete Customer"
      size="md"
      :closable="true"
    >
      <div class="delete-modal-content">
        <p>Are you sure you want to delete {{ formatCustomerName(customer) }}?</p>
        <p class="warning-text">This action cannot be undone.</p>

        <div class="modal-actions">
          <AppButton
            variant="secondary"
            @click="showDeleteModal = false"
          >
            Cancel
          </AppButton>
          <AppButton
            variant="danger"
            :loading="deleting"
            @click="handleDelete"
          >
            Delete Customer
          </AppButton>
        </div>
      </div>
    </AppModal>
  </div>
</template>

<script setup lang="ts">
import type {
  Customer,
  CustomerStatus,
  CUSTOMER_STATUS_LABELS,
  CUSTOMER_STATUS_COLORS,
  formatCustomerName,
  getInitials,
  getCustomerStatusVariant
} from '~/schemas/customer'

// Page meta
definePageMeta({
  title: 'Customer Details'
})

// Route params
const route = useRoute()
const customerId = computed(() => route.params.id as string)

// Store
const customerStore = useCustomerStore()
const toast = useToast()

// Reactive state
const customer = ref<Customer>({} as Customer)
const loading = ref(true)
const error = ref<string>('')
const deleting = ref(false)
const showDeleteModal = ref(false)
const showStatusModal = ref(false)
const selectedStatus = ref<CustomerStatus | null>(null)
const changingStatus = ref(false)

// Computed
const currentCustomer = computed(() => customerStore.currentCustomer)

// Available statuses (exclude current status)
const availableStatuses = computed<CustomerStatus[]>(() => {
  const allStatuses: CustomerStatus[] = ['ACTIVE', 'INACTIVE', 'SUSPENDED', 'TERMINATED']
  return allStatuses.filter(status => status !== customer.value.status)
})

// Methods
const fetchCustomer = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await customerStore.fetchCustomerById(customerId.value)
    customer.value = response
  } catch (err: any) {
    console.error('Failed to fetch customer:', err)
    error.value = err.message || 'Failed to load customer details'
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  navigateTo(`/customers/${customerId.value}/edit`)
}

const handleChangeStatus = () => {
  selectedStatus.value = null
  showStatusModal.value = true
}

const handleCancelStatusChange = () => {
  selectedStatus.value = null
  showStatusModal.value = false
}

const handleSubmitStatusChange = async () => {
  if (!selectedStatus.value || selectedStatus.value === customer.value.status) {
    return
  }

  changingStatus.value = true

  try {
    const response = await customerStore.changeCustomerStatus({
      id: customerId.value,
      status: selectedStatus.value
    })

    // Update local customer data
    customer.value = response

    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: `Customer status changed to ${CUSTOMER_STATUS_LABELS[selectedStatus.value]}.`,
      life: 5000
    })

    showStatusModal.value = false
    selectedStatus.value = null

  } catch (err: any) {
    console.error('Failed to change customer status:', err)
    // Error handling is done in customerStore
  } finally {
    changingStatus.value = false
  }
}

const handleDelete = async () => {
  deleting.value = true

  try {
    await customerStore.deleteCustomer(customerId.value)

    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: `${formatCustomerName(customer.value)} has been successfully deleted.`,
      life: 5000
    })

    showDeleteModal.value = false
    navigateTo('/customers')

  } catch (err: any) {
    console.error('Failed to delete customer:', err)
    // Error handling is done in customerStore
  } finally {
    deleting.value = false
  }
}

const handleAddAddress = () => {
  toast.add({
    severity: 'info',
    summary: 'Coming Soon',
    detail: 'Address management will be implemented soon.',
    life: 5000
  })
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const formatDateTime = (dateString: string) => {
  return new Date(dateString).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Initialize
onMounted(() => {
  fetchCustomer()
})

// Watch for route changes
watch(() => route.params.id, () => {
  if (route.params.id) {
    fetchCustomer()
  }
})
</script>

<style scoped>
.customer-detail-page {
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

.back-link {
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: var(--font-size-sm);
  margin-bottom: var(--space-2);
  display: inline-block;
  transition: color var(--transition-fast) var(--transition-timing);
}

.back-link:hover {
  color: var(--color-primary);
}

.customer-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-top: var(--space-2);
}

.customer-avatar {
  width: 64px;
  height: 64px;
  background: var(--color-primary);
  color: var(--color-text-primary);
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  flex-shrink: 0;
}

.customer-info {
  flex: 1;
}

.customer-name {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.customer-email {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.page-header__actions {
  display: flex;
  gap: var(--space-3);
  flex-shrink: 0;
}

/* Loading and Error States */
.loading-state,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-12);
  text-align: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--color-border);
  border-top: 4px solid var(--color-primary);
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
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-6);
}

.customer-dates {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  text-align: right;
}

/* Info Cards */
.info-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.info-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  background: var(--color-surface-alt);
  border-bottom: 1px solid var(--color-border);
}

.info-card__header h2 {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.edit-button {
  background: none;
  border: none;
  color: var(--color-text-secondary);
  cursor: pointer;
  padding: var(--space-1);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  transition: all var(--transition-fast) var(--transition-timing);
}

.edit-button:hover {
  color: var(--color-primary);
  background: var(--color-surface);
}

.info-card__content {
  padding: var(--space-6);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-6);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.info-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-value {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

.info-value--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

.info-value--mono {
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: var(--font-size-sm);
}

.info-link {
  color: var(--color-primary);
  text-decoration: none;
  transition: color var(--transition-fast) var(--transition-timing);
}

.info-link:hover {
  color: var(--color-primary-hover);
}

/* Empty Content */
.empty-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: var(--space-8);
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: var(--space-4);
  opacity: 0.5;
}

.empty-content p {
  margin: 0 0 var(--space-4) 0;
  color: var(--color-text-secondary);
}

/* Status Modal */
.status-modal-content {
  padding: var(--space-4) 0;
}

.status-modal-content > p {
  margin: 0 0 var(--space-4) 0;
  color: var(--color-text-primary);
}

.current-status {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-surface-secondary);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-6);
}

.current-status-label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.status-options {
  margin-bottom: var(--space-6);
}

.status-option-label {
  display: block;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-3);
}

.status-option-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--space-3);
}

.status-option-button {
  padding: var(--space-3);
  background: var(--color-surface-secondary);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-fast) var(--transition-timing);
  display: flex;
  justify-content: center;
}

.status-option-button:hover {
  background: var(--color-surface-hover);
  border-color: var(--color-border);
}

.status-option-button.active {
  background: var(--color-primary-100);
  border-color: var(--color-primary);
}

.status-change-notice {
  padding: var(--space-3);
  background: var(--blue-50);
  border: 1px solid var(--blue-200);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-6);
}

.notice-text {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.notice-text .pi {
  color: var(--blue-600);
}

/* Delete Modal */
.delete-modal-content {
  padding: var(--space-4) 0;
}

.delete-modal-content p {
  margin: 0 0 var(--space-4) 0;
  color: var(--color-text-primary);
}

.warning-text {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.modal-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  margin-top: var(--space-6);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }
  
  .customer-header {
    gap: var(--space-3);
  }
  
  .customer-avatar {
    width: 48px;
    height: 48px;
    font-size: var(--font-size-lg);
  }
  
  .customer-name {
    font-size: var(--font-size-xl);
  }
  
  .page-header__actions {
    justify-content: stretch;
  }
  
  .page-header__actions .app-button {
    flex: 1;
  }
  
  .status-section {
    flex-direction: column;
    gap: var(--space-3);
    text-align: center;
  }
  
  .customer-dates {
    text-align: center;
  }
  
  .info-grid {
    grid-template-columns: 1fr;
    gap: var(--space-4);
  }
  
  .info-card__content {
    padding: var(--space-4);
  }
  
  .modal-actions {
    flex-direction: column;
  }

  .status-option-grid {
    grid-template-columns: 1fr;
  }

  .current-status {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .status-option-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
