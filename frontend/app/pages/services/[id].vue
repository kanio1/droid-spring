<template>
  <div class="service-detail-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/services" class="back-link">
        ← Back to Services
      </NuxtLink>
      <div class="page-header__content">
        <div class="title-row">
          <h1 class="page-title">Service {{ service?.serviceName || '...' }}</h1>
          <StatusBadge v-if="service" :status="service.status" type="service" size="large" />
        </div>
        <p class="page-subtitle" v-if="service">
          {{ formatServiceType(service.serviceType) }} - {{ service.serviceCode }}
        </p>
      </div>
      <div class="page-header__actions">
        <Button
          v-if="canActivate"
          label="Activate"
          icon="pi pi-play"
          severity="success"
          @click="handleActivate"
        />
        <Button
          v-if="canConfigure"
          label="Configure"
          icon="pi pi-cog"
          severity="info"
          @click="handleConfigure"
        />
        <Button
          v-if="canEdit"
          label="Edit"
          icon="pi pi-pencil"
          severity="primary"
          @click="handleEdit"
        />
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner />
      <p>Loading service details...</p>
    </div>

    <div v-else-if="error" class="error-state">
      <i class="pi pi-exclamation-triangle"></i>
      <p>{{ error }}</p>
      <Button label="Retry" @click="fetchService" />
    </div>

    <div v-else-if="service" class="service-content">
      <!-- Service Summary Card -->
      <div class="card service-summary">
        <div class="card-header">
          <h2>Service Summary</h2>
        </div>
        <div class="card-body">
          <div class="summary-grid">
            <div class="summary-item">
              <label>Service Code</label>
              <span>{{ service.serviceCode }}</span>
            </div>
            <div class="summary-item">
              <label>Service Name</label>
              <span>{{ service.serviceName }}</span>
            </div>
            <div class="summary-item">
              <label>Type</label>
              <span>{{ formatServiceType(service.serviceType) }}</span>
            </div>
            <div class="summary-item">
              <label>Category</label>
              <span>{{ formatCategory(service.category) }}</span>
            </div>
            <div class="summary-item">
              <label>Provisioning Time</label>
              <span>{{ service.provisioningTime }} minutes</span>
            </div>
            <div class="summary-item">
              <label>Auto Provision</label>
              <div class="auto-provision">
                <i :class="service.autoProvision ? 'pi pi-check-circle text-success' : 'pi pi-times-circle text-muted'"></i>
                <span class="ml-2">{{ service.autoProvision ? 'Enabled' : 'Disabled' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Service Dependencies -->
      <div v-if="service.dependencies && service.dependencies.length > 0" class="card service-dependencies">
        <div class="card-header">
          <h2>Service Dependencies</h2>
        </div>
        <div class="card-body">
          <div class="dependencies-list">
            <div v-for="(dep, index) in service.dependencies" :key="index" class="dependency-item">
              <i class="pi pi-link dependency-icon"></i>
              <div class="dependency-info">
                <div class="dependency-code">{{ dep.serviceCode }}</div>
                <div class="dependency-name">{{ dep.serviceName }}</div>
              </div>
              <StatusBadge :status="dep.status" type="service" size="small" />
            </div>
          </div>
        </div>
      </div>

      <!-- Service Features -->
      <div v-if="service.features && service.features.length > 0" class="card service-features">
        <div class="card-header">
          <h2>Service Features</h2>
        </div>
        <div class="card-body">
          <div class="features-list">
            <div v-for="(feature, index) in service.features" :key="index" class="feature-item">
              <i class="pi pi-check-circle text-success"></i>
              <span>{{ feature.name }}</span>
              <span v-if="feature.description" class="feature-description">- {{ feature.description }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Activation History -->
      <div class="card activation-history">
        <div class="card-header">
          <h2>Activation History</h2>
        </div>
        <div class="card-body">
          <AppTable :columns="activationColumns" :data="service.activations || []" :show-pagination="false">
            <template #cell-customerName="{ row }">
              <div v-if="row.customerName" class="customer-name">
                <div class="customer-avatar small">
                  {{ getCustomerInitials(row.customerName) }}
                </div>
                <div>
                  <div class="customer-name__text">{{ row.customerName }}</div>
                  <div class="customer-id">{{ row.customerId }}</div>
                </div>
              </div>
              <span v-else>—</span>
            </template>
            <template #cell-activationDate="{ row }">
              {{ formatDateTime(row.activationDate) }}
            </template>
            <template #cell-status="{ row }">
              <StatusBadge :status="row.status" type="activation-status" size="small" />
            </template>
            <template #cell-actions="{ row }">
              <Button
                icon="pi pi-eye"
                text
                rounded
                @click="handleViewActivation(row)"
                v-tooltip.top="'View activation'"
              />
            </template>
            <template #empty>
              <div class="empty-history">No activation history</div>
            </template>
          </AppTable>
        </div>
      </div>

      <!-- Pricing Information -->
      <div v-if="service.pricing" class="card pricing-info">
        <div class="card-header">
          <h2>Pricing Information</h2>
        </div>
        <div class="card-body">
          <div class="pricing-grid">
            <div class="pricing-item" v-if="service.pricing.monthlyFee">
              <label>Monthly Fee</label>
              <span class="price">{{ formatCurrency(service.pricing.monthlyFee, service.pricing.currency) }}</span>
            </div>
            <div class="pricing-item" v-if="service.pricing.setupFee">
              <label>Setup Fee</label>
              <span class="price">{{ formatCurrency(service.pricing.setupFee, service.pricing.currency) }}</span>
            </div>
            <div class="pricing-item" v-if="service.pricing.usageRate">
              <label>Usage Rate</label>
              <span class="price">{{ service.pricing.usageRate }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Provisioning Configuration -->
      <div v-if="service.provisioningConfig" class="card provisioning-config">
        <div class="card-header">
          <h2>Provisioning Configuration</h2>
        </div>
        <div class="card-body">
          <div class="config-grid">
            <div v-if="service.provisioningConfig.script" class="config-item">
              <label>Provisioning Script</label>
              <code class="script">{{ service.provisioningConfig.script }}</code>
            </div>
            <div v-if="service.provisioningConfig.apiEndpoint" class="config-item">
              <label>API Endpoint</label>
              <code class="endpoint">{{ service.provisioningConfig.apiEndpoint }}</code>
            </div>
          </div>
        </div>
      </div>

      <!-- Notes -->
      <div v-if="service.notes" class="card service-notes">
        <div class="card-header">
          <h2>Notes</h2>
        </div>
        <div class="card-body">
          <p>{{ service.notes }}</p>
        </div>
      </div>

      <!-- Metadata -->
      <div class="card service-metadata">
        <div class="card-header">
          <h2>Metadata</h2>
        </div>
        <div class="card-body">
          <div class="metadata-grid">
            <div class="metadata-item">
              <label>Created</label>
              <span>{{ formatDateTime(service.createdAt) }}</span>
            </div>
            <div class="metadata-item">
              <label>Last Updated</label>
              <span>{{ formatDateTime(service.updatedAt) }}</span>
            </div>
            <div class="metadata-item">
              <label>Version</label>
              <span>{{ service.version }}</span>
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
import { useServiceStore } from '~/stores/service'

// Page meta
definePageMeta({
  title: 'Service Details'
})

// Route params
const route = useRoute()
const serviceId = route.params.id as string

// Store
const serviceStore = useServiceStore()
const { showToast } = useToast()

// Reactive state
const loading = ref(true)
const error = ref<string | null>(null)

// Computed
const service = computed(() => serviceStore.currentService)
const canActivate = computed(() => service.value && service.value.status === 'ACTIVE')
const canConfigure = computed(() => service.value && (service.value.status === 'ACTIVE' || service.value.status === 'INACTIVE'))
const canEdit = computed(() => service.value && service.value.status !== 'DEPRECATED')

// Table columns
const activationColumns = [
  { key: 'customerName', label: 'Customer', style: 'width: 25%' },
  { key: 'activationDate', label: 'Date', style: 'width: 20%' },
  { key: 'status', label: 'Status', style: 'width: 15%' },
  { key: 'actions', label: 'Actions', style: 'width: 10%' }
]

// Methods
const fetchService = async () => {
  try {
    loading.value = true
    error.value = null
    await serviceStore.fetchServiceById(serviceId)
  } catch (err: any) {
    error.value = err.message || 'Failed to load service'
  } finally {
    loading.value = false
  }
}

const handleEdit = () => {
  navigateTo(`/services/${serviceId}/edit`)
}

const handleActivate = async () => {
  if (!service.value) return

  const confirmed = confirm(`Are you sure you want to activate service ${service.value.serviceName}?`)

  if (confirmed) {
    try {
      showToast({
        severity: 'success',
        summary: 'Service Activation',
        detail: `Service ${service.value.serviceName} activation initiated.`,
        life: 3000
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to activate service',
        life: 5000
      })
    }
  }
}

const handleConfigure = async () => {
  if (!service.value) return

  showToast({
    severity: 'info',
    summary: 'Configuration',
    detail: `Service configuration for ${service.value.serviceName}.`,
    life: 3000
  })
}

const handleViewActivation = (row: any) => {
  navigateTo(`/services/activations/${row.id}`)
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

const formatCurrency = (amount: number, currency: string = 'USD'): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency
  }).format(amount)
}

const formatServiceType = (type: string): string => {
  const types: Record<string, string> = {
    INTERNET: 'Internet',
    TELEPHONY: 'Telephony',
    TELEVISION: 'Television',
    MOBILE: 'Mobile',
    CLOUD: 'Cloud'
  }
  return types[type] || type
}

const formatCategory = (category: string): string => {
  const categories: Record<string, string> = {
    CONNECTIVITY: 'Connectivity',
    COMMUNICATION: 'Communication',
    ENTERTAINMENT: 'Entertainment',
    CLOUD_SERVICES: 'Cloud Services'
  }
  return categories[category] || category
}

const getCustomerInitials = (name: string): string => {
  if (!name) return 'N/A'
  const names = name.split(' ')
  if (names.length >= 2) {
    return `${names[0][0]}${names[1][0]}`.toUpperCase()
  }
  return name.substring(0, 2).toUpperCase()
}

// Lifecycle
onMounted(async () => {
  await fetchService()
})
</script>

<style scoped>
.service-detail-page {
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

/* Service Summary */
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

.auto-provision {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

/* Dependencies */
.dependencies-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.dependency-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-surface-secondary);
  border-radius: var(--radius-md);
}

.dependency-icon {
  font-size: 1.25rem;
  color: var(--color-primary);
}

.dependency-info {
  flex: 1;
  min-width: 0;
}

.dependency-code {
  font-family: monospace;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.dependency-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* Features */
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

.feature-description {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

/* Activation History */
.empty-history {
  text-align: center;
  padding: var(--space-4);
  color: var(--color-text-muted);
}

/* Customer in Activation */
.customer-name {
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

.customer-avatar.small {
  width: 32px;
  height: 32px;
  font-size: var(--font-size-xs);
}

.customer-name__text {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.customer-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Pricing */
.pricing-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.pricing-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.pricing-item label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.price {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* Provisioning Configuration */
.config-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.config-item label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

code.script,
code.endpoint {
  font-family: 'Courier New', monospace;
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  background: var(--color-surface-secondary);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
}

/* Notes */
.service-notes p {
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
  .pricing-grid,
  .metadata-grid {
    grid-template-columns: 1fr;
  }

  .dependency-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .customer-name {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }
}
</style>
