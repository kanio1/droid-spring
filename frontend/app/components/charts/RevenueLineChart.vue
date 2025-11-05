<template>
  <div class="revenue-chart-container">
    <div class="revenue-chart">
      <canvas ref="chartCanvas" :width="width" :height="height" @mousemove="handleMouseMove" @mouseleave="handleMouseLeave"></canvas>

      <!-- Tooltip -->
      <div
        v-if="tooltip.visible"
        class="chart-tooltip"
        :style="{
          left: tooltip.x + 'px',
          top: tooltip.y + 'px'
        }"
      >
        <div class="tooltip-label">{{ tooltip.data?.label }}</div>
        <div class="tooltip-value">{{ formatCurrency(tooltip.data?.revenue || 0) }}</div>
      </div>
    </div>

    <!-- Export Button -->
    <Button
      icon="pi pi-download"
      label="Export PNG"
      severity="secondary"
      size="small"
      text
      class="export-button"
      @click="handleExport"
    />
  </div>
</template>

<script setup lang="ts">
interface RevenueData {
  month: string
  revenue: number
  label: string
}

const props = defineProps<{
  data: RevenueData[]
  width?: number
  height?: number
  color?: string
}>()

const chartCanvas = ref<HTMLCanvasElement | null>(null)
const width = computed(() => props.width || 800)
const height = computed(() => props.height || 300)
const color = computed(() => props.color || '#3b82f6')

// Tooltip state
const tooltip = ref({
  visible: false,
  x: 0,
  y: 0,
  data: null as RevenueData | null
})

// Animation state
const animationProgress = ref(0)

const drawChart = () => {
  if (!chartCanvas.value) return

  const canvas = chartCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // Clear canvas
  ctx.clearRect(0, 0, width.value, height.value)

  // Chart padding
  const padding = 40
  const chartWidth = width.value - padding * 2
  const chartHeight = height.value - padding * 2

  // Get data
  const data = props.data || []
  if (data.length === 0) return

  // Find min and max values
  const revenues = data.map(d => d.revenue)
  const minRevenue = Math.min(...revenues)
  const maxRevenue = Math.max(...revenues)
  const revenueRange = maxRevenue - minRevenue || 1

  // Draw axes
  ctx.strokeStyle = '#e5e7eb'
  ctx.lineWidth = 1

  // X-axis
  ctx.beginPath()
  ctx.moveTo(padding, height.value - padding)
  ctx.lineTo(width.value - padding, height.value - padding)
  ctx.stroke()

  // Y-axis
  ctx.beginPath()
  ctx.moveTo(padding, padding)
  ctx.lineTo(padding, height.value - padding)
  ctx.stroke()

  // Draw grid lines and Y-axis labels
  ctx.font = '12px sans-serif'
  ctx.fillStyle = '#6b7280'
  ctx.textAlign = 'right'

  const gridLines = 5
  for (let i = 0; i <= gridLines; i++) {
    const y = padding + (chartHeight / gridLines) * i
    const value = maxRevenue - (revenueRange / gridLines) * i

    // Grid line
    ctx.strokeStyle = '#f3f4f6'
    ctx.beginPath()
    ctx.moveTo(padding, y)
    ctx.lineTo(width.value - padding, y)
    ctx.stroke()

    // Label
    ctx.fillText(formatCurrency(value), padding - 10, y + 4)
  }

  // Calculate points
  const points = data.map((item, index) => {
    const x = padding + (chartWidth / (data.length - 1)) * index
    const y = height.value - padding - ((item.revenue - minRevenue) / revenueRange) * chartHeight
    return { x, y, data: item }
  })

  // Draw line chart with animation
  const progress = animationProgress.value
  if (progress > 0) {
    ctx.strokeStyle = color.value
    ctx.lineWidth = 3
    ctx.beginPath()

    const endIndex = Math.floor(points.length * progress)
    for (let i = 0; i < endIndex; i++) {
      if (i === 0) {
        ctx.moveTo(points[i].x, points[i].y)
      } else {
        ctx.lineTo(points[i].x, points[i].y)
      }
    }

    // Partial line for current point
    if (progress < 1 && endIndex < points.length && endIndex > 0) {
      const currentPoint = points[endIndex]
      const prevPoint = points[endIndex - 1]
      const partialProgress = (progress * points.length) % 1
      const x = prevPoint.x + (currentPoint.x - prevPoint.x) * partialProgress
      const y = prevPoint.y + (currentPoint.y - prevPoint.y) * partialProgress
      ctx.lineTo(x, y)
    }

    ctx.stroke()
  }

  // Draw points with animation
  const pointsToDraw = Math.floor(points.length * progress)
  ctx.fillStyle = color.value

  for (let i = 0; i < pointsToDraw; i++) {
    const point = points[i]
    // Point
    ctx.beginPath()
    ctx.arc(point.x, point.y, 5, 0, Math.PI * 2)
    ctx.fill()

    // X-axis label
    ctx.fillStyle = '#6b7280'
    ctx.font = '11px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(point.data.label, point.x, height.value - padding + 20)
    ctx.fillStyle = color.value
  }
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'PLN',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value)
}

// Animation function
const animateChart = () => {
  animationProgress.value = 0
  const startTime = Date.now()
  const duration = 1500 // 1.5 seconds

  const animate = () => {
    const elapsed = Date.now() - startTime
    const progress = Math.min(elapsed / duration, 1)
    // Easing function for smooth animation
    animationProgress.value = 1 - Math.pow(1 - progress, 3)
    drawChart()

    if (progress < 1) {
      requestAnimationFrame(animate)
    }
  }

  requestAnimationFrame(animate)
}

// Mouse event handlers
const handleMouseMove = (event: MouseEvent) => {
  if (!chartCanvas.value) return

  const rect = chartCanvas.value.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top

  // Chart padding
  const padding = 40
  const chartWidth = width.value - padding * 2
  const chartHeight = height.value - padding * 2

  // Get data
  const data = props.data || []
  if (data.length === 0) return

  // Find min and max values
  const revenues = data.map(d => d.revenue)
  const minRevenue = Math.min(...revenues)
  const maxRevenue = Math.max(...revenues)
  const revenueRange = maxRevenue - minRevenue || 1

  // Check if mouse is near any data point
  const dataPoints = data.map((item, index) => {
    const pointX = padding + (chartWidth / (data.length - 1)) * index
    const pointY = height.value - padding - ((item.revenue - minRevenue) / revenueRange) * chartHeight
    return { x: pointX, y: pointY, data: item }
  })

  const hoverThreshold = 10
  let hoveredPoint = null

  for (const point of dataPoints) {
    const distance = Math.sqrt(Math.pow(x - point.x, 2) + Math.pow(y - point.y, 2))
    if (distance < hoverThreshold) {
      hoveredPoint = point
      break
    }
  }

  if (hoveredPoint) {
    tooltip.value = {
      visible: true,
      x: hoveredPoint.x + 10,
      y: hoveredPoint.y - 10,
      data: hoveredPoint.data
    }
  } else {
    tooltip.value.visible = false
  }
}

const handleMouseLeave = () => {
  tooltip.value.visible = false
}

// Export to PNG
const handleExport = () => {
  if (!chartCanvas.value) return

  const canvas = chartCanvas.value
  canvas.toBlob((blob) => {
    if (!blob) return

    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `revenue-chart-${Date.now()}.png`
    link.click()
    URL.revokeObjectURL(url)
  })
}

onMounted(() => {
  animateChart()
})

watch(() => props.data, () => {
  animateChart()
}, { deep: true })
</script>

<style scoped>
.revenue-chart-container {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.revenue-chart {
  position: relative;
  width: 100%;
  height: 100%;
}

canvas {
  display: block;
  width: 100%;
  height: 100%;
}

/* Tooltip */
.chart-tooltip {
  position: absolute;
  background: rgba(0, 0, 0, 0.9);
  color: white;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--font-size-sm);
  pointer-events: none;
  z-index: 10;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  transform: translate(-50%, -100%);
  white-space: nowrap;
}

.chart-tooltip::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 6px solid rgba(0, 0, 0, 0.9);
}

.tooltip-label {
  font-weight: var(--font-weight-medium);
  margin-bottom: var(--space-1);
  color: rgba(255, 255, 255, 0.8);
}

.tooltip-value {
  font-weight: var(--font-weight-bold);
  font-size: var(--font-size-base);
}

/* Export Button */
.export-button {
  align-self: flex-start;
  padding: var(--space-2) var(--space-3);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  cursor: pointer;
  transition: all var(--transition-fast) var(--transition-timing);
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.export-button:hover {
  background: var(--color-surface-secondary);
  border-color: var(--color-primary);
  color: var(--color-text-primary);
}

.export-button::before {
  content: '⬇';
  font-size: var(--font-size-sm);
}
</style>
