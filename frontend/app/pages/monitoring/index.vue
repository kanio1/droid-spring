<template>
  <div class="monitoring-dashboard">
    <div class="dashboard-header">
      <h1>Resource Monitoring Dashboard</h1>
      <div class="header-actions">
        <Button
          label="Refresh"
          icon="pi pi-refresh"
          :loading="loading"
          @click="refreshData"
        />
        <Button
          label="Settings"
          icon="pi pi-cog"
          outlined
          @click="navigateTo('/monitoring/settings')"
        />
      </div>
    </div>

    <!-- Alert Statistics Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon critical">
          <i class="pi pi-exclamation-triangle"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ criticalAlerts.length }}</div>
          <div class="stat-label">Critical Alerts</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon warning">
          <i class="pi pi-info-circle"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ warningAlerts.length }}</div>
          <div class="stat-label">Warning Alerts</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon success">
          <i class="pi pi-check-circle"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ activeResources }}</div>
          <div class="stat-label">Active Resources</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon info">
          <i class="pi pi-chart-line"></i>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ totalMetrics }}</div>
          <div class="stat-label">Total Metrics</div>
        </div>
      </div>
    </div>

    <!-- Active Alerts Section -->
    <div class="section">
      <AlertList
        title="Active Alerts"
        :alerts="activeAlerts"
        :loading="alertsLoading"
        @acknowledge="handleAcknowledgeAlert"
        @resolve="handleResolveAlert"
      />
    </div>

    <!-- Metrics Charts Section -->
    <div class="section">
      <h2>Resource Metrics</h2>
      <div class="charts-grid">
        <MetricChart
          v-for="chart in metricCharts"
          :key="chart.title"
          :title="chart.title"
          :metrics="chart.metrics"
          :chart-type="chart.type"
        />
      </div>
    </div>

    <!-- Metrics Summary Section -->
    <div class="section">
      <h2>Metrics Summary</h2>
      <MetricsSummary :summary-metrics="metricsSummary" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, defineAsyncComponent } from 'vue'
import { useMetricsStore } from '~/stores/monitoring/metricsStore'
import { useAlertsStore } from '~/stores/monitoring/alertsStore'
import { useLazyLoad } from '~/composables/useLazyLoad'

// Lazy load heavy components
const AlertList = defineAsyncComponent(() => import('~/components/monitoring/AlertList.vue'))
const MetricChart = defineAsyncComponent(() => import('~/components/monitoring/MetricChart.vue'))
const MetricsSummary = defineAsyncComponent(() => import('~/components/monitoring/MetricsSummary.vue'))

definePageMeta({
  layout: 'default',
  title: 'Resource Monitoring',
  description: 'Real-time monitoring and alerting for BSS infrastructure. Track system metrics, performance, and resource utilization with comprehensive dashboards.',
  keywords: 'monitoring, alerts, metrics, system health, infrastructure, BSS monitoring, performance, resources',
  ogTitle: 'BSS Resource Monitoring Dashboard',
  ogDescription: 'Real-time monitoring and alerting for BSS infrastructure. Track system metrics, performance, and resource utilization.',
  ogImage: '/images/monitoring-og.png',
  twitterCard: 'summary_large_image'
})

// Stores
const metricsStore = useMetricsStore()
const alertsStore = useAlertsStore()

// State
const loading = ref(false)
const customerId = ref(1) // In production, get from auth/session

// Computed
const alertsLoading = computed(() => alertsStore.loading)
const activeAlerts = computed(() => alertsStore.activeAlerts)
const criticalAlerts = computed(() => alertsStore.criticalAlerts)
const warningAlerts = computed(() => alertsStore.warningAlerts)
const metrics = computed(() => metricsStore.metrics)

const activeResources = computed(() => {
  // In production, fetch from API
  return 12
})

const totalMetrics = computed(() => metrics.value.length)

const metricsSummary = computed(() => {
  const metricTypes = [...new Set(metrics.value.map(m => m.metricType))]
  return metricTypes.map(type => {
    const typeMetrics = metrics.value.filter(m => m.metricType === type)
    const values = typeMetrics.map(m => m.value)

    return {
      metricType: type,
      currentValue: values[values.length - 1] || 0,
      unit: typeMetrics[0]?.unit || '',
      averageValue: values.reduce((a, b) => a + b, 0) / values.length || 0,
      minValue: Math.min(...values) || 0,
      maxValue: Math.max(...values) || 0,
      trend: 'STABLE' as const,
      lastUpdated: new Date().toISOString()
    }
  })
})

const metricCharts = computed(() => {
  const metricTypes = [...new Set(metrics.value.map(m => m.metricType))]
  return metricTypes.map(type => {
    const typeMetrics = metrics.value.filter(m => m.metricType === type)
    return {
      title: `${type} Usage`,
      metrics: typeMetrics,
      type: 'line' as const
    }
  })
})

// Methods
const refreshData = async () => {
  loading.value = true

  try {
    await Promise.all([
      alertsStore.fetchActiveAlertsByCustomer(customerId.value),
      metricsStore.fetchMetrics(customerId.value)
    ])
  } catch (error) {
    console.error('Error refreshing data:', error)
  } finally {
    loading.value = false
  }
}

const handleAcknowledgeAlert = async (alertId: number) => {
  try {
    await alertsStore.acknowledgeAlert(alertId, 'current-user')
  } catch (error) {
    console.error('Error acknowledging alert:', error)
  }
}

const handleResolveAlert = async (alertId: number) => {
  try {
    await alertsStore.resolveAlert(alertId, 'current-user')
  } catch (error) {
    console.error('Error resolving alert:', error)
  }
}

// Lifecycle
onMounted(() => {
  refreshData()
})
</script>

<style scoped>
.monitoring-dashboard {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.dashboard-header h1 {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
}

.header-actions {
  display: flex;
  gap: 1rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  display: flex;
  gap: 1rem;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

.stat-icon.critical {
  background: #fef2f2;
  color: #ef4444;
}

.stat-icon.warning {
  background: #fffbeb;
  color: #f59e0b;
}

.stat-icon.success {
  background: #f0fdf4;
  color: #10b981;
}

.stat-icon.info {
  background: #eff6ff;
  color: #3b82f6;
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #111827;
}

.stat-label {
  font-size: 0.875rem;
  color: #6b7280;
}

.section {
  margin-bottom: 3rem;
}

.section h2 {
  margin: 0 0 1.5rem 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #1f2937;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 1.5rem;
}
</style>
