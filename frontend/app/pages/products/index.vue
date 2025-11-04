<template>
  <div class="products-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Products</h1>
        <p class="page-subtitle">Manage product catalog and pricing</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Add Product"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/products/create')"
        />
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="products-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search products..."
            @input="handleSearch"
            style="width: 100%"
          />
        </span>

        <Dropdown
          v-model="statusFilter"
          :options="statusOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Statuses"
          style="width: 150px"
          @change="handleStatusFilter"
        />

        <Dropdown
          v-model="typeFilter"
          :options="typeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Types"
          style="width: 150px"
          @change="handleTypeFilter"
        />

        <Dropdown
          v-model="categoryFilter"
          :options="categoryOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Categories"
          style="width: 150px"
          @change="handleCategoryFilter"
        />

        <Dropdown
          v-model="sortOption"
          :options="sortOptions"
          optionLabel="label"
          optionValue="value"
          style="width: 180px"
          @change="handleSortChange"
        />
      </div>
    </div>

    <!-- Products Table -->
    <ProductTable
      :products="products"
      :loading="productStore.loading"
      :pagination="productStore.pagination"
      :searchTerm="searchTerm"
      :statusFilter="statusFilter"
      :typeFilter="typeFilter"
      :categoryFilter="categoryFilter"
      @view="handleView"
      @edit="handleEdit"
      @delete="handleDelete"
      @create="navigateTo('/products/create')"
      @page="handlePage"
      @sort="handleSort"
    />

    <!-- Toast for notifications -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { useProductStore } from '~/stores/product'
import type { Product, ProductStatus, ProductType, ProductCategory } from '~/schemas/product'
import { PRODUCT_STATUS_LABELS, PRODUCT_TYPE_LABELS, PRODUCT_CATEGORY_LABELS } from '~/schemas/product'
import { formatPrice, getProductValidity } from '~/schemas/product'

// Page meta
definePageMeta({
  title: 'Products'
})

// Store
const productStore = useProductStore()
const { showToast } = useToast()

// Reactive state
const searchTerm = ref('')
const statusFilter = ref<ProductStatus | ''>('')
const typeFilter = ref<ProductType | ''>('')
const categoryFilter = ref<ProductCategory | ''>('')
const sortOption = ref('createdAt,desc')
const sortField = ref('createdAt')
const sortOrder = ref(-1)

// Filter options
const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Deprecated', value: 'DEPRECATED' }
]

const typeOptions = [
  { label: 'All Types', value: '' },
  { label: 'Service', value: 'SERVICE' },
  { label: 'Tariff', value: 'TARIFF' },
  { label: 'Bundle', value: 'BUNDLE' },
  { label: 'Add-on', value: 'ADDON' }
]

const categoryOptions = [
  { label: 'All Categories', value: '' },
  { label: 'Mobile', value: 'MOBILE' },
  { label: 'Broadband', value: 'BROADBAND' },
  { label: 'TV', value: 'TV' },
  { label: 'Cloud', value: 'CLOUD' }
]

const sortOptions = [
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' },
  { label: 'Name A-Z', value: 'name,asc' },
  { label: 'Name Z-A', value: 'name,desc' },
  { label: 'Price Low-High', value: 'price,asc' },
  { label: 'Price High-Low', value: 'price,desc' },
  { label: 'Status A-Z', value: 'status,asc' }
]

// Computed
const products = computed(() => productStore.products)

// Methods
const handleSearch = useDebounceFn(async () => {
  await productStore.fetchProducts({
    searchTerm: searchTerm.value || undefined,
    page: 0,
    sort: `${sortField.value},${sortOrder.value === -1 ? 'desc' : 'asc'}`,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    category: categoryFilter.value || undefined
  })
}, 300)

const handleStatusFilter = async () => {
  await productStore.fetchProducts({
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    page: 0,
    sort: `${sortField.value},${sortOrder.value === -1 ? 'desc' : 'asc'}`
  })
}

const handleTypeFilter = async () => {
  await productStore.fetchProducts({
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    page: 0,
    sort: `${sortField.value},${sortOrder.value === -1 ? 'desc' : 'asc'}`
  })
}

const handleCategoryFilter = async () => {
  await productStore.fetchProducts({
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    page: 0,
    sort: `${sortField.value},${sortOrder.value === -1 ? 'desc' : 'asc'}`
  })
}

const handleSortChange = async () => {
  const [field, order] = sortOption.value.split(',')
  sortField.value = field
  sortOrder.value = order === 'desc' ? -1 : 1

  await productStore.fetchProducts({
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    page: 0,
    sort: sortOption.value
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  sortField.value = field
  sortOrder.value = order

  await productStore.fetchProducts({
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    page: productStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await productStore.fetchProducts({
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    category: categoryFilter.value || undefined,
    page,
    size: rows,
    sort: `${sortField.value},${sortOrder.value === -1 ? 'desc' : 'asc'}`
  })
}

const handleView = (row: Product) => {
  navigateTo(`/products/${row.id}`)
}

const handleEdit = (row: Product) => {
  navigateTo(`/products/${row.id}/edit`)
}

const handleDelete = async (row: Product) => {
  const confirmed = confirm(
    `Are you sure you want to delete "${row.name}"?`
  )

  if (confirmed) {
    try {
      await productStore.deleteProduct(row.id, row.version)

      showToast({
        severity: 'success',
        summary: 'Product Deleted',
        detail: `"${row.name}" has been successfully deleted.`,
        life: 3000
      })

      // Refresh the list
      await productStore.fetchProducts({
        searchTerm: searchTerm.value || undefined,
        status: statusFilter.value || undefined,
        type: typeFilter.value || undefined,
        category: categoryFilter.value || undefined
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to delete product',
        life: 5000
      })
    }
  }
}

// Lifecycle
onMounted(async () => {
  await productStore.fetchProducts()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await productStore.fetchProducts()
})
</script>

<style scoped>
.products-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

/* Page Header */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.page-header__content {
  flex: 1;
}

.page-title {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-3xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

.page-subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.page-header__actions {
  flex-shrink: 0;
}

/* Filters */
.products-filters {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.filters-row {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  flex-wrap: wrap;
}

.filters-row > * {
  flex-shrink: 0;
}

.filters-row .p-inputtext {
  width: 100%;
  min-width: 200px;
}

.filters-row .p-dropdown {
  min-width: 140px;
}

/* Mobile Responsive */
@media (max-width: 1024px) {
  .filters-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filters-row > * {
    width: 100%;
  }

  .filters-row .p-inputtext {
    min-width: unset;
  }
}
</style>
