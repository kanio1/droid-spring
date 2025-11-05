<template>
  <div class="cycles-chart-container">
    <div class="cycles-chart">
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
        <div class="tooltip-value">{{ tooltip.data?.count }} cycles</div>
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
interface CycleData {
  status: string
  count: number
  label: string
  color: string
}

const props = defineProps<{
  data: CycleData[]
  width?: number
  height?: number
}>()

const chartCanvas = ref<HTMLCanvasElement | null>(null)
const width = computed(() => props.width || 800)
const height = computed(() => props.height || 250)

// Tooltip state
const tooltip = ref({
  visible: false,
  x: 0,
  y: 0,
  data: null as CycleData | null
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

  const data = props.data || []
  if (data.length === 0) return

  // Chart padding
  const padding = 40
  const chartWidth = width.value - padding * 2
  const chartHeight = height.value - padding * 2

  // Find max value
  const maxCount = Math.max(...data.map(d => d.count), 1)

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
    const value = Math.round((maxCount / gridLines) * i)

    // Grid line
    ctx.strokeStyle = '#f3f4f6'
    ctx.beginPath()
    ctx.moveTo(padding, y)
    ctx.lineTo(width.value - padding, y)
    ctx.stroke()

    // Label
    ctx.fillText(value.toString(), padding - 10, y + 4)
  }

  // Draw bars
  const barWidth = chartWidth / data.length * 0.6
  const barSpacing = chartWidth / data.length
  const progress = animationProgress.value

  data.forEach((item, index) => {
    const x = padding + barSpacing * index + (barSpacing - barWidth) / 2
    const barHeight = (item.count / maxCount) * chartHeight * progress
    const y = height.value - padding - barHeight

    // Draw bar
    ctx.fillStyle = item.color
    ctx.fillRect(x, y, barWidth, barHeight)

    // Draw value on top of bar (only when bar is fully visible)
    if (progress > 0.7) {
      ctx.fillStyle = '#374151'
      ctx.font = 'bold 14px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(item.count.toString(), x + barWidth / 2, y - 8)
    }

    // Draw label
    if (progress > 0.9) {
      ctx.font = '12px sans-serif'
      ctx.fillStyle = '#6b7280'
      ctx.fillText(item.label, x + barWidth / 2, height.value - padding + 20)
    }
  })
}

// Animation function
const animateChart = () => {
  animationProgress.value = 0
  const startTime = Date.now()
  const duration = 1300 // 1.3 seconds

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

  const data = props.data || []
  if (data.length === 0) return

  // Chart padding
  const padding = 40
  const chartWidth = width.value - padding * 2
  const chartHeight = height.value - padding * 2

  // Find max value
  const maxCount = Math.max(...data.map(d => d.count), 1)

  // Check if mouse is over any bar
  const barWidth = chartWidth / data.length * 0.6
  const barSpacing = chartWidth / data.length

  let hoveredIndex = -1

  for (let i = 0; i < data.length; i++) {
    const barX = padding + barSpacing * i + (barSpacing - barWidth) / 2
    const barHeight = (data[i].count / maxCount) * chartHeight
    const barY = height.value - padding - barHeight

    // Check if mouse is within bar bounds
    if (x >= barX && x <= barX + barWidth && y >= barY && y <= height.value - padding) {
      hoveredIndex = i
      break
    }
  }

  if (hoveredIndex >= 0) {
    tooltip.value = {
      visible: true,
      x: x + 10,
      y: y - 10,
      data: data[hoveredIndex]
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
    link.download = `cycles-chart-${Date.now()}.png`
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
.cycles-chart-container {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.cycles-chart {
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
