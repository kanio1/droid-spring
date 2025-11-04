<template>
  <Button
    :label="label"
    :icon="icon"
    :severity="severity"
    :variant="variant"
    :size="size"
    :disabled="disabled"
    :loading="loading"
    :outlined="outlined"
    :text="text"
    :rounded="rounded"
    :class="buttonClass"
    @click="onClick"
  >
    <template v-if="$slots.icon" #icon>
      <slot name="icon"></slot>
    </template>
  </Button>
</template>

<script setup lang="ts">
import Button from 'primevue/button'

interface Props {
  label?: string
  icon?: string
  severity?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'danger' | 'help'
  variant?: 'outlined' | 'text' | 'filled'
  size?: 'small' | 'normal' | 'large'
  disabled?: boolean
  loading?: boolean
  outlined?: boolean
  text?: boolean
  rounded?: boolean
  fullWidth?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  severity: 'primary',
  variant: 'filled',
  size: 'normal',
  rounded: true
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const buttonClass = computed(() => ({
  'w-full': props.fullWidth,
  'app-button--small': props.size === 'small',
  'app-button--large': props.size === 'large'
}))

const onClick = (event: MouseEvent) => {
  emit('click', event)
}
</script>

<style scoped>
.app-button--small {
  font-size: 0.875rem;
  padding: 0.375rem 0.75rem;
}

.app-button--large {
  font-size: 1.125rem;
  padding: 0.75rem 1.5rem;
}

:deep(.p-button) {
  font-family: var(--font-family-base);
  font-weight: var(--font-weight-medium);
  transition: all var(--transition-fast) var(--transition-timing);
}

:deep(.p-button.p-button-primary) {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

:deep(.p-button.p-button-primary:hover) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

:deep(.p-button.p-button-success) {
  background: var(--color-accent);
  border-color: var(--color-accent);
  color: white;
}

:deep(.p-button.p-button-success:hover) {
  background: var(--color-accent-hover);
  border-color: var(--color-accent-hover);
}

:deep(.p-button.p-button-warning) {
  background: var(--color-warning);
  border-color: var(--color-warning);
  color: white;
}

:deep(.p-button.p-button-warning:hover) {
  background: var(--color-warning-hover);
  border-color: var(--color-warning-hover);
}

:deep(.p-button.p-button-danger) {
  background: var(--color-danger);
  border-color: var(--color-danger);
  color: white;
}

:deep(.p-button.p-button-danger:hover) {
  background: var(--color-danger-hover);
  border-color: var(--color-danger-hover);
}
</style>
