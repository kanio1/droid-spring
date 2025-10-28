// Pagination composable - manage pagination state and URL sync

interface PaginationOptions {
  initialPage?: number
  initialSize?: number
  initialSort?: string
  pageSizes?: number[]
  syncWithUrl?: boolean
}

interface PaginationState {
  page: number
  size: number
  sort: string
  total?: number
  pageCount?: number
}

export const usePagination = (options: PaginationOptions = {}) => {
  const {
    initialPage = 0,
    initialSize = 20,
    initialSort = 'createdAt,desc',
    pageSizes = [10, 20, 50, 100],
    syncWithUrl = true
  } = options

  const route = useRoute()
  const router = useRouter()

  // Reactive state
  const page = ref(initialPage)
  const size = ref(initialSize)
  const sort = ref(initialSort)
  const total = ref<number>()
  const pageCount = ref<number>()

  // Computed values
  const hasNext = computed(() => {
    if (!pageCount.value && !total.value) return false
    if (pageCount.value !== undefined) {
      return page.value < pageCount.value - 1
    }
    if (total.value !== undefined) {
      return (page.value + 1) * size.value < total.value
    }
    return false
  })

  const hasPrevious = computed(() => page.value > 0)

  const startIndex = computed(() => {
    if (page.value === 0) return 1
    return (page.value * size.value) + 1
  })

  const endIndex = computed(() => {
    if (total.value !== undefined) {
      return Math.min((page.value + 1) * size.value, total.value)
    }
    if (pageCount.value !== undefined) {
      return (page.value + 1) * size.value
    }
    return (page.value + 1) * size.value
  })

  const queryParams = computed(() => ({
    page: page.value,
    size: size.value,
    sort: sort.value
  }))

  const paginationInfo = computed(() => {
    const totalItems = total.value || '?'
    return `Showing ${startIndex.value}-${endIndex.value} of ${totalItems} results`
  })

  // URL synchronization
  const syncWithRoute = () => {
    if (!syncWithUrl) return

    const query = { ...route.query }
    
    // Update pagination params in URL
    if (page.value !== initialPage) {
      query.page = page.value.toString()
    } else {
      delete query.page
    }
    
    if (size.value !== initialSize) {
      query.size = size.value.toString()
    } else {
      delete query.size
    }
    
    if (sort.value !== initialSort) {
      query.sort = sort.value
    } else {
      delete query.sort
    }

    // Only update if query changed
    const queryChanged = JSON.stringify(query) !== JSON.stringify(route.query)
    
    if (queryChanged) {
      router.replace({ query })
    }
  }

  // Initialize from URL
  const initializeFromRoute = () => {
    if (!syncWithUrl) return

    const query = route.query
    
    if (query.page) {
      const pageNum = parseInt(query.page as string, 10)
      if (!isNaN(pageNum) && pageNum >= 0) {
        page.value = pageNum
      }
    }
    
    if (query.size) {
      const sizeNum = parseInt(query.size as string, 10)
      if (!isNaN(sizeNum) && pageSizes.includes(sizeNum)) {
        size.value = sizeNum
      }
    }
    
    if (query.sort && typeof query.sort === 'string') {
      sort.value = query.sort
    }
  }

  // Pagination methods
  const setPage = (newPage: number) => {
    if (newPage >= 0) {
      page.value = newPage
      syncWithRoute()
    }
  }

  const setSize = (newSize: number) => {
    if (pageSizes.includes(newSize)) {
      size.value = newSize
      // Reset to first page when changing size
      page.value = 0
      syncWithRoute()
    }
  }

  const setSort = (newSort: string) => {
    sort.value = newSort
    // Reset to first page when changing sort
    page.value = 0
    syncWithRoute()
  }

  const setTotal = (newTotal: number) => {
    total.value = newTotal
    pageCount.value = Math.ceil(newTotal / size.value)
  }

  const setPageCount = (newPageCount: number) => {
    pageCount.value = newPageCount
    // Adjust page if it's out of bounds
    if (page.value >= newPageCount && newPageCount > 0) {
      page.value = newPageCount - 1
      syncWithRoute()
    }
  }

  // Convenience methods
  const nextPage = () => {
    if (hasNext.value) {
      page.value += 1
      syncWithRoute()
    }
  }

  const previousPage = () => {
    if (hasPrevious.value) {
      page.value -= 1
      syncWithRoute()
    }
  }

  const firstPage = () => {
    page.value = 0
    syncWithRoute()
  }

  const lastPage = () => {
    if (pageCount.value !== undefined) {
      page.value = pageCount.value - 1
      syncWithRoute()
    } else if (total.value !== undefined) {
      const lastPageIndex = Math.floor((total.value - 1) / size.value)
      page.value = Math.max(0, lastPageIndex)
      syncWithRoute()
    }
  }

  const goToPage = (pageNumber: number) => {
    if (pageNumber >= 0) {
      // Check if page is valid (for known page count)
      if (pageCount.value !== undefined && pageNumber >= pageCount.value) {
        return false
      }
      
      page.value = pageNumber
      syncWithRoute()
      return true
    }
    return false
  }

  // Reset to initial state
  const reset = () => {
    page.value = initialPage
    size.value = initialSize
    sort.value = initialSort
    total.value = undefined
    pageCount.value = undefined
    syncWithRoute()
  }

  // Sort helpers
  const toggleSortDirection = (column: string) => {
    const [currentColumn, currentDirection] = sort.value.split(',')
    
    if (currentColumn === column) {
      // Toggle direction
      const newDirection = currentDirection === 'asc' ? 'desc' : 'asc'
      setSort(`${column},${newDirection}`)
    } else {
      // Default to ascending for new column
      setSort(`${column},asc`)
    }
  }

  const setSortColumn = (column: string, direction: 'asc' | 'desc' = 'asc') => {
    setSort(`${column},${direction}`)
  }

  const clearSort = () => {
    setSort('')
  }

  // Initialize on mount
  onMounted(() => {
    initializeFromRoute()
  })

  // Watch for route changes (when syncWithUrl is enabled)
  if (syncWithUrl) {
    watch(() => route.query, () => {
      initializeFromRoute()
    }, { deep: true })
  }

  return {
    // State
    page: readonly(page),
    size: readonly(size),
    sort: readonly(sort),
    total: readonly(total),
    pageCount: readonly(pageCount),
    
    // Computed
    hasNext: readonly(hasNext),
    hasPrevious: readonly(hasPrevious),
    startIndex: readonly(startIndex),
    endIndex: readonly(endIndex),
    queryParams: readonly(queryParams),
    paginationInfo: readonly(paginationInfo),
    
    // Settings
    pageSizes,
    
    // Methods
    setPage,
    setSize,
    setSort,
    setTotal,
    setPageCount,
    nextPage,
    previousPage,
    firstPage,
    lastPage,
    goToPage,
    reset,
    
    // Sort helpers
    toggleSortDirection,
    setSortColumn,
    clearSort
  }
}
