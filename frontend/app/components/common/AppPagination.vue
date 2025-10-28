<template>
  <div class="app-pagination" :class="paginationClasses">
    <!-- Page Size Selector -->
    <div v-if="showSizeSelector" class="pagination__size-selector">
      <label class="size-selector__label">Show:</label>
      <AppSelect
        :model-value="size"
        :options="pageSizeOptions"
        size="sm"
        @change="handleSizeChange"
      />
    </div>

    <!-- Pagination Controls -->
    <div class="pagination__controls">
      <!-- Previous Button -->
      <button
        type="button"
        class="pagination__button pagination__button--nav"
        :disabled="!hasPrevious"
        @click="previousPage"
        aria-label="Previous page"
      >
        ‹
      </button>

      <!-- Page Numbers -->
      <div class="pagination__numbers">
        <template v-for="page in visiblePages" :key="page">
          <button
            v-if="page === '...'"
            type="button"
            class="pagination__button pagination__button--ellipsis"
            disabled
          >
            ...
          </button>
          <button
            v-else
            type="button"
            :class="[
              'pagination__button',
              'pagination__button--page',
              { 'pagination__button--current': page === currentPage }
            ]"
            :disabled="page === currentPage"
            @click="goToPage(page as number)"
            :aria-label="`Go to page ${page}`"
          >
            {{ page }}
          </button>
        </template>
      </div>

      <!-- Next Button -->
      <button
        type="button"
        class="pagination__button pagination__button--nav"
        :disabled="!hasNext"
        @click="nextPage"
        aria-label="Next page"
      >
        ›
      </button>
    </div>

    <!-- Page Info -->
    <div v-if="showPageInfo" class="pagination__info">
      {{ pageInfoText }}
    </div>
  </div>
</template>

<script setup lang="ts">
interface Props {
  page: number
  size: number
  total: number
  pageCount?: number
  hasNext?: boolean
  hasPrevious?: boolean
  showSizeSelector?: boolean
  showPageInfo?: boolean
  maxVisiblePages?: number
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showSizeSelector: true,
  showPageInfo: false,
  maxVisiblePages: 7,
  compact: false
})

const emit = defineEmits<{
  pageChange: [page: number]
  sizeChange: [size: number]
}>()

// Page size options
const pageSizeOptions = [
  { value: 10, label: '10' },
  { value: 20, label: '20' },
  { value: 50, label: '50' },
  { value: 100, label: '100' }
]

// Computed properties
const currentPage = computed(() => props.page + 1) // Convert 0-based to 1-based for display
const totalPages = computed(() => {
  return props.pageCount || Math.ceil(props.total / props.size)
})

const startItem = computed(() => {
  if (props.total === 0) return 0
  return props.page * props.size + 1
})

const endItem = computed(() => {
  return Math.min((props.page + 1) * props.size, props.total)
})

const pageInfoText = computed(() => {
  if (props.total === 0) return 'No items'
  return `Showing ${startItem.value}-${endItem.value} of ${props.total}`
})

const paginationClasses = computed(() => [
  'app-pagination',
  {
    'app-pagination--compact': props.compact,
    'app-pagination--minimal': !props.showSizeSelector && !props.showPageInfo
  }
])

// Visible pages calculation (with ellipsis for large page counts)
const visiblePages = computed(() => {
  const total = totalPages.value
  const current = currentPage.value
  const maxVisible = props.maxVisiblePages

  if (total <= maxVisible) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }

  const pages: (number | string)[] = []
  const start = Math.max(2, current - 2)
  const end = Math.min(total - 1, current + 2)

  // Always show first page
  pages.push(1)

  // Add ellipsis if needed
  if (start > 2) {
    pages.push('...')
  }

  // Add page range
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }

  // Add ellipsis if needed
  if (end < total - 1) {
    pages.push('...')
  }

  // Always show last page
  if (total > 1) {
    pages.push(total)
  }

  return pages
})

// Methods
const previousPage = () => {
  if (props.hasPrevious) {
    emit('pageChange', props.page - 1)
  }
}

const nextPage = () => {
  if (props.hasNext) {
    emit('pageChange', props.page + 1)
  }
}

const goToPage = (page: number) => {
  const zeroBasedPage = page - 1
  if (zeroBasedPage !== props.page && zeroBasedPage >= 0 && zeroBasedPage < totalPages.value) {
    emit('pageChange', zeroBasedPage)
  }
}

const handleSizeChange = (newSize: string | number) => {
  const size = typeof newSize === 'string' ? parseInt(newSize, 10) : newSize
  if (size !== props.size) {
    emit('sizeChange', size)
  }
}
</script>

<style scoped>
.app-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.app-pagination--compact {
  gap: var(--space-2);
}

.app-pagination--minimal {
  justify-content: center;
}

/* Size Selector */
.pagination__size-selector {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.size-selector__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.size-selector__label .app-select {
  min-width: 70px;
}

/* Controls */
.pagination__controls {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.pagination__numbers {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.pagination__button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: all var(--transition-fast) var(--transition-timing);
  text-decoration: none;
  user-select: none;
}

.pagination__button:hover:not(:disabled) {
  background: var(--color-surface-alt);
  border-color: var(--color-primary);
  color: var(--color-text-primary);
}

.pagination__button:focus {
  outline: 2px solid var(--color-primary);
  outline-offset: 1px;
}

.pagination__button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Navigation Buttons */
.pagination__button--nav {
  width: 36px;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-bold);
}

/* Page Numbers */
.pagination__button--page {
  min-width: 40px;
  font-weight: var(--font-weight-normal);
}

.pagination__button--current {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-semibold);
}

.pagination__button--current:hover {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-text-primary);
}

/* Ellipsis */
.pagination__button--ellipsis {
  border: none;
  background: transparent;
  cursor: default;
  pointer-events: none;
}

/* Page Info */
.pagination__info {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

/* Compact styles */
.app-pagination--compact .pagination__button {
  min-width: 32px;
  height: 32px;
  font-size: var(--font-size-xs);
}

.app-pagination--compact .pagination__button--nav {
  width: 32px;
}

/* Mobile Responsive */
@media (max-width: 640px) {
  .app-pagination {
    flex-direction: column;
    gap: var(--space-3);
    align-items: stretch;
  }
  
  .pagination__size-selector {
    justify-content: center;
  }
  
  .pagination__controls {
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .pagination__info {
    text-align: center;
    order: -1;
  }
  
  .pagination__button--page {
    min-width: 36px;
  }
}

/* Touch-friendly sizing on mobile */
@media (max-width: 480px) {
  .pagination__button {
    min-width: 40px;
    height: 40px;
    font-size: var(--font-size-base);
  }
  
  .pagination__button--nav {
    width: 40px;
  }
}

/* High contrast mode */
@media (prefers-contrast: high) {
  .pagination__button {
    border-width: 2px;
  }
  
  .pagination__button--current {
    border-width: 2px;
  }
}

/* Reduced motion */
@media (prefers-reduced-motion: reduce) {
  .pagination__button {
    transition: none;
  }
}
</style>
