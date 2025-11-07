<template>
  <div class="metrics-page page-container">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header__content">
        <h1 class="page-title">
          <i class="pi pi-chart-line"></i>
          Real-Time Metrics
        </h1>
        <p class="page-subtitle">
          Time-series monitoring with TimescaleDB
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
          label="Auto Refresh"
          :icon="autoRefresh ? 'pi pi-pause' : 'pi pi-play'"
          :severity="autoRefresh ? 'warning' : 'primary'"
          @click="toggleAutoRefresh"
        />
      </div>
    </div>

    <!-- Real-time Metric Cards -->
    <div class="metrics-grid" v-if="latestMetrics">
      <div class="metric-card" v-for="metric in displayedMetrics" :key="metric.name">
        <div class="metric-card__header">
          <span class="metric-card__label">{{ metric.name }}</span>
          <Tag :value="getTrendIcon(metric.trend)" :severity="getTrendSeverity(metric.trend)" />
        </div>
        <div class="metric-card__value">{{ formatMetricValue(metric.value, metric.unit) }}</div>
        <div class="metric-card__trend">
          <i :class="getTrendIcon(metric.trend)"></i>
          <span :class="getTrendClass(metric.trend)">
            {{ metric.trend ? metric.trend.changeDescription : 'N/A' }}
          </span>
        </div>
      </div>
    </div>

    <!-- Time Range Selector -->
    <div class="time-range-selector">
      <Button
        v-for="range in timeRanges"
        :key="range.label"
        :label="range.label"
        :severity="selectedTimeRange === range.value ? 'primary' : 'secondary'"
        outlined
        size="small"
        @click="selectTimeRange(range.value)"
      />
    </div>

    <!-- Tabs -->
    <div class="tabs-container">
      <TabView>
        <TabPanel header="Performance Metrics" leftIcon="pi pi-tachometer-alt">
          <div class="tab-content">
            <div class="section">
              <h3>
                <i class="pi pi-chart-area"></i>
                Database Performance Over Time
              </h3>
              <div class="chart-container" v-if="performanceData.length > 0">
                <Line :data="getPerformanceChartData()" :options="chartOptions" />
              </div>
              <div v-else class="no-data">
                <i class="pi pi-info-circle"></i>
                <p>No performance data available</p>
              </div>
            </div>

            <div class="section">
              <h3>
                <i class="pi pi-list"></i>
                Top Metrics
              </h3>
              <DataTable
                :value="topMetrics"
                :paginator="true"
                :rows="10"
                :loading="loading"
                responsiveLayout="scroll"
                class="p-datatable-sm"
              >
                <Column field="metricName" header="Metric" style="width: 200px">
                  <template #body="{ data }">
                    <strong>{{ data.getDisplayName() }}</strong>
                  </template>
                </Column>
                <Column field="averageValue" header="Average" style="width: 150px">
                  <template #body="{ data }">
                    {{ formatNumber(data.averageValue) }}
                  </template>
                </Column>
                <Column field="maximumValue" header="Maximum" style="width: 150px">
                  <template #body="{ data }">
                    {{ formatNumber(data.maximumValue) }}
                  </template>
                </Column>
                <Column field="sampleCount" header="Samples" style="width: 120px">
                  <template #body="{ data }">
                    {{ formatNumber(data.sampleCount) }}
                  </template>
                </Column>
              </DataTable>
            </div>
          </div>
        </TabPanel>

        <TabPanel header="Business Metrics" leftIcon="pi pi-briefcase">
          <div class="tab-content">
            <div class="section">
              <h3>
                <i class="pi pi-dollar"></i>
                Revenue & Business KPIs
              </h3>
              <div class="chart-container" v-if="businessData.length > 0">
                <Line :data="getBusinessChartData()" :options="chartOptions" />
              </div>
              <div v-else class="no-data">
                <i class="pi pi-info-circle"></i>
                <p>No business data available</p>
              </div>
            </div>

            <div class="section" v-if="businessStatistics">
              <h3>
                <i class="pi pi-chart-bar"></i>
                Business Statistics
              </h3>
              <div class="stats-cards">
                <div class="stat-item">
                  <span class="stat-label">Average Revenue</span>
                  <span class="stat-value">${{ formatNumber(businessStatistics.average) }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">Peak Revenue</span>
                  <span class="stat-value">${{ formatNumber(businessStatistics.maximum) }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">Sample Count</span>
                  <span class="stat-value">{{ formatNumber(businessStatistics.sampleCount) }}</span>
                </div>
              </div>
            </div>
          </div>
        </TabPanel>

        <TabPanel header="Resource Monitoring" leftIcon="pi pi-server">
          <div class="tab-content">
            <div class="section">
              <h3>
                <i class="pi pi-desktop"></i>
                System Resource Usage
              </h3>
              <div class="resource-grid">
                <div class="resource-card" v-for="resource in resourceData" :key="resource.resourceName">
                  <div class="resource-card__header">
                    <h4>{{ resource.resourceName }}</h4>
                    <Tag :value="getUsageTag(resource.maximumUsagePercent)" :severity="getUsageSeverity(resource.maximumUsagePercent)" />
                  </div>
                  <div class="resource-usage">
                    <ProgressBar :value="resource.averageUsagePercent" :showValue="false" style="height: 20px" />
                    <div class="usage-labels">
                      <span>Avg: {{ resource.averageUsagePercent.toFixed(1) }}%</span>
                      <span>Peak: {{ resource.maximumUsagePercent.toFixed(1) }}%</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="section" v-if="capacityPlanning">
              <h3>
                <i class="pi pi-exclamation-triangle"></i>
                Capacity Planning
              </h3>
              <div class="capacity-grid">
                <div class="capacity-card" v-for="capacity in capacityPlanning" :key="capacity.resourceType">
                  <div class="capacity-card__header">
                    <h4>{{ capacity.resourceType.toUpperCase() }}</h4>
                    <Tag :value="capacity.statusColor.toUpperCase()" :severity="getCapacitySeverity(capacity.maximumUsage)" />
                  </div>
                  <div class="capacity-stats">
                    <div class="capacity-stat">
                      <span class="stat-label">Average Usage</span>
                      <span class="stat-value">{{ capacity.averageUsage?.toFixed(1) || 0 }}%</span>
                    </div>
                    <div class="capacity-stat">
                      <span class="stat-label">Peak Usage</span>
                      <span class="stat-value">{{ capacity.maximumUsage?.toFixed(1) || 0 }}%</span>
                    </div>
                    <div class="capacity-stat">
                      <span class="stat-label">Minimum Usage</span>
                      <span class="stat-value">{{ capacity.minimumUsage?.toFixed(1) || 0 }}%</span>
                    </div>
                  </div>
                  <div class="capacity-recommendation">
                    <i class="pi pi-info-circle"></i>
                    <span>{{ capacity.capacityRecommendation }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </TabPanel>

        <TabPanel header="Trends" leftIcon="pi pi-trending-up">
          <div class="tab-content">
            <div class="section">
              <h3>
                <i class="pi pi-arrow-up-right"></i>
                Metric Trends
              </h3>
              <div class="trends-grid">
                <div class="trend-card" v-for="trend in trends" :key="trend.metricName">
                  <div class="trend-card__header">
                    <h4>{{ trend.metricName }}</h4>
                    <Tag :value="trend.changeDirection" :severity="getTrendSeverity(trend)" />
                  </div>
                  <div class="trend-comparison">
                    <div class="trend-value">
                      <span class="label">Current</span>
                      <span class="value">{{ formatNumber(trend.currentValue) }}</span>
                    </div>
                    <div class="trend-arrow">
                      <i :class="getTrendIcon(trend)"></i>
                    </div>
                    <div class="trend-value">
                      <span class="label">Previous</span>
                      <span class="value">{{ formatNumber(trend.previousValue) }}</span>
                    </div>
                  </div>
                  <div class="trend-change">
                    {{ trend.changeDescription }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </TabPanel>
      </TabView>
    </div>

    <!-- Toast Messages -->
    <Toast />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useApi } from '~/composables/useApi'
import { useToast } from 'primevue/usetoast'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js'

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
)

// Meta
definePageMeta({
  title: 'Real-Time Metrics',
  description: 'Time-series monitoring with TimescaleDB',
  layout: 'default'
})

// Composables
const { get } = useApi()
const toast = useToast()

// State
const loading = ref(false)
const autoRefresh = ref(true)
const selectedTimeRange = ref('24h')
const performanceData = ref<any[]>([])
const businessData = ref<any[]>([])
const resourceData = ref<any[]>([])
const topMetrics = ref<any[]>([])
const trends = ref<any[]>([])
const businessStatistics = ref<any>(null)
const capacityPlanning = ref<any[]>([])
const latestMetrics = ref<any>({})

const timeRanges = [
  { label: '1H', value: '1h' },
  { label: '6H', value: '6h' },
  { label: '24H', value: '24h' },
  { label: '7D', value: '7d' },
  { label: '30D', value: '30d' }
]

// Chart options
const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'top' as const
    },
    title: {
      display: false
    }
  },
  scales: {
    y: {
      beginAtZero: true
    }
  }
}

let refreshInterval: NodeJS.Timeout | null = null

// Computed
const displayedMetrics = computed(() => {
  return Object.entries(latestMetrics.value).map(([name, value]: [string, any]) => ({
    name,
    value: value.value,
    unit: value.unit,
    trend: value.trend
  }))
})

// Methods
const fetchAllData = async () => {
  loading.value = true

  try {
    const now = new Date()
    const startTime = new Date(now.getTime() - getTimeRangeMs(selectedTimeRange.value))

    // Fetch performance data
    const perfResponse = await get(`/timeseries/metrics/db_query_time?startTime=${startTime.toISOString()}&endTime=${now.toISOString()}`)
    performanceData.value = perfResponse.data || []

    // Fetch business data
    const businessResponse = await get(`/timeseries/business-metrics/revenue?startTime=${startTime.toISOString()}&endTime=${now.toISOString()}`)
    businessData.value = businessResponse.data || []

    // Fetch resource data
    const resourceResponse = await get(`/timeseries/resource-metrics?host=server-01&resourceType=cpu&startTime=${startTime.toISOString()}&endTime=${now.toISOString()}`)
    resourceData.value = resourceResponse.data || []

    // Fetch top metrics
    const topResponse = await get(`/timeseries/metrics/top?startTime=${startTime.toISOString()}&endTime=${now.toISOString()}&limit=10`)
    topMetrics.value = topResponse.data || []

    // Fetch latest metrics
    const metrics = ['db_query_time', 'db_connections_active', 'db_cache_hit_ratio']
    const latest: any = {}
    for (const metric of metrics) {
      const response = await get(`/timeseries/metrics/${metric}/latest`)
      latest[metric] = { value: response.data, unit: getUnit(metric) }
    }
    latestMetrics.value = latest

  } catch (error: any) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error.message || 'Failed to fetch metrics data',
      life: 5000
    })
  } finally {
    loading.value = false
  }
}

const getTimeRangeMs = (range: string) => {
  switch (range) {
    case '1h': return 60 * 60 * 1000
    case '6h': return 6 * 60 * 60 * 1000
    case '24h': return 24 * 60 * 60 * 1000
    case '7d': return 7 * 24 * 60 * 60 * 1000
    case '30d': return 30 * 24 * 60 * 60 * 1000
    default: return 24 * 60 * 60 * 1000
  }
}

const selectTimeRange = (range: string) => {
  selectedTimeRange.value = range
  fetchAllData()
}

const refreshData = () => {
  fetchAllData()
  toast.add({
    severity: 'success',
    summary: 'Success',
    detail: 'Metrics data refreshed',
    life: 3000
  })
}

const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

const startAutoRefresh = () => {
  if (refreshInterval) clearInterval(refreshInterval)
  refreshInterval = setInterval(() => {
    fetchAllData()
  }, 30000) // Refresh every 30 seconds
}

const stopAutoRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
}

const getPerformanceChartData = () => {
  const labels = performanceData.value.map((d: any) => new Date(d.timestamp).toLocaleTimeString())
  const data = performanceData.value.map((d: any) => d.averageValue)

  return {
    labels,
    datasets: [
      {
        label: 'Query Time (ms)',
        data,
        borderColor: 'rgb(75, 192, 192)',
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        tension: 0.1
      }
    ]
  }
}

const getBusinessChartData = () => {
  const labels = businessData.value.map((d: any) => new Date(d.timestamp).toLocaleDateString())
  const data = businessData.value.map((d: any) => d.totalValue)

  return {
    labels,
    datasets: [
      {
        label: 'Revenue ($)',
        data,
        borderColor: 'rgb(53, 162, 235)',
        backgroundColor: 'rgba(53, 162, 235, 0.2)',
        tension: 0.1
      }
    ]
  }
}

const getUnit = (metric: string) => {
  if (metric.includes('time')) return 'ms'
  if (metric.includes('connections')) return 'connections'
  if (metric.includes('ratio')) return '%'
  return ''
}

const formatMetricValue = (value: number, unit: string) => {
  if (value === null || value === undefined) return 'N/A'
  return `${formatNumber(value)} ${unit || ''}`.trim()
}

const formatNumber = (num: number) => {
  if (num === null || num === undefined) return '0'
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 2 }).format(num)
}

const getTrendIcon = (trend: any) => {
  if (!trend) return 'pi pi-minus'
  return trend.isIncreasing() ? 'pi pi-arrow-up' : trend.isDecreasing() ? 'pi pi-arrow-down' : 'pi pi-minus'
}

const getTrendSeverity = (trend: any) => {
  if (!trend) return 'info'
  return trend.isIncreasing() ? 'success' : trend.isDecreasing() ? 'danger' : 'info'
}

const getTrendClass = (trend: any) => {
  if (!trend) return ''
  return trend.isIncreasing() ? 'text-green-500' : trend.isDecreasing() ? 'text-red-500' : 'text-gray-500'
}

const getUsageTag = (usage: number) => {
  return usage?.toFixed(1) + '%' || '0%'
}

const getUsageSeverity = (usage: number) => {
  if (!usage) return 'info'
  if (usage > 90) return 'danger'
  if (usage > 70) return 'warning'
  return 'success'
}

const getCapacitySeverity = (usage: number) => {
  if (!usage) return 'info'
  if (usage > 90) return 'danger'
  if (usage > 80) return 'warning'
  return 'success'
}

// Lifecycle
onMounted(() => {
  fetchAllData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.metrics-page {
  padding: 2rem;
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-title {
  margin: 0;
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.page-subtitle {
  margin: 0.5rem 0 0 0;
  color: #6b7280;
  font-size: 1rem;
}

.page-header__actions {
  display: flex;
  gap: 1rem;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.metric-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.metric-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.metric-card__label {
  font-size: 0.875rem;
  color: #6b7280;
  text-transform: uppercase;
  font-weight: 600;
}

.metric-card__value {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  margin-bottom: 0.5rem;
}

.metric-card__trend {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.time-range-selector {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 2rem;
  justify-content: center;
}

.tabs-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.tab-content {
  padding: 2rem;
}

.section {
  margin-bottom: 3rem;
}

.section h3 {
  margin: 0 0 1.5rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.chart-container {
  height: 400px;
  margin-bottom: 2rem;
}

.no-data {
  text-align: center;
  padding: 3rem;
  color: #6b7280;
}

.no-data i {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
}

.stat-item {
  background: #f9fafb;
  border-radius: 8px;
  padding: 1.5rem;
  text-align: center;
}

.stat-label {
  display: block;
  font-size: 0.875rem;
  color: #6b7280;
  margin-bottom: 0.5rem;
}

.stat-value {
  display: block;
  font-size: 1.5rem;
  font-weight: 700;
  color: #1f2937;
}

.resource-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.resource-card {
  background: #f9fafb;
  border-radius: 8px;
  padding: 1.5rem;
}

.resource-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.resource-card__header h4 {
  margin: 0;
  font-size: 1rem;
  color: #1f2937;
}

.resource-usage {
  margin-top: 1rem;
}

.usage-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 0.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}

.capacity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.capacity-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  border: 2px solid #e5e7eb;
}

.capacity-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.capacity-card__header h4 {
  margin: 0;
  font-size: 1.25rem;
  color: #1f2937;
}

.capacity-stats {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.capacity-stat {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-bottom: 1px solid #e5e7eb;
}

.capacity-stat .stat-label {
  color: #6b7280;
  font-size: 0.875rem;
}

.capacity-stat .stat-value {
  font-weight: 600;
  color: #1f2937;
}

.capacity-recommendation {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background: #f9fafb;
  border-radius: 4px;
  font-size: 0.875rem;
  color: #374151;
}

.capacity-recommendation i {
  color: #3b82f6;
}

.trends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.trend-card {
  background: #f9fafb;
  border-radius: 8px;
  padding: 1.5rem;
}

.trend-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.trend-card__header h4 {
  margin: 0;
  font-size: 1rem;
  color: #1f2937;
}

.trend-comparison {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.trend-value {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.trend-value .label {
  font-size: 0.75rem;
  color: #6b7280;
  margin-bottom: 0.25rem;
}

.trend-value .value {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1f2937;
}

.trend-arrow {
  font-size: 1.5rem;
  color: #3b82f6;
}

.trend-change {
  text-align: center;
  font-size: 0.875rem;
  font-weight: 600;
}

.text-green-500 {
  color: #10b981;
}

.text-red-500 {
  color: #ef4444;
}

.text-gray-500 {
  color: #6b7280;
}
</style>
