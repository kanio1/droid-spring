<template>
  <form @submit.prevent="handleSubmit" class="user-form">
    <div class="user-form__header">
      <h2>{{ isEdit ? 'Edit User' : 'Create User' }}</h2>
    </div>

    <div class="user-form__body">
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
        v-if="!isEdit"
        v-model="formData.keycloakId"
        label="Keycloak ID"
        :required="true"
        :error="errors.keycloakId"
        placeholder="Enter Keycloak ID"
      />

      <AppSelect
        v-if="isEdit"
        v-model="formData.status"
        label="Status"
        :options="statusOptions"
        :error="errors.status"
      />

      <div v-if="isEdit" class="user-form__roles">
        <label class="user-form__label">Roles</label>
        <div class="user-form__roles-list">
          <label
            v-for="role in availableRoles"
            :key="role"
            class="user-form__role-item"
          >
            <input
              type="checkbox"
              :value="role"
              v-model="formData.roles"
            />
            <span>{{ role }}</span>
          </label>
        </div>
      </div>
    </div>

    <div class="user-form__actions">
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
        {{ isEdit ? 'Update User' : 'Create User' }}
      </AppButton>
    </div>
  </form>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { User, UserStatus } from '~/app/composables/useUserManagement'

interface Props {
  user?: User | null
  isEdit?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isEdit: false
})

const emit = defineEmits<{
  submit: [data: any]
  cancel: []
}>()

const { getAvailableRoles } = useUserManagement()
const loading = ref(false)
const availableRoles = ref<string[]>([])

const formData = reactive({
  firstName: '',
  lastName: '',
  email: '',
  keycloakId: '',
  status: 'ACTIVE' as UserStatus,
  roles: [] as string[]
})

const errors = reactive({
  firstName: '',
  lastName: '',
  email: '',
  keycloakId: '',
  status: ''
})

const statusOptions = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Suspended', value: 'SUSPENDED' },
  { label: 'Pending Verification', value: 'PENDING_VERIFICATION' }
]

onMounted(async () => {
  if (props.isEdit && props.user) {
    formData.firstName = props.user.firstName
    formData.lastName = props.user.lastName
    formData.email = props.user.email
    formData.status = props.user.status
    formData.roles = [...props.user.roles]
  }

  try {
    availableRoles.value = await getAvailableRoles()
  } catch (error) {
    console.error('Failed to load roles:', error)
  }
})

const validateForm = (): boolean => {
  let isValid = true

  // Reset errors
  Object.keys(errors).forEach(key => {
    errors[key as keyof typeof errors] = ''
  })

  if (!formData.firstName.trim()) {
    errors.firstName = 'First name is required'
    isValid = false
  }

  if (!formData.lastName.trim()) {
    errors.lastName = 'Last name is required'
    isValid = false
  }

  if (!formData.email.trim()) {
    errors.email = 'Email is required'
    isValid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
    errors.email = 'Email must be valid'
    isValid = false
  }

  if (!props.isEdit && !formData.keycloakId.trim()) {
    errors.keycloakId = 'Keycloak ID is required'
    isValid = false
  }

  return isValid
}

const handleSubmit = () => {
  if (!validateForm()) {
    return
  }

  const submitData = props.isEdit
    ? {
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        status: formData.status,
        roles: formData.roles
      }
    : {
        firstName: formData.firstName,
        lastName: formData.lastName,
        email: formData.email,
        keycloakId: formData.keycloakId
      }

  emit('submit', submitData)
}

const handleCancel = () => {
  emit('cancel')
}
</script>

<style scoped>
.user-form {
  @apply bg-white rounded-lg shadow-md p-6;
}

.user-form__header {
  @apply mb-6;
}

.user-form__header h2 {
  @apply text-2xl font-bold text-gray-800;
}

.user-form__body {
  @apply space-y-4;
}

.user-form__roles {
  @apply space-y-2;
}

.user-form__label {
  @apply block text-sm font-medium text-gray-700;
}

.user-form__roles-list {
  @apply grid grid-cols-2 gap-2 mt-2;
}

.user-form__role-item {
  @apply flex items-center space-x-2 p-2 border rounded hover:bg-gray-50;
}

.user-form__actions {
  @apply flex justify-end space-x-3 mt-6;
}
</style>
