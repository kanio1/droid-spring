<template>
  <AppTable
    :columns="tableColumns"
    :data="data"
    :loading="loading"
    :show-pagination="showPagination"
    :pagination="pagination"
    :clickable="clickable"
    :max-height="maxHeight"
    @page="handlePage"
    @sort="handleSort"
    @row-click="handleRowClick"
  >
    <!-- Record ID column -->
    <template v-if="hasColumn('id')" #cell-id="{ row }">
      <div class="record-id-cell">
        <span class="record-id-text">{{ getShortId(row.id) }}</span>
      </div>
    </template>

    <!-- Usage Type column -->
    <template v-if="hasColumn('usageType')" #cell-usageType="{ row }">
      <StatusBadge :status="row.usageType" type="usage-type" size="small" />
    </template>

    <!-- Usage Amount column -->
    <template v-if="hasColumn('usageAmount')" #cell-usageAmount="{ row }">
      <div class="usage-amount-cell">
        <span class="usage-amount-value">
          {{ formatUsageAmount(row.usageAmount, row.unit) }}
        </span>
      </div>
    </template>

    <!-- Customer column -->
    <template v-if="hasColumn('customerId')" #cell-customerId="{ row }">
      <div class="customer-cell">
        <div class="customer-avatar">
          {{ getCustomerInitials(row.customerId) }}
        </div>
        <div class="customer-info">
          <div class="customer-id-text">{{ getShortId(row.customerId) }}</div>
        </div>
      </div>
    </template>

    <!-- Rating Status column -->
    <template v-if="hasColumn('isRated')" #cell-isRated="{ row }">
      <StatusBadge :status="row.isRated ? 'RATED' : 'PENDING'" type="rating" size="small" />
    </template>

    <!-- Cost column -->
    <template v-if="hasColumn('cost')" #cell-cost="{ row }">
      <div class="cost-cell">
        <span v-if="row.cost !== null && row.cost !== undefined" class="cost-value">
          {{ formatCurrency(row.cost, row.currency) }}
        </span>
        <span v-else class="cost-empty">—</span>
      </div>
    </template>

    <!-- Source column -->
    <template v-if="hasColumn('source')" #cell-source="{ row }">
      <div class="source-cell">
        <span v-if="row.source" class="source-value">
          {{ row.source }}
        </span>
        <span v-else class="source-empty">—</span>
      </div>
    </template>

    <!-- Destination column -->
    <template v-if="hasColumn('destination')" #cell-destination="{ row }">
      <div class="destination-cell">
        <span v-if="row.destination" class="destination-value">
          {{ row.destination }}
        </span>
        <span v-else class="destination-empty">—</span>
      </div>
    </template>

    <!-- Timestamp column -->
    <template v-if="hasColumn('timestamp')" #cell-timestamp="{ row }">
      <div class="timestamp-cell">
        <span v-if="row.timestamp" class="timestamp-value">
          {{ formatDateTime(row.timestamp) }}
        </span>
        <span v-else class="timestamp-empty">—</span>
      </div>
    </template>

    <!-- Actions column -->
    <template v-if="hasColumn('actions')" #cell-actions="{ row }">
      <div class="actions-cell">
        <Button
          icon="pi pi-eye"
          text
          rounded
          @click.stop="handleView(row)"
          v-tooltip.top="'View details'"
        />
        <Button
          icon="pi pi-pencil"
          text
          rounded
          @click.stop="handleEdit(row)"
          v-tooltip.top="'Edit record'"
          v-if="canEditRecord(row)"
        />
        <Button
          icon="pi pi-star"
          text
          rounded
          severity="warning"
          @click.stop="handleRate(row)"
          v-tooltip.top="'Mark as rated'"
          v-if="!row.isRated && showRateAction"
        />
        <Button
          icon="pi pi-trash"
          text
          rounded
          severity="danger"
          @click.stop="handleDelete(row)"
          v-tooltip.top="'Delete record'"
          v-if="showDeleteAction"
        />
      </div>
    </template>

    <!-- Default slot for custom columns -->
    <template v-for="(definition, key) in customColumns" #[`cell-${key}`]="{ row }" :key="key">
      <slot :name="`cell-${key}`" :row="row" />
    </template>

    <!-- Empty state -->
    <template #empty>
      <div class="empty-state">
        <i class="pi pi-database empty-state-icon"></i>
        <h3 class="empty-state-title">No usage records found</h3>
        <p class="empty-state-description">
          {{ emptyStateMessage }}
        </p>
      </div>
    </template>
  </AppTable>
</template>

<script setup lang="ts">
import AppTable from './AppTable.vue'
import { formatUsageAmount, type UsageRecord } from '~/schemas/billing'

// Props
interface Props {
  data: UsageRecord[]
  loading?: boolean
  showPagination?: boolean
  pagination?: any
  clickable?: boolean
  maxHeight?: string
  showRateAction?: boolean
  showDeleteAction?: boolean
  emptyStateMessage?: string
  customColumns?: Record<string, any>
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  showPagination: true,
  pagination: undefined,
  clickable: true,
  maxHeight: undefined,
  showRateAction: true,
  showDeleteAction: true,
  emptyStateMessage: 'No usage records to display',
  customColumns: () => ({})
})

// Emits
const emit = defineEmits<{
  page: [payload: { page: number; rows: number }]
  sort: [payload: { field: string; order: number }]
  rowClick: [row: UsageRecord]
  view: [row: UsageRecord]
  edit: [row: UsageRecord]
  rate: [row: UsageRecord]
  delete: [row: UsageRecord]
}>()

// Default table columns
const defaultColumns = [
  {
    key: 'id',
    label: 'Record ID',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'usageType',
    label: 'Type',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'usageAmount',
    label: 'Usage',
    sortable: true,
    style: 'width: 12%',
    align: 'right' as const
  },
  {
    key: 'customerId',
    label: 'Customer',
    sortable: false,
    style: 'width: 15%'
  },
  {
    key: 'isRated',
    label: 'Rating',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'cost',
    label: 'Cost',
    sortable: true,
    style: 'width: 12%',
    align: 'right' as const
  },
  {
    key: 'source',
    label: 'Source',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'destination',
    label: 'Destination',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'timestamp',
    label: 'Timestamp',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 10%'
  }
]

// Computed
const tableColumns = computed(() => {
  // Merge default columns with custom columns
  const columns = [...defaultColumns]

  // Add custom columns
  Object.entries(props.customColumns).forEach(([key, definition]) => {
    columns.push({
      key,
      ...definition
    })
  })

  return columns
})

// Methods
const hasColumn = (key: string): boolean => {
  return tableColumns.value.some(col => col.key === key)
}

const handlePage = (payload: { page: number; rows: number }) => {
  emit('page', payload)
}

const handleSort = (payload: { field: string; order: number }) => {
  emit('sort', payload)
}

const handleRowClick = (row: UsageRecord) => {
  if (props.clickable) {
    emit('rowClick', row)
  }
}

const handleView = (row: UsageRecord) => {
  emit('view', row)
}

const handleEdit = (row: UsageRecord) => {
  emit('edit', row)
}

const handleRate = (row: UsageRecord) => {
  emit('rate', row)
}

const handleDelete = (row: UsageRecord) => {
  emit('delete', row)
}

const canEditRecord = (row: UsageRecord): boolean => {
  return !row.isRated
}

// Utility functions
const getShortId = (id: string): string => {
  return id.substring(0, 8)
}

const getCustomerInitials = (customerId: string): string => {
  return customerId.substring(0, 2).toUpperCase()
}

const formatDateTime = (dateString: string): string => {
  return new Date(dateString).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatCurrency = (amount: number, currency: string = 'USD'): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2
  }).format(amount)
}
</script>

<style scoped>
/* Record ID Cell */
.record-id-cell {
  font-family: monospace;
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.record-id-text {
  font-size: var(--font-size-xs);
}

/* Usage Amount Cell */
.usage-amount-cell {
  text-align: right;
}

.usage-amount-value {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* Customer Cell */
.customer-cell {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.customer-avatar {
  width: 40px;
  height: 40px;
  background: var(--color-primary);
  color: white;
  border-radius: var(--radius-full);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  flex-shrink: 0;
}

.customer-info {
  flex: 1;
  min-width: 0;
}

.customer-id-text {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-family: monospace;
}

/* Cost Cell */
.cost-cell {
  text-align: right;
}

.cost-value {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.cost-empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Source & Destination Cells */
.source-cell,
.destination-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-value,
.destination-value {
  color: var(--color-text-primary);
}

.source-empty,
.destination-empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Timestamp Cell */
.timestamp-cell {
  max-width: 180px;
}

.timestamp-value {
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
}

.timestamp-empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Actions Cell */
.actions-cell {
  display: flex;
  gap: var(--space-1);
  align-items: center;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-12);
  text-align: center;
}

.empty-state-icon {
  font-size: 4rem;
  margin-bottom: var(--space-4);
  opacity: 0.5;
  color: var(--color-text-muted);
}

.empty-state-title {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.empty-state-description {
  margin: 0;
  color: var(--color-text-secondary);
  max-width: 400px;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .customer-cell {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }

  .customer-avatar {
    width: 32px;
    height: 32px;
  }

  .actions-cell {
    flex-wrap: wrap;
  }

  .source-cell,
  .destination-cell {
    max-width: 120px;
  }

  .timestamp-cell {
    max-width: 120px;
  }
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .customer-cell {
    gap: var(--space-2);
  }

  .customer-avatar {
    width: 36px;
    height: 36px;
  }

  .source-cell,
  .destination-cell,
  .timestamp-cell {
    max-width: 150px;
  }
}
</style>
