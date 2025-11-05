<template>
  <div class="billing-dashboard-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">Billing Dashboard</h1>
        <p class="page-subtitle">
          Monitor usage, revenue, and billing cycles in real-time
        </p>
      </div>
      <div class="page-header__actions">
        <Button
          label="Refresh"
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          @click="refreshData"
          :loading="loading"
        />
        <Button
          label="New Cycle"
          icon="pi pi-plus"
          severity="primary"
          @click="navigateTo('/billing/cycles/create')"
        />
      </div>
    </div>

    <!-- KPI Statistics -->
    <div class="kpi-section">
      <h2 class="section-title">Key Performance Indicators</h2>
      <div class="kpi-grid">
        <div class="kpi-card kpi-card--revenue">
          <div class="kpi-card__icon">
            <i class="pi pi-dollar"></i>
          </div>
          <div class="kpi-card__content">
            <div class="kpi-card__value">{{ formatCurrency(totalRevenue) }}</div>
            <div class="kpi-card__label">Total Revenue</div>
            <div class="kpi-card__trend" :class="{ positive: revenueTrend > 0, negative: revenueTrend < 0 }">
              <i :class="revenueTrend > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'"></i>
              <span>{{ Math.abs(revenueTrend) }}% from last month</span>
            </div>
          </div>
        </div>

        <div class="kpi-card kpi-card--cycles">
          <div class="kpi-card__icon">
            <i class="pi pi-calendar"></i>
          </div>
          <div class="kpi-card__content">
            <div class="kpi-card__value">{{ billingCycles.length }}</div>
            <div class="kpi-card__label">Total Cycles</div>
            <div class="kpi-card__sublabel">
              {{ completedBillingCycles.length }} completed
            </div>
          </div>
        </div>

        <div class="kpi-card kpi-card--usage">
          <div class="kpi-card__icon">
            <i class="pi pi-chart-line"></i>
          </div>
          <div class="kpi-card__content">
            <div class="kpi-card__value">{{ usageRecords.length }}</div>
            <div class="kpi-card__label">Total Usage Records</div>
            <div class="kpi-card__sublabel">
              {{ unratedUsageRecords.length }} unrated
            </div>
          </div>
        </div>

        <div class="kpi-card kpi-card--pending">
          <div class="kpi-card__icon">
            <i class="pi pi-clock"></i>
          </div>
          <div class="kpi-card__content">
            <div class="kpi-card__value">{{ pendingBillingCycles.length }}</div>
            <div class="kpi-card__label">Pending Cycles</div>
            <div class="kpi-card__sublabel">
              {{ processingBillingCycles.length }} processing
            </div>
          </div>
        </div>

        <div class="kpi-card kpi-card--rated">
          <div class="kpi-card__icon">
            <i class="pi pi-check-circle"></i>
          </div>
          <div class="kpi-card__content">
            <div class="kpi-card__value">{{ ratedUsageRecords.length }}</div>
            <div class="kpi-card__label">Rated Records</div>
            <div class="kpi-card__sublabel">
              {{ formatPercentage(ratedPercentage) }} of total
            </div>
          </div>
        </div>

        <div class="kpi-card kpi-card--average">
          <div class="kpi-card__icon">
            <i class="pi pi-bar-chart"></i>
          </div>
          <div class="kpi-card__content">
            <div class="kpi-card__value">{{ formatCurrency(averageCycleValue) }}</div>
            <div class="kpi-card__label">Avg. Cycle Value</div>
            <div class="kpi-card__sublabel">
              Per billing cycle
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Revenue Charts Section -->
    <div class="charts-section">
      <h2 class="section-title">Revenue Analytics</h2>
      <div class="charts-grid">
        <!-- Monthly Revenue Chart -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h3 class="chart-card__title">
              <i class="pi pi-chart-bar"></i>
              Monthly Revenue
            </h3>
            <div class="chart-card__actions">
              <Button
                icon="pi pi-ellipsis-v"
                severity="secondary"
                text
                rounded
              />
            </div>
          </div>
          <div class="chart-card__body">
            <RevenueLineChart :data="monthlyRevenueData" :width="800" :height="300" color="#3b82f6" />
            <div class="chart-stats">
              <div class="chart-stat">
                <span class="chart-stat__label">This Month:</span>
                <span class="chart-stat__value">{{ formatCurrency(currentMonthRevenue) }}</span>
              </div>
              <div class="chart-stat">
                <span class="chart-stat__label">Last Month:</span>
                <span class="chart-stat__value">{{ formatCurrency(lastMonthRevenue) }}</span>
              </div>
              <div class="chart-stat">
                <span class="chart-stat__label">Growth:</span>
                <span class="chart-stat__value" :class="{ positive: revenueTrend > 0, negative: revenueTrend < 0 }">
                  {{ revenueTrend > 0 ? '+' : '' }}{{ revenueTrend }}%
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Usage by Type Chart -->
        <div class="chart-card">
          <div class="chart-card__header">
            <h3 class="chart-card__title">
              <i class="pi pi-chart-pie"></i>
              Usage by Type
            </h3>
            <div class="chart-card__actions">
              <Button
                icon="pi pi-ellipsis-v"
                severity="secondary"
                text
                rounded
              />
            </div>
          </div>
          <div class="chart-card__body">
            <div style="display: flex; gap: 20px; align-items: center;">
              <UsagePieChart :data="usageByTypeData" :width="300" :height="300" />
              <div class="usage-breakdown" style="flex: 1;">
                <div class="usage-item" v-for="(count, type) in usageByType" :key="type">
                  <div class="usage-item__label">
                    <i :class="getUsageTypeIcon(type)" class="usage-item__icon"></i>
                    <span>{{ getUsageTypeLabel(type) }}</span>
                  </div>
                  <span class="usage-item__value">{{ count }} records</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Billing Cycle Status -->
        <div class="chart-card chart-card--full">
          <div class="chart-card__header">
            <h3 class="chart-card__title">
              <i class="pi pi-calendar"></i>
              Billing Cycles Overview
            </h3>
            <div class="chart-card__actions">
              <Button
                icon="pi pi-ellipsis-v"
                severity="secondary"
                text
                rounded
              />
            </div>
          </div>
          <div class="chart-card__body">
            <CyclesBarChart :data="cyclesByStatusData" :width="800" :height="250" />
            <div class="cycles-overview" style="margin-top: 20px; display: flex; gap: 30px; justify-content: center;">
              <div class="cycle-status-item" v-for="(cycles, status) in cyclesByStatus" :key="status">
                <div class="cycle-status-item__header">
                  <i :class="getCycleStatusIcon(status)" class="cycle-status-item__icon"></i>
                  <span class="cycle-status-item__label">{{ getBillingCycleStatusLabel(status) }}</span>
                </div>
                <div class="cycle-status-item__value">{{ cycles.length }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Alerts Section -->
    <div class="alerts-section">
      <h2 class="section-title">Billing Alerts</h2>
      <div class="alerts-list">
        <!-- High Priority Alert -->
        <div v-if="unratedUsageRecords.length > 100" class="alert alert--high">
          <div class="alert__icon">
            <i class="pi pi-exclamation-triangle"></i>
          </div>
          <div class="alert__content">
            <h4 class="alert__title">High Unrated Usage Volume</h4>
            <p class="alert__message">
              You have {{ unratedUsageRecords.length }} unrated usage records that require attention.
              This may impact billing accuracy.
            </p>
          </div>
          <div class="alert__actions">
            <NuxtLink to="/billing/usage-records?unrated=true">
              <Button label="Review" severity="danger" size="small" outlined />
            </NuxtLink>
          </div>
        </div>

        <!-- Processing Cycle Alert -->
        <div v-if="currentCycle" class="alert alert--info">
          <div class="alert__icon">
            <i class="pi pi-info-circle"></i>
          </div>
          <div class="alert__content">
            <h4 class="alert__title">Billing Cycle in Progress</h4>
            <p class="alert__message">
              Cycle #{{ currentCycle.cycleNumber }} is currently processing.
              Started: {{ formatDate(currentCycle.startDate) }}
            </p>
          </div>
          <div class="alert__actions">
            <NuxtLink :to="`/billing/cycles/${currentCycle.id}`">
              <Button label="View" severity="info" size="small" outlined />
            </NuxtLink>
          </div>
        </div>

        <!-- Next Cycle Alert -->
        <div v-if="nextCycle" class="alert alert--warning">
          <div class="alert__icon">
            <i class="pi pi-clock"></i>
          </div>
          <div class="alert__content">
            <h4 class="alert__title">Upcoming Billing Cycle</h4>
            <p class="alert__message">
              Next cycle scheduled to start: {{ formatDate(nextCycle.startDate) }}
            </p>
          </div>
          <div class="alert__actions">
            <NuxtLink :to="`/billing/cycles/${nextCycle.id}`">
              <Button label="Configure" severity="warning" size="small" outlined />
            </NuxtLink>
          </div>
        </div>

        <!-- Failed Cycles Alert -->
        <div v-if="failedCycles.length > 0" class="alert alert--danger">
          <div class="alert__icon">
            <i class="pi pi-times-circle"></i>
          </div>
          <div class="alert__content">
            <h4 class="alert__title">Failed Billing Cycles</h4>
            <p class="alert__message">
              {{ failedCycles.length }} cycle(s) failed to process and require manual intervention.
            </p>
          </div>
          <div class="alert__actions">
            <NuxtLink to="/billing/cycles?status=FAILED">
              <Button label="Review" severity="danger" size="small" outlined />
            </NuxtLink>
          </div>
        </div>

        <!-- No Alerts -->
        <div v-if="alerts.length === 0" class="alert alert--success">
          <div class="alert__icon">
            <i class="pi pi-check-circle"></i>
          </div>
          <div class="alert__content">
            <h4 class="alert__title">All Clear</h4>
            <p class="alert__message">
              No billing issues requiring attention at this time.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Activity -->
    <div class="activity-section">
      <h2 class="section-title">Recent Activity</h2>
      <div class="activity-list">
        <div
          v-for="activity in recentActivity"
          :key="activity.id"
          class="activity-item"
        >
          <div class="activity-item__icon" :class="`activity-item__icon--${activity.type}`">
            <i :class="getActivityIcon(activity.type)"></i>
          </div>
          <div class="activity-item__content">
            <div class="activity-item__title">{{ activity.title }}</div>
            <div class="activity-item__description">{{ activity.description }}</div>
            <div class="activity-item__time">{{ activity.timestamp }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useBillingStore } from '~/stores/billing'
import { useToast } from 'primevue/usetoast'
import type { UsageRecord, BillingCycle, UsageType, BillingCycleStatus } from '~/schemas/billing'
import {
  getUsageTypeLabel,
  getBillingCycleStatusLabel,
  formatUsageAmount,
  calculateTotalCost
} from '~/schemas/billing'
import RevenueLineChart from '~/components/charts/RevenueLineChart.vue'
import UsagePieChart from '~/components/charts/UsagePieChart.vue'
import CyclesBarChart from '~/components/charts/CyclesBarChart.vue'

// Meta
definePageMeta({
  title: 'Billing Dashboard'
})

// Stores & Composables
const billingStore = useBillingStore()
const toast = useToast()

// Reactive State
const loading = ref(false)

// Computed Properties
const usageRecords = computed(() => billingStore.usageRecords)
const billingCycles = computed(() => billingStore.billingCycles)
const currentCycle = computed(() => billingStore.currentCycle)
const nextCycle = computed(() => billingStore.nextCycle)
const unratedUsageRecords = computed(() => billingStore.unratedUsageRecords)
const ratedUsageRecords = computed(() => billingStore.ratedUsageRecords)
const pendingBillingCycles = computed(() => billingStore.pendingBillingCycles)
const processingBillingCycles = computed(() => billingStore.processingBillingCycles)
const completedBillingCycles = computed(() => billingStore.completedBillingCycles)

const totalRevenue = computed(() => {
  return billingCycles.value.reduce((sum, cycle) => sum + (cycle.totalCost || 0), 0)
})

const averageCycleValue = computed(() => {
  if (billingCycles.value.length === 0) return 0
  return totalRevenue.value / billingCycles.value.length
})

const ratedPercentage = computed(() => {
  if (usageRecords.value.length === 0) return 0
  return (ratedUsageRecords.value.length / usageRecords.value.length) * 100
})

const revenueTrend = computed(() => {
  // Mock data - in real implementation, compare with previous period
  return 12.5
})

const currentMonthRevenue = computed(() => {
  // Mock data - current month revenue
  return 125000
})

const lastMonthRevenue = computed(() => {
  // Mock data - last month revenue
  return 111000
})

const usageByType = computed(() => {
  const counts: Record<UsageType, number> = {
    [UsageType.VOICE]: 0,
    [UsageType.SMS]: 0,
    [UsageType.DATA]: 0,
    [UsageType.SERVICE]: 0
  }

  usageRecords.value.forEach(record => {
    counts[record.usageType]++
  })

  return counts
})

const cyclesByStatus = computed(() => {
  const groups: Record<BillingCycleStatus, BillingCycle[]> = {
    [BillingCycleStatus.PENDING]: [],
    [BillingCycleStatus.SCHEDULED]: [],
    [BillingCycleStatus.PROCESSING]: [],
    [BillingCycleStatus.COMPLETED]: [],
    [BillingCycleStatus.FAILED]: [],
    [BillingCycleStatus.CANCELLED]: []
  }

  billingCycles.value.forEach(cycle => {
    groups[cycle.status].push(cycle)
  })

  return groups
})

const failedCycles = computed(() => {
  return billingCycles.value.filter(c => c.status === BillingCycleStatus.FAILED)
})

const alerts = computed(() => {
  const alertList = []

  if (unratedUsageRecords.value.length > 100) {
    alertList.push({ type: 'high', id: 'unrated' })
  }

  if (currentCycle.value) {
    alertList.push({ type: 'info', id: 'processing' })
  }

  if (nextCycle.value) {
    alertList.push({ type: 'warning', id: 'upcoming' })
  }

  if (failedCycles.value.length > 0) {
    alertList.push({ type: 'danger', id: 'failed' })
  }

  return alertList
})

const recentActivity = computed(() => {
  // Mock recent activity - in real app, fetch from API
  return [
    {
      id: '1',
      type: 'cycle',
      title: 'Billing Cycle Completed',
      description: 'Cycle #2024-11 completed successfully with $12,450 revenue',
      timestamp: '2 hours ago'
    },
    {
      id: '2',
      type: 'usage',
      title: 'Usage Records Ingested',
      description: `1,250 new usage records ingested for processing`,
      timestamp: '4 hours ago'
    },
    {
      id: '3',
      type: 'cycle',
      title: 'Billing Cycle Started',
      description: 'Cycle #2024-12 started for 45 active subscriptions',
      timestamp: '1 day ago'
    },
    {
      id: '4',
      type: 'alert',
      title: 'System Alert',
      description: 'High volume of voice usage detected - unusual pattern',
      timestamp: '2 days ago'
    }
  ]
})

// Chart Data
const monthlyRevenueData = computed(() => {
  // Mock data - 12 months of revenue
  return [
    { month: 'Jan', revenue: 85000, label: 'Jan' },
    { month: 'Feb', revenue: 92000, label: 'Feb' },
    { month: 'Mar', revenue: 88000, label: 'Mar' },
    { month: 'Apr', revenue: 95000, label: 'Apr' },
    { month: 'May', revenue: 102000, label: 'May' },
    { month: 'Jun', revenue: 98000, label: 'Jun' },
    { month: 'Jul', revenue: 105000, label: 'Jul' },
    { month: 'Aug', revenue: 110000, label: 'Aug' },
    { month: 'Sep', revenue: 115000, label: 'Sep' },
    { month: 'Oct', revenue: 120000, label: 'Oct' },
    { month: 'Nov', revenue: 125000, label: 'Nov' },
    { month: 'Dec', revenue: 130000, label: 'Dec' }
  ]
})

const usageByTypeData = computed(() => {
  return [
    { label: 'Voice', value: usageByType.value.VOICE },
    { label: 'SMS', value: usageByType.value.SMS },
    { label: 'Data', value: usageByType.value.DATA },
    { label: 'Service', value: usageByType.value.SERVICE }
  ]
})

const cyclesByStatusData = computed(() => {
  return [
    { status: 'PENDING', count: cyclesByStatus.value.PENDING.length, label: 'Pending', color: '#f59e0b' },
    { status: 'SCHEDULED', count: cyclesByStatus.value.SCHEDULED.length, label: 'Scheduled', color: '#3b82f6' },
    { status: 'PROCESSING', count: cyclesByStatus.value.PROCESSING.length, label: 'Processing', color: '#8b5cf6' },
    { status: 'COMPLETED', count: cyclesByStatus.value.COMPLETED.length, label: 'Completed', color: '#10b981' },
    { status: 'FAILED', count: cyclesByStatus.value.FAILED.length, label: 'Failed', color: '#ef4444' }
  ]
})

// Helper Functions
function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(amount)
}

function formatPercentage(value: number): string {
  return `${value.toFixed(1)}%`
}

function formatDate(date: string | Date): string {
  return new Date(date).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

function getUsageTypeLabel(type: string): string {
  const typeMap: Record<string, string> = {
    VOICE: 'Voice',
    SMS: 'SMS',
    DATA: 'Data',
    SERVICE: 'Service'
  }
  return typeMap[type] || type
}

function getUsageTypeIcon(type: string): string {
  const iconMap: Record<string, string> = {
    VOICE: 'pi pi-phone',
    SMS: 'pi pi-comment',
    DATA: 'pi pi-download',
    SERVICE: 'pi pi-cog'
  }
  return iconMap[type] || 'pi pi-circle'
}

function getBillingCycleStatusLabel(status: string): string {
  const statusMap: Record<string, string> = {
    PENDING: 'Pending',
    SCHEDULED: 'Scheduled',
    PROCESSING: 'Processing',
    COMPLETED: 'Completed',
    FAILED: 'Failed',
    CANCELLED: 'Cancelled'
  }
  return statusMap[status] || status
}

function getCycleStatusIcon(status: string): string {
  const iconMap: Record<string, string> = {
    PENDING: 'pi pi-clock',
    SCHEDULED: 'pi pi-calendar-plus',
    PROCESSING: 'pi pi-spin pi-spinner',
    COMPLETED: 'pi pi-check-circle',
    FAILED: 'pi pi-times-circle',
    CANCELLED: 'pi pi-ban'
  }
  return iconMap[status] || 'pi pi-circle'
}

function getActivityIcon(type: string): string {
  const iconMap: Record<string, string> = {
    cycle: 'pi pi-calendar',
    usage: 'pi pi-chart-line',
    alert: 'pi pi-bell'
  }
  return iconMap[type] || 'pi pi-circle'
}

// Event Handlers
async function refreshData() {
  try {
    loading.value = true
    await Promise.all([
      billingStore.fetchUsageRecords(),
      billingStore.fetchBillingCycles()
    ])
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Dashboard data refreshed',
      life: 3000
    })
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to refresh data',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

// Lifecycle
onMounted(async () => {
  try {
    loading.value = true
    await Promise.all([
      billingStore.fetchUsageRecords(),
      billingStore.fetchBillingCycles()
    ])
  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to load dashboard data',
      life: 5000
    })
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.billing-dashboard-page {
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
  display: flex;
  gap: var(--space-2);
}

/* Section Title */
.section-title {
  margin: 0 0 var(--space-4) 0;
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

/* KPI Section */
.kpi-section {
  display: flex;
  flex-direction: column;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-4);
}

.kpi-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  gap: var(--space-3);
  transition: all 0.3s;
}

.kpi-card:hover {
  border-color: var(--color-primary);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.kpi-card__icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  background: var(--color-primary-100);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 1.75rem;
  flex-shrink: 0;
}

.kpi-card--revenue .kpi-card__icon {
  background: var(--green-100);
  color: var(--green-600);
}

.kpi-card--cycles .kpi-card__icon {
  background: var(--blue-100);
  color: var(--blue-600);
}

.kpi-card--usage .kpi-card__icon {
  background: var(--purple-100);
  color: var(--purple-600);
}

.kpi-card--pending .kpi-card__icon {
  background: var(--orange-100);
  color: var(--orange-600);
}

.kpi-card--rated .kpi-card__icon {
  background: var(--green-100);
  color: var(--green-600);
}

.kpi-card--average .kpi-card__icon {
  background: var(--blue-100);
  color: var(--blue-600);
}

.kpi-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.kpi-card__value {
  font-size: var(--font-size-2xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  line-height: 1;
}

.kpi-card__label {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
}

.kpi-card__sublabel {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: var(--space-1);
}

.kpi-card__trend {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  margin-top: var(--space-1);
}

.kpi-card__trend.positive {
  color: var(--green-600);
}

.kpi-card__trend.negative {
  color: var(--red-600);
}

/* Charts Section */
.charts-section {
  display: flex;
  flex-direction: column;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: var(--space-4);
}

.chart-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chart-card--full {
  grid-column: 1 / -1;
}

.chart-card__header {
  padding: var(--space-4);
  background: var(--color-surface-secondary);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-card__title {
  margin: 0;
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.chart-card__body {
  padding: var(--space-6);
  flex: 1;
}

.chart-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 250px;
  text-align: center;
}

.chart-icon {
  font-size: 4rem;
  color: var(--color-text-secondary);
  opacity: 0.3;
  margin-bottom: var(--space-4);
}

.chart-message {
  margin: 0 0 var(--space-4) 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.chart-stats {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  width: 100%;
  max-width: 300px;
}

.chart-stat {
  display: flex;
  justify-content: space-between;
  padding: var(--space-2) var(--space-3);
  background: var(--color-surface-secondary);
  border-radius: var(--radius-md);
}

.chart-stat__label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.chart-stat__value {
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.chart-stat__value.positive {
  color: var(--green-600);
}

.chart-stat__value.negative {
  color: var(--red-600);
}

.usage-breakdown {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.usage-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-3);
  background: var(--color-surface-secondary);
  border-radius: var(--radius-md);
}

.usage-item__label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.usage-item__icon {
  color: var(--color-primary);
}

.usage-item__value {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.cycles-overview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--space-3);
}

.cycle-status-item {
  padding: var(--space-3);
  background: var(--color-surface-secondary);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  align-items: center;
  text-align: center;
}

.cycle-status-item__header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
}

.cycle-status-item__icon {
  color: var(--color-primary);
  font-size: 1.25rem;
}

.cycle-status-item__label {
  font-size: var(--font-size-sm);
}

.cycle-status-item__value {
  font-size: var(--font-size-xl);
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
}

/* Alerts Section */
.alerts-section {
  display: flex;
  flex-direction: column;
}

.alerts-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.alert {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.alert--high {
  border-left: 4px solid var(--red-500);
  background: var(--red-50);
}

.alert--info {
  border-left: 4px solid var(--blue-500);
  background: var(--blue-50);
}

.alert--warning {
  border-left: 4px solid var(--orange-500);
  background: var(--orange-50);
}

.alert--danger {
  border-left: 4px solid var(--red-500);
  background: var(--red-50);
}

.alert--success {
  border-left: 4px solid var(--green-500);
  background: var(--green-50);
}

.alert__icon {
  font-size: 1.75rem;
  flex-shrink: 0;
  margin-top: var(--space-1);
}

.alert--high .alert__icon {
  color: var(--red-600);
}

.alert--info .alert__icon {
  color: var(--blue-600);
}

.alert--warning .alert__icon {
  color: var(--orange-600);
}

.alert--danger .alert__icon {
  color: var(--red-600);
}

.alert--success .alert__icon {
  color: var(--green-600);
}

.alert__content {
  flex: 1;
}

.alert__title {
  margin: 0 0 var(--space-1) 0;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.alert__message {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.alert__actions {
  flex-shrink: 0;
}

/* Activity Section */
.activity-section {
  display: flex;
  flex-direction: column;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.activity-item {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
  display: flex;
  gap: var(--space-3);
  transition: all 0.2s;
}

.activity-item:hover {
  border-color: var(--color-primary);
}

.activity-item__icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  background: var(--color-surface-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 1.25rem;
  flex-shrink: 0;
}

.activity-item__icon--cycle {
  background: var(--blue-100);
  color: var(--blue-600);
}

.activity-item__icon--usage {
  background: var(--purple-100);
  color: var(--purple-600);
}

.activity-item__icon--alert {
  background: var(--orange-100);
  color: var(--orange-600);
}

.activity-item__content {
  flex: 1;
}

.activity-item__title {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.activity-item__description {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-1);
}

.activity-item__time {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-header__actions {
    width: 100%;
  }

  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }

  .cycles-overview {
    grid-template-columns: 1fr;
  }
}
</style>
