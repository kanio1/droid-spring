<template>
  <component
    :is="tag"
    :class="badgeClasses"
    :aria-label="ariaLabel"
  >
    <span v-if="icon && iconPosition === 'left'" class="badge__icon badge__icon--left">
      {{ icon }}
    </span>
    <span v-if="text || $slots.default" class="badge__text">
      <slot>{{ text }}</slot>
    </span>
    <span v-if="icon && iconPosition === 'right'" class="badge__icon badge__icon--right">
      {{ icon }}
    </span>
  </component>
</template>

<script setup lang="ts">
interface Props {
  variant?: 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'neutral'
  size?: 'xs' | 'sm' | 'md' | 'lg'
  text?: string
  icon?: string
  iconPosition?: 'left' | 'right'
  pill?: boolean
  outline?: boolean
  tag?: 'span' | 'div' | 'button' | 'a'
  href?: string
  ariaLabel?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'neutral',
  size: 'md',
  text: '',
  iconPosition: 'left',
  pill: false,
  outline: false,
  tag: 'span'
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const badgeClasses = computed(() => [
  'app-badge',
  `app-badge--${props.variant}`,
  `app-badge--${props.size}`,
  {
    'app-badge--pill': props.pill,
    'app-badge--outline': props.outline,
    'app-badge--with-icon': props.icon,
    'app-badge--clickable': props.tag === 'button' || props.tag === 'a'
  }
])

const handleClick = (event: MouseEvent) => {
  emit('click', event)
}
</script>

<style scoped>
.app-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-family-base);
  font-weight: var(--font-weight-medium);
  line-height: 1;
  text-decoration: none;
  white-space: nowrap;
  border: 1px solid transparent;
  transition: all var(--transition-fast) var(--transition-timing);
  user-select: none;
}

/* Sizes */
.app-badge--xs {
  padding: 0.125rem 0.375rem;
  font-size: 0.625rem;
  border-radius: var(--radius-sm);
}

.app-badge--sm {
  padding: 0.25rem 0.5rem;
  font-size: var(--font-size-xs);
  border-radius: var(--radius-sm);
}

.app-badge--md {
  padding: var(--space-1) var(--space-3);
  font-size: var(--font-size-xs);
  border-radius: var(--radius-md);
}

.app-badge--lg {
  padding: var(--space-2) var(--space-4);
  font-size: var(--font-size-sm);
  border-radius: var(--radius-md);
}

/* Pill variant */
.app-badge--pill {
  border-radius: var(--radius-full);
}

/* Variants */
.app-badge--primary {
  background: var(--color-primary);
  color: var(--color-text-primary);
  border-color: var(--color-primary);
}

.app-badge--primary.app-badge--outline {
  background: transparent;
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.app-badge--secondary {
  background: var(--color-secondary);
  color: var(--color-text-primary);
  border-color: var(--color-secondary);
}

.app-badge--secondary.app-badge--outline {
  background: transparent;
  color: var(--color-secondary);
  border-color: var(--color-secondary);
}

.app-badge--success {
  background: var(--color-success);
  color: var(--color-text-primary);
  border-color: var(--color-success);
}

.app-badge--success.app-badge--outline {
  background: transparent;
  color: var(--color-success);
  border-color: var(--color-success);
}

.app-badge--danger {
  background: var(--color-danger);
  color: var(--color-text-primary);
  border-color: var(--color-danger);
}

.app-badge--danger.app-badge--outline {
  background: transparent;
  color: var(--color-danger);
  border-color: var(--color-danger);
}

.app-badge--warning {
  background: var(--color-warning);
  color: var(--color-text-primary);
  border-color: var(--color-warning);
}

.app-badge--warning.app-badge--outline {
  background: transparent;
  color: var(--color-warning);
  border-color: var(--color-warning);
}

.app-badge--info {
  background: var(--color-primary-light);
  color: var(--color-text-primary);
  border-color: var(--color-primary);
}

.app-badge--info.app-badge--outline {
  background: transparent;
  color: var(--color-primary);
  border-color: var(--color-primary);
}

.app-badge--neutral {
  background: var(--color-surface-alt);
  color: var(--color-text-secondary);
  border-color: var(--color-border);
}

.app-badge--neutral.app-badge--outline {
  background: transparent;
  color: var(--color-text-secondary);
  border-color: var(--color-border);
}

/* Clickable state */
.app-badge--clickable {
  cursor: pointer;
}

.app-badge--clickable:hover:not(.app-badge--outline) {
  opacity: 0.9;
  transform: translateY(-1px);
}

.app-badge--clickable:hover.app-badge--outline {
  background: currentColor;
  color: var(--color-surface);
}

/* Icon spacing */
.badge__icon {
  display: flex;
  align-items: center;
  font-size: 1.1em;
}

.badge__icon--left {
  margin-right: var(--space-1);
}

.badge__icon--right {
  margin-left: var(--space-1);
}

/* Size adjustments for icon-only badges */
.app-badge--with-icon {
  padding: var(--space-2);
}

.app-badge--with-icon.app-badge--xs {
  padding: 0.25rem;
}

.app-badge--with-icon.app-badge--sm {
  padding: var(--space-1);
}

.app-badge--with-icon.app-badge--md {
  padding: var(--space-2);
}

.app-badge--with-icon.app-badge--lg {
  padding: var(--space-3);
}

/* Mobile adjustments */
@media (max-width: 640px) {
  .app-badge--xs,
  .app-badge--sm {
    font-size: 0.625rem;
    padding: 0.1875rem 0.375rem;
  }
}
</style>
