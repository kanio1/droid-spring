<template>
  <form @submit.prevent="handleSubmit" class="customer-form">
    <div class="customer-form__header">
      <h2>{{ isEdit ? 'Edit Customer' : 'Create Customer' }}</h2>
    </div>

    <div class="customer-form__body">
      <AppInput
        v-model="formData.firstName"
        label="First Name"
        :required="true"
        :error="errors.firstName"
        placeholder="Enter first name"
      />

      <AppInput
        v-model="formData.lastName"
        label="Last Name"
        :required="true"
        :error="errors.lastName"
        placeholder="Enter last name"
      />

      <AppInput
        v-model="formData.email"
        label="Email"
        type="email"
        :required="true"
        :error="errors.email"
        placeholder="Enter email address"
      />

      <AppInput
        v-model="formData.phone"
        label="Phone"
        :error="errors.phone"
        placeholder="Enter phone number (e.g., +48123456789)"
      />

      <AppInput
        v-model="formData.pesel"
        label="PESEL"
        :error="errors.pesel"
        placeholder="Enter 11-digit PESEL"
        @blur="validatePeselField"
      />

      <AppInput
        v-model="formData.nip"
        label="NIP"
        :error="errors.nip"
        placeholder="Enter 10-digit NIP"
        @blur="validateNipField"
      />

      <AppSelect
        v-if="isEdit"
        v-model="formData.status"
        label="Status"
        :options="statusOptions"
        :error="errors.status"
        optionLabel="label"
        optionValue="value"
      />
    </div>

    <div class="customer-form__actions">
      <AppButton
        type="button"
        variant="secondary"
        @click="handleCancel"
      >
        Cancel
      </AppButton>
      <AppButton
        type="submit"
        variant="primary"
        :loading="loading"
      >
        {{ isEdit ? 'Update Customer' : 'Create Customer' }}
      </AppButton>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { validateCreateCustomer, validateUpdateCustomer, type Customer, type CreateCustomerCommand, type UpdateCustomerCommand, type CustomerFormData, CUSTOMER_STATUS_LABELS } from '~/schemas/customer'

interface Props {
  customer?: Customer | null
  isEdit?: boolean
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isEdit: false,
  loading: false
})

const emit = defineEmits<{
  submit: [data: CreateCustomerCommand | UpdateCustomerCommand]
  cancel: []
}>()

const formData = reactive<CustomerFormData>({
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  pesel: '',
  nip: '',
  ...(props.isEdit ? { id: '', version: 0, status: 'ACTIVE' as any } : {})
})

const errors = reactive<Record<string, string>>({})

const statusOptions = Object.entries(CUSTOMER_STATUS_LABELS).map(([value, label]) => ({
  label,
  value
}))

onMounted(() => {
  if (props.customer && props.isEdit) {
    formData.id = props.customer.id
    formData.version = props.customer.version
    formData.status = props.customer.status
    formData.firstName = props.customer.firstName
    formData.lastName = props.customer.lastName
    formData.email = props.customer.email
    formData.phone = props.customer.phone || ''
    formData.pesel = props.customer.pesel || ''
    formData.nip = props.customer.nip || ''
  }
})

const validateForm = (): boolean => {
  Object.keys(errors).forEach(key => delete errors[key])

  try {
    if (props.isEdit) {
      validateUpdateCustomer(formData)
    } else {
      const { id, version, status, ...createData } = formData
      validateCreateCustomer(createData)
    }
    return true
  } catch (error: any) {
    if (error.errors) {
      error.errors.forEach((err: any) => {
        errors[err.path] = err.message
      })
    }
    return false
  }
}

const validatePeselField = () => {
  if (formData.pesel && formData.pesel.length > 0 && formData.pesel.length !== 11) {
    errors.pesel = 'PESEL must be exactly 11 digits'
  } else if (errors.pesel === 'PESEL must be exactly 11 digits') {
    delete errors.pesel
  }
}

const validateNipField = () => {
  if (formData.nip && formData.nip.length > 0 && formData.nip.length !== 10) {
    errors.nip = 'NIP must be exactly 10 digits'
  } else if (errors.nip === 'NIP must be exactly 10 digits') {
    delete errors.nip
  }
}

const handleSubmit = () => {
  if (!validateForm()) {
    return
  }

  if (props.isEdit) {
    const updateData: UpdateCustomerCommand = {
      id: formData.id,
      version: formData.version,
      firstName: formData.firstName,
      lastName: formData.lastName,
      email: formData.email,
      phone: formData.phone,
      pesel: formData.pesel,
      nip: formData.nip
    }
    emit('submit', updateData)
  } else {
    const createData: CreateCustomerCommand = {
      firstName: formData.firstName,
      lastName: formData.lastName,
      email: formData.email,
      phone: formData.phone,
      pesel: formData.pesel,
      nip: formData.nip
    }
    emit('submit', createData)
  }
}

const handleCancel = () => {
  emit('cancel')
}
</script>

<style scoped>
.customer-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.customer-form__header {
  border-bottom: 1px solid var(--color-border);
  padding-bottom: var(--space-4);
}

.customer-form__header h2 {
  margin: 0;
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.customer-form__body {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--space-4);
}

.customer-form__actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

@media (max-width: 768px) {
  .customer-form__body {
    grid-template-columns: 1fr;
  }

  .customer-form__actions {
    flex-direction: column-reverse;
  }

  .customer-form__actions button {
    width: 100%;
  }
}
</style>
