<template>
  <div class="app-table" :class="tableClasses">
    <!-- Table Header -->
    <div v-if="$slots.header || title" class="app-table__header">
      <div class="app-table__header-content">
        <h3 v-if="title" class="app-table__title">{{ title }}</h3>
        <div v-if="$slots.header" class="app-table__header-slot">
          <slot name="header" />
        </div>
      </div>
    </div>

    <!-- Table Container -->
    <div class="app-table__container" :style="{ maxHeight: maxHeight }">
      <table class="app-table__table">
        <thead v-if="columns.length > 0" class="app-table__thead">
          <tr class="app-table__tr">
            <th
              v-for="column in columns"
              :key="column.key"
              class="app-table__th"
              :class="getColumnClasses(column)"
              :style="{ width: column.width }"
              @click="handleSort(column)"
            >
              <div class="app-table__th-content">
                <span class="app-table__th-text">{{ column.label }}</span>
                <span v-if="column.sortable" class="app-table__sort" :class="getSortClasses(column)">
                  {{ getSortIcon(column) }}
                </span>
              </div>
            </th>
          </tr>
        </thead>
        
        <tbody class="app-table__tbody">
          <tr
            v-for="(row, rowIndex) in visibleRows"
            :key="getRowKey(row, rowIndex)"
            class="app-table__tr table-row-hover"
            :class="{
              'app-table__tr--clickable': clickable,
              'app-table__tr--selected': isRowSelected(row)
            }"
            @click="handleRowClick(row, rowIndex)"
          >
            <td
              v-for="column in columns"
              :key="column.key"
              class="app-table__td"
              :class="getColumnClasses(column)"
            >
              <slot
                :name="`cell-${column.key}`"
                :value="getCellValue(row, column)"
                :row="row"
                :row-index="rowIndex"
                :column="column"
              >
                {{ formatCellValue(getCellValue(row, column), column) }}
              </slot>
            </td>
          </tr>
          
          <!-- Empty State -->
          <tr v-if="visibleRows.length === 0" class="app-table__tr">
            <td :colspan="columns.length" class="app-table__td app-table__td--empty">
              <div class="app-table__empty">
                <slot name="empty">
                  <span class="app-table__empty-text">{{ emptyText }}</span>
                </slot>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Table Footer with Pagination -->
    <div v-if="showPagination" class="app-table__footer">
      <div class="app-table__info">
        <span class="app-table__info-text">
          {{ paginationInfo }}
        </span>
      </div>
      
      <div class="app-table__pagination">
        <slot name="pagination" :pagination="paginationProps">
          <AppPagination
            v-bind="paginationProps"
            @page-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface TableColumn {
  key: string
  label: string
  sortable?: boolean
  width?: string
  align?: 'left' | 'center' | 'right'
  format?: (value: any) => string
}

interface PaginationProps {
  page: number
  size: number
  total: number
  pageCount: number
  hasNext: boolean
  hasPrevious: boolean
}

interface Props {
  columns: TableColumn[]
  data: Array<any>
  title?: string
  loading?: boolean
  clickable?: boolean
  selectedRows?: Array<any>
  emptyText?: string
  maxHeight?: string
  showPagination?: boolean
  pagination?: PaginationProps
  page?: number
  size?: number
  total?: number
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  clickable: false,
  selectedRows: () => [],
  emptyText: 'No data available',
  maxHeight: '400px',
  showPagination: true
})

const emit = defineEmits<{
  sort: [column: TableColumn, direction: 'asc' | 'desc']
  rowClick: [row: any, index: number]
  'update:selectedRows': [rows: Array<any>]
  pageChange: [page: number]
  sizeChange: [size: number]
  selectionChange: [rows: Array<any>]
}>()

// Reactive state
const sortColumn = ref<string>('')
const sortDirection = ref<'asc' | 'desc'>('asc')
const internalPage = ref(1)
const internalSize = ref(props.size || 20)

// Computed properties
const visibleRows = computed(() => {
  if (!props.showPagination) return props.data
  
  const start = (internalPage.value - 1) * internalSize.value
  const end = start + internalSize.value
  return props.data.slice(start, end)
})

const tableClasses = computed(() => [
  'app-table',
  {
    'app-table--loading': props.loading,
    'app-table--clickable': props.clickable,
    'app-table--with-pagination': props.showPagination
  }
])

const paginationProps = computed(() => ({
  page: internalPage.value,
  size: internalSize.value,
  total: props.total || props.data.length,
  pageCount: Math.ceil((props.total || props.data.length) / internalSize.value),
  hasNext: internalPage.value < Math.ceil((props.total || props.data.length) / internalSize.value),
  hasPrevious: internalPage.value > 1
}))

const paginationInfo = computed(() => {
  const total = props.total || props.data.length
  const start = (internalPage.value - 1) * internalSize.value + 1
  const end = Math.min(internalPage.value * internalSize.value, total)
  
  return `Showing ${start}-${end} of ${total} results`
})

// Methods
const getRowKey = (row: any, index: number): string | number => {
  return row.id || row.key || index
}

const getCellValue = (row: any, column: TableColumn): any => {
  return row[column.key]
}

const formatCellValue = (value: any, column: TableColumn): string => {
  if (column.format) {
    return column.format(value)
  }
  return String(value ?? '')
}

const getColumnClasses = (column: TableColumn): string[] => {
  const classes = ['app-table__td']
  if (column.align) {
    classes.push(`app-table__td--${column.align}`)
  }
  return classes
}

const handleSort = (column: TableColumn) => {
  if (!column.sortable) return
  
  if (sortColumn.value === column.key) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortColumn.value = column.key
    sortDirection.value = 'asc'
  }
  
  emit('sort', column, sortDirection.value)
}

const getSortClasses = (column: TableColumn): string => {
  if (sortColumn.value !== column.key) return 'app-table__sort--inactive'
  return `app-table__sort--${sortDirection.value}`
}

const getSortIcon = (column: TableColumn): string => {
  if (!column.sortable || sortColumn.value !== column.key) return '↕'
  return sortDirection.value === 'asc' ? '↑' : '↓'
}

const handleRowClick = (row: any, index: number) => {
  if (!props.clickable) return
  emit('rowClick', row, index)
}

const handlePageChange = (page: number) => {
  internalPage.value = page
  emit('pageChange', page)
}

const handleSizeChange = (size: number) => {
  internalSize.value = size
  internalPage.value = 1 // Reset to first page
  emit('sizeChange', size)
}

const isRowSelected = (row: any): boolean => {
  return props.selectedRows.some(selectedRow => 
    selectedRow.id === row.id || selectedRow === row
  )
}
</script>

<style scoped>
.app-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.app-table--loading {
  opacity: 0.7;
  pointer-events: none;
}

/* Header */
.app-table__header {
  padding: var(--space-4) var(--space-6);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-alt);
}

.app-table__header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.app-table__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

/* Container */
.app-table__container {
  overflow-x: auto;
  overflow-y: auto;
}

.app-table__table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--font-size-sm);
}

/* Header */
.app-table__thead {
  background: var(--color-surface-alt);
  position: sticky;
  top: 0;
  z-index: 1;
}

.app-table__th {
  padding: var(--space-3) var(--space-4);
  text-align: left;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
  cursor: default;
  user-select: none;
}

.app-table__th-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.app-table__th[aria-sort] {
  cursor: pointer;
}

.app-table__th[aria-sort]:hover {
  background: var(--color-border-light);
}

.app-table__sort {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  transition: color var(--transition-fast) var(--transition-timing);
}

.app-table__sort--inactive {
  opacity: 0.3;
}

.app-table__sort--active {
  color: var(--color-primary);
  opacity: 1;
}

/* Body */
.app-table__tbody {
  background: var(--color-surface);
}

.app-table__tr {
  border-bottom: 1px solid var(--color-border);
  transition: background-color var(--transition-fast) var(--transition-timing);
}

.app-table__tr:last-child {
  border-bottom: none;
}

.app-table__tr--clickable {
  cursor: pointer;
}

.app-table__tr--clickable:hover {
  background: var(--color-surface-alt);
}

.app-table__tr--selected {
  background: var(--color-primary-light);
}

.app-table__td {
  padding: var(--space-3) var(--space-4);
  color: var(--color-text-primary);
  vertical-align: middle;
}

.app-table__td--left {
  text-align: left;
}

.app-table__td--center {
  text-align: center;
}

.app-table__td--right {
  text-align: right;
}

.app-table__td--empty {
  padding: var(--space-8);
  text-align: center;
}

.app-table__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
}

.app-table__empty-text {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Footer */
.app-table__footer {
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--color-border);
  background: var(--color-surface-alt);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.app-table__info {
  flex: 1;
}

.app-table__info-text {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.app-table__pagination {
  display: flex;
  align-items: center;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .app-table__header {
    padding: var(--space-3) var(--space-4);
  }
  
  .app-table__header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }
  
  .app-table__th,
  .app-table__td {
    padding: var(--space-2) var(--space-3);
    font-size: var(--font-size-xs);
  }
  
  .app-table__footer {
    padding: var(--space-3) var(--space-4);
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-3);
  }
  
  .app-table__info {
    text-align: center;
  }
}

/* Scrollbar Styling */
.app-table__container::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

.app-table__container::-webkit-scrollbar-track {
  background: var(--color-surface-alt);
}

.app-table__container::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: var(--radius-sm);
}

.app-table__container::-webkit-scrollbar-thumb:hover {
  background: var(--color-text-muted);
}
</style>
