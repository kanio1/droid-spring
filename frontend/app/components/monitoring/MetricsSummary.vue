<template>
  <div class="metrics-summary">
    <div
      v-for="metric in summaryMetrics"
      :key="metric.metricType"
      class="metric-card"
    >
      <div class="metric-header">
        <h4>{{ metric.metricType }}</h4>
        <Badge :value="metric.unit" />
      </div>
      <div class="metric-value">
        {{ formatValue(metric.currentValue) }}
      </div>
      <div class="metric-stats">
        <div class="stat">
          <span class="stat-label">Avg</span>
          <span class="stat-value">{{ formatValue(metric.averageValue) }}</span>
        </div>
        <div class="stat">
          <span class="stat-label">Min</span>
          <span class="stat-value">{{ formatValue(metric.minValue) }}</span>
        </div>
        <div class="stat">
          <span class="stat-label">Max</span>
          <span class="stat-value">{{ formatValue(metric.maxValue) }}</span>
        </div>
      </div>
      <div class="metric-trend">
        <i :class="getTrendIcon(metric.trend)"></i>
        <span :class="['trend-text', `trend-${metric.trend.toLowerCase()}`]">
          {{ metric.trend }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { MetricSummary } from '~/types/monitoring/types'

interface Props {
  summaryMetrics: MetricSummary[]
}

defineProps<Props>()

const formatValue = (value: number): string => {
  if (value >= 1000000) {
    return `${(value / 1000000).toFixed(2)}M`
  } else if (value >= 1000) {
    return `${(value / 1000).toFixed(2)}K`
  }
  return value.toFixed(2)
}

const getTrendIcon = (trend: string) => {
  switch (trend) {
    case 'UP':
      return 'pi pi-arrow-up'
    case 'DOWN':
      return 'pi pi-arrow-down'
    default:
      return 'pi pi-minus'
  }
}
</script>

<style scoped>
.metrics-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.metric-card {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.metric-header h4 {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
}

.metric-value {
  font-size: 2rem;
  font-weight: 700;
  color: #111827;
  margin-bottom: 1rem;
}

.metric-stats {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.stat {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 0.75rem;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-value {
  font-size: 0.875rem;
  font-weight: 600;
  color: #374151;
}

.metric-trend {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

.trend-text {
  font-weight: 600;
}

.trend-up {
  color: #10b981;
}

.trend-down {
  color: #ef4444;
}

.trend-stable {
  color: #6b7280;
}
</style>
