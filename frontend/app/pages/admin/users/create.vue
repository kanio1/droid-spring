<template>
  <div class="create-user-page">
    <div class="create-user-page__header">
      <button @click="handleBack" class="back-button">
        ← Back to Users
      </button>
      <h1>Create New User</h1>
    </div>

    <div class="create-user-page__content">
      <UserForm
        :is-edit="false"
        @submit="handleSubmit"
        @cancel="handleBack"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserManagement } from '~/app/composables/useUserManagement'

definePageMeta({
  layout: 'default',
  middleware: 'auth'
})

const { createUser } = useUserManagement()
const router = useRouter()

const handleSubmit = async (userData: any) => {
  try {
    const user = await createUser(userData)
    navigateTo(`/admin/users/${user.id}/edit`)
  } catch (error) {
    console.error('Failed to create user:', error)
  }
}

const handleBack = () => {
  router.push('/admin/users')
}
</script>

<style scoped>
.create-user-page {
  @apply p-6 max-w-4xl mx-auto;
}

.create-user-page__header {
  @apply mb-6;
}

.back-button {
  @apply text-blue-600 hover:text-blue-800 mb-2;
}

.create-user-page__header h1 {
  @apply text-3xl font-bold text-gray-900;
}

.create-user-page__content {
  @apply bg-white rounded-lg shadow p-6;
}
</style>
