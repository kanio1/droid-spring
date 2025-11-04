<template>
  <AppTable
    :columns="columns"
    :data="products"
    :loading="loading"
    :show-pagination="true"
    :pagination="pagination"
    @page="onPage"
    @sort="onSort"
    @update:sortField="sortField = $event"
    @update:sortOrder="sortOrder = $event"
  >
    <!-- Name and Code column -->
    <template #cell-name="{ row }">
      <div class="product-name">
        <div class="product-info">
          <div class="product-full-name">{{ row.name }}</div>
          <div class="product-code">{{ row.productCode }}</div>
        </div>
      </div>
    </template>

    <!-- Type column -->
    <template #cell-productType="{ value }">
      <span class="product-type">{{ PRODUCT_TYPE_LABELS[value] }}</span>
    </template>

    <!-- Category column -->
    <template #cell-category="{ value }">
      <Tag :value="PRODUCT_CATEGORY_LABELS[value]" severity="info" />
    </template>

    <!-- Price column -->
    <template #cell-price="{ row }">
      <div class="product-price">
        {{ formatPrice(row) }}
        <span class="billing-period">/{{ row.billingPeriod }}</span>
      </div>
    </template>

    <!-- Status column -->
    <template #cell-status="{ row }">
      <StatusBadge :status="row.status" type="product" size="small" />
    </template>

    <!-- Validity column -->
    <template #cell-validity="{ row }">
      <span v-if="row.validityStart || row.validityEnd" class="product-validity">
        {{ getProductValidity(row) }}
      </span>
      <span v-else class="product-validity--empty">—</span>
    </template>

    <!-- Actions column -->
    <template #cell-actions="{ row }">
      <div class="product-actions">
        <Button
          icon="pi pi-eye"
          text
          rounded
          @click="$emit('view', row)"
          v-tooltip.top="'View details'"
        />
        <Button
          icon="pi pi-pencil"
          text
          rounded
          @click="$emit('edit', row)"
          v-tooltip.top="'Edit product'"
        />
        <Button
          icon="pi pi-trash"
          text
          rounded
          severity="danger"
          @click="$emit('delete', row)"
          v-tooltip.top="'Delete product'"
        />
      </div>
    </template>

    <!-- Empty state -->
    <template #empty>
      <slot name="empty">
        <div class="empty-state">
          <i class="pi pi-box empty-state__icon"></i>
          <h3 class="empty-state__title">No products found</h3>
          <p class="empty-state__description">
            {{ searchTerm || statusFilter || typeFilter || categoryFilter ?
              'Try adjusting your search criteria' :
              'Get started by adding your first product'
            }}
          </p>
          <Button
            v-if="!searchTerm && !statusFilter && !typeFilter && !categoryFilter"
            label="Add First Product"
            icon="pi pi-plus"
            severity="primary"
            @click="$emit('create')"
          />
        </div>
      </slot>
    </template>
  </AppTable>
</template>

<script setup lang="ts">
import { formatPrice, PRODUCT_TYPE_LABELS, PRODUCT_CATEGORY_LABELS, getProductValidity } from '~/schemas/product'
import type { Product } from '~/schemas/product'

interface Props {
  products: Product[]
  loading?: boolean
  pagination: any
  searchTerm?: string
  statusFilter?: string
  typeFilter?: string
  categoryFilter?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  view: [product: Product]
  edit: [product: Product]
  delete: [product: Product]
  create: []
  page: [{ page: number; rows: number }]
  sort: [{ field: string; order: number }]
}>()

const sortField = ref('createdAt')
const sortOrder = ref(-1)

const columns = [
  {
    key: 'name',
    label: 'Product',
    sortable: false,
    style: 'width: 25%'
  },
  {
    key: 'productType',
    label: 'Type',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'category',
    label: 'Category',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'price',
    label: 'Price',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'validity',
    label: 'Validity',
    sortable: false,
    style: 'width: 16%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 10%'
  }
]

const onPage = (event: any) => {
  emit('page', { page: event.page, rows: event.rows })
}

const onSort = (event: any) => {
  emit('sort', { field: event.sortField, order: event.sortOrder })
}
</script>

<style scoped>
.product-name {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.product-info {
  flex: 1;
  min-width: 0;
}

.product-full-name {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-code {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.product-type {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
}

.product-price {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.billing-period {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  font-weight: var(--font-weight-normal);
}

.product-validity {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.product-validity--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

.product-actions {
  display: flex;
  gap: var(--space-1);
  align-items: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-12);
  text-align: center;
}

.empty-state__icon {
  font-size: 4rem;
  margin-bottom: var(--space-4);
  opacity: 0.5;
  color: var(--color-text-muted);
}

.empty-state__title {
  margin: 0 0 var(--space-2) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.empty-state__description {
  margin: 0 0 var(--space-6) 0;
  color: var(--color-text-secondary);
  max-width: 400px;
}
</style>
