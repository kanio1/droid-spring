<template>
  <div class="unpaid-invoices-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Unpaid Invoices</h1>
        <p class="page-subtitle">Track and manage outstanding invoices</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Create Invoice"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/invoices/create')"
        />
      </div>
    </div>

    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-card__icon">
          <i class="pi pi-exclamation-circle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Total Unpaid</div>
          <div class="summary-card__value">{{ unpaidInvoices.length }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon overdue">
          <i class="pi pi-exclamation-triangle"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Overdue</div>
          <div class="summary-card__value">{{ overdueInvoices.length }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon amount">
          <i class="pi pi-dollar"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Outstanding Amount</div>
          <div class="summary-card__value">{{ formatCurrency(totalOutstandingAmount, 'USD') }}</div>
        </div>
      </div>

      <div class="summary-card">
        <div class="summary-card__icon amount-overdue">
          <i class="pi pi-calendar-times"></i>
        </div>
        <div class="summary-card__content">
          <div class="summary-card__label">Overdue Amount</div>
          <div class="summary-card__value">{{ formatCurrency(totalOverdueAmount, 'USD') }}</div>
        </div>
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="invoices-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search unpaid invoices..."
            @input="handleSearch"
            style="width: 100%"
          />
        </span>

        <Dropdown
          v-model="typeFilter"
          :options="typeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Types"
          style="width: 180px"
          @change="handleTypeFilter"
        />

        <Dropdown
          v-model="sortOption"
          :options="sortOptions"
          optionLabel="label"
          optionValue="value"
          style="width: 180px"
          @change="handleSortChange"
        />

        <div class="quick-filter">
          <Button
            :label="overdueFilter ? 'All Unpaid' : 'Overdue Only'"
            :icon="overdueFilter ? 'pi pi-list' : 'pi pi-exclamation-triangle'"
            :severity="overdueFilter ? 'secondary' : 'danger'"
            outlined
            @click="toggleOverdueFilter"
          />
        </div>
      </div>
    </div>

    <!-- Invoices Table -->
    <div class="invoices-table">
      <AppTable
        :columns="tableColumns"
        :data="filteredInvoices"
        :loading="invoiceStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Invoice Number column -->
        <template #cell-invoiceNumber="{ row }">
          <div class="invoice-number">
            <span class="invoice-number__text">{{ row.invoiceNumber }}</span>
          </div>
        </template>

        <!-- Customer column -->
        <template #cell-customerName="{ row }">
          <div class="customer-name">
            <div class="customer-avatar">
              {{ getCustomerInitials(row) }}
            </div>
            <div class="customer-info">
              <div class="customer-name__text">{{ row.customerName || 'N/A' }}</div>
              <div class="customer-id">{{ row.customerId }}</div>
            </div>
          </div>
        </template>

        <!-- Type column -->
        <template #cell-invoiceType="{ row }">
          <StatusBadge :status="row.invoiceType" type="invoice-type" size="small" />
        </template>

        <!-- Due Date column -->
        <template #cell-dueDate="{ row }">
          <div class="due-date">
            <span class="date" :class="{ 'date--overdue': isOverdue(row) }">
              {{ formatDate(row.dueDate) }}
            </span>
            <div v-if="isOverdue(row)" class="days-overdue">
              {{ getDaysOverdue(row) }} days overdue
            </div>
          </div>
        </template>

        <!-- Amount column -->
        <template #cell-totalAmount="{ row }">
          <span class="amount" :class="{ 'amount--overdue': isOverdue(row) }">
            {{ formatCurrency(row.totalAmount, row.currency) }}
          </span>
        </template>

        <!-- Days Until Due column -->
        <template #cell-daysUntilDue="{ row }">
          <div class="days-until">
            <span v-if="!isOverdue(row)" class="days-value">
              {{ getDaysUntilDue(row) }} days
            </span>
            <span v-else class="days-value overdue">Overdue</span>
          </div>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="invoice-actions">
            <Button
              icon="pi pi-eye"
              text
              rounded
              @click.stop="handleView(row)"
              v-tooltip.top="'View details'"
            />
            <Button
              icon="pi pi-send"
              text
              rounded
              @click.stop="handleSend(row)"
              v-tooltip.top="'Send reminder'"
              v-if="canSendInvoice(row)"
            />
            <Button
              icon="pi pi-credit-card"
              text
              rounded
              @click.stop="handleMarkPaid(row)"
              v-tooltip.top="'Mark as paid'"
            />
            <Button
              icon="pi pi-external-link"
              text
              rounded
              @click.stop="handleViewCustomer(row)"
              v-tooltip.top="'View customer'"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-check-circle empty-state__icon"></i>
            <h3 class="empty-state__title">All invoices are paid!</h3>
            <p class="empty-state__description">
              Great job! You have no unpaid invoices at the moment.
            </p>
            <Button
              label="View All Invoices"
              icon="pi pi-list"
              severity="primary"
              @click="navigateTo('/invoices')"
            />
          </div>
        </template>
      </AppTable>
    </div>

    <!-- Toast for notifications -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { useInvoiceStore } from '~/stores/invoice'
import { isInvoiceOverdue, canSendInvoice } from '~/schemas/invoice'

// Page meta
definePageMeta({
  title: 'Unpaid Invoices'
})

// Store
const invoiceStore = useInvoiceStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const typeFilter = ref('')
const sortOption = ref('dueDate,asc')
const overdueFilter = ref(false)

// Table columns configuration
const tableColumns = [
  {
    key: 'invoiceNumber',
    label: 'Invoice #',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'customerName',
    label: 'Customer',
    sortable: false,
    style: 'width: 20%'
  },
  {
    key: 'invoiceType',
    label: 'Type',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'dueDate',
    label: 'Due Date',
    sortable: true,
    style: 'width: 15%'
  },
  {
    key: 'totalAmount',
    label: 'Amount',
    sortable: true,
    style: 'width: 12%',
    align: 'right'
  },
  {
    key: 'daysUntilDue',
    label: 'Time Left',
    sortable: false,
    style: 'width: 12%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 14%'
  }
]

// Filter options
const typeOptions = [
  { label: 'All Types', value: '' },
  { label: 'One Time', value: 'ONE_TIME' },
  { label: 'Recurring', value: 'RECURRING' },
  { label: 'Adjustment', value: 'ADJUSTMENT' }
]

const sortOptions = [
  { label: 'Due Date (Earliest)', value: 'dueDate,asc' },
  { label: 'Due Date (Latest)', value: 'dueDate,desc' },
  { label: 'Amount (High-Low)', value: 'totalAmount,desc' },
  { label: 'Amount (Low-High)', value: 'totalAmount,asc' },
  { label: 'Invoice # A-Z', value: 'invoiceNumber,asc' },
  { label: 'Invoice # Z-A', value: 'invoiceNumber,desc' }
]

// Computed
const invoices = computed(() => invoiceStore.unpaidInvoices)
const overdueInvoices = computed(() => invoiceStore.overdueInvoices)
const unpaidInvoices = computed(() => invoiceStore.unpaidInvoices)
const totalOutstandingAmount = computed(() => invoiceStore.totalOutstandingAmount)
const totalOverdueAmount = computed(() => invoiceStore.totalOverdueAmount)

const filteredInvoices = computed(() => {
  let filtered = invoices.value

  if (searchTerm.value) {
    const search = searchTerm.value.toLowerCase()
    filtered = filtered.filter(inv =>
      inv.invoiceNumber.toLowerCase().includes(search) ||
      inv.customerName?.toLowerCase().includes(search)
    )
  }

  if (typeFilter.value) {
    filtered = filtered.filter(inv => inv.invoiceType === typeFilter.value)
  }

  if (overdueFilter.value) {
    filtered = filtered.filter(inv => isOverdue(inv))
  }

  return filtered
})

const paginationProps = computed(() => ({
  page: invoiceStore.pagination.page,
  size: invoiceStore.pagination.size,
  total: filteredInvoices.value.length
}))

// Methods
const handleSearch = useDebounceFn(async () => {
  await invoiceStore.fetchInvoices({
    page: 0,
    sort: sortOption.value,
    type: typeFilter.value || undefined,
    query: searchTerm.value || undefined,
    unpaid: true
  })
}, 300)

const handleTypeFilter = async () => {
  await invoiceStore.fetchInvoices({
    page: 0,
    sort: sortOption.value,
    query: searchTerm.value || undefined,
    type: typeFilter.value || undefined,
    unpaid: true
  })
}

const handleSortChange = async () => {
  await invoiceStore.fetchInvoices({
    page: 0,
    sort: sortOption.value,
    type: typeFilter.value || undefined,
    query: searchTerm.value || undefined,
    unpaid: true
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await invoiceStore.fetchInvoices({
    page: invoiceStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    type: typeFilter.value || undefined,
    query: searchTerm.value || undefined,
    unpaid: true
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await invoiceStore.fetchInvoices({
    page,
    size: rows,
    sort: sortOption.value,
    type: typeFilter.value || undefined,
    query: searchTerm.value || undefined,
    unpaid: true
  })
}

const handleView = (row: any) => {
  navigateTo(`/invoices/${row.id}`)
}

const handleViewCustomer = (row: any) => {
  navigateTo(`/customers/${row.customerId}`)
}

const handleSend = async (row: any) => {
  try {
    await invoiceStore.changeInvoiceStatus({
      id: row.id,
      status: 'SENT'
    })

    showToast({
      severity: 'success',
      summary: 'Reminder Sent',
      detail: `Payment reminder sent for invoice ${row.invoiceNumber}.`,
      life: 3000
    })

    await invoiceStore.fetchInvoices({ unpaid: true })

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to send reminder',
      life: 5000
    })
  }
}

const handleMarkPaid = async (row: any) => {
  const confirmed = confirm(`Mark invoice ${row.invoiceNumber} as paid?`)

  if (confirmed) {
    try {
      await invoiceStore.changeInvoiceStatus({
        id: row.id,
        status: 'PAID'
      })

      showToast({
        severity: 'success',
        summary: 'Invoice Marked as Paid',
        detail: `Invoice ${row.invoiceNumber} has been marked as paid.`,
        life: 3000
      })

      await invoiceStore.fetchInvoices({ unpaid: true })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to update invoice status',
        life: 5000
      })
    }
  }
}

const toggleOverdueFilter = () => {
  overdueFilter.value = !overdueFilter.value
}

// Utility functions
const getCustomerInitials = (row: any): string => {
  if (!row.customerName) return 'N/A'
  const names = row.customerName.split(' ')
  if (names.length >= 2) {
    return `${names[0][0]}${names[1][0]}`.toUpperCase()
  }
  return row.customerName.substring(0, 2).toUpperCase()
}

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const formatCurrency = (amount: number, currency: string = 'USD'): string => {
  if (!amount) return formatCurrency(0, currency)
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency
  }).format(amount)
}

const isOverdue = (row: any): boolean => {
  return isInvoiceOverdue(row)
}

const getDaysOverdue = (row: any): number => {
  const today = new Date()
  const dueDate = new Date(row.dueDate)
  const diffTime = today.getTime() - dueDate.getTime()
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
}

const getDaysUntilDue = (row: any): number => {
  const today = new Date()
  const dueDate = new Date(row.dueDate)
  const diffTime = dueDate.getTime() - today.getTime()
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
}

// Lifecycle
onMounted(async () => {
  await invoiceStore.fetchInvoices({ unpaid: true })
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await invoiceStore.fetchInvoices({ unpaid: true })
})
</script>

<style scoped>
.unpaid-invoices-page {
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

/* Summary Cards */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.summary-card {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  align-items: center;
}

.summary-card__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  background: var(--color-info-100);
  color: var(--color-info-600);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
  flex-shrink: 0;
}

.summary-card__icon.overdue {
  background: var(--color-red-100);
  color: var(--color-red-600);
}

.summary-card__icon.amount {
  background: var(--color-green-100);
  color: var(--color-green-600);
}

.summary-card__icon.amount-overdue {
  background: var(--color-orange-100);
  color: var(--color-orange-600);
}

.summary-card__content {
  flex: 1;
}

.summary-card__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-1);
}

.summary-card__value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

/* Filters */
.invoices-filters {
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
  min-width: 250px;
}

.quick-filter {
  margin-left: auto;
}

/* Table */
.invoices-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Invoice Number Cell */
.invoice-number__text {
  font-family: monospace;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* Customer Name Cell */
.customer-name {
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

.customer-name__text {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.customer-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Due Date Cell */
.due-date {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.date {
  color: var(--color-text-primary);
}

.date--overdue {
  color: var(--color-red-600);
  font-weight: var(--font-weight-semibold);
}

.days-overdue {
  font-size: var(--font-size-xs);
  color: var(--color-red-500);
  font-weight: var(--font-weight-medium);
}

/* Amount Cell */
.amount {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.amount--overdue {
  color: var(--color-red-600);
}

/* Days Until Cell */
.days-until {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.days-value {
  font-size: var(--font-size-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

.days-value.overdue {
  color: var(--color-red-600);
}

/* Actions */
.invoice-actions {
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

.empty-state__icon {
  font-size: 4rem;
  margin-bottom: var(--space-4);
  opacity: 0.5;
  color: var(--color-green-500);
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

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }

  .page-header__actions {
    align-self: flex-start;
  }

  .summary-cards {
    grid-template-columns: 1fr;
  }

  .filters-row {
    flex-direction: column;
    gap: var(--space-3);
  }

  .filters-row .p-inputtext {
    min-width: unset;
    width: 100%;
  }

  .filters-row > * {
    width: 100%;
  }

  .quick-filter {
    margin-left: 0;
    width: 100%;
  }

  .quick-filter button {
    width: 100%;
  }

  .customer-name {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }

  .customer-avatar {
    width: 32px;
    height: 32px;
  }

  .customer-name__text {
    font-size: var(--font-size-sm);
  }

  .customer-id {
    font-size: var(--font-size-xs);
  }
}

/* Tablet adjustments */
@media (min-width: 769px) and (max-width: 1024px) {
  .customer-name {
    gap: var(--space-2);
  }

  .customer-avatar {
    width: 36px;
    height: 36px;
  }
}
</style>
