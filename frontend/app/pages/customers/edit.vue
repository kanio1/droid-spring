<template>
  <div class="customer-form-page">
    <!-- Page Header -->
    <div class="page-header">
      <NuxtLink to="/customers" class="back-link">
        ← Back to Customers
      </NuxtLink>
      <h1 class="page-title">Edit Customer</h1>
      <p class="page-subtitle">Update customer information</p>
    </div>

    <!-- Form -->
    <div class="customer-form">
      <form @submit.prevent="handleSubmit" class="form">
        <!-- Personal Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Personal Information</h2>
            <p>Update the customer's basic personal details</p>
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.firstName"
              label="First Name"
              placeholder="Enter first name"
              :error="errors.firstName"
              required
              @blur="validateField('firstName')"
            />

            <AppInput
              v-model="formData.lastName"
              label="Last Name"
              placeholder="Enter last name"
              :error="errors.lastName"
              required
              @blur="validateField('lastName')"
            />
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.pesel"
              label="PESEL"
              placeholder="Enter PESEL (11 digits)"
              :error="errors.pesel"
              @blur="validateField('pesel')"
            />

            <AppInput
              v-model="formData.nip"
              label="NIP"
              placeholder="Enter NIP (10 digits)"
              :error="errors.nip"
              @blur="validateField('nip')"
            />
          </div>
        </div>

        <!-- Contact Information Section -->
        <div class="form-section">
          <div class="form-section__header">
            <h2>Contact Information</h2>
            <p>Update how to reach the customer</p>
          </div>

          <div class="form-grid form-grid--single">
            <AppInput
              v-model="formData.email"
              label="Email Address"
              type="email"
              placeholder="Enter email address"
              :error="errors.email"
              required
              @blur="validateField('email')"
            />
          </div>

          <div class="form-grid">
            <AppInput
              v-model="formData.phone"
              label="Phone Number"
              placeholder="Enter phone number"
              :error="errors.phone"
              @blur="validateField('phone')"
            />
          </div>
        </div>

        <!-- Form Actions -->
        <div class="form-actions">
          <Button
            type="button"
            severity="secondary"
            @click="handleCancel"
            :disabled="submitting"
          >
            Cancel
          </Button>

          <Button
            type="submit"
            severity="primary"
            :loading="submitting"
            :disabled="!isFormValid || submitting"
          >
            Update Customer
          </Button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { UpdateCustomerCommand } from '~/schemas/customer'

// Page meta
definePageMeta({
  title: 'Edit Customer'
})

// Route params
const route = useRoute()
const customerId = computed(() => route.params.id as string)

// Store
const customerStore = useCustomerStore()
const toast = useToast()

// Reactive state
const formData = ref<UpdateCustomerCommand>({
  id: '',
  firstName: '',
  lastName: '',
  pesel: '',
  nip: '',
  email: '',
  phone: '',
  version: 0
})

const errors = ref<Record<string, string>>({})
const submitting = ref(false)

// Load customer data on mount
onMounted(async () => {
  try {
    const customer = await customerStore.fetchCustomerById(customerId.value)
    formData.value = {
      id: customer.id,
      firstName: customer.firstName,
      lastName: customer.lastName,
      pesel: customer.pesel || '',
      nip: customer.nip || '',
      email: customer.email,
      phone: customer.phone || '',
      version: customer.version
    }
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to load customer',
      life: 5000
    })
    navigateTo('/customers')
  }
})

// Form validation
const validateField = (field: keyof UpdateCustomerCommand) => {
  const value = formData.value[field]
  let error = ''

  switch (field) {
    case 'firstName':
      if (!value?.trim()) {
        error = 'First name is required'
      } else if (value.trim().length < 2) {
        error = 'First name must be at least 2 characters'
      } else if (!/^[A-Za-zĄąĆćĘęŁłŃńÓóŚśŹźŻż]+$/.test(value)) {
        error = 'First name can only contain letters'
      }
      break

    case 'lastName':
      if (!value?.trim()) {
        error = 'Last name is required'
      } else if (value.trim().length < 2) {
        error = 'Last name must be at least 2 characters'
      } else if (!/^[A-Za-zĄąĆćĘęŁłŃńÓóŚśŹźŻż]+$/.test(value)) {
        error = 'Last name can only contain letters'
      }
      break

    case 'email':
      if (!value?.trim()) {
        error = 'Email is required'
      } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        error = 'Please enter a valid email address'
      } else if (value.length > 255) {
        error = 'Email must be less than 255 characters'
      }
      break

    case 'phone':
      if (value && !/^\+?[1-9]\d{1,14}$/.test(value)) {
        error = 'Please enter a valid phone number'
      }
      break

    case 'pesel':
      if (value && !/^\d{11}$/.test(value)) {
        error = 'PESEL must be exactly 11 digits'
      }
      break

    case 'nip':
      if (value && !/^\d{10}$/.test(value)) {
        error = 'NIP must be exactly 10 digits'
      }
      break
  }

  errors.value[field] = error
  return !error
}

const validateForm = () => {
  const fields: (keyof UpdateCustomerCommand)[] = ['firstName', 'lastName', 'email']
  let isValid = true

  // Clear previous errors
  errors.value = {}

  // Validate required fields
  fields.forEach(field => {
    if (!validateField(field)) {
      isValid = false
    }
  })

  // Validate optional fields if provided
  ;['phone', 'pesel', 'nip'].forEach(field => {
    const fieldKey = field as keyof UpdateCustomerCommand
    if (formData.value[fieldKey]) {
      if (!validateField(fieldKey)) {
        isValid = false
      }
    }
  })

  return isValid
}

const isFormValid = computed(() => {
  return formData.value.firstName.trim() &&
         formData.value.lastName.trim() &&
         formData.value.email.trim() &&
         !errors.value.firstName &&
         !errors.value.lastName &&
         !errors.value.email
})

// Form submission
const handleSubmit = async () => {
  if (!validateForm()) {
    toast.add({
      severity: 'error',
      summary: 'Validation Error',
      detail: 'Please fix the errors in the form before submitting.',
      life: 5000
    })
    return
  }

  submitting.value = true

  try {
    // Clean up the form data (remove empty strings for optional fields)
    const submitData: UpdateCustomerCommand = {
      id: formData.value.id,
      firstName: formData.value.firstName.trim(),
      lastName: formData.value.lastName.trim(),
      email: formData.value.email.trim(),
      phone: formData.value.phone?.trim() || undefined,
      pesel: formData.value.pesel?.trim() || undefined,
      nip: formData.value.nip?.trim() || undefined,
      version: formData.value.version
    }

    const response = await customerStore.updateCustomer(submitData)

    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: `Customer ${submitData.firstName} ${submitData.lastName} has been successfully updated.`,
      life: 5000
    })

    // Redirect to the customer detail page
    navigateTo(`/customers/${response.id}`)

  } catch (error: any) {
    // Check if it's a validation error from the server
    if (error.data?.errors) {
      Object.keys(error.data.errors).forEach(field => {
        errors.value[field] = error.data.errors[field]
      })
    }

    console.error('Failed to update customer:', error)
    // Error handling is done in customerStore
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  navigateTo(`/customers/${customerId.value}`)
}

// Auto-validate fields on blur
const handleFieldBlur = (field: keyof UpdateCustomerCommand) => {
  validateField(field)
}
</script>

<style scoped>
.customer-form-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  max-width: 800px;
  margin: 0 auto;
}

/* Page Header */
.page-header {
  text-align: center;
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

.page-title {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

/* Form */
.customer-form {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-8);
}

/* Form Sections */
.form-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.form-section__header {
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--color-border-light);
}

.form-section__header h2 {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.form-section__header p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

/* Form Grid */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.form-grid--single {
  grid-template-columns: 1fr;
}

/* Form Actions */
.form-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .customer-form-page {
    max-width: none;
    margin: 0;
  }

  .page-title {
    font-size: var(--font-size-2xl);
  }

  .customer-form {
    padding: var(--space-4);
    border: none;
    border-radius: 0;
  }

  .form {
    gap: var(--space-6);
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: var(--space-3);
  }

  .form-actions {
    flex-direction: column;
    gap: var(--space-3);
  }

  .form-actions button {
    width: 100%;
  }
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .form-grid {
    gap: var(--space-3);
  }
}
</style>
