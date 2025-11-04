<template>
  <Tag
    :value="label"
    :severity="variant"
    :class="['status-badge', `status-badge--${variant}`]"
    :style="customStyle"
  />
</template>

<script setup lang="ts">
import Tag from 'primevue/tag'

interface Props {
  status: string
  type?: 'customer' | 'product' | 'order' | 'invoice' | 'payment' | 'subscription'
  size?: 'small' | 'normal' | 'large'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'normal'
})

const getVariant = (status: string, type?: string) => {
  const variants: Record<string, Record<string, string>> = {
    customer: {
      ACTIVE: 'success',
      INACTIVE: 'secondary',
      SUSPENDED: 'warning',
      TERMINATED: 'danger'
    },
    product: {
      ACTIVE: 'success',
      INACTIVE: 'secondary',
      DEPRECATED: 'warning'
    },
    order: {
      PENDING: 'warning',
      CONFIRMED: 'info',
      PROCESSING: 'info',
      COMPLETED: 'success',
      CANCELLED: 'danger'
    },
    invoice: {
      DRAFT: 'secondary',
      ISSUED: 'info',
      SENT: 'info',
      PAID: 'success',
      OVERDUE: 'danger',
      CANCELLED: 'danger'
    },
    payment: {
      PENDING: 'warning',
      PROCESSING: 'info',
      COMPLETED: 'success',
      FAILED: 'danger',
      REFUNDED: 'secondary'
    },
    subscription: {
      ACTIVE: 'success',
      SUSPENDED: 'warning',
      CANCELLED: 'danger',
      EXPIRED: 'secondary'
    }
  }

  return variants[type || 'customer']?.[status] || 'secondary'
}

const getLabel = (status: string, type?: string) => {
  const labels: Record<string, Record<string, string>> = {
    customer: {
      ACTIVE: 'Active',
      INACTIVE: 'Inactive',
      SUSPENDED: 'Suspended',
      TERMINATED: 'Terminated'
    },
    product: {
      ACTIVE: 'Active',
      INACTIVE: 'Inactive',
      DEPRECATED: 'Deprecated'
    },
    order: {
      PENDING: 'Pending',
      CONFIRMED: 'Confirmed',
      PROCESSING: 'Processing',
      COMPLETED: 'Completed',
      CANCELLED: 'Cancelled'
    },
    invoice: {
      DRAFT: 'Draft',
      ISSUED: 'Issued',
      SENT: 'Sent',
      PAID: 'Paid',
      OVERDUE: 'Overdue',
      CANCELLED: 'Cancelled'
    },
    payment: {
      PENDING: 'Pending',
      PROCESSING: 'Processing',
      COMPLETED: 'Completed',
      FAILED: 'Failed',
      REFUNDED: 'Refunded'
    },
    subscription: {
      ACTIVE: 'Active',
      SUSPENDED: 'Suspended',
      CANCELLED: 'Cancelled',
      EXPIRED: 'Expired'
    }
  }

  return labels[type || 'customer']?.[status] || status
}

const variant = computed(() => getVariant(props.status, props.type))
const label = computed(() => getLabel(props.status, props.type))

const customStyle = computed(() => ({
  fontSize: props.size === 'small' ? '0.75rem' : props.size === 'large' ? '1rem' : '0.875rem',
  fontWeight: '500',
  padding: props.size === 'small' ? '0.25rem 0.5rem' : props.size === 'large' ? '0.5rem 1rem' : '0.375rem 0.75rem'
}))
</script>

<style scoped>
.status-badge {
  text-transform: uppercase;
  letter-spacing: 0.05em;
  border-radius: var(--radius-full);
}

.status-badge--success {
  background: var(--color-success-light);
  color: var(--color-accent);
  border: 1px solid var(--color-accent);
}

.status-badge--secondary {
  background: var(--color-surface-alt);
  color: var(--color-text-secondary);
  border: 1px solid var(--color-border);
}

.status-badge--warning {
  background: var(--color-warning-light);
  color: var(--color-warning);
  border: 1px solid var(--color-warning);
}

.status-badge--danger {
  background: var(--color-danger-light);
  color: var(--color-danger);
  border: 1px solid var(--color-danger);
}

.status-badge--info {
  background: var(--color-primary-light);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
}
</style>
