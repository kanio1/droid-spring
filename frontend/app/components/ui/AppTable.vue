<template>
  <div class="app-table">
    <DataTable
      :value="data"
      :loading="loading"
      :paginator="showPagination"
      :rows="pagination?.size || 20"
      :totalRecords="pagination?.totalElements || 0"
      :lazy="true"
      :rowsPerPageOptions="[10, 20, 50, 100]"
      :sortField="sortField"
      :sortOrder="sortOrder"
      :responsiveLayout="'scroll'"
      :scrollable="true"
      scrollHeight="600px"
      dataKey="id"
      :rowHover="true"
      v-model:selection="selectedItems"
      selectionMode="multiple"
      :metaKeySelection="false"
      @page="onPage"
      @sort="onSort"
      @update:sortField="updateSortField"
      @update:sortOrder="updateSortOrder"
    >
      <Column v-if="selectable" selectionMode="multiple" headerStyle="width: 3rem"></Column>

      <Column
        v-for="column in columns"
        :key="column.key"
        :field="column.key"
        :header="column.label"
        :sortable="column.sortable !== false"
        :style="column.style"
        :bodyStyle="column.bodyStyle"
        :headerStyle="column.headerStyle"
      >
        <template v-if="column.slot" #body="slotProps">
          <slot :name="`cell-${column.key}`" v-bind="slotProps"></slot>
        </template>
      </Column>

      <template v-if="$slots.empty" #empty>
        <slot name="empty"></slot>
      </template>

      <template v-if="$slots.loading" #loading>
        <slot name="loading"></slot>
      </template>
    </DataTable>

    <div v-if="showPagination && pagination" class="table-pagination-info">
      <div class="pagination-info">
        Showing {{ (pagination.page * pagination.size) + 1 }} to
        {{ Math.min((pagination.page + 1) * pagination.size, pagination.totalElements) }}
        of {{ pagination.totalElements }} entries
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'

interface ColumnDef {
  key: string
  label: string
  sortable?: boolean
  style?: string
  bodyStyle?: string
  headerStyle?: string
  slot?: boolean
}

interface Props {
  columns: ColumnDef[]
  data: any[]
  loading?: boolean
  showPagination?: boolean
  pagination?: {
    page: number
    size: number
    totalElements: number
  }
  selectable?: boolean
  sortField?: string
  sortOrder?: number
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  showPagination: true,
  selectable: false
})

const emit = defineEmits<{
  page: [{ page: number; rows: number }]
  sort: [{ field: string; order: number }]
  'update:sortField': [field: string]
  'update:sortOrder': [order: number]
  'update:selectedItems': [items: any[]]
}>()

const selectedItems = ref<any[]>([])

const onPage = (event: any) => {
  emit('page', { page: event.page, rows: event.rows })
}

const onSort = (event: any) => {
  emit('sort', { field: event.sortField, order: event.sortOrder })
}

const updateSortField = (field: string) => {
  emit('update:sortField', field)
}

const updateSortOrder = (order: number) => {
  emit('update:sortOrder', order)
}

watch(selectedItems, (newVal) => {
  emit('update:selectedItems', newVal)
})
</script>

<style scoped>
.app-table {
  width: 100%;
}

.table-pagination-info {
  padding: var(--space-3) 0;
  border-top: 1px solid var(--color-border);
}

.pagination-info {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

:deep(.p-datatable) {
  font-family: var(--font-family-base);
}

:deep(.p-datatable .p-datatable-header) {
  background: var(--color-surface);
  border-color: var(--color-border);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-semibold);
}

:deep(.p-datatable .p-datatable-thead > tr > th) {
  background: var(--color-surface-alt);
  border-color: var(--color-border);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-semibold);
  padding: var(--space-3);
}

:deep(.p-datatable .p-datatable-tbody > tr) {
  background: var(--color-surface);
  border-color: var(--color-border);
}

:deep(.p-datatable .p-datatable-tbody > tr:hover) {
  background: var(--color-surface-alt);
}

:deep(.p-datatable .p-datatable-tbody > tr > td) {
  padding: var(--space-3);
  border-color: var(--color-border);
}

:deep(.p-paginator) {
  background: var(--color-surface);
  border-color: var(--color-border);
}

:deep(.p-paginator .p-paginator-pages .p-paginator-page) {
  color: var(--color-text-secondary);
}

:deep(.p-paginator .p-paginator-pages .p-paginator-page:hover) {
  background: var(--color-surface-alt);
}

:deep(.p-paginator .p-paginator-pages .p-paginator-page.p-highlight) {
  background: var(--color-primary);
  color: white;
}
</style>
