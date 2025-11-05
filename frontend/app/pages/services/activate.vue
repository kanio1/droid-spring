<template>
  <div class="service-activate-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <div class="breadcrumb">
          <NuxtLink to="/services" class="breadcrumb__link">Services</NuxtLink>
          <i class="pi pi-chevron-right breadcrumb__separator"></i>
          <span class="breadcrumb__current">Activate Service</span>
        </div>
        <h1 class="page-title">Activate Service for Customer</h1>
        <p class="page-subtitle">
          Activate a service for an existing customer
        </p>
      </div>
      <div class="page-header__actions">
        <NuxtLink to="/services">
          <Button label="Cancel" icon="pi pi-times" severity="secondary" />
        </NuxtLink>
      </div>
    </div>

    <!-- Progress Steps -->
    <div class="progress-steps">
      <div class="progress-step" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
        <div class="progress-step__icon">
          <i class="pi pi-user"></i>
        </div>
        <div class="progress-step__content">
          <div class="progress-step__title">Customer</div>
          <div class="progress-step__description">Select customer</div>
        </div>
      </div>

      <div class="progress-divider" :class="{ active: currentStep >= 2 }"></div>

      <div class="progress-step" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
        <div class="progress-step__icon">
          <i class="pi pi-cog"></i>
        </div>
        <div class="progress-step__content">
          <div class="progress-step__title">Service</div>
          <div class="progress-step__description">Choose service</div>
        </div>
      </div>

      <div class="progress-divider" :class="{ active: currentStep >= 3 }"></div>

      <div class="progress-step" :class="{ active: currentStep >= 3, completed: currentStep > 3 }">
        <div class="progress-step__icon">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="progress-step__content">
          <div class="progress-step__title">Eligibility</div>
          <div class="progress-step__description">Verify coverage</div>
        </div>
      </div>

      <div class="progress-divider" :class="{ active: currentStep >= 4 }"></div>

      <div class="progress-step" :class="{ active: currentStep >= 4, completed: currentStep > 4 }">
        <div class="progress-step__icon">
          <i class="pi pi-calendar"></i>
        </div>
        <div class="progress-step__content">
          <div class="progress-step__title">Configuration</div>
          <div class="progress-step__description">Set details</div>
        </div>
      </div>

      <div class="progress-divider" :class="{ active: currentStep >= 5 }"></div>

      <div class="progress-step" :class="{ active: currentStep >= 5, completed: currentStep > 5 }">
        <div class="progress-step__icon">
          <i class="pi pi-flag"></i>
        </div>
        <div class="progress-step__content">
          <div class="progress-step__title">Confirm</div>
          <div class="progress-step__description">Review & activate</div>
        </div>
      </div>
    </div>

    <!-- Activation Form -->
    <div class="activation-form-container">
      <div class="form-card">
        <!-- Step 1: Customer Selection -->
        <div v-if="currentStep === 1" class="form-step">
          <div class="form-step__header">
            <h2>Select Customer</h2>
            <p>Choose the customer for whom you want to activate a service</p>
          </div>

          <div class="form-step__body">
            <div class="form-field">
              <label for="customer" class="form-field__label">
                Customer <span class="required-mark">*</span>
              </label>
              <AutoComplete
                id="customer"
                v-model="selectedCustomer"
                :suggestions="customerSuggestions"
                @complete="searchCustomers"
                dropdown
                :forceSelection="false"
                optionLabel="name"
                placeholder="Search by name, email, or ID"
                :class="{ 'p-invalid': errors.customer }"
                style="width: 100%"
              >
                <template #option="{ option }">
                  <div class="customer-option">
                    <div class="customer-option__name">{{ option.name }}</div>
                    <div class="customer-option__details">
                      {{ option.email }} | ID: {{ option.id }}
                    </div>
                  </div>
                </template>
              </AutoComplete>
              <small v-if="errors.customer" class="p-error">{{ errors.customer }}</small>
              <small class="form-field__help">Start typing to search for customers</small>
            </div>

            <div v-if="selectedCustomer" class="selected-customer-card">
              <div class="selected-customer-card__header">
                <h3>Selected Customer</h3>
              </div>
              <div class="selected-customer-card__body">
                <div class="customer-detail">
                  <span class="customer-detail__label">Name:</span>
                  <span class="customer-detail__value">{{ selectedCustomer.name }}</span>
                </div>
                <div class="customer-detail">
                  <span class="customer-detail__label">Email:</span>
                  <span class="customer-detail__value">{{ selectedCustomer.email }}</span>
                </div>
                <div class="customer-detail">
                  <span class="customer-detail__label">Phone:</span>
                  <span class="customer-detail__value">{{ selectedCustomer.phone || '---' }}</span>
                </div>
                <div class="customer-detail">
                  <span class="customer-detail__label">Address:</span>
                  <span class="customer-detail__value">{{ formatCustomerAddress(selectedCustomer) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 2: Service Selection -->
        <div v-if="currentStep === 2" class="form-step">
          <div class="form-step__header">
            <h2>Select Service</h2>
            <p>Choose the service to activate for this customer</p>
          </div>

          <div class="form-step__body">
            <div class="form-field">
              <label for="service" class="form-field__label">
                Service <span class="required-mark">*</span>
              </label>
              <Dropdown
                id="service"
                v-model="selectedService"
                :options="serviceOptions"
                optionLabel="name"
                placeholder="Select a service"
                :class="{ 'p-invalid': errors.service }"
                style="width: 100%"
              >
                <template #option="{ option }">
                  <div class="service-option">
                    <div class="service-option__header">
                      <span class="service-option__name">{{ option.name }}</span>
                      <Tag :value="option.type" severity="secondary" />
                    </div>
                    <div class="service-option__details">
                      {{ formatPrice(option.price, option.currency) }} / {{ getBillingCycleLabel(option.billingCycle) }}
                    </div>
                    <div class="service-option__code">{{ option.code }}</div>
                  </div>
                </template>
              </Dropdown>
              <small v-if="errors.service" class="p-error">{{ errors.service }}</small>
              <small class="form-field__help">Pre-filtered to show only active services</small>
            </div>

            <div v-if="selectedService" class="selected-service-card">
              <div class="selected-service-card__header">
                <h3>Selected Service</h3>
              </div>
              <div class="selected-service-card__body">
                <div class="service-detail">
                  <div class="service-detail__header">
                    <span class="service-detail__name">{{ selectedService.name }}</span>
                    <Tag :value="selectedService.status" :severity="getStatusVariant(selectedService.status)" />
                  </div>
                  <div class="service-detail__info">
                    <div class="service-detail__item">
                      <i class="pi pi-dollar"></i>
                      <span>{{ formatPrice(selectedService.price, selectedService.currency) }}</span>
                    </div>
                    <div class="service-detail__item">
                      <i class="pi pi-refresh"></i>
                      <span>{{ getBillingCycleLabel(selectedService.billingCycle) }}</span>
                    </div>
                    <div class="service-detail__item">
                      <i class="pi pi-users"></i>
                      <span>{{ selectedService.activeCustomerCount }} customers</span>
                    </div>
                  </div>
                  <div class="service-detail__description">
                    {{ selectedService.description }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 3: Eligibility Check -->
        <div v-if="currentStep === 3" class="form-step">
          <div class="form-step__header">
            <h2>Eligibility Check</h2>
            <p>Verifying customer eligibility and coverage</p>
          </div>

          <div class="form-step__body">
            <div class="eligibility-check">
              <div class="eligibility-item" :class="{ success: eligibilityChecks.coverage, error: eligibilityChecks.coverage === false }">
                <div class="eligibility-item__icon">
                  <i v-if="eligibilityChecks.coverage" class="pi pi-check-circle"></i>
                  <i v-else-if="eligibilityChecks.coverage === false" class="pi pi-times-circle"></i>
                  <i v-else class="pi pi-spin pi-spinner"></i>
                </div>
                <div class="eligibility-item__content">
                  <div class="eligibility-item__title">Coverage Availability</div>
                  <div class="eligibility-item__description">
                    {{ eligibilityCheckMessages.coverage }}
                  </div>
                </div>
              </div>

              <div class="eligibility-item" :class="{ success: eligibilityChecks.duplicate, error: eligibilityChecks.duplicate === false }">
                <div class="eligibility-item__icon">
                  <i v-if="eligibilityChecks.duplicate" class="pi pi-check-circle"></i>
                  <i v-else-if="eligibilityChecks.duplicate === false" class="pi pi-times-circle"></i>
                  <i v-else class="pi pi-spin pi-spinner"></i>
                </div>
                <div class="eligibility-item__content">
                  <div class="eligibility-item__title">Service Duplication</div>
                  <div class="eligibility-item__description">
                    {{ eligibilityCheckMessages.duplicate }}
                  </div>
                </div>
              </div>

              <div class="eligibility-item" :class="{ success: eligibilityChecks.capacity, error: eligibilityChecks.capacity === false }">
                <div class="eligibility-item__icon">
                  <i v-if="eligibilityChecks.capacity" class="pi pi-check-circle"></i>
                  <i v-else-if="eligibilityChecks.capacity === false" class="pi pi-times-circle"></i>
                  <i v-else class="pi pi-spin pi-spinner"></i>
                </div>
                <div class="eligibility-item__content">
                  <div class="eligibility-item__title">Service Capacity</div>
                  <div class="eligibility-item__description">
                    {{ eligibilityCheckMessages.capacity }}
                  </div>
                </div>
              </div>
            </div>

            <div v-if="eligibilityChecksFailed" class="eligibility-failed">
              <i class="pi pi-exclamation-triangle"></i>
              <h3>Activation Not Possible</h3>
              <p>One or more eligibility checks failed. Please review the requirements above.</p>
            </div>

            <div v-else-if="eligibilityChecksPassed && allChecksCompleted" class="eligibility-passed">
              <i class="pi pi-check-circle"></i>
              <h3>Eligible for Activation</h3>
              <p>All eligibility checks passed. You can proceed with the activation.</p>
            </div>
          </div>
        </div>

        <!-- Step 4: Configuration -->
        <div v-if="currentStep === 4" class="form-step">
          <div class="form-step__header">
            <h2>Activation Configuration</h2>
            <p>Configure the service activation details</p>
          </div>

          <div class="form-step__body">
            <div class="form-grid">
              <div class="form-field">
                <label for="startDate" class="form-field__label">
                  Start Date <span class="required-mark">*</span>
                </label>
                <Calendar
                  id="startDate"
                  v-model="activationData.startDate"
                  :minDate="new Date()"
                  dateFormat="yy-mm-dd"
                  placeholder="Select start date"
                  :class="{ 'p-invalid': errors.startDate }"
                  style="width: 100%"
                />
                <small v-if="errors.startDate" class="p-error">{{ errors.startDate }}</small>
                <small class="form-field__help">When the service should become active</small>
              </div>

              <div class="form-field">
                <label for="activationType" class="form-field__label">
                  Activation Type <span class="required-mark">*</span>
                </label>
                <Dropdown
                  id="activationType"
                  v-model="activationData.activationType"
                  :options="activationTypeOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Select type"
                  :class="{ 'p-invalid': errors.activationType }"
                  style="width: 100%"
                />
                <small v-if="errors.activationType" class="p-error">{{ errors.activationType }}</small>
                <small class="form-field__help">Type of activation</small>
              </div>

              <div class="form-field form-field--full">
                <label for="notes" class="form-field__label">
                  Notes
                </label>
                <Textarea
                  id="notes"
                  v-model="activationData.notes"
                  placeholder="Add any additional notes..."
                  :autoResize="true"
                  rows="3"
                  style="width: 100%"
                />
                <small class="form-field__help">Optional notes about this activation</small>
              </div>
            </div>

            <div v-if="selectedService" class="service-summary">
              <h3>Service Summary</h3>
              <div class="service-summary__content">
                <div class="summary-row">
                  <span class="summary-row__label">Service:</span>
                  <span class="summary-row__value">{{ selectedService.name }}</span>
                </div>
                <div class="summary-row">
                  <span class="summary-row__label">Price:</span>
                  <span class="summary-row__value">{{ formatPrice(selectedService.price, selectedService.currency) }}</span>
                </div>
                <div class="summary-row">
                  <span class="summary-row__label">Billing Cycle:</span>
                  <span class="summary-row__value">{{ getBillingCycleLabel(selectedService.billingCycle) }}</span>
                </div>
                <div class="summary-row" v-if="selectedService.dataLimit">
                  <span class="summary-row__label">Data Limit:</span>
                  <span class="summary-row__value">{{ formatDataLimit(selectedService.dataLimit) }}</span>
                </div>
                <div class="summary-row" v-if="selectedService.speed">
                  <span class="summary-row__label">Speed:</span>
                  <span class="summary-row__value">{{ formatSpeed(selectedService.speed) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Step 5: Confirmation -->
        <div v-if="currentStep === 5" class="form-step">
          <div class="form-step__header">
            <h2>Confirm Activation</h2>
            <p>Review all details before activating the service</p>
          </div>

          <div class="form-step__body">
            <div class="confirmation-section">
              <div class="confirmation-card">
                <div class="confirmation-card__header">
                  <h3>Customer Information</h3>
                </div>
                <div class="confirmation-card__body">
                  <div class="info-row">
                    <span class="info-row__label">Name:</span>
                    <span class="info-row__value">{{ selectedCustomer?.name }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-row__label">Email:</span>
                    <span class="info-row__value">{{ selectedCustomer?.email }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-row__label">Phone:</span>
                    <span class="info-row__value">{{ selectedCustomer?.phone || '---' }}</span>
                  </div>
                </div>
              </div>

              <div class="confirmation-card">
                <div class="confirmation-card__header">
                  <h3>Service Information</h3>
                </div>
                <div class="confirmation-card__body">
                  <div class="info-row">
                    <span class="info-row__label">Service:</span>
                    <span class="info-row__value">{{ selectedService?.name }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-row__label">Code:</span>
                    <span class="info-row__value">{{ selectedService?.code }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-row__label">Type:</span>
                    <span class="info-row__value">{{ selectedService?.type }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-row__label">Price:</span>
                    <span class="info-row__value">{{ selectedService ? formatPrice(selectedService.price, selectedService.currency) : '---' }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-row__label">Billing:</span>
                    <span class="info-row__value">{{ selectedService ? getBillingCycleLabel(selectedService.billingCycle) : '---' }}</span>
                  </div>
                </div>
              </div>

              <div class="confirmation-card">
                <div class="confirmation-card__header">
                  <h3>Activation Details</h3>
                </div>
                <div class="confirmation-card__body">
                  <div class="info-row">
                    <span class="info-row__label">Start Date:</span>
                    <span class="info-row__value">{{ formatDate(activationData.startDate) }}</span>
                  </div>
                  <div class="info-row">
                    <span class="info-row__label">Activation Type:</span>
                    <span class="info-row__value">{{ activationData.activationType }}</span>
                  </div>
                  <div class="info-row" v-if="activationData.notes">
                    <span class="info-row__label">Notes:</span>
                    <span class="info-row__value">{{ activationData.notes }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="terms-section">
              <Checkbox
                v-model="activationData.agreeToTerms"
                :binary="true"
                inputId="terms"
              />
              <label for="terms" class="terms-label">
                I confirm that all information is correct and I authorize the activation of this service
              </label>
            </div>
          </div>
        </div>

        <!-- Navigation Buttons -->
        <div class="form-navigation">
          <Button
            v-if="currentStep > 1"
            label="Back"
            icon="pi pi-arrow-left"
            severity="secondary"
            outlined
            @click="previousStep"
          />
          <div class="navigation-spacer"></div>
          <Button
            v-if="currentStep < 5"
            label="Next"
            icon="pi pi-arrow-right"
            severity="primary"
            :disabled="!canProceedToNext"
            @click="nextStep"
          />
          <Button
            v-if="currentStep === 5"
            label="Activate Service"
            icon="pi pi-check"
            severity="success"
            :loading="loading"
            :disabled="!activationData.agreeToTerms"
            @click="confirmActivation"
          />
        </div>
      </div>
    </div>

    <!-- Success Dialog -->
    <Dialog
      v-model:visible="showSuccessDialog"
      modal
      header="Activation Successful"
      :style="{ width: '500px' }"
    >
      <div class="success-content">
        <i class="pi pi-check-circle success-icon"></i>
        <h3>Service Activated Successfully</h3>
        <p>
          The service <strong>{{ selectedService?.name }}</strong> has been activated
          for customer <strong>{{ selectedCustomer?.name }}</strong>.
        </p>
        <div class="success-actions">
          <NuxtLink to="/services">
            <Button label="Back to Services" icon="pi pi-list" severity="secondary" />
          </NuxtLink>
          <Button
            label="Activate Another"
            icon="pi pi-plus"
            severity="primary"
            @click="resetForm"
          />
        </div>
      </div>
    </Dialog>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useServiceStore } from '~/stores/service'
import { useToast } from 'primevue/usetoast'
import type { Service } from '~/schemas/service'
import {
  formatPrice,
  formatDataLimit,
  formatSpeed,
  getBillingCycleLabel,
  getStatusVariant
} from '~/schemas/service'

// Meta
definePageMeta({
  title: 'Activate Service'
})

// Route & Router
const route = useRoute()
const router = useRouter()

// Stores
const serviceStore = useServiceStore()
const toast = useToast()

// Reactive State
const loading = ref(false)
const currentStep = ref(1)
const showSuccessDialog = ref(false)

const selectedCustomer = ref<any>(null)
const selectedService = ref<Service | null>(null)
const customerSuggestions = ref<any[]>([])

const activationData = reactive({
  startDate: new Date(),
  activationType: 'IMMEDIATE',
  notes: '',
  agreeToTerms: false
})

const errors = ref<Record<string, string>>({})

const eligibilityChecks = reactive({
  coverage: null as boolean | null,
  duplicate: null as boolean | null,
  capacity: null as boolean | null
})

const eligibilityCheckMessages = reactive({
  coverage: 'Checking coverage availability...',
  duplicate: 'Checking for duplicate services...',
  capacity: 'Checking service capacity...'
})

// Options
const activationTypeOptions = [
  { label: 'Immediate', value: 'IMMEDIATE' },
  { label: 'Scheduled', value: 'SCHEDULED' }
]

// Computed
const serviceOptions = computed(() => serviceStore.activeServices)

const allChecksCompleted = computed(() => {
  return eligibilityChecks.coverage !== null &&
         eligibilityChecks.duplicate !== null &&
         eligibilityChecks.capacity !== null
})

const eligibilityChecksPassed = computed(() => {
  return eligibilityChecks.coverage === true &&
         eligibilityChecks.duplicate === true &&
         eligibilityChecks.capacity === true
})

const eligibilityChecksFailed = computed(() => {
  return allChecksCompleted.value && !eligibilityChecksPassed.value
})

const canProceedToNext = computed(() => {
  switch (currentStep.value) {
    case 1:
      return selectedCustomer.value !== null
    case 2:
      return selectedService.value !== null
    case 3:
      return eligibilityChecksPassed.value
    case 4:
      return activationData.startDate !== null && activationData.activationType !== ''
    case 5:
      return activationData.agreeToTerms
    default:
      return false
  }
})

// Helper Functions
function formatDate(date: Date | string | null): string {
  if (!date) return '---'
  const d = new Date(date)
  return d.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function formatCustomerAddress(customer: any): string {
  if (!customer.address) return '---'
  const addr = customer.address
  return `${addr.street || ''}, ${addr.city || ''}, ${addr.postalCode || ''}`.replace(/^, |, $/, '')
}

// Methods
function searchCustomers(event: any) {
  const query = event.query.toLowerCase()
  // Mock customer data - in real app, this would be an API call
  customerSuggestions.value = [
    { id: '1', name: 'John Doe', email: 'john.doe@example.com', phone: '123-456-7890', address: { street: '123 Main St', city: 'Warsaw', postalCode: '00-001' } },
    { id: '2', name: 'Jane Smith', email: 'jane.smith@example.com', phone: '987-654-3210', address: { street: '456 Oak Ave', city: 'Krakow', postalCode: '30-001' } },
    { id: '3', name: 'Bob Johnson', email: 'bob.johnson@example.com', phone: '555-123-4567', address: { street: '789 Pine Rd', city: 'Gdansk', postalCode: '80-001' } }
  ].filter(c =>
    c.name.toLowerCase().includes(query) ||
    c.email.toLowerCase().includes(query) ||
    c.id.includes(query)
  )
}

async function checkEligibility() {
  if (!selectedCustomer.value || !selectedService.value) return

  // Simulate eligibility checks
  eligibilityChecks.coverage = null
  eligibilityChecks.duplicate = null
  eligibilityChecks.capacity = null

  // Check coverage
  await new Promise(resolve => setTimeout(resolve, 1000))
  eligibilityChecks.coverage = true
  eligibilityCheckMessages.coverage = 'Coverage available at customer location'

  // Check for duplicates
  await new Promise(resolve => setTimeout(resolve, 800))
  eligibilityChecks.duplicate = true
  eligibilityCheckMessages.duplicate = 'No duplicate services found'

  // Check capacity
  await new Promise(resolve => setTimeout(resolve, 600))
  eligibilityChecks.capacity = true
  eligibilityCheckMessages.capacity = `Service has capacity (${selectedService.value.activeCustomerCount}/${selectedService.value.maxCustomerCount || '∞'} customers)`
}

function validateStep(step: number): boolean {
  errors.value = {}

  switch (step) {
    case 1:
      if (!selectedCustomer.value) {
        errors.value.customer = 'Please select a customer'
        return false
      }
      break
    case 2:
      if (!selectedService.value) {
        errors.value.service = 'Please select a service'
        return false
      }
      break
    case 4:
      if (!activationData.startDate) {
        errors.value.startDate = 'Start date is required'
        return false
      }
      if (!activationData.activationType) {
        errors.value.activationType = 'Activation type is required'
        return false
      }
      break
  }

  return true
}

function nextStep() {
  if (!validateStep(currentStep.value)) {
    toast.add({
      severity: 'error',
      summary: 'Validation Error',
      detail: 'Please fill in all required fields',
      life: 5000
    })
    return
  }

  currentStep.value++

  if (currentStep.value === 3) {
    checkEligibility()
  }
}

function previousStep() {
  currentStep.value--
}

async function confirmActivation() {
  if (!selectedCustomer.value || !selectedService.value) return

  try {
    loading.value = true

    await serviceStore.activateServiceForCustomer({
      serviceId: selectedService.value.id,
      customerId: selectedCustomer.value.id,
      startDate: activationData.startDate,
      activationType: activationData.activationType,
      notes: activationData.notes
    })

    showSuccessDialog.value = true
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to activate service',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

function resetForm() {
  selectedCustomer.value = null
  selectedService.value = null
  currentStep.value = 1
  activationData.startDate = new Date()
  activationData.activationType = 'IMMEDIATE'
  activationData.notes = ''
  activationData.agreeToTerms = false
  showSuccessDialog.value = false
}

// Lifecycle
onMounted(async () => {
  // Load services
  await serviceStore.fetchServices({ status: 'ACTIVE' })

  // Check if serviceId is provided in query params
  const serviceId = route.query.serviceId as string
  if (serviceId) {
    const service = serviceStore.services.find(s => s.id === serviceId)
    if (service) {
      selectedService.value = service
      currentStep.value = 1
    }
  }
})
</script>

<style scoped>
.service-activate-page {
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

.breadcrumb {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
  font-size: var(--font-size-sm);
}

.breadcrumb__link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-weight-medium);
}

.breadcrumb__link:hover {
  text-decoration: underline;
}

.breadcrumb__separator {
  color: var(--color-text-secondary);
  font-size: 0.75rem;
}

.breadcrumb__current {
  color: var(--color-text-secondary);
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

.page-header__actions {
  flex-shrink: 0;
}

/* Progress Steps */
.progress-steps {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow-x: auto;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  transition: all 0.3s;
}

.progress-step.active {
  background: var(--color-primary-100);
}

.progress-step.completed {
  background: var(--green-100);
}

.progress-step__icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  background: var(--color-surface-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  font-size: 1.25rem;
}

.progress-step.active .progress-step__icon {
  background: var(--color-primary);
  color: white;
}

.progress-step.completed .progress-step__icon {
  background: var(--green-500);
  color: white;
}

.progress-step__content {
  display: flex;
  flex-direction: column;
}

.progress-step__title {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
}

.progress-step__description {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.progress-divider {
  width: 40px;
  height: 2px;
  background: var(--color-border);
  transition: all 0.3s;
}

.progress-divider.active {
  background: var(--color-primary);
}

/* Form Container */
.activation-form-container {
  display: flex;
  flex-direction: column;
}

.form-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.form-step__header {
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
}

.form-step__header h2 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.form-step__header p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.form-step__body {
  padding: var(--space-6);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.form-field--full {
  grid-column: 1 / -1;
}

.form-field__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.form-field__help {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.required-mark {
  color: var(--red-500);
}

/* Customer Selection */
.customer-option {
  padding: var(--space-2);
}

.customer-option__name {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.customer-option__details {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

.selected-customer-card {
  margin-top: var(--space-4);
  border: 2px solid var(--color-primary);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.selected-customer-card__header {
  background: var(--color-primary-100);
  padding: var(--space-3) var(--space-4);
}

.selected-customer-card__header h3 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
}

.selected-customer-card__body {
  padding: var(--space-4);
  background: var(--color-surface);
}

.customer-detail {
  display: flex;
  justify-content: space-between;
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--color-border);
}

.customer-detail:last-child {
  border-bottom: none;
}

.customer-detail__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.customer-detail__value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

/* Service Selection */
.service-option {
  padding: var(--space-2);
}

.service-option__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  margin-bottom: var(--space-1);
}

.service-option__name {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.service-option__details {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-1);
}

.service-option__code {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  font-family: monospace;
}

.selected-service-card {
  margin-top: var(--space-4);
  border: 2px solid var(--color-primary);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.selected-service-card__header {
  background: var(--color-primary-100);
  padding: var(--space-3) var(--space-4);
}

.selected-service-card__header h3 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-primary);
}

.selected-service-card__body {
  padding: var(--space-4);
  background: var(--color-surface);
}

.service-detail__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);
}

.service-detail__name {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.service-detail__info {
  display: flex;
  gap: var(--space-4);
  margin-bottom: var(--space-3);
}

.service-detail__item {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.service-detail__item i {
  color: var(--color-primary);
}

.service-detail__description {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.6;
}

/* Eligibility Check */
.eligibility-check {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.eligibility-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--color-surface-secondary);
  border-radius: var(--radius-lg);
  transition: all 0.3s;
}

.eligibility-item.success {
  background: var(--green-50);
  border-left: 4px solid var(--green-500);
}

.eligibility-item.error {
  background: var(--red-50);
  border-left: 4px solid var(--red-500);
}

.eligibility-item__icon {
  font-size: 1.5rem;
  flex-shrink: 0;
}

.eligibility-item.success .eligibility-item__icon {
  color: var(--green-500);
}

.eligibility-item.error .eligibility-item__icon {
  color: var(--red-500);
}

.eligibility-item__content {
  flex: 1;
}

.eligibility-item__title {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.eligibility-item__description {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.eligibility-failed,
.eligibility-passed {
  margin-top: var(--space-6);
  padding: var(--space-6);
  border-radius: var(--radius-lg);
  text-align: center;
}

.eligibility-failed {
  background: var(--red-50);
  border: 2px solid var(--red-500);
}

.eligibility-passed {
  background: var(--green-50);
  border: 2px solid var(--green-500);
}

.eligibility-failed i,
.eligibility-passed i {
  font-size: 3rem;
  margin-bottom: var(--space-3);
}

.eligibility-failed i {
  color: var(--red-500);
}

.eligibility-passed i {
  color: var(--green-500);
}

.eligibility-failed h3,
.eligibility-passed h3 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
}

.eligibility-failed h3 {
  color: var(--red-700);
}

.eligibility-passed h3 {
  color: var(--green-700);
}

.eligibility-failed p,
.eligibility-passed p {
  margin: 0;
  color: var(--color-text-secondary);
}

/* Service Summary */
.service-summary {
  margin-top: var(--space-6);
  padding: var(--space-4);
  background: var(--color-surface-secondary);
  border-radius: var(--radius-lg);
}

.service-summary h3 {
  margin: 0 0 var(--space-3) 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.service-summary__content {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--color-border);
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-row__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.summary-row__value {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* Confirmation Section */
.confirmation-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.confirmation-card {
  background: var(--color-surface-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.confirmation-card__header {
  padding: var(--space-3) var(--space-4);
  background: var(--color-background-secondary);
  border-bottom: 1px solid var(--color-border);
}

.confirmation-card__header h3 {
  margin: 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.confirmation-card__body {
  padding: var(--space-4);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--color-border);
}

.info-row:last-child {
  border-bottom: none;
}

.info-row__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.info-row__value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  text-align: right;
}

/* Terms Section */
.terms-section {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-4);
  padding: var(--space-4);
  background: var(--color-surface-secondary);
  border-radius: var(--radius-lg);
}

.terms-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  cursor: pointer;
}

/* Navigation */
.form-navigation {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-6);
  border-top: 1px solid var(--color-border);
  background: var(--color-surface-secondary);
}

.navigation-spacer {
  flex: 1;
}

/* Success Dialog */
.success-content {
  text-align: center;
  padding: var(--space-4);
}

.success-icon {
  font-size: 4rem;
  color: var(--green-500);
  margin-bottom: var(--space-3);
}

.success-content h3 {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.success-content p {
  margin: 0 0 var(--space-4) 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.success-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: center;
  margin-top: var(--space-4);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .progress-steps {
    flex-wrap: wrap;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .service-detail__info {
    flex-direction: column;
    gap: var(--space-2);
  }

  .success-actions {
    flex-direction: column;
  }
}
</style>
