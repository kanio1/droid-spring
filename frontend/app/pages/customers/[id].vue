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
} from '~/types/customer'

// Page meta
definePageMeta({
  title: 'Customer Details'
})

// Route params
const route = useRoute()
const customerId = computed(() => route.params.id as string)

// Composables
const { get, del } = useApi()
const { showToast, confirm } = useToast()
const { showModal, closeModal } = useModal()

// Reactive state
const customer = ref<Customer>({} as Customer)
const loading = ref(true)
const error = ref<string>('')
const deleting = ref(false)
const showDeleteModal = ref(false)

// Methods
const fetchCustomer = async () => {
  loading.value = true
  error.value = ''
  
  try {
    const response = await get<Customer>(`/customers/${customerId.value}`)
    customer.value = response.data
  } catch (err: any) {
    console.error('Failed to fetch customer:', err)
    if (err.status === 404) {
      error.value = 'Customer not found'
    } else {
      error.value = 'Failed to load customer details'
    }
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  navigateTo(`/customers/${customerId.value}/edit`)
}

const handleChangeStatus = () => {
  // TODO: Implement status change modal
  showToast({
    type: 'info',
    title: 'Coming Soon',
    message: 'Status change functionality will be implemented soon.'
  })
}

const handleDelete = async () => {
  deleting.value = true
  
  try {
    await del(`/customers/${customerId.value}`)
    
    showToast({
      type: 'success',
      title: 'Customer Deleted',
      message: `${formatCustomerName(customer.value)} has been successfully deleted.`
    })
    
    showDeleteModal.value = false
    navigateTo('/customers')
    
  } catch (err) {
    console.error('Failed to delete customer:', err)
    // Error handling is done in useApi composable
  } finally {
    deleting.value = false
  }
}

const handleAddAddress = () => {
  showToast({
    type: 'info',
    title: 'Coming Soon',
    message: 'Address management will be implemented soon.'
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
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .info-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
