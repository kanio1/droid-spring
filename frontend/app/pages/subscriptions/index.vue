<template>
  <div class="subscriptions-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Subscriptions</h1>
        <p class="page-subtitle">Manage customer subscriptions and renewals</p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Create Subscription"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/subscriptions/create')"
        />
      </div>
    </div>

    <!-- Filters and Search -->
    <div class="subscriptions-filters">
      <div class="filters-row">
        <span class="p-input-icon-left">
          <i class="pi pi-search" />
          <InputText
            v-model="searchTerm"
            placeholder="Search subscriptions..."
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
          style="width: 180px"
          @change="handleStatusFilter"
        />

        <Dropdown
          v-model="productFilter"
          :options="productOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="All Products"
          style="width: 180px"
          @change="handleProductFilter"
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

    <!-- Subscriptions Table -->
    <div class="subscriptions-table">
      <AppTable
        :columns="tableColumns"
        :data="subscriptions"
        :loading="subscriptionStore.loading"
        :show-pagination="true"
        :pagination="paginationProps"
        @page="handlePage"
        @sort="handleSort"
        @row-click="handleView"
      >
        <!-- Subscription Number column -->
        <template #cell-subscriptionNumber="{ row }">
          <div class="subscription-number">
            <span class="subscription-number__text">{{ row.subscriptionNumber }}</span>
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

        <!-- Product column -->
        <template #cell-productName="{ row }">
          <div class="product-info">
            <div class="product-name">{{ row.productName || 'N/A' }}</div>
            <div class="product-id">{{ row.productId }}</div>
          </div>
        </template>

        <!-- Status column -->
        <template #cell-status="{ row }">
          <StatusBadge :status="row.status" type="subscription" size="small" />
        </template>

        <!-- Period column -->
        <template #cell-billingPeriod="{ row }">
          <span class="period">
            {{ formatBillingPeriod(row.billingPeriod) }}
          </span>
        </template>

        <!-- Next Billing column -->
        <template #cell-nextBillingDate="{ row }">
          <div class="next-billing">
            <span v-if="row.nextBillingDate" class="date">
              {{ formatDate(row.nextBillingDate) }}
            </span>
            <span v-else class="date--empty">—</span>
            <i v-if="isExpiringSoon(row)" class="pi pi-exclamation-triangle text-warning text-xs ml-1"></i>
          </div>
        </template>

        <!-- Amount column -->
        <template #cell-amount="{ row }">
          <span v-if="row.amount" class="amount">
            {{ formatCurrency(row.amount, row.currency) }}
          </span>
          <span v-else class="amount--empty">—</span>
        </template>

        <!-- Auto Renew column -->
        <template #cell-autoRenew="{ row }">
          <div class="auto-renew">
            <i :class="row.autoRenew ? 'pi pi-check-circle text-success' : 'pi pi-times-circle text-muted'"></i>
            <span class="ml-2">{{ row.autoRenew ? 'Yes' : 'No' }}</span>
          </div>
        </template>

        <!-- Actions column -->
        <template #cell-actions="{ row }">
          <div class="subscription-actions">
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
              v-tooltip.top="'Edit subscription'"
              v-if="canEditSubscription(row)"
            />
            <Button
              icon="pi pi-refresh"
              text
              rounded
              severity="info"
              @click.stop="handleRenew(row)"
              v-tooltip.top="'Renew subscription'"
              v-if="canRenewSubscription(row)"
            />
            <Button
              icon="pi pi-times"
              text
              rounded
              severity="danger"
              @click.stop="handleCancel(row)"
              v-tooltip.top="'Cancel subscription'"
              v-if="canCancelSubscription(row)"
            />
          </div>
        </template>

        <!-- Empty state -->
        <template #empty>
          <div class="empty-state">
            <i class="pi pi-sync empty-state__icon"></i>
            <h3 class="empty-state__title">No subscriptions found</h3>
            <p class="empty-state__description">
              {{ searchTerm || statusFilter || productFilter ?
                'Try adjusting your search criteria' :
                'Get started by creating your first subscription'
              }}
            </p>
            <Button
              v-if="!searchTerm && !statusFilter && !productFilter"
              label="Create First Subscription"
              icon="pi pi-plus"
              severity="primary"
              @click="navigateTo('/subscriptions/create')"
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
import { useSubscriptionStore } from '~/stores/subscription'
import { canCancelSubscription, canRenewSubscription } from '~/schemas/subscription'

// Page meta
definePageMeta({
  title: 'Subscriptions'
})

// Store
const subscriptionStore = useSubscriptionStore()
const { showToast } = useToast()
const route = useRoute()

// Reactive state
const searchTerm = ref('')
const statusFilter = ref('')
const productFilter = ref('')
const sortOption = ref('createdAt,desc')

// Table columns configuration
const tableColumns = [
  {
    key: 'subscriptionNumber',
    label: 'Subscription #',
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
    key: 'productName',
    label: 'Product',
    sortable: false,
    style: 'width: 15%'
  },
  {
    key: 'status',
    label: 'Status',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'billingPeriod',
    label: 'Period',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'nextBillingDate',
    label: 'Next Billing',
    sortable: true,
    style: 'width: 12%'
  },
  {
    key: 'amount',
    label: 'Amount',
    sortable: true,
    style: 'width: 10%',
    align: 'right'
  },
  {
    key: 'autoRenew',
    label: 'Auto Renew',
    sortable: true,
    style: 'width: 10%'
  },
  {
    key: 'actions',
    label: 'Actions',
    sortable: false,
    style: 'width: 10%'
  }
]

// Filter options
const statusOptions = [
  { label: 'All Statuses', value: '' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Suspended', value: 'SUSPENDED' },
  { label: 'Cancelled', value: 'CANCELLED' },
  { label: 'Expired', value: 'EXPIRED' }
]

const productOptions = [
  { label: 'All Products', value: '' },
  { label: 'Internet Basic', value: 'INTERNET_BASIC' },
  { label: 'Internet Pro', value: 'INTERNET_PRO' },
  { label: 'Phone Unlimited', value: 'PHONE_UNLIMITED' },
  { label: 'TV Premium', value: 'TV_PREMIUM' }
]

const sortOptions = [
  { label: 'Newest First', value: 'createdAt,desc' },
  { label: 'Oldest First', value: 'createdAt,asc' },
  { label: 'Subscription # A-Z', value: 'subscriptionNumber,asc' },
  { label: 'Next Billing Date', value: 'nextBillingDate,asc' },
  { label: 'Amount High-Low', value: 'amount,desc' },
  { label: 'Amount Low-High', value: 'amount,asc' }
]

// Computed
const subscriptions = computed(() => subscriptionStore.subscriptions)
const paginationProps = computed(() => ({
  page: subscriptionStore.pagination.page,
  size: subscriptionStore.pagination.size,
  total: subscriptionStore.pagination.totalElements
}))

// Methods
const handleSearch = useDebounceFn(async () => {
  await subscriptionStore.fetchSubscriptions({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    productId: productFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}, 300)

const handleStatusFilter = async () => {
  await subscriptionStore.fetchSubscriptions({
    page: 0,
    sort: sortOption.value,
    productId: productFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    status: statusFilter.value || undefined
  })
}

const handleProductFilter = async () => {
  await subscriptionStore.fetchSubscriptions({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    searchTerm: searchTerm.value || undefined,
    productId: productFilter.value || undefined
  })
}

const handleSortChange = async () => {
  await subscriptionStore.fetchSubscriptions({
    page: 0,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    productId: productFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleSort = async ({ field, order }: { field: string; order: number }) => {
  await subscriptionStore.fetchSubscriptions({
    page: subscriptionStore.pagination.page,
    sort: `${field},${order === -1 ? 'desc' : 'asc'}`,
    status: statusFilter.value || undefined,
    productId: productFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handlePage = async ({ page, rows }: { page: number; rows: number }) => {
  await subscriptionStore.fetchSubscriptions({
    page,
    size: rows,
    sort: sortOption.value,
    status: statusFilter.value || undefined,
    productId: productFilter.value || undefined,
    searchTerm: searchTerm.value || undefined
  })
}

const handleView = (row: any) => {
  navigateTo(`/subscriptions/${row.id}`)
}

const handleEdit = (row: any) => {
  navigateTo(`/subscriptions/${row.id}/edit`)
}

const handleRenew = async (row: any) => {
  try {
    await subscriptionStore.renewSubscription({
      id: row.id
    })

    showToast({
      severity: 'success',
      summary: 'Subscription Renewed',
      detail: `Subscription ${row.subscriptionNumber} has been renewed.`,
      life: 3000
    })

    await subscriptionStore.fetchSubscriptions({
      status: statusFilter.value || undefined,
      productId: productFilter.value || undefined,
      searchTerm: searchTerm.value || undefined
    })

  } catch (error: any) {
    showToast({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to renew subscription',
      life: 5000
    })
  }
}

const handleCancel = async (row: any) => {
  const confirmed = confirm(`Are you sure you want to cancel subscription ${row.subscriptionNumber}?`)

  if (confirmed) {
    try {
      await subscriptionStore.changeSubscriptionStatus({
        id: row.id,
        status: 'CANCELLED'
      })

      showToast({
        severity: 'success',
        summary: 'Subscription Cancelled',
        detail: `Subscription ${row.subscriptionNumber} has been cancelled.`,
        life: 3000
      })

      await subscriptionStore.fetchSubscriptions({
        status: statusFilter.value || undefined,
        productId: productFilter.value || undefined,
        searchTerm: searchTerm.value || undefined
      })

    } catch (error: any) {
      showToast({
        severity: 'error',
        summary: 'Error',
        detail: error.message || 'Failed to cancel subscription',
        life: 5000
      })
    }
  }
}

const canEditSubscription = (row: any) => {
  return row.status === 'ACTIVE' || row.status === 'SUSPENDED'
}

const canRenewSubscriptionFn = (row: any) => {
  return canRenewSubscription(row)
}

const canCancelSubscriptionFn = (row: any) => {
  return canCancelSubscription(row)
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

const formatCurrency = (amount: number, currency: string): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency
  }).format(amount)
}

const formatBillingPeriod = (period: string): string => {
  const periods: Record<string, string> = {
    MONTHLY: 'Monthly',
    QUARTERLY: 'Quarterly',
    YEARLY: 'Yearly'
  }
  return periods[period] || period
}

const isExpiringSoon = (row: any): boolean => {
  if (!row.nextBillingDate) return false
  const today = new Date()
  const nextBilling = new Date(row.nextBillingDate)
  const diffTime = nextBilling.getTime() - today.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))
  return diffDays <= 30 && diffDays >= 0
}

// Lifecycle
onMounted(async () => {
  await subscriptionStore.fetchSubscriptions()
})

// Watch for route changes to refresh data
watch(() => route.fullPath, async () => {
  await subscriptionStore.fetchSubscriptions()
})
</script>

<style scoped>
.subscriptions-page {
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
.subscriptions-filters {
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

/* Table */
.subscriptions-table {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

/* Subscription Number Cell */
.subscription-number__text {
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

/* Product Info */
.product-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.product-name {
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.product-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
}

/* Period Cell */
.period {
  color: var(--color-text-primary);
  font-weight: var(--font-weight-medium);
}

/* Next Billing Cell */
.next-billing {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.date {
  color: var(--color-text-primary);
}

.date--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Amount Cell */
.amount {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.amount--empty {
  color: var(--color-text-muted);
  font-style: italic;
}

/* Auto Renew Cell */
.auto-renew {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

/* Actions */
.subscription-actions {
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
