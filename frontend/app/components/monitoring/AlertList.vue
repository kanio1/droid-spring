<template>
  <div class="alert-list">
    <div class="alert-list-header">
      <h3>{{ title }}</h3>
      <Badge :value="alerts.length" severity="danger" />
    </div>

    <div v-if="loading" class="loading-state">
      <ProgressSpinner />
    </div>

    <div v-else-if="alerts.length === 0" class="empty-state">
      <i class="pi pi-check-circle" style="font-size: 2rem; color: #10b981;"></i>
      <p>No active alerts</p>
    </div>

    <div v-else class="alerts">
      <div
        v-for="alert in alerts"
        :key="alert.id"
        :class="['alert-item', `alert-${alert.severity.toLowerCase()}`]"
      >
        <div class="alert-icon">
          <i :class="getAlertIcon(alert.severity)"></i>
        </div>
        <div class="alert-content">
          <div class="alert-header">
            <span class="alert-severity">{{ alert.severity }}</span>
            <span class="alert-metric">{{ alert.metricType }}</span>
          </div>
          <p class="alert-message">{{ alert.message }}</p>
          <div class="alert-footer">
            <span class="alert-time">{{ formatTime(alert.triggeredAt) }}</span>
            <div class="alert-actions">
              <Button
                v-if="alert.status === 'OPEN'"
                label="Acknowledge"
                icon="pi pi-check"
                size="small"
                outlined
                @click="handleAcknowledge(alert.id)"
              />
              <Button
                v-if="alert.status !== 'RESOLVED'"
                label="Resolve"
                icon="pi pi-times"
                size="small"
                severity="success"
                @click="handleResolve(alert.id)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { Alert } from '~/types/monitoring/types'

interface Props {
  title: string
  alerts: Alert[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  acknowledge: [alertId: number]
  resolve: [alertId: number]
}>()

const getAlertIcon = (severity: string) => {
  switch (severity) {
    case 'CRITICAL':
      return 'pi pi-exclamation-triangle'
    case 'WARNING':
      return 'pi pi-info-circle'
    default:
      return 'pi pi-check-circle'
  }
}

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) return `${days}d ago`
  if (hours > 0) return `${hours}h ago`
  if (minutes > 0) return `${minutes}m ago`
  return 'Just now'
}

const handleAcknowledge = (alertId: number) => {
  emit('acknowledge', alertId)
}

const handleResolve = (alertId: number) => {
  emit('resolve', alertId)
}
</script>

<style scoped>
.alert-list {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.alert-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.alert-list-header h3 {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
  color: #1f2937;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: #6b7280;
}

.empty-state i {
  margin-bottom: 1rem;
}

.alerts {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.alert-item {
  display: flex;
  gap: 1rem;
  padding: 1rem;
  border-radius: 8px;
  border-left: 4px solid;
  background: #f9fafb;
}

.alert-critical {
  border-left-color: #ef4444;
  background: #fef2f2;
}

.alert-warning {
  border-left-color: #f59e0b;
  background: #fffbeb;
}

.alert-ok {
  border-left-color: #10b981;
  background: #f0fdf4;
}

.alert-icon {
  display: flex;
  align-items: flex-start;
  padding-top: 0.25rem;
}

.alert-icon i {
  font-size: 1.25rem;
}

.alert-critical .alert-icon i {
  color: #ef4444;
}

.alert-warning .alert-icon i {
  color: #f59e0b;
}

.alert-ok .alert-icon i {
  color: #10b981;
}

.alert-content {
  flex: 1;
}

.alert-header {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  margin-bottom: 0.5rem;
}

.alert-severity {
  font-weight: 600;
  font-size: 0.875rem;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  text-transform: uppercase;
}

.alert-critical .alert-severity {
  background: #fee2e2;
  color: #991b1b;
}

.alert-warning .alert-severity {
  background: #fef3c7;
  color: #92400e;
}

.alert-metric {
  font-size: 0.875rem;
  color: #6b7280;
}

.alert-message {
  margin: 0 0 0.75rem 0;
  color: #374151;
  font-size: 0.875rem;
}

.alert-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.alert-time {
  font-size: 0.75rem;
  color: #9ca3af;
}

.alert-actions {
  display: flex;
  gap: 0.5rem;
}
</style>
