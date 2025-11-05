<template>
  <div class="service-details-page page-container">
    <!-- Breadcrumb -->
    <nav class="breadcrumb-nav">
      <ol class="breadcrumb">
        <li class="breadcrumb-item">
          <NuxtLink to="/services" class="breadcrumb-link">
            <i class="pi pi-cog"></i>
            Services
          </NuxtLink>
        </li>
        <li class="breadcrumb-item breadcrumb-item--active">
          <span>{{ service?.name || 'Loading...' }}</span>
        </li>
      </ol>
    </nav>

    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <div class="service-title">
          <i :class="service ? getTypeIcon(service.type) : 'pi pi-cog'" class="service-icon"></i>
          <div>
            <h1 class="page-title">{{ service?.name || 'Loading...' }}</h1>
            <p class="service-code">{{ service?.code }}</p>
          </div>
        </div>
      </div>
      <div class="page-header__actions">
        <NuxtLink :to="`/services/${route.params.id}?edit=true`">
          <Button
            label="Edit"
            icon="pi pi-pencil"
            severity="info"
          />
        </NuxtLink>
        <NuxtLink :to="`/services/activate?serviceId=${route.params.id}`">
          <Button
            label="Activate"
            icon="pi pi-play"
            severity="success"
          />
        </NuxtLink>
        <Button
          label="Delete"
          icon="pi pi-trash"
          severity="danger"
          @click="handleDeleteClick"
        />
      </div>
    </div>

    <!-- Service Statistics -->
    <div class="service-stats">
      <div class="stat-card stat-card--price">
        <div class="stat-card__icon">
          <i class="pi pi-dollar"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">
            {{ service ? formatPrice(service.price, service.currency) : '---' }}
          </div>
          <div class="stat-card__label">Price / {{ service ? getBillingCycleLabel(service.billingCycle) : '---' }}</div>
        </div>
      </div>

      <div class="stat-card stat-card--customers">
        <div class="stat-card__icon">
          <i class="pi pi-users"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ service?.activeCustomerCount || 0 }}</div>
          <div class="stat-card__label">Active Customers</div>
        </div>
      </div>

      <div class="stat-card stat-card--data">
        <div class="stat-card__icon">
          <i class="pi pi-download"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">
            {{ service ? formatDataLimit(service.dataLimit) : '---' }}
          </div>
          <div class="stat-card__label">Data Limit</div>
        </div>
      </div>

      <div class="stat-card stat-card--speed">
        <div class="stat-card__icon">
          <i class="pi pi-bolt"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">
            {{ service ? formatSpeed(service.speed) : '---' }}
          </div>
          <div class="stat-card__label">Speed</div>
        </div>
      </div>

      <div class="stat-card stat-card--sla">
        <div class="stat-card__icon">
          <i class="pi pi-shield"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ service?.slaUptime || 0 }}%</div>
          <div class="stat-card__label">SLA Uptime</div>
        </div>
      </div>

      <div class="stat-card stat-card--support">
        <div class="stat-card__icon">
          <i class="pi pi-headphones"></i>
        </div>
        <div class="stat-card__content">
          <div class="stat-card__value">{{ service?.supportLevel || '---' }}</div>
          <div class="stat-card__label">Support Level</div>
        </div>
      </div>
    </div>

    <!-- Service Information -->
    <div class="service-info">
      <div class="info-card">
        <div class="info-card__header">
          <h2 class="info-card__title">
            <i class="pi pi-info-circle"></i>
            Service Information
          </h2>
        </div>
        <div class="info-card__body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-item__label">Name</span>
              <span class="info-item__value">{{ service?.name }}</span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Code</span>
              <span class="info-item__value">{{ service?.code }}</span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Type</span>
              <span class="info-item__value">
                <Tag :value="service?.type" severity="secondary" />
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Category</span>
              <span class="info-item__value">
                <Tag :value="service?.category" severity="info" />
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Status</span>
              <span class="info-item__value">
                <Tag :value="service?.status" :severity="service ? getStatusVariant(service.status) : 'secondary'" />
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Technology</span>
              <span class="info-item__value">
                <Tag :value="service?.technology" severity="success" />
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Created</span>
              <span class="info-item__value">{{ formatDate(service?.createdAt) }}</span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Last Updated</span>
              <span class="info-item__value">{{ formatDate(service?.updatedAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="info-card">
        <div class="info-card__header">
          <h2 class="info-card__title">
            <i class="pi pi-list"></i>
            Service Details
          </h2>
        </div>
        <div class="info-card__body">
          <div class="info-grid">
            <div class="info-item info-item--full">
              <span class="info-item__label">Description</span>
              <p class="info-item__value">{{ service?.description || 'No description provided' }}</p>
            </div>
            <div class="info-item">
              <span class="info-item__label">Data Limit</span>
              <span class="info-item__value">
                {{ service ? formatDataLimit(service.dataLimit) : '---' }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Speed</span>
              <span class="info-item__value">
                {{ service ? formatSpeed(service.speed) : '---' }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Voice Minutes</span>
              <span class="info-item__value">
                {{ service?.voiceMinutes ? formatVoiceMinutes(service.voiceMinutes) : '---' }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">SMS Count</span>
              <span class="info-item__value">
                {{ service?.smsCount || '---' }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Bandwidth</span>
              <span class="info-item__value">
                {{ service?.bandwidth || '---' }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Latency</span>
              <span class="info-item__value">
                {{ service?.latency || '---' }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Max Customers</span>
              <span class="info-item__value">{{ service?.maxCustomerCount || '---' }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="info-card">
        <div class="info-card__header">
          <h2 class="info-card__title">
            <i class="pi pi-dollar"></i>
            Pricing Information
          </h2>
        </div>
        <div class="info-card__body">
          <div class="info-grid">
            <div class="info-item">
              <span class="info-item__label">Price</span>
              <span class="info-item__value price-highlight">
                {{ service ? formatPrice(service.price, service.currency) : '---' }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Currency</span>
              <span class="info-item__value">{{ service?.currency }}</span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Billing Cycle</span>
              <span class="info-item__value">
                {{ service ? getBillingCycleLabel(service.billingCycle) : '---' }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-item__label">Annual Cost</span>
              <span class="info-item__value">
                {{ service ? formatPrice(calculateAnnualPrice(service), service.currency) : '---' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Service Features -->
    <div class="service-features" v-if="service?.features && service.features.length > 0">
      <div class="features-card">
        <div class="features-card__header">
          <h2 class="features-card__title">
            <i class="pi pi-check-circle"></i>
            Service Features
          </h2>
        </div>
        <div class="features-card__body">
          <ul class="features-list">
            <li v-for="(feature, index) in service.features" :key="index" class="feature-item">
              <i class="pi pi-check"></i>
              <span>{{ feature }}</span>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- Coverage Requirements -->
    <div class="coverage-section" v-if="service?.requiredCoverageNodes && service.requiredCoverageNodes.length > 0">
      <div class="coverage-card">
        <div class="coverage-card__header">
          <h2 class="coverage-card__title">
            <i class="pi pi-map"></i>
            Coverage Requirements
          </h2>
        </div>
        <div class="coverage-card__body">
          <div class="coverage-nodes">
            <Tag
              v-for="(node, index) in service.requiredCoverageNodes"
              :key="index"
              :value="`Node ${node}`"
              severity="warning"
            />
          </div>
          <p class="coverage-description">
            This service requires coverage in {{ service.requiredCoverageNodes.length }} node(s)
          </p>
        </div>
      </div>
    </div>

    <!-- Related Information -->
    <div class="related-info">
      <div class="related-card">
        <div class="related-card__icon">
          <i class="pi pi-users"></i>
        </div>
        <div class="related-card__content">
          <h3 class="related-card__title">Activated Customers</h3>
          <p class="related-card__description">
            View and manage customers using this service
          </p>
          <NuxtLink to="/customers">
            <Button label="View Customers" icon="pi pi-arrow-right" severity="secondary" size="small" outlined />
          </NuxtLink>
        </div>
      </div>

      <div class="related-card">
        <div class="related-card__icon">
          <i class="pi pi-star"></i>
        </div>
        <div class="related-card__content">
          <h3 class="related-card__title">Similar Services</h3>
          <p class="related-card__description">
            Find services with similar features and pricing
          </p>
          <NuxtLink to="/services">
            <Button label="Browse Services" icon="pi pi-arrow-right" severity="secondary" size="small" outlined />
          </NuxtLink>
        </div>
      </div>

      <div class="related-card">
        <div class="related-card__icon">
          <i class="pi pi-chart-line"></i>
        </div>
        <div class="related-card__content">
          <h3 class="related-card__title">Service Analytics</h3>
          <p class="related-card__description">
            View performance metrics and usage statistics
          </p>
          <Button label="View Analytics" icon="pi pi-arrow-right" severity="secondary" size="small" outlined />
        </div>
      </div>

      <div class="related-card">
        <div class="related-card__icon">
          <i class="pi pi-cog"></i>
        </div>
        <div class="related-card__content">
          <h3 class="related-card__title">Service Management</h3>
          <p class="related-card__description">
            Configure and manage service settings
          </p>
          <Button label="Manage Service" icon="pi pi-arrow-right" severity="secondary" size="small" outlined />
        </div>
      </div>
    </div>

    <!-- Delete Confirmation Dialog -->
    <Dialog
      v-model:visible="showDeleteDialog"
      modal
      header="Confirm Delete"
      :style="{ width: '450px' }"
    >
      <div class="confirmation-content">
        <i class="pi pi-exclamation-triangle confirmation-icon" />
        <p>Are you sure you want to delete this service?</p>
        <p class="confirmation-warning">
          This action cannot be undone. All customers using this service will be affected.
        </p>
      </div>
      <template #footer>
        <Button
          label="Cancel"
          icon="pi pi-times"
          severity="secondary"
          @click="showDeleteDialog = false"
        />
        <Button
          label="Delete"
          icon="pi pi-trash"
          severity="danger"
          :loading="loading"
          @click="confirmDelete"
        />
      </template>
    </Dialog>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useServiceStore } from '~/stores/service'
import { useToast } from 'primevue/usetoast'
import type { Service } from '~/schemas/service'
import {
  getStatusVariant,
  getTypeIcon,
  formatPrice,
  formatDataLimit,
  formatSpeed,
  formatVoiceMinutes,
  getBillingCycleLabel,
  calculateAnnualPrice
} from '~/schemas/service'

// Meta
definePageMeta({
  title: 'Service Details'
})

// Stores & Composables
const route = useRoute()
const serviceStore = useServiceStore()
const toast = useToast()

// Reactive State
const loading = ref(false)
const showDeleteDialog = ref(false)

// Computed
const service = computed<Service | null>(() => serviceStore.currentService)

// Helper Functions
function formatDate(date?: string | Date | null): string {
  if (!date) return '---'
  const d = new Date(date)
  return d.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

// Event Handlers
function handleDeleteClick() {
  showDeleteDialog.value = true
}

async function confirmDelete() {
  if (!service.value) return

  try {
    loading.value = true
    await serviceStore.deleteService(service.value.id)
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Service deleted successfully',
      life: 5000
    })
    showDeleteDialog.value = false
    navigateTo('/services')
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to delete service',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

// Lifecycle
onMounted(async () => {
  try {
    loading.value = true
    await serviceStore.fetchServiceById(route.params.id as string)
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to load service',
      life: 5000
    })
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.service-details-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* Breadcrumb */
.breadcrumb-nav {
  margin-bottom: var(--space-2);
}

.breadcrumb {
  display: flex;
  list-style: none;
  padding: 0;
  margin: 0;
  gap: var(--space-2);
  align-items: center;
}

.breadcrumb-item:not(.breadcrumb-item--active)::after {
  content: '/';
  margin-left: var(--space-2);
  color: var(--color-text-secondary);
}

.breadcrumb-link {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
  transition: color 0.2s;
}

.breadcrumb-link:hover {
  color: var(--color-primary-hover);
}

.breadcrumb-item--active {
  color: var(--color-text-secondary);
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

.service-title {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.service-icon {
  font-size: 2rem;
  color: var(--color-primary);
}

.page-title {
  margin: 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.service-code {
  margin: var(--space-1) 0 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
}

.page-header__actions {
  flex-shrink: 0;
  display: flex;
  gap: var(--space-2);
}

/* Service Statistics */
.service-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-4);
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.stat-card__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  background: var(--color-primary-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 1.5rem;
}

.stat-card--price .stat-card__icon {
  background: var(--green-100);
  color: var(--green-600);
}

.stat-card--customers .stat-card__icon {
  background: var(--blue-100);
  color: var(--blue-600);
}

.stat-card--data .stat-card__icon {
  background: var(--purple-100);
  color: var(--purple-600);
}

.stat-card--speed .stat-card__icon {
  background: var(--orange-100);
  color: var(--orange-600);
}

.stat-card--sla .stat-card__icon {
  background: var(--green-100);
  color: var(--green-600);
}

.stat-card--support .stat-card__icon {
  background: var(--blue-100);
  color: var(--blue-600);
}

.stat-card__content {
  flex: 1;
}

.stat-card__value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1;
}

.stat-card__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-top: var(--space-1);
}

/* Service Information */
.service-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: var(--space-4);
}

.info-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.info-card__header {
  background: var(--color-background-secondary);
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.info-card__title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.info-card__body {
  padding: var(--space-4);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.info-item--full {
  grid-column: 1 / -1;
}

.info-item__label {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.info-item__value {
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

.info-item__value p {
  margin: 0;
  line-height: 1.6;
}

.price-highlight {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--green-600);
}

/* Service Features */
.service-features {
  display: flex;
  flex-direction: column;
}

.features-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.features-card__header {
  background: var(--color-background-secondary);
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.features-card__title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.features-card__body {
  padding: var(--space-4);
}

.features-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--space-2);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  background: var(--color-background-secondary);
  border-radius: var(--radius-md);
  font-weight: var(--font-weight-medium);
}

.feature-item i {
  color: var(--green-500);
  font-size: 0.9rem;
}

/* Coverage Requirements */
.coverage-section {
  display: flex;
  flex-direction: column;
}

.coverage-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.coverage-card__header {
  background: var(--color-background-secondary);
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.coverage-card__title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.coverage-card__body {
  padding: var(--space-4);
}

.coverage-nodes {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.coverage-description {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

/* Related Information */
.related-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--space-4);
}

.related-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  gap: var(--space-3);
  transition: all 0.2s;
}

.related-card:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.related-card__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  background: var(--color-primary-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 1.5rem;
  flex-shrink: 0;
}

.related-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.related-card__title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.related-card__description {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  flex: 1;
}

/* Confirmation Dialog */
.confirmation-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) 0;
}

.confirmation-icon {
  font-size: 3rem;
  color: var(--orange-500);
}

.confirmation-warning {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  text-align: center;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header__actions {
    width: 100%;
  }

  .service-stats {
    grid-template-columns: 1fr;
  }

  .service-info {
    grid-template-columns: 1fr;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .features-list {
    grid-template-columns: 1fr;
  }

  .related-info {
    grid-template-columns: 1fr;
  }
}
</style>
