<template>
  <div
    :class="cardClass"
    :style="customStyle"
  >
    <!-- Header -->
    <div v-if="title || $slots.header" class="card__header">
      <div class="card__header-content">
        <slot name="header">
          <h3 v-if="title" class="card__title">
            {{ title }}
          </h3>
        </slot>
      </div>
      <div v-if="$slots['header-actions']" class="card__header-actions">
        <slot name="header-actions" />
      </div>
    </div>

    <!-- Body -->
    <div class="card__body">
      <slot />
    </div>

    <!-- Footer -->
    <div v-if="$slots.footer" class="card__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title?: string
  variant?: 'default' | 'bordered' | 'elevated' | 'flat'
  padding?: 'none' | 'small' | 'normal' | 'large'
  hoverable?: boolean
  clickable?: boolean
  customStyle?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
  padding: 'normal',
  hoverable: false,
  clickable: false
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const cardClass = computed(() => ({
  'card': true,
  'card--default': props.variant === 'default',
  'card--bordered': props.variant === 'bordered',
  'card--elevated': props.variant === 'elevated',
  'card--flat': props.variant === 'flat',
  'card--hoverable': props.hoverable,
  'card--clickable': props.clickable,
  'card--padding-none': props.padding === 'none',
  'card--padding-small': props.padding === 'small',
  'card--padding-large': props.padding === 'large'
}))

const handleClick = (event: MouseEvent) => {
  if (props.clickable) {
    emit('click', event)
  }
}
</script>

<style scoped>
.card {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  width: 100%;
  transition: all var(--transition-base) var(--transition-timing);
}

/* Variants */
.card--default {
  border: 1px solid var(--color-border);
}

.card--bordered {
  border: 2px solid var(--color-border);
}

.card--elevated {
  box-shadow: var(--shadow-md);
  border: none;
}

.card--flat {
  border: none;
  box-shadow: none;
  background: transparent;
}

/* Interactive states */
.card--hoverable:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.card--clickable {
  cursor: pointer;
}

.card--clickable:hover {
  transform: translateY(-1px);
}

/* Padding */
.card--padding-none .card__body {
  padding: 0;
}

.card--padding-small .card__body {
  padding: var(--space-4);
}

.card__body {
  padding: var(--space-6);
}

.card--padding-large .card__body {
  padding: var(--space-8);
}

/* Header */
.card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-6);
  border-bottom: 1px solid var(--color-border);
}

.card--padding-none .card__header {
  padding: var(--space-6) var(--space-6) 0;
}

.card--padding-small .card__header {
  padding: var(--space-4) var(--space-4) 0;
}

.card__header-content {
  flex: 1;
}

.card__header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.card__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

/* Footer */
.card__footer {
  padding: var(--space-6);
  border-top: 1px solid var(--color-border);
  margin-top: auto;
}

.card--padding-none .card__footer {
  padding: 0 var(--space-6) var(--space-6);
}

.card--padding-small .card__footer {
  padding: 0 var(--space-4) var(--space-4);
}

/* Responsive */
@media (max-width: 640px) {
  .card__body {
    padding: var(--space-4);
  }

  .card__header {
    padding: var(--space-4);
  }

  .card__footer {
    padding: var(--space-4);
  }

  .card--hoverable:hover {
    transform: none;
  }

  .card--clickable:hover {
    transform: none;
  }
}
</style>
