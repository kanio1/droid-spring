<template>
  <div class="metric-chart">
    <div class="chart-header">
      <h3>{{ title }}</h3>
      <div class="chart-controls">
        <Button
          v-for="period in timePeriods"
          :key="period"
          :label="period"
          :class="{ 'p-button-outlined': selectedPeriod !== period }"
          size="small"
          @click="selectedPeriod = period"
        />
      </div>
    </div>
    <div class="chart-container">
      <canvas ref="chartCanvas"></canvas>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler
} from 'chart.js'
import type { ResourceMetric } from '~/types/monitoring/types'

// Register Chart.js components
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler
)

interface Props {
  title: string
  metrics: ResourceMetric[]
  chartType?: 'line' | 'bar'
  height?: number
}

const props = withDefaults(defineProps<Props>(), {
  chartType: 'line',
  height: 300
})

const chartCanvas = ref<HTMLCanvasElement | null>(null)
const chart = ref<ChartJS | null>(null)
const selectedPeriod = ref('1H')

const timePeriods = ['1H', '6H', '12H', '24H', '7D']

const createChart = () => {
  if (!chartCanvas.value || !props.metrics.length) return

  const ctx = chartCanvas.value.getContext('2d')
  if (!ctx) return

  if (chart.value) {
    chart.value.destroy()
  }

  const filteredMetrics = filterMetricsByPeriod(props.metrics, selectedPeriod.value)

  const labels = filteredMetrics.map(m =>
    new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  )

  const data = filteredMetrics.map(m => m.value)

  chart.value = new ChartJS(ctx, {
    type: props.chartType,
    data: {
      labels,
      datasets: [
        {
          label: props.title,
          data,
          borderColor: '#3B82F6',
          backgroundColor: 'rgba(59, 130, 246, 0.1)',
          fill: true,
          tension: 0.4,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: true,
        },
        tooltip: {
          mode: 'index',
          intersect: false,
        },
      },
      scales: {
        y: {
          beginAtZero: true,
          grid: {
            color: 'rgba(0, 0, 0, 0.05)',
          },
        },
        x: {
          grid: {
            display: false,
          },
        },
      },
    },
  })
}

const filterMetricsByPeriod = (metrics: ResourceMetric[], period: string) => {
  const now = new Date()
  const hours = {
    '1H': 1,
    '6H': 6,
    '12H': 12,
    '24H': 24,
    '7D': 168,
  }[period] || 1

  const cutoff = new Date(now.getTime() - hours * 60 * 60 * 1000)

  return metrics
    .filter(m => new Date(m.timestamp) >= cutoff)
    .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
}

watch([() => props.metrics, selectedPeriod], () => {
  createChart()
})

onMounted(() => {
  createChart()
})
</script>

<style scoped>
.metric-chart {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.chart-header h3 {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
}

.chart-controls {
  display: flex;
  gap: 0.5rem;
}

.chart-container {
  position: relative;
  height: 300px;
}
</style>
