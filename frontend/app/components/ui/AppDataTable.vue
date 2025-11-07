<template>
  <div class="data-table" :class="tableClass">
    <!-- Table Container -->
    <div class="data-table__container">
      <table class="data-table__table">
        <!-- Header -->
        <thead class="data-table__thead">
          <tr class="data-table__tr">
            <th
              v-for="column in columns"
              :key="column.key"
              :class="getHeaderClass(column)"
              @click="handleSort(column)"
            >
              <div class="data-table__th-content">
                <span class="data-table__th-text">{{ column.label }}</span>
                <Icon
                  v-if="column.sortable"
                  :name="getSortIcon(column.key)"
                  :size="16"
                  class="data-table__sort-icon"
                />
              </div>
            </th>
          </tr>
        </thead>

        <!-- Body -->
        <tbody class="data-table__tbody">
          <tr
            v-for="(row, index) in paginatedData"
            :key="getRowKey(row, index)"
            class="data-table__tr"
            :class="{ 'data-table__tr--clickable': clickable }"
            @click="handleRowClick(row, index)"
          >
            <td
              v-for="column in columns"
              :key="column.key"
              class="data-table__td"
            >
              <slot
                :name="`cell-${column.key}`"
                :value="row[column.key]"
                :row="row"
                :index="index"
              >
                {{ formatCellValue(row[column.key], column) }}
              </slot>
            </td>
          </tr>

          <!-- Empty State -->
          <tr v-if="data.length === 0">
            <td :colspan="columns.length" class="data-table__td data-table__td--empty">
              <div class="data-table__empty">
                <Icon name="lucide:inbox" :size="48" class="data-table__empty-icon" />
                <p class="data-table__empty-text">{{ emptyText }}</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="showPagination && data.length > 0" class="data-table__footer">
      <div class="data-table__info">
        Showing {{ startIndex + 1 }} to {{ Math.min(endIndex, data.length) }} of {{ data.length }} results
      </div>
      <div class="data-table__pagination">
        <button
          class="data-table__page-btn"
          :disabled="currentPage === 1"
          @click="changePage(currentPage - 1)"
        >
          <Icon name="lucide:chevron-left" :size="16" />
        </button>

        <button
          v-for="page in visiblePages"
          :key="page"
          class="data-table__page-btn"
          :class="{ 'data-table__page-btn--active': page === currentPage }"
          @click="changePage(page)"
        >
          {{ page }}
        </button>

        <button
          class="data-table__page-btn"
          :disabled="currentPage === totalPages"
          @click="changePage(currentPage + 1)"
        >
          <Icon name="lucide:chevron-right" :size="16" />
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface Column {
  key: string
  label: string
  sortable?: boolean
  width?: string
  align?: 'left' | 'center' | 'right'
  formatter?: (value: any) => string
}

interface Props {
  data: any[]
  columns: Column[]
  pageSize?: number
  clickable?: boolean
  showPagination?: boolean
  emptyText?: string
  stickyHeader?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  pageSize: 10,
  clickable: false,
  showPagination: true,
  emptyText: 'No data available',
  stickyHeader: true
})

const emit = defineEmits<{
  sort: [key: string, direction: 'asc' | 'desc']
  rowClick: [row: any, index: number]
  pageChange: [page: number]
}>()

const currentPage = ref(1)
const sortKey = ref<string | null>(null)
const sortDirection = ref<'asc' | 'desc'>('asc')

const tableClass = computed(() => ({
  'data-table--sticky': props.stickyHeader
}))

const sortedData = computed(() => {
  if (!sortKey.value) return props.data

  return [...props.data].sort((a, b) => {
    const aVal = a[sortKey.value!]
    const bVal = b[sortKey.value!]

    if (aVal < bVal) return sortDirection.value === 'asc' ? -1 : 1
    if (aVal > bVal) return sortDirection.value === 'asc' ? 1 : -1
    return 0
  })
})

const totalPages = computed(() => {
  return Math.ceil(props.data.length / props.pageSize)
})

const startIndex = computed(() => {
  return (currentPage.value - 1) * props.pageSize
})

const endIndex = computed(() => {
  return startIndex.value + props.pageSize
})

const paginatedData = computed(() => {
  return sortedData.value.slice(startIndex.value, endIndex.value)
})

const visiblePages = computed(() => {
  const pages: number[] = []
  const total = totalPages.value
  const current = currentPage.value

  for (let i = Math.max(1, current - 2); i <= Math.min(total, current + 2); i++) {
    pages.push(i)
  }

  return pages
})

const getRowKey = (row: any, index: number) => {
  return row.id || row.key || index
}

const getHeaderClass = (column: Column) => ({
  'data-table__th': true,
  'data-table__th--sortable': column.sortable,
  [`data-table__th--align-${column.align || 'left'}`]: true
})

const getSortIcon = (key: string) => {
  if (sortKey.value !== key) return 'lucide:chevrons-up-down'
  return sortDirection.value === 'asc' ? 'lucide:chevron-up' : 'lucide:chevron-down'
}

const formatCellValue = (value: any, column: Column) => {
  if (column.formatter) {
    return column.formatter(value)
  }
  return value ?? '-'
}

const handleSort = (column: Column) => {
  if (!column.sortable) return

  if (sortKey.value === column.key) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = column.key
    sortDirection.value = 'asc'
  }

  emit('sort', sortKey.value, sortDirection.value)
}

const handleRowClick = (row: any, index: number) => {
  if (props.clickable) {
    emit('rowClick', row, index)
  }
}

const changePage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    emit('pageChange', page)
  }
}

// Reset to first page when data changes
watch(
  () => props.data,
  () => {
    currentPage.value = 1
  }
)
</script>

<style scoped>
.data-table {
  display: flex;
  flex-direction: column;
  width: 100%;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.data-table__container {
  overflow-x: auto;
}

.data-table__table {
  width: 100%;
  border-collapse: collapse;
}

.data-table__thead {
  background: var(--color-surface-alt);
  border-bottom: 1px solid var(--color-border);
}

.data-table__th {
  padding: var(--space-4) var(--space-6);
  text-align: left;
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  white-space: nowrap;
  position: relative;
}

.data-table__th--sortable {
  cursor: pointer;
  user-select: none;
}

.data-table__th--sortable:hover {
  background: var(--color-border-light);
}

.data-table__th-content {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.data-table__th--align-left {
  text-align: left;
}

.data-table__th--align-center {
  text-align: center;
}

.data-table__th--align-right {
  text-align: right;
}

.data-table__sort-icon {
  color: var(--color-text-muted);
}

.data-table__tbody {
  background: var(--color-surface);
}

.data-table__tr {
  border-bottom: 1px solid var(--color-border);
  transition: background-color var(--transition-fast) var(--transition-timing);
}

.data-table__tr:hover {
  background: var(--color-surface-alt);
}

.data-table__tr--clickable {
  cursor: pointer;
}

.data-table__tr--clickable:hover {
  background: var(--color-primary-light);
}

.data-table__td {
  padding: var(--space-4) var(--space-6);
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  vertical-align: middle;
}

.data-table__td--align-left {
  text-align: left;
}

.data-table__td--align-center {
  text-align: center;
}

.data-table__td--align-right {
  text-align: right;
}

.data-table__td--empty {
  padding: var(--space-12);
}

.data-table__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  color: var(--color-text-muted);
}

.data-table__empty-icon {
  opacity: 0.5;
}

.data-table__empty-text {
  margin: 0;
  font-size: var(--font-size-sm);
}

.data-table__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-6);
  background: var(--color-surface-alt);
  border-top: 1px solid var(--color-border);
}

.data-table__info {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.data-table__pagination {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.data-table__page-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 var(--space-2);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-fast) var(--transition-timing);
}

.data-table__page-btn:hover:not(:disabled) {
  background: var(--color-surface-alt);
  border-color: var(--color-primary);
}

.data-table__page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.data-table__page-btn--active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-text-light);
  font-weight: var(--font-weight-medium);
}

/* Sticky header */
.data-table--sticky .data-table__thead th {
  position: sticky;
  top: 0;
  z-index: 10;
}

/* Responsive */
@media (max-width: 768px) {
  .data-table__th,
  .data-table__td {
    padding: var(--space-3) var(--space-4);
  }

  .data-table__footer {
    flex-direction: column;
    gap: var(--space-4);
  }
}
</style>
