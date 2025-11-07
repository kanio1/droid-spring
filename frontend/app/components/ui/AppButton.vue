<template>
  <component
    :is="component"
    v-bind="componentProps"
    :class="buttonClass"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <Icon
      v-if="loading"
      name="lucide:loader-2"
      :size="iconSize"
      class="btn__icon btn__icon--spinner"
    />
    <Icon
      v-else-if="icon && iconPosition === 'left'"
      :name="icon"
      :size="iconSize"
      class="btn__icon btn__icon--left"
    />
    <span v-if="$slots.default || label" class="btn__text">
      <slot>{{ label }}</slot>
    </span>
    <Icon
      v-if="!loading && icon && iconPosition === 'right'"
      :name="icon"
      :size="iconSize"
      class="btn__icon btn__icon--right"
    />
  </component>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'success' | 'warning'
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  label?: string
  icon?: string
  iconPosition?: 'left' | 'right'
  disabled?: boolean
  loading?: boolean
  fullWidth?: boolean
  rounded?: boolean
  type?: 'button' | 'submit' | 'reset'
  href?: string
  to?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  iconPosition: 'left',
  disabled: false,
  loading: false,
  fullWidth: false,
  rounded: true,
  type: 'button'
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const component = computed(() => {
  if (props.to) return 'NuxtLink'
  if (props.href) return 'a'
  return 'button'
})

const componentProps = computed(() => {
  const base = {
    type: props.type,
    class: props.class
  }

  if (props.to) {
    return { ...base, to: props.to }
  }

  if (props.href) {
    return { ...base, href: props.href, target: '_blank', rel: 'noopener noreferrer' }
  }

  return base
})

const buttonClass = computed(() => ({
  'btn': true,
  'btn--primary': props.variant === 'primary',
  'btn--secondary': props.variant === 'secondary',
  'btn--outline': props.variant === 'outline',
  'btn--ghost': props.variant === 'ghost',
  'btn--danger': props.variant === 'danger',
  'btn--success': props.variant === 'success',
  'btn--warning': props.variant === 'warning',
  'btn--xs': props.size === 'xs',
  'btn--sm': props.size === 'sm',
  'btn--md': props.size === 'md',
  'btn--lg': props.size === 'lg',
  'btn--xl': props.size === 'xl',
  'btn--full-width': props.fullWidth,
  'btn--rounded': props.rounded,
  'btn--disabled': props.disabled,
  'btn--loading': props.loading
}))

const iconSize = computed(() => {
  const sizes: Record<string, string> = {
    xs: '12',
    sm: '14',
    md: '16',
    lg: '18',
    xl: '20'
  }
  return sizes[props.size]
})

const handleClick = (event: MouseEvent) => {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}
</script>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  font-family: var(--font-family-base);
  font-weight: var(--font-weight-medium);
  text-align: center;
  text-decoration: none;
  white-space: nowrap;
  vertical-align: middle;
  cursor: pointer;
  user-select: none;
  border: 1px solid transparent;
  transition: all var(--transition-fast) var(--transition-timing);
  outline: none;
  position: relative;
}

.btn:focus-visible {
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

/* Full width */
.btn--full-width {
  width: 100%;
}

/* Disabled state */
.btn--disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

/* Sizes */
.btn--xs {
  padding: 0.25rem 0.5rem;
  font-size: var(--font-size-xs);
  min-height: 24px;
}

.btn--sm {
  padding: 0.375rem 0.75rem;
  font-size: var(--font-size-sm);
  min-height: 32px;
}

.btn--md {
  padding: 0.5rem 1rem;
  font-size: var(--font-size-sm);
  min-height: 40px;
}

.btn--lg {
  padding: 0.625rem 1.25rem;
  font-size: var(--font-size-base);
  min-height: 48px;
}

.btn--xl {
  padding: 0.75rem 1.5rem;
  font-size: var(--font-size-lg);
  min-height: 56px;
}

/* Rounded */
.btn--rounded {
  border-radius: var(--radius-full);
}

/* Variants - Primary */
.btn--primary {
  background: var(--color-primary);
  color: var(--color-text-primary);
  border-color: var(--color-primary);
}

.btn--primary:hover:not(.btn--disabled) {
  background: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* Variants - Secondary */
.btn--secondary {
  background: var(--color-secondary);
  color: var(--color-text-primary);
  border-color: var(--color-secondary);
}

.btn--secondary:hover:not(.btn--disabled) {
  background: var(--color-secondary-hover);
  border-color: var(--color-secondary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* Variants - Success */
.btn--success {
  background: var(--color-success);
  color: var(--color-text-primary);
  border-color: var(--color-success);
}

.btn--success:hover:not(.btn--disabled) {
  background: var(--color-success-hover);
  border-color: var(--color-success-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* Variants - Warning */
.btn--warning {
  background: var(--color-warning);
  color: var(--color-text-primary);
  border-color: var(--color-warning);
}

.btn--warning:hover:not(.btn--disabled) {
  background: var(--color-warning-hover);
  border-color: var(--color-warning-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* Variants - Danger */
.btn--danger {
  background: var(--color-danger);
  color: var(--color-text-light);
  border-color: var(--color-danger);
}

.btn--danger:hover:not(.btn--disabled) {
  background: var(--color-danger-hover);
  border-color: var(--color-danger-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

/* Variants - Outline */
.btn--outline {
  background: transparent;
  color: var(--color-text-primary);
  border-color: var(--color-border);
}

.btn--outline:hover:not(.btn--disabled) {
  background: var(--color-surface-alt);
  border-color: var(--color-primary);
  color: var(--color-primary);
  transform: translateY(-1px);
}

/* Variants - Ghost */
.btn--ghost {
  background: transparent;
  color: var(--color-text-secondary);
  border-color: transparent;
}

.btn--ghost:hover:not(.btn--disabled) {
  background: var(--color-surface-alt);
  color: var(--color-text-primary);
  transform: translateY(-1px);
}

/* Loading state */
.btn--loading {
  pointer-events: none;
}

.btn__icon--spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* Icon */
.btn__icon {
  display: inline-flex;
  flex-shrink: 0;
}

.btn__text {
  display: inline-flex;
  align-items: center;
}

/* Active state */
.btn:active:not(.btn--disabled) {
  transform: translateY(0);
}

.btn:active:not(.btn--disabled):not(.btn--ghost):not(.btn--outline) {
  box-shadow: var(--shadow-sm);
}

/* Responsive */
@media (max-width: 640px) {
  .btn--lg {
    min-height: 44px;
    padding: 0.5rem 1rem;
  }

  .btn--xl {
    min-height: 52px;
    padding: 0.625rem 1.25rem;
  }
}
</style>
