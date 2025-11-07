<template>
  <span
    :class="badgeClass"
    :style="customStyle"
  >
    <Icon
      v-if="icon && iconPosition === 'left'"
      :name="icon"
      :size="iconSize"
      class="badge__icon badge__icon--left"
    />
    <slot>{{ text }}</slot>
    <Icon
      v-if="icon && iconPosition === 'right'"
      :name="icon"
      :size="iconSize"
      class="badge__icon badge__icon--right"
    />
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  variant?: 'primary' | 'secondary' | 'success' | 'warning' | 'danger' | 'info' | 'neutral'
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  text?: string
  icon?: string
  iconPosition?: 'left' | 'right'
  dot?: boolean
  outlined?: boolean
  rounded?: boolean
  customStyle?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'primary',
  size: 'md',
  iconPosition: 'left',
  dot: false,
  outlined: false,
  rounded: false
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const badgeClass = computed(() => ({
  'badge': true,
  'badge--primary': props.variant === 'primary',
  'badge--secondary': props.variant === 'secondary',
  'badge--success': props.variant === 'success',
  'badge--warning': props.variant === 'warning',
  'badge--danger': props.variant === 'danger',
  'badge--info': props.variant === 'info',
  'badge--neutral': props.variant === 'neutral',
  'badge--xs': props.size === 'xs',
  'badge--sm': props.size === 'sm',
  'badge--md': props.size === 'md',
  'badge--lg': props.size === 'lg',
  'badge--xl': props.size === 'xl',
  'badge--outlined': props.outlined,
  'badge--rounded': props.rounded,
  'badge--dot': props.dot,
  'badge--clickable': Boolean($attrs.onClick)
}))

const iconSize = computed(() => {
  const sizes: Record<string, string> = {
    xs: '10',
    sm: '12',
    md: '14',
    lg: '16',
    xl: '18'
  }
  return sizes[props.size]
})

const handleClick = (event: MouseEvent) => {
  if ($attrs.onClick) {
    emit('click', event)
  }
}
</script>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  font-weight: var(--font-weight-medium);
  line-height: 1;
  white-space: nowrap;
  user-select: none;
  transition: all var(--transition-fast) var(--transition-timing);
  border: 1px solid transparent;
}

/* Sizes */
.badge--xs {
  padding: 0.125rem 0.375rem;
  font-size: 0.625rem;
  min-height: 16px;
}

.badge--sm {
  padding: 0.1875rem 0.5rem;
  font-size: 0.6875rem;
  min-height: 20px;
}

.badge--md {
  padding: 0.25rem 0.625rem;
  font-size: 0.75rem;
  min-height: 24px;
}

.badge--lg {
  padding: 0.375rem 0.75rem;
  font-size: 0.8125rem;
  min-height: 28px;
}

.badge--xl {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  min-height: 32px;
}

/* Rounded variant */
.badge--rounded {
  border-radius: var(--radius-full);
}

/* Dot variant */
.badge--dot {
  border-radius: 50%;
  padding: 0.375rem;
  width: 8px;
  height: 8px;
  min-height: 8px;
  min-width: 8px;
}

/* Variants - Solid */
.badge--primary {
  background: var(--color-primary);
  color: var(--color-text-primary);
}

.badge--secondary {
  background: var(--color-secondary);
  color: var(--color-text-primary);
}

.badge--success {
  background: var(--color-success);
  color: var(--color-text-primary);
}

.badge--warning {
  background: var(--color-warning);
  color: var(--color-text-primary);
}

.badge--danger {
  background: var(--color-danger);
  color: var(--color-text-primary);
}

.badge--info {
  background: var(--color-accent);
  color: var(--color-text-primary);
}

.badge--neutral {
  background: var(--color-surface-alt);
  color: var(--color-text-secondary);
}

/* Variants - Outlined */
.badge--outlined.badge--primary {
  background: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.badge--outlined.badge--secondary {
  background: transparent;
  border-color: var(--color-secondary);
  color: var(--color-secondary);
}

.badge--outlined.badge--success {
  background: transparent;
  border-color: var(--color-success);
  color: var(--color-success);
}

.badge--outlined.badge--warning {
  background: transparent;
  border-color: var(--color-warning);
  color: var(--color-warning);
}

.badge--outlined.badge--danger {
  background: transparent;
  border-color: var(--color-danger);
  color: var(--color-danger);
}

.badge--outlined.badge--info {
  background: transparent;
  border-color: var(--color-accent);
  color: var(--color-accent);
}

.badge--outlined.badge--neutral {
  background: transparent;
  border-color: var(--color-border);
  color: var(--color-text-secondary);
}

/* Clickable */
.badge--clickable {
  cursor: pointer;
}

.badge--clickable:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.badge--clickable:active {
  transform: translateY(0);
}

/* Icon spacing */
.badge__icon {
  display: inline-flex;
}

.badge--dot .badge__icon {
  margin: 0;
}
</style>
