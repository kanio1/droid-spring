<template>
  <div class="usage-chart-container">
    <div class="usage-chart">
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
        <div class="tooltip-value">{{ tooltip.data?.value }} records</div>
      </div>
    </div>

    <div class="chart-legend">
      <div v-for="(item, index) in legendData" :key="index" class="legend-item">
        <div class="legend-color" :style="{ backgroundColor: getColor(index) }"></div>
        <span class="legend-label">{{ item.label }}</span>
        <span class="legend-value">{{ item.value }}</span>
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
interface UsageData {
  label: string
  value: number
  icon?: string
}

const props = defineProps<{
  data: UsageData[]
  width?: number
  height?: number
}>()

const chartCanvas = ref<HTMLCanvasElement | null>(null)
const width = computed(() => props.width || 300)
const height = computed(() => props.height || 300)
const legendData = computed(() => props.data || [])

// Tooltip state
const tooltip = ref({
  visible: false,
  x: 0,
  y: 0,
  data: null as UsageData | null
})

// Animation state
const animationProgress = ref(0)

const colors = [
  '#3b82f6', // blue
  '#10b981', // emerald
  '#f59e0b', // amber
  '#ef4444', // red
  '#8b5cf6', // violet
  '#ec4899', // pink
  '#14b8a6', // teal
  '#f97316'  // orange
]

const drawChart = () => {
  if (!chartCanvas.value) return

  const canvas = chartCanvas.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // Clear canvas
  ctx.clearRect(0, 0, width.value, height.value)

  const data = props.data || []
  if (data.length === 0) return

  // Calculate total
  const total = data.reduce((sum, item) => sum + item.value, 0)

  // Chart settings
  const centerX = width.value / 2
  const centerY = height.value / 2
  const radius = Math.min(width.value, height.value) / 2 - 40

  // Calculate slice angles
  const slices = data.map((item, index) => {
    const sliceAngle = (item.value / total) * 2 * Math.PI
    return { item, index, sliceAngle }
  })

  // Draw pie slices with animation
  let currentAngle = -Math.PI / 2
  const progress = animationProgress.value

  slices.forEach((slice) => {
    const animatedAngle = slice.sliceAngle * progress
    const color = colors[slice.index % colors.length]

    ctx.beginPath()
    ctx.moveTo(centerX, centerY)
    ctx.arc(centerX, centerY, radius, currentAngle, currentAngle + animatedAngle)
    ctx.closePath()
    ctx.fillStyle = color
    ctx.fill()

    // Draw border
    ctx.strokeStyle = '#fff'
    ctx.lineWidth = 2
    ctx.stroke()

    currentAngle += slice.sliceAngle
  })

  // Draw center circle for donut effect (only when fully animated)
  if (progress > 0.95) {
    ctx.beginPath()
    ctx.arc(centerX, centerY, radius * 0.6, 0, 2 * Math.PI)
    ctx.fillStyle = '#fff'
    ctx.fill()
  }
}

// Helper function
const getColor = (index: number) => {
  return colors[index % colors.length]
}

// Animation function
const animateChart = () => {
  animationProgress.value = 0
  const startTime = Date.now()
  const duration = 1200 // 1.2 seconds

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

  // Calculate total
  const total = data.reduce((sum, item) => sum + item.value, 0)

  // Chart settings
  const centerX = width.value / 2
  const centerY = height.value / 2
  const radius = Math.min(width.value, height.value) / 2 - 40

  // Calculate distance from center
  const distanceFromCenter = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2))

  // Check if mouse is within the pie chart
  if (distanceFromCenter > radius || distanceFromCenter < radius * 0.6) {
    tooltip.value.visible = false
    return
  }

  // Calculate angle
  const angle = Math.atan2(y - centerY, x - centerX) + Math.PI / 2
  const normalizedAngle = angle < 0 ? angle + 2 * Math.PI : angle

  // Find which slice the mouse is over
  let currentAngle = -Math.PI / 2
  let hoveredIndex = -1

  for (let i = 0; i < data.length; i++) {
    const sliceAngle = (data[i].value / total) * 2 * Math.PI
    const sliceEnd = currentAngle + sliceAngle

    if (normalizedAngle >= currentAngle && normalizedAngle < sliceEnd) {
      hoveredIndex = i
      break
    }

    currentAngle = sliceEnd
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
    link.download = `usage-chart-${Date.now()}.png`
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
.usage-chart-container {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: center;
}

.usage-chart {
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

.chart-legend {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--font-size-sm);
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  flex-shrink: 0;
}

.legend-label {
  flex: 1;
  color: var(--color-text-primary);
}

.legend-value {
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
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
