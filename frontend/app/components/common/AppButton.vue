<template>
  <component
    :is="tag"
    :type="tag === 'button' ? type : undefined"
    :disabled="disabled"
    :class="buttonClasses"
    @click="handleClick"
  >
    <span v-if="loading" class="button__spinner" aria-hidden="true"></span>
    <span v-if="icon && !loading" class="button__icon" :class="{ 'button__icon--left': iconPosition === 'left' }">
      {{ icon }}
    </span>
    <span v-if="$slots.default" class="button__text">
      <slot />
    </span>
    <span v-if="icon && iconPosition === 'right'" class="button__icon button__icon--right">
      {{ icon }}
    </span>
  </component>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  disabled?: boolean
  loading?: boolean
  type?: 'button' | 'submit' | 'reset'
  tag?: 'button' | 'a' | 'NuxtLink'
  href?: string
  icon?: string
  iconPosition?: 'left' | 'right'
  fullWidth?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  disabled: false,
  loading: false,
  type: 'button',
  tag: 'button',
  iconPosition: 'left',
  fullWidth: false
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const buttonClasses = computed(() => {
  return [
    'app-button',
    `app-button--${props.variant}`,
    `app-button--${props.size}`,
    {
      'app-button--disabled': props.disabled,
      'app-button--loading': props.loading,
      'app-button--full-width': props.fullWidth
    }
  ]
})

const handleClick = (event: MouseEvent) => {
  if (props.disabled || props.loading) {
    event.preventDefault()
    return
  }
  emit('click', event)
}
</script>

<style scoped>
.app-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  font-family: var(--font-family-base);
  font-weight: var(--font-weight-medium);
  text-decoration: none;
  cursor: pointer;
  transition: all var(--transition-fast) var(--transition-timing);
  position: relative;
  white-space: nowrap;
  outline: none;
  user-select: none;
}

.app-button:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

/* Variants */
.app-button--primary {
  background: var(--color-primary);
  color: var(--color-text-primary);
  border-color: var(--color-primary);
}

.app-button--primary:hover:not(.app-button--disabled):not(.app-button--loading) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  transform: translateY(-1px);
}

.app-button--secondary {
  background: var(--color-surface);
  color: var(--color-text-primary);
  border-color: var(--color-border);
}

.app-button--secondary:hover:not(.app-button--disabled):not(.app-button--loading) {
  background: var(--color-surface-alt);
  border-color: var(--color-border-light);
  transform: translateY(-1px);
}

.app-button--success {
  background: var(--color-success);
  color: var(--color-text-primary);
  border-color: var(--color-success);
}

.app-button--success:hover:not(.app-button--disabled):not(.app-button--loading) {
  background: var(--color-success-hover);
  border-color: var(--color-success-hover);
  transform: translateY(-1px);
}

.app-button--danger {
  background: var(--color-danger);
  color: var(--color-text-primary);
  border-color: var(--color-danger);
}

.app-button--danger:hover:not(.app-button--disabled):not(.app-button--loading) {
  background: var(--color-danger-hover);
  border-color: var(--color-danger-hover);
  transform: translateY(-1px);
}

.app-button--warning {
  background: var(--color-warning);
  color: var(--color-text-primary);
  border-color: var(--color-warning);
}

.app-button--warning:hover:not(.app-button--disabled):not(.app-button--loading) {
  background: var(--color-warning-hover);
  border-color: var(--color-warning-hover);
  transform: translateY(-1px);
}

.app-button--ghost {
  background: transparent;
  color: var(--color-text-secondary);
  border-color: transparent;
}

.app-button--ghost:hover:not(.app-button--disabled):not(.app-button--loading) {
  background: var(--color-surface-alt);
  color: var(--color-text-primary);
}

/* Sizes */
.app-button--sm {
  height: var(--space-8);
  padding: 0 var(--space-3);
  font-size: var(--font-size-sm);
}

.app-button--md {
  height: var(--button-height);
  padding: 0 var(--space-4);
  font-size: var(--font-size-base);
}

.app-button--lg {
  height: var(--space-12);
  padding: 0 var(--space-6);
  font-size: var(--font-size-lg);
}

/* States */
.app-button--disabled,
.app-button--loading {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

.app-button--disabled:hover,
.app-button--loading:hover {
  transform: none !important;
}

/* Full width */
.app-button--full-width {
  width: 100%;
}

/* Icon styles */
.button__icon {
  display: flex;
  align-items: center;
  font-size: 1.1em;
}

.button__icon--left {
  margin-right: var(--space-1);
}

.button__icon--right {
  margin-left: var(--space-1);
}

/* Loading spinner */
.button__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Mobile adjustments */
@media (max-width: 640px) {
  .app-button--sm {
    height: var(--space-9);
  }
  
  .app-button--md {
    height: var(--space-10);
  }
  
  .app-button--lg {
    height: var(--space-12);
  }
}
</style>
