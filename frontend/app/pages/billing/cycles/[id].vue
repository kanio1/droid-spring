<template>
  <div class="cycle-detail-page">
    <div class="page-header">
      <NuxtLink to="/billing/cycles" class="back-link">← Back to Billing Cycles</NuxtLink>
      <div class="title-row">
        <h1 class="page-title">Billing Cycle {{ cycle?.cycleName || '...' }}</h1>
        <StatusBadge v-if="cycle" :status="cycle.status" type="billing-cycle" size="large" />
      </div>
      <p class="page-subtitle" v-if="cycle">{{ formatDate(cycle.startDate) }} to {{ formatDate(cycle.endDate) }}</p>
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner />
      <p>Loading billing cycle details...</p>
    </div>

    <div v-else-if="cycle" class="cycle-content">
      <div class="card cycle-summary">
        <div class="card-header"><h2>Cycle Summary</h2></div>
        <div class="card-body">
          <div class="summary-grid">
            <div class="summary-item">
              <label>Cycle Name</label>
              <span>{{ cycle.cycleName }}</span>
            </div>
            <div class="summary-item">
              <label>Customer</label>
              <span>{{ cycle.customerName }}</span>
            </div>
            <div class="summary-item">
              <label>Period</label>
              <span>{{ formatDate(cycle.startDate) }} - {{ formatDate(cycle.endDate) }}</span>
            </div>
            <div class="summary-item">
              <label>Total Amount</label>
              <span class="amount">{{ formatCurrency(cycle.totalAmount, cycle.currency) }}</span>
            </div>
            <div class="summary-item">
              <label>Usage Records</label>
              <span>{{ cycle.usageRecordCount || 0 }}</span>
            </div>
            <div class="summary-item">
              <label>Invoices Generated</label>
              <span>{{ cycle.invoiceCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="card cycle-usage">
        <div class="card-header">
          <h2>Usage Records</h2>
        </div>
        <div class="card-body">
          <AppTable :columns="usageColumns" :data="cycle.usageRecords || []" :show-pagination="false">
            <template #cell-timestamp="{ row }">{{ formatDateTime(row.timestamp) }}</template>
            <template #cell-usageType="{ row }">
              <StatusBadge :status="row.usageType" type="usage-type" size="small" />
            </template>
            <template #cell-usageAmount="{ row }">
              {{ formatUsageAmount(row.usageAmount, row.usageType) }}
            </template>
            <template #cell-ratedCost="{ row }">
              <span v-if="row.ratedCost">{{ formatCurrency(row.ratedCost) }}</span>
              <span v-else class="text-muted">—</span>
            </template>
          </AppTable>
        </div>
      </div>

      <div class="card cycle-invoices">
        <div class="card-header">
          <h2>Generated Invoices</h2>
        </div>
        <div class="card-body">
          <AppTable :columns="invoiceColumns" :data="cycle.invoices || []" :show-pagination="false">
            <template #cell-invoiceNumber="{ row }">
              <NuxtLink :to="`/invoices/${row.id}`" class="invoice-link">{{ row.invoiceNumber }}</NuxtLink>
            </template>
            <template #cell-invoiceDate="{ row }">{{ formatDate(row.invoiceDate) }}</template>
            <template #cell-totalAmount="{ row }">
              <span class="amount">{{ formatCurrency(row.totalAmount) }}</span>
            </template>
            <template #cell-status="{ row }">
              <StatusBadge :status="row.status" type="invoice" size="small" />
            </template>
          </AppTable>
        </div>
      </div>
    </div>

    <Toast />
  </div>
</template>

<script setup lang="ts">
import { useBillingStore } from '~/stores/billing'

definePageMeta({ title: 'Billing Cycle Details' })

const route = useRoute()
const billingStore = useBillingStore()
const { showToast } = useToast()
const cycleId = route.params.id as string

const loading = ref(true)
const cycle = computed(() => billingStore.currentBillingCycle)

const usageColumns = [
  { key: 'timestamp', label: 'Timestamp', style: 'width: 20%' },
  { key: 'usageType', label: 'Type', style: 'width: 15%' },
  { key: 'usageAmount', label: 'Amount', style: 'width: 15%' },
  { key: 'ratedCost', label: 'Cost', style: 'width: 15%' },
  { key: 'destination', label: 'Destination', style: 'width: 35%' }
]

const invoiceColumns = [
  { key: 'invoiceNumber', label: 'Invoice #', style: 'width: 25%' },
  { key: 'invoiceDate', label: 'Date', style: 'width: 20%' },
  { key: 'totalAmount', label: 'Amount', style: 'width: 20%' },
  { key: 'status', label: 'Status', style: 'width: 20%' },
  { key: 'actions', label: 'Actions', style: 'width: 15%' }
]

const fetchCycle = async () => {
  try {
    loading.value = true
    await billingStore.fetchBillingCycleById(cycleId)
  } catch (err: any) {
    showToast({ severity: 'error', summary: 'Error', detail: err.message, life: 5000 })
  } finally {
    loading.value = false
  }
}

const formatDate = (date: string) => new Date(date).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })
const formatDateTime = (date: string) => new Date(date).toLocaleString('en-US')
const formatCurrency = (amount: number, currency = 'USD') => new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amount)
const formatUsageAmount = (amount: number, type: string) => {
  if (type === 'DATA') return `${(amount / (1024 * 1024)).toFixed(2)} MB`
  if (type === 'VOICE') return `${amount} min`
  if (type === 'SMS') return `${amount} SMS`
  return amount.toString()
}

onMounted(fetchCycle)
</script>

<style scoped>
.cycle-detail-page { display: flex; flex-direction: column; gap: var(--space-6); }
.page-header { padding-bottom: var(--space-4); border-bottom: 1px solid var(--color-border); }
.back-link { color: var(--color-primary); text-decoration: none; font-size: var(--font-size-sm); display: inline-flex; align-items: center; gap: var(--space-1); margin-bottom: var(--space-3); }
.title-row { display: flex; align-items: center; gap: var(--space-3); margin-bottom: var(--space-1); }
.page-title { margin: 0; font-size: var(--font-size-3xl); font-weight: var(--font-weight-bold); color: var(--color-text-primary); }
.page-subtitle { margin: 0; color: var(--color-text-secondary); }
.card { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; }
.card-header { padding: var(--space-4) var(--space-6); border-bottom: 1px solid var(--color-border); background: var(--color-surface-secondary); }
.card-header h2 { margin: 0; font-size: var(--font-size-lg); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); }
.card-body { padding: var(--space-6); }
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: var(--space-12); text-align: center; }
.summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: var(--space-4); }
.summary-item { display: flex; flex-direction: column; gap: var(--space-1); }
.summary-item label { font-size: var(--font-size-xs); font-weight: var(--font-weight-medium); color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.05em; }
.summary-item > span { font-size: var(--font-size-base); color: var(--color-text-primary); }
.amount { font-weight: var(--font-weight-semibold); color: var(--color-text-primary); font-size: var(--font-size-lg); }
.invoice-link { color: var(--color-primary); text-decoration: none; font-weight: var(--font-weight-medium); }
.invoice-link:hover { text-decoration: underline; }
@media (max-width: 768px) { .summary-grid { grid-template-columns: 1fr; } }
</style>
